'use client';

import { useState, useEffect } from 'react';
import type { Device, WeatherData } from './types'; // Убедись, что типы импортированы!

// --- ХЕЛПЕР ДЛЯ ИКОНОК (SVG Inline для скорости) ---
const deviceIcons: { [key: string]: React.ReactNode } = {
  // Кофеварка со стимом
  COFFEE_MAKER: (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-yellow-700">
      <path strokeLinecap="round" strokeLinejoin="round" d="M11.25 9v.75m0 3v.75m3.375-3v.75m0 3v.75M12 21.75c3.728 0 6.75-3.022 6.75-6.75V6.75A1.125 1.125 0 0 0 17.625 5.625h-11.25A1.125 1.125 0 0 0 5.25 6.75V15c0 3.728 3.022 6.75 6.75 6.75Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 9.75a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 2.25V4.5M9.75 3a3 3 0 0 0 0 3m4.5-3a3 3 0 0 1 0 3" />
    </svg>
  ),
  // Теплый пол (Волны тепла снизу)
  HEATED_FLOOR: (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-red-500">
      <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 19.5H19.5" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M6 16.5V13.5C6 11.2909 7.79086 9.5 10 9.5H14C16.2091 9.5 18 11.2909 18 13.5V16.5" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 19.5V16.5M12 19.5V16.5M15 19.5V16.5" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 9.5V6.5M10.5 6.5A1.5 1.5 0 1 1 10.5 3.5M13.5 6.5A1.5 1.5 0 1 0 13.5 3.5" />
    </svg>
  ),
  // Утюг (Appliance)
  APPLIANCE: (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-6 h-6 text-amber-600">
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 15c0 1.657-1.343 3-3 3H6c-1.657 0-3-1.343-3-3V6.75c0-.414.336-.75.75-.75H13.5l3.75 3.75V15c0 1.657-1.343 3-3 3" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 18H6a3 3 0 0 1-3-3V6.75c0-.414.336-.75.75-.75H13.5l3.75 3.75V15a3 3 0 0 1-3 3H12Zm0 0v-4.5" />
    </svg>
  ),
  // Остальные девайсы...
  LIGHT: '💡',
  AC: '❄️',
  TV: '📺',
  STEREO: '🔊',
  VACUUM: '🧹',
  DISHWASHER: '🧼',
  KETTLE: '🍵',
  WINDOW: '🪟',
  SECURITY_SENSOR: '🛡️',
  'DEFAULT': '⚙️',
  MOTION_SENSOR: '🏃‍♂️', // или '📡'
  DOOR_SENSOR: '🚪',
};

