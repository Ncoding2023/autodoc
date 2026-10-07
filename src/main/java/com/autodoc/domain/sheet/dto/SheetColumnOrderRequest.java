package com.autodoc.domain.sheet.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SheetColumnOrderRequest(
        @NotEmpty List<Long> columnIds
) {
}
