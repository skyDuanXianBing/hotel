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
- There is no root build script; run commands inside the relevant subproject.

## Commands Agents Usually Guess Wrong
- `client/`: `bun install`, `bun run dev`, `bun run type-check`, `bun run build`, `bun run lint`.
- `client/`: `bun run build` already runs `type-check` before `build-only`; do not duplicate unless you need both logs separately.
- `server/`: `./mvnw compile`, `./mvnw test`, `./mvnw package`, `./mvnw -Dtest=RoomTypeServiceUniqueCodeTest test`, `./mvnw -Dtest=RoomTypeServiceUniqueCodeTest#createRoomType_codeConflicts_appendsNumericSuffix test`.
- `ios/`: use `npm`, not `bun`: `npm run build`, `npm run test:unit`, `npm run test:e2e`.

## Verification Minimums
- `client/` changes: run `bun run build`.
- `server/` changes: run `./mvnw compile`; add focused tests when logic changes.
- `ios/` changes: run `npm run build`; add `npm run test:unit` for logic changes.
- Do not claim a fix is done without running the relevant verification.

## Frontend Gotchas
- Keep frontend work in TypeScript and Vue SFCs with `<script setup lang="ts">`.
- Preserve `client/src/main.ts` import order: `./utils/suConfigProxy` must stay the first import.
- Normal authenticated/store-scoped requests must go through `client/src/utils/request.ts`.
- Public no-login registration flows must use `client/src/utils/publicRequest.ts`; it rewrites `VITE_API_BASE_URL` from `/api/v1` to `/api` for `/api/public/...` endpoints.
- Do not manually pass `storeId` for normal store-scoped APIs; `client/src/utils/request.ts` adds `X-Store-Id` from local storage.
- Client formatting is not default Prettier: `client/.prettierrc.json` uses no semicolons, single quotes, width 100; `client/.editorconfig` uses 2 spaces and LF.

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

## Public API Boundary
- Most backend controllers are under `/api/v1/...`.
- Public registration endpoints are separate: `/api/public/registration/...` and `/api/public/registration-booking/...`; use `publicRequest.ts` for those flows.

## Test Notes
- Server tests already exist in `server/src/test/java/server/demo/{service,util,controller,...}`; add focused tests next to the touched area.
- `ios/cypress.config.ts` uses `http://localhost:5173` as `baseUrl`; do not run browser automation unless the user explicitly approves the plan.
