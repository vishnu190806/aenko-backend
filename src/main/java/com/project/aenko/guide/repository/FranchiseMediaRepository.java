package com.project.aenko.guide.repository;

import com.project.aenko.guide.entity.FranchiseMedia;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FranchiseMediaRepository extends JpaRepository<FranchiseMedia, Long> {
    List<FranchiseMedia> findByFranchiseId(Long franchiseId);

    Optional<FranchiseMedia> findByFranchiseIdAndMediaId(Long franchiseId, Long mediaId);
}
