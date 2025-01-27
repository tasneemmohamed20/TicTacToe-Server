# TicTacToe Server

Server component for the multiplayer Tic-tac-toe game that handles multiple concurrent game sessions.

## Features

- Multi-client Support
- User Authentication and Management
- Real-time Game State Synchronization
- Score Tracking and Persistence
- Database Integration
- Client Session Management

## Prerequisites

- Java 8 or higher
- Derby database
- GSON library
- NetBeans IDE or similar

## Database Setup

1. Create database:
```sql
CREATE TABLE Users (
    USERID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(10) DEFAULT 'offline' CHECK (status IN ('online', 'offline','ingame')),
    Score INT DEFAULT 0
);
```
## Installation
1. Clone repository:
```
 git clone https://github.com/tasneemmohamed20/TicTacToe-Server.git
```
3. Configure database:
* Open src/db/DBConfig.java
* Update database credentials:
  
```
private static final String URL = "jdbc:derby://localhost:1527/users";
private static final String USER = "your_username";
private static final String PASSWORD = "your_password";
```

## Running the Server
1. Using NetBeans:
    * Open project in NetBeans
    * Right-click project > Clean and Build
    * Right-click project > Run
2. Using Command Line:
   
```
cd TicTacToe-Server/server
javac -d build/classes src/serverUI/ServerUI.java
java -cp "build/classes:lib/*" serverUI.ServerUI
```

## Server Management
* Monitor active connections through server UI
* View player statistics
* Track game sessions
* Manage server status

## Team Members

- [Nada Ali](https://github.com/nada263204)
- [Mariam Rafaat](https://github.com/mariam175)
- [Eslam El-Sayed](https://github.com/eslamelsayed010)
- [Mohamed Khaled](https://github.com/mohamedKhaled655)
- [Tasneem M. Mohamed](https://github.com/tasneemmohamed20)

# Note
# Ensure the server is running before starting client applications. The server must be accessible from client machines for online gameplay to function.

