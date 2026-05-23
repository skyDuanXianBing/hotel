# AGENTS.md

Scope: entire repo unless a deeper `AGENTS.md` overrides it.

## Working Rules
- Do not start frontend/backend dev servers yourself; the user owns runtime startup.
- Before any code change, show a short numbered plan and wait for explicit approval.
- Keep changes scoped; do not refactor unrelated areas.

## Repo Shape
- `client/`: main PMS web app, Vue 3 + TypeScript + Vite + Element Plus + Pinia. Package manager is `bun`.
- `server/`: Spring Boot backend, Java 17, Maven Wrapper, main class `server/src/main/java/server/demo/DemoApplication.java`.
- `ios/`: separate Ionic/Vue app with its own `npm` workflow, Vitest, and Cypress.
- `channel-simulator/`: local Su / SiteMinder channel simulator, TypeScript + Express + Vue dashboard. Package manager is `npm`.
- `docs/`: product, API, database, and feature specifications. `docs/ios-room-status-close-room/` contains the iOS room-status close-room repair spec.
- There is no root build script; run commands inside the relevant subproject.

## Commands Agents Usually Guess Wrong
- `client/`: `bun install`, `bun run dev`, `bun run type-check`, `bun run build`, `bun run lint`.
- `client/`: `bun run build` already runs `type-check` before `build-only`; do not duplicate unless you need both logs separately.
- `server/`: `./mvnw compile`, `./mvnw test`, `./mvnw package`, `./mvnw -Dtest=RoomTypeServiceUniqueCodeTest test`, `./mvnw -Dtest=RoomTypeServiceUniqueCodeTest#createRoomType_codeConflicts_appendsNumericSuffix test`.
- `ios/`: use `npm`, not `bun`: `npm run build`, `npm run test:unit`, `npm run test:e2e`.
- `channel-simulator/`: use `npm`, not `bun`: `npm install`, `npm run dev`, `npm run type-check`, `npm run build`, `npm run start`, `npm run verify:local-e2e`.

## Verification Minimums
- `client/` changes: run `bun run build`.
- `server/` changes: run `./mvnw compile`; add focused tests when logic changes.
- `ios/` changes: run `npm run build`; add `npm run test:unit` for logic changes.
- `channel-simulator/` changes: run `npm run build`; add `npm run type-check` separately only when you need isolated type logs.
- Local channel E2E changes: `npm run verify:local-e2e` in `channel-simulator/` is an integration check and requires the user to have already started PMS and simulator.
- Do not claim a fix is done without running the relevant verification.

## Frontend Gotchas
- Keep frontend work in TypeScript and Vue SFCs with `<script setup lang="ts">`.
- Preserve `client/src/main.ts` import order: `./utils/suConfigProxy` must stay the first import.
- Normal authenticated/store-scoped requests must go through `client/src/utils/request.ts`.
- Public no-login registration flows must use `client/src/utils/publicRequest.ts`; it rewrites `VITE_API_BASE_URL` from `/api/v1` to `/api` for `/api/public/...` endpoints.
- Do not manually pass `storeId` for normal store-scoped APIs; `client/src/utils/request.ts` adds `X-Store-Id` from local storage.
- Client formatting is not default Prettier: `client/.prettierrc.json` uses no semicolons, single quotes, width 100; `client/.editorconfig` uses 2 spaces and LF.

## Channel Frontend Notes
- Channel management lives under `client/src/views/channel/`.
- Keep the page shell in `ChannelManagement.vue`; shared data logic belongs in `composables/useChannelData.ts`.
- Repeated channel UI belongs in `components/`, dialog/drawer UI belongs in `components/dialogs/`, and tab content belongs in `components/tabs/`.
- Shared channel types and constants belong in `types.ts` and `constants.ts`.
- Production channel pages must not fake OAuth, hotel authorization, room mappings, orders, or sync success. If a real API is not connected, show a clear user-facing message and use `channel-simulator/` for simulated end-to-end flows.

## Multi-Store Backend Rules
- Store-scoped controllers/endpoints must use `@StoreScoped`.
- Store-scoped entities must implement `StoreScopedEntity` and usually add `@EntityListeners(StoreScopedEntityListener.class)`.
- In services, get store context from `StoreContextHolder` / `StoreContextUtils`, not from request params.
- Repositories for store-scoped data should constrain by `storeId`.

## Backend Config Gotchas
- Backend config loads from environment plus local `.env` files via `spring.config.import`: root `./.env` or `./server/.env` both work.
- Committed Flyway migrations exist under `server/src/main/resources/db/migration/`, but `spring.flyway.enabled=false` by default in `server/src/main/resources/application.properties`; do not assume migrations run automatically.
- The app still uses `spring.jpa.hibernate.ddl-auto=update`; for schema changes, prefer explicit Flyway migrations over relying on Hibernate.
- API responses are expected to use the `success` / `message` / `data` envelope via `ApiResponse<T>`.
- Su API local mock is opt-in via `SU_API_LOCAL_MOCK_ENABLED=true`; when enabled, `SuApiConfig` reads `SU_API_LOCAL_MOCK_BASE_URL`, `SU_API_LOCAL_MOCK_CLIENT_ID`, and `SU_API_LOCAL_MOCK_CLIENT_SECRET`.
- Do not point production-like configs at the local mock accidentally. Keep local mock values in local `.env` files only.

## Channel Simulator and Local E2E
- `channel-simulator/` is a local test tool, not a production service and not the official Su sandbox.
- Do not start PMS, frontend, iOS, or simulator dev servers yourself; the user owns long-running runtime startup.
- The simulator default URL is `http://localhost:4000`; PMS usually points to it with `SU_API_BASE_URL` or the local mock env vars.
- The simulator sends webhooks to PMS via `PMS_BASE_URL`, defaulting to `http://localhost:8092`.
- PMS test-support endpoints live under `/api/v1/test-support/channel-e2e` and are disabled by default.
- Enable PMS test-support only for local E2E with `CHANNEL_E2E_TEST_SUPPORT_ENABLED=true` and `CHANNEL_E2E_TEST_SUPPORT_KEY=<local key>`.
- Requests to test-support read endpoints require normal store context (`Authorization` and `X-Store-Id`) plus `X-Test-Support-Key`.
- `/api/v1/test-support/channel-e2e/setup-local` is intentionally permitted without normal auth but still requires `X-Test-Support-Key`.
- The simulator dashboard and `verify:local-e2e` may pass PMS auth through `Authorization`, `X-Store-Id`, and `X-Test-Support-Key`; never persist real tokens in source files.
- Mock Su pull reservation behavior depends on pending in-memory simulator state; restarting the simulator clears pending reservations and logs.
- Supported local E2E channels are currently `BOOKING` and `AIRBNB`; keep fixtures and verifier logic aligned with those codes.

## Public API Boundary
- Most backend controllers are under `/api/v1/...`.
- Public registration endpoints are separate: `/api/public/registration/...` and `/api/public/registration-booking/...`; use `publicRequest.ts` for those flows.

## Test Notes
- Server tests already exist in `server/src/test/java/server/demo/{service,util,controller,...}`; add focused tests next to the touched area.
- `ios/cypress.config.ts` uses `http://localhost:5173` as `baseUrl`; do not run browser automation unless the user explicitly approves the plan.
