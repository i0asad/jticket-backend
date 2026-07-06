# JTicket Backend API

This is the Spring Boot backend service for the **JTicket Platform**, an advanced issue tracking and workflow management system.

## 🚀 Overview

The backend acts as a stateless REST API that securely handles all business logic, database transactions, and file operations for the JTicket platform.

### Core Architecture
- **Framework**: Spring Boot 3
- **Language**: Java 17
- **Database**: PostgreSQL (via Spring Data JPA / Hibernate)
- **Security**: Spring Security with Stateless JSON Web Tokens (JWT)
- **Cloud Storage**: Azure Blob Storage SDK

## 🛡️ Key Features

- **Stateless Authentication**: Fully decoupled JWT-based authentication system.
- **Strict Role-Based Access Control**: Multi-tier permission model restricting endpoints to `CLIENT`, `WORKER`, `MOD`, and `ADMIN` roles.
- **Azure Blob Storage Integration**: Secure, direct integration with Azure Blob Storage. The backend generates temporary Shared Access Signatures (SAS tokens) so authorized clients can download private ticket attachments securely.
- **Dual-Rating System**: Advanced algorithms and APIs to track the credibility scores of both workers and clients.
- **Automated CI/CD**: Seamless deployment to Azure App Service using GitHub Actions (OIDC).

## ⚙️ Environment Variables

To run the application, ensure the following environment variables are set (either locally or in your cloud provider's configuration):

| Variable | Description |
|---|---|
| `PGHOST` | PostgreSQL host URL |
| `PGPORT` | PostgreSQL port (e.g., 5432) |
| `PGDATABASE` | PostgreSQL database name |
| `PGUSER` | PostgreSQL username |
| `PGPASSWORD` | PostgreSQL password |
| `JWT_SECRET` | 256-bit secure secret for signing JWTs |
| `AZURE_STORAGE_CONNECTION_STRING` | Azure Blob Storage connection string |
| `AZURE_STORAGE_CONTAINER_NAME` | Container name for storing ticket attachments |
| `CORS_ALLOWED_ORIGINS` | Permitted frontend origins (e.g., `https://stjticket2026.z29.web.core.windows.net`) |
| `ADMIN_USERNAME` | Default admin username for system initialization |
| `ADMIN_EMAIL` | Default admin email |
| `ADMIN_PASSWORD` | Default admin password |

## 🚀 Deployment

The backend is configured to be deployed on **Azure App Service**. 
Continuous Deployment (CD) can be triggered by pushing to the `main` branch, which uses GitHub Actions and Azure's OpenID Connect (OIDC) integration to build the Maven package and deploy the `.jar` directly to Azure.

**API Live Endpoint**: `https://jticket-backend-api-h2fbcghnabbwh9a0.centralindia-01.azurewebsites.net/api`
