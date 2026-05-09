# Auth Service - Vietnam Restaurants

## Overview

This module encapsulates the **Auth Service**, the centralized Identity Provider and Access Management component of the microservices ecosystem. Its primary mandate is to manage user identities, process authentication requests, and issue standardized **JSON Web Tokens (JWT)** for securing distributed APIs.

**Core Responsibilities:**

* **Identity Management:** Secure handling of user credentials and registration processes.
* **Authentication:** Verifying client identity and generating cryptographically signed JWTs upon successful login.
* **Authorization:** Enforcing Role-Based Access Control (RBAC) schemas (e.g., SYSTEM_ADMIN, USER, RESTAURANT_OWNER).
* **Token Validation:** Providing internal endpoints to validate token integrity and extract claims for downstream consumption.

## Architectural Integration for New APIs

When exposing new protected APIs within domain services (e.g., a secured endpoint in `restaurant-service`), the requests must be authenticated. The system supports two primary integration patterns for JWT handling:

### Pattern 1: Centralized Validation at the API Gateway (Recommended)

In this topology, the API Gateway intercepts requests, validates the JWT, and mutates the request headers before forwarding.

* **Auth Service Role:** Issues the JWT containing subject (`userId`) and authorities (`roles`).
* **API Gateway Role:** Implements a global security filter. It verifies the signature of the `Authorization` header. If valid, it extracts the claims and injects them into internal headers (e.g., `X-User-Id`, `X-User-Roles`) before routing the traffic.
* **Target Service Role:** Remains agnostic to JWT cryptography. The downstream service executes authorization logic solely based on the trusted internal headers provided by the Gateway.

### Pattern 2: Decentralized Validation (Service-Level Security)

In scenarios where the API Gateway functions purely as a pass-through proxy, validation shifts to the target service.

1. Add the necessary JWT processing dependencies (e.g., `io.jsonwebtoken:jjwt`) to the target service.
2. Propagate the symmetric or public `SECRET_KEY` utilized by the Auth Service to the target service's configuration.
3. Implement a Spring Security configuration or a custom interceptor within the target service to parse the `Authorization: Bearer <token>` header, validate the signature, and establish the security context dynamically.

## Standard Endpoint Specification

* `POST /auth/login`: Authenticates credentials and returns an active JWT payload.
* `POST /auth/register`: Provisions a new user identity within the system.
* `GET /auth/validate`: Internal endpoint designed to verify token integrity and return parsed claims.

*(Note: Public-facing endpoints must be explicitly routed through the API Gateway configuration to be accessible to external clients).*