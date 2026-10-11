# AENKO Backend — Anime Journey, Quest & Guild Platform

Spring Boot (4.1.1, Java 17) modular monolith. MySQL at runtime, JWT auth,
MyAnimeList as the anime metadata provider behind a provider abstraction.

## Run

```bash
cd aenko-backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Backend: `http://localhost:8080` · Frontend (separate React+Vite app) expects
CORS access from `http://localhost:5173` (already configured).

## Configuration (environment variables)

| Variable            | Default (dev)                                                        | Purpose                        |
|---------------------|----------------------------------------------------------------------|--------------------------------|
| `DB_URL`            | `jdbc:mysql://localhost:3306/aenko?createDatabaseIfNotExist=true...` | MySQL URL                      |
| `DB_USERNAME`       | `root`                                                               | DB user                        |
| `DB_PASSWORD`       | `root`                                                               | DB password                    |
| `JWT_SECRET`        | dev-only placeholder (≥32 chars)                                     | JWT signing secret             |
| `JWT_EXPIRATION_MS` | `86400000` (24h)                                                     | Token lifetime                 |
| `MAL_CLIENT_ID`     | _(empty)_                                                            | MyAnimeList API client ID      |
| `AI_ENABLED`        | `false`                                                              | Enable AI insights             |
| `AI_PROVIDER`       | `openai`                                                             | `openai`, `ollama` (local, no key), or `gemini` (cloud, needs key) |
| `AI_BASE_URL`       | _(provider default)_                                                 | Endpoint override (blank = documented default per provider) |
| `AI_API_KEY`        | _(empty, falls back to `GEMINI_API_KEY`)_                            | AI key (never hardcoded; not needed for `ollama`) |
| `GEMINI_API_KEY`    | _(empty)_                                                            | Gemini key from Google AI Studio (used when `AI_API_KEY` is unset) |
| `AI_MODEL`          | _(provider default)_                                                 | Model override (blank = `gpt-4o-mini`, or `gemini-3.5-flash-lite` for `gemini`) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173`                                           | Comma-separated frontend origins allowed by CORS |

Get a free MAL client ID: https://myanimelist.net/apiconfig → create app →
set `MAL_CLIENT_ID`. Without it, `/api/anime/**` returns a clean `502`
explaining what to configure; everything else works.

## AI Anime Intelligence

Default (`AI_ENABLED=false`) the insight endpoints return clearly labelled
local fallback guidance (`personalized=false`). For a genuine model response
(`personalized=true`) configure one provider:

**Local Ollama (no cloud key):**

1. Install Ollama: https://ollama.com/download and check installed models:
   `ollama list` (pull one if needed, e.g. `ollama pull llama3.1:8b`).
2. Start the server (`ollama serve`, or the Ollama app) so
   `http://localhost:11434/v1/chat/completions` responds.
3. Restart the backend with:
   `AI_ENABLED=true AI_PROVIDER=ollama AI_BASE_URL=http://localhost:11434/v1 AI_MODEL=<exact tag from ollama list>`
   (example: `AI_MODEL=llama3.1:8b`). No `AI_API_KEY` is needed; the backend
   sends a local-only `"ollama"` placeholder bearer token that Ollama ignores.

**Cloud (OpenAI-compatible):**

`AI_ENABLED=true AI_PROVIDER=openai AI_BASE_URL=<endpoint> AI_API_KEY=<key> AI_MODEL=<model>`

**Google Gemini (cloud, recommended for deployment):**

1. Create a key in [Google AI Studio](https://aistudio.google.com/apikey)
   (see [API key docs](https://ai.google.dev/gemini-api/docs/api-key)). The key
   stays in environment variables only — never in code, frontend files, or Git.
2. Local PowerShell (current session only, nothing is written to disk):
   `$env:GEMINI_API_KEY='<paste-key-here>'`
3. Restart the backend with:
   `AI_ENABLED=true AI_PROVIDER=gemini AI_MODEL=gemini-3.5-flash-lite`
   (`AI_BASE_URL` and `AI_MODEL` may be omitted: they default to the
   documented Gemini endpoint `https://generativelanguage.googleapis.com/v1beta/openai`
   and `gemini-3.5-flash-lite`, a Stable fast model per the
   [official model list](https://ai.google.dev/gemini-api/docs/models);
   set `AI_MODEL=gemini-3.8-flash` for the newest Stable Flash instead.)
   `AI_API_KEY` falls back to `GEMINI_API_KEY` automatically.
4. Switching back to Ollama needs no code change:
   `AI_ENABLED=true AI_PROVIDER=ollama AI_BASE_URL=http://localhost:11434/v1 AI_MODEL=<ollama tag>`
   (Gemini settings are simply ignored by the `ollama` provider).

Requests use non-streaming `POST {baseUrl}/chat/completions` with
`model`, bounded `max_tokens=300`, and system/user messages carrying only
compact permitted stats (counts, level/XP, top genres, recent activity).
The Gemini key travels only as an `Authorization: Bearer` header; error
responses are mapped to the labelled fallback and never surface keys or
upstream bodies. Connect (8s) / read (30s) timeouts apply; any failure
(disabled, missing cloud key, invalid key 401, rate-limit 429, server
errors, connection refused, model-not-found 404, timeout, empty choices)
returns the labelled fallback and never modifies XP, quests, achievements,
progress, libraries, or guilds. React never calls any AI provider directly
and never sees any key.

## Production deployment (Render backend + Vercel frontend)

Code is deployment-ready; hosting accounts and the deploy itself are manual.
In the **Render dashboard** for the backend service, set these environment
variables (values below are examples — the real secrets live only in Render):

| Render variable | Value |
|---|---|
| `AI_ENABLED` | `true` |
| `AI_PROVIDER` | `gemini` |
| `GEMINI_API_KEY` | _(paste the AI Studio key as a secret)_ |
| `AI_MODEL` | `gemini-3.5-flash-lite` (optional; this is the default) |
| `CORS_ALLOWED_ORIGINS` | `https://<your-app>.vercel.app` (exact Vercel URL, no trailing slash) |
| `DB_URL` | Render MySQL/Postgres JDBC URL for the production database |
| `DB_USERNAME` / `DB_PASSWORD` | production DB credentials (secrets) |
| `JWT_SECRET` | fresh random string ≥32 chars (secret; never reuse the dev default) |
| `MAL_CLIENT_ID` | _(optional)_ MyAnimeList client ID, or anime search returns `502` |

Notes: `AI_BASE_URL` can stay unset (Gemini default applies, trailing
slashes are normalized). No `.env` file is needed or read in production;
Ollama must not be installed on Render — the `gemini` path makes no local
calls. After deploy, verify with: `GET https://<render-url>/api/franchises`
(public, `200`) and, logged in, `POST https://<render-url>/api/ai/insight`
(`personalized:true` proves a live Gemini response).

In the **Vercel dashboard** for the frontend, set exactly one variable:

| Vercel variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://<render-backend-url>` (no trailing slash) |

Keep developing locally with `VITE_API_BASE_URL=http://localhost:8080`.
Never create `VITE_GEMINI_API_KEY` or any `VITE_*` secret: everything
starting with `VITE_` is bundled into public frontend assets. The AI
Insights page keeps calling `GET/POST /api/ai/insight` on the backend with
the unchanged `{type, content, personalized}` contract.

## API (all JSON, Postman-friendly)

Auth: `Authorization: Bearer <JWT>` (from register/login).

| Method | Endpoint                                        | Auth | Description                          |
|--------|-------------------------------------------------|------|--------------------------------------|
| POST   | `/api/auth/register`                            | no   | Register (always role `USER`)        |
| POST   | `/api/auth/login`                               | no   | Login → JWT + user                   |
| GET    | `/api/auth/me`                                  | yes  | Current user                         |
| GET    | `/api/anime/search?q=naruto&page=0&size=10`     | no   | MAL search (paginated)               |
| GET    | `/api/anime/{malId}`                            | no   | MAL anime details                    |
| GET    | `/api/anime/{malId}/relations`                  | no   | Related anime (sequel/prequel/...)   |
| GET    | `/api/anime/ranking?type=all&page=0&size=10`    | no   | MAL top-anime ranking                |
| GET    | `/api/library?status=WATCHING&page=0&size=20`   | yes  | Own library (paginated, filterable)  |
| POST   | `/api/library`                                  | yes  | Add: `{"mediaId":1}` or `{"provider":"MAL","externalId":"21"}` + status/rating/notes |
| PUT    | `/api/library/{mediaId}`                        | yes  | Update status/rating/notes           |
| DELETE | `/api/library/{mediaId}`                        | yes  | Remove entry                         |
| POST   | `/api/progress/{mediaId}/episode/{n}`            | yes  | Mark episode watched (+20 XP)        |
| GET    | `/api/progress/{mediaId}`                       | yes  | Watched episodes                     |
| DELETE | `/api/progress/{mediaId}/episode/{n}`            | yes  | Unmark episode                       |
| GET    | `/api/quests`                                   | no   | All quests                           |
| GET    | `/api/quests/my`                                | yes  | Started quests + progress            |
| POST   | `/api/quests/{id}/start`                        | yes  | Start quest                          |
| POST   | `/api/quests/{id}/complete`                     | yes  | Claim (backend-verified)             |
| GET    | `/api/achievements`                             | no   | All (with unlocked flags if logged in) |
| GET    | `/api/achievements/my`                          | yes  | Unlocked achievements                |
| POST   | `/api/guilds`                                   | yes  | Create guild (you become OWNER)      |
| GET    | `/api/guilds`                                   | no   | Public guilds                        |
| GET    | `/api/guilds/{id}`                              | no   | Guild + member count + total XP      |
| POST   | `/api/guilds/{id}/join`                         | yes  | Join                                 |
| POST   | `/api/guilds/{id}/leave`                        | yes  | Leave (owner cannot)                 |
| GET    | `/api/guilds/{id}/members`                      | yes  | Members                              |
| GET    | `/api/guilds/{id}/leaderboard`                  | yes  | XP leaderboard                       |
| GET/POST | `/api/guilds/{id}/quests`                     | yes  | List / create (owner/admin) guild quest |
| GET    | `/api/franchises`                               | no   | Curated franchises only (empty unless real curated rows exist) |
| GET    | `/api/franchises/{id}`                          | no   | Franchise + linked anime             |
| GET    | `/api/franchises/{id}/watch-order`              | no   | Watch orders + steps                 |
| GET    | `/api/profile`                                  | yes  | AnimeVerse profile + stats           |
| GET    | `/api/profile/stats`                            | yes  | Derived statistics, XP, level        |
| GET    | `/api/activity`                                 | yes  | Recent activity (paginated)          |
| GET/POST | `/api/ai/insight`                             | yes  | Personalized insight (or fallback)   |

Library statuses: `PLANNED WATCHING COMPLETED ON_HOLD DROPPED`.

## Anime Guide data flow

`Guide page → /api/anime/search → /api/anime/{malId} (+ /relations) → AnimeService → AnimeProvider → MalAnimeProvider → MAL API`

- The guide works on **external MAL ids** and needs no library import:
  `GET /api/anime/{malId}/relations` returns live `related_anime` data
  (`[{anime, relationType}]`, e.g. `SEQUEL`, `SIDE_STORY`) without any local
  `Media`/`MediaRelation` rows. Missing relationships return `[]` — nothing is
  fabricated.
- Anime details request the MAL-supported `related_anime` field.
- Curated `/api/franchises/**` watch orders are a separate concept and are
  only served when real curated rows exist. The seed catalogue contains two
  verified main-series release orders (idempotent, metadata-only, no live
  MAL call at startup, never touches user data):
  - **Naruto** — "Main Series — Release Order": `Naruto` (MAL 20, TV, 220
    eps) → `Naruto: Shippuuden` (MAL 1735, TV, 500 eps). Direct sequel link
    verified both ways on MAL title pages.
  - **Steins;Gate** — "Main Series — Release Order": `Steins;Gate`
    (MAL 9253, TV, 24 eps) → `Steins;Gate 0` (MAL 30484, TV, 23 eps).
    Continuation verified via the 23β bridge special (MAL 32188).
  Movies/OVAs/specials are excluded and each guide says so. Franchise media
  and watch-order steps now also expose `externalId`/`externalUrl` so the
  Guide matches a selected anime to its curated franchise by provider id,
  never by title guessing.

## Rules (all server-side, never client-driven)

- Episode watched = **20 XP** (once per unique episode — re-watching an
  unmarked episode pays nothing extra), anime completed = **100 XP**,
  quests/achievements pay their configured reward **once** (idempotent transactions).
- Level = `totalXp / 500 + 1`.
- Guild contribution: **+5 guild XP** per watched episode, per guild.
- Guild quests pay their configured reward as guild XP on completion, split
  among contributors proportionally — exactly once per quest.
- XP, quest completion, achievements, guild XP: backend-verified only.
- AI is advisory-only and degrades to a local fallback when disabled.

## Tests

```bash
./mvnw test   # 104 tests, H2 in-memory, no Docker, no live MAL or Gemini key needed
```

Covers: auth/JWT, validation, security + CORS (incl. configurable origins),
MAL provider (mocked HTTP,
incl. related-anime parsing/empty states/config + error handling),
anime detail/relations API contracts (provider-backed, no local Media needed),
AI provider (mocked HTTP: success parse, non-streaming bounded request,
ollama/openai/gemini selection, provider URL/model defaults, trailing-slash
URLs, Bearer auth, 401/429/500, malformed bodies, timeouts, missing keys,
no key leakage, fallback labelling, no side effects),
guide (verified curated catalogue, idempotent seeding, external-id matching,
no fictional seed data), library, progress, quests, achievements, guilds,
profile/stats, repositories, response caching, error handling.
