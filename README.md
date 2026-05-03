# Banking Microservices Application

A full-stack banking application built with Angular and Spring Boot microservices.

## 📸 Application Screenshots

### Dashboard Overview

![Dashboard](./images/dashboard.png)
_Main dashboard showing account statistics and recent activity_

### Account & Transaction Management

![Account Transactions](./images/accountTransaction.png)
_Transaction history and management interface_

### Create New Account

![Create New Account](./images/createNewAccount.png)
_Account creation form with validation_

### Account Details

![Account Details](./images/accountDetails.png)
_Detailed view of account information_

### Updated Account Details After Transaction

![Updated Account Details](./images/updatedAccountDetailsAfterTransaction.png)
_Account details reflecting recent transactions_

## 🚀 Tech Stack

### Backend

- Java 23
- Spring Boot 3.2.4
- Spring Data JPA
- H2 Database (in-memory)
- Maven

### Frontend

- Angular 21 (Standalone Components)
- Bootstrap 5
- Font Awesome
- ngx-toastr for notifications

## 📋 Features

### Account Management

- Create new accounts with validation
- View all accounts in dashboard
- View individual account details
- Account types: SAVINGS and CHECKING
- Account status: ACTIVE, INACTIVE, FROZEN

### Transaction Operations

- Deposit money
- Withdraw money
- Real-time balance updates
- Transaction history
- Transaction summary (total deposits/withdrawals)

### User Interface

- Responsive dashboard with statistics
- Form validation with error messages
- Toast notifications for success/error
- Loading spinners for async operations
- Clean, professional design

## 🏗️ Project Structure

banking-microservices/
├── backend/
│ ├── account-service/ # Account microservice
│ │ ├── src/
│ │ │ ├── main/
│ │ │ └── test/
│ │ └── pom.xml
│ └── pom.xml # Parent POM
├── banking-ui/ # Angular frontend
│ ├── src/
│ ├── angular.json
│ └── package.json
├── images/ # Application screenshots
│ ├── dashboard.png
│ ├── accountTransaction.png
│ ├── createNewAccount.png
│ ├── accountDetails.png
│ └── updatedAccountDetailsAfterTransaction.png
└── README.md

## 🚦 Running the Application

### Prerequisites

- Java 23
- Maven 3.9+
- Node.js 18+
- Angular CLI 21

### Backend Setup

```bash
cd backend
mvn clean install
mvn spring-boot:run -pl account-service
Backend runs on: http://localhost:8081

### Frontend Setup
cd banking-ui
npm install
ng serve

Frontend runs on: http://localhost:4200

Database Console
URL: http://localhost:8081/h2-console

JDBC URL: jdbc:h2:mem:bankingdb

Username: sa

Password: (blank)

📱 API Endpoints
Account Service (/api/accounts)
Method	Endpoint	Description
GET	/	Get all accounts
GET	/{id}	Get account by ID
GET	/number/{accountNumber}	Get account by number
POST	/	Create new account
DELETE	/{id}	Delete account
GET	/status/{status}	Get accounts by status
Transaction Endpoints
Method	Endpoint	Description
POST	/{accountNumber}/deposit	Deposit money
POST	/{accountNumber}/withdraw	Withdraw money
POST	/transactions	Process unified transaction
Transaction Service (/api/transactions)
Method	Endpoint	Description
GET	/account/{accountNumber}	Get account transactions
GET	/account/{accountNumber}/type/{type}	Get by transaction type
GET	/account/{accountNumber}/summary	Get transaction summary
GET	/reference/{reference}	Get transaction by reference
🧪 Testing
Run backend tests:

bash
cd backend
mvn test
🤝 Contributing
Feel free to fork this project and submit pull requests!

📝 License
This project is for educational purposes.
```