export default function Page() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [weather, setWeather] = useState<number>(0);
  const [transcript, setTranscript] = useState<string>('');
  const [aiAdvice, setAiAdvice] = useState<string>('Просто скажите команду...');
  const [loadingAi, setLoadingAi] = useState<boolean>(false);
  const [isListening, setIsListening] = useState<boolean>(false);

  useEffect(() => {
    fetchDevices();
    fetchWeather();
  }, []);

  const fetchDevices = async () => {
    const res = await fetch('http://localhost:8080/devices');
    const data = await res.json();
    setDevices(data);
  };

  const fetchWeather = async () => {
    // В реальном API здесь был бы fetch
    setWeather(7.3);
  };

  const toggleDevice = async (id: number) => {
    // В реальном API здесь был бы fetch(POST /ai/command)
    setDevices(devices.map(d => d.id === id ? { ...d, status: d.status === 'ON' ? 'OFF' : 'ON' } : d));
  };

  const handleLocalSliderChange = (id: number, value: number) => {
    setDevices(devices.map(d => d.id === id ? { ...d, targetValue: value } : d));
  };

  const changeDeviceValue = async (id: number, value: number) => {
    // В реальном API здесь был бы fetch(POST /devices/{id}/value)
    console.log(`API Call: Change device ${id} value to ${value}`);  };

  // --- ГОЛОСОВОЙ ИНТЕРФЕЙС (Web Speech API) ---
  const startListening = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert("Сэр, ваш браузер не поддерживает распознавание речи.");
      return;
    }

    const recognition = new SpeechRecognition();
    recognition.lang = 'ru-RU';
    recognition.continuous = false;
    recognition.interimResults = false;

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
      // Это реальный вызов JARVIS_SERVICE_JAVA
      const res = await fetch('http://localhost:8080/ai/voice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text })
      });
      const data = await res.text(); // Бэкенд возвращает текст: "Сделано, сэр."
      setAiAdvice(`🎙️ Джарвис: ${data}`);
      fetchDevices(); // Обновляем статусы в реальном времени!
    } catch (e) {
      setAiAdvice("Джарвис временно недоступен.");
    } finally {
      setLoadingAi(false);
    }
  };

  const handleAskAI = async () => {
    setLoadingAi(true);
    try {
      // Реальный вызов AI_SERVICE_JAVA
      const res = await fetch('http://localhost:8080/ai/recommendation', { method: 'POST' });
      const data = await res.json(); // Ожидаем JSON {advice: "..."}
      setAiAdvice(`🧠 Джарвис (Анализ среды): ${data.advice}`);
    } catch (e) {
      setAiAdvice("AI временно недоступен.");
    } finally {
      setLoadingAi(false);
    }
  };

 return (
     <main className="min-h-screen bg-gray-50 p-4 md:p-8 text-gray-800">
       <div className="max-w-[1500px] mx-auto space-y-6">

         {/* === ЗАГОЛОВОК (ОТЦЕНТРОВАННЫЙ И УВЕЛИЧЕННЫЙ) === */}
                 <header className="relative w-full md:w-[85%] lg:w-[75%] mx-auto flex items-center bg-white p-6 md:py-8 rounded-3xl shadow-sm border border-gray-100 overflow-hidden shrink-0">

                   {/* ФОНОВЫЙ ТЕКСТ (Водяной знак) */}
                   <div className="absolute inset-0 z-0 flex flex-col items-center justify-center pointer-events-none select-none opacity-[0.05]">
                     <p className="text-[50px] md:text-[80px] font-black text-indigo-900 uppercase leading-[0.85] whitespace-nowrap">
                       Smart Security
                     </p>
                     <p className="text-[30px] md:text-[50px] font-bold text-indigo-900 uppercase leading-[0.85] whitespace-nowrap mt-1">
                       Life is easier with us
                     </p>
                   </div>

                   {/* Левый пустой блок (нужен для идеальной центровки) */}
                   <div className="relative z-10 flex-1 hidden sm:block"></div>

                   {/* КОНТЕНТ ЗАГОЛОВКА (СТРОГО ПО ЦЕНТРУ) */}
                   <div className="relative z-10 flex-1 flex flex-col items-center text-center whitespace-nowrap">
                     <h1 className="text-4xl md:text-5xl font-black text-gray-900 tracking-tight mb-2 drop-shadow-sm">
                       Smart Home AI 🏠
                     </h1>
                     <p className="text-gray-500 uppercase text-[10px] md:text-xs tracking-[0.2em] font-bold">
                       Панель управления (Всего: {devices.length})
                     </p>
                   </div>

                   {/* БЛОК ПОГОДЫ (ПРИЖАТ ВПРАВО) */}
                   <div className="relative z-10 flex-1 text-right">
                     <p className="text-[10px] md:text-xs text-gray-400 font-bold uppercase tracking-widest mb-1">Погода</p>
                     <p className="text-4xl md:text-5xl font-black text-blue-600">{weather} °C</p>
                   </div>
                 </header>

         {/* === ГЛАВНАЯ СЕТКА === */}
         <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 grid-flow-dense">

           {/* 1. БЛОК ИИ (ДЖАРВИС СПРАВА) */}
           <section className="bg-gradient-to-br from-indigo-600 to-purple-700 p-6 rounded-3xl shadow-lg text-white flex flex-col xl:col-start-4 xl:row-span-3 lg:col-start-3 lg:row-span-3">
             <div className="flex justify-between items-start mb-4 border-b border-white/20 pb-4">
               <div>
                 <h2 className="text-xl font-bold">Джарвис 🧠</h2>
                 <p className="opacity-80 mt-1 text-xs">Жду команд, сэр...</p>
               </div>
               <button
                 onClick={startListening}
                 disabled={loadingAi}
                 className={`p-3 rounded-xl shadow-2xl transition transform hover:scale-105 ${isListening ? 'bg-red-500 animate-pulse' : 'bg-white text-purple-700 disabled:opacity-50'}`}
                 title="Голосовая команда"
               >
                 <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={2.5} stroke="currentColor" className="w-6 h-6">
                   <path strokeLinecap="round" strokeLinejoin="round" d="M12 18.75a6 6 0 0 0 6-6v-1.5m-6 7.5a6 6 0 0 1-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 0 1-3-3V4.5a3 3 0 1 1 6 0v8.25a3 3 0 0 1-3 3Z" />
                 </svg>
               </button>
             </div>

             <div className="space-y-4 flex-1 flex flex-col justify-center">
                {transcript && (
                  <div className="bg-white/10 p-3 rounded-xl text-sm font-semibold border border-white/20 shadow-inner">
                    🗣️ «{transcript}»
                  </div>
                )}
                <div className="bg-white/20 p-4 rounded-xl min-h-[100px] italic text-xs leading-relaxed border border-white/10 shadow-inner flex items-center justify-center text-center">
                  {aiAdvice}
                </div>
             </div>

             <button
               onClick={handleAskAI}
               disabled={loadingAi || isListening}
               className="bg-white text-purple-700 w-full py-3 rounded-xl font-extrabold shadow-xl hover:bg-gray-50 transition transform hover:-translate-y-0.5 disabled:opacity-70 disabled:cursor-not-allowed mt-4 uppercase tracking-wider text-xs"
             >
               {loadingAi ? "Анализируем..." : "Совет AI"}
             </button>
           </section>

           {/* 2. УЛЬТРА-КОМПАКТНЫЕ КАРТОЧКИ УСТРОЙСТВ */}
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
                      <div className="w-full animate-fade-in pb-1">
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