package com.project.aenko.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.auth.entity.User;
import org.junit.jupiter.api.Test;

class SecurityCorsTest extends AenkoIntegrationTest {

    @Test
    void corsPreflightAllowsFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/library")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void allProtectedResourcesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/library")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/progress/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/quests/my")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/achievements/my")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/profile/stats")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/activity")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/ai/insight")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicCatalogEndpointsDoNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/quests")).andExpect(status().isOk());
        mockMvc.perform(get("/api/achievements")).andExpect(status().isOk());
        mockMvc.perform(get("/api/guilds")).andExpect(status().isOk());
        mockMvc.perform(get("/api/franchises")).andExpect(status().isOk());
    }

    @Test
    void tamperedUserIdInTokenIsRejected() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        String valid = tokenFor(user);
        String tampered = valid.substring(0, valid.length() - 2) + "xx";
        mockMvc.perform(get("/api/profile").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }
}
