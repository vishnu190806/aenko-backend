package com.project.aenko.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.project.aenko.AenkoIntegrationTest;
import com.project.aenko.activity.service.ActivityService;
import com.project.aenko.ai.dto.InsightResponse;
import com.project.aenko.ai.provider.AiProvider;
import com.project.aenko.ai.repository.AiInsightRepository;
import com.project.aenko.ai.service.AiService;
import com.project.aenko.auth.entity.User;
import com.project.aenko.library.repository.UserMediaRepository;
import com.project.aenko.progression.service.ProgressionService;
import com.project.aenko.user.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class AiApiTest extends AenkoIntegrationTest {

    @Autowired
    private AiService aiService;

    @Autowired
    private ProgressionService progressionService;

    @Autowired
    private AiInsightRepository aiInsightRepository;

    @Autowired
    private UserMediaRepository userMediaRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void insightFallsBackGracefullyWhenAiIsDisabled() throws Exception {
        User user = createUser("goku", "goku@aenko.dev", "secret123");

        mockMvc.perform(get("/api/ai/insight").header("Authorization", authHeader(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalized").value(false))
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    @Test
    void aiNeverModifiesAuthoritativeState() {
        User user = createUser("goku", "goku@aenko.dev", "secret123");
        long xpBefore = progressionService.totalXp(user.getId());

        InsightResponse response = aiService.insight(user.getId());

        assertThat(response.personalized()).isFalse();
        assertThat(response.content()).isNotBlank();
        assertThat(progressionService.totalXp(user.getId())).isEqualTo(xpBefore);
    }

    @Test
    void genuineModelResponseIsMarkedPersonalized() {
        User user = createUser("vegeta", "vegeta@aenko.dev", "secret123");
        AiService live = new AiService(
                applicationContext.getBean(ProfileService.class),
                applicationContext.getBean(ActivityService.class),
                stubProvider(true, "You have been exploring action anime. Finish your in-progress series first."),
                aiInsightRepository);

        InsightResponse response = live.insight(user.getId());

        assertThat(response.personalized()).isTrue();
        assertThat(response.content()).contains("in-progress series");
    }

    @Test
    void providerFailureFallsBackWithoutTouchingUserData() {
        User user = createUser("piccolo", "piccolo@aenko.dev", "secret123");
        long xpBefore = progressionService.totalXp(user.getId());
        long libraryBefore = userMediaRepository.count();
        long insightsBefore = aiInsightRepository.count();
        AiService failing = new AiService(
                applicationContext.getBean(ProfileService.class),
                applicationContext.getBean(ActivityService.class),
                stubProvider(true, null),
                aiInsightRepository);

        InsightResponse response = failing.insight(user.getId());

        assertThat(response.personalized()).isFalse();
        assertThat(response.content()).isNotBlank();
        assertThat(progressionService.totalXp(user.getId())).isEqualTo(xpBefore);
        assertThat(userMediaRepository.count()).isEqualTo(libraryBefore);
        // Only the fallback insight row itself may be stored.
        assertThat(aiInsightRepository.count()).isEqualTo(insightsBefore + 1);
    }

    private AiProvider stubProvider(boolean available, String cannedResponse) {
        return new AiProvider() {
            @Override
            public boolean isAvailable() {
                return available;
            }

            @Override
            public String providerName() {
                return "stub";
            }

            @Override
            public String generateInsight(String userContext) {
                if (cannedResponse == null) {
                    throw new IllegalStateException("AI provider unavailable");
                }
                return cannedResponse;
            }
        };
    }
}
