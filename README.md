# E-WALLET PROJECT

A project built with Microservices Architecture and organized as a Mono Repository with Package By Layer structure.

## 📋 Project Description

This is a modern e-wallet system built with microservices architecture. The system provides features for managing digital wallets, user authentication, profile management, bank transactions, and e-wallet transactions.

## 🏗️ Architecture

### Overall Architecture

- **Architecture**: Microservices Architecture
- **Structure**: Mono Repository
- **Organization Rule**: Package By Layer
- **Java Version**: 21
- **Build Tool**: Gradle

### Technologies Used

- **Framework**: Spring Boot 4.0.3, Spring Cloud 2025.1.0
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **Message Broker**: Apache Kafka 7.4.4
- **Authentication**: Keycloak
- **Service Discovery**: Spring Cloud Netflix Eureka
- **Containerization**: Docker & Docker Compose

## 🏢 Project Structure

```
ewallet-microservices/
├── libs/                          # Common libraries
│   └── common-library/            # Shared components
├── services/                      # Main microservices
│   ├── api-gateway/              # API Gateway (Port 8080)
│   ├── auth-service/             # Authentication Service (Port 8888)
│   ├── profile-service/          # Profile Management Service (Port 8082)
│   ├── bank-adapter-service/     # Bank Adapter Service (Port 8083)
│   ├── transaction-service/      # Transaction Service (Port 8084)
│   └── wallet-service/           # Wallet Service (Port 8085)
├── platforms/                    # Platform services
│   └── eureka-server/            # Service Discovery (Port 8761)
├── config/                       # Application configuration
├── deploy/                       # Deployment files
└── scripts/                      # Project scripts
```

## 🚀 Installation and Startup Guide

### System Requirements

- Java 21+
- Docker & Docker Compose
- Gradle (or use gradlew)
- Git

### Step 1: Clone Project

```bash
git clone <repository-url>
cd ewallet-microservices
```

## 🔨 How to Run Application

There are 2 ways to run the application: **Development Mode** and **Deployment Mode**

---

### 📝 **Development Mode**

This mode is for development and testing. Infrastructure services (PostgreSQL, Keycloak, Redis, Kafka) run through Docker, while Spring Boot microservices run directly from IDE or terminal.

#### Step 1: Start Infrastructure Services

```bash
docker-compose up -d
```

This command will start:

- PostgreSQL (Port 5432)
- Keycloak (Port 9999)
- Redis (Port 6379)
- Kafka (Port 9092, 19092)
- Kafka UI (Port 8080 - if configured)

#### Step 2: Create Databases

PostgreSQL needs separate databases for each service. Connect to PostgreSQL and create databases:

```bash
# Connect to PostgreSQL container
docker exec -it <postgres-container-id> psql -U admin -d postgres

# Or use this command from terminal
docker exec -it <postgres-container-id> psql -U admin -d postgres -c "
CREATE DATABASE wallet_db;
CREATE DATABASE profile_db;
CREATE DATABASE transaction_db;
CREATE DATABASE bank_adapter_db;
"
```

Or run each command separately:

```bash
docker exec -it <postgres-container-id> psql -U admin -d postgres -c "CREATE DATABASE wallet_db;"
docker exec -it <postgres-container-id> psql -U admin -d postgres -c "CREATE DATABASE profile_db;"
docker exec -it <postgres-container-id> psql -U admin -d postgres -c "CREATE DATABASE transaction_db;"
docker exec -it <postgres-container-id> psql -U admin -d postgres -c "CREATE DATABASE bank_adapter_db;"
```

**List of Databases to Create:**

| Database Name     | Used For              | Environment Variable      |
| ----------------- | --------------------- | ------------------------- |
| `wallet_db`       | Wallet Service        | `POSTGRES_WALLET_DB`      |
| `profile_db`      | Profile Service       | `POSTGRES_PROFILE_DB`     |
| `transaction_db`  | Transaction Service   | `POSTGRES_TRANSACTION_DB` |
| `bank_adapter_db` | Bank Adapter Service  | `POSTGRES_BANK_ADAPTER_DB`|
| `transaction_db`  | MongoDB (Transaction) | `MONGO_TRANSACTION_DB`    |

**Note**: These database names are defined in the project's `.env` file.

#### Step 3: Build Project

```bash
./gradlew build
```

Or on Windows:

```bash
gradlew.bat build
```

