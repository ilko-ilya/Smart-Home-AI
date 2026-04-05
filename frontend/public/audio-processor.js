class AudioProcessor extends AudioWorkletProcessor {
  constructor() {
    super();
    this.chunkSize = 4096; // Накопичуємо по 256мс звуку, як і було задумано архітектурою
    this.buffer = new Float32Array(this.chunkSize);
    this.pointer = 0;
  }

  process(inputs, outputs, parameters) {
    const input = inputs[0];

    if (input && input.length > 0) {
      const channelData = input[0];

      // Записуємо мікро-шматочки (по 128 семплів) у наш великий буфер
      for (let i = 0; i < channelData.length; i++) {
        this.buffer[this.pointer++] = channelData[i];

        // Як тільки зібрали 4096 семплів (256 мс) - конвертуємо і відправляємо!
        if (this.pointer >= this.chunkSize) {
          const int16Array = new Int16Array(this.chunkSize);
          for (let j = 0; j < this.chunkSize; j++) {
            int16Array[j] = Math.max(-1, Math.min(1, this.buffer[j])) * 0x7FFF;
          }

          this.port.postMessage(int16Array.buffer, [int16Array.buffer]);
          this.pointer = 0; // Скидаємо вказівник для наступної порції
        }
      }
    }

    return true;
  }
}

registerProcessor('audio-processor', AudioProcessor);