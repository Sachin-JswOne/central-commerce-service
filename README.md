# Central Commerce Service

## Overview
The **Central Commerce Service** is a core microservice for the JSW One e-commerce platform. It is built using Java 21 and Spring Boot 3.4.2, structured as a multi-module Maven project. This service orchestrates commerce operations, integrating with Commercetools, Google Cloud Platform (GCP), and various internal JSW services.

## Tech Stack
- **Java**: 21
- **Framework**: Spring Boot 3.4.2
- **Build Tool**: Maven
- **Cloud Provider**: Google Cloud Platform (GCP)
- **Database/Storage**: Google Cloud Firestore, Datastore, Elasticsearch
- **External Integrations**: Commercetools SDK (Java API)
- **API**: REST, GraphQL

## Project Structure
The project is divided into the following Maven modules:

| Module | Description |
|--------|-------------|
| **central-commerce-core** | Contains the core business logic, domain models, service interfaces, and implementations. Handles integrations with Commercetools and GCP. |
| **central-commerce-web** | The web layer containing REST controllers, GraphQL resolvers, exception handling, and input validation. |
| **central-commerce-worker** | Dedicated module for background jobs, asynchronous processing, and scheduled tasks. |
| **central-commerce-application** | The entry point of the application. It bundles the core, web, and worker modules to create the executable Spring Boot artifact. |

## Prerequisites
- **JDK 21**: Ensure Java 21 is installed and `JAVA_HOME` is configured.
- **Maven**: Maven 3.8+ is required for building the project.
- **GCP Credentials**: Valid Google Cloud credentials configured (e.g., `GOOGLE_APPLICATION_CREDENTIALS`).
- **Commercetools Credentials**: API Client credentials for your Commercetools project.

## Build and Run

### 1. Build the Project
Navigate to the project root and run:
```bash
mvn clean install
```

### 2. Run the Application
You can run the application directly using the Maven Spring Boot plugin from the root directory:
```bash
mvn spring-boot:run -pl central-commerce-application
```

Or run the packaged JAR file after building:
```bash
java -jar central-commerce-application/target/central-commerce-application-0.0.1-SNAPSHOT.jar
```

## Configuration
The application is configured using standard Spring Boot `application.properties` or `application.yml` files, typically located in `central-commerce-application/src/main/resources`.

Key configuration areas include:
- **Server Port**: `server.port`
- **Commercetools**: Project key, client ID, client secret, API URL, Auth URL.
- **GCP**: Project ID, Credentials file path.
- **Elasticsearch**: Connection URLs and credentials.
- **Internal Services**: Base URLs for other JSW microservices.

## Development
- **Code Style**: Conforms to standard Java 21 coding conventions.
- **Testing**: Uses JUnit 5 and Mockito. Run tests with `mvn test`.
