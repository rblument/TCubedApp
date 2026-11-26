/**
 * The Drag Drop features previously located in the dashboard.html file within 
 * the <script> tags.
 */

// Grabs the ID of the task being dragged
        function drag(ev) {
            ev.dataTransfer.setData("text", ev.target.id);
        }

        // Prevents the browser's default block on dropping elements
        function allowDrop(ev) {
            ev.preventDefault();
        }

        // Moves the task into the new column container
        function drop(ev, container) {
            ev.preventDefault();
            var taskId = ev.dataTransfer.getData("text");
            var draggedElement = document.getElementById(taskId);
            
            // Appends the task to the bottom of the container
            if (container.classList.contains('container')) {
                container.appendChild(draggedElement);

                var bucketId = container.getAttribute('data-bucket-id') || 1;
                var tasksInContainer = container.querySelectorAll('.task');
                var position = Array.from(tasksInContainer).indexOf(draggedElement) + 1;

                persistTaskMove(taskId, bucketId, position);
            }
        }

        // Moves the task to Trash with 7-day retention
        function dropToTrash(ev) {
            ev.preventDefault();
            var taskId = ev.dataTransfer.getData("text");
            var draggedElement = document.getElementById(taskId);
            
            if (draggedElement) {
                draggedElement.remove(); // Removes the HTML element completely from the active board
                persistTaskMove(taskId, 4, 0, true);
                var badge = document.getElementById('trashCountBadge');
                if (badge) {
                    var current = parseInt(badge.textContent, 10) || 0;
                    badge.textContent = current + 1;
                }
                showToast('Task moved to Trash (kept for 7 days)', 'info');
            }
            ev.target.classList.remove('drag-over'); // Resets the hover color
        }

        // Opens Archive Picker modal without removing the card until confirmed
        function dropToArchive(ev) {
            ev.preventDefault();
            var taskId = ev.dataTransfer.getData("text");
            ev.target.classList.remove('drag-over');
            if (typeof openArchivePickerModal === 'function') {
                openArchivePickerModal(taskId);
            }
        }


