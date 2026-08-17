# Bugs & Errors Handled — Full ProjectFlow Build Log

A chronological record of every real problem hit during the build, what caused it, and how it was fixed. Organized by category since many of the same mistake-patterns recurred across services.

---

## Category 1: pom.xml / dependency problems (recurred across almost every service)

### Fake/mistyped artifact IDs (recurring theme — Eclipse autocomplete guessing wrong)
Across User, Project, Task, Notification, and Gateway services, dependency-add autocomplete repeatedly produced **non-existent Maven artifacts** that looked plausible but weren't real:
- `spring-boot-starter-webmvc` → should be `spring-boot-starter-web`
- `spring-boot-starter-webmvc-test` → not real; use `spring-boot-starter-test`
- `spring-boot-starter-data-jpa-test`, `spring-boot-starter-mail-test`, `spring-boot-starter-validation-test` → none real; all replaced by the single `spring-boot-starter-test`, which bundles JUnit 5 + Mockito + AssertJ regardless of what layer you're testing.

**Lesson:** one `spring-boot-starter-test` covers all standard testing needs — there's no such thing as a per-feature "-test" variant starter.

### Missing `spring-cloud.version` property + missing BOM import
Project Service's first pom referenced `${spring-cloud.version}` without ever defining it, and had no `<dependencyManagement>` block importing `spring-cloud-dependencies`. Fix:
```xml
<properties>
    <spring-cloud.version>2025.0.3</spring-cloud.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```
Once the BOM is imported, `spring-cloud-starter-openfeign` and `spring-cloud-starter-circuitbreaker-resilience4j` need **no explicit `<version>` tags** — the BOM manages compatible versions automatically. Adding an explicit version manually risks silently overriding the BOM and causing a mismatch.

### VMware Tanzu Enterprise dependency accidentally added
Autocomplete/search on Spring Initializr surfaced **"Spring Cloud Gateway Access Control [Enterprise]"** and similar `com.vmware.tanzu.spring` artifacts — paid, commercial extensions not in the standard Spring Cloud BOM, causing `Non-resolvable import POM` and missing-version errors. Fix: delete these dependency blocks entirely; they were never needed. Correct choice on Initializr is always the plain, non-"[Enterprise]" tagged option.

### Spring Cloud version mismatch across services (Gateway-specific)
Gateway was generated with `spring-cloud.version=2025.1.2` while every other service used `2025.0.3`. Result: `NoClassDefFoundError: org/springframework/boot/web/server/autoconfigure/ServerProperties` at startup — different Spring Cloud release trains have different internal package structures even against the identical Spring Boot version. **Fix:** align every service's `spring-cloud.version` to the same value (`2025.0.3`).

### springdoc-openapi version mismatch for Spring Boot 4 vs 3.5
Initial Spring Initializr default was Spring Boot **4.1.0**, which broke JWT (jjwt) and Swagger dependency resolution since the surrounding ecosystem (Spring Cloud, Resilience4j) hadn't caught up to Boot 4 yet. **Fix:** standardized on Spring Boot **3.5.16** across all five services — the "boring, stable" choice, with springdoc `2.8.5` and jjwt `0.12.6`, both fully compatible.

### `spring-boot-starter-web` + WebFlux conflict (Gateway only)
Gateway's pom had `spring-boot-starter-webmvc` (itself a typo) sitting alongside `spring-cloud-starter-gateway-server-webflux`. Having both blocking (Tomcat) and reactive (Netty) web starters on the classpath simultaneously causes Spring Boot to fail, confused about which server to start. **Rule going forward:** Gateway projects must never include any MVC/blocking web starter.

---

## Category 2: Eclipse / Maven project structure issues

### Multi-module structure never actually set up
Early attempt generated ProjectFlow as **one single Spring Boot application** (`groupId: com.harsh`, `artifactId: projectflow`, one flat `pom.xml`) instead of a parent POM + 5 independent service modules. Decision made to delete and restart clean: generate each service **individually** via Spring Initializr with consistent `groupId: com.harsh.projectflow`, distinct artifacts (`user-service`, `project-service`, etc.), rather than retrofitting a monolith into a multi-module shape after the fact.

### Untracked service folders in Git
`task-service` and `api-gateway` were generated as separate Initializr downloads and physically existed on disk, but were never `git add`ed — showed as untracked files (`git status`) despite being visible in Eclipse. Fix: `git add task-service api-gateway`, then commit and push.

