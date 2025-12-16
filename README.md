# Telecom Billing Processor (v2)

### Author: Jawad Azeem
This is version 2 of the Senate Telecom Billing Processor project.  
It is a Spring Boot application that performs a full ETL (Extract, Transform, Load) workflow on telecom billing data stored in CSV format.
It reads the data, processes it into structured records, computes various analytics, and exposes the results via REST API endpoints.
A live version of the API is hosted on AWS.

## Functionality
- Reads billing data from a CSV file using a custom parser
- Converts each row into a `BillingRecord`
- Aggregates analytics such as:
    - total charges
    - highest charge
    - record count
    - charges grouped by state
- Exposes results through REST API endpoints

## How to Run
- Access the Live API at:
   - http://senate-telecom-billing-api-env.eba-wbb6t53w.us-east-1.elasticbeanstalk.com/
   - https://telecom.jawadazeem.com

## Technologies Used
- Java 21
- Spring Boot (Web)
- Docker
- Maven
- JUnit 5 & Mockito
- PostgreSQL
- AWS (RDS, Elastic Beanstalk, S3)

## Architecture
- ETL ingestion service
- Stateless service layer
- Repository-based persistence

## Notes
This is an early version of the project. 
Future versions will add:
- CSV upload endpoint
- additional analytics
- AI integration for anomaly detection
- Kubernetes deployment

Version: **v2** (Dec 2025)