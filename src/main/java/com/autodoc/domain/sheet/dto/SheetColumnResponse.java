package com.autodoc.domain.sheet.dto;
import com.autodoc.domain.sheet.*;
public record SheetColumnResponse(Long id, String columnKey, String columnName, SheetColumnType columnType, Integer columnOrder) {
 public static SheetColumnResponse from(SheetColumn c){return new SheetColumnResponse(c.getId(),c.getColumnKey(),c.getColumnName(),c.getColumnType(),c.getColumnOrder());}}
