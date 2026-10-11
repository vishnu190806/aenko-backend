package com.project.aenko.anime.repository;

import com.project.aenko.anime.entity.MediaExternalId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaExternalIdRepository extends JpaRepository<MediaExternalId, Long> {
    Optional<MediaExternalId> findByMediaIdAndProvider(Long mediaId, String provider);

    Optional<MediaExternalId> findByProviderAndExternalId(String provider, String externalId);

    List<MediaExternalId> findByMediaId(Long mediaId);
}
