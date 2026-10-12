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

        // Deletes the task when dropped in the trash
        function dropToTrash(ev) {
            ev.preventDefault();
            var taskId = ev.dataTransfer.getData("text");
            var draggedElement = document.getElementById(taskId);
            
            if (draggedElement) {
                draggedElement.remove(); // Removes the HTML element completely
                persistTaskMove(taskId, 4, 0);
            }
            ev.target.classList.remove('drag-over'); // Resets the hover color
        }


