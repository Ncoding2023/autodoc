package com.autodoc.domain.sheet;

import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class SheetColumnRepositoryTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(SheetColumnRepositoryTest.class);
    @Autowired private SheetColumnRepository sheetColumnRepository;

    @Test void createsSheetColumn() {
        SheetColumn saved = sheetColumnRepository.saveAndFlush(column("create", "create", 1));
        assertThat(saved.getId()).isNotNull();
        log.info("SHEETS 열 생성 완료: id={}", saved.getId());
    }

    @Test void readsSheetColumn() {
        SheetColumn saved = sheetColumnRepository.saveAndFlush(column("read", "Read", 1));
        assertThat(sheetColumnRepository.findById(saved.getId())).get().extracting(SheetColumn::getColumnName).isEqualTo("Read");
        log.info("SHEETS 열 조회 완료: id={}", saved.getId());
    }

    @Test void updatesSheetColumn() {
        SheetColumn saved = sheetColumnRepository.saveAndFlush(column("update", "Before", 1));
        sheetColumnRepository.findById(saved.getId()).orElseThrow().update("After", SheetColumnType.DATE);
        sheetColumnRepository.flush();
        assertThat(sheetColumnRepository.findById(saved.getId())).get()
                .extracting(SheetColumn::getColumnName, SheetColumn::getColumnType).containsExactly("After", SheetColumnType.DATE);
        log.info("SHEETS 열 수정 완료: id={}", saved.getId());
    }

    @Test void deletesSheetColumn() {
        SheetColumn saved = sheetColumnRepository.saveAndFlush(column("delete", "Delete", 1));
        sheetColumnRepository.deleteById(saved.getId());
        sheetColumnRepository.flush();
        assertThat(sheetColumnRepository.findById(saved.getId())).isEmpty();
        log.info("SHEETS 열 삭제 완료: id={}", saved.getId());
    }

    private SheetColumn column(String key, String name, int order) {
        return new SheetColumn(1L, key, name, SheetColumnType.TEXT, order);
    }
}
