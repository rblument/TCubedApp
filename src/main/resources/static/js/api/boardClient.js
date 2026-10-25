/**
 * Asynchronous client operations for the TCubed task board.
 */

/**
 * Sends a POST request to create a new task in the database and dynamically
 * appends the new task card to the appropriate board column.
 *
 * @param {Object} taskData object containing { title, body, assignee, bucketId }
 * @returns {Promise<Object>} the created Task object returned from the server
 */
function createTask(taskData) {
    return fetch('/api/tasks', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(taskData)
    })
    .then(async response => {
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || 'Failed to create task.');
        }
        return response.json();
    });
}

/**
 * Renders a newly created task object into its corresponding column container on the DOM.
 *
 * @param {Object} task the created Task returned by the backend
 */
function appendTaskToBoard(task) {
    const targetBucket = document.querySelector(`.container[data-bucket-id="${task.bucketId}"]`);
    if (!targetBucket) return;

    const taskDiv = document.createElement('div');
    taskDiv.className = 'task';
    taskDiv.setAttribute('draggable', 'true');
    taskDiv.id = task.taskId;
    taskDiv.setAttribute('ondragstart', 'drag(event)');

    const titleStrong = document.createElement('strong');
    titleStrong.textContent = task.taskId;

    const titleSpan = document.createElement('span');
    titleSpan.textContent = task.title;

    const br = document.createElement('br');
    const assigneeSmall = document.createElement('small');
    assigneeSmall.style.color = 'gray';
    assigneeSmall.textContent = task.assignee || 'Unassigned';

    taskDiv.appendChild(titleStrong);
    taskDiv.appendChild(document.createTextNode(': '));
    taskDiv.appendChild(titleSpan);

    if (task.body) {
        const bodyP = document.createElement('p');
        bodyP.className = 'task-body';
        bodyP.textContent = task.body;
        taskDiv.appendChild(bodyP);
    }

    taskDiv.appendChild(br);
    taskDiv.appendChild(assigneeSmall);

    targetBucket.appendChild(taskDiv);
}

/**
 * Handles submission of the Create Task form on the dashboard.
 *
 * @param {Event} event form submit event
 */
function handleCreateTaskSubmit(event) {
    event.preventDefault();
    const form = event.target;
    const titleInput = document.getElementById('taskTitle');
    const bodyInput = document.getElementById('taskBody');
    const assigneeInput = document.getElementById('taskAssignee');
    const bucketInput = document.getElementById('taskBucketId');

    const payload = {
        title: titleInput.value.trim(),
        body: bodyInput.value.trim(),
        assignee: assigneeInput.value.trim(),
        bucketId: parseInt(bucketInput.value, 10) || 1
    };

    if (!payload.title) return;

    createTask(payload)
        .then(createdTask => {
            appendTaskToBoard(createdTask);
            form.reset();
        })
        .catch(error => {
            console.error('Error creating task:', error);
            alert('Could not create task: ' + error.message);
        });
}

/**
 * Persists a task drag-and-drop movement by calling the REST API.
 * Updates the task's bucket and vertical position.
 *
 * @param {number|string} taskId      the ID of the task being moved
 * @param {number}        newBucketId the target destination bucket ID
 * @param {number}        newPosition the vertical position index in the destination bucket
 */
function persistTaskMove(taskId, newBucketId, newPosition) {
    fetch(`/api/tasks/${taskId}/move?newBucketId=${newBucketId}&newPosition=${newPosition}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) {
            console.error("Failed to persist task movement in the database.");
        }
    })
    .catch(error => console.error("Error moving task:", error));
}
