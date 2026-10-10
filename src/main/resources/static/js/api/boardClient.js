/**
 * Asynchronous client operations for the TCubed task board.
 */

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
            // Optional: Revert the DOM movement if the server fails
        }
    })
    .catch(error => console.error("Error moving task:", error));
}
