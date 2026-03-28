// utils/jarvisVoice.ts

export const speakJarvis = async (text: string) => {

  const CARTESIA_API_KEY = "sk_car_keLxJRL7JPYn1KEXpbFdMA";

  // Голос Oleh (Professional Guy):
  const VOICE_ID = "05ffab9c-d380-4909-8375-cd12f59238c3";

  try {
    const response = await fetch("https://api.cartesia.ai/tts/bytes", {
      method: "POST",
      headers: {
        "Cartesia-Version": "2024-06-10",
        "X-API-Key": CARTESIA_API_KEY,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model_id: "sonic-3",
        transcript: text,
        language: "uk", // Українізація успішна!
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
      console.error("Ошибка Cartesia API:", errorData);
      return;
    }

    const audioBlob = await response.blob();
    const audioUrl = URL.createObjectURL(audioBlob);
    const audio = new Audio(audioUrl);

    // Важно: кликни по странице браузера перед тестом!
    await audio.play();

  } catch (error) {
    console.error("Ошибка при запросе к Cartesia:", error);
  }
};
