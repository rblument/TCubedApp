/**
 * Asynchronous client operations for the TCubed task board.
 */

/**
 * Creates and displays a toast notification in the fixed bottom-right container.
 * Automatically removes the toast after 3000ms.
 *
 * @param {string} message notification text
 * @param {'success'|'error'|'info'} type notification style variant
 */
function showToast(message, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.classList.add('toast-hide');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

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
 * Renders a newly created task object into its corresponding column container on the DOM,
 * including data attributes and the edit button.
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

    taskDiv.setAttribute('data-task-id', task.taskId);
    taskDiv.setAttribute('data-task-title', task.title || '');
    taskDiv.setAttribute('data-task-body', task.body || '');
    taskDiv.setAttribute('data-task-assignee', task.assignee || '');
    taskDiv.setAttribute('data-bucket-id', task.bucketId || 1);

    taskDiv.dataset.taskId = task.taskId;
    taskDiv.dataset.taskTitle = task.title || '';
    taskDiv.dataset.taskBody = task.body || '';
    taskDiv.dataset.taskAssignee = task.assignee || '';
    taskDiv.dataset.bucketId = task.bucketId || 1;

    const editBtn = document.createElement('button');
    editBtn.type = 'button';
    editBtn.className = 'btn-edit-task';
    editBtn.textContent = '✏️';
    editBtn.title = 'Edit Task';
    editBtn.setAttribute('draggable', 'false');
    editBtn.onclick = function(e) {
        if (e) e.stopPropagation();
        openEditTaskModal(this.closest('.task'));
    };
    taskDiv.appendChild(editBtn);

    const titleStrong = document.createElement('strong');
    titleStrong.textContent = task.taskId;

    const titleSpan = document.createElement('span');
    titleSpan.className = 'task-title';
    titleSpan.textContent = task.title;

    taskDiv.appendChild(titleStrong);
    taskDiv.appendChild(document.createTextNode(': '));
    taskDiv.appendChild(titleSpan);

    if (task.body) {
        const bodyP = document.createElement('p');
        bodyP.className = 'task-body';
        bodyP.textContent = task.body;
        taskDiv.appendChild(bodyP);
    }

    const br = document.createElement('br');
    taskDiv.appendChild(br);

    const assigneeSmall = document.createElement('small');
    assigneeSmall.className = 'task-assignee';
    assigneeSmall.style.color = 'gray';
    assigneeSmall.textContent = task.assignee || '';
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
            showToast('Task created successfully!', 'success');
        })
        .catch(error => {
            console.error('Error creating task:', error);
            showToast(error.message || 'Could not create task', 'error');
        });
}

/**
 * Persists a task drag-and-drop movement by calling the REST API.
 * Updates the task's bucket and vertical position.
 *
 * @param {number|string} taskId      the ID of the task being moved
 * @param {number}        newBucketId the target destination bucket ID
 * @param {number}        newPosition the vertical position index in the destination bucket
 * @returns {Promise<void>}
 */