### Nested `.git` repository inside `task-service`
Even after adding, `task-service` showed in GitHub with a submodule-style folder icon (arrow overlay) instead of a normal folder, and `git status` showed `modified: task-service (modified content)` as one opaque unit rather than listing individual file changes — the signature of a nested Git repo. Root cause: a stray `.git` folder existed inside `task-service` itself (confirmed via `dir task-service /a`). Fix:
```
rmdir /s /q task-service\.git
git rm -r --cached task-service
git add task-service
git commit -m "Fix task-service nested repo"
```

### Renaming main application classes
When renaming `ProjectServiceApplication` → `TaskServiceApplication` (by copying a previous service's generated project as a starting point), had to verify three things stayed in sync: the class declaration, the `SpringApplication.run(TaskServiceApplication.class, args)` reference line, and the physical package folder path matching the `package` declaration. Using Eclipse's Refactor → Rename handles all three automatically; manual text find-and-replace risks missing the `.run()` reference specifically.

---

## Category 3: application.properties errors

### `server-port` (hyphen) instead of `server.port` (dot)
Silently ignored by Spring Boot — not an error, just never took effect. Gateway kept trying to use the default port. Property names must exactly match Spring's naming convention (dot-separated).

### Route path typo: `/task/**` instead of `/tasks/**`
Task Controller is mapped at `/tasks` (plural). Gateway route predicate was written as `/task/**` (singular), causing every request through the Gateway to 404 even though Task Service itself was completely healthy — the mismatch was in the Gateway's routing config, not the target service.

### Missing `spring.jpa.hibernate.ddl-auto=update`
Task Service's `application.properties` was copied from Project Service but this line was dropped during the copy. Result: `relation "tasks" does not exist` — Hibernate assumed the schema already existed and never attempted to create it, since without `ddl-auto=update` there's no instruction to create tables at all.

### Real database password committed to a public GitHub repo (across all 5 services)
`spring.datasource.password=<real password>` was hardcoded and pushed to a public repo across multiple commits before being caught. Full remediation:
1. **Rotated the actual PostgreSQL password immediately** — the step that matters most, since it neutralizes the leak regardless of what remains in Git history.
2. **Rewrote Git history** using BFG Repo-Cleaner (Python's `pip`/`git-filter-repo` wasn't available locally, so BFG — a standalone `.jar`, no Python dependency — was used instead):
   ```
   git clone --mirror <repo-url>
   java -jar bfg-1.15.0.jar --replace-text passwords.txt projectflow.git
   git reflog expire --expire=now --all
   git gc --prune=now --aggressive
   git push origin --force --all
   ```
3. **Windows-specific hiccup during `git gc`:** OneDrive-synced working folder caused file-lock conflicts ("Deletion of directory './objects/02' failed") — answered `n` to skip retry, since it was a non-essential compaction step; the actual history rewrite via BFG had already succeeded independently.
4. **Local working copy resync:** `git fetch origin` + `git reset --hard origin/main` in the normal (non-mirror) working folder — this step is separate from the mirror-clone cleanup and easy to forget.
5. **Side effect:** BFG's `--replace-text` over-redacted `notification-service/application.properties` locally, truncating it to one line. Fixed by simply rewriting the file's full correct contents from scratch.
6. **Permanent fix going forward:** every credential (`spring.datasource.password`, `spring.mail.username`, `spring.mail.password`) replaced with `${ENV_VAR}` placeholder syntax, with real values set only in each service's Eclipse Run Configuration → Environment tab — never committed to any file at all.

**Lesson learned generically:** rotating the credential matters more than scrubbing history (a rotated secret is inert even if still visible in old commits) — but both should be done for a public repo. Environment variables from the start avoid the whole category of problem.

### Environment variable set on the wrong Run Configuration
When adding `MAIL_USERNAME`/`MAIL_PASSWORD`, Eclipse's Run Configurations dialog defaulted to showing an unrelated previous entry (`UserServiceApplication`) instead of `NotificationServiceApplication`. Variables set there had zero effect, since each service's Run Configuration is independent. Fix: explicitly select the correct service's entry in the left sidebar list before adding environment variables.

---

## Category 4: Runtime / Java errors

### `MissingRequestHeaderException` surfacing as generic 500
`@RequestHeader("X-User-Id") Long userId` is required by default. Testing without setting the header in Thunder Client threw this exception — but the generic `@ExceptionHandler(Exception.class)` catch-all intercepted it and returned an unhelpful "An unexpected error occurred" instead of a clear 400. Adding `ex.printStackTrace()` inside the generic handler (visible only server-side, not to the API client) revealed the real cause immediately. **Lesson:** always log the real exception in the generic catch-all during development, even though the client-facing message stays intentionally generic.

### `HttpMediaTypeNotSupportedException` — wrong Content-Type
Thunder Client defaulted to `application/x-www-form-urlencoded` instead of `application/json` for a POST body. Fix: explicitly select "JSON" as the body format in the client, which sets the correct `Content-Type` header automatically.

### `ObjectOptimisticLockingFailureException` / `StaleObjectStateException` on Notification save
Root cause: constructing the `Notification` entity in a way that left `id` non-null before calling `.save()` (likely via mismatched use of the `@AllArgsConstructor`), causing Hibernate to attempt an UPDATE on a row that didn't exist yet instead of an INSERT. Fix: always build entities via the no-args constructor + explicit setters, letting `id` stay `null` until Hibernate assigns it during the actual insert — never construct with a manually-supplied `id`.

### `NoSuchBeanDefinitionException` — missing `@Service`
A service class was missing its `@Service` annotation, so Spring never registered it as a bean, causing `@Autowired` injection to fail elsewhere. Fix: add `@Service`.

### `NullPointerException` from unset `userId` before save
Early version of `NotificationService.sendNotification()` never called `.setUserId(...)` before saving, leaving the field null despite the DTO validation requiring it upstream (validation on the *request* DTO doesn't automatically populate the *entity*). Fixed by explicitly setting every field from the request onto the entity before `.save()`.

### `getOrder()` `@Override` error with no explanation
`JwtAuthenticationFilter` compiled with a red error on `@Override public int getOrder()`, but the cause wasn't visible in that part of the file. Root cause was several lines up: the class declaration was missing `implements GlobalFilter, Ordered` entirely (only had `@Component`), and the `import org.springframework.core.Ordered;` line was absent. Without implementing `Ordered`, there was nothing for `getOrder()` to actually override. Fix: add both the import and the `implements` clause.

### `CommentNotFoundException` handler with mismatched parameter type
`GlobalExceptionHandler` in Task Service had a method annotated `@ExceptionHandler(CommentNotFoundException.class)` but its parameter was typed `TaskNotFoundException ex` — a copy-paste mismatch. Fixed to match the annotation's exception type exactly.

---

## Category 5: Network / infrastructure (not code bugs, but real blockers)

### Port 8080 conflict with Jenkins
Gateway's default port was already in use locally by Jenkins. Fixed by moving Gateway to port `9000` — no other service needed to change, since nothing else calls the Gateway directly (it's the entry point, not a dependency of anything else).

### SMTP (port 587 and 465) blocked at the network/device level
Notification Service's real email delivery consistently timed out on both standard SMTP ports, tested across multiple networks (home WiFi, mobile hotspot) with identical results — pointing to a device-level block (likely endpoint security software on the office-issued laptop used for development) rather than any single network's router configuration. This was accepted as an infrastructure constraint outside the code's control; the DB-persistence and exception-handling behavior around the failure were fully verified and are correct regardless of live delivery being blocked in this specific environment.

---

## Cross-cutting lessons worth remembering
1. **One `spring-cloud.version` per whole system** — every service using Spring Cloud must match, or you get obscure `ClassNotFoundException`/`NoClassDefFoundError` at startup.
2. **Never trust Eclipse's dependency autocomplete blindly** — verify real artifact names against Maven Central or known-good examples; fake artifact names compile-error in ways that look like configuration problems rather than typos.
3. **Environment variables from day one** for any credential — retrofitting this after a leak is far more work (history rewriting, password rotation) than just doing it correctly from the start.
4. **Generic exception handlers should always log server-side** (`ex.printStackTrace()` or a real logger), even while returning a clean, non-revealing message to the client — otherwise every unexpected error becomes a guessing game.
5. **A working circuit breaker isn't proven by reading the code — it's proven by actually stopping the dependency and testing both states** (up vs down) with the exact same request, and confirming both produce correct, non-crashing behavior.
