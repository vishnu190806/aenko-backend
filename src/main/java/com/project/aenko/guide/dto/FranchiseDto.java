package com.project.aenko.guide.dto;

import com.project.aenko.guide.entity.Franchise;
import java.util.List;

public record FranchiseDto(Long id, String name, String description, List<FranchiseMediaDto> media) {
    public static FranchiseDto from(Franchise franchise, List<FranchiseMediaDto> media) {
        return new FranchiseDto(franchise.getId(), franchise.getName(), franchise.getDescription(), media);
    }
}
