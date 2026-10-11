package com.project.aenko.guide;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.anime.repository.MediaExternalIdRepository;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.common.config.SeedDataLoader;
import com.project.aenko.guide.repository.FranchiseRepository;
import com.project.aenko.guide.repository.WatchOrderRepository;
import com.project.aenko.guide.repository.WatchOrderStepRepository;
import com.project.aenko.library.repository.UserMediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;

class GuideApiTest extends AenkoIntegrationTest {

    @Autowired
    private SeedDataLoader seedDataLoader;

    @Autowired
    private FranchiseRepository franchiseRepository;

    @Autowired
    private WatchOrderRepository watchOrderRepository;

    @Autowired
    private WatchOrderStepRepository watchOrderStepRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private MediaExternalIdRepository externalIdRepository;

    @Autowired
    private UserMediaRepository userMediaRepository;

    @Test
    void curatedCatalogueIsSeededWithRealFranchises() throws Exception {
        mockMvc.perform(get("/api/franchises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].name").value(
                        org.hamcrest.Matchers.hasItems("Naruto", "Steins;Gate")));
    }

    @Test
    void fictionalExampleSagaIsAbsent() {
        assertThat(franchiseRepository.findAll())
                .noneMatch(f -> "Example Saga".equals(f.getName()));
    }

    @Test
    void narutoWatchOrderHasVerifiedSteps() throws Exception {
        Long narutoId = franchiseRepository.findAll().stream()
                .filter(f -> "Naruto".equals(f.getName()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(get("/api/franchises/" + narutoId + "/watch-order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Main Series \u2014 Release Order"))
                .andExpect(jsonPath("$[0].steps.length()").value(2))
                .andExpect(jsonPath("$[0].steps[0].title").value("Naruto"))
                .andExpect(jsonPath("$[0].steps[0].externalId").value("20"))
                .andExpect(jsonPath("$[0].steps[0].stepType").value("REQUIRED"))
                .andExpect(jsonPath("$[0].steps[1].title").value("Naruto: Shippuuden"))
                .andExpect(jsonPath("$[0].steps[1].externalId").value("1735"));
    }

    @Test
    void steinsGateWatchOrderHasVerifiedSteps() throws Exception {
        Long sgId = franchiseRepository.findAll().stream()
                .filter(f -> "Steins;Gate".equals(f.getName()))
                .findFirst().orElseThrow().getId();

        mockMvc.perform(get("/api/franchises/" + sgId + "/watch-order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].steps.length()").value(2))
                .andExpect(jsonPath("$[0].steps[0].title").value("Steins;Gate"))
                .andExpect(jsonPath("$[0].steps[0].externalId").value("9253"))
                .andExpect(jsonPath("$[0].steps[1].title").value("Steins;Gate 0"))
                .andExpect(jsonPath("$[0].steps[1].externalId").value("30484"));
    }

    @Test
    void stepsResolveToCanonicalMediaWithMalMappings() {
        for (String malId : new String[]{"20", "1735", "9253", "30484"}) {
            var link = externalIdRepository.findByProviderAndExternalId("MAL", malId);
            assertThat(link).as("MAL mapping for %s", malId).isPresent();
            assertThat(mediaRepository.findById(link.get().getMediaId()))
                    .as("canonical media for MAL %s", malId).isPresent();
        }
    }

    @Test
    void franchiseMediaCarriesExternalIdsForMatching() throws Exception {
        mockMvc.perform(get("/api/franchises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Naruto')].media[*].externalId")
                        .value(org.hamcrest.Matchers.hasItems("20", "1735")));
    }

    @Test
    void catalogueIsAvailableWithoutAnyLibraryEntries() {
        assertThat(userMediaRepository.count()).isEqualTo(0);
        assertThat(franchiseRepository.count()).isEqualTo(2);
        assertThat(watchOrderStepRepository.count()).isEqualTo(4);
    }

    @Test
    void repeatedSeedingDoesNotDuplicateCatalogue() {
        long franchises = franchiseRepository.count();
        long orders = watchOrderRepository.count();
        long steps = watchOrderStepRepository.count();
        long media = mediaRepository.count();

        seedDataLoader.run(new DefaultApplicationArguments(new String[]{}));
        seedDataLoader.run(new DefaultApplicationArguments(new String[]{}));

        assertThat(franchiseRepository.count()).isEqualTo(franchises);
        assertThat(watchOrderRepository.count()).isEqualTo(orders);
        assertThat(watchOrderStepRepository.count()).isEqualTo(steps);
        assertThat(mediaRepository.count()).isEqualTo(media);
    }

    @Test
    void unknownFranchiseReturns404() throws Exception {
        mockMvc.perform(get("/api/franchises/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void unknownFranchiseWatchOrderReturns404() throws Exception {
        mockMvc.perform(get("/api/franchises/9999/watch-order"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void malformedIdReturns400Not500() throws Exception {
        mockMvc.perform(get("/api/franchises/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
