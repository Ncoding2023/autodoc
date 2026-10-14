import { redirect, type ActionFunctionArgs, type LoaderFunctionArgs } from 'react-router-dom';
import { documentApi } from '../api/document-api';
import { sheetApi } from '../api/sheet-api';
import { templateApi } from '../api/template-api';
import { getApiError } from '../api/client';
import type { Document, DocumentTemplate } from '../api/types';
import { readWritingFormat } from './template-selection';
import { readSheetDraft } from '../components/sheet-data';

function readId(value: string | null | undefined) {
  if (!value || !/^[1-9]\d*$/.test(value) || !Number.isSafeInteger(Number(value))) {
    throw new Response('요청한 문서 또는 템플릿을 찾을 수 없습니다.', { status: 404 });
  }
  return Number(value);
}

function handleFailure(failure: unknown, request: Request): never {
  const error = getApiError(failure);
  if (error?.code === 'UNAUTHORIZED' || error?.code === 'INACTIVE_USER' || error?.code === 'USER_NOT_FOUND') {
    const url = new URL(request.url);
    throw redirect(`/login?returnTo=${encodeURIComponent(url.pathname + url.search)}`);
  }
  if (error?.code === 'ACCESS_DENIED') throw new Response('이 문서 또는 템플릿에 접근할 권한이 없습니다.', { status: 403 });
  if (error?.code === 'DOCUMENT_NOT_FOUND' || error?.code === 'TEMPLATE_NOT_FOUND') {
    throw new Response('문서 또는 템플릿을 찾을 수 없습니다. 삭제되었을 수 있습니다.', { status: 404 });
  }
  throw failure;
}

export async function loadDocuments({ request }: LoaderFunctionArgs) {
  try { return await documentApi.getMyDocuments(request.signal); }
  catch (failure) { handleFailure(failure, request); }
}

export async function loadDocument({ params, request }: LoaderFunctionArgs) {
  const id = readId(params.documentId);
  try { return await documentApi.getById(id, request.signal); }
  catch (failure) { handleFailure(failure, request); }
}

export async function loadDocumentTemplate({ request }: LoaderFunctionArgs) {
  const url = new URL(request.url);
  const format = readWritingFormat(url.searchParams.get('format'));
  if (!format) throw redirect('/documents/new');
  const id = readId(url.searchParams.get('templateId'));
  try {
    const template = await templateApi.getById(id, request.signal);
    if (template.scope !== 'BASIC' || template.writingFormat !== format) {
      throw new Response('선택한 형식에 맞는 기본 템플릿을 다시 선택해 주세요.', { status: 400 });
    }
    return template;
  } catch (failure) {
    if (failure instanceof Response) throw failure;
    handleFailure(failure, request);
  }
}

export async function loadDocumentForEdit(args: LoaderFunctionArgs) {
  const document = await loadDocument(args);
  if (document.writingFormat !== 'SHEETS') return { document };
  try {
    const [columns, rows] = await Promise.all([
      sheetApi.getColumns(document.id, args.request.signal),
      sheetApi.getRows(document.id, args.request.signal),
    ]);
    return { document, sheet: {
      columns: [...columns].sort((a, b) => a.columnOrder - b.columnOrder),
      rows: [...rows].sort((a, b) => a.rowNo - b.rowNo).map(({ rowData }) => ({ rowData })),
    } };
  } catch (failure) { handleFailure(failure, args.request); }
}

export function readDocumentInput(data: FormData, source: DocumentTemplate | Document) {
  const title = String(data.get('title') ?? '').trim();
  const documentType = 'scope' in source ? source.documentType : String(data.get('documentType') ?? '').trim();
  if (!title || title.length > 255) return { error: '문서 제목을 1~255자로 입력해 주세요.' };
  if (!documentType || documentType.length > 50) return { error: '문서 유형을 1~50자로 입력해 주세요.' };
  return { input: {
    title, documentType,
    bodyContent: source.writingFormat === 'DOCS' ? String(data.get('bodyContent') ?? '') : 'scope' in source ? null : source.bodyContent,
    importantNotes: source.writingFormat === 'DOCS' ? String(data.get('importantNotes') ?? '') : 'scope' in source ? null : source.importantNotes,
    cautions: source.writingFormat === 'DOCS' ? String(data.get('cautions') ?? '') : 'scope' in source ? null : source.cautions,
  } };
}

function actionFailure(failure: unknown, request: Request, fallback = '저장하지 못했습니다. 입력 내용과 서버 연결을 확인하고 다시 시도해 주세요.') {
  const code = getApiError(failure)?.code;
  if (code === 'UNAUTHORIZED' || code === 'INACTIVE_USER' || code === 'USER_NOT_FOUND' || code === 'ACCESS_DENIED' || code === 'DOCUMENT_NOT_FOUND' || code === 'TEMPLATE_NOT_FOUND') {
    handleFailure(failure, request);
  }
  return { error: getApiError(failure)?.message ?? fallback };
}

export async function createDocument(args: ActionFunctionArgs) {
  const { request } = args;
  const template = await loadDocumentTemplate(args);
  const data = await request.formData();
  const result = readDocumentInput(data, template);
  if (result.error) return { error: result.error };
  const draft = template.writingFormat === 'SHEETS' ? readSheetDraft(String(data.get('sheetDraft') ?? '')) : {};
  if (draft.error) return { error: draft.error };
  try {
    const document = await documentApi.create({ ...result.input!, templateId: template.id, writingFormat: template.writingFormat, sheet: draft.sheet });
    return redirect(`/documents/${document.id}?created=1`);
  } catch (failure) { return actionFailure(failure, request); }
}

export async function updateDocument(args: ActionFunctionArgs) {
  const { request } = args;
  const document = await loadDocument(args);
  const data = await request.formData();
  const result = readDocumentInput(data, document);
  if (result.error) return { error: result.error };
  const draft = document.writingFormat === 'SHEETS' ? readSheetDraft(String(data.get('sheetDraft') ?? '')) : {};
  if (draft.error) return { error: draft.error };
  try {
    await documentApi.update(document.id, { ...result.input!, sheet: draft.sheet });
    return redirect(`/documents/${document.id}?updated=1`);
  } catch (failure) { return actionFailure(failure, request); }
}

export async function deleteDocument({ request, params }: ActionFunctionArgs) {
  const id = readId(params.documentId);
  const data = await request.formData();
  if (data.get('intent') !== 'delete') return { error: '삭제 요청이 올바르지 않습니다.' };
  try {
    await documentApi.remove(id);
    return redirect('/documents?deleted=1');
  } catch (failure) {
    return actionFailure(failure, request, '문서를 삭제하지 못했습니다. 다시 시도해 주세요.');
  }
}
