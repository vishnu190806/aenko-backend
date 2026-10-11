package com.project.aenko.guild.service;

import com.project.aenko.achievement.service.AchievementService;
import com.project.aenko.activity.service.ActivityService;
import com.project.aenko.auth.repository.UserRepository;
import com.project.aenko.common.exception.DuplicateResourceException;
import com.project.aenko.common.exception.ResourceNotFoundException;
import com.project.aenko.guild.dto.GuildDto;
import com.project.aenko.guild.dto.GuildMemberDto;
import com.project.aenko.guild.dto.GuildQuestDto;
import com.project.aenko.guild.dto.GuildQuestRequest;
import com.project.aenko.guild.dto.GuildRequest;
import com.project.aenko.guild.dto.LeaderboardEntry;
import com.project.aenko.guild.entity.Guild;
import com.project.aenko.guild.entity.GuildMember;
import com.project.aenko.guild.entity.GuildQuest;
import com.project.aenko.guild.entity.GuildQuestProgress;
import com.project.aenko.guild.entity.GuildXpTransaction;
import com.project.aenko.guild.repository.GuildMemberRepository;
import com.project.aenko.guild.repository.GuildQuestProgressRepository;
import com.project.aenko.guild.repository.GuildQuestRepository;
import com.project.aenko.guild.repository.GuildRepository;
import com.project.aenko.guild.repository.GuildXpTransactionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuildService {

    public static final int GUILD_XP_PER_EPISODE = 5;

    private final GuildRepository guildRepository;
    private final GuildMemberRepository guildMemberRepository;
    private final GuildQuestRepository guildQuestRepository;
    private final GuildQuestProgressRepository guildQuestProgressRepository;
    private final GuildXpTransactionRepository guildXpTransactionRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final AchievementService achievementService;

    public GuildService(GuildRepository guildRepository, GuildMemberRepository guildMemberRepository,
            GuildQuestRepository guildQuestRepository,
            GuildQuestProgressRepository guildQuestProgressRepository,
            GuildXpTransactionRepository guildXpTransactionRepository,
            UserRepository userRepository, ActivityService activityService,
            @Lazy AchievementService achievementService) {
        this.guildRepository = guildRepository;
        this.guildMemberRepository = guildMemberRepository;
        this.guildQuestRepository = guildQuestRepository;
        this.guildQuestProgressRepository = guildQuestProgressRepository;
        this.guildXpTransactionRepository = guildXpTransactionRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.achievementService = achievementService;
    }

    @Transactional
    public GuildDto create(Long ownerId, GuildRequest request) {
        Guild guild = new Guild();
        guild.setName(request.name());
        guild.setTag(request.tag());
        guild.setDescription(request.description());
        guild.setOwnerId(ownerId);
        guild.setPrivacy(request.privacy() != null ? request.privacy().toUpperCase() : Guild.Privacy.PUBLIC.name());
        guild = guildRepository.save(guild);

        GuildMember owner = new GuildMember();
        owner.setGuildId(guild.getId());
        owner.setUserId(ownerId);
        owner.setRole(GuildMember.GuildRole.OWNER.name());
        guildMemberRepository.save(owner);

        activityService.record(ownerId, "CREATED_GUILD", guild.getId(), guild.getName());
        activityService.record(ownerId, "JOINED_GUILD", guild.getId(), guild.getName());
        achievementService.checkAchievements(ownerId);
        return toDto(guild);
    }

    @Transactional(readOnly = true)
    public List<GuildDto> listPublic() {
        return guildRepository.findByPrivacy(Guild.Privacy.PUBLIC.name()).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuildDto get(Long guildId) {
        return toDto(requireGuild(guildId));
    }

    @Transactional
    public void join(Long userId, Long guildId) {
        Guild guild = requireGuild(guildId);
        if (guildMemberRepository.findByGuildIdAndUserId(guildId, userId).isPresent()) {
            throw new DuplicateResourceException("You are already a member of this guild");
        }
        GuildMember member = new GuildMember();
        member.setGuildId(guildId);
        member.setUserId(userId);
        member.setRole(GuildMember.GuildRole.MEMBER.name());
        guildMemberRepository.save(member);
        activityService.record(userId, "JOINED_GUILD", guildId, guild.getName());
        achievementService.checkAchievements(userId);
    }

    @Transactional
    public void leave(Long userId, Long guildId) {
        GuildMember member = guildMemberRepository.findByGuildIdAndUserId(guildId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this guild"));
        if (GuildMember.GuildRole.OWNER.name().equals(member.getRole())) {
            throw new IllegalArgumentException("The guild owner cannot leave the guild");
        }
        guildMemberRepository.delete(member);
        activityService.record(userId, "LEFT_GUILD", guildId, null);
    }

    @Transactional(readOnly = true)
    public List<GuildMemberDto> members(Long guildId) {
        requireGuild(guildId);
        Map<Long, String> usernames = userRepository.findAllById(
                guildMemberRepository.findByGuildId(guildId).stream().map(GuildMember::getUserId).toList())
                .stream().collect(Collectors.toMap(u -> u.getId(), u -> u.getUsername()));
        return guildMemberRepository.findByGuildId(guildId).stream()
                .map(member -> new GuildMemberDto(member.getUserId(),
                        usernames.getOrDefault(member.getUserId(), "unknown"),
                        member.getRole(), member.getJoinedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard(Long guildId) {
        requireGuild(guildId);
        Map<Long, String> usernames = userRepository.findAll().stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getUsername()));
        return guildXpTransactionRepository.leaderboardByGuild(guildId).stream()
                .map(row -> new LeaderboardEntry(row.getUserId(),
                        usernames.getOrDefault(row.getUserId(), "unknown"), row.getXp()))
                .toList();
    }

    @Transactional
    public GuildQuestDto createQuest(Long userId, Long guildId, GuildQuestRequest request) {
        requireManager(userId, guildId);
        GuildQuest quest = new GuildQuest();
        quest.setGuildId(guildId);
        quest.setName(request.name());
        quest.setDescription(request.description());
        quest.setTargetValue(request.targetValue());
        quest.setXpReward(request.xpReward());
        quest.setStatus("ACTIVE");
        quest = guildQuestRepository.save(quest);
        return GuildQuestDto.from(quest, 0);
    }

    @Transactional(readOnly = true)
    public List<GuildQuestDto> quests(Long guildId) {
        requireGuild(guildId);
        return guildQuestRepository.findByGuildId(guildId).stream()
                .map(quest -> GuildQuestDto.from(quest, questProgressTotal(quest.getId())))
                .toList();
    }

    /**
     * Called after a member's verified episode watch. Contributes +1 to every
     * active guild quest and records guild XP. When a quest reaches its target
     * it completes exactly once and pays its configured reward.
     * All amounts are server-side.
     */
    @Transactional
    public void contributeEpisode(Long userId, Long mediaId) {
        for (GuildMember membership : guildMemberRepository.findByUserId(userId)) {
            GuildXpTransaction transaction = new GuildXpTransaction();
            transaction.setGuildId(membership.getGuildId());
            transaction.setUserId(userId);
            transaction.setSourceType("EPISODE");
            transaction.setSourceId(String.valueOf(mediaId));
            transaction.setAmount(GUILD_XP_PER_EPISODE);
            guildXpTransactionRepository.save(transaction);

            for (GuildQuest quest : guildQuestRepository.findByGuildIdAndStatus(membership.getGuildId(), "ACTIVE")) {
                GuildQuestProgress progress = guildQuestProgressRepository
                        .findByGuildQuestIdAndUserId(quest.getId(), userId)
                        .orElseGet(() -> {
                            GuildQuestProgress created = new GuildQuestProgress();
                            created.setGuildQuestId(quest.getId());
                            created.setUserId(userId);
                            return created;
                        });
                progress.setContribution(progress.getContribution() + 1);
                guildQuestProgressRepository.save(progress);
                if (questProgressTotal(quest.getId()) >= quest.getTargetValue()
                        && "ACTIVE".equals(quest.getStatus())) {
                    quest.setStatus("COMPLETED");
                    guildQuestRepository.save(quest);
                    payGuildQuestReward(quest);
                }
            }
        }
    }

    private int questProgressTotal(Long guildQuestId) {
        return guildQuestProgressRepository.findByGuildQuestId(guildQuestId).stream()
                .mapToInt(GuildQuestProgress::getContribution).sum();
    }

    /**
     * Pays the quest's configured XP reward as guild XP, split among
     * contributors in proportion to their contributions (integer shares that
     * sum to exactly the reward). Runs at most once per quest: the reward
     * transaction itself is the idempotency record.
     */
    private void payGuildQuestReward(GuildQuest quest) {
        if (quest.getXpReward() <= 0) {
            return;
        }
        if (guildXpTransactionRepository.existsByGuildIdAndSourceTypeAndSourceId(
                quest.getGuildId(), "GUILD_QUEST", String.valueOf(quest.getId()))) {
            return;
        }
        List<GuildQuestProgress> contributors = guildQuestProgressRepository.findByGuildQuestId(quest.getId())
                .stream()
                .filter(progress -> progress.getContribution() > 0)
                .sorted(Comparator.comparingInt(GuildQuestProgress::getContribution).reversed())
                .toList();
        int total = contributors.stream().mapToInt(GuildQuestProgress::getContribution).sum();
        if (contributors.isEmpty() || total <= 0) {
            return;
        }
        int remaining = quest.getXpReward();
        for (int i = 0; i < contributors.size(); i++) {
            GuildQuestProgress contributor = contributors.get(i);
            int share = (i == contributors.size() - 1)
                    ? remaining
                    : (int) ((long) quest.getXpReward() * contributor.getContribution() / total);
            remaining -= share;
            if (share <= 0) {
                continue;
            }
            GuildXpTransaction bonus = new GuildXpTransaction();
            bonus.setGuildId(quest.getGuildId());
            bonus.setUserId(contributor.getUserId());
            bonus.setSourceType("GUILD_QUEST");
            bonus.setSourceId(String.valueOf(quest.getId()));
            bonus.setAmount(share);
            guildXpTransactionRepository.save(bonus);
        }
    }

    private void requireManager(Long userId, Long guildId) {
        GuildMember member = guildMemberRepository.findByGuildIdAndUserId(guildId, userId)
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this guild"));
        if (!GuildMember.GuildRole.OWNER.name().equals(member.getRole())
                && !GuildMember.GuildRole.ADMIN.name().equals(member.getRole())) {
            throw new AccessDeniedException("Only guild owners/admins can manage guild quests");
        }
    }

    private Guild requireGuild(Long guildId) {
        return guildRepository.findById(guildId)
                .orElseThrow(() -> new ResourceNotFoundException("Guild not found"));
    }

    private GuildDto toDto(Guild guild) {
        return GuildDto.from(guild,
                guildMemberRepository.countByGuildId(guild.getId()),
                guildXpTransactionRepository.totalXpByGuild(guild.getId()));
    }
}
