package com.autodoc.domain.sheet;

import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class SheetRowRepositoryTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(SheetRowRepositoryTest.class);
    @Autowired private SheetRowRepository sheetRowRepository;

    @Test void createsSheetRow() {
        SheetRow saved = sheetRowRepository.saveAndFlush(row(1, "{\"name\":\"create\"}"));
        assertThat(saved.getId()).isNotNull();
        log.info("SHEETS 행 생성 완료: id={}", saved.getId());
    }

    @Test void readsSheetRow() {
        SheetRow saved = sheetRowRepository.saveAndFlush(row(1, "{\"name\":\"read\"}"));
        assertThat(sheetRowRepository.findById(saved.getId())).get().extracting(SheetRow::getRowData).isEqualTo("{\"name\":\"read\"}");
        log.info("SHEETS 행 조회 완료: id={}", saved.getId());
    }

    @Test void updatesSheetRow() {
        SheetRow saved = sheetRowRepository.saveAndFlush(row(1, "{\"name\":\"before\"}"));
        sheetRowRepository.findById(saved.getId()).orElseThrow().updateRowData("{\"name\":\"after\"}");
        sheetRowRepository.flush();
        assertThat(sheetRowRepository.findById(saved.getId())).get().extracting(SheetRow::getRowData).isEqualTo("{\"name\":\"after\"}");
        log.info("SHEETS 행 수정 완료: id={}", saved.getId());
    }

    @Test void deletesSheetRow() {
        SheetRow saved = sheetRowRepository.saveAndFlush(row(1, "{\"name\":\"delete\"}"));
        sheetRowRepository.deleteById(saved.getId());
        sheetRowRepository.flush();
        assertThat(sheetRowRepository.findById(saved.getId())).isEmpty();
        log.info("SHEETS 행 삭제 완료: id={}", saved.getId());
    }

    private SheetRow row(int rowNo, String rowData) { return new SheetRow(1L, rowNo, rowData); }
}
