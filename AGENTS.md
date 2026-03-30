# AGENTS.md

This file is for coding agents operating in `/Volumes/d/xianyu/hotel`.
It captures repository-specific build, lint, test, and coding rules.

## Scope And Rule Sources
- Applies repo-wide unless a deeper `AGENTS.md` overrides it.
- Derived from `CLAUDE.md`, package scripts, Maven config, and current code patterns.
- No Cursor rules were found in `.cursor/rules/` or `.cursorrules`.
- No Copilot rules were found in `.github/copilot-instructions.md`.
- If another agent file conflicts, prefer this root `AGENTS.md`.

## Repository Overview
- `client/`: main frontend, Vue 3 + TypeScript + Vite + Element Plus + Pinia.
- `server/`: main backend, Spring Boot 3.5 + Java 17 + Maven + JPA.
- `ios/`: separate Ionic/Vue app with its own Vitest and Cypress setup.
- Domain: hotel booking and operations with multi-store isolation.

## Operating Rules
- Do not start frontend or backend services yourself; the user owns runtime startup.
- For any code change, present a plan first and wait for explicit confirmation.
- Prefer the smallest safe change that follows existing patterns.
- Do not add dependencies unless necessary and approved.
- Never use destructive git commands unless explicitly requested.
- Keep frontend work in TypeScript; do not downgrade to JavaScript.

## Multi-Store Rules
- Store-scoped backend endpoints must use `@StoreScoped`.
- Store-scoped entities must implement `StoreScopedEntity`.
- Store-scoped entities should use `@EntityListeners(StoreScopedEntityListener.class)`.
- Service-layer store context should come from `StoreContextHolder` or `StoreContextUtils`, not request parameters.
- Frontend should not manually pass `storeId` for normal store-scoped APIs.
- Frontend request flow should rely on `client/src/utils/request.ts` to attach `X-Store-Id`.

## Package Manager And Tooling
- `client/` uses `bun`; prefer `bun run ...`.
- `server/` uses Maven Wrapper: `./mvnw ...`.
- `ios/` is independent; use its existing `npm` scripts.
- Prefer repository scripts over ad-hoc commands.

## Build, Lint, And Test Commands

### Root
- There is no single root build script.
- Run commands from the relevant subproject directory.

### Frontend: `client/`
- Install deps: `bun install`
- Dev: `bun run dev`
- Type check: `bun run type-check`
- Build: `bun run build`
- Build only: `bun run build-only`
- Lint all: `bun run lint`
- Oxlint fix: `bun run lint:oxlint`
- ESLint fix: `bun run lint:eslint`
- Format `src/`: `bun run format`
- There is currently no standard unit test script in `client/package.json`.
- For frontend changes, minimum verification is usually `bun run type-check` and `bun run build`.

### Backend: `server/`
- Run app: `./mvnw spring-boot:run`
- Compile: `./mvnw compile`
- Test all: `./mvnw test`
- Package: `./mvnw package`
- Single test class: `./mvnw -Dtest=RoomTypeServiceUniqueCodeTest test`
- Single test method: `./mvnw -Dtest=RoomTypeServiceUniqueCodeTest#createRoomType_codeConflicts_appendsNumericSuffix test`
- Common test folders:
  - `server/src/test/java/server/demo/service/`
  - `server/src/test/java/server/demo/util/`
  - `server/src/test/java/server/demo/controller/`

### iOS App: `ios/`
- Dev: `npm run dev`
- Build: `npm run build`
- Preview: `npm run preview`
- Unit tests: `npm run test:unit`
- E2E tests: `npm run test:e2e`
- Lint: `npm run lint`
- Single Vitest file: `npx vitest run tests/unit/example.spec.ts`
- Single Vitest test name: `npx vitest run tests/unit/example.spec.ts -t "test name"`
- Single Cypress spec: `npx cypress run --spec "tests/e2e/specs/example.cy.ts"`
- `ios/cypress.config.ts` uses `http://localhost:5173` as `baseUrl`.
- Do not run browser automation unless the user explicitly approves the test plan.

