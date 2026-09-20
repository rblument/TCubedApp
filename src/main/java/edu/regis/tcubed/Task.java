package edu.regis.tcubed;

public class Task {
    private String taskId;
    private String title;
    private String description;
    private String status;
    private String assignee;

    public Task(String taskId, String title, String description, String status, String assignee) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignee = assignee;
    }

    // Getters are required for Thymeleaf to read the properties
    public String getTaskId() { return taskId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getAssignee() { return assignee; }
}