#### Step 4: Configure Keycloak Realm

After Keycloak starts, you need to import realm configuration:

1. Access Keycloak Admin Console: `http://localhost:9999`
2. Login with admin credentials
3. Import file `realm-export.json` (from project root)
4. This file will automatically create Realm, Clients, Roles, and Users for the project

See **🔐 Keycloak** section for details.

#### Step 5: Run All Microservices

Run all services at once using script:

```bash
bash scripts/run-all.sh dev
```

This script will start services in proper order (Eureka Server first, then other services).

#### Step 6: Verify Application is Running

- **Eureka Dashboard**: http://localhost:8761
- **API Gateway**: http://localhost:8080
- **Keycloak Admin**: http://localhost:9999

---

### 🚀 **Deployment Mode**

This mode is for production deployment. All services (infrastructure + microservices) run in Docker containers.

#### Step 1: Build Docker Images

```bash
cd deploy
docker-compose build
```

#### Step 2: Start All Systems

```bash
docker-compose up -d
```

Or if you want to see logs in real-time:

```bash
docker-compose up
```

This command will start all services including:

- PostgreSQL
- Keycloak
- Redis
- Kafka
- Eureka Server
- API Gateway
- Auth Service
- Profile Service
- Bank Adapter Service
- Transaction Service
- Wallet Service

#### Step 3: Create Databases (If Needed)

If databases were not created automatically, connect to PostgreSQL container and create them:

```bash
# Connect to PostgreSQL container
docker exec -it <postgres-container-id> psql -U admin -d postgres -c "
CREATE DATABASE wallet_db;
CREATE DATABASE profile_db;
CREATE DATABASE transaction_db;
CREATE DATABASE bank_adapter_db;
"
```

**Note**: In Deployment Mode, databases may be automatically created via init scripts. Check logs to confirm.

#### Step 4: Import Keycloak Realm Configuration

```bash
# Access Keycloak Admin Console
# http://localhost:9999

# Login with admin credentials
# Then import realm-export.json file from project root
```

#### Step 5: Verify Application is Running

```bash
# Check containers status
docker-compose ps

# View logs of specific service
docker-compose logs <service-name>

# Monitor logs in real-time
docker-compose logs -f <service-name>
```

#### Stop All Systems

```bash
# Stop and remove containers
docker-compose down

# Stop, remove containers and remove volumes (warning: data will be lost)
docker-compose down -v
```

---

## 📊 Comparison of 2 Modes

| Function           | Development                           | Deployment                          |
| ------------------ | ------------------------------------- | ----------------------------------- |
| Infrastructure     | Docker containers                     | Docker containers                   |
| Microservices      | Run from IDE/Terminal                 | Docker containers                   |
| Best for           | Development, testing                  | Production                          |
| Startup Speed      | Fast (only build needed services)     | Slower (build all images)           |
| Debugging          | Easy                                  | Difficult, use logs                 |
| Config File        | `application-dev.yaml`                | `application-docker.yaml`           |
| Startup Method     | `docker-compose up -d` + `run-all.sh` | `cd deploy && docker-compose up -d` |

## 📱 Microservices

| Service                  | Port | Description                                               |
| ------------------------ | ---- | --------------------------------------------------------- |
| **API Gateway**          | 8080 | Main gateway - single entry point for client requests     |
| **Auth Service**         | 8888 | User authentication and authorization management          |
| **Profile Service**      | 8082 | User profile management                                   |
| **Bank Adapter Service** | 8083 | Connection and interaction with banking systems           |
| **Transaction Service**  | 8084 | Transaction management                                    |
| **Wallet Service**       | 8085 | Digital wallet and balance management                     |
| **Eureka Server**        | 8761 | Service Discovery                                         |

## 🔧 Configuration

### Application Configuration

Each microservice has 2 configuration files located in `src/main/resources/`:

#### 1. **application-dev.yaml** (Development Mode)

Used when running services from IDE or terminal in Development Mode.

- Connect to PostgreSQL, Keycloak, Redis, Kafka on `localhost`
- Detailed logs for debugging support
- Configuration suitable for development

#### 2. **application-docker.yaml** (Deployment Mode)

Used when running services in Docker containers from `deploy/docker-compose.yaml`.

- Connect to PostgreSQL, Keycloak, Redis, Kafka via Docker network (container names)
- Less detailed logs, optimized for production
- Configuration suitable for deployment

#### How It Works

