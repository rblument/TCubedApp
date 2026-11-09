package edu.regis.tcubed.controller;

import edu.regis.tcubed.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Adapt controller construction/field name to the actual current controller. */
class ProjectApiControllerTest {
    private MockMvc mvc;
    private ProjectRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(ProjectRepository.class);
        ProjectApiController controller =
        new ProjectApiController(repository);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/projects"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }
}

