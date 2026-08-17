# User Service — Reference Document

## What it does
Handles user registration, login, and identity data. It's the source of truth for "who is this person" — every other service trusts User Service (indirectly, via the Gateway) instead of managing its own auth logic.

- Port: `8081`
- Database: `users_db`

## Core concepts (the "why," not just the "what")

### Stateful (old CMS way) vs Stateless (JWT way)
In CMS, `HttpSession` meant the server remembered who was logged in, in its own RAM, tied to a random `JSESSIONID` cookie. That breaks across 5 separate services — Service A's session means nothing to Service B, since each has its own RAM.

JWT flips this: instead of the server remembering you, **you carry proof of who you are** — a signed token — on every request. No shared session storage needed. This is why JWT fits microservices.

**Analogy:** JWT is a concert wristband. Show ID once at the gate (login), get a wristband with a hologram (signature). After that, any staff member (any service) can glance at the wristband and know you're legit — no need to re-check ID every time.

### The token itself
`header.payload.signature`
- **Header** — says "I'm a JWT, signed with algorithm X"
- **Payload** — actual data (`userId`, `email`, `role`) — NOT encrypted, just base64. Never put secrets here.
- **Signature** — HMAC-SHA256 hash of header+payload, computed using a **secret only the server(s) know**. This is the hologram. If anyone edits the payload, the signature no longer matches, and verification fails instantly.

### BCrypt (password hashing) — separate concept from JWT signing
- **One-way.** There is no `decode()`. You never reverse a hash back into a password.
- **How login actually verifies a password:** the stored hash contains the salt *inside itself* (e.g. `$2a$10$N9qo8uLOickgx2ZMRZoMy...` — `$2a$` = algorithm, `10` = cost factor, next chunk = salt, rest = hash). `passwordEncoder.matches(rawPassword, storedHash)` pulls the salt out of the stored hash, re-hashes the incoming raw password using that same salt, and compares the two hash outputs character-for-character. If they match, the original passwords must have been identical.
- **Cost factor** (we used the default, `10`): controls how many hashing rounds run internally — `2^cost` rounds. Cost 10 = 1,024 rounds, cost 12 = 4,096. Each +1 roughly doubles the work. This is deliberate — BCrypt is designed to be *slow* so that if a database is ever breached, brute-forcing millions of password guesses against the stolen hashes becomes computationally impractical (each individual guess costs real time).
- **Brute force**, concretely: trying every possible password (or a dictionary of common ones) against a stolen hash until one matches. Fast hash algorithms (like plain SHA-256) let attackers try billions of guesses/sec on a GPU. BCrypt's deliberate slowness turns that into an impractically long attack.

### JWT signing (HMAC-SHA256) — does NOT touch the password at all
By the time `JwtUtil.generateToken()` runs, password verification is already finished (handled separately by BCrypt inside `UserService.login()`). HMAC signs the **claims** (`userId`, `email`, `role`, expiry) — not any password. The signature proves the claims weren't tampered with; it doesn't hide or encrypt them.

Both BCrypt and HMAC follow the same underlying philosophy: **recompute and compare**, never decrypt/reveal.

## Key classes and what they do

**`JwtUtil`**
- `getSigningKey()` — converts the secret string into a proper `SecretKey` object via `Keys.hmacShaKeyFor(secret.getBytes())`.
- `generateToken(userId, email, role)` — builds claims, signs with HMAC-SHA256, returns the final token string. Uses `Jwts.builder()...signWith(getSigningKey())...compact()`.
- `extractClaims(token)` — verifies the signature via `Jwts.parser().verifyWith(getSigningKey())...parseSignedClaims(token)`. Throws an exception automatically if the token was tampered with or has expired.
- `isTokenValid(token)` — wraps `extractClaims` in try/catch, returns boolean.

**`PasswordEncoderConfig`** — registers `BCryptPasswordEncoder` as a Spring bean.

**`SecurityConfig`** — disables CSRF (irrelevant for stateless JWT APIs — no cookie to hijack), sets session policy to `STATELESS`, and permits all requests here (`anyRequest().permitAll()`) because **JWT verification does NOT happen inside User Service** — it happens once, centrally, at the API Gateway. This keeps User Service simple and avoids duplicating auth logic across every service.

**`UserService`**
- `register()`: check email uniqueness → hash password with BCrypt → save → immediately generate a JWT so the user is logged in right after registering.
- `login()`: find by email (if not found, throw a deliberately vague `InvalidCredentialsException` — not `UserNotFoundException` — so attackers can't distinguish "wrong email" from "wrong password") → `passwordEncoder.matches()` → generate token.

## Endpoints
```
POST   /auth/register
POST   /auth/login
GET    /users/{id}
GET    /users
GET    /users/{id}/exists   ← internal, called by other services via Feign
PUT    /users/{id}
DELETE /users/{id}
```

## Entity design notes
- `Role` enum stored as `@Enumerated(EnumType.STRING)` — stores `"DEVELOPER"` as text, not a number, so reordering the enum later doesn't silently corrupt existing data.
- `@PrePersist`/`@PreUpdate` hooks set `createdAt`/`updatedAt` automatically — Hibernate calls these for you, you never call them yourself.
- Lombok `@Data` needs care on JPA entities: added `@EqualsAndHashCode(of = "id")` (default `@Data` behavior uses ALL fields for equals/hashCode, which breaks once an entity moves from unsaved `id=null` to saved `id=5`) and `@ToString(exclude = "password")` (so a stray `log.info(user)` never prints the BCrypt hash).
- `password` never appears in any response DTO — `UserResponse` is a separate class with no password field.

## application.properties essentials
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/users_db
spring.jpa.hibernate.ddl-auto=update
jwt.secret=<must match the Gateway's secret exactly>
jwt.expiration=86400000
```
