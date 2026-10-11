package com.project.aenko.library;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class LibraryApiTest extends AenkoIntegrationTest {

    @Autowired
    private MediaRepository mediaRepository;

    private Media createMedia(String title, int episodes, String genres) {
        Media media = new Media();
        media.setTitle(title);
        media.setEpisodes(episodes);
        media.setGenres(genres);
        media.setScore(8.5);
        return mediaRepository.save(media);
    }

    @Test
    void addListUpdateAndDeleteLibraryEntry() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Naruto", 220, "Action,Adventure");
        String auth = authHeader(user);

        mockMvc.perform(post("/api/library")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":" + media.getId() + ",\"status\":\"WATCHING\",\"rating\":9,\"notes\":\"Great!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Naruto"))
                .andExpect(jsonPath("$.status").value("WATCHING"))
                .andExpect(jsonPath("$.rating").value(9));

        mockMvc.perform(get("/api/library?page=0&size=10").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].genres.length()").value(2));

        mockMvc.perform(put("/api/library/" + media.getId())
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\",\"rating\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(delete("/api/library/" + media.getId()).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/library").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void duplicateLibraryEntryIsRejected() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Naruto", 220, "Action");
        String auth = authHeader(user);

        mockMvc.perform(post("/api/library")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":" + media.getId() + "}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/library")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":" + media.getId() + "}"))
                .andExpect(status().isConflict());
    }

    @Test
    void userCannotSeeAnotherUsersLibrary() throws Exception {
        User userA = createUser("goku", "goku@aenko.dev", "secret123");
        User userB = createUser("vegeta", "vegeta@aenko.dev", "secret123");
        Media media = createMedia("Naruto", 220, "Action");

        mockMvc.perform(post("/api/library")
                        .header("Authorization", authHeader(userB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":" + media.getId() + "}"))
                .andExpect(status().isCreated());

        // userA's library must not contain userB's entry
        mockMvc.perform(get("/api/library").header("Authorization", authHeader(userA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // userA cannot update userB's entry
        mockMvc.perform(put("/api/library/" + media.getId())
                        .header("Authorization", authHeader(userA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void librarySupportsPaginationAndStatusFilter() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        String auth = authHeader(user);
        for (int i = 1; i <= 3; i++) {
            Media media = createMedia("Anime " + i, 12, "Action");
            mockMvc.perform(post("/api/library")
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mediaId\":" + media.getId() + ",\"status\":\""
                                    + (i == 1 ? "COMPLETED" : "PLANNED") + "\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/library?page=0&size=2").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/library?status=COMPLETED").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void invalidRatingIsRejected() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        Media media = createMedia("Naruto", 220, "Action");

        mockMvc.perform(post("/api/library")
                        .header("Authorization", authHeader(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaId\":" + media.getId() + ",\"rating\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists());
    }
}
