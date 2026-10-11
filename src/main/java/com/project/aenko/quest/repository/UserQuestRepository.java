package com.project.aenko.quest.repository;

import com.project.aenko.quest.entity.UserQuest;
import com.project.aenko.quest.entity.UserQuest.QuestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserQuestRepository extends JpaRepository<UserQuest, Long> {
    Optional<UserQuest> findByUserIdAndQuestId(Long userId, Long questId);

    List<UserQuest> findByUserId(Long userId);

    List<UserQuest> findByUserIdAndStatus(Long userId, QuestStatus status);

    long countByUserIdAndStatus(Long userId, QuestStatus status);
}
