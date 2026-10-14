package com.autodoc.domain.sheet.dto;
import com.autodoc.domain.sheet.SheetColumnType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record SheetColumnUpdateRequest(@NotBlank @Size(max = 100) String columnName, @NotNull SheetColumnType columnType) {}
