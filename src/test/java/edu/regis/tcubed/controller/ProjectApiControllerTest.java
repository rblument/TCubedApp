package edu.regis.tcubed.controller;

import edu.regis.tcubed.model.Account;
import edu.regis.tcubed.model.Project;
import edu.regis.tcubed.repository.ProjectRepository;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Adapt controller construction/field name to the actual current controller. */
class ProjectApiControllerTest {
    private MockMvc mvc;
    private ProjectRepository repository;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        repository = mock(ProjectRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProjectApiController(repository)).build();
        session = new MockHttpSession();
        session.setAttribute("userId", "user@example.com");
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void listsProjectsForSignedInUser() throws Exception {
        when(repository.getProjectsByUserId("user@example.com"))
                .thenReturn(List.of(new Project(7, "Capstone", "Team work")));
        mvc.perform(get("/api/projects").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(7))
                .andExpect(jsonPath("$[0].name").value("Capstone"));
        verify(repository).getProjectsByUserId("user@example.com");
    }

    @Test
    void retrievesAnAccessibleProject() throws Exception {
        when(repository.findProject(7, "user@example.com"))
                .thenReturn(new Project(7, "Capstone", "Team work"));
        mvc.perform(get("/api/projects/7").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Capstone"));
    }

    @Test
    void missingOrInaccessibleProjectReturnsNotFound() throws Exception {
        mvc.perform(get("/api/projects/99").session(session))
                .andExpect(status().isNotFound());
        verify(repository).findProject(99, "user@example.com");
    }

    @Test
    void createsValidProject() throws Exception {
        when(repository.createProject("New Project", "Description", "user@example.com"))
                .thenReturn(new Project(9, "New Project", "Description"));
        mvc.perform(post("/api/projects").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New Project\",\"description\":\"Description\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(9));
    }

    @Test
    void rejectsBlankProjectNameWithoutWritingToRepository() throws Exception {
        mvc.perform(post("/api/projects").session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }


    @Test
    void updatesExistingProject() throws Exception {
        when(repository.updateProject(7, "Renamed", "Updated", "user@example.com"))
                .thenReturn(new Project(7, "Renamed", "Updated"));
        mvc.perform(put("/api/projects/7").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Renamed\",\"description\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }



    @Test
    void deletesExistingProject() throws Exception {
        when(repository.deleteProject(7, "user@example.com")).thenReturn(true);
        mvc.perform(delete("/api/projects/7").session(session))
                .andExpect(status().isNoContent());
    }


    @Test
    void deletionIntegrityConflictReturnsConflict() throws Exception {
        when(repository.deleteProject(7, "user@example.com"))
                .thenThrow(new SQLIntegrityConstraintViolationException("Foreign key"));
        mvc.perform(delete("/api/projects/7").session(session))
                .andExpect(status().isConflict());
    }


    @Test
    void addsProjectMember() throws Exception {
        when(repository.addProjectMember(7, "teammate@example.com", "user@example.com"))
                .thenReturn(true);
        when(repository.getProjectMembers(7, "user@example.com"))
                .thenReturn(List.of("user@example.com", "teammate@example.com"));
        mvc.perform(post("/api/projects/7/members").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\" teammate@example.com \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1]").value("teammate@example.com"));
        verify(repository).addProjectMember(7, "teammate@example.com", "user@example.com");
    }


    @Test
    void forbidsAddingMemberWhenRepositoryDeniesAccess() throws Exception {
        mvc.perform(post("/api/projects/7/members").session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"teammate@example.com\"}"))
                .andExpect(status().isForbidden());
        verify(repository, never()).getProjectMembers(anyInt(), anyString());
    }


    @Test
    void removesProjectMember() throws Exception {
        when(repository.removeProjectMember(7, "teammate@example.com", "user@example.com"))
                .thenReturn(true);
        when(repository.getProjectMembers(7, "user@example.com"))
                .thenReturn(List.of("user@example.com"));
        mvc.perform(delete("/api/projects/7/members/teammate@example.com").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("user@example.com"));
        verify(repository).removeProjectMember(7, "teammate@example.com", "user@example.com");
    }

    @Test
    void getMembersByNameUnauthenticatedIsRejected() throws Exception {
        mvc.perform(get("/api/projects/members/by-name").param("projectName", "Capstone"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
    }

    @Test
    void getMembersByNameReturnsMembersForAuthorizedUser() throws Exception {
        when(repository.getProjectsByUserId("user@example.com"))
                .thenReturn(List.of(new Project(1, "Capstone", "Workspace")));

        Account a1 = new Account();
        a1.setUserId("user@example.com");
        a1.setFirstName("Oscar");
        a1.setLastName("Castillo");

        Account a2 = new Account();
        a2.setUserId("dylan@example.com");
        a2.setFirstName("Dylan");
        a2.setLastName("Cassagnol");

        when(repository.getProjectMemberAccountsByProjectName("Capstone"))
                .thenReturn(List.of(a1, a2));

        mvc.perform(get("/api/projects/members/by-name")
                .param("projectName", "Capstone")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user@example.com"))
                .andExpect(jsonPath("$[0].firstName").value("Oscar"))
                .andExpect(jsonPath("$[0].lastName").value("Castillo"))
                .andExpect(jsonPath("$[1].userId").value("dylan@example.com"))
                .andExpect(jsonPath("$[1].firstName").value("Dylan"))
                .andExpect(jsonPath("$[1].lastName").value("Cassagnol"));

        verify(repository).getProjectMemberAccountsByProjectName("Capstone");
    }

    @Test
    void getMembersByNameForbiddenWhenUserNotMember() throws Exception {
        when(repository.getProjectsByUserId("user@example.com"))
                .thenReturn(List.of(new Project(1, "OtherProject", "Workspace")));
        when(repository.getProjectMemberAccountsByProjectName("SecretProject"))
                .thenReturn(List.of());

        mvc.perform(get("/api/projects/members/by-name")
                .param("projectName", "SecretProject")
                .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMembersByNameRejectsBlankProjectName() throws Exception {
        mvc.perform(get("/api/projects/members/by-name")
                .param("projectName", "   ")
                .session(session))
                .andExpect(status().isBadRequest());
    }
}
