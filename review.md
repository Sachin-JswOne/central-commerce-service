# Claude Code Review Instructions

This file provides context and guidelines for Claude when reviewing pull requests in the `central-commerce-service` repository.

---

## PR-Specific Context

Before starting the review, look up the PR-specific context files located at:

```
.github/reviews/PR-{PR_NUMBER}/jira.md        # Jira ticket details, acceptance criteria
.github/reviews/PR-{PR_NUMBER}/testcases.md   # Test cases and scenarios
```

If these files exist, use them to:
- Understand the intent and business context of the change
- Verify the implementation aligns with the acceptance criteria
- Check whether the test cases cover the described scenarios

If they are missing, note it and proceed with a best-effort review based on the diff and PR description.

---

## Project Context

- **Service:** `central-commerce-service` — the unified commerce backend (Spring Boot, Maven, Java)
- **Modules:** `application`, `core`, `web`, `worker`
- **Integrations:** Commercetools SDK, Talon.One (promotions)
- **Role:** Central wrapper for cart, orders, and product management — new features go here, not in legacy `jsw_*` services

---

## Code Style Guidelines

### General
- Follow standard Java naming conventions: `camelCase` for variables/methods, `PascalCase` for classes, `UPPER_SNAKE_CASE` for constants
- Keep methods focused and short — prefer single responsibility
- Avoid deep nesting; extract conditions into well-named methods or early returns
- No unused imports, variables, or dead code
- Avoid magic numbers/strings — use named constants or enums

### Spring Boot Specifics
- Use constructor injection over field injection (`@Autowired` on fields is discouraged)
- Annotate service classes with `@Service`, repositories with `@Repository`, controllers with `@RestController`
- Keep controllers thin — delegate business logic to service classes
- Use `@Slf4j` (Lombok) for logging; no `System.out.println`
- Use `ResponseEntity<T>` for controller responses with appropriate HTTP status codes
- Handle exceptions via `@ControllerAdvice` / `@ExceptionHandler` — do not swallow exceptions silently

### API Design
- REST endpoints should follow resource-based URL patterns (`/orders/{id}`, not `/getOrder?id=...`)
- Use appropriate HTTP methods: `GET` for reads, `POST` for create, `PUT`/`PATCH` for update, `DELETE` for delete
- Request/response DTOs should be separate from domain/entity classes
- Validate inputs using Bean Validation (`@NotNull`, `@Valid`, etc.) at controller boundaries

### Error Handling
- All exceptions should be logged with context (avoid logging and re-throwing without adding context)
- Return structured error responses — do not expose stack traces or internal details in API responses
- Distinguish between client errors (4xx) and server errors (5xx)

### Security
- Never log sensitive data (tokens, passwords, PII)
- Validate and sanitize all external inputs
- Authorization checks should be enforced at the service or controller layer, not just the frontend
- Do not hardcode credentials, secrets, or environment-specific config — use properties/environment variables

### Testing
- Unit tests should cover business logic in service classes
- Use `@SpringBootTest` sparingly — prefer slice tests (`@WebMvcTest`, `@DataJpaTest`) or plain unit tests
- Mock external dependencies (Commercetools, Talon.One) in tests
- Test names should describe the scenario: `should_returnCart_whenValidCustomerIdProvided`

### Commercetools Integration
- Use the Commercetools SDK types correctly — avoid raw JSON manipulation where typed SDK methods exist
- Handle Commercetools API errors explicitly (e.g., `ConcurrentModificationException` for version conflicts)
- Do not expose Commercetools internal IDs or types directly in public API responses

---

## Review Checklist

When reviewing a PR, evaluate the following:

### Correctness
- [ ] Does the implementation match the Jira acceptance criteria (if available)?
- [ ] Are edge cases handled (null inputs, empty collections, missing resources)?
- [ ] Are error paths handled correctly and returned with appropriate status codes?

### Code Quality
- [ ] Is the code readable and self-explanatory without excessive comments?
- [ ] Are there any obvious code smells (long methods, duplicate logic, inappropriate abstractions)?
- [ ] Are constants used instead of magic values?

### Security
- [ ] Is sensitive data protected from logging and API responses?
- [ ] Are inputs validated at the boundary?
- [ ] Are authorization rules enforced correctly?

### Testing
- [ ] Are there sufficient unit/integration tests for the new logic?
- [ ] Do the tests cover both happy path and failure scenarios?
- [ ] Do tests align with the test cases defined in `testcases.md` (if available)?

### API Consistency
- [ ] Does the new API follow existing patterns in the service?
- [ ] Are DTOs used correctly, separating API contracts from domain models?

### Dependencies & Configuration
- [ ] Are new dependencies justified and minimal?
- [ ] Is configuration externalized (no hardcoded values)?

---

## Tone

- Be constructive and specific — point to the exact line and explain why it's a concern
- Distinguish between blocking issues (bugs, security, broken tests) and suggestions (style, improvements)
- Acknowledge good patterns when you see them
