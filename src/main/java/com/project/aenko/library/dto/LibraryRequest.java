package com.project.aenko.library.dto;

import com.project.aenko.library.entity.UserMedia.LibraryStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record LibraryRequest(
        Long mediaId,
        String provider,
        String externalId,
        LibraryStatus status,
        @Min(value = 1, message = "Rating must be between 1 and 10") @Max(value = 10, message = "Rating must be between 1 and 10") Integer rating,
        @Size(max = 2000, message = "Notes must be under 2000 characters") String notes) {
}
