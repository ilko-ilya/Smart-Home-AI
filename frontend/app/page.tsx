'use client';

import { useState, useEffect, useRef } from 'react';
import type { Device } from './types';
import { speakJarvis } from '../utils/jarvisVoice';

// Читаємо URL з Docker/середовища, а якщо його немає — використовуємо localhost за замовчуванням
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
// Автоматично змінюємо http:// на ws:// для веб-сокетів
const WS_BASE_URL = API_BASE_URL.replace(/^http/, 'ws');

const deviceIcons: { [key: string]: React.ReactNode } = {
    COFFEE_MAKER: (
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-yellow-700">
            <path strokeLinecap="round" strokeLinejoin="round" d="M11.25 9v.75m0 3v.75m3.375-3v.75m0 3v.75M12 21.75c3.728 0 6.75-3.022 6.75-6.75V6.75A1.125 1.125 0 0 0 17.625 5.625h-11.25A1.125 1.125 0 0 0 5.25 6.75V15c0 3.728 3.022 6.75 6.75 6.75Z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9.75a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 2.25V4.5M9.75 3a3 3 0 0 0 0 3m4.5-3a3 3 0 0 1 0 3" />
        </svg>
    ),
    HEATED_FLOOR: (
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-red-500">
            <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 19.5H19.5" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 16.5V13.5C6 11.2909 7.79086 9.5 10 9.5H14C16.2091 9.5 18 11.2909 18 13.5V16.5" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M9 19.5V16.5M12 19.5V16.5M15 19.5V16.5" />
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9.5V6.5M10.5 6.5A1.5 1.5 0 1 1 10.5 3.5M13.5 6.5A1.5 1.5 0 1 0 13.5 3.5" />
        </svg>
    ),
    APPLIANCE: (
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-amber-600">
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 15c0 1.657-1.343 3-3 3H6c-1.657 0-3-1.343-3-3V6.75c0-.414.336-.75.75-.75H13.5l3.75 3.75V15c0 1.657-1.343 3-3 3" />
        </svg>
    ),
    LIGHT: <span className="text-xl">💡</span>,
    AC: <span className="text-xl">❄️</span>,
    TV: <span className="text-xl">📺</span>,
    STEREO: <span className="text-xl">🔊</span>,
    VACUUM: <span className="text-xl">🧹</span>,
    DISHWASHER: <span className="text-xl">🧼</span>,
    KETTLE: <span className="text-xl">🍵</span>,
    WINDOW: <span className="text-xl">🪟</span>,
    SECURITY_SENSOR: <span className="text-xl">🛡️</span>,
    MOTION_SENSOR: <span className="text-xl">🏃‍♂️</span>,
    DOOR_SENSOR: <span className="text-xl">🚪</span>,
    DEFAULT: <span className="text-xl">⚙️</span>,
};

