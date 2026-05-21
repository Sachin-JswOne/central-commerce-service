# Test Cases

## Overview

- **PR:** 2
- **Jira:** JSWOC-20267 
- **Feature/Fix:** Implement Authorization Across CCP Services

---

## Test Cases

### TC-01: Access protected API with valid JWT and required role

* **Type:** Integration
* **Scenario:** Verify that a user with a valid JWT and correct role can access a protected endpoint.
* **Given:** A valid JWT token is generated for a user with `ROLE_ADMIN`.
* **When:** The user calls `/api/admin/users` with the JWT token in the `Authorization` header.
* **Then:** The API returns `200 OK` and the requested resource is accessible.
* **Status:** Covered

---

### TC-02: Access protected API with valid JWT but insufficient role

* **Type:** Integration
* **Scenario:** Verify RBAC denies access when the user does not have the required role.
* **Given:** A valid JWT token is generated for a user with `ROLE_USER`.
* **When:** The user calls `/api/admin/users` with the JWT token in the `Authorization` header.
* **Then:** The API returns `403 Forbidden` with an authorization error message.
* **Status:** Covered

---

### TC-03: Access protected API with expired JWT token

* **Type:** Integration
* **Scenario:** Verify authentication fails for expired JWT tokens.
* **Given:** An expired JWT token is available for a valid user.
* **When:** The user calls `/api/profile` with the expired JWT token.
* **Then:** The API returns `401 Unauthorized` with a token expired or invalid token error response.
* **Status:** Covered

---

### TC-04: Access protected API without JWT token

* **Type:** Integration
* **Scenario:** Verify authentication is enforced for protected APIs.
* **Given:** A protected API endpoint exists that requires JWT authentication.
* **When:** The user calls `/api/profile` without an `Authorization` header.
* **Then:** The API returns `401 Unauthorized` and access is denied.
* **Status:** Covered

## Edge Cases

<!--
List any edge cases that should be explicitly verified:
- Empty/null inputs
- Boundary values
- Concurrent requests
- Downstream service failures
-->

## Known Gaps / Out of Scope

<!-- Any test scenarios intentionally not covered in this PR, with justification -->
