/* 
 *  T^3: TCubed Task Tracking Tool
 *  Database Seed Script (SeedDB.sql)
 *  Purpose: Populates deterministic dummy data in TCubedDB for development 
 *           and JUnit repository/integration testing.
 *  Usage:   Run after SetupDB.sql
 */

USE TCubedDB;

/* ********** 1. IDEMPOTENT RESET (Child -> Parent Order) ********** */
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM TASK;
DELETE FROM BUCKET;
DELETE FROM BOARD;
DELETE FROM PROJECT_MEMBER;
DELETE FROM PROJECT;
DELETE FROM ACCOUNT;

ALTER TABLE TASK AUTO_INCREMENT = 1;
ALTER TABLE BUCKET AUTO_INCREMENT = 1;
ALTER TABLE BOARD AUTO_INCREMENT = 1;
ALTER TABLE PROJECT AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

/* ********** 2. SEED ACCOUNTS ********** */
INSERT INTO ACCOUNT 
    (UserId, Password, FirstName, LastName, Question, Answer, IsStudent)
VALUES
    -- Original default user from SetupDB.sql (Active student, member of Projects 1 & 3)
    ('test@regis.edu',   SHA2('TestP@ss', 256),    'Testy',     'McTest',     0, 'Denver',    1),
    -- Secondary student user (Member of Project 1, owns multiple tasks)
    ('alison@regis.edu', SHA2('AlisonP@ss1', 256), 'Alison',    'Wonderland', 1, 'Rabbit',    1),
    -- Non-student user (IsStudent = 0, member of Projects 1 & 2 for isolation tests)
    ('bob@regis.edu',    SHA2('BobP@ss2', 256),    'Bob',       'Builder',    2, 'Bobsville', 0),
    -- Isolated user (0 projects and 0 tasks to test empty dashboard/task views)
    ('empty@regis.edu',  SHA2('EmptyP@ss3', 256),  'EmptyTest', 'McEmpty',    0, 'Empty',     1);

/* ********** 3. SEED PROJECTS ********** */
INSERT INTO PROJECT 
    (ProjectId, Name, Description, CreatedAt)
VALUES
    (1, 'Capstone', 'Primary project with multiple members, boards, buckets, and tasks.', '2026-01-15 08:00:00'),
    (2, 'Security & Auth Audit', 'Secondary project restricted to Bob to test authorization boundaries.', '2026-01-16 10:30:00'),
    (3, 'Empty Sandbox Project', 'Project with zero boards and zero tasks to test empty-state rendering.', '2026-01-17 14:00:00');

/* ********** 4. SEED PROJECT MEMBERS ********** */
INSERT INTO PROJECT_MEMBER 
    (UserId, ProjectId)
VALUES
    -- Project 1 has three collaborating members
    ('test@regis.edu',   1),
    ('alison@regis.edu', 1),
    ('bob@regis.edu',    1),
    -- Project 2 is exclusive to Bob (test@regis.edu and alice@regis.edu should not see it)
    ('bob@regis.edu',    2),
    -- Project 3 has one member but no boards/tasks
    ('test@regis.edu',   3);

/* ********** 5. SEED BOARDS ********** */
INSERT INTO BOARD 
    (BoardId, Name, Description, ProjectId)
VALUES
    (1, 'Sprint 1 Development', 'Main Kanban board for core application features.', 1),
    (2, 'QA & Documentation',   'Secondary board under Project 1 with zero buckets.', 1),
    (3, 'Security Review',      'Kanban board for Project 2 security tasks.', 2);

/* ********** 6. SEED BUCKETS ********** */
INSERT INTO BUCKET 
    (BucketId, Name, Position, BoardId)
VALUES
    -- Standard Kanban workflow columns for Board 1 (Project 1)
    (1, 'To Do',       1, 1),
    (2, 'In Progress', 2, 1),
    (3, 'Done',        3, 1),
    -- Empty bucket on Board 1 to test rendering a column with 0 tasks
    (4, 'Blocked',     4, 1),
    -- Bucket for Board 3 (Project 2)
    (5, 'Backlog',     1, 3);

/* ********** 7. SEED TASKS ********** */
INSERT INTO TASK 
    (TaskId, Title, Body, Position, CreatedAt, UpdatedAt, BucketId, Assignee)
VALUES    
    -- Bucket 1 ('To Do' on Board 1): Two tasks to test vertical Position ordering + NULL Assignee
    (1, 'Review documentation draft', 'Review and provide notes on the preliminary project write-up.', 1, '2026-01-15 09:00:00', '2026-01-15 09:00:00', 1, 'test@regis.edu'),
    (2, 'Gather general reference links', 'Unassigned task to verify null Assignee handling in UI and DAO.', 2, '2026-01-15 09:15:00', '2026-01-15 09:15:00', 1, NULL),

    -- Bucket 2 ('In Progress' on Board 1): Active tasks assigned to different team members
    (3, 'Draft weekly summary report', 'Compile notes and progress updates into a shared document.', 1, '2026-01-15 10:00:00', '2026-01-16 11:20:00', 2, 'alison@regis.edu'),
    (4, 'Organize team meeting notes', 'Clean up action items and share notes with participants.', 2, '2026-01-15 10:30:00', '2026-01-16 14:00:00', 2, 'test@regis.edu'),

    -- Bucket 3 ('Done' on Board 1): Completed task
    (5, 'Setup initial project repository', 'Create baseline folder structure and configuration templates.', 1, '2026-01-15 08:30:00', '2026-01-15 16:45:00', 3, 'test@regis.edu'),

    -- Bucket 5 ('Backlog' on Board 3 / Project 2): Isolated task for Bob
    (6, 'Review security guidelines', 'Read and verify compliance with general access policies.', 1, '2026-01-16 12:00:00', '2026-01-16 12:00:00', 5, 'bob@regis.edu');

/* ********** 8. ADVANCE AUTO_INCREMENT COUNTERS ********** */
-- Prevents primary key collisions when tests insert new records after seeding
ALTER TABLE PROJECT AUTO_INCREMENT = 4;
ALTER TABLE BOARD   AUTO_INCREMENT = 4;
ALTER TABLE BUCKET  AUTO_INCREMENT = 6;
ALTER TABLE TASK    AUTO_INCREMENT = 7;