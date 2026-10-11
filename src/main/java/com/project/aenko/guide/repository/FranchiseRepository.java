package com.project.aenko.guide.repository;

import com.project.aenko.guide.entity.Franchise;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FranchiseRepository extends JpaRepository<Franchise, Long> {
    Optional<Franchise> findByName(String name);
}
