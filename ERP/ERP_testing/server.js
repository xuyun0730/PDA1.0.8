const express = require('express');
const fs = require('fs/promises');
const path = require('path');
const QRCode = require('qrcode');

const app = express();
const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.HOST || '0.0.0.0';
const DATA_DIR = path.join(__dirname, 'data');
const DATA_FILE = path.join(DATA_DIR, 'mock-db.json');

app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: true }));
app.use(express.static(path.join(__dirname, 'public')));

function createSeedState() {
  return {
    users: [
      {
        username: 'pda',
        password: '123456',
        name: 'PDA Test User'
      }
    ],
    token: {
      value: 'mock-token-001',
      issuedAt: new Date().toISOString()
    },
    batches: [
      {
        id: 'batch001',
        batch: 'PB20260525001',
        maxSerialNum: '1',
        batchType: 'total',
        outputProduct: {
          id: 'out001',
          name: '总混成品 A',
          num: 3
        },
        materialList: [
          {
            id: 'mat001',
            name: '直投原料 A',
            formulaWeight: '10',
            unitName: 'kg',
            netContentAllowGt: '9.5',
            netContentAllowLt: '10.5',
            isPremix: false,
            belongOutputProductId: 'out001',
            varietyPackUnitId: 'unit001',
            materialFlag: 'belongDirectFeedMaterial'
          },
          {
            id: 'mat002',
            name: '预混原料 B',
            formulaWeight: '5',
            unitName: 'kg',
            netContentAllowGt: '4.9',
            netContentAllowLt: '5.1',
            isPremix: true,
            belongOutputProductId: '',
            varietyPackUnitId: 'unit002',
            materialFlag: 'belongPremixMaterial'
          }
        ]
      },
      {
        id: 'batch002',
        batch: 'PB20260525002',
        maxSerialNum: '1',
        batchType: 'premix',
        outputProduct: {
          id: 'out002',
          name: '预混成品 A',
          num: 1
        },
        materialList: [
          {
            id: 'mat101',
            name: '预混辅料 A',
            formulaWeight: '2',
            unitName: 'kg',
            netContentAllowGt: '1.9',
            netContentAllowLt: '2.1',
            isPremix: true,
            belongOutputProductId: 'out002',
            varietyPackUnitId: 'unit101',
            materialFlag: 'belongPremixMaterial'
          }
        ]
      }
    ],
    usedCodes: [],
    launchRecords: [],
    receipts: [],
    interfaceLogs: [],
    erpMaterialMovements: [],
    sequence: {
      receipt: 0,
      interfaceLog: 0,
      materialMovement: 0
    }
  };
}

function ensureStateShape(state) {
  state.users = Array.isArray(state.users) ? state.users : [];
  state.batches = Array.isArray(state.batches) ? state.batches : [];
  state.usedCodes = Array.isArray(state.usedCodes) ? state.usedCodes : [];
  state.launchRecords = Array.isArray(state.launchRecords) ? state.launchRecords : [];
  state.receipts = Array.isArray(state.receipts) ? state.receipts : [];
  state.interfaceLogs = Array.isArray(state.interfaceLogs) ? state.interfaceLogs : [];
  state.erpMaterialMovements = Array.isArray(state.erpMaterialMovements) ? state.erpMaterialMovements : [];
  state.sequence = state.sequence && typeof state.sequence === 'object' ? state.sequence : {};
  state.sequence.receipt = Number(state.sequence.receipt || 0);
  state.sequence.interfaceLog = Number(state.sequence.interfaceLog || 0);
  state.sequence.materialMovement = Number(state.sequence.materialMovement || 0);
  return state;
}

async function ensureDataFile() {
  await fs.mkdir(DATA_DIR, { recursive: true });
  try {
    await fs.access(DATA_FILE);
  } catch {
    await fs.writeFile(DATA_FILE, JSON.stringify(createSeedState(), null, 2), 'utf8');
  }
}

async function readState() {
  await ensureDataFile();
  const text = await fs.readFile(DATA_FILE, 'utf8');
  return ensureStateShape(JSON.parse(text));
}

async function writeState(state) {
  await fs.mkdir(DATA_DIR, { recursive: true });
  const tempFile = `${DATA_FILE}.tmp`;
  await fs.writeFile(tempFile, JSON.stringify(ensureStateShape(state), null, 2), 'utf8');
  await fs.rename(tempFile, DATA_FILE);
}

function success(data = {}, message = 'success') {
  return { code: 0, message, data };
}

function failure(message = 'failed', code = 1, data = {}) {
  return { code, message, data };
}

function pickFirst(...values) {
  for (const value of values) {
    if (value !== undefined && value !== null && value !== '') {
      return value;
    }
  }
  return undefined;
}

