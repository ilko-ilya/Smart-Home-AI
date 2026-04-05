import { NextResponse } from 'next/server';

export async function POST(request: Request) {
    try {
        const { text } = await request.json();

        // 👉 1. Захист від пустого тексту
        if (!text || !text.trim()) {
            console.warn("⚠️ TTS request rejected: Text is empty");
            return NextResponse.json({ error: "Text is empty" }, { status: 400 });
        }

        // 👉 2. Логування (обрізаємо до 50 символів для чистоти логів)
        console.log(`🔊 TTS request: "${text.substring(0, 50)}${text.length > 50 ? '...' : ''}"`);

        // Беремо ключ із серверного оточення. У браузер він НЕ потрапить!
        const apiKey = process.env.CARTESIA_API_KEY;

        if (!apiKey) {
            console.error("Відсутній CARTESIA_API_KEY у .env.local");
            return NextResponse.json({ error: "API key is missing" }, { status: 500 });
        }

        const VOICE_ID = "05ffab9c-d380-4909-8375-cd12f59238c3";

        // Робимо запит до Cartesia від імені нашого сервера
        const response = await fetch("https://api.cartesia.ai/tts/bytes", {
            method: "POST",
            headers: {
                "Cartesia-Version": "2024-06-10",
                "X-API-Key": apiKey,
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                model_id: "sonic-3",
                transcript: text,
                language: "uk",
                voice: {
                    mode: "id",
                    id: VOICE_ID
                },
                output_format: {
                    container: "mp3",
                    encoding: "mp3",
                    sample_rate: 44100
                }
            }),
        });

        if (!response.ok) {
            const errorData = await response.text();
            console.error("Cartesia API Error:", errorData);
            return NextResponse.json({ error: "Failed to fetch audio" }, { status: response.status });
        }

        // Отримуємо байси (ArrayBuffer) і передаємо їх на наш фронтенд
        const audioBuffer = await response.arrayBuffer();

        return new NextResponse(audioBuffer, {
            headers: {
                'Content-Type': 'audio/mpeg',
            },
        });

    } catch (error) {
        console.error("Internal TTS Error:", error);
        return NextResponse.json({ error: "Internal Server Error" }, { status: 500 });
    }
}