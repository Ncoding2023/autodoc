import assert from 'node:assert/strict';
import { after, before, beforeEach, test } from 'node:test';
import { createServer } from 'vite';
import { createElement } from 'react';
import { renderToString } from 'react-dom/server';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';

let server;
let routes;
let documentApi;
let templateApi;
let originals;
let documents;
let template;
let createCalls;
let deleteCalls;

before(async () => {
  server = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
  routes = await server.ssrLoadModule('/src/pages/document-routes.ts');
  ({ documentApi } = await server.ssrLoadModule('/src/api/document-api.ts'));
  ({ templateApi } = await server.ssrLoadModule('/src/api/template-api.ts'));
  originals = { documents: { ...documentApi }, templates: { ...templateApi } };
});

beforeEach(() => {
  documents = new Map();
  createCalls = 0;
  deleteCalls = 0;
  template = { id: 7, scope: 'BASIC', name: '인수인계서', writingFormat: 'DOCS', documentType: 'handover', structureData: '{}', templateStyleData: '{}' };
  templateApi.getById = async () => ({ ...template });
  documentApi.create = async (input) => {
    createCalls++;
    const document = { ...input, id: 42, status: 'DRAFT' };
    documents.set(document.id, document);
    return document;
  };
  documentApi.getMyDocuments = async () => [...documents.values()];
  documentApi.getById = async (id) => {
    if (!documents.has(id)) throw apiError('DOCUMENT_NOT_FOUND');
    return { ...documents.get(id) };
  };
  documentApi.update = async (id, input) => {
    const document = { ...documents.get(id), ...input };
    documents.set(id, document);
    return document;
  };
  documentApi.remove = async (id) => { deleteCalls++; documents.delete(id); };
});

after(async () => {
  if (originals) {
    Object.assign(documentApi, originals.documents);
    Object.assign(templateApi, originals.templates);
  }
  await server?.close();
});

function args(path, fields, documentId) {
  const url = new URL(path, 'http://localhost');
  const body = new FormData();
  for (const [name, value] of Object.entries(fields ?? {})) body.set(name, value);
  return { request: new Request(url, fields ? { method: 'POST', body } : undefined),
    url, pattern: path, params: documentId ? { documentId } : {}, context: undefined };
}

function apiError(code) {
  return { isAxiosError: true, response: { data: { code, message: code } } };
}

test('문서 생성 → 목록 → 상세 → 수정 → 삭제 흐름을 연결한다', async () => {
  const created = await routes.createDocument(args('/documents/new/write?format=DOCS&templateId=7',
    { title: '  첫 문서  ', documentType: '위조 유형', writingFormat: 'SHEETS', bodyContent: '본문\n두 번째 줄', importantNotes: '중요', cautions: '주의' }));
  assert.equal(created.headers.get('Location'), '/documents/42?created=1');
  const saved = documents.get(42);
  assert.equal(saved.title, '첫 문서');
  assert.equal(saved.templateId, 7);
  assert.equal(saved.writingFormat, 'DOCS');
  assert.equal(saved.documentType, 'handover');
  assert.equal((await routes.loadDocuments(args('/documents'))).length, 1);
  assert.deepEqual((await routes.loadDocument(args('/documents/42', undefined, '42'))).bodyContent.split(/\r?\n/), ['본문', '두 번째 줄']);
  const updated = await routes.updateDocument(args('/documents/42/edit',
    { title: '수정한 문서', documentType: 'report', bodyContent: '수정 본문', importantNotes: '', cautions: '수정 주의' }, '42'));
  assert.equal(updated.headers.get('Location'), '/documents/42?updated=1');
  assert.equal(documents.get(42).bodyContent, '수정 본문');
  assert.equal(documents.get(42).importantNotes, '');
  assert.equal(documents.get(42).cautions, '수정 주의');
  const deleted = await routes.deleteDocument(args('/documents/42', { intent: 'delete' }, '42'));
  assert.equal(deleted.headers.get('Location'), '/documents?deleted=1');
  assert.equal((await routes.loadDocuments(args('/documents'))).length, 0);
  console.info('문서 생성·조회·수정·삭제 전체 흐름 검증 완료');
});

test('공백 제목과 최대 길이 초과 입력은 저장하지 않는다', async () => {
  for (const title of ['   ', 'a'.repeat(256)]) {
    const result = await routes.createDocument(args('/documents/new/write?format=DOCS&templateId=7', { title }));
    assert.ok(result.error);
  }
  assert.equal(createCalls, 0);
  console.info('문서 제목 입력 검증 완료');
});

