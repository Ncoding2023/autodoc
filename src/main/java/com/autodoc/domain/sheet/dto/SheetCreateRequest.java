package com.autodoc.domain.sheet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SheetCreateRequest(
        @NotNull List<@NotNull @Valid SheetColumnCreateRequest> columns,
        @NotNull List<@NotNull @Valid SheetRowRequest> rows
) {}
