SIP Goal Tracker 💰📈🚀
Your personalized financial planning companion to achieve dreams through Systematic Investment Plans.

Table of Contents
About the Project

Features

Technologies Used

Architecture

Getting Started

Prerequisites

Backend Setup

Frontend Setup

API Endpoints

Deployment on AWS

License

Contact

About The Project
The SIP Goal Tracker is a robust financial planning application designed to help users set and achieve their monetary dreams through Systematic Investment Plans (SIPs). Users can define their financial goals (e.g., buying property, funding education), specify a time horizon, and the application will intelligently calculate the optimal monthly investment required. It also provides dynamic "what-if" scenarios, allowing users to see projected future values based on custom monthly investments. For actionable insights, the system suggests investment schemes based on a diversified allocation strategy and provides comprehensive PDF reports via download or email.

This project focuses on a well-structured backend API built with Spring Boot, a scalable PostgreSQL database, and integration with AWS Lambda for complex financial calculations, ensuring high performance and maintainability.

Features
User Management: Register and retrieve user profiles (name, email).

Goal Definition: Users can specify their financial "dream type" (e.g., Education, Retirement, Foreign Trip), a target "Goal Value" (Future Value), and an investment "Time Period" (1, 2, 3, 4, or 5 years).

Dynamic SIP Calculation:

SIP from FV: Calculates the optimal monthly SIP amount required to achieve a specific future value goal.

FV from SIP ("What-If"): Calculates the projected future value based on a user-defined monthly investment amount.

Intelligent Scheme Suggestions: Provides actionable investment advice by suggesting the best scheme within each category (Bank Savings, Mutual Funds, Equity/Gold) and allocating the optimal monthly SIP according to a 50-30-20 principle.

Calculation Caching: Optimizes performance by caching calculation results for recurring (futureValue, timePeriodYears) combinations, reducing redundant AWS Lambda invocations.

Comprehensive PDF Reports: Generates detailed PDF reports summarizing user inputs, calculation results, and investment scheme suggestions.

Email Delivery: Allows users to receive their personalized PDF reports directly to their email inbox.

Scalable Architecture: Designed with a microservices-like approach leveraging AWS Lambda for computations and a managed database.

Technologies Used
This project is built with a modern, robust tech stack:

Backend
Spring Boot: 3.2.5 (Java 17) - Framework for building RESTful APIs.

Maven: 3.8+ - Dependency management and build automation.

Spring Data JPA: For seamless interaction with the PostgreSQL database.

Spring Web: For building RESTful endpoints.

Spring Mail: For sending emails with attachments.

Jackson: For JSON serialization/deserialization, including jackson-datatype-jsr310 for Java 8 Date/Time API handling.

iText 7: For generating PDF documents.

Database
PostgreSQL: 15+ - Robust, open-source relational database.

Cloud Infrastructure
AWS Lambda: Serverless compute for complex SIP/FV calculations.

Amazon RDS: Managed PostgreSQL database service.

AWS Elastic Beanstalk: Platform-as-a-Service for deploying and scaling the Spring Boot backend.

Amazon S3: Object storage for hosting the static React frontend.

Amazon CloudFront: Content Delivery Network (CDN) for fast and secure frontend delivery.

Frontend (Conceptual - Backend Support Ready)
React: 18+ - JavaScript library for building user interfaces.

axios / fetch API: For making HTTP requests to the backend.

Architecture
The project follows a layered architecture with clear separation of concerns, leveraging cloud services for scalability and efficiency:

Code snippet

graph TD
    A[React Frontend] --> B(Spring Boot Backend);
    B --> C(PostgreSQL Database RDS);
    B --> D(AWS Lambda);

    subgraph AWS Cloud
        C;
        D;
        E(Elastic Beanstalk);
        F(S3 + CloudFront);
    end

    A --- F;
    B --- E;

    style A fill:#f9f,stroke:#333,stroke-width:2px
    style B fill:#bbf,stroke:#333,stroke-width:2px
    style C fill:#ccf,stroke:#333,stroke-width:2px
    style D fill:#fcf,stroke:#333,stroke-width:2px
    style E fill:#aaf,stroke:#333,stroke-width:2px
    style F fill:#ddf,stroke:#333,stroke-width:2px

    %% Detailed Interactions
    B -- API Calls (REST) --> C;
    B -- Invoke (HTTP POST) --> D;
    F -- Serves Static Files --> A;
    E -- Hosts Backend App --> B;

    subgraph Backend Data Flow
        direction LR
        User_Input[User Input (FV/SIP, Years)] --> Backend_Service(UserRequestService);
        Backend_Service -- Check Cache --> DB_User_Requests[DB: user_requests];
        DB_User_Requests -- Found? --> Backend_Service;
        Backend_Service -- Not Found --> Lambda_Calc[AWS Lambda Calculation];
        Lambda_Calc --> Backend_Service;
        Backend_Service -- Store Results --> DB_Goal_Calculations[DB: goal_calculations];
        DB_Goal_Calculations -- Retrieve Schemes --> DB_Investment_Schemes[DB: investment_scheme_rates];
        DB_Investment_Schemes --> Backend_Service;
        Backend_Service -- Return to UI --> UI_Display[UI: Display Results];
    end
React Frontend: Provides the interactive user interface.

Spring Boot Backend: Acts as the API gateway, orchestrating business logic, data persistence, and external calls. It implements a custom caching mechanism by querying the user_requests and goal_calculations tables before invoking Lambda for repeated calculations.

