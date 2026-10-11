package com.project.aenko.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthApiTest extends AenkoIntegrationTest {

    @Test
    void registerCreatesUserAndReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"goku\",\"email\":\"goku@aenko.dev\",\"password\":\"kamehameha\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("goku"))
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        createUser("goku", "goku@aenko.dev", "kamehameha");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"vegeta\",\"email\":\"goku@aenko.dev\",\"password\":\"finalflash\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        createUser("goku", "goku@aenko.dev", "kamehameha");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"goku\",\"email\":\"other@aenko.dev\",\"password\":\"finalflash\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerAssignsUserRoleEvenIfAdminRequested() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"piccolo\",\"email\":\"piccolo@aenko.dev\",\"password\":\"makankosappo\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("USER"));
    }

    @Test
    void registerRejectsInvalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginReturnsTokenForValidCredentials() throws Exception {
        createUser("goku", "goku@aenko.dev", "kamehameha");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"goku@aenko.dev\",\"password\":\"kamehameha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("goku@aenko.dev"));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        createUser("goku", "goku@aenko.dev", "kamehameha");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"goku@aenko.dev\",\"password\":\"wrong\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void meReturnsCurrentUserWithValidToken() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "kamehameha");
        mockMvc.perform(get("/api/auth/me").header("Authorization", authHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("goku"));
    }

    @Test
    void meRejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/library")).andExpect(status().isUnauthorized());
    }
}
