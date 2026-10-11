package com.project.aenko.guide.repository;

import com.project.aenko.guide.entity.WatchOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchOrderRepository extends JpaRepository<WatchOrder, Long> {
    List<WatchOrder> findByFranchiseId(Long franchiseId);

    Optional<WatchOrder> findByFranchiseIdAndName(Long franchiseId, String name);
}
