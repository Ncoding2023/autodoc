package com.autodoc.domain.sheet.dto;
import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
public record SheetRowRequest(@NotNull JsonNode rowData) {
    @AssertTrue(message = "행 데이터는 객체 형태여야 합니다.")
    public boolean isObjectData() { return rowData == null || rowData.isObject(); }
}