**Development Mode:**

```bash
# Spring Boot will automatically use application-dev.yaml file
bash scripts/run-all.sh dev
```

**Deployment Mode:**

```bash
# Docker will use application-docker.yaml via SPRING_PROFILES_ACTIVE=docker
cd deploy && docker-compose up -d
```

Reference configuration files are also available in `config/config-repo/`.

See `config.md` for details about ports and configuration.

### Environment Variables Configuration (.env)

All environment variables are defined in `.env` file at project root.

**.env file contains:**

```env
# PostgreSQL Configuration
POSTGRES_DB=keycloak_db              # Main database for Keycloak
POSTGRES_USER=admin                  # PostgreSQL username
POSTGRES_PASSWORD=admin              # PostgreSQL password

# Keycloak Configuration
KEYCLOAK_ADMIN=admin                 # Keycloak admin username
KEYCLOAK_ADMIN_PASSWORD=admin        # Keycloak admin password
KEYCLOAK_CLIENT_SECRET=...           # OAuth2 client secret

# Database Specific Configurations (for each service)
POSTGRES_WALLET_DB=wallet_db                   # Wallet Service DB
POSTGRES_PROFILE_DB=profile_db                 # Profile Service DB
POSTGRES_TRANSACTION_DB=transaction_db         # Transaction Service DB
POSTGRES_BANK_ADAPTER_DB=bank_adapter_db       # Bank Adapter Service DB
MONGO_TRANSACTION_DB=transaction_db            # MongoDB Transaction DB
```

**How to Use:**

- Docker Compose will automatically load variables from `.env` file
- Microservices will read from these environment variables via `application-dev.yaml` or `application-docker.yaml`
- To change credentials or database names, edit `.env` file and restart services

**⚠️ Security Note:**

- Do not commit `.env` file to repository (add `.env` to `.gitignore`)
- Change default password (`admin`) in production environment
- Keep `KEYCLOAK_CLIENT_SECRET` safe in production

## 🗄️ Database

The project uses PostgreSQL as the main database. Migrations and schema are managed through tools like Flyway or Liquibase (if configured).

### Databases to Create

Each microservice has its own database:

| Database Name     | Used For             | Environment Variable      | Note                      |
| ----------------- | -------------------- | ------------------------- | ------------------------- |
| `wallet_db`       | Wallet Service       | `POSTGRES_WALLET_DB`      | User wallet management    |
| `profile_db`      | Profile Service      | `POSTGRES_PROFILE_DB`     | User profile management   |
| `transaction_db`  | Transaction Service  | `POSTGRES_TRANSACTION_DB` | Transaction history       |
| `bank_adapter_db` | Bank Adapter Service | `POSTGRES_BANK_ADAPTER_DB`| Bank connection           |
| `keycloak_db`     | Keycloak             | `POSTGRES_DB`             | Authentication & authz    |

**MongoDB (Optional):**

- `transaction_db` (MongoDB) | Transaction Service (Alternative) | `MONGO_TRANSACTION_DB`

### Starting PostgreSQL

PostgreSQL is automatically started via Docker Compose:

```bash
docker-compose up -d postgres
```

## 🔐 Keycloak

Keycloak is used for OAuth2/OIDC authentication.

### Access Keycloak Admin Console

```
http://localhost:9999
```

Login with credentials from `KEYCLOAK_ADMIN` and `KEYCLOAK_ADMIN_PASSWORD` environment variables.

### Configure Realm and Roles

The project uses `realm-export.json` file to configure Realm and Roles for Keycloak.

#### Guide to Import Realm from `realm-export.json` File

1. **Access Keycloak Admin Console**
   - Open: `http://localhost:9999`
   - Login with admin account

2. **Import Realm Configuration**
   - On main page, find **"Add Realm"** section or click realm dropdown menu
   - Select **"Import"** option or upload file
   - Choose `realm-export.json` file from project root

3. **Apply Configuration**
   - `realm-export.json` file will automatically create:
     - New Realm for project
     - Clients (Applications) for each service
     - Users, Roles, and Permissions
     - OAuth2/OIDC configuration

4. **Verify Configuration**
   - Check if Realm was created successfully
   - View list of Clients and Roles in Admin Console
   - Services will automatically connect to Keycloak on startup

#### `realm-export.json` File

This file contains:

- Realm configuration
- Client configurations (for API Gateway, Auth Service, etc.)
- User roles and permissions
- OAuth2/OIDC settings

