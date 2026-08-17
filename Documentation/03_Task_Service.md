# Task Service — Reference Document

## What it does
Manages tasks and comments within projects. The most involved service — it calls **two** separate Feign clients (Project Service AND User Service) and enforces a status state machine.

- Port: `8083`
- Database: `tasks_db`

## Core concepts

### Two independent Feign relationships
Task Service has its own `ProjectServiceClient` + `ProjectServiceFallback` and its own `UserServiceClient` + `UserServiceFallback` — structurally identical to what Project Service built for its single User Service dependency, just duplicated for two targets. This is intentional: **each service only knows about its own direct dependencies.** Task Service doesn't know or care that Project Service also calls User Service internally — that's not its concern. This is loose coupling in practice, not just a buzzword.

```java
@FeignClient(name = "project-service", url = "${project-service.url}", fallback = ProjectServiceFallback.class)
public interface ProjectServiceClient {
    @GetMapping("/projects/{id}/exists")
    boolean projectExists(@PathVariable("id") Long id);
}
```
(User Service client follows the identical pattern.)

### `createTask()` — validating against two dependencies
```java
boolean projectExists = projectServiceClient.projectExists(request.getProjectId());
if (!projectExists) throw new TaskNotFoundException(...);

if (request.getAssigneeId() != null) {
    boolean assigneeExists = userServiceClient.userExists(request.getAssigneeId());
    if (!assigneeExists) throw new TaskNotFoundException(...);
}
```
`projectId` is always required, so that check always runs. `assigneeId` is optional (a task can exist unassigned), so the user-existence check is conditional on it being provided at all. Either dependency being down triggers its own independent fallback → `false` → rejection, same defensive pattern as Project Service.

### Status transition state machine — the genuinely new concept
Your spec's lifecycle: `TODO → IN_PROGRESS → IN_REVIEW → DONE → CANCELLED`. Naively, any status could be overwritten to any other status with zero validation. `TaskStatusValidator` prevents that:

```java
@Component
public class TaskStatusValidator {
    private final Map<TaskStatus, Set<TaskStatus>> allowedTransitions = new EnumMap<>(TaskStatus.class);

    public TaskStatusValidator() {
        allowedTransitions.put(TaskStatus.TODO, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED));
        allowedTransitions.put(TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.IN_REVIEW, TaskStatus.CANCELLED));
        allowedTransitions.put(TaskStatus.IN_REVIEW, EnumSet.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED));
        allowedTransitions.put(TaskStatus.DONE, EnumSet.noneOf(TaskStatus.class));       // terminal
        allowedTransitions.put(TaskStatus.CANCELLED, EnumSet.noneOf(TaskStatus.class));  // terminal
    }

    public boolean isValidTransition(TaskStatus current, TaskStatus next) {
        return allowedTransitions.get(current).contains(next);
    }
}
```

- `EnumMap`/`EnumSet` — specialized, more memory-efficient Map/Set implementations for enum keys, functionally the same as regular Map/Set.
- `IN_REVIEW → IN_PROGRESS` is allowed (a rejected review sends work back) — a deliberate real-world touch, not strictly linear.
- `DONE` and `CANCELLED` map to **empty sets** — terminal states, no further transitions allowed once reached.
- Kept as its **own separate `@Component`**, not baked into `TaskService` directly — so it can be tested in isolation (does `TODO → DONE` correctly fail?) without needing a database, Feign, or any other dependency.

**Verified behavior (tested):** `TODO → IN_PROGRESS` → `200 OK`. Immediately after, `IN_PROGRESS → DONE` (illegal, since IN_PROGRESS only allows IN_REVIEW/CANCELLED) → `409 Conflict` with a clear message. Confirmed via stack trace that `InvalidTaskStatusTransitionException` fired correctly and was caught by `GlobalExceptionHandler`.

**Real-world parallel:** this is a simplified version of what Jira's workflow engine does. Jira's is more elaborate — configurable per-project workflows stored as data (not hardcoded Java), plus conditions/validators/permission checks/post-functions attached to each transition. The core idea (a task has a status, only certain status-to-status moves are legal) is identical to what we built.

### Comment ownership check
```java
public void deleteComment(Long taskId, Long commentId) {
    Comment comment = commentRepository.findById(commentId)...
    if (!comment.getTaskId().equals(taskId)) {
        throw new CommentNotFoundException(...);
    }
    ...
}
```
Guards against `DELETE /tasks/5/comments/12` where comment 12 actually belongs to task 7 — without this check, the URL's `taskId` would be silently ignored and any comment could be deleted regardless of which task it's nested under.

## Endpoints
```
POST   /tasks
GET    /tasks/{id}
GET    /tasks/project/{projectId}
GET    /tasks/assignee/{userId}
PUT    /tasks/{id}
DELETE /tasks/{id}
PATCH  /tasks/{id}/status
POST   /tasks/{id}/comments
GET    /tasks/{id}/comments
DELETE /tasks/{id}/comments/{commentId}
```
`@PatchMapping` used specifically for the status endpoint — semantically correct for "partially update one field," as opposed to `PUT` which implies replacing the whole resource.

## Entity design notes
- `Task`: title, description, status, priority, projectId, assigneeId (nullable — task can be unassigned), createdBy, dueDate.
- `Comment`: taskId, userId, content. `userId` comes from the `X-User-Id` header, never the request body (same reasoning as `createdBy` elsewhere).
- Assignment currently happens via `PUT /tasks/{id}` (bundled into general update) rather than a dedicated endpoint — matches the original spec, though a dedicated `PATCH /tasks/{id}/assign` was considered for cleaner Notification Service event triggering later.

## application.properties essentials
```properties
server.port=8083
spring.datasource.url=jdbc:postgresql://localhost:5432/tasks_db
spring.cloud.openfeign.circuitbreaker.enabled=true
project-service.url=http://localhost:8082
user-service.url=http://localhost:8081
```
