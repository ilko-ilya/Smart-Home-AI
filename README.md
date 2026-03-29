# 🏠 Smart Home AI (Jarvis)

AI-powered full-stack smart home system that combines **deterministic backend logic with LLM-based decision-making** using the Orchestrator pattern.

## 🚀 Key Highlights
- Hybrid AI + rule-based architecture for reliability
- Orchestrator pattern (centralized system coordination)
- Real-time voice interaction (Speech-to-Text & Text-to-Speech)
- Context-aware automation (devices, sensors, weather)

> 🎙️ **Voice-first experience:**
> Control your home using natural language. Jarvis understands voice commands (STT), executes actions, and responds with spoken feedback (TTS), including explanations and proactive recommendations.
![Smart Home Dashboard](./Animation.gif)

---

## 💡 Why This Project Matters (Engineering Challenges)

This project demonstrates solving real-world backend challenges:

- **AI as a System Component:** AI is used as a decision-making part of backend logic, not just a chatbot.
- **AI Response Parsing & Validation:** Structured JSON extraction and validation from LLM responses.
- **Data Orchestration:** Aggregation of data from the database (devices), sensors, and external APIs (weather).
- **Voice Control & Audio Feedback:** Real-time command processing with Web Speech API integration for a complete two-way audio interface.
- **Hybrid Architecture:** Combination of deterministic logic and probabilistic AI.

---

## 🧠 Architecture Overview

The backend follows the **Orchestrator pattern**:

- `JarvisService` acts as the central orchestrator.
- It coordinates:
  - `DeviceService`
  - `WeatherService`
  - `SensorDataService`
  - `AIClient`

### Flow:

1. User sends command (voice or text).
2. `JarvisService` aggregates system context.
3. AI processes request (if needed).
4. Backend executes actions.
5. Response is returned and **verbally spoken** by the UI.

👉 Clear separation of concerns + controlled AI usage.

---

## 🤖 How AI Works

- Backend sends structured prompts to LLM (Llama 3.1 via Groq).
- Model returns structured responses.
- Responses are validated before execution.
- If parsing fails → fallback logic is applied.

### Example AI response:

```json
{
  "action": "TURN_ON",
  "device": "LIGHT",
  "room": "KITCHEN"
}
```

---

## ⚙️ Hybrid Approach (AI + Rule-based)

To ensure reliability:

- **Critical scenarios** ("I'm home", "I'm leaving")  
  → handled by backend logic (NO AI).

- **Flexible commands** ("make it comfortable", "turn something on")  
  → handled by AI.

💡 This reduces dependency on external AI APIs and improves stability.

---

## 🏡 Smart Automation Scenarios

- **"I'm leaving":**
  - security system ON
  - vacuum ON
  - all other devices OFF

- **"I'm home":**
  - security system OFF
  - hallway light ON
  - music ON (20% volume)

---

## 🗣️ AI Assistant (Jarvis)

- **LLM:** Llama 3.1 (Groq API)
- **Voice interaction:** Web Speech API (STT & TTS)

**Jarvis:**
- executes commands
- explains actions verbally (Voice Synthesis)
- gives proactive recommendations

**Context used for analysis:**
- outside temperature
- indoor climate
- active devices

---

## 🔗 API Examples

### POST /ai/voice

```json
{
  "text": "turn on the kitchen light"
}
```

Response:

```text
Done, sir. The light is turned on.
```

---

### POST /ai/recommendation

Response:

```json
{
  "advice": "Sir, it's quite chilly outside. I recommend turning on the heated floor."
}
```

---

## 🧱 Tech Stack

### Backend
- Java 21
- Spring Boot 4.0.4
- Spring Data JPA
- PostgreSQL

### Frontend
- Next.js
- React
- TypeScript
- Tailwind CSS

### AI & Voice
- Llama 3.1 (Groq API)
- Web Speech API (STT/TTS)

### DevOps
- Docker
- Docker Compose

---

## 🐳 Infrastructure

The system is fully containerized:

- Backend (Spring Boot)
- Frontend (Next.js)
- PostgreSQL

All services run seamlessly via Docker Compose.

---

## 📂 Project Structure

```text
smart-home-AI/
├── backend/
│   ├── src/main/java/...
│   ├── docker-compose.yml
│   └── .env.example
├── frontend/
├── README.md
└── Animation.gif
```

---

## ⚙️ Run the Project

### 1. Clone repository

```bash
git clone https://github.com/ilko-ilya/smart-home-ai.git
cd smart-home-AI
```

---

### 2. Configure environment

```text
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
GROQ_API_KEY=your_api_key
```

---

### 3. Run with Docker

```bash
cd backend
docker-compose up -d --build
```

---

### 4. Open in browser

```text
http://localhost:3000
```

---

## 🚀 Summary

This project demonstrates:

- Full-stack development
- AI integration into backend workflows
- Two-way Voice control & synthesis (Speech-to-Text & Text-to-Speech)
- System design (Orchestrator pattern)
- Containerized infrastructure

---

## 👨‍💻 Author

**Ilya Samilyak** Java Developer