import type { DocumentTemplate, WritingFormat } from '../api/types';

export function readWritingFormat(value: string | null): WritingFormat | null {
  return value === 'DOCS' || value === 'SHEETS' ? value : null;
}

export function getBasicTemplates(templates: DocumentTemplate[], format: WritingFormat) {
  return templates.filter((template) => template.scope === 'BASIC' && template.writingFormat === format)
    .sort((first, second) => first.name.localeCompare(second.name, 'ko'));
}

export function findSelectedTemplate(templates: DocumentTemplate[], id: string | null) {
  if (!id || !/^[1-9]\d*$/.test(id) || !Number.isSafeInteger(Number(id))) return null;
  return templates.find((template) => template.id === Number(id)) ?? null;
}

const sectionLabels: Record<string, string> = {
  title: '제목', body: '본문', bodyContent: '본문', importantNotes: '중요 사항', cautions: '주의 사항',
};

export function getTemplateSections(template: DocumentTemplate): string[] {
  try {
    const structure: unknown = JSON.parse(template.structureData);
    if (!structure || typeof structure !== 'object') return [];
    const data = structure as Record<string, unknown>;
    if (template.writingFormat === 'DOCS' && Array.isArray(data.sections)) {
      return data.sections.filter((section): section is string => typeof section === 'string')
        .map((section) => sectionLabels[section] ?? section);
    }
    if (template.writingFormat === 'SHEETS' && Array.isArray(data.columns)) {
      return data.columns.flatMap((column: unknown) => {
        if (!column || typeof column !== 'object' || !('name' in column) || typeof column.name !== 'string') return [];
        return [column.name];
      });
    }
  } catch {
    // 이전 템플릿의 구조가 유효하지 않아도 목록은 조회할 수 있다.
  }
  return [];
}
