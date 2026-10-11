package com.project.aenko.quest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.auth.entity.User;
import com.project.aenko.progression.service.ProgressionService;
import com.project.aenko.quest.repository.QuestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QuestApiTest extends AenkoIntegrationTest {

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private ProgressionService progressionService;

    @Test
    void seededQuestsAreListedPublicly() throws Exception {
        mockMvc.perform(get("/api/quests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void startProgressAndCompleteQuest() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        String auth = authHeader(user);
        Long questId = questRepository.findAll().stream()
                .filter(q -> q.getTargetValue() == 5)
                .findFirst().orElseThrow().getId();

        mockMvc.perform(post("/api/quests/" + questId + "/start").header("Authorization", auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("STARTED"));

        // duplicate start is rejected
        mockMvc.perform(post("/api/quests/" + questId + "/start").header("Authorization", auth))
                .andExpect(status().isConflict());

        // premature completion is rejected by backend verification
        mockMvc.perform(post("/api/quests/" + questId + "/complete").header("Authorization", auth))
                .andExpect(status().isBadRequest());

        // watch 5 episodes -> quest auto-completes and pays XP once
        Media media = new Media();
        media.setTitle("Naruto");
        media.setEpisodes(220);
        media.setGenres("Action");
        media = mediaRepository.save(media);
        for (int ep = 1; ep <= 5; ep++) {
            mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/" + ep)
                            .header("Authorization", auth))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/quests/my").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + questId + ")].status").value("COMPLETED"));

        long xpAfterCompletion = progressionService.totalXp(user.getId());

        // completing again must not pay XP twice
        mockMvc.perform(post("/api/quests/" + questId + "/complete").header("Authorization", auth))
                .andExpect(status().isOk());
        assertThat(progressionService.totalXp(user.getId())).isEqualTo(xpAfterCompletion);
    }
}
