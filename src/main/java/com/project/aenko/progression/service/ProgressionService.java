package com.project.aenko.progression.service;

import com.project.aenko.progression.entity.XpTransaction;
import com.project.aenko.progression.repository.XpTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single place where personal XP is granted. All amounts are decided
 * server-side; clients can never submit XP directly.
 */
@Service
public class ProgressionService {

    public static final int XP_PER_EPISODE = 20;
    public static final int XP_PER_ANIME_COMPLETED = 100;
    public static final int XP_PER_LEVEL = 500;

    private final XpTransactionRepository xpTransactionRepository;

    public ProgressionService(XpTransactionRepository xpTransactionRepository) {
        this.xpTransactionRepository = xpTransactionRepository;
    }

    @Transactional
    public void awardXp(Long userId, String sourceType, String sourceId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("XP amount must be positive");
        }
        // Every source is idempotent per (user, type, source id): re-watching an
        // already-rewarded episode (e.g. after unmarking it) must not pay again.
        // The transaction row itself is the idempotency record and survives
        // progress deletion.
        if (sourceId != null && isIdempotentSource(sourceType)
                && xpTransactionRepository.existsByUserIdAndSourceTypeAndSourceId(userId, sourceType, sourceId)) {
            return;
        }
        XpTransaction transaction = new XpTransaction();
        transaction.setUserId(userId);
        transaction.setSourceType(sourceType);
        transaction.setSourceId(sourceId);
        transaction.setAmount(amount);
        xpTransactionRepository.save(transaction);
    }

    private boolean isIdempotentSource(String sourceType) {
        return "EPISODE".equals(sourceType) || "QUEST".equals(sourceType)
                || "ACHIEVEMENT".equals(sourceType) || "ANIME_COMPLETED".equals(sourceType);
    }

    @Transactional(readOnly = true)
    public long totalXp(Long userId) {
        return xpTransactionRepository.totalXpByUser(userId);
    }

    public static int levelFor(long totalXp) {
        return (int) (totalXp / XP_PER_LEVEL) + 1;
    }

    @Transactional(readOnly = true)
    public int level(Long userId) {
        return levelFor(totalXp(userId));
    }
}
