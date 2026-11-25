package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Task;
import edu.regis.tcubed.repository.ProjectRepository;
import edu.regis.tcubed.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ArchiveApiController using MockMvc and mocked repositories.
 */
class ArchiveApiControllerTest {

    private MockMvc mvc;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        projectRepository = mock(ProjectRepository.class);
        ArchiveApiController controller = new ArchiveApiController(taskRepository, projectRepository);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();

        session = new MockHttpSession();
        session.setAttribute("userId", "testuser@regis.edu");
        session.setAttribute("activeProjectId", "Capstone");
    }

    @Test
    void getTrashUnauthenticatedReturns401() throws Exception {
        mvc.perform(get("/api/workspace/trash"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void getTrashAuthenticatedReturns200WithTasks() throws Exception {
        Task trashedTask = new Task(10, "Old Task", "Desc", 1, 4, "testuser");
        trashedTask.setUpdatedAt("2026-11-20 10:00");
        trashedTask.setDaysRemaining(3);

        when(taskRepository.getTrashedTasksByProjectName("Capstone")).thenReturn(List.of(trashedTask));

        mvc.perform(get("/api/workspace/trash").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(10))
                .andExpect(jsonPath("$[0].daysRemaining").value(3));

        verify(taskRepository).getTrashedTasksByProjectName("Capstone");
    }

    @Test
    void getArchivesUnauthenticatedReturns401() throws Exception {
        mvc.perform(get("/api/workspace/archives"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void getArchivesAuthenticatedReturns200WithFolders() throws Exception {
        Map<String, Object> folder = Map.of(
                "bucketId", 5,
                "name", "Sprint 1",
                "position", 5,
                "tasks", List.of()
        );
        when(taskRepository.getArchiveFoldersWithTasks("Capstone")).thenReturn(List.of(folder));

        mvc.perform(get("/api/workspace/archives").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bucketId").value(5))
                .andExpect(jsonPath("$[0].name").value("Sprint 1"));

        verify(taskRepository).getArchiveFoldersWithTasks("Capstone");
    }

    @Test
    void createArchiveFolderUnauthenticatedReturns401() throws Exception {
        mvc.perform(post("/api/workspace/archives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sprint 1\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void createArchiveFolderSuccessReturns201() throws Exception {
        Map<String, Object> created = Map.of(
                "bucketId", 5,
                "name", "Sprint 1",
                "position", 5,
                "tasks", List.of()
        );
        when(taskRepository.createArchiveFolder("Capstone", "Sprint 1")).thenReturn(created);

        mvc.perform(post("/api/workspace/archives")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectName\": \"Capstone\", \"name\": \"Sprint 1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bucketId").value(5))
                .andExpect(jsonPath("$.name").value("Sprint 1"));

        verify(taskRepository).createArchiveFolder("Capstone", "Sprint 1");
    }

    @Test
    void createArchiveFolderDuplicateReturns400() throws Exception {
        when(taskRepository.createArchiveFolder("Capstone", "Sprint 1"))
                .thenThrow(new IllegalArgumentException("An archive folder with that name already exists."));

        mvc.perform(post("/api/workspace/archives")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectName\": \"Capstone\", \"name\": \"Sprint 1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("An archive folder with that name already exists."));
    }

    @Test
    void deleteArchiveFolderUnauthenticatedReturns401() throws Exception {
        mvc.perform(delete("/api/workspace/archives/5"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void deleteArchiveFolderSuccessReturns204() throws Exception {
        when(taskRepository.deleteArchiveFolder("Capstone", 5)).thenReturn(true);

        mvc.perform(delete("/api/workspace/archives/5").session(session))
                .andExpect(status().isNoContent());

        verify(taskRepository).deleteArchiveFolder("Capstone", 5);
    }

    @Test
    void deleteArchiveFolderNotFoundReturns404() throws Exception {
        when(taskRepository.deleteArchiveFolder("Capstone", 99)).thenReturn(false);

        mvc.perform(delete("/api/workspace/archives/99").session(session))
                .andExpect(status().isNotFound());

        verify(taskRepository).deleteArchiveFolder("Capstone", 99);
    }

    @Test
    void deleteArchiveFolderNotEmptyReturns409Conflict() throws Exception {
        when(taskRepository.deleteArchiveFolder("Capstone", 5))
                .thenThrow(new IllegalStateException("Folder is not empty. Restore or delete its tasks before removing the folder."));

        mvc.perform(delete("/api/workspace/archives/5").session(session))
                .andExpect(status().isConflict())
                .andExpect(content().string("Folder is not empty. Restore or delete its tasks before removing the folder."));
    }
}
