/* 
 *  T^3: TCubed Task Tracking Tool
 * 
 *   (C) Richard Blumenthal, All rights reserved
 * 
 *   Unauthorized use, duplication or distribution without the authors'
 *   permission is strictly prohibited.
 * 
 *   Unless required by applicable law or agreed to in writing, this
 *   software is distributed on an "AS IS" basis without warranties
 *   or conditions of any kind, either expressed or implied.
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

CREATE TABLE Account (
    UserId VARCHAR(256) PRIMARY KEY,
    Password VARCHAR(256) NOT NULL,
    FirstName VARCHAR(256),
    LastName VARCHAR(256),
    Question INT,
    Answer VARCHAR(256),
    IsStudent TINYINT DEFAULT 0
);

-- Password encoding should be SHA-256, not in the clear.
INSERT INTO ACCOUNT 
  (UserId, Password, FirstName, LastName, Question, Answer, IsStudent)
 VALUES
  ('test@regis.edu', 'TestP@ss', 'Testy', 'McTest', 0, 'Denver', 1);
