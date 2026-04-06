# 🏠 Smart Home AI (Jarvis)

An enterprise-grade smart home system that combines deterministic backend logic with an Agentic AI workflow.  
This is not just a chatbot — it is a real AI agent capable of controlling devices, searching the web, and processing voice commands in real time.

**🔐 Secure by Design:** API keys are *never* exposed to the client. All third-party integrations (LLM, TTS, Search) are handled securely via backend or Next.js server-side routes (BFF pattern).

---

> 🎙️ **Voice-First Experience:**
> Stream your voice via WebSockets directly to the backend. Jarvis uses Whisper STT to understand context, executes agentic loops (like searching the web for current weather), and responds with spoken feedback via TTS.

![Smart Home Dashboard](./Animation.gif)

---

## 🚀 Key Highlights

- 🤖 **Agentic AI Workflow** — LLM can call tools and execute backend logic
- 🎙️ **Full Voice Pipeline** — Speech-to-Text (Whisper) + Text-to-Speech (Cartesia)
- 🧠 **Hybrid Intelligence** — rule-based + AI for maximum reliability
- ⚡ **Fast & Cheap** — optimized for near-zero cost using free-tier APIs
- 🧱 **Enterprise Backend Design** — DTO isolation, transactions, validation

---

## 🧭 System Flow

```text
User (Voice/Text)
        ↓
Whisper STT
        ↓
JarvisService (Orchestrator)
        ↓
LLM (Groq - Llama 3.3)
   ↙              ↘
Tool Calls       Direct Response
   ↓
Backend Logic (DB / Search APIs)
        ↓
LLM Final Response
        ↓
TTS (Cartesia via Next.js BFF)
        ↓
User hears response 🎧
```

---

## 🔊 Text-to-Speech (TTS) Architecture

The system uses the **Cartesia API** for high-fidelity voice responses.

**🔒 Security (BFF Pattern)**
API keys are NEVER exposed to the client. We implemented a Backend-for-Frontend (BFF) approach:
`Frontend → Next.js API Route (/api/tts) → Cartesia API`

**📁 Implementation Details:**
* `frontend/app/api/tts/route.ts` (Server-side proxy)
* `frontend/utils/jarvisVoice.ts` (Client-side consumer)
* `.env.local` (Secure key storage, ignored by Git)

---

## 🛡️ Resilience & Fault Tolerance

* **Tavily → DuckDuckGo Fallback:** Automatic switch to HTML parsing if the primary search API fails or rate-limits (no downtime).
* **Defensive Execution:** Strict protection against empty AI responses, missing TTS text, and infinite AI tool-calling loops.
* **Smart Noise Filtering:** Whisper STT ignores background noise and hallucinations, activating strictly via the trigger word ("Jarvis").
* **Payload Optimization:** Search results are truncated to 1000 characters to save LLM tokens and reduce latency.
* **Planned Improvements:** Retry & timeout policies, Circuit Breaker pattern for external API calls.

*System is designed to never fail silently.*

---

## 🔐 Security Architecture

* **Zero Client Exposure:** All third-party API keys (Cartesia, Groq, Tavily) are strictly kept on the server.
* **Server-Side Proxy (BFF):** Used for TTS to prevent token leakage in the browser.
* **DTO Isolation:** Hides internal database structure (Entities) from the AI and the frontend client, transferring only necessary data.

---

## 🤖 How the Agentic AI Works

This system uses **Tool Calling (Function Calling)** instead of simple prompting.

**Example tool:**
```json
{
  "name": "control_devices",
  "arguments": {
    "actions": [
      { "deviceId": 1, "targetStatus": "ON" }
    ]
  }
}
```

**Flow:**
1. LLM decides what to do
2. Calls backend function
3. Backend executes logic (DB / API)
4. Result is sent back to LLM
5. LLM generates final response

---

## ⚙️ Hybrid Approach (AI + Rule-based)

**Critical Scenarios (NO AI):**
- "I'm leaving"
- "I'm home"

✔ Zero latency  
✔ Zero cost  
✔ 100% deterministic

**Flexible Commands (AI):**
- "Turn on TV and check the news"
- "What's the weather?"

✔ Context-aware  
✔ Flexible  
✔ Extensible

---

## 💰 Cost Optimization Strategy

- Uses free-tier APIs (Groq, Open-Meteo).
- Tavily limited to 990 requests/month with automatic DuckDuckGo scraper fallback.
- Scenario-based commands bypass LLM entirely.
- Fast-path execution for basic device control.

**Result:** Near-zero operational cost with production-ready efficiency.

---

## 🎯 User Experience (UX)

- **Trigger word activation:** System listens securely and activates only on "Jarvis".
- **Natural language commands:** Multi-language input supported (handles complex, unstructured sentences).
- **Instant feedback:** Real-time UI updates and immediate voice responses.

---

## ⚠️ Known Limitations

- **State Management:** Tavily API request counter is in-memory and resets on application restart.
- **Latency:** External APIs (Groq, Cartesia, Tavily) may introduce slight network-dependent latency.
- **Throttling:** No application-level rate limiting yet for TTS/LLM endpoints.

---

## 🧪 Testing

**Currently Implemented:**
- Manual End-to-End (E2E) testing (Voice → STT → Agentic Loop → Device execution → TTS).
- Web Search fallback validation.

**Planned:**
- Unit tests for core services (`JarvisService`, `ScenarioService`).
- Integration tests for the Agentic Workflow and Database transactions.

---

## ❗ What Makes This Project Different

This is **NOT** a simple chatbot wrapper.

✔ Real Agentic Loop  
✔ Backend-controlled logic (not prompt hacks)  
✔ State-aware system (devices, sensors, weather)  
✔ Voice-first architecture with premium Audio  
✔ Deterministic + AI hybrid

This system behaves as a true AI agent capable of decision-making, tool execution, and context-aware responses.

---

## 🧱 Tech Stack

**Backend:**
- Java 21
- Spring Boot (Web, Data JPA, Validation, WebSockets)
- PostgreSQL
- Jackson, Lombok

**Frontend:**
- Next.js (React) App Router
- TypeScript
- Tailwind CSS

**AI & Integrations:**
- Groq API (Llama 3.3)
- OpenAI Whisper (Speech-to-Text)
- Cartesia AI (Text-to-Speech)
- Tavily API (Search) / DuckDuckGo
- Open-Meteo (Weather)

---

## ⚙️ Run Locally

**1. Clone repository**
```bash
git clone https://github.com/ilko-ilya/smart-home-ai.git
cd smart-home-AI  
```

**2. Configure Backend environment (`backend/.env`)**
```env
POSTGRES_USER=postgres  
POSTGRES_PASSWORD=your_password  
POSTGRES_DB=smarthome  

GROQ_API_KEY=your_key  
TAVILY_API_KEY=your_key  

WEATHER_LAT=50.51  
WEATHER_LON=30.79  
```

**3. Configure Frontend environment (`frontend/.env.local`)**
```env
CARTESIA_API_KEY=your_cartesia_key
```

**4. Run with Docker**
```bash
cd backend  
docker-compose up -d --build  
```

**5. Open in browser**
```text
http://localhost:3000  
```

---

## 🔮 Future Improvements

- Add Redis caching for weather and AI responses.
- Implement per-user rate limiting for TTS and LLM endpoints.
- Persist Tavily usage counters (currently in-memory).
- Streaming AI text responses over WebSocket.

---

## 👨‍💻 Author

**Ilya Samilyak** | Java Developer