package edu.regis.tcubed;

/**
 * Represents a task within a project board in the TCubed tracking system,
 * aligned with the database TASK table schema.
 * Encapsulates task metadata including its identifier, title, body content,
 * position within a bucket, bucket identifier, and assigned team member.
 *
 * @author Oscar Castillo Saucedo
 */
public class Task {
    private int taskId;
    private String title;
    private String body;
    private int position;
    private int bucketId;
    private String assignee;

    /**
     * Default constructor for {@code Task}.
     */
    public Task() {
    }

    /**
     * Constructs a new {@code Task} instance with all attributes.
     *
     * @param taskId   the unique identifier for the task
     * @param title    the short title or summary of the task
     * @param body     the detailed content/body of the task
     * @param position the order position of the task within its bucket
     * @param bucketId the identifier of the bucket/column containing the task
     * @param assignee the name of the team member assigned to the task
     */
    public Task(int taskId, String title, String body, int position, int bucketId, String assignee) {
        this.taskId = taskId;
        this.title = title;
        this.body = body;
        this.position = position;
        this.bucketId = bucketId;
        this.assignee = assignee;
    }

    /**
     * Constructs a new {@code Task} instance without an assignee.
     *
     * @param taskId   the unique identifier for the task
     * @param title    the short title or summary of the task
     * @param body     the detailed content/body of the task
     * @param position the order position of the task within its bucket
     * @param bucketId the identifier of the bucket/column containing the task
     */
    public Task(int taskId, String title, String body, int position, int bucketId) {
        this(taskId, title, body, position, bucketId, null);
    }

    // Getters are required for Thymeleaf to read the properties

    /**
     * Retrieves the unique identifier of the task.
     *
     * @return the task ID
     */
    public int getTaskId() {
        return taskId;
    }

    /**
     * Sets the unique identifier of the task.
     *
     * @param taskId the task ID to set
     */
    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    /**
     * Retrieves the title of the task.
     *
     * @return the task title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of the task.
     *
     * @param title the task title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Retrieves the body content of the task.
     *
     * @return the task body
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the body content of the task.
     *
     * @param body the task body to set
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Retrieves the ordering position of the task within its bucket.
     *
     * @return the task position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the ordering position of the task within its bucket.
     *
     * @param position the task position to set
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Retrieves the bucket ID containing this task.
     *
     * @return the bucket ID
     */
    public int getBucketId() {
        return bucketId;
    }

    /**
     * Sets the bucket ID containing this task.
     *
     * @param bucketId the bucket ID to set
     */
    public void setBucketId(int bucketId) {
        this.bucketId = bucketId;
    }

    /**
     * Retrieves the name of the assigned team member.
     *
     * @return the assignee name
     */
    public String getAssignee() {
        return assignee;
    }

    /**
     * Sets the name of the assigned team member.
     *
     * @param assignee the assignee name to set
     */
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    /**
     * Helper method mapping bucketId to status names for view rendering.
     *
     * @return status name corresponding to the bucketId
     */
    public String getStatus() {
        return switch (bucketId) {
            case 1 -> "To-Do";
            case 2 -> "In Progress";
            case 3 -> "Done";
            default -> "Bucket " + bucketId;
        };
    }

    /**
     * Returns a string representation of the {@code Task} instance.
     *
     * @return a formatted string containing task attributes
     */
    @Override
    public String toString() {
        return "Task{" +
                "taskId=" + taskId +
                ", title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", position=" + position +
                ", bucketId=" + bucketId +
                ", assignee='" + assignee + '\'' +
                '}';
    }
}