export default function Page() {
    const [devices, setDevices] = useState<Device[]>([]);
    const [weather, setWeather] = useState<number | string>(0);
    const [time, setTime] = useState<string>("");
    const [aiAdvice, setAiAdvice] = useState<string>('Натисніть кнопку для старту WebSocket стрімінгу...');
    const [loadingAi, setLoadingAi] = useState<boolean>(false);
    const [isStreaming, setIsStreaming] = useState<boolean>(false);

    const wsRef = useRef<WebSocket | null>(null);
    const audioContextRef = useRef<AudioContext | null>(null);
    const processorRef = useRef<any>(null);
    const mediaStreamRef = useRef<MediaStream | null>(null);

    const isIntentionalStopRef = useRef<boolean>(false);

    // Refs для захисту від гонки та кешування Worklet
    const reconnectingRef = useRef<boolean>(false);
    const workletLoadedRef = useRef<boolean>(false);

    // Cleanup при анмаунті (якщо юзер пішов зі сторінки)
    useEffect(() => {
        fetchDevices();
        fetchWeather();
        const timer = setInterval(() => setTime(new Date().toLocaleTimeString('uk-UA')), 1000);

        return () => {
            clearInterval(timer);
            isIntentionalStopRef.current = true;
            stopStreaming();
        };
    }, []);

    const fetchDevices = async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/devices`);
            setDevices(await res.json());
        } catch (e) { console.error(e); }
    };

    const fetchWeather = async () => {
        try {
            const res = await fetch(`${API_BASE_URL}/api/weather`);
            setWeather(await res.json());
        } catch (error) { setWeather(7.3); }
    };

    // 🔥 ФІКС БАГА 1.1: Відправляємо реальний PUT-запит
    const toggleDevice = async (id: number) => {
        // 1. Оптимістичне оновлення UI (змінюємо кнопку відразу для чуйності)
        setDevices(prev => prev.map(d => d.id === id ? { ...d, status: d.status === 'ON' ? 'OFF' : 'ON' } : d));

        try {
            // 2. Відправляємо запит на сервер
            const response = await fetch(`${API_BASE_URL}/devices/${id}/toggle`, {
                method: 'PUT',
            });

            if (!response.ok) {
                throw new Error('Не вдалося перемкнути пристрій на сервері');
            }

            console.log(`Тумблер пристрою ${id} успішно натиснуто на сервері`);
        } catch (error) {
            console.error('Помилка перемикання:', error);
            // 3. Якщо сервер повернув помилку - відкочуємо тумблер в UI назад
            setDevices(prev => prev.map(d => d.id === id ? { ...d, status: d.status === 'ON' ? 'OFF' : 'ON' } : d));
        }
    };

    const handleLocalSliderChange = (id: number, value: number) => {
        setDevices(prev => prev.map(d => d.id === id ? { ...d, targetValue: value } : d));
    };

    // 🔥 ФІКС БАГА 1.1: Відправляємо повзунок на сервер
    const changeDeviceValue = async (id: number, value: number) => {
        // 1. Оптимістичне оновлення UI
        setDevices(prev => prev.map(d => d.id === id ? { ...d, targetValue: value } : d));

        try {
            // 2. Відправляємо запит на сервер (передаємо value як query-параметр)
            const response = await fetch(`${API_BASE_URL}/devices/${id}/value?value=${value}`, {
                method: 'PUT',
            });

            if (!response.ok) {
                throw new Error('Не вдалося змінити значення пристрою на сервері');
            }
            console.log(`API Call: Зміна значення пристрою ${id} на ${value} успішна`);
        } catch (error) {
            console.error('Помилка зміни значення:', error);
            // Тут можна додати відкат, але зазвичай для повзунка це не так критично
        }
    };

    const toggleStreaming = () => {
        if (isStreaming) {
            isIntentionalStopRef.current = true;
            stopStreaming();
        } else {
            isIntentionalStopRef.current = false;
            startStreaming();
        }
    };

    const startStreaming = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaStreamRef.current = stream;

            const ws = new WebSocket(`${WS_BASE_URL}/audio-stream`);
            wsRef.current = ws;

            ws.onopen = () => {
                setIsStreaming(true);
                reconnectingRef.current = false; // Скидаємо прапорець реконнекту при успіху
                setAiAdvice("✅ З'єднання встановлено. Говоріть 'Джарвіс' у будь-який момент!");
            };

            ws.onmessage = (event) => {
                if (typeof event.data === 'string') {
                    // 1. Команда виконана (Джарвіс відповідає голосом)
                    if (event.data.startsWith('JARVIS_REPLY:')) {
                        const reply = event.data.replace('JARVIS_REPLY:', '');
                        speakJarvis(reply);
                        setAiAdvice(`🧠 Джарвіс: ${reply}`);
                        fetchDevices(); // Оновлюємо кнопки
                    }
                    // 2. Живі субтитри (ти ще говориш)
                    else if (event.data.startsWith('JARVIS_PARTIAL:')) {
                        const partialText = event.data.replace('JARVIS_PARTIAL:', '');
                        setAiAdvice(`✍️ Джарвіс слухає: ${partialText}...`);
                    }
                    // 3. Фінальний текст (ти закінчив говорити)
                    else if (event.data.startsWith('JARVIS_FINAL:')) {
                        const finalText = event.data.replace('JARVIS_FINAL:', '');
                        setAiAdvice(`✅ Почув: ${finalText}`);
                    }
                }
            };

            ws.onerror = () => {
                setAiAdvice("❌ Помилка WebSocket з'єднання");
            };

            // Фікс гонки реконнектів
            ws.onclose = () => {
                if (!isIntentionalStopRef.current) {
                    stopStreaming();
                    if (!reconnectingRef.current) {
                        reconnectingRef.current = true;
                        setAiAdvice("З'єднання втрачено. Перепідключення...");
                        setTimeout(() => {
                            reconnectingRef.current = false;
                            if (!isIntentionalStopRef.current) startStreaming();
                        }, 1500);
                    }
                } else {
                    stopStreaming();
                }
            };

            const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
            const audioContext = new AudioContextClass({ sampleRate: 16000 });
            audioContextRef.current = audioContext;

            // Кешуємо завантаження Worklet (щоб не вантажити двічі)
            try {
                if (!workletLoadedRef.current) {
                    await audioContext.audioWorklet.addModule('/audio-processor.js');
                    workletLoadedRef.current = true;
                }
            } catch (err) {
                console.error("AudioWorklet load failed", err);
                setAiAdvice("❌ Критична помилка: файл /audio-processor.js не знайдено!");
                return;
            }

            const workletNode = new window.AudioWorkletNode(audioContext, 'audio-processor');
            processorRef.current = workletNode;

            const source = audioContext.createMediaStreamSource(stream);
            const gainNode = audioContext.createGain();
            gainNode.gain.value = 0;

            source.connect(workletNode);
            workletNode.connect(gainNode);
            gainNode.connect(audioContext.destination);

            workletNode.port.onmessage = (event) => {
                if (ws.readyState === WebSocket.OPEN) {
                    // BACKPRESSURE: Якщо сервер або мережа тупить, не забиваємо пам'ять браузера
                    // Якщо в черзі на відправку більше 50 КБ даних (~1.5 секунди звуку) - пропускаємо кадри
                    if (ws.bufferedAmount > 50000) {
                        console.warn("Мережа перевантажена. Пропускаємо аудіо-кадр.");
                        return;
                    }
                    ws.send(event.data);
                }
            };

        } catch (error) {
            console.error(error);
            setAiAdvice("❌ Помилка доступу до мікрофона або сервера.");
            stopStreaming();
        }
    };

    const stopStreaming = () => {
        if (wsRef.current) {
            wsRef.current.onclose = null;
            wsRef.current.close();
            wsRef.current = null;
        }
        if (processorRef.current) {
            processorRef.current.disconnect();
            processorRef.current = null;
        }
        if (audioContextRef.current) {
            audioContextRef.current.close();
            audioContextRef.current = null;
        }
        if (mediaStreamRef.current) {
            mediaStreamRef.current.getTracks().forEach(track => track.stop());
            mediaStreamRef.current = null;
        }
        setIsStreaming(false);
    };

    const handleAskAI = async () => {
        setLoadingAi(true);
        try {
            const res = await fetch(`${API_BASE_URL}/ai/recommendation`, { method: 'POST' });
            const data = await res.json();
            speakJarvis(data.advice);
            setAiAdvice(`🧠 Джарвіс: ${data.advice}`);
        } catch (e) {
            setAiAdvice("ШІ недоступний.");
        } finally {
            setLoadingAi(false);
        }
    };

    return (
        <main className="min-h-screen bg-gray-50 p-4 md:p-8 text-gray-800">
            <div className="max-w-[1500px] mx-auto space-y-6">
                <header className="relative w-full md:w-[85%] lg:w-[75%] mx-auto grid grid-cols-1 md:grid-cols-3 items-center bg-white p-6 md:py-8 rounded-3xl shadow-sm border border-gray-100 overflow-hidden shrink-0">
                    <div className="absolute inset-0 z-0 flex flex-col items-center justify-center pointer-events-none select-none opacity-[0.05]">
                        <p className="text-[50px] md:text-[80px] font-black text-indigo-900 uppercase leading-[0.85] whitespace-nowrap">Smart Security</p>
                        <p className="text-[30px] md:text-[50px] font-bold text-indigo-900 uppercase leading-[0.85] mt-1">Life is easier with us</p>
                    </div>
                    <div className="hidden md:block w-full"></div>
                    <div className="relative z-10 flex flex-col items-center text-center w-full">
                        <h1 className="text-4xl md:text-5xl font-black text-gray-900 tracking-tight mb-2">Smart Home AI 🏠</h1>
                        <p className="text-gray-500 uppercase text-[10px] md:text-xs tracking-[0.2em] font-bold">
                            Панель керування (Всього: {devices.length})
                        </p>
                    </div>
                    <div className="relative z-10 flex flex-col items-center md:items-end justify-center w-full mt-4 md:mt-0">
                        <span className="text-sm text-gray-400 font-semibold uppercase">погода</span>
                        <div className="text-4xl font-bold text-blue-600">{weather} °C</div>
                        <div className="text-lg font-medium text-gray-500 mt-2 bg-gray-100 px-3 py-1 rounded-full">{time || "00:00:00"}</div>
                    </div>
                </header>

                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 grid-flow-dense">
                    <section className="bg-gradient-to-br from-indigo-600 to-purple-700 p-6 rounded-3xl shadow-lg text-white flex flex-col xl:col-start-4 xl:row-span-3 lg:col-start-3 lg:row-span-3">
                        <div className="flex justify-between items-start mb-4 border-b border-white/20 pb-4">
                            <div>
                                <h2 className="text-xl font-bold">Джарвіс 🧠</h2>
                                <p className="opacity-80 mt-1 text-xs">WebSocket Стрімінг</p>
                            </div>
                            <button onClick={toggleStreaming} className={`p-3 rounded-xl ${isStreaming ? 'bg-red-500 animate-pulse shadow-[0_0_15px_rgba(239,68,68,0.6)]' : 'bg-white text-purple-700'}`}>
                                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-6 h-6">
                                    {isStreaming ? (
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M5.25 7.5A2.25 2.25 0 017.5 5.25h9a2.25 2.25 0 012.25 2.25v9a2.25 2.25 0 01-2.25 2.25h-9a2.25 2.25 0 01-2.25-2.25v-9z" />
                                    ) : (
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M12 18.75a6 6 0 0 0 6-6v-1.5m-6 7.5a6 6 0 0 1-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 0 1-3-3V4.5a3 3 0 1 1 6 0v8.25a3 3 0 0 1-3 3Z" />
                                    )}
                                </svg>
                            </button>
                        </div>
                        <div className="space-y-4 flex-1 flex flex-col justify-center">
                            <div className="bg-white/20 p-4 rounded-xl min-h-[100px] text-xs flex items-center justify-center text-center font-medium leading-relaxed">{aiAdvice}</div>
                        </div>
                        <button onClick={handleAskAI} className="bg-white text-purple-700 w-full py-3 rounded-xl font-extrabold mt-4 uppercase text-xs">
                            {loadingAi ? "Обробка..." : "Порада ШІ"}
                        </button>
                    </section>

                    {devices.map((device) => (
                        <div key={device.id} className="p-3 border border-gray-100 rounded-2xl bg-white/90 backdrop-blur-sm hover:border-purple-300 hover:shadow-md hover:scale-[1.02] transition-all flex flex-col justify-between shadow-sm relative overflow-hidden h-[115px]">
                            <div className="flex justify-between items-start gap-2">
                                <div className="flex items-center gap-3 overflow-hidden">
                                    <div className="w-9 h-9 flex-none flex items-center justify-center bg-gray-50 rounded-xl border border-gray-100 text-gray-600 [&_svg]:w-5 [&_svg]:h-5 shrink-0">
                                        {deviceIcons[device.type] || deviceIcons['DEFAULT']}
                                    </div>
                                    <div className="min-w-0">
                                        <h3 className="font-bold text-gray-900 truncate text-xs" title={device.name}>{device.name}</h3>
                                        <div className="flex gap-1.5 mt-1">
                                            <span className="text-[8px] text-blue-600 font-bold px-1.5 py-0.5 bg-blue-50 rounded uppercase">{device.room || 'Дім'}</span>
                                            <span className="text-[8px] text-gray-400 font-mono px-1.5 py-0.5 bg-gray-50 rounded uppercase">{device.type}</span>
                                        </div>
                                    </div>
                                </div>
                                <div
                                    onClick={() => toggleDevice(device.id)}
                                    className={`w-10 h-5.5 flex-none rounded-full cursor-pointer relative transition-colors mt-1 shrink-0 ${device.status === 'ON' ? 'bg-green-500' : 'bg-gray-200'}`}
                                >
                                    <div className={`w-5.5 h-5.5 bg-white rounded-full border border-gray-100 absolute top-0 shadow-sm transition-transform ${device.status === 'ON' ? 'translate-x-4.5' : 'left-0'}`}></div>
                                </div>
                            </div>

                            <div className="h-8 flex items-end w-full">
                                {['TV', 'AC', 'LIGHT', 'STEREO', 'HEATED_FLOOR'].includes(device.type) && device.status === 'ON' ? (
                                    <div className="w-full pb-1">
                                        <div className="flex justify-between text-[8px] text-gray-400 mb-1 font-bold">
                                            <span>РІВЕНЬ</span>
                                            <span className="text-purple-600">{device.targetValue || 0}%</span>
                                        </div>
                                        <input
                                            type="range" min="0" max="100"
                                            value={device.targetValue || 0}
                                            onChange={(e) => handleLocalSliderChange(device.id, parseInt(e.target.value))}
                                            onMouseUp={(e) => changeDeviceValue(device.id, parseInt((e.target as HTMLInputElement).value))}
                                            className="w-full h-1 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-purple-600"
                                        />
                                    </div>
                                ) : (
                                    <div className="h-1"></div>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </main>
    );
}