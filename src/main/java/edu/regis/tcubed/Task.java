package edu.regis.tcubed;

/**
 * Represents a task within a project board in the TCubed tracking system.
 * Encapsulates task metadata including its identifier, title, description,
 * workflow status, and assigned team member.
 *
 * @author Oscar Castillo Saucedo
 */
public class Task {
    private String taskId;
    private String title;
    private String description;
    private String status;
    private String assignee;

    /**
     * Constructs a new {@code Task} instance with the specified attributes.
     *
     * @param taskId      the unique identifier for the task (e.g., "TP-101")
     * @param title       the short title or summary of the task
     * @param description the detailed description of the task requirements
     * @param status      the current workflow status (e.g., "To-Do", "In Progress", "Done")
     * @param assignee    the name of the team member assigned to the task
     */
    public Task(String taskId, String title, String description, String status, String assignee) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignee = assignee;
    }

    // Getters are required for Thymeleaf to read the properties

    /**
     * Retrieves the unique identifier of the task.
     *
     * @return the task ID
     */
    public String getTaskId() { return taskId; }

    /**
     * Retrieves the title of the task.
     *
     * @return the task title
     */
    public String getTitle() { return title; }

    /**
     * Retrieves the detailed description of the task.
     *
     * @return the task description
     */
    public String getDescription() { return description; }

    /**
     * Retrieves the current workflow status of the task.
     *
     * @return the task status
     */
    public String getStatus() { return status; }

    /**
     * Retrieves the name of the assigned team member.
     *
     * @return the assignee name
     */
    public String getAssignee() { return assignee; }
}