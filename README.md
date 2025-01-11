# TicTacToe-Server
#database
CREATE TABLE Users (
    userId INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) ,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(10) DEFAULT 'offline' CHECK (status IN ('online', 'offline','ingame')),
    Score INT DEFAULT 0,
    PRIMARY KEY (userId)
);


