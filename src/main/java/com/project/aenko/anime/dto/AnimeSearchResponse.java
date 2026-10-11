package com.project.aenko.anime.dto;

import java.util.List;

public record AnimeSearchResponse(List<AnimeDto> content, int page, int size, boolean hasNext) {
}
