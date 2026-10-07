package com.autodoc.domain.sheet.dto;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
public record SheetRowRequest(@NotNull JsonNode rowData) {}
