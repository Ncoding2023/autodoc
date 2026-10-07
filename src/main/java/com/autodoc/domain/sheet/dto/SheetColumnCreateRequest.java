package com.autodoc.domain.sheet.dto;
import com.autodoc.domain.sheet.SheetColumnType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public record SheetColumnCreateRequest(@NotBlank String columnKey, @NotBlank String columnName, @NotNull SheetColumnType columnType) {}
