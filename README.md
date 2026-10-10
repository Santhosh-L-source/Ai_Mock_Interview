# 🎙️ AI Mock Interviewer Platform

An intelligent, enterprise-grade AI-powered Mock Interview and Proctoring platform built with **Spring Boot 3**, **LangChain4j**, **PostgreSQL**, **LiveKit WebRTC**, and **Deepgram STT**.

The platform conducts interactive technical and behavioral interviews, analyzes student performance in real time across 12 scoring parameters, tracks proctoring violations (e.g. mobile detection, tab switching, multiple faces), and provides administrative scheduling and performance analytics.

---

## 🌟 Key Features

### 1. 🤖 Adaptive AI Interviewer (LangChain4j & Groq / OpenAI)
- **Personalized Roadmaps**: Generates custom 5-stage interview roadmaps from candidate resumes or job roles.
- **Dynamic Question Generation**: Formulates follow-up questions in real-time based on the candidate's prior responses.
- **Multi-Parameter Evaluation**: Evaluates answers based on relevance, depth, accuracy, clarity, and communication skills.

### 2. 🗣️ Real-time Voice & Audio Pipeline
- **Speech-to-Text (STT)**: High-accuracy real-time speech transcription powered by Deepgram.
- **Text-to-Speech (TTS)**: Realistic audio synthesis using OpenAI TTS (`alloy` / `nova`).
- **Voice Orchestration**: Automatically handles question timeouts, transitions, and turn-by-turn conversational flow.

### 3. 📹 WebRTC & Live Proctoring (LiveKit)
- **Video & Audio Streaming**: WebRTC peer-to-peer room token generation and management using LiveKit Server SDK.
- **Malpractice Detection**: Real-time logging of proctoring events (e.g., mobile device detected, face out of frame, multiple faces, tab switching).

### 4. 📅 Admin Scheduling & Bulk Student Management
- **Campus Drives & Scheduled Interviews**: Create and manage scheduled interview sessions with time windows.
- **Batch Assignment**: Bulk assign interviews to students by department, year (e.g., 2nd Year, 3rd Year), or roll number.
- **Marks & Attendance Export**: One-click Excel sheet export with attendance status, question-wise breakdown, and final scores.

### 5. 📄 Resume Parsing & Candidate Profiling
- Built-in PDF parsing using **Apache PDFBox** to extract skills, experience, and education directly from candidate resumes.

---

## 🏗️ Architecture & Technology Stack

| Layer | Technology |
|---|---|
| **Backend Framework** | Spring Boot 3.3.4 (Java 17 / 21) |
| **AI / LLM Integration** | LangChain4j 0.35.0 (Groq / OpenAI models) |
| **Database & ORM** | PostgreSQL (Supabase compatible), Spring Data JPA, Hibernate |
| **Database Migrations** | Flyway (V1 through V7 scripts) |
| **Real-Time WebRTC** | LiveKit Server SDK |
| **Voice Processing** | Deepgram STT API, OpenAI TTS API |
| **Document Processing** | Apache PDFBox, Apache POI |
| **Frontend** | Vanilla JavaScript, HTML5, CSS3, LiveKit Client SDK |

---

## 🚀 Getting Started

### Prerequisites
- **Java JDK 17** or **21** installed
- **Git** installed
- PostgreSQL database (Local or Cloud instance like Supabase)

### 1. Clone the Repository
```bash
git clone https://github.com/Santhosh-L-source/Ai_Mock_Interview.git
cd Ai_Mock_Interview
```

### 2. Configure Environment Variables
Copy the sample environment file and configure your API keys:
```bash
cp .env.example .env
```

| Variable | Description | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/postgres` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `postgres` |
| `SERVER_PORT` | HTTP Server port | `8082` |
| `GROQ_API_KEY` | Groq API Key for LLM reasoning | Required for LLM features |
| `LIVEKIT_API_KEY` | LiveKit WebRTC API key | Required for WebRTC rooms |
| `LIVEKIT_API_SECRET` | LiveKit API secret | Required for WebRTC rooms |
| `LIVEKIT_URL` | LiveKit WebSocket URL | `wss://your-domain.livekit.cloud` |
| `DEEPGRAM_API_KEY` | Deepgram API Key for STT | Required for speech transcription |
| `OPENAI_TTS_API_KEY` | OpenAI API Key for TTS synthesis | Optional |

### 3. Run Unit Tests
Run the automated test suite to ensure everything is operating cleanly:
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### 4. Start the Application
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

The application will be accessible at:
```
http://localhost:8082
```

---

## 📡 REST API Summary

### 🔐 Authentication (`/api/auth`)
- `POST /api/auth/student/login` - Student login via Roll Number or Email
- `POST /api/auth/admin/login` - Admin login with username & password
- `POST /api/auth/reset-password` - Reset candidate password

### 📋 Interview & Sessions (`/api/interview`)
- `POST /api/interview/start` - Initialize new interview session
- `POST /api/interview/next` - Submit answer and fetch next question
- `POST /api/interview/final-scorecard` - Generate final comprehensive scorecard
- `POST /api/interview/proctor/violation` - Record proctoring infraction

### 🎙️ Voice & Real-Time Events (`/api/voice`)
- `POST /api/voice/event` - Process voice events (Timer expired / Next clicked)
- `POST /api/voice/livekit/token` - Generate LiveKit participant token

### 📅 Admin Management (`/api/admin/schedule`)
- `POST /api/admin/schedule/interviews` - Create scheduled interview
- `POST /api/admin/schedule/assign-year` - Bulk assign interview by student academic year
- `GET /api/admin/schedule/export-attendance` - Download Excel scorecard & attendance report
- `POST /api/admin/schedule/students` - Create student credentials

---

## 📂 Project Structure

```
AI-Interviewer/
├── .mvn/                         # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/ai/interviewer/
│   │   │   ├── config/           # App, Security, WebClient configs
│   │   │   ├── controller/       # REST API Controllers
│   │   │   ├── domain/
│   │   │   │   ├── dto/          # Data Transfer Objects & Requests/Responses
│   │   │   │   └── entity/       # JPA Entities (Candidate, Session, Turn, Proctor)
│   │   │   ├── repository/       # Spring Data Repositories
│   │   │   └── service/          # Business logic (LLM, STT, TTS, LiveKit, Export)
│   │   └── resources/
│   │       ├── db/migration/     # Flyway SQL migrations (V1 - V7)
│   │       ├── static/           # Web portal frontend (HTML, CSS, JS)
│   │       └── application.yml   # Spring application configuration
│   └── test/                     # Unit and Integration test suite
├── .env.example                  # Environment variables template
├── .gitignore                    # Git exclusions
├── pom.xml                       # Maven build configuration
└── README.md                     # Project documentation
```

---

## 🛡️ License
Distributed under the MIT License.
