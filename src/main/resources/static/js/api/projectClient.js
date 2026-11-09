/**
 * Project API Client
 * @author Dylan Cassagnol, Oscar Castillo Saucedo
 * Project CRUD operations and member management client functions.
 */

const PROJECT_API = '/api/projects';

/**
 * Creates and displays a toast notification in the fixed bottom-right container.
 * Automatically removes the toast after 3000ms.
 *
 * @param {string} message notification text
 * @param {'success'|'error'|'info'} type notification style variant
 */
function showProjectToast(message, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type} show toast-visible`;
    toast.textContent = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('toast-hide');
        toast.classList.remove('show', 'toast-visible');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

/**
 * Retrieves all projects the current user belongs to.
 *
 * @returns {Promise<Array>} array of project objects
 */
async function getAllProjects() {
    const response = await fetch(PROJECT_API);

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to retrieve projects: ${response.status}`);
    }

    return response.json();
}

/**
 * Sends a POST request to create a new project workspace.
 *
 * @param {Object} projectData object with name and optional description
 * @returns {Promise<Object>} the created project record
 */
async function createProject(projectData) {
    const response = await fetch(PROJECT_API, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(projectData)
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to create project: ${response.status}`);
    }

    return response.json();
}

/**
 * Sends a PUT request to update an existing project's name and description.
 *
 * @param {number|string} projectId the ID of the project to update
 * @param {Object} projectData object with updated name and description
 * @returns {Promise<Object>} the updated project record
 */
async function updateProject(projectId, projectData) {
    const response = await fetch(`${PROJECT_API}/${encodeURIComponent(projectId)}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(projectData)
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to update project: ${response.status}`);
    }

    return response.json();
}

/**
 * Sends a DELETE request to delete a project workspace and its child records.
 *
 * @param {number|string} projectId the ID of the project to delete
 * @returns {Promise<boolean>} true upon successful deletion
 */
async function deleteProject(projectId) {
    const response = await fetch(`${PROJECT_API}/${encodeURIComponent(projectId)}`, {
        method: 'DELETE'
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to delete project: ${response.status}`);
    }

    return true;
}

/**
 * Retrieves all members assigned to a project workspace.
 *
 * @param {number|string} projectId the project ID
 * @returns {Promise<Array<string>>} list of user IDs
 */
async function getProjectMembers(projectId) {
    const response = await fetch(`${PROJECT_API}/${encodeURIComponent(projectId)}/members`);

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to retrieve project members: ${response.status}`);
    }

    return response.json();
}

/**
 * Adds a new member to a project workspace.
 *
 * @param {number|string} projectId the project ID
 * @param {string} userId user ID (email) or first name
 * @returns {Promise<Array<string>>} updated list of project member user IDs
 */
async function addProjectMember(projectId, userId) {
    const response = await fetch(`${PROJECT_API}/${encodeURIComponent(projectId)}/members`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userId: userId })
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to add project member: ${response.status}`);
    }

    return response.json();
}

/**
 * Removes a member from a project workspace.
 *
 * @param {number|string} projectId the project ID
 * @param {string} memberId user ID of member to remove
 * @returns {Promise<Array<string>>} updated list of project member user IDs
 */
async function removeProjectMember(projectId, memberId) {
    const response = await fetch(`${PROJECT_API}/${encodeURIComponent(projectId)}/members/${encodeURIComponent(memberId)}`, {
        method: 'DELETE'
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to remove project member: ${response.status}`);
    }

    return response.json();
}

if (typeof window !== 'undefined') {
    window.showProjectToast = showProjectToast;
    if (typeof window.showToast !== 'function') {
        window.showToast = showProjectToast;
    }
    window.getAllProjects = getAllProjects;
    window.createProject = createProject;
    window.updateProject = updateProject;
    window.deleteProject = deleteProject;
    window.getProjectMembers = getProjectMembers;
    window.addProjectMember = addProjectMember;
    window.removeProjectMember = removeProjectMember;
}
