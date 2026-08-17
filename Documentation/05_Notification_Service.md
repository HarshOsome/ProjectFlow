# Notification Service — Reference Document

## What it does
Records and sends notifications (email) triggered by events in other services (task assigned, status changed, member added). Lightest service — no Feign clients, no circuit breakers — but introduces `JavaMailSender` and a genuinely important resilience pattern.

- Port: `8084`
- Database: `notifications_db`

## Core concepts

### Save-then-send ordering — the key design decision
```java
public NotificationResponse sendNotification(NotificationRequest request) {
    Notification notification = new Notification();
    // ...set fields...
    Notification saved = notificationRepository.save(notification);   // 1. SAVE FIRST

    try {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        // ...
        mailSender.send(mailMessage);                                  // 2. THEN attempt delivery
    } catch (Exception e) {
        throw new EmailDeliveryException("Notification saved but email delivery failed: " + e.getMessage());
    }
    return toNotificationResponse(saved);
}
```

**Why this order, specifically:** if email sending fails (bad credentials, SMTP blocked, provider downtime), a **permanent database record still exists** proving this notification was supposed to happen — visible via `GET /notifications/user/{userId}`, even though the actual email never arrived. If the order were reversed (send first, only save on success), a failed send would leave **zero trace anywhere** — you'd have no way to know notifications were silently being dropped. This was tested for real: SMTP delivery was blocked at the network/device level throughout development, and the DB write succeeded every single time regardless, with `EmailDeliveryException` firing cleanly with an accurate underlying cause instead of the request crashing.

### `JavaMailSender` — auto-configured, never written by hand
The moment `spring-boot-starter-mail` is on the classpath and `spring.mail.*` properties are set, Spring Boot auto-configures a working `JavaMailSender` bean — same auto-configuration pattern as `DataSource` in User Service. `SimpleMailMessage` is the plain-text email object (`setTo`, `setSubject`, `setText`).

### SMTP configuration (Gmail)
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=465
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.socketFactory.port=465
spring.mail.properties.mail.smtp.socketFactory.class=javax.net.ssl.SSLSocketFactory
```
- Gmail requires an **app password** (16-character, generated under Google Account → Security → App Passwords, requires 2-Step Verification enabled first) — never the real account password.
- Port 587 uses STARTTLS (starts unencrypted, upgrades mid-handshake). Port 465 uses implicit SSL (encrypted from the first byte). Don't mix both configs together — pick one style.
- **Both ports were tested and both timed out** on the development machine/network — confirmed via `Test-NetConnection`, tested on multiple networks (home WiFi, mobile hotspot) with identical failure. This pointed to a device-level block (likely corporate endpoint security on the office laptop used) rather than a network-specific issue, since it failed consistently across different networks. This is an infrastructure constraint outside the code's control — the code and configuration are correct; delivery itself couldn't be verified live in this environment. The database-persistence and exception-handling behavior were fully verified regardless.

### Environment variables instead of hardcoded secrets
`${MAIL_USERNAME}` / `${MAIL_PASSWORD}` in `application.properties` — real values set only in Eclipse's Run Configuration → Environment tab, per service, never committed to Git. This keeps real credentials entirely out of version control while the properties file itself (safe to commit) only ever contains placeholder syntax.

## Endpoints
```
POST   /notifications/send        ← called by other services
GET    /notifications/user/{userId}
GET    /notifications/user/{userId}/unread
PATCH  /notifications/{id}/read
DELETE /notifications/{id}
```

## Entity design notes
- `Notification`: userId, userEmail, type (enum), subject, message, isRead, createdAt only — deliberately no `updatedAt`, since the only thing that ever changes post-creation is `isRead` flipping to true, which doesn't need its own "last updated" tracking for this use case.
- `NotificationType` enum: `TASK_ASSIGNED`, `TASK_STATUS_CHANGED`, `TASK_COMMENT_ADDED`, `PROJECT_MEMBER_ADDED`, `PROJECT_DEADLINE_APPROACHING`.
- `userId` in `NotificationRequest` comes from the request body (not an `X-User-Id` header) since this endpoint is called by *other services*, not directly by an end user through the Gateway.

## Repository queries
- `findByUserId` → all notifications for a user
- `findByUserIdAndIsReadFalse` (chained `And` condition) → unread only

## application.properties essentials
```properties
server.port=8084
spring.datasource.url=jdbc:postgresql://localhost:5432/notifications_db
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```
