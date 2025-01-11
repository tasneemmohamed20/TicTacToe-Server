# TicTacToe-Server
#database
CREATE TABLE Users (
    USERID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(10) DEFAULT 'offline' CHECK (status IN ('online', 'offline','ingame')),
    Score INT DEFAULT 0
);


