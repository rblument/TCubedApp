package edu.regis.tcubed.model;

public class Project {
    private int projectId;
    private String name;
    private String description;

    public Project(int projectId, String name, String description) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
    }

    // Getters
    public int getProjectId() { return projectId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}