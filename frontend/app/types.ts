export interface Device {
  id: number;
  name: string;
  type: string;
  room?: string;
  status: 'ON' | 'OFF';
  targetValue?: number;
}

export interface WeatherData {
  temperature: number;
}