import assert from 'node:assert/strict';
import { after, before, beforeEach, test } from 'node:test';
import { createServer } from 'vite';

let server;
let model;
let sheetApi;
let client;
let originalAdapter;
let columns;
let rows;
let requests;

before(async () => {
  server = await createServer({ server: { middlewareMode: true }, appType: 'custom' });
  model = await server.ssrLoadModule('/src/components/sheet-data.ts');
  ({ sheetApi } = await server.ssrLoadModule('/src/api/sheet-api.ts'));
  ({ apiClient: client } = await server.ssrLoadModule('/src/api/client.ts'));
  originalAdapter = client.defaults.adapter;
});

beforeEach(() => {
  columns = [
    { id: 1, columnKey: 'task', columnName: '업무', columnType: 'TEXT', columnOrder: 1 },
    { id: 2, columnKey: 'date', columnName: '날짜', columnType: 'DATE', columnOrder: 2 },
    { id: 3, columnKey: 'done', columnName: '완료', columnType: 'BOOLEAN', columnOrder: 3 },
  ];
  rows = [];
  requests = [];
  client.defaults.adapter = async (config) => {
    const input = typeof config.data === 'string' ? JSON.parse(config.data) : config.data;
    requests.push({ method: config.method, url: config.url, input });
    let result;
    if (config.method === 'get') result = config.url.endsWith('/columns') ? columns : rows;
    else if (config.url.endsWith('/columns/order')) {
      columns = input.columnIds.map((id, index) => ({ ...columns.find(column => column.id === id), columnOrder: index + 1 }));
      result = columns;
    } else if (config.url.includes('/columns')) {
      const id = Number(config.url.split('/').at(-1));
      if (config.method === 'post') { result = { ...input, id: 4, columnOrder: 4 }; columns.push(result); }
      else if (config.method === 'patch') { columns = columns.map(column => column.id === id ? { ...column, ...input } : column); result = columns.find(column => column.id === id); }
      else {
        const key = columns.find(column => column.id === id).columnKey;
        columns = columns.filter(column => column.id !== id);
        rows = rows.map(row => ({ ...row, rowData: Object.fromEntries(Object.entries(row.rowData).filter(([name]) => name !== key)) }));
      }
    } else {
      const id = Number(config.url.split('/').at(-1));
      if (config.method === 'post') { result = { id: 11, rowNo: 1, ...input }; rows.push(result); }
      else if (config.method === 'patch') { rows = rows.map(row => row.id === id ? { ...row, ...input } : row); result = rows.find(row => row.id === id); }
      else rows = rows.filter(row => row.id !== id);
    }
    return { data: result, status: 200, statusText: 'OK', headers: {}, config };
  };
});

after(async () => { if (client) client.defaults.adapter = originalAdapter; await server?.close(); });

function form(values) {
  const data = new FormData();
  for (const [name, value] of Object.entries(values)) data.set(name, value);
  return data;
}

test('열 추가·수정·순서 변경과 행 추가·수정·삭제 API를 연결한다', async () => {
  await sheetApi.getColumns(42);
  await sheetApi.addColumn(42, { columnKey: 'amount', columnName: '수량', columnType: 'NUMBER' });
  await sheetApi.updateColumn(42, 4, { columnName: '예상 수량', columnType: 'NUMBER' });
  const ids = model.moveColumn(columns, 4, -1);
  const reordered = await sheetApi.updateColumnOrder(42, ids);
  assert.deepEqual(reordered.map(column => column.id), [1, 2, 4, 3]);
  const input = model.readRowInput(form({ 'cell:task': '요구사항 검토', 'cell:date': '2026-10-15', 'cell:done': 'false', 'cell:amount': '2.5' }), columns);
  const row = await sheetApi.addRow(42, { rowData: input.rowData });
  assert.equal(row.rowData.amount, 2.5);
  assert.equal(row.rowData.done, false);
  await sheetApi.updateRow(42, 11, { rowData: { ...row.rowData, task: '완료', done: true } });
  assert.equal((await sheetApi.getRows(42))[0].rowData.done, true);
  await sheetApi.removeColumn(42, 4);
  assert.ok(!Object.hasOwn((await sheetApi.getRows(42))[0].rowData, 'amount'));
  await sheetApi.removeRow(42, 11);
  assert.deepEqual(await sheetApi.getRows(42), []);
  assert.ok(requests.some(request => request.url === '/documents/42/columns/order' && request.method === 'patch' && request.input.columnIds.length === 4));
  assert.ok(requests.some(request => request.url === '/documents/42/rows/11' && request.method === 'patch'));
  console.info('SHEETS 열·행 편집 API 전체 흐름 검증 완료');
});

