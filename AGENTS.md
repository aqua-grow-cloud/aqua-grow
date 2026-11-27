# AGENTS.md - Aqua Grow monorepo

Multi-tenant aquaculture SaaS. One repo, four deployables. Deployment config lives in a
separate repo (`aqua-grow-cloud/aqua-grow-config`) and is **not** touched from here.

| Dir | What | Stack |
|---|---|---|
| `api/` | REST API | Spring Boot 4.2, Java 21, PostgreSQL, Liquibase |
| `mcp/` | MCP server for AI tools | Spring Boot 4.2, Spring AI 2.1 |
| `resource-monitor/` | Kubernetes pod metrics | Spring Boot 4.2, fabric8 |
| `frontend/` | Web UI (plain npm, NOT a Gradle project) | Vue 3, TypeScript, Vite, Vuetify, Pinia, TanStack Query |
| `build-logic/`, `gradle/` | Shared Gradle convention plugin and version catalog | |
| `docker/`, `scripts/` | Shared Dockerfile and helper scripts | |

## Commands
```bash
./gradlew build                      # all backend services + tests
./gradlew :api:test                  # one service
./gradlew :api:test --tests "ClassName.method"
cd frontend && npm ci && npm run api:generate   # generated client is gitignored
cd frontend && npm test -- --run     # Vitest (use --run, watch mode never exits)
cd frontend && npm run build
```
Run only the tests for the service you changed, then the broader build if the change is shared.

## Hard rules
1. **Multi-tenancy:** every JPA entity has `tenantId` and `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")`. Always set `TenantContext.getTenantId()` when creating entities.
2. **Never edit `frontend/src/api/generated/`** (Orval output, gitignored). Wrap generated hooks in feature service files.
3. **Never modify an existing Liquibase changeset.** Add `db/changelog/changesets/NN-description.xml` and register it in `db.changelog-master.xml`.
4. **Do not touch** `.github/workflows/`, `docker/`, `build-logic/` or secrets unless the task is explicitly about them.
5. **No try/catch in controllers.** Throw `BusinessException`; the global `@ControllerAdvice` handles it.
6. **TypeScript strict, no `any`.**
7. Add tests for every new feature or bug fix (JUnit 5 + Mockito backend, Vitest frontend).
8. Keep changes scoped to the service(s) named in the task. No drive-by refactors.

## Backend conventions
- Boot 4 / Spring Framework 7 / Jackson 3 (`tools.jackson.*`). Tests use `@MockitoBean`, not `@MockBean`.
- Layering: thin `@RestController` -> `@Service` (`@Transactional`, business logic) -> `@Repository`.
- 4 spaces, K&R braces, ~120 cols. Lombok: `@Data @Builder @NoArgsConstructor @AllArgsConstructor @RequiredArgsConstructor @Slf4j`.
- DB names `snake_case`. Classes carry a suffix: `PondController`, `PondRepository`, `BatchService`.
- Controller tests: `@WebMvcTest`; repository tests: `@DataJpaTest`.

## Frontend conventions
- Feature-first: `src/features/<feature>/{components,composables,api,stores}` plus `<Feature>Page.vue`. Add routes in `router/index.ts`.
- `<script setup lang="ts">`, 2 spaces, single quotes. Components `PascalCase.vue`, composables `useName.ts`, stores `useNameStore.ts`.
- Server state: TanStack Query hooks. Global state: Pinia. Local state: refs.
- API errors go through the `useApiAction` composable.
- Import order: external, Vue, `@/` absolute, relative.

## API contract
Backend changes alter the OpenAPI spec (`/v3/api-docs`). If an endpoint's shape changes, say so in the PR
description. `scripts/export-openapi.sh` regenerates `frontend/openapi.json`. The CI contract check is advisory
today (known drift), do not try to fix unrelated drift in a feature PR.

## PR expectations
- Small, focused diff. Title in imperative mood. Describe what changed and how you tested it.
- CI (`ci` check) must pass. Never merge; a human approves and merges.
