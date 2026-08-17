# Project Service — Reference Document

## What it does
Manages projects and project membership. First service in the build to actually talk to another service over the network (Feign) and the first to need a circuit breaker.

- Port: `8082`
- Database: `projects_db`

## Core concepts

### Why no JPA relationships to User entities
`ProjectMember` stores `userId` as a plain `Long`, not a `@ManyToOne` reference to a `User` entity. This is deliberate: `User` lives in a **completely different database** (`users_db`), owned by a different service. JPA relationships only work within a single database's entity graph — you cannot `@ManyToOne` across service boundaries. This is "database per service" made concrete. Instead of a foreign key, you validate `userId` exists by calling User Service via Feign.

### Feign Client — what it actually is
`UserServiceClient` is an **interface with no implementation written by hand**. At startup, Spring Cloud OpenFeign scans for interfaces annotated `@FeignClient` (enabled via `@EnableFeignClients` on the main application class) and generates a working implementation — a dynamic proxy — behind the scenes. Calling `userServiceClient.userExists(5L)` on that proxy:
1. Reads the annotation `@GetMapping("/users/{id}/exists")`
2. Substitutes the path variable
3. Builds the full URL from the `url` property (`${user-service.url}` → `http://localhost:8081`)
4. Fires a real HTTP GET request
5. Deserializes the response back into the return type

```java
@FeignClient(name = "user-service", url = "${user-service.url}", fallback = UserServiceFallback.class)
public interface UserServiceClient {
    @GetMapping("/users/{id}/exists")
    boolean userExists(@PathVariable("id") Long id);
}
```

Note: `name` is mostly a label for logging/circuit-breaker bookkeeping when not using service discovery. The actual routing happens via the `url` property. `name` only becomes load-bearing if service discovery (e.g. Eureka) is added later.

### Circuit breaker fallback — what it is and why it returns `false`
`UserServiceFallback implements UserServiceClient` — a real class, not another interface, because it needs actual method bodies to run when the real Feign call fails (connection refused, timeout, 5xx).

```java
@Component
public class UserServiceFallback implements UserServiceClient {
    @Override
    public boolean userExists(Long id) {
        return false;
    }
}
```

**Why `false` and not `true` or an exception:** when you genuinely can't verify something because the dependency is down, the safe default is to assume it's *not* safe to proceed — same logic as "if you can't check someone's ID, don't let them in." Returning `true` would let unverified members get added. Throwing would just crash the request the fallback exists to prevent. Returning `false` lets the existing `if (!userExists) throw ...` logic handle it exactly like a normal "not found" case — no special-casing needed anywhere else.

Required properties for the fallback to actually activate:
```properties
spring.cloud.openfeign.circuitbreaker.enabled=true
```
Without this line, `fallback=` is silently ignored, and a downed dependency throws a raw exception instead.

**Verified behavior (tested):** same request, same real `userId`, User Service running → `409 Conflict` (already a member, proving the real Feign call succeeded and the app logic ran). User Service stopped → `404 Not Found` in ~30-40ms (fast failure via fallback, no hang, no crash, no cascading failure into Project Service).

## Key classes

**`ProjectService.addMember()`** — the method that ties everything together. Order of checks matters:
1. Does the project exist? (local DB check — cheap, no network)
2. Does the user exist? (Feign call — expensive, may hit the fallback)
3. Is this user already a member? (local DB check)

Cheapest/local checks happen first; the network call only happens if truly needed.

**`ProjectMemberRepository`** — derived query methods:
- `findByProjectId` → `GET /projects/{id}/members`
- `findByProjectIdAndUserId` (chains two conditions with `And`) → check before adding a member
- `existsByProjectIdAndUserId` → cheap existence check without fetching the row
- `deleteByProjectIdAndUserId` → needs `@Transactional` on the calling service method — derived delete queries require a transaction context to execute reliably.

## Endpoints
```
POST   /projects
GET    /projects
GET    /projects/{id}
PUT    /projects/{id}
DELETE /projects/{id}
POST   /projects/{id}/members
DELETE /projects/{id}/members/{userId}
GET    /projects/{id}/members
GET    /projects/{id}/exists   ← internal, called by Task Service via Feign
```

## Entity design notes
- Two entities: `Project` (name, description, status, dates, createdBy) and `ProjectMember` (projectId, userId, memberRole, joinedAt).
- `MemberRole` (OWNER/MANAGER/DEVELOPER/VIEWER) is a **separate enum** from User Service's `Role` (ADMIN/PROJECT_MANAGER/DEVELOPER/VIEWER) — a user's system-wide role and their standing on one specific project are genuinely different concepts.
- `createdBy` is read from the `X-User-Id` request header, never trusted from the request body — otherwise a client could claim any userId as the creator.

## application.properties essentials
```properties
server.port=8082
spring.datasource.url=jdbc:postgresql://localhost:5432/projects_db
spring.cloud.openfeign.circuitbreaker.enabled=true
user-service.url=http://localhost:8081
spring-cloud.version=2025.0.3   # must match every other service using Spring Cloud
```

## pom.xml notes
Needs `spring-cloud-dependencies` imported as a BOM under `<dependencyManagement>` so Feign and Resilience4j resolve to compatible versions automatically (no explicit `<version>` tags on those two starters — let the BOM manage it).
