package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Task;
import edu.regis.tcubed.repository.ProjectRepository;
import edu.regis.tcubed.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BoardControllerTest {

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private BoardController controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);
        controller = new BoardController(taskRepository, projectRepository);
        session = new MockHttpSession();
        session.setAttribute("userId", "logged_in_user");
        session.setAttribute("activeProjectId", "Capstone");
    }

    @Test
    void createTaskWithUnassignedDoesNotDefaultToSessionUserId() throws SQLException {
        Task newTask = new Task();
        newTask.setTitle("Unassigned Task");
        newTask.setAssignee("");

        when(taskRepository.createTask(any(Task.class), eq("Capstone")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.createTask(newTask, session);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Task created = (Task) response.getBody();
        assertNotNull(created);
        assertNull(created.getAssignee());
    }

    @Test
    void createTaskWithNullAssigneeDoesNotDefaultToSessionUserId() throws SQLException {
        Task newTask = new Task();
        newTask.setTitle("Unassigned Task");
        newTask.setAssignee(null);

        when(taskRepository.createTask(any(Task.class), eq("Capstone")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.createTask(newTask, session);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Task created = (Task) response.getBody();
        assertNotNull(created);
        assertNull(created.getAssignee());
    }

    @Test
    void createTaskWithExplicitAssigneePreservesAssignee() throws SQLException {
        Task newTask = new Task();
        newTask.setTitle("Assigned Task");
        newTask.setAssignee("other_user");

        when(taskRepository.createTask(any(Task.class), eq("Capstone")))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.createTask(newTask, session);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Task created = (Task) response.getBody();
        assertNotNull(created);
        assertEquals("other_user", created.getAssignee());
    }
}
