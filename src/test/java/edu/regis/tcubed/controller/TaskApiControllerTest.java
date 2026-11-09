package edu.regis.tcubed.controller;

import edu.regis.tcubed.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Adapt controller construction/field name to the actual current controller. */
class TaskApiControllerTest {
    private MockMvc mvc;
    private TaskRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(TaskRepository.class);
        TaskApiController controller =
        new TaskApiController(repository);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }
}