**Location**: `/realm-export.json` (in project root)

## 🔄 Message Broker (Kafka)

Kafka is used for asynchronous messaging between services.

- **Bootstrap Servers**: `kafka:9092` (from inside container)
- **Bootstrap Servers**: `localhost:19092` (from outside)

## ⚙️ Cache (Redis)

Redis is used for caching:

- **Host**: localhost
- **Port**: 6379

## 📊 Monitoring & Troubleshooting

### Viewing Logs

#### **Development Mode**

```bash
# View logs of specific infrastructure service
docker-compose logs <service-name>

# Monitor logs in real-time
docker-compose logs -f <service-name>

# View logs of Spring Boot services (from terminal where script runs)
# or view in IDE
```

#### **Deployment Mode**

```bash
# Go to deploy folder
cd deploy

# View logs of specific service
docker-compose logs <service-name>

# Monitor logs in real-time
docker-compose logs -f <service-name>

# View logs of all services
docker-compose logs
```

### Eureka Dashboard

Access at: `http://localhost:8761`

This dashboard shows all services currently running and ready.

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Check Container Status (Deployment Mode)

```bash
cd deploy
docker-compose ps
```

## 🛠️ Development

### Adding Dependencies

Edit `build.gradle` or service's `build.gradle` file, then:

```bash
./gradlew build
```

### Unit Testing

```bash
./gradlew test
```

## 📦 Docker Deployment

### Run Application in Deployment Mode

See **Deployment Mode** section above for detailed deployment guide.

### Build Docker Images (Deployment Mode)

```bash
cd deploy
docker-compose build
```

### Run with Docker (Deployment Mode)

```bash
cd deploy
docker-compose up -d
```

Or view logs in real-time:

```bash
cd deploy
docker-compose up
```

### Stop Services (Deployment Mode)

```bash
cd deploy
docker-compose down
```

### Remove Volumes (Deployment Mode)

```bash
cd deploy
docker-compose down -v
```

**⚠️ Warning**: This command will delete all data!

### View Container Details (Deployment Mode)

```bash
cd deploy
docker-compose ps
docker-compose logs -f <service-name>
```

## 🔗 API Endpoints

### ⚠️ API Access Mode

**API can only be accessed when running application in Development Mode (Dev).**

#### Development Mode - APIs Available ✅

When running in Development Mode, all API endpoints are available via API Gateway:

```
http://localhost:8080/
```

**Example Endpoints:**

```
GET  http://localhost:8080/wallet/balance              # Get wallet balance
GET  http://localhost:8080/profile/user/{id}          # Get user information
GET  http://localhost:8080/transactions/history       # Transaction history
POST http://localhost:8080/auth/login                 # Login
```

#### Deployment Mode - APIs Not Available ❌

When running in Deployment Mode (production), API endpoints are **NOT** exposed outside. Access only through:

- Internal service-to-service communication (in Docker network)
- API Gateway (if configured separately)

### How to Access APIs

#### **1. In Development Mode - Via API Gateway**

Open terminal and run:

```bash
# Terminal 1: Start infrastructure
docker-compose up -d

# Terminal 2: Start services
bash scripts/run-all.sh dev
```

Then access APIs:

```bash
# Example: Check gateway health
curl http://localhost:8080/actuator/health

# Example: Call Wallet Service API
curl http://localhost:8080/wallet/balance
```

#### **2. View Swagger UI (Only in Dev Mode)**

If project is configured with Swagger/SpringFox:

```
http://localhost:8080/swagger-ui.html
```

#### **3. Using Postman or Insomnia**

- Base URL: `http://localhost:8080/`
- Path prefix: `/wallet/**`, `/profile/**`, `/transaction/**`, etc.
- Add authorization header if needed (OAuth2/JWT token from Keycloak)
- Example header:
  ```
  Authorization: Bearer <your-jwt-token>
  ```
- Example request: `GET http://localhost:8080/wallet/balance`

### API Gateway Routes

API Gateway routes requests to corresponding services based on path:

| Path Prefix        | Routed To            | Port |
| ------------------ | -------------------- | ---- |
| `/wallet/**`       | Wallet Service       | 8085 |
| `/profile/**`      | Profile Service      | 8082 |
| `/transaction/**`  | Transaction Service  | 8084 |
| `/bank-adapter/**` | Bank Adapter Service | 8083 |
| `/auth/**`         | Auth Service         | 8888 |

