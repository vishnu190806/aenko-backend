package com.project.aenko.guide.service;

import com.project.aenko.anime.repository.MediaExternalIdRepository;
import com.project.aenko.anime.repository.MediaRepository;
import com.project.aenko.common.exception.ResourceNotFoundException;
import com.project.aenko.guide.dto.FranchiseDto;
import com.project.aenko.guide.dto.FranchiseMediaDto;
import com.project.aenko.guide.dto.WatchOrderDto;
import com.project.aenko.guide.dto.WatchOrderStepDto;
import com.project.aenko.guide.entity.Franchise;
import com.project.aenko.guide.repository.FranchiseMediaRepository;
import com.project.aenko.guide.repository.FranchiseRepository;
import com.project.aenko.guide.repository.WatchOrderRepository;
import com.project.aenko.guide.repository.WatchOrderStepRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuideService {

    private final FranchiseRepository franchiseRepository;
    private final FranchiseMediaRepository franchiseMediaRepository;
    private final WatchOrderRepository watchOrderRepository;
    private final WatchOrderStepRepository watchOrderStepRepository;
    private final MediaRepository mediaRepository;
    private final MediaExternalIdRepository externalIdRepository;

    public GuideService(FranchiseRepository franchiseRepository,
            FranchiseMediaRepository franchiseMediaRepository,
            WatchOrderRepository watchOrderRepository,
            WatchOrderStepRepository watchOrderStepRepository,
            MediaRepository mediaRepository,
            MediaExternalIdRepository externalIdRepository) {
        this.franchiseRepository = franchiseRepository;
        this.franchiseMediaRepository = franchiseMediaRepository;
        this.watchOrderRepository = watchOrderRepository;
        this.watchOrderStepRepository = watchOrderStepRepository;
        this.mediaRepository = mediaRepository;
        this.externalIdRepository = externalIdRepository;
    }

    @Transactional(readOnly = true)
    public List<FranchiseDto> franchises() {
        return franchiseRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FranchiseDto franchise(Long id) {
        return toDto(franchiseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Franchise not found")));
    }

    @Transactional(readOnly = true)
    public List<WatchOrderDto> watchOrders(Long franchiseId) {
        franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> new ResourceNotFoundException("Franchise not found"));
        return watchOrderRepository.findByFranchiseId(franchiseId).stream().map(order -> {
            List<WatchOrderStepDto> steps = watchOrderStepRepository
                    .findByWatchOrderIdOrderBySequenceNumberAsc(order.getId()).stream()
                    .map(step -> {
                        var media = mediaRepository.findById(step.getMediaId());
                        var external = findExternal(step.getMediaId());
                        return new WatchOrderStepDto(step.getSequenceNumber(), step.getMediaId(),
                                media.map(m -> m.getTitle()).orElse("Unknown anime"),
                                step.getStepType(), step.getNotes(),
                                external == null ? null : external.getExternalId(),
                                external == null ? null : external.getExternalUrl());
                    })
                    .toList();
            return new WatchOrderDto(order.getId(), order.getFranchiseId(), order.getName(),
                    order.getDescription(), steps);
        }).toList();
    }

    /** Prefers the MAL mapping so the guide can match provider (external) ids. */
    private com.project.aenko.anime.entity.MediaExternalId findExternal(Long mediaId) {
        var links = externalIdRepository.findByMediaId(mediaId);
        return links.stream()
                .filter(link -> "MAL".equals(link.getProvider()))
                .findFirst()
                .orElseGet(() -> links.stream().findFirst().orElse(null));
    }

    private FranchiseDto toDto(Franchise franchise) {
        List<FranchiseMediaDto> media = franchiseMediaRepository.findByFranchiseId(franchise.getId()).stream()
                .map(link -> {
                    var m = mediaRepository.findById(link.getMediaId());
                    var external = findExternal(link.getMediaId());
                    if (m.isEmpty()) {
                        return new FranchiseMediaDto(link.getMediaId(), "Unknown anime", null,
                                external == null ? null : external.getExternalId(),
                                external == null ? null : external.getExternalUrl());
                    }
                    return new FranchiseMediaDto(m.get().getId(), m.get().getTitle(), m.get().getCoverImage(),
                            external == null ? null : external.getExternalId(),
                            external == null ? null : external.getExternalUrl());
                })
                .toList();
        return FranchiseDto.from(franchise, media);
    }
}