function persistTaskMove(taskId, newBucketId, newPosition) {
    return fetch(`/api/tasks/${taskId}/move?newBucketId=${newBucketId}&newPosition=${newPosition}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) {
            console.error("Failed to persist task movement in the database.");
            showToast('Failed to move task', 'error');
        } else {
            if (parseInt(newBucketId, 10) === 4) {
                showToast('Task archived to Trash', 'info');
            } else {
                showToast('Task moved', 'success');
            }
        }
    })
    .catch(error => {
        console.error("Error moving task:", error);
        showToast('Failed to move task', 'error');
    });
}

/** Reference to the currently edited task DOM element */
let currentEditingTaskElement = null;

/**
 * Opens the task edit modal and populates form fields from the task element attributes.
 *
 * @param {HTMLElement} taskElement the .task DOM element being edited
 */
function openEditTaskModal(taskElement) {
    if (!taskElement) return;
    currentEditingTaskElement = taskElement;

    const taskId = taskElement.getAttribute('data-task-id') || taskElement.id;
    let taskTitle = taskElement.getAttribute('data-task-title');
    if (taskTitle === null || taskTitle === undefined) {
        const titleSpan = taskElement.querySelector('.task-title');
        taskTitle = titleSpan ? titleSpan.textContent.trim() : '';
    }

    let taskBody = taskElement.getAttribute('data-task-body');
    if (taskBody === null || taskBody === undefined) {
        const bodyP = taskElement.querySelector('.task-body');
        taskBody = bodyP ? bodyP.textContent.trim() : '';
    }

    let taskAssignee = taskElement.getAttribute('data-task-assignee');
    if (taskAssignee === null || taskAssignee === undefined) {
        const assigneeSmall = taskElement.querySelector('.task-assignee') || taskElement.querySelector('small');
        taskAssignee = assigneeSmall ? assigneeSmall.textContent.trim() : '';
    }

    let bucketId = taskElement.getAttribute('data-bucket-id');
    if (!bucketId) {
        const container = taskElement.closest('.container');
        if (container) {
            bucketId = container.getAttribute('data-bucket-id');
        }
    }

    const idInput = document.getElementById('editTaskId');
    const titleInput = document.getElementById('editTaskTitle');
    const bodyInput = document.getElementById('editTaskBody');
    const assigneeInput = document.getElementById('editTaskAssignee');
    const bucketInput = document.getElementById('editTaskBucketId');

    if (idInput) idInput.value = taskId;
    if (titleInput) titleInput.value = taskTitle;
    if (bodyInput) bodyInput.value = taskBody;
    if (assigneeInput) assigneeInput.value = taskAssignee;
    if (bucketInput) bucketInput.value = bucketId || '1';

    const modal = document.getElementById('taskEditModal');
    if (modal) {
        modal.style.display = 'flex';
    }
}

/**
 * Closes the task edit modal and resets its form.
 */
function closeEditTaskModal() {
    const modal = document.getElementById('taskEditModal');
    if (modal) {
        modal.style.display = 'none';
    }
    currentEditingTaskElement = null;
    const form = document.getElementById('editTaskForm');
    if (form) {
        form.reset();
    }
}

/**
 * Handles submission of the Edit Task modal form.
 *
 * @param {Event} event form submit event
 */
async function handleEditTaskSubmit(event) {
    event.preventDefault();
    const taskId = document.getElementById('editTaskId').value;
    const title = document.getElementById('editTaskTitle').value.trim();
    const body = document.getElementById('editTaskBody').value.trim();
    const assignee = document.getElementById('editTaskAssignee').value.trim();
    const bucketId = parseInt(document.getElementById('editTaskBucketId').value, 10) || 1;

    if (!taskId || !title) return;

    const payload = {
        title: title,
        body: body,
        assignee: assignee,
        bucketId: bucketId
    };

    try {
        const updatedTask = await updateTask(taskId, payload);

        const taskElement = currentEditingTaskElement ||
                            document.getElementById(taskId) ||
                            document.querySelector(`[data-task-id="${taskId}"]`);

        if (taskElement) {
            taskElement.setAttribute('data-task-id', updatedTask.taskId);
            taskElement.setAttribute('data-task-title', updatedTask.title || '');
            taskElement.setAttribute('data-task-body', updatedTask.body || '');
            taskElement.setAttribute('data-task-assignee', updatedTask.assignee || '');
            taskElement.setAttribute('data-bucket-id', updatedTask.bucketId);

            taskElement.dataset.taskId = updatedTask.taskId;
            taskElement.dataset.taskTitle = updatedTask.title || '';
            taskElement.dataset.taskBody = updatedTask.body || '';
            taskElement.dataset.taskAssignee = updatedTask.assignee || '';
            taskElement.dataset.bucketId = updatedTask.bucketId;

            const titleSpan = taskElement.querySelector('.task-title');
            if (titleSpan) {
                titleSpan.textContent = updatedTask.title;
            }

            let bodyP = taskElement.querySelector('.task-body');
            if (updatedTask.body) {
                if (!bodyP) {
                    bodyP = document.createElement('p');
                    bodyP.className = 'task-body';
                    const assigneeSmall = taskElement.querySelector('.task-assignee') || taskElement.querySelector('small');
                    if (assigneeSmall) {
                        taskElement.insertBefore(bodyP, assigneeSmall);
                    } else {
                        taskElement.appendChild(bodyP);
                    }
                }
                bodyP.textContent = updatedTask.body;
                bodyP.style.display = '';
            } else if (bodyP) {
                bodyP.remove();
            }

            const assigneeSmall = taskElement.querySelector('.task-assignee') || taskElement.querySelector('small');
            if (assigneeSmall) {
                assigneeSmall.textContent = updatedTask.assignee || '';
            }

            const targetBucketId = parseInt(updatedTask.bucketId, 10);
            if (targetBucketId === 4) {
                taskElement.remove();
            } else if (targetBucketId >= 1 && targetBucketId <= 3) {
                const targetBucket = document.querySelector(`.container[data-bucket-id="${targetBucketId}"]`);
                if (targetBucket && taskElement.parentNode !== targetBucket) {
                    targetBucket.appendChild(taskElement);
                }
            }
        }

        closeEditTaskModal();
        showToast('Task updated successfully!', 'success');
    } catch (error) {
        console.error('Error updating task:', error);
        showToast(error.message || 'Failed to update task', 'error');
    }
}

/**
 * Handles deletion of the task currently open in the edit modal.
 */
async function handleDeleteTaskClick() {
    const taskId = document.getElementById('editTaskId').value;
    if (!taskId) return;

    try {
        await deleteTask(taskId);
        const taskElement = currentEditingTaskElement ||
                            document.getElementById(taskId) ||
                            document.querySelector(`[data-task-id="${taskId}"]`);
        if (taskElement) {
            taskElement.remove();
        }
        closeEditTaskModal();
        showToast('Task deleted permanently.', 'info');
    } catch (error) {
        console.error('Error deleting task:', error);
        showToast(error.message || 'Failed to delete task', 'error');
    }
}

// Global window registrations & modal backdrop click handlers
if (typeof window !== 'undefined') {
    window.addEventListener('click', (event) => {
        const modal = document.getElementById('taskEditModal');
        if (event.target === modal) {
            closeEditTaskModal();
        }
    });

    window.addEventListener('keydown', (event) => {
        if (event.key === 'Escape') {
            const modal = document.getElementById('taskEditModal');
            if (modal && modal.style.display !== 'none') {
                closeEditTaskModal();
            }
        }
    });

    window.showToast = showToast;
    window.openEditTaskModal = openEditTaskModal;
    window.closeEditTaskModal = closeEditTaskModal;
    window.handleEditTaskSubmit = handleEditTaskSubmit;
    window.handleDeleteTaskClick = handleDeleteTaskClick;
    window.createTask = createTask;
    window.appendTaskToBoard = appendTaskToBoard;
    window.handleCreateTaskSubmit = handleCreateTaskSubmit;
    window.persistTaskMove = persistTaskMove;
}
