

/**
 * API Client
 * @author Dylan Cassagnol
 * project CRUD operations.
 */

const PROJECT_API = '/api/projects';

// GET all projects
async function getAllProjects() {
    const response = await fetch(PROJECT_API);

    if (!response.ok) {
        throw new Error(`Failed to retrieve projects: ${response.status}`);
    }

    return response.json();
}

// POST new project
async function createProject(projectData) {
    const response = await fetch(PROJECT_API, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(projectData)
    });

    if (!response.ok) {
        throw new Error(`Failed to create project: ${response.status}`);
    }

    return response.json();
}

// PUT existing project
async function updateProject(projectId, projectData) {
    const response = await fetch(`${PROJECT_API}/${encodeURIComponent(projectId)}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(projectData)
    });

    if (!response.ok) {
        throw new Error(`Failed to update project: ${response.status}`);
    }

    return response.json();
}

// Delete a project
async function deleteProject(projectId) {
    const response = await fetch(`${PROJECT_API}/${encodeURIComponent(projectId)}`, {
        method: 'DELETE'
    });

    if (!response.ok) {
        throw new Error(`Failed to delete project: ${response.status}`);
    }

    return true;
}
