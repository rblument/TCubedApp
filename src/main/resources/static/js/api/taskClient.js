
/**
 * Task API Client
 * @author Dylan Cassagnol
 * task retrieval, updates, and deletion.
 */

const TASK_API = '/api/tasks';

// GET all tasks
async function getAllTasks() {
    const response = await fetch(TASK_API);

    if (!response.ok) {
        throw new Error(`Failed to retrieve tasks: ${response.status}`);
    }

    return response.json();
}

// GET task by ID
async function getTaskById(taskId) {
    const response = await fetch(`${TASK_API}/${encodeURIComponent(taskId)}`);

    if (!response.ok) {
        throw new Error(`Failed to retrieve task: ${response.status}`);
    }

    return response.json();
}

// PUT existing task
async function updateTask(taskId, taskData) {
    const response = await fetch(`${TASK_API}/${encodeURIComponent(taskId)}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(taskData)
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to update task: ${response.status}`);
    }

    return response.json();
}

// DELETE task
async function deleteTask(taskId) {
    const response = await fetch(`${TASK_API}/${encodeURIComponent(taskId)}`, {
        method: 'DELETE'
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || `Failed to delete task: ${response.status}`);
    }

    return true;
}

if (typeof window !== 'undefined') {
    window.getAllTasks = getAllTasks;
    window.getTaskById = getTaskById;
    window.updateTask = updateTask;
    window.deleteTask = deleteTask;
}

