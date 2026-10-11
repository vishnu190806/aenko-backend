package com.project.aenko.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.auth.entity.User;
import com.project.aenko.progression.service.ProgressionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ProgressApiTest extends AenkoIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ProgressionService progressionService;

    private Media createMedia(String title, Integer episodes) {
        Media media = new Media();
        media.setTitle(title);
        media.setEpisodes(episodes);
        media.setGenres("Action");
        return mediaRepository.save(media);
    }

    @Test
    void markWatchViewAndRemoveEpisode() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Naruto", 220);
        String auth = authHeader(user);

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchedEpisodes").value(1))
                .andExpect(jsonPath("$.episodes").value(1));

        mockMvc.perform(get("/api/progress/" + media.getId()).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchedEpisodes").value(1));

        assertThat(progressionService.totalXp(user.getId()))
                // 1 episode (20 XP) + "First Episode" achievement (20 XP)
                .isEqualTo(ProgressionService.XP_PER_EPISODE + 20);

        mockMvc.perform(delete("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void rewatchesAfterUnmarkDoNotAwardDuplicateXp() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Naruto", 220);
        String auth = authHeader(user);

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isOk());
        long xpAfterFirstWatch = progressionService.totalXp(user.getId());

        mockMvc.perform(delete("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isNoContent());

        // Re-marking the same episode restores progress but must not pay XP again.
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watchedEpisodes").value(1));

        assertThat(progressionService.totalXp(user.getId())).isEqualTo(xpAfterFirstWatch);
    }

    @Test
    void duplicateEpisodeIsRejected() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Naruto", 220);
        String auth = authHeader(user);

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isConflict());
    }

    @Test
    void episodeBeyondKnownTotalIsRejected() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Short Anime", 12);

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/13")
                        .header("Authorization", authHeader(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonPositiveEpisodeIsRejected() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Naruto", 220);

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/0")
                        .header("Authorization", authHeader(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completingAllEpisodesCompletesAnimeAndAwardsBonusXp() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Short Anime", 2);
        String auth = authHeader(user);

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", auth))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/2").header("Authorization", auth))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/library?status=COMPLETED").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        // 2 episodes (40) + anime completion (100)
        // + "First Episode" (20) + "First Anime Completed" (50) achievements
        long expected = 2L * ProgressionService.XP_PER_EPISODE + ProgressionService.XP_PER_ANIME_COMPLETED + 70;
        assertThat(progressionService.totalXp(user.getId())).isEqualTo(expected);
        assertThat(progressionService.level(user.getId())).isEqualTo(1);
    }
}
