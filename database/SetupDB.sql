/* 
 *  T^3: TCubed Task Tracking Tool
 * 
 */

-- If the TCubedDB exists, drop it. You will lose existing data.
DROP DATABASE IF EXISTS TCubedDB;

-- Create a database in MySql named: TCubedDB
CREATE DATABASE TCubedDB;

-- Create user representing the TCubedTs application.
CREATE USER IF NOT EXISTS 'TCubedTs'@'localhost' IDENTIFIED BY 'TCubed2026';

-- Give the TCubedTs application the following priveledges.
GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,DROP ON TCubedDB.* TO 'TCubedTs'@'localhost';

/* ********** ADD TABLES TO NEW DATABASE ********** */

-- Switch to the DB.
USE TCubedDB;

CREATE TABLE ACCOUNT (
    UserId VARCHAR(256) PRIMARY KEY,
    Password VARCHAR(256) NOT NULL,
    FirstName VARCHAR(256),
    LastName VARCHAR(256),
    Question INT,
    Answer VARCHAR(256),
    IsStudent TINYINT DEFAULT 0
);

CREATE TABLE PROJECT (
    ProjectId INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(256) NOT NULL,
    Description TEXT,
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE PROJECT_MEMBER (
    UserId VARCHAR(256) NOT NULL,
    ProjectId INT NOT NULL,

    PRIMARY KEY (UserId, ProjectId),
    FOREIGN KEY (UserId)
        REFERENCES ACCOUNT(UserId),
    FOREIGN KEY (ProjectId)
        REFERENCES PROJECT(ProjectId)
);

CREATE TABLE BOARD (
    BoardId INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(256) NOT NULL,
    Description TEXT,
    ProjectId INT NOT NULL,

    FOREIGN KEY (ProjectId)
        REFERENCES PROJECT(ProjectId)
);

CREATE TABLE BUCKET (
    BucketId INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(256) NOT NULL,
    Position INT,
    BoardId INT NOT NULL,

    FOREIGN KEY (BoardId)
        REFERENCES BOARD(BoardId)
);

CREATE TABLE TASK (
    TaskId INT AUTO_INCREMENT PRIMARY KEY,
    Title VARCHAR(256) NOT NULL,
    Body TEXT,
    Position INT,
    CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt DATETIME DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    BucketId INT NOT NULL,

    FOREIGN KEY (BucketId)
        REFERENCES BUCKET(BucketId)
);
-- Password encoding should be SHA-256, not in the clear.
INSERT INTO ACCOUNT 
  (UserId, Password, FirstName, LastName, Question, Answer, IsStudent)
 VALUES
  ('test@regis.edu', 'TestP@ss', 'Testy', 'McTest', 0, 'Denver', 1);
