# Banking Microservices Application

A production-deployed full-stack banking application built with **Spring Boot** and **Angular**. The system includes **JWT authentication**, **role-based access control**, account management, transaction processing, analytics dashboard, CSV export, PostgreSQL production database, and cloud deployment using **Render** and **Vercel**.

---

## 🌐 Live Demo

- **Frontend Live URL:** https://banking-microservices-eta.vercel.app/login
- **Backend API URL:** https://banking-api-r5w3.onrender.com

> Note: The backend is hosted on Render free tier, so the first request may take some time if the server is sleeping.

---

## 📌 Project Overview

This project is a secure banking management system where users can register, log in, create bank accounts, perform deposits and withdrawals, view transaction history, export transaction data, and analyze financial activity through a modern Angular dashboard.

The application supports two roles:

- **CUSTOMER**: Can manage and view their own accounts and transactions.
- **ADMIN**: Can view all accounts, manage account status, delete accounts, and access administrative controls.

The backend follows a layered Spring Boot architecture, while the frontend uses Angular standalone components, route guards, HTTP interceptors, reusable services, and responsive UI design.

---

## 📸 Application Screenshots

### Dashboard Overview

![Dashboard](./images/dashboard.png)  
_Main dashboard showing account statistics and recent banking activity_

### Account & Transaction Management

![Account Transactions](./images/accountTransaction.png)  
_Transaction history and account management interface_

### Create New Account

![Create New Account](./images/createNewAccount.png)  
_Account creation form with validation_

### Account Details

![Account Details](./images/accountDetails.png)  
_Detailed account information with transaction actions_

### Updated Account Details After Transaction

![Updated Account Details](./images/updatedAccountDetailsAfterTransaction.png)  
_Account details reflecting updated balance after transaction_

### Analytics Dashboard

![Analytics](./images/analytics.png)  
_Analytics dashboard with charts and financial summaries_

---

## 🚀 Tech Stack

### Backend

- Java 21
- Spring Boot 3.2.4
- Spring Security
- JWT Authentication
- BCrypt Password Encryption
- Spring Data JPA
- Hibernate
- H2 Database for development
- PostgreSQL for production
- Maven
- JUnit / Mockito
- Docker
- Render Deployment

### Frontend

- Angular 21
- TypeScript
- Angular Standalone Components
- Angular Router
- Angular Route Guards
- HTTP Interceptor
- Reactive Forms
- Bootstrap 5
- Font Awesome
- ngx-toastr
- Chart.js
- Vercel Deployment

---

## 📋 Features

## Authentication & Security

- User registration
- Admin registration with configured admin key
- User login with username and password
- JWT token generation and validation
- BCrypt password encryption
- Role-based authorization
- CUSTOMER and ADMIN roles
- Angular Auth Guard for protected routes
- Angular HTTP Interceptor for automatic JWT token injection
- Logout functionality
- Password visibility toggle
- Email validation on registration
- CORS configuration for frontend-backend communication

---

## Account Management

- Create new bank accounts
- View account list
- View account by ID
- View account by account number
- Delete accounts
- Freeze and activate accounts
- Role-based account visibility
  - Admin can view all accounts
  - Customer can view only their own accounts
- Account types:
  - SAVINGS
  - CHECKING
- Account statuses:
  - ACTIVE
  - FROZEN
  - INACTIVE

---

## Transaction Operations

- Deposit money
- Withdraw money
- Process transactions using unified endpoint
- Track transaction history
- View transaction summary
- Calculate total deposits
- Calculate total withdrawals
- Calculate net balance
- Record failed transactions
  - Insufficient balance
  - Frozen account
  - Invalid transaction attempts
- Filter transactions by:
  - Date
  - Type
  - Amount
- Export transaction history to CSV

---

## Analytics Dashboard

The analytics dashboard provides visual insights into banking activity.

### Charts Included

- **Bar Chart**: Monthly deposits vs withdrawals
- **Pie Chart**: Deposit and withdrawal distribution
- **Line Chart**: Balance history over recent transactions
- **Stats Cards**:
  - Total deposits
  - Total withdrawals
  - Net balance
  - Transaction summary

---

## User Interface Features

- Login page with validation
- Registration page with validation
- Password show/hide toggle
- Dashboard with statistics cards
- Account list table
- Account details page
- Create account form
- Deposit and withdrawal forms
- Transaction history table
- Analytics dashboard
- CSV export option
- Toast notifications
- Loading spinners
- Responsive design
- Professional gradient styling
- Shared navbar component

---

## Backend API Endpoints

### Authentication API

Base URL:

