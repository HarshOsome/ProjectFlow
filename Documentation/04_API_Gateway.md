# API Gateway — Reference Document

## What it does
The single entry point for all client traffic. This is where JWT validation actually happens — centrally, once, instead of every service re-checking tokens independently.

- Port: `9000` (moved from default `8080` due to a local Jenkins port conflict)
- No database — pure routing + auth layer

## Core concepts

### Why WebFlux (Reactive), not MVC — the biggest structural difference from every other service
Every other service used `spring-boot-starter-web` (blocking, Tomcat-based Spring MVC). The Gateway uses **Spring Cloud Gateway**, which is built on **Spring WebFlux** — non-blocking, reactive. A Gateway's whole job is proxying a very high volume of requests through to other services as fast as possible; reactive/non-blocking architecture handles that kind of high-throughput proxying more efficiently than blocking MVC.

**Critical rule:** never have both `spring-boot-starter-web` and the reactive gateway starter on the classpath at once — Spring Boot gets confused about which web server (Tomcat vs Netty) to start, and fails at startup. This bit us twice during setup (a mistyped `spring-boot-starter-webmvc` dependency snuck in both times from autocomplete).

On Spring Initializr: pick **"Reactive Gateway"**, not plain **"Gateway"** (that one is MVC/blocking) and definitely not any of the **"[Enterprise]"** tagged VMware Tanzu options (paid add-ons, not needed).

### The full authentication chain, end to end
1. **Login** — `POST /auth/login` hits the Gateway. Gateway checks its `PUBLIC_PATHS` list, sees this path is exempt, lets it through untouched. User Service verifies password (BCrypt), issues a JWT (HMAC-SHA256 signed), returns it.
2. **Client holds the token** and attaches it as `Authorization: Bearer <token>` on every subsequent request.
3. **Every request passes through the Gateway first** — `JwtAuthenticationFilter` is a `GlobalFilter`, meaning it runs on literally every request, protected or not.
4. **Filter logic:** is the path public? No → is there a `Bearer` token? No → reject with 401 immediately. Yes → verify the signature using the exact same JJWT code pattern as `JwtUtil` in User Service (`Jwts.parser().verifyWith(getSigningKey())...parseSignedClaims(token)`).
5. **If valid:** extract `userId` and `role` from the claims, then **mutate the outgoing request** to add `X-User-Id` and `X-User-Role` as plain headers, before forwarding to the target service.
6. **Downstream services (Project/Task) never see the token at all.** They just read `X-User-Id`/`X-User-Role` off the headers and trust them completely — because the only way those headers could exist is if the Gateway put them there, and the Gateway only does that after a real signature check passed.

**One sentence summary:** authentication happens exactly once, centrally, at the Gateway — every service behind it trusts that single checkpoint instead of re-verifying identity independently.

### `JwtAuthenticationFilter` — code and the reactive programming introduced
```java
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    private static final List<String> PUBLIC_PATHS = List.of("/auth/register", "/auth/login");

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (PUBLIC_PATHS.contains(path)) return chain.filter(exchange);

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return unauthorized(exchange);

        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload();

            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role)
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            return unauthorized(exchange);
        }
    }

    @Override
    public int getOrder() { return -1; }
}
```

- **`GlobalFilter`** — Spring Cloud Gateway interface: "run this on every single request, before routing." This is the mechanism that makes centralized JWT checking possible at all.
- **`Ordered` + `getOrder()` returning `-1`** — controls execution order among multiple filters; `-1` means run early, before routing decisions.
- **`Mono<Void>`** — first real exposure to reactive programming. In blocking MVC, a method returns a value directly. In WebFlux, methods return a `Mono` — a promise of "zero or one value, eventually" — instead of blocking and waiting. `chain.filter(exchange)` doesn't execute immediately; it returns a `Mono` representing "the rest of the chain, running asynchronously."
- The `jwt.secret` value here **must be character-for-character identical** to User Service's `jwt.secret` — this is the shared key that lets the Gateway verify tokens it never created itself.

### Route configuration
```properties
spring.cloud.gateway.routes[0].id=user-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**,/users/**

spring.cloud.gateway.routes[1].id=project-service
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/projects/**

spring.cloud.gateway.routes[2].id=task-service
spring.cloud.gateway.routes[2].uri=http://localhost:8083
spring.cloud.gateway.routes[2].predicates[0]=Path=/tasks/**
```
Each route says "if the request path matches this pattern, forward it to this URL." Path predicates must match the target service's actual `@RequestMapping` exactly (a real bug hit here: `/task/**` written instead of `/tasks/**`, silently causing every task request to 404 at the Gateway even though Task Service itself was healthy).

## Bugs encountered and fixed (Gateway-specific)
1. **`spring-boot-starter-webmvc` (fake/mistyped) accidentally added twice** during dependency edits — conflicts with WebFlux, deleted both times.
2. **`server-port=8080` (hyphen instead of dot)** — silently ignored by Spring since it wasn't a recognized property name; fixed to `server.port`.
3. **Route path typo** `/task/**` vs `/tasks/**` — caused legitimate 404s that looked like a bigger problem than it was.
4. **`NoClassDefFoundError: ServerProperties`** — caused by Spring Cloud version mismatch (Gateway was on `2025.1.2` while every other service used `2025.0.3`). Different Spring Cloud release trains have different internal package structures even against the same Spring Boot version — all services in one system need matching Spring Cloud versions.
5. **Missing `implements GlobalFilter, Ordered`** on the class declaration — `@Override` on `getOrder()` had nothing to actually override, since the interface wasn't implemented at all. Easy to miss since the code still looked structurally complete.
6. **Port 8080 conflict with Jenkins** — moved Gateway to port 9000.

## Verified end-to-end test (all passed)
1. `POST http://localhost:9000/auth/login` → succeeds without auth (public path).
2. `POST http://localhost:9000/projects` with `Authorization: Bearer <token>`, **no manually-set `X-User-Id`** → `201 Created`, proving the Gateway auto-injected the header from the token.
3. Same request with no token at all → `401 Unauthorized` immediately from the Gateway; Project Service never contacted.
