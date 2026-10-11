package com.project.aenko.library.repository;

import com.project.aenko.library.entity.UserMedia;
import com.project.aenko.library.entity.UserMedia.LibraryStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMediaRepository extends JpaRepository<UserMedia, Long> {
    Optional<UserMedia> findByUserIdAndMediaId(Long userId, Long mediaId);

    Page<UserMedia> findByUserId(Long userId, Pageable pageable);

    Page<UserMedia> findByUserIdAndStatus(Long userId, LibraryStatus status, Pageable pageable);

    List<UserMedia> findByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, LibraryStatus status);

    long countByUserId(Long userId);
}
