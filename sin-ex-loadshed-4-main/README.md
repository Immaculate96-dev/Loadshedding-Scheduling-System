# System Intergration

Loadshedding Schedule System

A microservices-based application for managing and displaying loadshedding schedules across different areas.
 
## Project Overview

This project is a distributed system consisting of multiple microservices that work together to provide loadshedding schedule information. The system is built using Spring Boot and follows microservices architecture principles.
System Architecture

    Places Service: Manages location data and area information
    Schedule Service: Handles loadshedding schedules
    Stage Service: Manages different stages of loadshedding
    Web Service: Provides the frontend interface
    Common: Shared code,utilities and alert services


## Prerequisites

    Java 17 or higher
    Maven 3.6.3 or higher
    Git

## Access the Application

   Web Interface: http://localhost:7100
   Places Service API: http://localhost:7000
   Schedule Service API: http://localhost:7002
   Stage Service API: http://localhost:7001
   ActiveMQ Console: http://localhost:8161/admin (username: admin, password: admin)

## Project Structure

sin-ex-loadshed-4/
├── common/         # Shared code and utilities
├── places/         # Places service
├── schedule/       # Schedule service
├── stage/          # Stage service
├── web/            # Web interface
└── pom.xml         # Maven parent POM

## API Endpoints
## Web Service (Port 7100)

    GET / - Home page showing current loadshedding stage
    GET /schedule - Schedule page with province selection
    POST /towns - Get towns for a selected province
    POST /post-alert - Post a new alert
    POST /schedule - Get schedule for a specific town and province

## Places Service (Port 7000)

    GET /provinces - Get list of all provinces
    GET /towns/{province} - Get list of towns in a province (URL-encode province name)
    " curl -X GET http://localhost:7000/towns/Limpopo -H "Content-Type: application/json"

## Stage Service (Port 7001)

    GET /stage - Get current loadshedding stage
    POST /stage - Set current loadshedding stage
    (windows) " curl.exe -X POST http://localhost:7001/stage -H "Content-Type: application/json" -d '5'"
    (linux)"curl -X POST http://localhost:7001/stage -H "Content-Type: application/json" -d '4'" or
    "curl -X POST http://localhost:7001/stage -H "Content-Type: application/json" -d '{"stage": 5}'"

## Schedule Service (Port 7002)

    GET /{province}/{town}/{stage} - Get loadshedding schedule for a specific location and stage
        Parameters should be URL-encoded
        Example: /Gauteng/Johannesburg/4

## ActiveMQ Configuration

This project uses ActiveMQ for message queuing between services. The Makefile will automatically handle the download and setup of ActiveMQ.
ActiveMQ Management Console

    URL: http://localhost:8161/admin
    Username: admin
    Password: admin