function parseJsonSafely(value, fallback = null) {
  if (typeof value !== 'string' || !value.trim()) {
    return fallback;
  }

  try {
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}

function normalizePayload(req) {
  return {
    ...req.query,
    ...req.body,
    data: typeof req.body?.data === 'object' ? req.body.data : req.query?.data
  };
}

function normalizeOperationType(value) {
  const type = String(value || '').trim().toLowerCase();
  if (type === 'overallfeed') {
    return 'total';
  }
  if (type === 'directfeed' || type === 'materialweigh') {
    return 'total';
  }
  if (type === 'premixfeed') {
    return 'premix';
  }
  return '';
}

function findBatch(state, payload) {
  const keyword = pickFirst(
    payload.batchId,
    payload.batch,
    payload.id,
    payload.relationInfoId,
    payload.batchNo,
    payload.productionBatch,
    payload.belongProduceBatch,
    payload.formulaBatch
  );

  if (!keyword) {
    return state.batches[0] || null;
  }

  return (
    state.batches.find((batch) => batch.id === keyword) ||
    state.batches.find((batch) => batch.batch === keyword) ||
    null
  );
}

function filterBatches(state, payload) {
  const exactBatch = findBatch(state, payload);
  if (exactBatch && pickFirst(payload.batchId, payload.id, payload.relationInfoId)) {
    return [exactBatch];
  }

  const keyword = String(payload.batch || '').trim().toLowerCase();
  const type = normalizeOperationType(payload.operationTypeCode);

  return state.batches.filter((batch) => {
    if (type === 'premix') {
      const hasPremixMaterial = Array.isArray(batch.materialList) && batch.materialList.some((item) => item.isPremix);
      if (!hasPremixMaterial) {
        return false;
      }
    }

    if (keyword) {
      const batchValue = String(batch.batch || '').toLowerCase();
      const productName = String(batch.outputProduct?.name || '').toLowerCase();
      if (!batchValue.includes(keyword) && !productName.includes(keyword)) {
        return false;
      }
    }

    return true;
  });
}

function buildQrContent(input) {
  const params = new URLSearchParams();
  params.set('businessVarietyId', input.businessVarietyId || 'mat001');
  params.set('weight', String(input.weight || '10'));
  params.set('unitName', input.unitName || 'kg');
  params.set('inventoryBatch', input.inventoryBatch || 'INV001');
  params.set('validDate', input.validDate || '2026-12-31');
  params.set('belongProduceBatch', input.belongProduceBatch || 'PB20260525001');
  params.set('serialNumber', String(input.serialNumber || '1'));
  return `https://unify.id-cas.cn/lotinfo?${params.toString()}`;
}

function parseQrContent(code) {
  if (!code || typeof code !== 'string') {
    return null;
  }

  try {
    const url = new URL(code);
    return Object.fromEntries(url.searchParams.entries());
  } catch {
    const result = {};
    for (const chunk of code.split('&')) {
      const [key, value] = chunk.split('=');
      if (key) {
        result[decodeURIComponent(key)] = decodeURIComponent(value || '');
      }
    }
    return Object.keys(result).length ? result : null;
  }
}

function findMaterialItemIndex(batch, businessVarietyId) {
  for (let index = 0; index < batch.materialList.length; index += 1) {
    const item = batch.materialList[index];
    if (String(item.id).toLowerCase() !== String(businessVarietyId || '').toLowerCase()) {
      continue;
    }

    return index;
  }

  return -1;
}

function materialExists(batch, businessVarietyId) {
  return findMaterialItemIndex(batch, businessVarietyId) >= 0;
}

function materialExistsById(batch, businessVarietyId) {
  return Array.isArray(batch?.materialList)
    && batch.materialList.some((item) => String(item.id).toLowerCase() === String(businessVarietyId || '').toLowerCase());
}

function findUsedCode(state, code) {
  return state.usedCodes.find((item) => item.code === code);
}

function buildUsageSummary(state) {
  return state.batches.map((batch) => {
    const batchUsedCodes = state.usedCodes.filter((item) => item.batch === batch.batch || item.batch === batch.id);
    const batchRecords = state.launchRecords.filter((item) => item.batch === batch.batch || item.batch === batch.id);
    const plannedMaterials = Array.isArray(batch.materialList) ? batch.materialList : [];
    const actualUsedMaterialIds = [
      ...new Set(
        batchRecords
          .map((record) => record.parsed?.businessVarietyId || record.payload?.businessVarietyId)
          .filter(Boolean)
      )
    ];

    return {
      id: batch.id,
      batch: batch.batch,
      batchType: batch.batchType,
      outputProduct: batch.outputProduct,
      productionItemCount: plannedMaterials.length,
      premixItemCount: plannedMaterials.filter((item) => item.isPremix).length,
      actualUseCount: batchRecords.length,
      actualUsedCodeCount: batchUsedCodes.length,
      actualUsedMaterialCount: actualUsedMaterialIds.length,
      actualUsedMaterialIds,
      lastUsedAt: batchRecords[0]?.createdAt || ''
    };
  });
}

function normalizeUsername(value) {
  return String(value || '').trim();
}

function sanitizeUser(user) {
  return {
    username: user.username,
    password: user.password,
    name: user.name || ''
  };
}

function extractCodeContents(payload) {
  const codeContentsList = parseJsonSafely(payload.codeContentsJson, []);
  const codeContents = Array.isArray(codeContentsList)
    ? codeContentsList
        .map((item) => ({
          traceCode: typeof item?.traceCode === 'string' ? item.traceCode.trim() : '',
          scanTimestamp: item?.scanTimestamp || '',
          scanTime: item?.scanTime || ''
        }))
        .filter((item) => item.traceCode)
    : [];

  const fallbackCode = pickFirst(
    payload.traceCode,
    payload.code,
    payload.qrCode,
    payload.barcode,
    payload.materialCode
  );

  if (codeContents.length === 0 && fallbackCode) {
    codeContents.push({
      traceCode: fallbackCode,
      scanTimestamp: payload.scanTimestamp || '',
      scanTime: payload.scanTime || ''
    });
  }

  return codeContents;
}

function parseLaunchDetailItems(payload) {
  const launchDetailList = parseJsonSafely(payload.launchDetailJson, []);
  return Array.isArray(launchDetailList) ? launchDetailList : [];
}

function buildBatchLaunchRecord(payload, currentBatch, launchDetailList, codeContents = []) {
  const createdAt = new Date().toISOString();
  return {
    id: `batch_record_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`,
    batch: currentBatch.batch,
    createdAt,
    operationTypeCode: payload.operationTypeCode,
    payload,
    items: launchDetailList.map((item) => ({
      serialNumber: Number(item?.serialNumber || 0),
      businessVarietyId: item?.businessVarietyId || '',
      inventoryBatch: item?.inventoryBatch || '',
      num: item?.num || '',
      unitName: item?.unitName || '',
      varietyPackUnitId: item?.varietyPackUnitId || '',
      validDate: item?.validDate || ''
    })),
    codeContents
  };
}

function updateBatchSerialNum(batch, serialNumber) {
  const current = Number(batch?.maxSerialNum || 0);
  const incoming = Number(serialNumber || 0);
  if (Number.isFinite(incoming) && incoming > current) {
    batch.maxSerialNum = String(incoming);
  }
}

function compactList(list, limit = 500) {
  if (Array.isArray(list) && list.length > limit) {
    list.length = limit;
  }
}

function nextStateId(state, key, prefix) {
  state.sequence[key] = Number(state.sequence[key] || 0) + 1;
  const datePart = new Date().toISOString().slice(0, 10).replaceAll('-', '');
  return `${prefix}_${datePart}_${String(state.sequence[key]).padStart(6, '0')}`;
}

function normalizePdaMaterialPayload(payload, parsed = {}, batch = null, traceCodes = []) {
  return {
    action: payload.action || '',
    relationInfoId: payload.relationInfoId || batch?.id || '',
    batch: batch?.batch || payload.batch || parsed.belongProduceBatch || '',
    businessVarietyId: parsed.businessVarietyId || payload.businessVarietyId || '',
    weight: parsed.weight || payload.weight || payload.num || '',
    unitName: parsed.unitName || payload.unitName || '',
    inventoryBatch: parsed.inventoryBatch || payload.inventoryBatch || '',
    validDate: parsed.validDate || payload.validDate || '',
    belongProduceBatch: parsed.belongProduceBatch || payload.belongProduceBatch || '',
    serialNumber: parsed.serialNumber || payload.serialNumber || '',
    traceCodes
  };
}

function normalizeLaunchDetailItem(item, currentBatch, traceCode = '') {
  return {
    batch: currentBatch.batch,
    relationInfoId: currentBatch.id,
    businessVarietyId: item?.businessVarietyId || '',
    weight: item?.num || item?.weight || '',
    unitName: item?.unitName || '',
    inventoryBatch: item?.inventoryBatch || '',
    validDate: item?.validDate || '',
    serialNumber: String(item?.serialNumber || ''),
    varietyPackUnitId: item?.varietyPackUnitId || '',
    traceCode
  };
}

function addDecimalString(a, b) {
  const left = Number(a || 0);
  const right = Number(b || 0);
  const total = (Number.isFinite(left) ? left : 0) + (Number.isFinite(right) ? right : 0);
  return String(Number(total.toFixed(6)));
}

function normalizeSerialNumber(value) {
  const text = String(value ?? '').trim();
  if (!text) {
    return '';
  }

  const numeric = Number(text);
  return Number.isFinite(numeric) ? String(numeric) : text;
}

function isSameBatchRef(batch, value) {
  const target = String(value || '');
  if (!target) {
    return false;
  }

  return target === String(batch?.batch || '') || target === String(batch?.id || '');
}

function movementMatchesBatch(movement, batch) {
  return isSameBatchRef(batch, movement?.batch) || isSameBatchRef(batch, movement?.relationInfoId);
}

function recordMatchesBatch(record, batch) {
  return (
    isSameBatchRef(batch, record?.batch)
    || isSameBatchRef(batch, record?.payload?.batch)
    || isSameBatchRef(batch, record?.payload?.relationInfoId)
    || isSameBatchRef(batch, record?.parsed?.belongProduceBatch)
    || isSameBatchRef(batch, record?.parsed?.batch)
  );
}

function recordTraceCodeAt(record, index = 0) {
  const codeContents = Array.isArray(record?.codeContents) ? record.codeContents : [];
  const traceCodes = Array.isArray(record?.traceCodes) ? record.traceCodes : [];
  return codeContents[index]?.traceCode || traceCodes[index] || record?.code || '';
}

function normalizeMovementForMaterialList(movement, batch) {
  return {
    ...movement,
    batch: movement?.batch || batch.batch,
    relationInfoId: movement?.relationInfoId || batch.id,
    businessVarietyId: movement?.businessVarietyId || '',
    weight: pickFirst(movement?.weight, movement?.num) || '',
    unitName: movement?.unitName || '',
    inventoryBatch: movement?.inventoryBatch || '',
    validDate: movement?.validDate || '',
    serialNumber: normalizeSerialNumber(movement?.serialNumber),
    traceCode: movement?.traceCode || '',
    createdAt: movement?.createdAt || ''
  };
}

function buildMovementsFromLaunchRecord(record, batch) {
  if (!recordMatchesBatch(record, batch)) {
    return [];
  }

  const payload = record?.payload || {};
  const recordItems = Array.isArray(record?.items) && record.items.length
    ? record.items
    : parseLaunchDetailItems(payload);

  if (recordItems.length) {
    return recordItems.map((item, index) =>
      normalizeMovementForMaterialList(
        {
          recordId: record.id || '',
          batch: batch.batch,
          relationInfoId: batch.id,
          businessVarietyId: item?.businessVarietyId || '',
          weight: pickFirst(item?.weight, item?.num) || '',
          unitName: item?.unitName || '',
          inventoryBatch: item?.inventoryBatch || '',
          validDate: item?.validDate || '',
          serialNumber: item?.serialNumber || '',
          varietyPackUnitId: item?.varietyPackUnitId || '',
          traceCode: recordTraceCodeAt(record, index),
          rawItem: item,
          createdAt: record.createdAt || ''
        },
        batch
      )
    );
  }

  const traceCode = recordTraceCodeAt(record, 0) || payload.traceCode || payload.code || '';
  const parsed = {
    ...payload,
    ...(parseQrContent(traceCode) || {}),
    ...(record?.parsed || {})
  };

  return [
    normalizeMovementForMaterialList(
      {
        recordId: record.id || '',
        batch: batch.batch,
        relationInfoId: batch.id,
        businessVarietyId: parsed.businessVarietyId || '',
        weight: pickFirst(parsed.weight, parsed.num) || '',
        unitName: parsed.unitName || '',
        inventoryBatch: parsed.inventoryBatch || '',
        validDate: parsed.validDate || '',
        serialNumber: parsed.serialNumber || '',
        traceCode,
        rawPayload: payload,
        createdAt: record.createdAt || ''
      },
      batch
    )
  ].filter((movement) => movement.businessVarietyId || movement.weight || movement.serialNumber);
}

function collectMaterialMovementsForBatch(state, batch) {
  const erpMovements = Array.isArray(state.erpMaterialMovements)
    ? state.erpMaterialMovements
        .filter((movement) => movementMatchesBatch(movement, batch))
        .map((movement) => normalizeMovementForMaterialList(movement, batch))
    : [];
  const erpRecordIds = new Set(erpMovements.map((movement) => movement.recordId).filter(Boolean));
  const recordMovements = Array.isArray(state.launchRecords)
    ? state.launchRecords
        .flatMap((record) => buildMovementsFromLaunchRecord(record, batch))
        .filter((movement) => !movement.recordId || !erpRecordIds.has(movement.recordId))
    : [];

  return [...erpMovements, ...recordMovements];
}

function resolveLatestSerialNumber(movements) {
  const withSerialNumber = movements.filter((movement) => movement.serialNumber);
  if (!withSerialNumber.length) {
    return '';
  }

  return withSerialNumber
    .slice()
    .sort((a, b) => Date.parse(b.createdAt || '') - Date.parse(a.createdAt || ''))[0].serialNumber;
}

function resolvePdaSerialNumber(batch, payload = {}, movements = []) {
  const requestedSerialNumber = normalizeSerialNumber(
    pickFirst(payload.serialNumber, payload.currentSerialNumber, payload.pdaSerialNumber, payload.launchSerialNumber)
  );
  if (requestedSerialNumber) {
    return requestedSerialNumber;
  }

  const currentMax = Number(batch?.maxSerialNum || 0);
  return String((Number.isFinite(currentMax) ? currentMax : 0) + 1);
}

function buildMaterialListForPda(state, batch, payload = {}) {
  const allMovements = collectMaterialMovementsForBatch(state, batch);
  const targetSerialNumber = resolvePdaSerialNumber(batch, payload, allMovements);
  const movements = allMovements.filter((movement) => movement.serialNumber === targetSerialNumber);

  return (Array.isArray(batch.materialList) ? batch.materialList : []).map((material) => {
    const enriched = {
      ...material,
      inputAmount: '0',
      inventoryBatch: {},
      validDate: {}
    };

    movements
      .filter((movement) => String(movement.businessVarietyId || '').toLowerCase() === String(material.id || '').toLowerCase())
      .forEach((movement) => {
        const inventoryBatch = movement.inventoryBatch || 'null';
        enriched.inputAmount = addDecimalString(enriched.inputAmount, movement.weight);
        enriched.inventoryBatch[inventoryBatch] = addDecimalString(enriched.inventoryBatch[inventoryBatch], movement.weight);
        enriched.validDate[inventoryBatch] = movement.validDate || '';
      });

    return enriched;
  });
}

function addReceipt(state, input) {
  const now = new Date().toISOString();
  const receipt = {
    id: nextStateId(state, 'receipt', 'receipt'),
    action: input.action || '',
    status: input.status || 'received',
    message: input.message || '',
    batch: input.batch || input.transformed?.batch || '',
    recordId: input.recordId || '',
    traceCodes: Array.isArray(input.traceCodes) ? input.traceCodes : [],
    transformed: input.transformed || {},
    rawRequest: input.rawRequest || {},
    rawResponse: input.rawResponse || null,
    handshake: {
      status: input.status === 'rejected' ? 'REJECTED' : 'RECEIVED',
      ackedAt: '',
      ackPayload: null
    },
    createdAt: now,
    updatedAt: now
  };

  state.receipts.unshift(receipt);
  compactList(state.receipts, 500);
  return receipt;
}

function attachReceiptToResponse(body, receipt) {
  const response = {
    ...body,
    data: {
      ...(body.data || {}),
      receiptId: receipt.id,
      receiptStatus: receipt.status,
      handshakeStatus: receipt.handshake.status,
      serverTime: receipt.updatedAt
    }
  };
  receipt.rawResponse = response;
  return response;
}

async function sendReceiptResponse(res, state, receiptInput, responseBody) {
  const receipt = addReceipt(state, receiptInput);
  const response = attachReceiptToResponse(responseBody, receipt);
  res.locals.receiptId = receipt.id;
  await writeState(state);
  res.json(response);
}

function addMaterialMovement(state, input) {
  const now = new Date().toISOString();
  const movement = {
    id: nextStateId(state, 'materialMovement', 'move'),
    type: 'material_launch',
    source: 'PDA',
    action: input.action || '',
    receiptId: input.receiptId || '',
    recordId: input.recordId || '',
    batch: input.batch || '',
    relationInfoId: input.relationInfoId || '',
    businessVarietyId: input.businessVarietyId || '',
    weight: input.weight || '',
    unitName: input.unitName || '',
    inventoryBatch: input.inventoryBatch || '',
    validDate: input.validDate || '',
    serialNumber: input.serialNumber || '',
    traceCode: input.traceCode || '',
    transformed: input.transformed || {},
    rawPayload: input.rawPayload || {},
    rawItem: input.rawItem || null,
    createdAt: now
  };

  state.erpMaterialMovements.unshift(movement);
  compactList(state.erpMaterialMovements, 1000);
  return movement;
}

function shouldLogInterface(req) {
  return req.path.startsWith('/echain/') || req.path.startsWith('/api/admin/qrcodes');
}

async function appendInterfaceLog(entry) {
  const state = await readState();
  const log = {
    id: nextStateId(state, 'interfaceLog', 'iflog'),
    method: entry.method,
    path: entry.path,
    action: entry.action || '',
    statusCode: entry.statusCode,
    durationMs: entry.durationMs,
    receiptId: entry.receiptId || '',
    rawRequest: entry.rawRequest || {},
    rawResponse: entry.rawResponse || null,
    createdAt: entry.createdAt
  };

  state.interfaceLogs.unshift(log);
  compactList(state.interfaceLogs, 500);
  await writeState(state);
}

app.use((req, res, next) => {
  if (!shouldLogInterface(req)) {
    next();
    return;
  }

  const startedAt = Date.now();
  const originalJson = res.json.bind(res);
  let rawResponse = null;

  res.json = (body) => {
    rawResponse = body;
    return originalJson(body);
  };

  res.on('finish', () => {
    appendInterfaceLog({
      method: req.method,
      path: req.originalUrl || req.path,
      action: req.body?.action || req.query?.action || '',
      statusCode: res.statusCode,
      durationMs: Date.now() - startedAt,
      receiptId: rawResponse?.data?.receiptId || res.locals.receiptId || '',
      rawRequest: {
        query: req.query || {},
        body: req.body || {}
      },
      rawResponse,
      createdAt: new Date().toISOString()
    }).catch((error) => {
      console.error('Failed to append interface log:', error);
    });
  });

  next();
});

app.get('/api/health', async (_req, res) => {
  res.json(success({ ok: true }));
});

app.get('/api/admin/users', async (_req, res) => {
  const state = await readState();
  res.json(
    success({
      users: state.users.map(sanitizeUser)
    })
  );
});

app.post('/api/admin/users', async (req, res) => {
  const state = await readState();
  const payload = req.body || {};
  const username = normalizeUsername(payload.username);
  const password = String(payload.password || '').trim();
  const name = String(payload.name || '').trim();

  if (!username || !password) {
    res.status(400).json(failure('username and password are required'));
    return;
  }

  const index = state.users.findIndex((user) => user.username === username);
  const userRecord = { username, password, name };

  if (index >= 0) {
    state.users[index] = userRecord;
  } else {
    state.users.unshift(userRecord);
  }

  await writeState(state);
  res.json(success({ user: sanitizeUser(userRecord) }));
});

app.delete('/api/admin/users/:username', async (req, res) => {
  const state = await readState();
  const username = normalizeUsername(req.params.username);
  const index = state.users.findIndex((user) => user.username === username);

  if (index < 0) {
    res.status(404).json(failure('user not found'));
    return;
  }

  state.users.splice(index, 1);
  await writeState(state);
  res.json(success({ deleted: true, username }));
});

app.post('/echain/public/mslJoint.do', async (req, res) => {
  const payload = normalizePayload(req);
  if (payload.action !== 'generateToken4Msl') {
    res.json(failure(`Unsupported action: ${payload.action}`));
    return;
  }

  const state = await readState();
  const username = pickFirst(payload.username, payload.userName, payload.loginName);
  const password = pickFirst(payload.password, payload.passWord, payload.pwd);
  const matchedUser = state.users.find((user) => user.username === username && user.password === password);

  if (username && password && !matchedUser) {
    res.json(failure('invalid username or password'));
    return;
  }

  res.json(
    success({
      token: state.token.value,
      userName: matchedUser?.name || username || 'PDA Test User',
      tenantName: payload.tenantName || 'mslscsyxt'
    })
  );
});

app.post('/echain/thirdParty/mslJoint.do', async (req, res) => {
  const payload = normalizePayload(req);
  const { action } = payload;
  const state = await readState();

  if (action === 'getBatchList') {
    const page = Math.max(Number(payload.page || 1), 1);
    const rp = Math.max(Number(payload.rp || 15), 1);
    const filteredBatches = filterBatches(state, payload);
    const start = (page - 1) * rp;
    const content = filteredBatches.slice(start, start + rp).map((batch) => ({
      id: batch.id,
      batch: batch.batch,
      maxSerialNum: batch.maxSerialNum,
      batchType: batch.batchType,
      outputProduct: batch.outputProduct,
      materialList: buildMaterialListForPda(state, batch, payload)
    }));

    res.json(success({ content }));
    return;
  }

  if (action === 'getFormulaDetail') {
    const batch = findBatch(state, payload);
    if (!batch) {
      res.json(failure('batch not found'));
      return;
    }

    res.json(
      success({
        id: batch.id,
        batch: batch.batch,
        maxSerialNum: batch.maxSerialNum,
        outputProduct: batch.outputProduct,
        materialList: buildMaterialListForPda(state, batch, payload)
      })
    );
    return;
  }

  if (action === 'checkExistCode') {
    const rawCode = pickFirst(payload.traceCode, payload.code, payload.qrCode, payload.barcode, payload.materialCode);
    const parsed = parseQrContent(rawCode) || payload;
    const codeKey = rawCode || buildQrContent(parsed);
    const traceCodes = codeKey ? [codeKey] : [];
    const currentBatch = findBatch(state, parsed);
    const transformed = normalizePdaMaterialPayload(payload, parsed, currentBatch, traceCodes);

    if (!currentBatch) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'batch not found',
          rawRequest: payload,
          transformed,
          traceCodes
        },
        failure('batch not found')
      );
      return;
    }

    if (parsed.belongProduceBatch && parsed.belongProduceBatch !== currentBatch.batch) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'qr batch does not match current batch',
          batch: currentBatch.batch,
          rawRequest: payload,
          transformed,
          traceCodes
        },
        failure('qr batch does not match current batch')
      );
      return;
    }

    if (!parsed.businessVarietyId || !materialExists(currentBatch, parsed.businessVarietyId)) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'material is not in current batch',
          batch: currentBatch.batch,
          rawRequest: payload,
          transformed,
          traceCodes
        },
        failure('material is not in current batch')
      );
      return;
    }

    if (findUsedCode(state, codeKey)) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'qr code already used',
          batch: currentBatch.batch,
          rawRequest: payload,
          transformed,
          traceCodes
        },
        failure('qr code already used')
      );
      return;
    }

    await sendReceiptResponse(
      res,
      state,
      {
        action,
        status: 'checked',
        message: 'qr code checked',
        batch: currentBatch.batch,
        rawRequest: payload,
        transformed,
        traceCodes
      },
      success({
        exists: false,
        code: codeKey,
        traceCode: codeKey,
        parsed,
        batch: currentBatch.batch
      })
    );
    return;
  }

  if (action === 'saveMaterialLaunchRecord') {
    const codeContents = extractCodeContents(payload);
    const traceCodes = codeContents.map((item) => item.traceCode);
    const firstCode = traceCodes[0];
    const parsed = parseQrContent(firstCode) || payload;
    const currentBatch = findBatch(state, {
      ...payload,
      ...parsed,
      relationInfoId: payload.relationInfoId,
      batch: payload.batch
    });
    const transformed = normalizePdaMaterialPayload(payload, parsed, currentBatch, traceCodes);

    if (!currentBatch) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'batch not found',
          rawRequest: payload,
          transformed,
          traceCodes
        },
        failure('batch not found')
      );
      return;
    }

    if (!parsed.businessVarietyId || !materialExists(currentBatch, parsed.businessVarietyId)) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'material is not in current batch',
          batch: currentBatch.batch,
          rawRequest: payload,
          transformed,
          traceCodes
        },
        failure('material is not in current batch')
      );
      return;
    }

    const duplicatedCode = traceCodes.find((item) => findUsedCode(state, item));
    if (duplicatedCode) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'qr code already used',
          batch: currentBatch.batch,
          rawRequest: payload,
          transformed,
          traceCodes
        },
        failure('qr code already used')
      );
      return;
    }

    const record = {
      id: `record_${Date.now()}_${Math.random().toString(16).slice(2, 8)}`,
      code: firstCode || buildQrContent(parsed),
      traceCodes,
      parsed,
      batch: currentBatch.batch,
      createdAt: new Date().toISOString(),
      payload
    };
    updateBatchSerialNum(currentBatch, payload.serialNumber);

    codeContents.forEach((codeContent) => {
      const code = codeContent.traceCode;
      state.usedCodes.push({
        code,
        batch: currentBatch.batch,
        businessVarietyId: parseQrContent(code)?.businessVarietyId || parsed.businessVarietyId,
        scanTimestamp: codeContent.scanTimestamp,
        scanTime: codeContent.scanTime,
        createdAt: record.createdAt
      });
    });

    state.launchRecords.unshift(record);
    const receipt = addReceipt(state, {
      action,
      status: 'saved',
      message: 'material launch saved',
      batch: currentBatch.batch,
      recordId: record.id,
      traceCodes,
      rawRequest: payload,
      transformed
    });
    record.receiptId = receipt.id;

    const movementSources = codeContents.length ? codeContents : [{ traceCode: firstCode || '' }];
    movementSources.forEach((codeContent) => {
      addMaterialMovement(state, {
        action,
        receiptId: receipt.id,
        recordId: record.id,
        batch: currentBatch.batch,
        relationInfoId: currentBatch.id,
        businessVarietyId: transformed.businessVarietyId,
        weight: transformed.weight,
        unitName: transformed.unitName,
        inventoryBatch: transformed.inventoryBatch,
        validDate: transformed.validDate,
        serialNumber: transformed.serialNumber,
        traceCode: codeContent.traceCode || '',
        transformed,
        rawPayload: payload
      });
    });

    const response = attachReceiptToResponse(
      success({
        recordId: record.id,
        saved: true,
        batch: currentBatch.batch
      }),
      receipt
    );
    res.locals.receiptId = receipt.id;
    await writeState(state);
    res.json(response);
    return;
  }

  if (action === 'batchSaveMaterialLaunchRecord') {
    const currentBatch = findBatch(state, payload);
    if (!currentBatch) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'batch not found',
          rawRequest: payload,
          transformed: {
            action,
            batch: payload.batch || payload.relationInfoId || '',
            items: []
          }
        },
        failure('batch not found')
      );
      return;
    }

    const launchDetailList = parseLaunchDetailItems(payload);
    const transformedItems = launchDetailList.map((item) => normalizeLaunchDetailItem(item, currentBatch));
    if (launchDetailList.length === 0) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'launchDetailJson is empty',
          batch: currentBatch.batch,
          rawRequest: payload,
          transformed: {
            action,
            batch: currentBatch.batch,
            relationInfoId: currentBatch.id,
            items: []
          }
        },
        failure('launchDetailJson is empty')
      );
      return;
    }

    for (const item of launchDetailList) {
      if (!item?.businessVarietyId || !materialExistsById(currentBatch, item.businessVarietyId)) {
        const message = `material is not in current batch: ${item?.businessVarietyId || ''}`;
        await sendReceiptResponse(
          res,
          state,
          {
            action,
            status: 'rejected',
            message,
            batch: currentBatch.batch,
            rawRequest: payload,
            transformed: {
              action,
              batch: currentBatch.batch,
              relationInfoId: currentBatch.id,
              items: transformedItems
            }
          },
          failure(message)
        );
        return;
      }
    }

    const codeContents = extractCodeContents(payload);
    const duplicatedCode = codeContents
      .map((item) => item.traceCode)
      .find((item) => findUsedCode(state, item));
    if (duplicatedCode) {
      await sendReceiptResponse(
        res,
        state,
        {
          action,
          status: 'rejected',
          message: 'qr code already used',
          batch: currentBatch.batch,
          rawRequest: payload,
          transformed: {
            action,
            batch: currentBatch.batch,
            relationInfoId: currentBatch.id,
            items: transformedItems
          },
          traceCodes: codeContents.map((item) => item.traceCode)
        },
        failure('qr code already used')
      );
      return;
    }

    const record = buildBatchLaunchRecord(payload, currentBatch, launchDetailList, codeContents);
    const maxSerialNumber = launchDetailList.reduce((max, item) => Math.max(max, Number(item?.serialNumber || 0)), 0);
    updateBatchSerialNum(currentBatch, maxSerialNumber);

    codeContents.forEach((codeContent) => {
      const code = codeContent.traceCode;
      state.usedCodes.push({
        code,
        batch: currentBatch.batch,
        businessVarietyId: parseQrContent(code)?.businessVarietyId || '',
        scanTimestamp: codeContent.scanTimestamp,
        scanTime: codeContent.scanTime,
        createdAt: record.createdAt
      });
    });

    state.launchRecords.unshift(record);
    const transformed = {
      action,
      batch: currentBatch.batch,
      relationInfoId: currentBatch.id,
      items: launchDetailList.map((item, index) =>
        normalizeLaunchDetailItem(item, currentBatch, codeContents[index]?.traceCode || '')
      )
    };
    const receipt = addReceipt(state, {
      action,
      status: 'saved',
      message: 'batch material launch saved',
      batch: currentBatch.batch,
      recordId: record.id,
      traceCodes: codeContents.map((item) => item.traceCode),
      rawRequest: payload,
      transformed
    });
    record.receiptId = receipt.id;

    launchDetailList.forEach((item, index) => {
      const normalizedItem = transformed.items[index];
      addMaterialMovement(state, {
        action,
        receiptId: receipt.id,
        recordId: record.id,
        batch: currentBatch.batch,
        relationInfoId: currentBatch.id,
        businessVarietyId: normalizedItem.businessVarietyId,
        weight: normalizedItem.weight,
        unitName: normalizedItem.unitName,
        inventoryBatch: normalizedItem.inventoryBatch,
        validDate: normalizedItem.validDate,
        serialNumber: normalizedItem.serialNumber,
        traceCode: normalizedItem.traceCode,
        transformed: normalizedItem,
        rawPayload: payload,
        rawItem: item
      });
    });

    const response = attachReceiptToResponse(
      success({
        recordId: record.id,
        saved: true,
        batch: currentBatch.batch,
        savedCount: record.items.length
      }),
      receipt
    );
    res.locals.receiptId = receipt.id;
    await writeState(state);
    res.json(response);
    return;
  }

  if (action === 'getReceiptStatus') {
    const receiptKey = pickFirst(payload.receiptId, payload.recordId);
    const receipt = state.receipts.find((item) => item.id === receiptKey || item.recordId === receiptKey);

    if (!receipt) {
      res.json(failure('receipt not found'));
      return;
    }

    res.json(success({ receipt }));
    return;
  }

  if (action === 'confirmReceipt' || action === 'ackReceipt') {
    const receiptKey = pickFirst(payload.receiptId, payload.recordId);
    const receipt = state.receipts.find((item) => item.id === receiptKey || item.recordId === receiptKey);

    if (!receipt) {
      res.json(failure('receipt not found'));
      return;
    }

    const now = new Date().toISOString();
    receipt.handshake.status = 'ACKED';
    receipt.handshake.ackedAt = now;
    receipt.handshake.ackPayload = payload;
    receipt.updatedAt = now;
    await writeState(state);

    res.json(
      success({
        receiptId: receipt.id,
        recordId: receipt.recordId,
        receiptStatus: receipt.status,
        handshakeStatus: receipt.handshake.status,
        ackedAt: now
      })
    );
    return;
  }

  res.json(failure(`Unsupported action: ${action}`));
});

