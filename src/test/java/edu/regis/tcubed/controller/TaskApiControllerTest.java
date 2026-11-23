package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Task;
import edu.regis.tcubed.repository.TaskRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testse Task API endpoints with a mocked repository.
 * No live MySQL connection is required to test!
 *
 * @author Dylan Cassagnol
 */
class TaskApiControllerTest {
    private MockMvc mvc;
    private TaskRepository repository;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new TaskApiController(repository)).build();
        session = new MockHttpSession();
        session.setAttribute("userId", "user@example.com");
        session.setAttribute("activeProjectId", "42");
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void requiresAnActiveProjectToListTasks() throws Exception {
        session.removeAttribute("activeProjectId");
        mvc.perform(get("/api/tasks").session(session)).andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void listsTasksForAuthenticatedMember() throws Exception {
        when(repository.getTasksForMember("user@example.com", "42"))
                .thenReturn(List.of(new Task(1, "Write tests", "", 1, 2)));
        mvc.perform(get("/api/tasks").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Write tests"));
        verify(repository).getTasksForMember("user@example.com", "42");
    }

    @Test
    void retrievesAccessibleTask() throws Exception {
        when(repository.findTaskForMember(1, "user@example.com", "42"))
                .thenReturn(new Task(1, "Write tests", "", 1, 2));
        mvc.perform(get("/api/tasks/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(1));
    }

    @Test
    void missingOrInaccessibleTaskReturnsNotFound() throws Exception {
        mvc.perform(get("/api/tasks/99").session(session))
                .andExpect(status().isNotFound());
        verify(repository).findTaskForMember(99, "user@example.com", "42");
    }

    @Test
    void updatesTaskForAuthorizedUser() throws Exception {
        when(repository.updateTaskForMember(eq(1), any(Task.class),
                eq("user@example.com"), eq("42")))
                .thenReturn(new Task(1, "Revised title", "", 1, 2));
        mvc.perform(put("/api/tasks/1").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Revised title\",\"bucketId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Revised title"));
    }

    @Test
    void rejectsBlankTaskTitleWithoutRepositoryAccess() throws Exception {
        mvc.perform(put("/api/tasks/1").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void deletesAccessibleTask() throws Exception {
        when(repository.deleteTaskForMember(1, "user@example.com", "42"))
                .thenReturn(true);
        mvc.perform(delete("/api/tasks/1").session(session))
                .andExpect(status().isNoContent());
    }


}