## Verification Expectations
- Backend code changes: at minimum run `./mvnw compile`; run targeted tests when relevant.
- Frontend changes in `client/`: at minimum run `bun run type-check` and `bun run build`.
- `ios/` changes: at minimum run `npm run build`; add `npm run test:unit` when logic changes.
- Do not claim a fix is complete without performing the relevant verification.

## Frontend Coding Standards
- Use Vue 3 with `<script setup lang="ts">`.
- Prefer `ref` over `reactive` unless a grouped object is clearly better.
- Use static imports by default.
- Keep styles scoped: `<style scoped>` or `<style scoped lang="scss">`.
- Follow existing path aliases such as `@/stores/user` and `@/api/auth`.
- Respect `client/.prettierrc.json` and `client/.editorconfig`:
  - semicolons off
  - single quotes on
  - width 100
  - indent 2 spaces
  - line endings LF
- Use existing Element Plus patterns instead of inventing new UI conventions.
- Centralize shared state in Pinia stores under `client/src/stores/`.
- Put reusable constants in `client/src/constants/` and utilities in `client/src/utils/`.

## Frontend API And Error Rules
- Do not call `fetch()` directly.
- Do not create raw `axios()` calls in feature code.
- Route API calls through `client/src/utils/request.ts` or the public wrapper.
- Base URLs must come from env vars such as `VITE_API_BASE_URL`.
- Let interceptors handle auth token, store header, and common error messaging.
- Use explicit `try/catch` around async operations that can fail.
- Do not silently swallow errors.
- Show user-facing failures with existing patterns such as `ElMessage`.
- Prefer early returns over nested conditionals; avoid nested ternaries.

## Frontend Naming And Types
- Components, classes, and types: `PascalCase`.
- Variables and functions: `camelCase`.
- Constants: `UPPER_SNAKE_CASE`.
- Prefer explicit TypeScript types for public APIs, store state, DTO-like structures, and complex refs.
- Avoid `any` unless narrowing is impractical, and explain why if used.

## Backend Coding Standards
- Use Java 17 features conservatively; optimize for readability.
- Keep package structure aligned with Spring roles: controller, service, repository, entity, dto, config.
- Controllers should return `ApiResponse<T>` consistently.
- Validate request payloads with Jakarta Validation and `@Valid`.
- Put business logic in services, not controllers.
- Repositories should include `storeId` constraints for store-scoped queries.
- Prefer specific exceptions or clear runtime messages over vague failures.
- Do not hardcode secrets, tokens, URLs, credentials, or store IDs.

## Backend Entity, API, And Transaction Rules
- JSON response envelope should remain `success`, `message`, `data`.
- Database columns use snake_case; Java fields and methods use camelCase.
- For schema behavior changes, prefer explicit Flyway migrations over relying on implicit Hibernate updates.
- Remember Hibernate update does not reliably relax `NOT NULL` to `NULL`.
- Use transactional boundaries in services where consistency matters.
- Fail loudly with meaningful messages when validation or store access fails.
- Do not ignore caught exceptions; log or convert them intentionally.
- Keep controller exception handling consistent with `ApiResponse.error(...)` patterns.

## Testing Guidance By Area
- Pure backend utility logic: add or update focused JUnit tests under `server/src/test/java/.../util/`.
- Backend service logic: add focused service tests under `server/src/test/java/.../service/`.
- Frontend UI in `client/`: prefer type-check and build verification unless a test harness already exists.
- `ios/` app: use Vitest for unit logic and Cypress for E2E when explicitly approved.

## Things Agents Should Not Do
- Do not start `bun run dev`, `./mvnw spring-boot:run`, or similar long-running services unless the user explicitly asks.
- Do not bypass the centralized request utility.
- Do not refactor unrelated files while making a targeted fix.
- Do not convert established TypeScript code to JavaScript.

## Useful File References
- Frontend request wrapper: `client/src/utils/request.ts`
- Frontend entry: `client/src/main.ts`
- Frontend lint config: `client/eslint.config.ts`
- Frontend prettier config: `client/.prettierrc.json`
- Backend Maven config: `server/pom.xml`
- Existing repo guidance: `CLAUDE.md`

## Agent Output Expectations
- Before editing code, present a numbered plan and wait for approval.
- After changes, report exactly what files changed and what verification ran.
- Keep follow-up suggestions short and concrete.