### Notes About API Availability

- 🟢 **Development Mode**: All APIs available, no authentication required (or with mock authentication)
- 🔴 **Deployment Mode**: APIs only available internally, not exposed outside
- 📝 **Production**: Need to configure API Gateway separately with security rules, rate limiting, etc.

## 📝 Conventions

- **Java Version**: 21
- **Naming Convention**: camelCase for methods/variables, PascalCase for classes
- **Package Structure**: Package by Layer
- **Configuration**: YAML configuration files in each service

## 🤝 Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License.

## 📞 Support

If you encounter issues or have questions, please:

1. Check `config.md` file for configuration details
2. Create an issue on repository
3. Contact development team

## 📌 Important Notes

### Application Configuration Files

**🔴 IMPORTANT**: Each microservice has 2 different configuration files:

- **`application-dev.yaml`**: For Development Mode (connect to localhost)
- **`application-docker.yaml`**: For Deployment Mode (connect via Docker network)

Make sure to edit the correct configuration file based on your mode. Example:

```
services/wallet-service/src/main/resources/
  ├── application-dev.yaml      # Development Mode
  └── application-docker.yaml   # Deployment Mode
```

### Keycloak & Realm Configuration

**🔴 IMPORTANT**: Must import `realm-export.json` file into Keycloak:

- This file contains all Realm configuration, Clients, Roles, and Users
- **Step 1**: Access http://localhost:9999
- **Step 2**: Login with admin account
- **Step 3**: Import `realm-export.json` file from project root
- **Step 4**: File will automatically create Realm and configure OAuth2/OIDC for services

See **🔐 Keycloak** section for details.

**Important Notes**:

- Realm must be configured before services connect
- If services cannot authenticate, check if Realm was imported successfully
- Use Admin Console to verify Clients and Roles exist

### Database & PostgreSQL

**🔴 IMPORTANT**: Databases must be created before services start:

**Databases to Create:**

- `wallet_db` - Wallet Service
- `profile_db` - Profile Service
- `transaction_db` - Transaction Service
- `bank_adapter_db` - Bank Adapter Service
- `keycloak_db` - Keycloak (created automatically)

**How to Create Databases:**

```bash
# From terminal
docker exec -it <postgres-container-id> psql -U admin -d postgres -c "
CREATE DATABASE wallet_db;
CREATE DATABASE profile_db;
CREATE DATABASE transaction_db;
CREATE DATABASE bank_adapter_db;
"
```

**Troubleshooting:**

- If error "database already exists", database was created before
- Check container name: `docker ps | grep postgres`
- Confirm credentials in `.env` file
- If PostgreSQL container not ready yet, wait a moment and retry

### Environment Variables (.env)

**🔴 IMPORTANT**: `.env` file defines all environment variables:

- **POSTGRES_DB**, **POSTGRES_USER**, **POSTGRES_PASSWORD**: Main PostgreSQL credentials
- **POSTGRES_WALLET_DB**, **POSTGRES_PROFILE_DB**, etc.: Database for each service
- **KEYCLOAK_ADMIN**, **KEYCLOAK_ADMIN_PASSWORD**: Keycloak credentials
- **KEYCLOAK_CLIENT_SECRET**: OAuth2 client secret

**Security Rules:**

- ⚠️ Do not commit `.env` file to repository
- 🔒 Change default passwords in production
- 🔐 Keep `KEYCLOAK_CLIENT_SECRET` secret

### General

- Ensure all Docker containers are running before starting services
- Eureka Server must be ready for services to register
- Check logs if services cannot connect to each other

### Development Mode

- Ensure Ports 8080, 8082-8085, 8761, 8888 are not used by other applications
- If port is in use, change port in service's `application-dev.yaml` file
- Script `run-all.sh dev` automatically loads `dev` profile and uses `application-dev.yaml`
- To stop services, press `Ctrl+C` in each terminal or run kill process command

### Deployment Mode

- All services run in containers, no need to worry about port conflicts on host machine
- Docker Compose automatically sets `SPRING_PROFILES_ACTIVE=docker` to load `application-docker.yaml`
- Environment variables managed via `.env` file or in `docker-compose.yaml` (in deploy folder)
- To update code, rebuild images: `cd deploy && docker-compose build && docker-compose up -d`
- Monitor logs regularly: `cd deploy && docker-compose logs -f`
