export const speakJarvis = async (text: string) => {
  try {
    // Тепер ми стукаємо на НАШ власний серверний маршрут
    const response = await fetch("/api/tts", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ text }),
    });

    if (!response.ok) {
      console.error("Помилка отримання аудіо від нашого API:", await response.text());
      return;
    }

    // Отримуємо mp3-файл і програємо його
    const audioBlob = await response.blob();
    const audioUrl = URL.createObjectURL(audioBlob);
    const audio = new Audio(audioUrl);

    // Важливо: для автовідтворення користувач має хоч раз клікнути по сторінці!
    await audio.play();

  } catch (error) {
    console.error("Помилка при відтворенні звуку Jarvis:", error);
  }
};