AWS Lambda: A serverless function dedicated to performing the complex financial calculations (SIP from FV, FV from SIP) based on pre-calculated weighted average rates.

PostgreSQL Database (RDS): Stores user profiles (users), logs each user request/transaction (user_requests), stores the calculation results for each request (goal_calculations), and holds the static investment scheme data (investment_scheme_rates).

Email Service: Integrated into the backend to send PDF reports.

Getting Started
To get a local copy up and running, follow these simple steps.

Prerequisites
Java 17+ (JDK installed and configured)

Maven 3.8+

PostgreSQL 15+ (running locally or accessible)

Node.js & npm / yarn (for the React frontend)

AWS Account (for Lambda deployment)

Postman / Insomnia (for API testing)

Backend Setup
Clone the repository:

Bash

git clone https://github.com/your-username/your-repo-name.git
cd sip-goal-tracker
Database Setup (PostgreSQL):

Ensure your PostgreSQL server is running.

Create a new database (e.g., sip_tracker_db).

Execute the following SQL script in your PostgreSQL client (e.g., pgAdmin Query Tool) to create all necessary tables and indexes:

SQL

-- SQL script to create tables (users, user_requests, goal_calculations, investment_scheme_rates)
-- (Full SQL script available in project documentation or previous conversation)
(Note: The full SQL script is quite long. You can paste it from our previous conversation or a dedicated schema.sql file in your repo.)

AWS Lambda Deployment:

Build Lambda JAR:

Navigate to the sip-lambda-calculator project (or the Lambda module if it's part of this repo).

Run mvn clean install to build the fat JAR.

Deploy to AWS Lambda:

Go to AWS Lambda Console.

Create a new function (Runtime: Java 17, Handler: com.example.sipcalculator.SipCalculatorLambda::handleRequest).

Upload the built JAR.

Create API Gateway:

Create a REST API (or HTTP API) that triggers your Lambda function.

Note down the API Gateway Invoke URL.

Backend application.properties Configuration:

Open src/main/resources/application.properties.

Update Database Credentials:

Properties

spring.datasource.url=jdbc:postgresql://localhost:5432/sip_tracker_db
spring.datasource.username=your_postgres_user
spring.datasource.password=your_postgres_password
Update AWS Lambda API URL:

Properties

aws.lambda.sip.api.url=YOUR_API_GATEWAY_INVOKE_URL
Update Email SMTP Configuration:

Properties

spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your_smtp_username
spring.mail.password=your_smtp_password # Use App Password for Gmail/Outlook!
app.email.sender=your_app_email@example.com
CORS (for local React dev):

Properties

spring.web.cors.origins=http://localhost:3000
Run the Backend:

Bash

mvn spring-boot:run
The application will start, connect to the database, and automatically populate the investment_scheme_rates table.

Frontend Setup (React)
(Assuming your React frontend project is separate and named sip-tracker-frontend)

Clone the React repository:

Bash

git clone https://github.com/your-username/your-react-repo.git
cd sip-tracker-frontend
Install dependencies:

Bash

npm install # or yarn install
Configure API Base URL:

Create a .env file in the root of your React project.

Add your backend's local URL:

REACT_APP_API_BASE_URL=http://localhost:8080/api
Ensure your React code uses process.env.REACT_APP_API_BASE_URL for API calls.

Run the Frontend:

Bash

npm start # or yarn start
This will typically open your React app in your browser at http://localhost:3000.

API Endpoints
Here's a quick reference to the main API endpoints provided by the Spring Boot backend:

Category

Method

Endpoint

Description

Request Body/Params

User Management

POST

/api/users/register

Registers a new user or retrieves existing.

{ "name": "...", "email": "..." }

GET

/api/users/{userId}

Retrieves user details by ID.

Path Variable: userId

SIP Calculation & Goals

POST

/api/requests/calculate-and-save

Calculate Monthly SIP from target Future Value.

Query Params: userId, futureValue, timePeriodYears, dreamType

POST

/api/requests/calculate-fv-from-sip

Calculate Future Value from user's Monthly SIP.

Query Params: userId, monthlySipAmount, timePeriodYears, dreamType (opt)

GET

/api/requests/user/{userId}

Get all requests/goals for a user.

Path Variable: userId

GET

/api/requests/{requestId}

Get details of a specific request and its calculation.

Path Variable: requestId

Investment Schemes

GET

/api/schemes/year/{year}

Get all scheme rates for a specific year.

Path Variable: year

GET

/api/requests/{requestId}/scheme-suggestions

Get 50-30-20 allocated scheme suggestions for a request.

Path Variable: requestId

Reports

GET

/api/reports/pdf/{requestId}

Download PDF report for a request.

Path Variable: requestId

POST

/api/reports/email/{requestId}

Email PDF report to user (can specify email in body).

Path Variable: requestId, Body (Optional): { "email": "..." }


Export to Sheets
Deployment on AWS
This project is designed for deployment on AWS using a combination of managed services for scalability and ease of operations.

Database: Deployed on Amazon RDS for PostgreSQL.

Backend: Deployed on AWS Elastic Beanstalk (Java SE platform).

Frontend: Deployed as a static website on Amazon S3, distributed globally via Amazon CloudFront (for performance and HTTPS).

Calculations: Handled by a dedicated AWS Lambda function invoked by the backend.

(Detailed deployment steps are outside the scope of this README but involve configuring VPCs, Security Groups, IAM Roles, and environment variables in AWS consoles.)

License
Distributed under the MIT License. See LICENSE for more information.

Contact
Your Name -Kshitij Gupta

Project Link: finsaath.com

