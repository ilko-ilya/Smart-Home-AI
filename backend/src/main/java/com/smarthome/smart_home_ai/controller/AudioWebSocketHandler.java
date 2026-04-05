package com.smarthome.smart_home_ai.controller;

import com.smarthome.smart_home_ai.service.AiClient;
import com.smarthome.smart_home_ai.service.JarvisService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioWebSocketHandler extends BinaryWebSocketHandler {

    private final AiClient aiClient;
    private final JarvisService jarvisService;

    private final Map<String, ByteArrayOutputStream> activeSpeechBuffers = new ConcurrentHashMap<>();
    private final Map<String, Integer> silenceCounters = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    // 🔥 VAD (шум)
    private final Map<String, Long> noiseLevels = new ConcurrentHashMap<>();
    private final Map<String, Boolean> calibrated = new ConcurrentHashMap<>();
    private final Map<String, Integer> calibrationFrames = new ConcurrentHashMap<>();

    // 🔥 realtime
    private final Map<String, Long> lastPartialSendTime = new ConcurrentHashMap<>();

    private final ExecutorService whisperExecutor = new ThreadPoolExecutor(
            5, 20, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @PreDestroy
    public void shutdown() {
        log.info("Зупинка пулу потоків Whisper...");
        whisperExecutor.shutdown();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String id = session.getId();

        log.info("Клієнт підключився: {}", id);

        activeSpeechBuffers.put(id, new ByteArrayOutputStream());
        silenceCounters.put(id, 0);

        noiseLevels.put(id, 0L);
        calibrated.put(id, false);
        calibrationFrames.put(id, 0);

        lastPartialSendTime.put(id, 0L);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, @NonNull BinaryMessage message) {

        String sessionId = session.getId();
        ByteArrayOutputStream speechBuffer = activeSpeechBuffers.get(sessionId);

        if (speechBuffer == null) return;

        byte[] payload = message.getPayload().array();

        boolean isSpeaking = hasVoice(session, payload);

        long now = System.currentTimeMillis();
        long lastPartial = lastPartialSendTime.getOrDefault(sessionId, 0L);

        if (isSpeaking) {
            speechBuffer.writeBytes(payload);
            silenceCounters.put(sessionId, 0);

            // 🔥 PARTIAL каждые 1.5 сек
            if (now - lastPartial > 1500 && speechBuffer.size() > 32000) {

                log.info("⚡ PARTIAL SEND");

                byte[] partialData = speechBuffer.toByteArray();
                lastPartialSendTime.put(sessionId, now);

                sendToWhisperAsync(session, partialData, true);
            }

            // 🔥 защита от зависания
            if (speechBuffer.size() > 160_000) {
                log.info("🔥 FORCE FINAL SEND");
                processCompletePhrase(session, speechBuffer);
            }

        } else {
            if (speechBuffer.size() > 0) {
                speechBuffer.writeBytes(payload);

                int silence = silenceCounters.getOrDefault(sessionId, 0) + 1;
                silenceCounters.put(sessionId, silence);

                if (silence >= 6) {
                    log.info("🔇 FINAL SEND");
                    processCompletePhrase(session, speechBuffer);
                }
            }
        }
    }

    private void processCompletePhrase(WebSocketSession session, ByteArrayOutputStream speechBuffer) {

        String sessionId = session.getId();

        byte[] rawPcmData = speechBuffer.toByteArray();

        log.info(">>> FINAL PHRASE size={}", rawPcmData.length);

        activeSpeechBuffers.put(sessionId, new ByteArrayOutputStream());
        silenceCounters.put(sessionId, 0);

        if (rawPcmData.length < 20000) {
            log.warn("Аудіо занадто коротке");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastRequestTime.getOrDefault(sessionId, 0L) < 1500) {
            log.warn("Rate limit...");
            return;
        }

        lastRequestTime.put(sessionId, now);

        sendToWhisperAsync(session, rawPcmData, false);
    }

    private void sendToWhisperAsync(WebSocketSession session, byte[] rawPcmData, boolean isPartial) {

        byte[] wavData = addWavHeader(rawPcmData);

        CompletableFuture.runAsync(() -> {

            String text = aiClient.transcribeAudioBytes(wavData);

            log.info("🧠 {} Whisper: {}", isPartial ? "PARTIAL" : "FINAL", text);

            if (text == null || text.isEmpty()) return;

            // 🔥 ВСТАВЛЯЄМО ФІЛЬТР ТУТ!
            // Якщо Whisper видав латиницю (галюцинацію) - ми просто обриваємо виконання і нічого не робимо
            if (!isMostlyCyrillic(text)) {
                log.warn("❌ Відкидаємо латиницю/галюцинацію: {}", text);
                return;
            }

            // 🔥 ВІДПРАВКА НА ФРОНТ У РЕАЛЬНОМУ ЧАСІ
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        String prefix = isPartial ? "JARVIS_PARTIAL:" : "JARVIS_FINAL:";
                        session.sendMessage(new TextMessage(prefix + text));
                    }
                } catch (Exception e) {
                    log.error("WS send error", e);
                }
            }

            // Якщо це фінальна фраза - відправляємо на виконання команди
            if (!isPartial) {
                handleFinalCommand(session, text);
            }

        }, whisperExecutor);
    }

    private void handleFinalCommand(WebSocketSession session, String text) {

        String lowerText = text.toLowerCase();

        if (containsWakeWord(lowerText)) {

            String command = lowerText
                    .replaceAll("(?i)(джарв[іи]с|jarvis|jervis|jers|джорем[іи]с|чарльз)", "")
                    .replaceAll("[.,!?-]", "")
                    .trim();

            log.info("🎯 COMMAND: {}", command);

            if (!command.isEmpty()) {

                String response = jarvisService.processVoiceCommand(command);

                if (session.isOpen()) {
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage("JARVIS_REPLY:" + response));
                        }
                    } catch (Exception e) {
                        log.error("WS send error", e);
                    }
                }
            }
        } else {
            log.info("❌ Wake word не найден");
        }
    }

    private boolean hasVoice(WebSocketSession session, byte[] pcm) {

        long sum = 0;

        for (int i = 0; i < pcm.length - 1; i += 2) {
            short sample = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            sum += Math.abs(sample);
        }

        long average = sum / (pcm.length / 2);
        String id = session.getId();

        // 🔥 УМНАЯ КАЛИБРОВКА (Без блокировки звука!)
        if (!calibrated.getOrDefault(id, false)) {

            long currentNoise = noiseLevels.getOrDefault(id, 0L);
            int frames = calibrationFrames.getOrDefault(id, 0);

            long newNoise = (currentNoise * frames + average) / (frames + 1);

            noiseLevels.put(id, newNoise);
            calibrationFrames.put(id, frames + 1);

            if (frames > 15) { // 10-15 фреймов дают идеальный базис шума
                calibrated.put(id, true);
                log.info("✅ calibration done! final noise baseline={}", newNoise);
            }
        }

        long noise = noiseLevels.get(id);

        // Порог голоса = шум + 800 (можно подкрутить, если будет плохо слышать)
        boolean isSpeaking = average > (noise + 800);

        // Логуємо тільки значні сплески звуку, щоб не засмічувати консоль
        if (average > (noise + 1500)) {
            log.info("🎤 СПЛЕСК ЗВУКУ: avg={}, noise={}, speaking={}", average, noise, isSpeaking);
        }

        return isSpeaking;
    }

    private byte[] addWavHeader(byte[] pcmData) {

        int sampleRate = 16000;
        short channels = 1;
        short bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;

        ByteBuffer buffer = ByteBuffer.allocate(44 + pcmData.length);
        buffer.order(LITTLE_ENDIAN);

        buffer.put("RIFF".getBytes());
        buffer.putInt(36 + pcmData.length);
        buffer.put("WAVE".getBytes());
        buffer.put("fmt ".getBytes());
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort(channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) (channels * bitsPerSample / 8));
        buffer.putShort(bitsPerSample);
        buffer.put("data".getBytes());
        buffer.putInt(pcmData.length);
        buffer.put(pcmData);

        return buffer.array();
    }

    private boolean containsWakeWord(String text) {
        if (text == null) return false;

        String normalized = text
                .toLowerCase()
                .replaceAll("[^\\p{L}\\p{Nd} ]", "");

        return normalized.contains("джарвис")
                || normalized.contains("джарвіс")
                || normalized.contains("jarvis")
                || normalized.contains("jervis")
                || normalized.contains("jers")
                || normalized.contains("джореміс")
                || normalized.contains("джоремис")
                || normalized.contains("чарльз");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {

        String id = session.getId();

        log.info("Клієнт відключився: {}", id);

        activeSpeechBuffers.remove(id);
        silenceCounters.remove(id);
        lastRequestTime.remove(id);

        noiseLevels.remove(id);
        calibrated.remove(id);
        calibrationFrames.remove(id);

        lastPartialSendTime.remove(id);
    }

    private boolean isMostlyCyrillic(String text) {
        if (text == null || text.isEmpty()) return false;

        long cyrillicCount = text.chars()
                .filter(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC)
                .count();

        return cyrillicCount >= text.length() * 0.3;
    }
}