```text
/api/auth
Method	Endpoint	Description
POST	/register	Register a customer user
POST	/register-admin	Register an admin user using configured admin key
POST	/login	Login user and return JWT token
Account API

Base URL:

/api/accounts
Method	Endpoint	Description
GET	/	Get accounts based on user role
POST	/	Create a new bank account
GET	/{id}	Get account by ID
GET	/number/{accountNumber}	Get account by account number
DELETE	/{id}	Delete an account
GET	/status/{status}	Get accounts by account status
POST	/{accountNumber}/deposit	Deposit money into account
POST	/{accountNumber}/withdraw	Withdraw money from account
POST	/transactions	Process unified transaction
PUT	/{accountNumber}/status	Update account status
Transaction API

Base URL:

/api/transactions
Method	Endpoint	Description
GET	/account/{accountNumber}	Get transaction history by account number
GET	/account/{accountNumber}/type/{type}	Get transactions by transaction type
GET	/account/{accountNumber}/summary	Get transaction summary
GET	/reference/{reference}	Get transaction by reference number

🔐 User Roles
Role	Access
CUSTOMER	View own accounts, create account, deposit, withdraw, view own transactions, export transactions, view analytics
ADMIN	View all accounts, freeze accounts, activate accounts, delete accounts, view transactions, manage account status, view analytics

🏗️ Project Structure
banking-microservices/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── account-service/
│       ├── pom.xml
│       └── src/
│           ├── main/
│           │   ├── java/com/banking/account/
│           │   │   ├── AccountServiceApplication.java
│           │   │   ├── config/
│           │   │   │   ├── CorsConfig.java
│           │   │   │   ├── DataLoader.java
│           │   │   │   └── SecurityConfig.java
│           │   │   ├── controller/
│           │   │   │   ├── AccountController.java
│           │   │   │   ├── AuthController.java
│           │   │   │   └── TransactionController.java
│           │   │   ├── dto/
│           │   │   │   ├── AccountDTO.java
│           │   │   │   ├── ApiResponse.java
│           │   │   │   ├── AuthRequest.java
│           │   │   │   ├── AuthResponse.java
│           │   │   │   ├── TransactionDTO.java
│           │   │   │   ├── TransactionHistoryRequest.java
│           │   │   │   └── TransactionRequest.java
│           │   │   ├── entity/
│           │   │   │   ├── Account.java
│           │   │   │   ├── Transaction.java
│           │   │   │   └── User.java
│           │   │   ├── repository/
│           │   │   │   ├── AccountRepository.java
│           │   │   │   ├── TransactionRepository.java
│           │   │   │   └── UserRepository.java
│           │   │   ├── security/
│           │   │   │   ├── CustomUserDetailsService.java
│           │   │   │   ├── JwtAuthenticationFilter.java
│           │   │   │   └── JwtService.java
│           │   │   └── service/
│           │   └── resources/
│           │       ├── application.properties
│           │       ├── application-dev.properties
│           │       └── application-prod.properties
│           └── test/java/com/banking/account/
│               ├── controller/
│               │   ├── AccountControllerTest.java
│               │   └── TransactionControllerTest.java
│               └── repository/
│                   └── AccountRepositoryTest.java
│
├── banking-ui/
│   ├── angular.json
│   ├── package.json
│   ├── package-lock.json
│   ├── proxy.conf.json
│   ├── vercel.json
│   ├── tsconfig.json
│   └── src/
│       ├── app/
│       │   ├── app.routes.ts
│       │   ├── app.config.ts
│       │   ├── components/
│       │   │   ├── auth/
│       │   │   │   ├── login.component.ts
│       │   │   │   ├── login.component.html
│       │   │   │   ├── login.component.scss
│       │   │   │   ├── register.component.ts
│       │   │   │   ├── register.component.html
│       │   │   │   └── register.component.scss
│       │   │   ├── dashboard/
│       │   │   │   ├── dashboard.component.ts
│       │   │   │   ├── dashboard.component.html
│       │   │   │   └── dashboard.component.scss
│       │   │   ├── account/
│       │   │   │   ├── create-account.component.ts
│       │   │   │   ├── create-account.component.html
│       │   │   │   ├── create-account.component.scss
│       │   │   │   ├── account-detail.component.ts
│       │   │   │   ├── account-detail.component.html
│       │   │   │   └── account-detail.component.scss
│       │   │   ├── analytics/
│       │   │   │   ├── analytics.component.ts
│       │   │   │   ├── analytics.component.html
│       │   │   │   └── analytics.component.scss
│       │   │   └── shared/
│       │   │       └── navbar/
│       │   │           ├── navbar.component.ts
│       │   │           ├── navbar.component.html
│       │   │           └── navbar.component.scss
│       │   ├── guards/
│       │   │   └── auth.guard.ts
│       │   ├── interceptors/
│       │   │   └── auth.interceptor.ts
│       │   ├── models/
│       │   │   ├── account.model.ts
│       │   │   ├── api-response.model.ts
│       │   │   └── transaction.model.ts
│       │   └── services/
│       │       ├── account.service.ts
│       │       ├── auth.service.ts
│       │       ├── export.service.ts
│       │       └── notification.service.ts
│       └── environments/
│           ├── environment.ts
│           └── environment.prod.ts
│
├── images/
│   ├── dashboard.png
│   ├── accountTransaction.png
│   ├── createNewAccount.png
│   ├── accountDetails.png
│   ├── updatedAccountDetailsAfterTransaction.png
│   └── analytics.png
│
└── README.md

🚦 Running the Application Locally
Prerequisites

Make sure the following tools are installed:

Java 21
Maven 3.9+
Node.js 18+
Angular CLI 21
Docker, optional
Backend Setup

Navigate to the backend directory:

cd backend

Build the backend:

mvn clean install

Run the backend:

mvn spring-boot:run -pl account-service

Backend runs on:

http://localhost:8081
Run Backend with Development Profile

The development profile uses H2 in-memory database.

mvn spring-boot:run -pl account-service -Dspring-boot.run.profiles=dev
Run Backend with Production Profile

The production profile is configured for PostgreSQL.

mvn spring-boot:run -pl account-service -Dspring-boot.run.profiles=prod
H2 Database Console

When using the development profile, the H2 console is available at:

http://localhost:8081/h2-console

Default H2 configuration:

JDBC URL: jdbc:h2:mem:bankingdb
Username: sa
Password:
Frontend Setup

Navigate to the Angular project:

cd banking-ui

Install dependencies:

npm install

Run the frontend:

ng serve

Frontend runs on:

http://localhost:4200
Frontend Proxy Configuration

For local development, Angular can connect to the Spring Boot backend using proxy.conf.json.

Example local backend target:

http://localhost:8081
Environment Configuration
Local Frontend Environment

Update:

banking-ui/src/environments/environment.ts

Example:

export const environment = {
  production: false,
  apiUrl: 'http://localhost:8081/api'
};
Production Frontend Environment

Update:

banking-ui/src/environments/environment.prod.ts

Example:

export const environment = {
  production: true,
  apiUrl: 'https://banking-api-r5w3.onrender.com/api'
};

🌐 Deployment
Backend Deployment

The backend is deployed on Render:

https://banking-api-r5w3.onrender.com

The production backend uses PostgreSQL.

Render free tier may put the backend to sleep when inactive. The first request after inactivity may take a few seconds.

Frontend Deployment

The frontend is deployed on Vercel:

https://banking-microservices-eta.vercel.app/login

The frontend uses the production backend API hosted on Render.

🧪 Testing

Run backend tests:

cd backend
mvn test

Test coverage includes:

Account controller tests
Transaction controller tests
Account repository tests

🐳 Docker

The backend includes a Dockerfile.

Build the backend Docker image:

cd backend
docker build -t banking-account-service .

Run the container:

docker run -p 8081:8081 banking-account-service

Backend will be available at:

http://localhost:8081

🧰 Useful Commands
Kill Process Running on Port 8081

For Windows CMD:

netstat -ano | findstr :8081
taskkill /PID YOUR_PID /F

For PowerShell:

Get-Process -Id (Get-NetTCPConnection -LocalPort 8081).OwningProcess | Stop-Process -Force

📊 Project Metrics
Metric	Value
Backend Endpoints	15+
Frontend Components	7+
Database Tables	3
User Roles	2
Charts Implemented	3
Deployment Platforms	Render + Vercel
Backend Database	H2 + PostgreSQL

🏆 Skills Demonstrated
Backend Development
Spring Boot application development
REST API design
Spring Security configuration
JWT authentication
Role-based authorization
BCrypt password encryption
JPA/Hibernate ORM
Repository pattern
DTO-based request/response handling
Maven multi-module structure
Environment-based configuration
Controller and repository testing
Frontend Development
Angular standalone components
TypeScript
Reactive forms
Route guards
HTTP interceptors
Reusable services
Bootstrap-based responsive UI
Toast notifications
Loading states
Chart.js data visualization
CSV export functionality
DevOps & Deployment
Git version control
GitHub repository management
Render backend deployment
Vercel frontend deployment
PostgreSQL production database
Docker support
Environment-specific configuration

🔒 Security Notes
Passwords are encrypted using BCrypt.
JWT is used for stateless authentication.
Protected endpoints require valid JWT tokens.
Role-based access is enforced for CUSTOMER and ADMIN users.
Admin registration requires a configured admin key.
Sensitive credentials should be stored using environment variables in production.

📌 Future Improvements
Add Swagger/OpenAPI documentation
Add service-layer unit tests
Add pagination and sorting for accounts and transactions
Add advanced role and permission management
Add email notifications for transactions
Add two-factor authentication
Add real-time notifications using WebSocket
Add CI/CD pipeline using GitHub Actions
Add PDF export for transaction reports
Add Docker Compose for backend and database
Add separate microservices such as user-service, notification-service, and loan-service
Add mobile app using Ionic, React Native, or Flutter

🤝 Contributing

Contributions, issues, and feature requests are welcome.

Feel free to fork this repository and submit pull requests.

📝 License

This project is for educational and portfolio purposes.
```
