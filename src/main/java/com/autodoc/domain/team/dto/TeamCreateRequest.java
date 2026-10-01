package com.autodoc.domain.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description
) {
}
