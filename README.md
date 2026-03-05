# Akeyless POC — React + Spring Boot

A proof-of-concept application that lets you **create, retrieve, and list static key-value secrets** in [Akeyless](https://www.akeyless.io/) through a React UI backed by a Spring Boot REST API.

---

## Project Structure

```
AkeyLess/
├── backend/          # Spring Boot 3 + Java 21 (Maven)
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/poc/akeyless/
│       │   ├── AkeylessApplication.java
│       │   ├── config/
│       │   │   ├── AkeylessProperties.java   # reads application.properties
│       │   │   └── WebConfig.java            # CORS config
│       │   ├── controller/
│       │   │   └── SecretsController.java    # REST endpoints
│       │   ├── model/
│       │   │   ├── SecretRequest.java
│       │   │   └── SecretResponse.java
│       │   └── service/
│       │       └── AkeylessService.java      # Akeyless REST API wrapper
│       └── resources/
│           └── application.properties
└── frontend/         # React 18 + Vite
    ├── src/
    │   ├── api/secretsApi.js                 # axios calls to backend
    │   ├── components/
    │   │   ├── SaveSecretForm.jsx
    │   │   ├── GetSecretForm.jsx
    │   │   └── ListSecrets.jsx
    │   ├── App.jsx
    │   └── App.css
    └── vite.config.js
```

---

## Prerequisites

| Tool | Version tested |
|------|---------------|
| Java | 21 |
| Maven | 3.9.x |
| Node.js | 18+ |
| npm | 9+ |

You also need an **Akeyless account**. Sign up for free at <https://console.akeyless.io>.

---

## 1 — Get Akeyless API Credentials

1. Log in to the [Akeyless Console](https://console.akeyless.io).
2. Go to **Auth Methods** → **+ New Auth Method** → choose **API Key**.
3. Give it a name (e.g. `poc-key`) and click **Create**.
4. Copy the **Access ID** and **Access Key** — you will need these next.

> The Access ID looks like `p-xxxxxxxx` and the Access Key is a long base64 string.

---

## 2 — Configure the Backend

Open [backend/src/main/resources/application.properties](backend/src/main/resources/application.properties) and fill in your credentials:

```properties
akeyless.access-id=p-xxxxxxxx
akeyless.access-key=YOUR_ACCESS_KEY_HERE
akeyless.api-url=https://api.akeyless.io
```

> **Never commit real credentials to source control.** For production use environment variables or a secret manager.

---

## 3 — Run the Backend

```bash
cd backend

# Windows (fix JAVA_HOME if needed)
set JAVA_HOME=C:\Program Files\Java\jdk-21

# Start the Spring Boot server on port 8080
mvn spring-boot:run
```

The server starts at `http://localhost:8080`.

### REST Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/api/secrets` | Create or update a static secret |
| `GET` | `/api/secrets/value?name=/path/to/key` | Retrieve a secret value |
| `GET` | `/api/secrets/list?path=/` | List secrets under a path |

#### Example — Save a secret with curl

```bash
curl -X POST http://localhost:8080/api/secrets \
  -H "Content-Type: application/json" \
  -d '{"name":"/poc/my-key","value":"hello-world","description":"test secret"}'
```

#### Example — Get a secret

```bash
curl "http://localhost:8080/api/secrets/value?name=%2Fpoc%2Fmy-key"
```

---

## 4 — Run the Frontend

```bash
cd frontend
npm install          # already done if you followed setup
npm run dev
```

Open <http://localhost:5173> in your browser.

The Vite dev server proxies all `/api` requests to `http://localhost:8080`, so no CORS issues during development.

### UI Tabs

| Tab | What it does |
|-----|-------------|
| **Save Secret** | Enter a path (key) and value → creates or updates a static secret in Akeyless |
| **Get Secret** | Enter a path → retrieves and displays the stored value |
| **List Secrets** | Enter a path prefix → lists all static secrets under that prefix |

> Secret paths **must start with `/`** (e.g. `/my-app/db-password`).

---

## 5 — Build for Production

### Backend JAR

```bash
cd backend
set JAVA_HOME=C:\Program Files\Java\jdk-21
mvn clean package -DskipTests
# Output: backend/target/akeyless-poc-1.0.0.jar

java -jar target/akeyless-poc-1.0.0.jar
```

### Frontend Static Files

```bash
cd frontend
npm run build
# Output in: frontend/dist/
```

Serve `frontend/dist/` with any static file server (Nginx, Apache, Vercel, etc.) and point `VITE_API_URL` to your deployed backend.

---

## How It Works

```
Browser (React)
    │  POST /api/secrets  {name, value}
    ▼
Spring Boot (SecretsController)
    │  1. POST /auth  → Akeyless token
    │  2. POST /create-secret  (or /set-secret-val if it already exists)
    ▼
Akeyless API  (api.akeyless.io)
```

Authentication to Akeyless uses the **API Key** access type. A fresh token is obtained before each operation (stateless — fine for a POC; in production you would cache and refresh the token).

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `JAVA_HOME not set` | Run `set JAVA_HOME=C:\Program Files\Java\jdk-21` before `mvn` |
| `401 Unauthorized` from Akeyless | Double-check `access-id` and `access-key` in `application.properties` |
| `Secret already exists` | The service automatically retries with `/set-secret-val` — this is handled transparently |
| Frontend shows network error | Make sure the backend is running on port 8080 before starting the frontend |
| CORS error in browser | The backend allows `http://localhost:5173`; if you changed the frontend port update `cors.allowed-origins` in `application.properties` |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Vite 7, Axios |
| Backend | Spring Boot 3.2, Java 21, Maven 3.9 |
| Secrets | Akeyless REST API v2 |
