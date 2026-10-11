package com.project.aenko.progress.dto;

import java.util.List;

public record ProgressResponse(Long mediaId, int watchedEpisodes, Integer totalEpisodes, List<Integer> episodes) {
}
