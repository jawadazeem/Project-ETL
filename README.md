# Senate Telecom Billing Processor (v1)

This is version 1 of the Senate Telecom Billing Processor project.  
It is a Spring Boot application that performs a full ETL (Extract, Transform, Load) workflow on telecom billing data stored in CSV format.

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
1. Clone the project
2. Open it in IntelliJ or VS Code
3. Run the main class: SenateTelecomBillingProcessorApplication.java
4. Access the REST API at `http://localhost:8080

## Technologies Used
- Java 17+
- Spring Boot (Web)
- Maven
- Streams & Lambdas
- Custom ETL processing

## Notes
This is an early version meant to establish the foundations of the ETL pipeline and REST API.  
Future versions will add:
- improved file loading
- global exception handling
- CSV upload endpoint
- database persistence
- Swagger documentation
- AWS deployment options

Version: **v1**