app.get('/api/admin/state', async (_req, res) => {
  const state = await readState();
  res.json(
    success({
      users: state.users.map((user) => ({ username: user.username, name: user.name })),
      batches: state.batches,
      usedCodes: state.usedCodes,
      launchRecords: state.launchRecords,
      receipts: state.receipts,
      interfaceLogs: state.interfaceLogs,
      erpMaterialMovements: state.erpMaterialMovements,
      qrUsageSummary: buildUsageSummary(state)
    })
  );
});

app.post('/api/admin/reset', async (_req, res) => {
  const state = createSeedState();
  await writeState(state);
  res.json(success({ reset: true }));
});

app.post('/api/admin/batches', async (req, res) => {
  const state = await readState();
  const payload = req.body || {};

  if (!payload.id || !payload.batch) {
    res.status(400).json(failure('id and batch are required'));
    return;
  }

  const batchRecord = {
    id: String(payload.id),
    batch: String(payload.batch),
    maxSerialNum: String(payload.maxSerialNum || '0'),
    batchType: String(payload.batchType || 'total'),
    outputProduct: payload.outputProduct || { id: '', name: '', num: 0 },
    materialList: Array.isArray(payload.materialList) ? payload.materialList : []
  };

  const index = state.batches.findIndex((item) => item.id === batchRecord.id || item.batch === batchRecord.batch);
  if (index >= 0) {
    state.batches[index] = batchRecord;
  } else {
    state.batches.unshift(batchRecord);
  }

  await writeState(state);
  res.json(success({ batch: batchRecord }));
});