test('열 이름과 데이터 형식의 입력값을 검증한다', () => {
  assert.ok(model.readColumnInput(form({ columnName: '  ', columnType: 'TEXT' })).error);
  assert.ok(model.readColumnInput(form({ columnName: '가'.repeat(101), columnType: 'TEXT' })).error);
  assert.ok(model.readColumnInput(form({ columnName: '업무', columnType: 'toString' })).error);
  assert.deepEqual(model.readColumnInput(form({ columnName: ' 업무 ', columnType: 'TEXT' })).input, { columnName: '업무', columnType: 'TEXT' });
  console.info('SHEETS 열 이름 및 데이터 형식 입력 검증 완료');
});

test('숫자 0과 아니오 값을 빈 값으로 취급하지 않는다', () => {
  const typed = [...columns, { id: 4, columnKey: 'amount', columnType: 'NUMBER', columnName: '수량' }];
  const result = model.readRowInput(form({ 'cell:task': '업무', 'cell:amount': '0', 'cell:done': 'false' }), typed);
  assert.equal(result.rowData.amount, 0);
  assert.equal(result.rowData.done, false);
  assert.equal(model.displayCell(0), '0');
  assert.equal(model.displayCell(false), '아니오');
  console.info('숫자 0 및 아니오 값 유지 검증 완료');
});

test('빈 숫자·날짜·예아니오 값은 null로 저장한다', () => {
  const typed = [...columns, { id: 4, columnKey: 'amount', columnType: 'NUMBER', columnName: '수량' }];
  const result = model.readRowInput(form({}), typed);
  assert.deepEqual(result.rowData, { task: '', date: null, done: null, amount: null });
  assert.ok(model.readRowInput(form({}), []).error);
  console.info('SHEETS 빈 입력 및 열 없는 행 입력 검증 완료');
});

test('유효하지 않은 숫자·날짜·예아니오 입력은 거부한다', () => {
  const typed = [...columns, { id: 4, columnKey: 'amount', columnType: 'NUMBER', columnName: '수량' }];
  for (const value of ['abc', 'Infinity', 'NaN']) assert.ok(model.readRowInput(form({ 'cell:amount': value }), typed).error);
  for (const value of ['2026-02-30', '2026-02-29', 'invalid']) assert.ok(model.readRowInput(form({ 'cell:date': value }), typed).error);
  assert.ok(model.readRowInput(form({ 'cell:done': 'no' }), typed).error);
  assert.equal(model.readRowInput(form({ 'cell:date': '2028-02-29' }), typed).rowData.date, '2028-02-29');
  console.info('SHEETS 데이터 형식 및 실제 날짜 검증 완료');
});

test('열 이동은 경계를 넘지 않고 전체 열 ID를 유지한다', () => {
  assert.equal(model.moveColumn(columns, 1, -1), null);
  assert.equal(model.moveColumn(columns, 3, 1), null);
  assert.equal(model.moveColumn(columns, 999, 1), null);
  assert.deepEqual(model.moveColumn(columns, 2, 1), [1, 3, 2]);
  assert.deepEqual(columns.map(column => column.id), [1, 2, 3]);
  console.info('열 이동 경계 및 전체 열 ID 보존 검증 완료');
});

test('행 키가 특수한 이름이어도 객체 속성으로 안전하게 저장한다', () => {
  const special = [{ id: 1, columnKey: '__proto__', columnName: '특수 키', columnType: 'TEXT' }];
  const result = model.readRowInput(form({ 'cell:__proto__': '값' }), special);
  assert.ok(Object.hasOwn(result.rowData, '__proto__'));
  assert.equal(result.rowData.__proto__, '값');
  assert.equal(Object.getPrototypeOf(result.rowData), Object.prototype);
  console.info('행 데이터 키 처리 검증 완료');
});
