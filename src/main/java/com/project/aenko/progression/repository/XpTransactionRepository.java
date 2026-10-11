package com.project.aenko.progression.repository;

import com.project.aenko.progression.entity.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface XpTransactionRepository extends JpaRepository<XpTransaction, Long> {

    @Query("select coalesce(sum(t.amount), 0) from XpTransaction t where t.userId = :userId")
    long totalXpByUser(Long userId);

    boolean existsByUserIdAndSourceTypeAndSourceId(Long userId, String sourceType, String sourceId);
}
