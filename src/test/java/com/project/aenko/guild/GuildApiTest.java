package com.project.aenko.guild;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class GuildApiTest extends AenkoIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Long createGuild(String auth, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/guilds")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"tag\":\"TAG\",\"description\":\"Desc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    @Test
    void createListJoinLeaveAndMembers() throws Exception {
        User owner = createUser("goku", "goku@aenko.dev", "secret123");
        User member = createUser("vegeta", "vegeta@aenko.dev", "secret123");

        Long guildId = createGuild(authHeader(owner), "Z Fighters");

        mockMvc.perform(get("/api/guilds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Z Fighters"))
                .andExpect(jsonPath("$[0].memberCount").value(1));

        mockMvc.perform(post("/api/guilds/" + guildId + "/join")
                        .header("Authorization", authHeader(member)))
                .andExpect(status().isOk());

        // duplicate join rejected
        mockMvc.perform(post("/api/guilds/" + guildId + "/join")
                        .header("Authorization", authHeader(member)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/guilds/" + guildId + "/members").header("Authorization", authHeader(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // owner cannot abandon the guild
        mockMvc.perform(post("/api/guilds/" + guildId + "/leave").header("Authorization", authHeader(owner)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/guilds/" + guildId + "/leave").header("Authorization", authHeader(member)))
                .andExpect(status().isOk());
    }

    @Test
    void guildCreationRequiresName() throws Exception {
        User owner = createUser("goku", "goku@aenko.dev", "secret123");
        mockMvc.perform(post("/api/guilds")
                        .header("Authorization", authHeader(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void guildXpQuestAndLeaderboard() throws Exception {
        User owner = createUser("goku", "goku@aenko.dev", "secret123");
        User member = createUser("vegeta", "vegeta@aenko.dev", "secret123");
        String ownerAuth = authHeader(owner);
        String memberAuth = authHeader(member);

        Long guildId = createGuild(ownerAuth, "Z Fighters");
        mockMvc.perform(post("/api/guilds/" + guildId + "/join").header("Authorization", memberAuth))
                .andExpect(status().isOk());

        // non-manager cannot create guild quests
        mockMvc.perform(post("/api/guilds/" + guildId + "/quests")
                        .header("Authorization", memberAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Q\",\"targetValue\":10,\"xpReward\":50}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/guilds/" + guildId + "/quests")
                        .header("Authorization", ownerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Watch 2 Episodes Together\",\"targetValue\":2,\"xpReward\":50}"))
                .andExpect(status().isCreated());

        Media media = new Media();
        media.setTitle("Naruto");
        media.setEpisodes(220);
        media.setGenres("Action");
        media = mediaRepository.save(media);

        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/1").header("Authorization", memberAuth))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/progress/" + media.getId() + "/episode/2").header("Authorization", memberAuth))
                .andExpect(status().isOk());

        // guild XP accumulated: 2 episodes x 5 XP + quest reward (50) paid once
        mockMvc.perform(get("/api/guilds/" + guildId).header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(60));

        // collective quest auto-completed by member contributions
        mockMvc.perform(get("/api/guilds/" + guildId + "/quests").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].currentValue").value(2));

        mockMvc.perform(get("/api/guilds/" + guildId + "/leaderboard").header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("vegeta"))
                .andExpect(jsonPath("$[0].xp").value(60));
    }
}
