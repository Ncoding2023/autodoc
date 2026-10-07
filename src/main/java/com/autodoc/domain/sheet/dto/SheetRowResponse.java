package com.autodoc.domain.sheet.dto;
import tools.jackson.databind.JsonNode;
public record SheetRowResponse(Long id, Integer rowNo, JsonNode rowData) {}
