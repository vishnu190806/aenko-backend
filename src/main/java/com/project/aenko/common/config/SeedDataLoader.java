package com.project.aenko.common.config;

import com.project.aenko.achievement.entity.Achievement;
import com.project.aenko.achievement.repository.AchievementRepository;
import com.project.aenko.anime.entity.Media;
import com.project.aenko.anime.entity.MediaExternalId;
import com.project.aenko.anime.repository.MediaExternalIdRepository;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.guide.entity.Franchise;
import com.project.aenko.guide.entity.FranchiseMedia;
import com.project.aenko.guide.entity.WatchOrder;
import com.project.aenko.guide.entity.WatchOrderStep;
import com.project.aenko.guide.repository.FranchiseMediaRepository;
import com.project.aenko.guide.repository.FranchiseRepository;
import com.project.aenko.guide.repository.WatchOrderRepository;
import com.project.aenko.guide.repository.WatchOrderStepRepository;
import com.project.aenko.quest.entity.Quest;
import com.project.aenko.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the fixed quest/achievement catalogue when the database is empty,
 * plus a small curated anime-guide catalogue of real franchises.
 * No users or passwords are ever seeded, and guide seeding never touches
 * user libraries, progress, XP, quests, achievements or guilds.
 * All guide seeding is idempotent: stable keys (franchise name,
 * provider + external id, order name, existing steps) make repeated
 * restarts safe, and no live MAL request is made at startup.
 */
