package com.project.aenko.guild.repository;

import com.project.aenko.guild.entity.Guild;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildRepository extends JpaRepository<Guild, Long> {
    List<Guild> findByPrivacy(String privacy);
}
