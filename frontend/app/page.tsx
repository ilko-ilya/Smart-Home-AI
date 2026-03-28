'use client';

import { useState, useEffect } from 'react';
import type { Device } from './types';
import { speakJarvis } from '../utils/jarvisVoice';

// --- ХЕЛПЕР ДЛЯ ИКОНОК ---
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
  const [transcript, setTranscript] = useState<string>('');
  const [aiAdvice, setAiAdvice] = useState<string>('Просто скажите команду...');
  const [loadingAi, setLoadingAi] = useState<boolean>(false);
  const [isListening, setIsListening] = useState<boolean>(false);

  useEffect(() => {
    fetchDevices();
    fetchWeather();

    // Часы
    const timer = setInterval(() => {
      setTime(new Date().toLocaleTimeString('uk-UA'));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const fetchDevices = async () => {
    try {
      const res = await fetch('http://localhost:8080/devices');
      const data = await res.json();
      setDevices(data);
    } catch (e) { console.error("Ошибка девайсов:", e); }
  };

  const fetchWeather = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/weather');
      const data = await res.json();
      setWeather(data);
    } catch (error) {
      setWeather(7.3);
    }
  };

  const toggleDevice = async (id: number) => {
    setDevices(prev => prev.map(d => d.id === id ? { ...d, status: d.status === 'ON' ? 'OFF' : 'ON' } : d));
  };

  const handleLocalSliderChange = (id: number, value: number) => {
    setDevices(prev => prev.map(d => d.id === id ? { ...d, targetValue: value } : d));
  };

  const changeDeviceValue = async (id: number, value: number) => {
    console.log(`API Call: Change device ${id} value to ${value}`);
  };

  const startListening = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) return;

    const recognition = new SpeechRecognition();
    recognition.lang = 'uk-UA'; // Українська мова розпізнавання
    recognition.onstart = () => setIsListening(true);
    recognition.onend = () => setIsListening(false);
    recognition.onresult = (event: any) => {
      const text = event.results[0][0].transcript;
      setTranscript(text);
      sendVoiceCommandToAi(text);
    };
    recognition.start();
  };

  const sendVoiceCommandToAi = async (text: string) => {
    setLoadingAi(true);
    try {
      const res = await fetch('http://localhost:8080/ai/voice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text })
      });
      const data = await res.text();
      speakJarvis(data);
      setAiAdvice(`🎙️ Джарвис: ${data}`);
      fetchDevices();
    } catch (e) {
      setAiAdvice("Джарвис недоступен.");
    } finally {
      setLoadingAi(false);
    }
  };

  const handleAskAI = async () => {
    setLoadingAi(true);
    try {
      const res = await fetch('http://localhost:8080/ai/recommendation', { method: 'POST' });
      const data = await res.json();
      speakJarvis(data.advice);
      setAiAdvice(`🧠 Джарвис: ${data.advice}`);
    } catch (e) {
      setAiAdvice("AI недоступен.");
    } finally {
      setLoadingAi(false);
    }
  };

  return (
    <main className="min-h-screen bg-gray-50 p-4 md:p-8 text-gray-800">
      <div className="max-w-[1500px] mx-auto space-y-6">

        {/* === ЗАГОЛОВОК === */}
        <header className="relative w-full md:w-[85%] lg:w-[75%] mx-auto grid grid-cols-1 md:grid-cols-3 items-center bg-white p-6 md:py-8 rounded-3xl shadow-sm border border-gray-100 overflow-hidden shrink-0">
          <div className="absolute inset-0 z-0 flex flex-col items-center justify-center pointer-events-none select-none opacity-[0.05]">
            <p className="text-[50px] md:text-[80px] font-black text-indigo-900 uppercase leading-[0.85] whitespace-nowrap">Smart Security</p>
            <p className="text-[30px] md:text-[50px] font-bold text-indigo-900 uppercase leading-[0.85] mt-1">Life is easier with us</p>
          </div>

          <div className="hidden md:block w-full"></div>

          <div className="relative z-10 flex flex-col items-center text-center w-full">
            <h1 className="text-4xl md:text-5xl font-black text-gray-900 tracking-tight mb-2">Smart Home AI 🏠</h1>
            <p className="text-gray-500 uppercase text-[10px] md:text-xs tracking-[0.2em] font-bold">
              Панель управления (Всего: {devices.length})
            </p>
          </div>

          <div className="relative z-10 flex flex-col items-center md:items-end justify-center w-full mt-4 md:mt-0">
            <span className="text-sm text-gray-400 font-semibold uppercase">погода</span>
            <div className="text-4xl font-bold text-blue-600">{weather} °C</div>
            <div className="text-lg font-medium text-gray-500 mt-2 bg-gray-100 px-3 py-1 rounded-full">{time || "00:00:00"}</div>
          </div>
        </header>

        {/* === ГЕТКА (ОРИГИНАЛЬНАЯ С grid-flow-dense) === */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 grid-flow-dense">

          {/* ДЖАРВИС */}
          <section className="bg-gradient-to-br from-indigo-600 to-purple-700 p-6 rounded-3xl shadow-lg text-white flex flex-col xl:col-start-4 xl:row-span-3 lg:col-start-3 lg:row-span-3">
            <div className="flex justify-between items-start mb-4 border-b border-white/20 pb-4">
              <div>
                <h2 className="text-xl font-bold">Джарвис 🧠</h2>
                <p className="opacity-80 mt-1 text-xs">Жду команд...</p>
              </div>
              <button onClick={startListening} className={`p-3 rounded-xl ${isListening ? 'bg-red-500 animate-pulse' : 'bg-white text-purple-700'}`}>
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-6 h-6">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 18.75a6 6 0 0 0 6-6v-1.5m-6 7.5a6 6 0 0 1-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 0 1-3-3V4.5a3 3 0 1 1 6 0v8.25a3 3 0 0 1-3 3Z" />
                </svg>
              </button>
            </div>
            <div className="space-y-4 flex-1 flex flex-col justify-center">
              {transcript && <div className="bg-white/10 p-3 rounded-xl text-sm italic">🗣️ «{transcript}»</div>}
              <div className="bg-white/20 p-4 rounded-xl min-h-[100px] text-xs flex items-center justify-center text-center">{aiAdvice}</div>
            </div>
            <button onClick={handleAskAI} className="bg-white text-purple-700 w-full py-3 rounded-xl font-extrabold mt-4 uppercase text-xs">
              {loadingAi ? "Анализируем..." : "Совет AI"}
            </button>
          </section>

          {/* ДЕВАЙСЫ (ОРИГИНАЛЬНЫЙ ДИЗАЙН С БЛЮРОМ И ТЕГАМИ) */}
          {devices.map((device) => (
             <div key={device.id} className="p-3 border border-gray-100 rounded-2xl bg-white/90 backdrop-blur-sm hover:border-purple-300 hover:shadow-md hover:scale-[1.02] transition-all flex flex-col justify-between shadow-sm relative overflow-hidden h-[115px]">
                 {/* ВЕРХ: Иконка + Текст + Тумблер */}
                 <div className="flex justify-between items-start gap-2">
                   <div className="flex items-center gap-3 overflow-hidden">
                      <div className="w-9 h-9 flex-none flex items-center justify-center bg-gray-50 rounded-xl border border-gray-100 text-gray-600 [&_svg]:w-5 [&_svg]:h-5 shrink-0">
                         {deviceIcons[device.type] || deviceIcons['DEFAULT']}
                      </div>
                      <div className="min-w-0">
                        <h3 className="font-bold text-gray-900 truncate text-xs" title={device.name}>{device.name}</h3>
                        <div className="flex gap-1.5 mt-1">
                          <span className="text-[8px] text-blue-600 font-bold px-1.5 py-0.5 bg-blue-50 rounded uppercase">{device.room || 'Дом'}</span>
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

                 {/* НИЗ: Ползунок */}
                 <div className="h-8 flex items-end w-full">
                    {['TV', 'AC', 'LIGHT', 'STEREO', 'HEATED_FLOOR'].includes(device.type) && device.status === 'ON' ? (
                      <div className="w-full pb-1">
                        <div className="flex justify-between text-[8px] text-gray-400 mb-1 font-bold">
                          <span>УРОВЕНЬ</span>
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