@Component
public class SeedDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);

    private final QuestRepository questRepository;
    private final AchievementRepository achievementRepository;
    private final FranchiseRepository franchiseRepository;
    private final FranchiseMediaRepository franchiseMediaRepository;
    private final WatchOrderRepository watchOrderRepository;
    private final WatchOrderStepRepository watchOrderStepRepository;
    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;

    public SeedDataLoader(QuestRepository questRepository, AchievementRepository achievementRepository,
            FranchiseRepository franchiseRepository, FranchiseMediaRepository franchiseMediaRepository,
            WatchOrderRepository watchOrderRepository, WatchOrderStepRepository watchOrderStepRepository,
            MediaRepository mediaRepository, MediaExternalIdRepository externalIdRepository) {
        this.questRepository = questRepository;
        this.achievementRepository = achievementRepository;
        this.franchiseRepository = franchiseRepository;
        this.franchiseMediaRepository = franchiseMediaRepository;
        this.watchOrderRepository = watchOrderRepository;
        this.watchOrderStepRepository = watchOrderStepRepository;
        this.mediaRepository = mediaRepository;
        this.externalIdRepository = externalIdRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (questRepository.count() == 0) {
            seedQuests();
            log.info("Seeded default quests");
        }
        if (achievementRepository.count() == 0) {
            seedAchievements();
            log.info("Seeded default achievements");
        }
        seedGuide();
    }

    private void seedQuests() {
        questRepository.save(quest("Watch 5 Episodes", "Watch any 5 episodes to warm up your journey.",
                Quest.QuestType.EPISODE_COUNT, 5, 50, "EASY"));
        questRepository.save(quest("Watch 10 Episodes", "Watch any 10 episodes across your library.",
                Quest.QuestType.EPISODE_COUNT, 10, 100, "EASY"));
        questRepository.save(quest("Complete Your First Anime", "Finish every known episode of one anime.",
                Quest.QuestType.ANIME_COUNT, 1, 100, "MEDIUM"));
        questRepository.save(quest("Explore 3 Genres", "Have anime from 3 different genres in your library.",
                Quest.QuestType.GENRE_COUNT, 3, 80, "MEDIUM"));
        questRepository.save(quest("3-Day Streak", "Watch episodes on 3 consecutive days.",
                Quest.QuestType.STREAK, 3, 120, "HARD"));
    }

    private Quest quest(String name, String description, Quest.QuestType type,
            int target, int xp, String difficulty) {
        Quest quest = new Quest();
        quest.setName(name);
        quest.setDescription(description);
        quest.setType(type);
        quest.setTargetValue(target);
        quest.setXpReward(xp);
        quest.setDifficulty(difficulty);
        return quest;
    }

    private void seedAchievements() {
        achievementRepository.save(achievement("First Episode", "Watch your first episode.",
                20, "EPISODES_WATCHED", 1));
        achievementRepository.save(achievement("First Anime Completed", "Complete your first anime.",
                50, "ANIME_COMPLETED", 1));
        achievementRepository.save(achievement("5 Anime Completed", "Complete 5 anime.",
                150, "ANIME_COMPLETED", 5));
        achievementRepository.save(achievement("100 Episodes Watched", "Watch 100 episodes in total.",
                200, "EPISODES_WATCHED", 100));
        achievementRepository.save(achievement("3 Genres Explored", "Collect anime from 3 genres.",
                100, "GENRES_EXPLORED", 3));
        achievementRepository.save(achievement("First Quest Completed", "Complete your first quest.",
                50, "QUESTS_COMPLETED", 1));
        achievementRepository.save(achievement("First Guild Joined", "Join your first guild.",
                30, "GUILD_JOINED", 1));
    }

    private Achievement achievement(String name, String description, int xp,
            String requirementType, int requirementValue) {
        Achievement achievement = new Achievement();
        achievement.setName(name);
        achievement.setDescription(description);
        achievement.setXpReward(xp);
        achievement.setRequirementType(requirementType);
        achievement.setRequirementValue(requirementValue);
        return achievement;
    }

    /**
     * Curated guide catalogue. Entries were verified against MAL title pages
     * (see README): each step is a main-series TV sequel pair in release
     * order. Movies, OVAs and specials are deliberately excluded and each
     * guide says so. Metadata is static and factual (title, type, episode
     * count); no synopsis or artwork is invented, and no provider call is
     * made. Every creation is guarded by a stable-key lookup, so reruns
     * never duplicate rows and never overwrite manually maintained data.
     */
    private void seedGuide() {
        seedFranchise("Naruto",
                "Ninja-era saga. This guide covers the two main TV series only — "
                        + "movies, OVAs and specials are excluded.",
                "Main Series \u2014 Release Order",
                "Watch the two main TV series in release order.",
                List.of(
                        guideEntry("20", "Naruto", 220, 1, "REQUIRED",
                                "Original TV series: 220 episodes (2002–2007). Start here."),
                        guideEntry("1735", "Naruto: Shippuuden", 500, 2, "REQUIRED",
                                "Direct sequel: 500 episodes (2007–2017). Watch after Naruto.")));
        seedFranchise("Steins;Gate",
                "Time-travel thriller. This guide covers the two main TV series only — "
                        + "the movie, OVAs and specials are excluded.",
                "Main Series \u2014 Release Order",
                "Watch the two main TV series in release order.",
                List.of(
                        guideEntry("9253", "Steins;Gate", 24, 1, "REQUIRED",
                                "Original TV series: 24 episodes (2011). Start here."),
                        guideEntry("30484", "Steins;Gate 0", 23, 2, "REQUIRED",
                                "Sequel: 23 episodes (2018). Continues the story via the optional 23\u03b2 bridge special.")));
    }

    private record GuideEntry(String externalId, String title, int episodes,
            int sequence, String stepType, String notes) {
    }

    private GuideEntry guideEntry(String externalId, String title, int episodes,
            int sequence, String stepType, String notes) {
        return new GuideEntry(externalId, title, episodes, sequence, stepType, notes);
    }

    private void seedFranchise(String name, String description, String orderName,
            String orderDescription, List<GuideEntry> entries) {
        Franchise franchise = franchiseRepository.findByName(name).orElseGet(() -> {
            Franchise created = new Franchise();
            created.setName(name);
            created.setDescription(description);
            return franchiseRepository.save(created);
        });

        List<Media> media = entries.stream().map(this::guideMedia).toList();
        for (Media m : media) {
            if (franchiseMediaRepository.findByFranchiseIdAndMediaId(franchise.getId(), m.getId()).isEmpty()) {
                FranchiseMedia link = new FranchiseMedia();
                link.setFranchiseId(franchise.getId());
                link.setMediaId(m.getId());
                franchiseMediaRepository.save(link);
            }
        }

        WatchOrder order = watchOrderRepository
                .findByFranchiseIdAndName(franchise.getId(), orderName).orElseGet(() -> {
                    WatchOrder created = new WatchOrder();
                    created.setFranchiseId(franchise.getId());
                    created.setName(orderName);
                    created.setDescription(orderDescription);
                    return watchOrderRepository.save(created);
                });

        if (watchOrderStepRepository.findByWatchOrderIdOrderBySequenceNumberAsc(order.getId()).isEmpty()) {
            for (int i = 0; i < entries.size(); i++) {
                WatchOrderStep step = new WatchOrderStep();
                step.setWatchOrderId(order.getId());
                step.setMediaId(media.get(i).getId());
                step.setSequenceNumber(entries.get(i).sequence());
                step.setStepType(entries.get(i).stepType());
                step.setNotes(entries.get(i).notes());
                watchOrderStepRepository.save(step);
            }
            log.info("Seeded curated guide '{}' with {} steps", name, entries.size());
        }
    }

    /**
     * Resolves the canonical {@link Media} for a curated MAL id, creating a
     * metadata-only row plus the {@code MAL → externalId} mapping on first
     * use. Never touches user libraries or progress.
     */
    private Media guideMedia(GuideEntry entry) {
        return externalIdRepository.findByProviderAndExternalId("MAL", entry.externalId())
                .flatMap(link -> mediaRepository.findById(link.getMediaId()))
                .orElseGet(() -> {
                    Media media = new Media();
                    media.setTitle(entry.title());
                    media.setType("tv");
                    media.setStatus("finished_airing");
                    media.setEpisodes(entry.episodes());
                    media.setUpdatedAt(Instant.now());
                    media = mediaRepository.save(media);

                    MediaExternalId link = new MediaExternalId();
                    link.setMediaId(media.getId());
                    link.setProvider("MAL");
                    link.setExternalId(entry.externalId());
                    link.setExternalUrl("https://myanimelist.net/anime/" + entry.externalId());
                    link.setLastSyncedAt(null);
                    externalIdRepository.save(link);
                    return media;
                });
    }
}