app.post('/api/admin/batches/:batchId/materials', async (req, res) => {
  const state = await readState();
  const batch = state.batches.find((item) => item.id === req.params.batchId || item.batch === req.params.batchId);

  if (!batch) {
    res.status(404).json(failure('batch not found'));
    return;
  }

  const materials = Array.isArray(req.body.materialList) ? req.body.materialList : Array.isArray(req.body) ? req.body : [];
  batch.materialList = materials;
  await writeState(state);
  res.json(success({ batch }));
});

app.post('/api/admin/qrcodes/generate', async (req, res) => {
  const payload = req.body || {};
  const code = buildQrContent(payload);
  res.json(
    success({
      traceCode: code,
      code,
      text: code
    })
  );
});

app.post('/api/admin/qrcodes/render', async (req, res) => {
  const payload = req.body || {};
  const text = buildQrContent(payload);
  const dataUrl = await QRCode.toDataURL(text, {
    errorCorrectionLevel: 'M',
    margin: 1,
    scale: 6
  });

  res.json(
    success({
      traceCode: text,
      text,
      dataUrl
    })
  );
});

app.get('/api/admin/records', async (_req, res) => {
  const state = await readState();
  res.json(success({ records: state.launchRecords }));
});

app.get('/api/admin/receipts', async (_req, res) => {
  const state = await readState();
  res.json(success({ receipts: state.receipts }));
});

