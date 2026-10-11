package com.project.aenko.guild;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.auth.entity.User;
import com.project.aenko.guild.repository.GuildXpTransactionRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class GuildQuestRewardTest extends AenkoIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private GuildXpTransactionRepository guildXpTransactionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Media createMedia() {
        Media media = new Media();
        media.setTitle("Naruto");
        media.setEpisodes(220);
        media.setGenres("Action");
        return mediaRepository.save(media);
    }

    private Long createGuildWithQuest(String ownerAuth, String name, int target, int reward) throws Exception {
        String guildBody = mockMvc.perform(post("/api/guilds")
                        .header("Authorization", ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long guildId = objectMapper.readTree(guildBody).get("id").asLong();
        mockMvc.perform(post("/api/guilds/" + guildId + "/quests")
                        .header("Authorization", ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Quest\",\"targetValue\":" + target + ",\"xpReward\":" + reward + "}"))
                .andExpect(status().isCreated());
        return guildId;
    }

    private long rewardTransactions(Long guildId, Long questId) {
        return guildXpTransactionRepository.findByGuildId(guildId).stream()
                .filter(tx -> "GUILD_QUEST".equals(tx.getSourceType())
                        && String.valueOf(questId).equals(tx.getSourceId()))
                .count();
    }

    @Test
    void rewardIsSplitProportionallyOnCompletion() throws Exception {
        User owner = createUser("goku", "goku@aenko.dev", "secret123");
        User member = createUser("vegeta", "vegeta@aenko.dev", "secret123");
        String ownerAuth = authHeader(owner);
        String memberAuth = authHeader(member);

        Long guildId = createGuildWithQuest(ownerAuth, "Z Fighters", 4, 100);
        mockMvc.perform(post("/api/guilds/" + guildId + "/join").header("Authorization", memberAuth))
                .andExpect(status().isOk());
        Media media = createMedia();

        // owner contributes 1, member contributes 3 -> quest completes on member's 3rd episode
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", ownerAuth))
                .andExpect(status().isOk());
        for (int ep = 1; ep <= 3; ep++) {
            mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/" + ep)
                            .header("Authorization", memberAuth))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/guilds/" + guildId + "/quests").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].currentValue").value(4));

        // 4 episodes x 5 guild XP + 100 quest reward = 120 guild XP
        mockMvc.perform(get("/api/guilds/" + guildId).header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(120));

        // proportional split: owner 25 (1/4), member 75 (3/4) on top of episode XP
        String boardBody = mockMvc.perform(get("/api/guilds/" + guildId + "/leaderboard")
                        .header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Long> boardXp = new java.util.HashMap<>();
        for (JsonNode row : objectMapper.readTree(boardBody)) {
            boardXp.put(row.get("username").asText(), row.get("xp").asLong());
        }
        assertThat(boardXp.get("vegeta")).isEqualTo(90L);
        assertThat(boardXp.get("goku")).isEqualTo(30L);
    }

    @Test
    void rewardIsPaidExactlyOnceDespiteFurtherActivity() throws Exception {
        User owner = createUser("goku", "goku@aenko.dev", "secret123");
        String ownerAuth = authHeader(owner);

        Long guildId = createGuildWithQuest(ownerAuth, "Z Fighters", 2, 50);
        // quest id is the only quest of the guild
        String questsBody = mockMvc.perform(get("/api/guilds/" + guildId + "/quests")
                        .header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode quests = objectMapper.readTree(questsBody);
        Long questId = quests.get(0).get("id").asLong();
        Media media = createMedia();

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", ownerAuth))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/2").header("Authorization", ownerAuth))
                .andExpect(status().isOk());
        assertThat(rewardTransactions(guildId, questId)).isEqualTo(1);

        // further activity only adds episode XP, never a second quest reward
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/3").header("Authorization", ownerAuth))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/4").header("Authorization", ownerAuth))
                .andExpect(status().isOk());

        assertThat(rewardTransactions(guildId, questId)).isEqualTo(1);
        mockMvc.perform(get("/api/guilds/" + guildId + "/quests").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
        // 4 episodes x 5 + single 50 reward
        mockMvc.perform(get("/api/guilds/" + guildId).header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(70));
    }

    @Test
    void noRewardBeforeTargetIsReached() throws Exception {
        User owner = createUser("goku", "goku@aenko.dev", "secret123");
        String ownerAuth = authHeader(owner);

        Long guildId = createGuildWithQuest(ownerAuth, "Z Fighters", 50, 500);
        Media media = createMedia();
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", ownerAuth))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/2").header("Authorization", ownerAuth))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/guilds/" + guildId + "/quests").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].currentValue").value(2));

        // only episode XP, no quest reward
        mockMvc.perform(get("/api/guilds/" + guildId).header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(10));
        assertThat(guildXpTransactionRepository.findByGuildId(guildId).stream()
                .noneMatch(tx -> "GUILD_QUEST".equals(tx.getSourceType()))).isTrue();
    }
}
