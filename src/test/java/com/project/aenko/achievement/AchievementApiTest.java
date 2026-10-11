package com.project.aenko.achievement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AchievementApiTest extends AenkoIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Test
    void achievementsListedPubliclyAndUnlockedByBackendRules() throws Exception {
        mockMvc.perform(get("/api/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));

        User user = createUser("goku", "goku@aenko.dev", "secret123");
        String auth = authHeader(user);

        mockMvc.perform(get("/api/achievements/my").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        Media media = new Media();
        media.setTitle("Naruto");
        media.setEpisodes(220);
        media.setGenres("Action");
        media = mediaRepository.save(media);
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isOk());

        // backend unlocked "First Episode" (and possibly genre quest achievements) — no client action needed
        mockMvc.perform(get("/api/achievements/my").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='First Episode')]").exists());
    }
}
