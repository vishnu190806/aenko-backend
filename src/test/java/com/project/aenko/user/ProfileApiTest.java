package com.project.aenko.user;

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
import org.springframework.http.MediaType;

class ProfileApiTest extends AenkoIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Test
    void profileAndStatsAreDerivedFromStoredRecords() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        String auth = authHeader(user);

        Media media = new Media();
        media.setTitle("Naruto");
        media.setEpisodes(2);
        media.setGenres("Action,Adventure");
        media = mediaRepository.save(media);

        mockMvc.perform(post("/api/library")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":" + media.getId() + ",\"status\":\"WATCHING\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/2").header("Authorization", auth))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/profile/stats").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAnime").value(1))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.episodesWatched").value(2))
                // 2 episodes (40) + completion (100) + two achievements (70)
                .andExpect(jsonPath("$.totalXp").value(210))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.topGenres[0].genre").value("Action"));

        mockMvc.perform(get("/api/profile").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("goku"))
                .andExpect(jsonPath("$.stats.completed").value(1));

        // activity recorded the journey
        mockMvc.perform(get("/api/activity").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(6));
    }
}
