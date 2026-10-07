export type UserRole = 'ADMIN' | 'MANAGER' | 'USER';
export type UserStatus = 'ACTIVE' | 'INACTIVE';
export type WritingFormat = 'DOCS' | 'SHEETS';
export type DocumentStatus = 'DRAFT' | 'COMPLETED';
export type TemplateScope = 'BASIC' | 'USER';
export type SheetColumnType = 'TEXT' | 'NUMBER' | 'DATE' | 'BOOLEAN';

export type User = {
  id: number;
  teamId: number | null;
  email: string;
  name: string;
  role: UserRole;
  status: UserStatus;
};

export type Team = {
  id: number;
  name: string;
  description: string | null;
};

export type DocumentTemplate = {
  id: number;
  scope: TemplateScope;
  name: string;
  writingFormat: WritingFormat;
  documentType: string;
  structureData: string;
  templateStyleData: string;
};

export type DocumentListItem = {
  id: number;
  title: string;
  writingFormat: WritingFormat;
  documentType: string;
  status: DocumentStatus;
};

export type Document = DocumentListItem & {
  templateId: number;
  bodyContent: string | null;
  importantNotes: string | null;
  cautions: string | null;
};

export type SheetColumn = {
  id: number;
  columnKey: string;
  columnName: string;
  columnType: SheetColumnType;
  columnOrder: number;
};

export type SheetRow = {
  id: number;
  rowNo: number;
  rowData: Record<string, unknown>;
};