test('문서 생성 폼 제출 주소에 선택한 템플릿과 형식을 유지한다', async () => {
  const { DocumentFormPage } = await server.ssrLoadModule('/src/pages/DocumentFormPage.tsx');
  const router = createMemoryRouter([{ path: '/documents/new/write', element: createElement(DocumentFormPage, { creating: true }), loader: async () => template }],
    { initialEntries: ['/documents/new/write?format=DOCS&templateId=7'] });
  try {
    if (!router.state.initialized) await new Promise((resolve) => {
      const unsubscribe = router.subscribe((state) => { if (state.initialized) { unsubscribe(); resolve(); } });
    });
    const html = renderToString(createElement(RouterProvider, { router }));
    assert.match(html, /action="\/documents\/new\/write\?format=DOCS&amp;templateId=7"/);
    assert.match(html, /name="title"/);
    assert.match(html, /name="bodyContent"/);
    console.info('생성 폼 제출 시 선택한 템플릿 및 형식 유지 검증 완료');
  } finally { router.dispose(); }
});

test('형식이 다른 템플릿과 사용자 템플릿을 기본 템플릿으로 사용할 수 없다', async () => {
  template.writingFormat = 'SHEETS';
  await assert.rejects(() => routes.loadDocumentTemplate(args('/documents/new/write?format=DOCS&templateId=7')),
    (error) => error instanceof Response && error.status === 400);
  template.writingFormat = 'DOCS';
  template.scope = 'USER';
  await assert.rejects(() => routes.createDocument(args('/documents/new/write?format=DOCS&templateId=7', { title: '문서' })),
    (error) => error instanceof Response && error.status === 400);
  assert.equal(createCalls, 0);
  console.info('템플릿 형식 및 기본 템플릿 범위 검증 완료');
});

test('SHEETS 정보 수정 시 화면에 없는 기존 콘텐츠를 보존한다', async () => {
  documents.set(42, { id: 42, templateId: 7, title: '기존 문서', documentType: 'list', writingFormat: 'SHEETS', status: 'DRAFT', bodyContent: '기존 데이터', importantNotes: '기존 중요', cautions: '기존 주의' });
  await routes.updateDocument(args('/documents/42/edit', { title: '수정 제목', documentType: 'list', bodyContent: '주입 데이터' }, '42'));
  assert.equal(documents.get(42).title, '수정 제목');
  assert.equal(documents.get(42).bodyContent, '기존 데이터');
  assert.equal(documents.get(42).importantNotes, '기존 중요');
  assert.equal(documents.get(42).cautions, '기존 주의');
  console.info('SHEETS 수정 시 기존 콘텐츠 보존 검증 완료');
});

test('권한 거부와 삭제된 문서를 구분하고 세션 만료 시 로그인으로 이동한다', async () => {
  for (const [code, status] of [['ACCESS_DENIED', 403], ['DOCUMENT_NOT_FOUND', 404]]) {
    documentApi.getById = async () => { throw apiError(code); };
    await assert.rejects(() => routes.loadDocument(args('/documents/42', undefined, '42')),
      (error) => error instanceof Response && error.status === status);
  }
  documentApi.getById = async () => { throw apiError('UNAUTHORIZED'); };
  await assert.rejects(() => routes.loadDocument(args('/documents/42/edit', undefined, '42')),
    (error) => error instanceof Response && error.headers.get('Location') === '/login?returnTo=%2Fdocuments%2F42%2Fedit');
  console.info('문서 접근 실패 및 세션 만료 처리 검증 완료');
});

test('잘못된 문서 ID와 삭제 의도로 요청한 값은 처리하지 않는다', async () => {
  for (const id of ['0', '-1', 'abc', '1.5']) {
    await assert.rejects(() => routes.loadDocument(args('/documents/' + id, undefined, id)),
      (error) => error instanceof Response && error.status === 404);
  }
  assert.ok((await routes.deleteDocument(args('/documents/42', { intent: 'save' }, '42'))).error);
  assert.equal(deleteCalls, 0);
  console.info('잘못된 문서 ID 및 삭제 요청 검증 완료');
});

test('서버 저장·삭제 실패 시 재시도 가능한 오류를 반환한다', async () => {
  documentApi.create = async () => { throw new Error('offline'); };
  assert.ok((await routes.createDocument(args('/documents/new/write?format=DOCS&templateId=7', { title: '문서' }))).error);
  documentApi.remove = async () => { throw new Error('offline'); };
  assert.match((await routes.deleteDocument(args('/documents/42', { intent: 'delete' }, '42'))).error, /삭제하지 못했습니다/);
  console.info('문서 저장·삭제 실패 처리 검증 완료');
});