app.get('/api/admin/interface-logs', async (_req, res) => {
  const state = await readState();
  res.json(success({ interfaceLogs: state.interfaceLogs }));
});

app.get('/api/admin/material-movements', async (_req, res) => {
  const state = await readState();
  res.json(success({ erpMaterialMovements: state.erpMaterialMovements }));
});

app.get('*', (_req, res) => {
  res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

function listen(port) {
  return new Promise((resolve, reject) => {
    const server = app.listen(port, HOST, () => resolve(server));

    server.once('error', (error) => {
      reject(error);
    });
  });
}

async function startServer() {
  await ensureDataFile();

  const maxAttempts = 20;
  let currentPort = PORT;

  for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
    try {
      const server = await listen(currentPort);
      const address = server.address();
      const actualPort = typeof address === 'object' && address ? address.port : currentPort;
      console.log(`ERP Mock Server running at http://${HOST}:${actualPort}`);
      return;
    } catch (error) {
      if (error.code === 'EADDRINUSE') {
        console.warn(`Port ${currentPort} is in use, trying ${currentPort + 1}`);
        currentPort += 1;
        continue;
      }

      throw error;
    }
  }

  throw new Error(`Unable to find an available port starting from ${PORT}`);
}

startServer().catch((error) => {
  console.error(error);
  process.exit(1);
});
