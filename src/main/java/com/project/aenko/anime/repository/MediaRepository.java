package com.project.aenko.anime.repository;

import com.project.aenko.anime.entity.Media;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
    Optional<Media> findByTitle(String title);
}
