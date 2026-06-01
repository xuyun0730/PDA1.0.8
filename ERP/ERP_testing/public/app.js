async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

const defaultQrPayload = {
  businessVarietyId: 'mat001',
  weight: '10',
  unitName: 'kg',
  inventoryBatch: 'INV001',
  validDate: '2026-12-31',
  belongProduceBatch: 'PB20260525001',
  serialNumber: '1'
};

const qrCoreFields = [
  ['businessVarietyId', '物料ID'],
  ['weight', '扫码重量'],
  ['unitName', '单位'],
  ['inventoryBatch', '库存批次'],
  ['validDate', '有效期'],
  ['belongProduceBatch', '所属生产批次'],
  ['serialNumber', '锅次']
];

let latestTraceCode = '';
let latestQrPayload = { ...defaultQrPayload };

function getCurrentBatchForPayload(payload, batches = window.__latestBatches || []) {
  const batchNo = payload?.belongProduceBatch || payload?.batch || '';
  return batches.find((batch) => batch.batch === batchNo || batch.id === payload?.relationInfoId) || batches[0] || null;
}

function findMaterialForPayload(payload, batch) {
  const materials = Array.isArray(batch?.materialList) ? batch.materialList : [];
  return materials.find((item) => item.id === payload?.businessVarietyId) || materials[0] || null;
}

function buildLaunchDetail(payload, material = null) {
  return {
    serialNumber: Number(payload.serialNumber || 1),
    businessVarietyId: payload.businessVarietyId || material?.id || '',
    inventoryBatch: payload.inventoryBatch || '',
    num: String(payload.weight || ''),
    unitName: payload.unitName || material?.unitName || '',
    varietyPackUnitId: material?.varietyPackUnitId || '',
    validDate: payload.validDate || ''
  };
}

function setPreviewJson(elementId, value, emptyText = '等待生成') {
  const element = document.getElementById(elementId);
  if (!element) {
    return;
  }

  element.textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
  element.classList.toggle('empty', !value);
  if (!value) {
    element.textContent = emptyText;
  }
}

function renderPdaPayloadPreviews(payload = latestQrPayload, traceCode = latestTraceCode) {
  const normalizedPayload = normalizeQrPayload(payload || defaultQrPayload);
  const currentBatch = getCurrentBatchForPayload(normalizedPayload);
  const currentMaterial = findMaterialForPayload(normalizedPayload, currentBatch);
  const relationInfoId = currentBatch?.id || 'batch001';
  const batchNo = currentBatch?.batch || normalizedPayload.belongProduceBatch || '';
  const launchDetail = buildLaunchDetail(normalizedPayload, currentMaterial);
  const codeContent = {
    traceCode: traceCode || '扫码后的完整 URL',
    scanTimestamp: Date.now(),
    scanTime: new Date().toISOString()
  };

  setPreviewJson('checkPayloadPreview', {
    action: 'checkExistCode',
    ecafeToken: 'mock-token-001',
    relationInfoId,
    batch: batchNo,
    traceCode: codeContent.traceCode
  });

  setPreviewJson('singleSavePayloadPreview', {
    action: 'saveMaterialLaunchRecord',
    ecafeToken: 'mock-token-001',
    relationInfoId,
    batch: batchNo,
    traceCode: codeContent.traceCode,
    codeContentsJson: JSON.stringify([codeContent]),
    serialNumber: normalizedPayload.serialNumber
  });

  setPreviewJson('batchSavePayloadPreview', {
    action: 'batchSaveMaterialLaunchRecord',
    ecafeToken: 'mock-token-001',
    relationInfoId,
    batch: batchNo,
    outputProductId: currentBatch?.outputProduct?.id || '',
    operationTypeCode: currentBatch?.batchType === 'premix' ? 'premixFeed' : 'directFeed',
    launchDetailJson: JSON.stringify([launchDetail]),
    codeContentsJson: JSON.stringify([codeContent])
  });
}

function materialMarkup(materialList) {
  return `<div class="materials">${materialList
    .map(
      (item) => `
        <div class="material-item">
          <span>${escapeHtml(item.name)} (${escapeHtml(item.id)})</span>
          <span>${escapeHtml(item.unitName)} / ${escapeHtml(item.netContentAllowGt)} - ${escapeHtml(item.netContentAllowLt)}</span>
        </div>`
    )
    .join('')}</div>`;
}

function renderUsageSummary(summaryList) {
  const tbody = document.getElementById('usageSummaryBody');
  tbody.innerHTML = summaryList.length
    ? summaryList
        .map(
          (item) => `
            <tr>
              <td>${escapeHtml(item.batch || '-')}</td>
              <td>
                <div class="table-stack">
                  <strong>${escapeHtml(item.outputProduct?.name || '-')}</strong>
                  <span>${escapeHtml(item.outputProduct?.id || '-')} / 数量 ${escapeHtml(item.outputProduct?.num ?? 0)}</span>
                </div>
              </td>
              <td>${escapeHtml(item.productionItemCount ?? 0)}</td>
              <td>${escapeHtml(item.premixItemCount ?? 0)}</td>
              <td>${escapeHtml(item.actualUseCount ?? 0)}</td>
              <td>${escapeHtml(item.actualUsedCodeCount ?? 0)}</td>
              <td>${escapeHtml(item.actualUsedMaterialCount ?? 0)}</td>
              <td>${escapeHtml(item.lastUsedAt || '-')}</td>
            </tr>`
        )
        .join('')
    : '<tr><td colspan="8" class="table-empty">暂无对照数据</td></tr>';
}

function topItems(list, limit = 6) {
  return Array.isArray(list) ? list.slice(0, limit) : [];
}

function renderErpTraceability(state) {
  const receipts = Array.isArray(state.receipts) ? state.receipts : [];
  const movements = Array.isArray(state.erpMaterialMovements) ? state.erpMaterialMovements : [];
  const logs = Array.isArray(state.interfaceLogs) ? state.interfaceLogs : [];

  document.getElementById('receiptCountChip').textContent = `${receipts.length} 条`;
  document.getElementById('movementCountChip').textContent = `${movements.length} 条`;
  document.getElementById('interfaceLogCountChip').textContent = `${logs.length} 条`;

  document.getElementById('receiptList').innerHTML = topItems(receipts)
    .map(
      (item) => `
        <article class="erp-mini-card">
          <div class="erp-mini-head">
            <strong>${escapeHtml(item.id)}</strong>
            <span class="status-chip ${item.status === 'rejected' ? 'is-empty' : 'is-generated'}">${escapeHtml(item.status)}</span>
          </div>
          <p>${escapeHtml(item.action || '-')} / ${escapeHtml(item.batch || '-')}</p>
          <p>握手：${escapeHtml(item.handshake?.status || '-')}，${escapeHtml(item.message || '-')}</p>
          <code>${escapeHtml(item.createdAt || '-')}</code>
        </article>`
    )
    .join('') || '<div class="endpoint">暂无回执</div>';

  document.getElementById('movementList').innerHTML = topItems(movements)
    .map(
      (item) => `
        <article class="erp-mini-card">
          <div class="erp-mini-head">
            <strong>${escapeHtml(item.businessVarietyId || '-')}</strong>
            <span class="status-chip is-generated">${escapeHtml(item.weight || '-')} ${escapeHtml(item.unitName || '')}</span>
          </div>
          <p>${escapeHtml(item.batch || '-')} / 锅次 ${escapeHtml(item.serialNumber || '-')}</p>
          <p>receiptId：${escapeHtml(item.receiptId || '-')}</p>
          <code>${escapeHtml(item.traceCode || '无 traceCode')}</code>
        </article>`
    )
    .join('') || '<div class="endpoint">暂无转换流水</div>';

  document.getElementById('interfaceLogList').innerHTML = topItems(logs)
    .map(
      (item) => `
        <article class="erp-mini-card">
          <div class="erp-mini-head">
            <strong>${escapeHtml(item.action || item.method || '-')}</strong>
            <span class="status-chip">${escapeHtml(item.statusCode || '-')}</span>
          </div>
          <p>${escapeHtml(item.method || '-')} ${escapeHtml(item.path || '-')}</p>
          <p>耗时 ${escapeHtml(item.durationMs ?? '-')}ms / receiptId ${escapeHtml(item.receiptId || '-')}</p>
          <code>raw JSON 已存档</code>
        </article>`
    )
    .join('') || '<div class="endpoint">暂无接口日志</div>';
}

function parseQrJson(value) {
  if (!value.trim()) {
    throw new Error('请输入 JSON 内容');
  }

  const parsed = JSON.parse(value);
  if (Array.isArray(parsed)) {
    return parsed[0] || {};
  }

  if (parsed && typeof parsed === 'object') {
    return parsed.data && typeof parsed.data === 'object' ? parsed.data : parsed;
  }

  throw new Error('JSON 内容必须是对象或数组');
}

function buildQrPayloadFromJson(rawValue) {
  const source = parseQrJson(rawValue);
  return {
    businessVarietyId: source.businessVarietyId || source.id || defaultQrPayload.businessVarietyId,
    weight: source.weight || source.formulaWeight || defaultQrPayload.weight,
    unitName: source.unitName || source.unit || defaultQrPayload.unitName,
    inventoryBatch: source.inventoryBatch || defaultQrPayload.inventoryBatch,
    validDate: source.validDate || defaultQrPayload.validDate,
    belongProduceBatch: source.belongProduceBatch || defaultQrPayload.belongProduceBatch,
    serialNumber: source.serialNumber || defaultQrPayload.serialNumber
  };
}

function normalizeQrPayload(payload) {
  return {
    businessVarietyId: payload.businessVarietyId || defaultQrPayload.businessVarietyId,
    weight: payload.weight || defaultQrPayload.weight,
    unitName: payload.unitName || defaultQrPayload.unitName,
    inventoryBatch: payload.inventoryBatch || defaultQrPayload.inventoryBatch,
    validDate: payload.validDate || defaultQrPayload.validDate,
    belongProduceBatch: payload.belongProduceBatch || defaultQrPayload.belongProduceBatch,
    serialNumber: payload.serialNumber || defaultQrPayload.serialNumber
  };
}

function normalizeIsoDate(date) {
  return date.toISOString().slice(0, 10);
}

function addDays(days) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date;
}

function deriveMaterialWeight(material) {
  if (material.formulaWeight) {
    return String(material.formulaWeight);
  }

  const low = Number(material.netContentAllowGt);
  const high = Number(material.netContentAllowLt);
  if (Number.isFinite(low) && Number.isFinite(high)) {
    return String((low + high) / 2);
  }

  if (material.netContentAllowGt) {
    return String(material.netContentAllowGt);
  }

  if (material.netContentAllowLt) {
    return String(material.netContentAllowLt);
  }

  return '';
}

function deriveSerialNumber(batch) {
  const value = Number(batch?.maxSerialNum || 1);
  return String(Number.isFinite(value) && value > 0 ? value : 1);
}

function buildMaterialQrPayload(batch, material) {
  const batchNo = batch.batch || batch.id || '';
  const payload = {
    businessVarietyId: material.id || '',
    weight: deriveMaterialWeight(material),
    unitName: material.unitName || defaultQrPayload.unitName,
    inventoryBatch: batchNo,
    validDate: normalizeIsoDate(addDays(365)),
    belongProduceBatch: batchNo,
    serialNumber: deriveSerialNumber(batch)
  };

  return payload;
}

function renderQrFieldPreview(payload) {
  const preview = document.getElementById('qrFieldPreview');
  if (!preview) {
    return;
  }

  preview.innerHTML = qrCoreFields
    .map(([field, label]) => {
      const value = payload[field] || '-';
      return `
        <div class="qr-field-pill">
          <span>${escapeHtml(label)}</span>
          <strong>${escapeHtml(value)}</strong>
        </div>`;
    })
    .join('');
}

function setLatestTraceCode(traceCode, payload) {
  latestTraceCode = traceCode || '';
  latestQrPayload = normalizeQrPayload(payload || defaultQrPayload);
  document.getElementById('qrResult').textContent = latestTraceCode || '等待生成';
  document.getElementById('qrCopyBtn')?.toggleAttribute('disabled', !latestTraceCode);
  renderQrFieldPreview(latestQrPayload);
  renderPdaPayloadPreviews(latestQrPayload, latestTraceCode);
}

async function generateAndRenderQr(payload) {
  const normalizedPayload = normalizeQrPayload(payload);
  const [textResult, renderResult] = await Promise.all([
    api('/api/admin/qrcodes/generate', {
      method: 'POST',
      body: JSON.stringify(normalizedPayload)
    }),
    api('/api/admin/qrcodes/render', {
      method: 'POST',
      body: JSON.stringify(normalizedPayload)
    })
  ]);

  const traceCode = textResult.data.traceCode || textResult.data.code || textResult.data.text;
  setLatestTraceCode(traceCode, normalizedPayload);
  document.getElementById('qrPreview').src = renderResult.data.dataUrl;
  return { code: traceCode, dataUrl: renderResult.data.dataUrl };
}

function fillQrFormFromPayload(payload) {
  const normalizedPayload = normalizeQrPayload(payload);
  latestQrPayload = normalizedPayload;
  const form = document.getElementById('qrForm');
  if (form?.businessVarietyId) {
    form.businessVarietyId.value = normalizedPayload.businessVarietyId;
    form.weight.value = normalizedPayload.weight;
    form.unitName.value = normalizedPayload.unitName;
    form.inventoryBatch.value = normalizedPayload.inventoryBatch;
    form.validDate.value = normalizedPayload.validDate;
    form.belongProduceBatch.value = normalizedPayload.belongProduceBatch;
    form.serialNumber.value = normalizedPayload.serialNumber;
  }

  const jsonInput = document.getElementById('qrJsonInput');
  if (jsonInput) {
    jsonInput.value = JSON.stringify(normalizedPayload, null, 2);
  }

  renderQrFieldPreview(normalizedPayload);
  renderPdaPayloadPreviews(normalizedPayload, latestTraceCode);
}

function downloadDataUrl(dataUrl, fileName) {
  const link = document.createElement('a');
  link.href = dataUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
}

function sanitizeFileName(value) {
  return String(value || 'qr').replace(/[\\/:*?"<>|\s]+/g, '_');
}

async function exportMaterialQr(batch, material) {
  const payload = buildMaterialQrPayload(batch, material);
  fillQrFormFromPayload(payload);
  const result = await generateAndRenderQr(payload);
  downloadDataUrl(
    result.dataUrl,
    `${sanitizeFileName(batch.batch || batch.id)}_${sanitizeFileName(material.name || material.id)}.png`
  );
  document.getElementById('qrForm')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function focusMaterialQr(batch, material) {
  const payload = buildMaterialQrPayload(batch, material);
  latestTraceCode = '';
  fillQrFormFromPayload(payload);
  setLatestTraceCode('', payload);
  document.getElementById('qrPreview')?.removeAttribute('src');
  document.getElementById('qrForm')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

const batchMaterialSamples = {
  premix: [
    {
      id: 'mat001',
      name: '预混原料1',
      formulaWeight: '10',
      netContentAllowGt: '9.5',
      netContentAllowLt: '10.5',
      isPremix: true,
      unitName: 'kg',
      varietyPackUnitId: 'unit001',
      belongOutputProductId: '',
      materialFlag: 'belongPremixMaterial'
    }
  ],
  total: [
    {
      id: 'mat001',
      name: '总混原料1',
      formulaWeight: '10',
      netContentAllowGt: '9.5',
      netContentAllowLt: '10.5',
      isPremix: false,
      unitName: 'kg',
      varietyPackUnitId: 'unit001',
      belongOutputProductId: 'out001',
      materialFlag: 'belongDirectFeedMaterial'
    }
  ]
};

const jsonDbStorageKey = 'erp-json-module-db-state';

let batchMaterialMode = 'premix';

const jsonModuleDefinitions = [
  {
    key: 'base',
    title: 'getBatchList 查询',
    type: '批次列表',
    status: '未生成',
    description: 'PDA 进入页面时按 operationTypeCode 拉取可投料批次'
  },
  {
    key: 'output',
    title: 'getFormulaDetail 主体',
    type: '配方明细',
    status: '未生成',
    description: 'PDA 选中批次后用 relationInfoId 获取产出品和物料'
  },
  {
    key: 'material-premix',
    title: 'materialList',
    type: '预混物料',
    status: '未生成',
    description: 'isPremix 为 true 的物料，扫码时 businessVarietyId 必须命中这里'
  },
  {
    key: 'material-total',
    title: 'materialList',
    type: '总混物料',
    status: '未生成',
    description: 'belongOutputProductId 非空的直投物料'
  },
  {
    key: 'full',
    title: 'getFormulaDetail 返回',
    type: '完整配方',
    status: '未生成',
    description: 'PDA 可直接消费的批次、产出品、物料组合结构'
  }
];

function createInitialJsonDbState() {
  return jsonModuleDefinitions.map((item) => ({
    key: item.key,
    title: item.title,
    type: item.type,
    status: item.status,
    updatedAt: '',
    description: item.description
  }));
}

function loadJsonDbState() {
  try {
    const parsed = JSON.parse(localStorage.getItem(jsonDbStorageKey) || 'null');
    if (Array.isArray(parsed) && parsed.length) {
      const saved = new Map(parsed.map((item) => [item.key, item]));
      return jsonModuleDefinitions.map((definition) => ({
        ...definition,
        status: saved.get(definition.key)?.status || definition.status,
        updatedAt: saved.get(definition.key)?.updatedAt || ''
      }));
    }
  } catch {
    // ignore storage corruption and fall back to defaults
  }

  return createInitialJsonDbState();
}

function saveJsonDbState(state) {
  localStorage.setItem(jsonDbStorageKey, JSON.stringify(state));
}

let jsonDbState = loadJsonDbState();

function updateJsonDbRow(key, status, updatedAt = new Date().toISOString()) {
  const row = jsonDbState.find((item) => item.key === key);
  if (row) {
    row.status = status;
    row.updatedAt = updatedAt;
    saveJsonDbState(jsonDbState);
  }
}

function resetJsonDbState() {
  jsonDbState = createInitialJsonDbState();
  saveJsonDbState(jsonDbState);
  renderJsonDbTable();
}

function batchFormSnapshot() {
  const form = document.getElementById('batchForm');
  const formData = new FormData(form);
  return {
    id: String(formData.get('id') || ''),
    batch: String(formData.get('batch') || ''),
    maxSerialNum: String(formData.get('maxSerialNum') || '0'),
    batchType: String(formData.get('batchType') || 'total'),
    outputProduct: {
      id: String(formData.get('outputProductId') || ''),
      name: String(formData.get('outputProductName') || ''),
      num: Number(formData.get('outputProductNum') || 0),
      unitName: '锅'
    }
  };
}

function buildBaseModuleJson(snapshot) {
  return {
    action: 'getBatchList',
    ecafeToken: 'mock-token-001',
    operationTypeCode: snapshot.batchType === 'premix' ? 'premixFeed' : 'directFeed',
    batch: snapshot.batch,
    page: 1,
    rp: 15
  };
}

function buildOutputModuleJson(snapshot) {
  return {
    action: 'getFormulaDetail',
    ecafeToken: 'mock-token-001',
    relationInfoId: snapshot.id,
    batch: snapshot.batch,
    outputProduct: snapshot.outputProduct
  };
}

function buildMaterialModuleJson(mode = batchMaterialMode) {
  const sample = batchMaterialSamples[mode] || batchMaterialSamples.premix;
  return sample;
}

function buildFullBatchModuleJson(snapshot, mode = batchMaterialMode) {
  return {
    id: snapshot.id,
    batch: snapshot.batch,
    maxSerialNum: snapshot.maxSerialNum,
    batchType: snapshot.batchType,
    outputProduct: snapshot.outputProduct,
    materialList: buildMaterialModuleJson(mode)
  };
}

function prettyJson(value) {
  return JSON.stringify(value, null, 2);
}

function setPreviewContent(elementId, value) {
  const element = document.getElementById(elementId);
  if (!element) {
    return;
  }

  element.textContent = prettyJson(value);
  element.classList.remove('empty');
}

function renderJsonDbTable() {
  const tbody = document.getElementById('jsonDbBody');
  if (!tbody) {
    return;
  }

  tbody.innerHTML = jsonDbState
    .map(
      (row) => `
        <tr>
          <td>${escapeHtml(row.title)}</td>
          <td>${escapeHtml(row.type)}</td>
          <td><span class="status-chip ${row.status === '未生成' ? 'is-empty' : 'is-generated'}">${escapeHtml(row.status)}</span></td>
          <td>${escapeHtml(row.updatedAt || '-')}</td>
          <td>${escapeHtml(row.description)}</td>
        </tr>`
    )
    .join('');
}

function syncModuleStatusChips() {
  const chipMap = {
    base: document.getElementById('baseModuleStatusChip'),
    output: document.getElementById('outputModuleStatusChip'),
    'material-premix': document.getElementById('materialPremixStatusChip'),
    'material-total': document.getElementById('materialTotalStatusChip'),
    full: document.getElementById('fullModuleStatusChip')
  };

  const statusLookup = new Map(jsonDbState.map((item) => [item.key, item]));
  Object.entries(chipMap).forEach(([key, chip]) => {
    if (!chip) {
      return;
    }

    const row = statusLookup.get(key);
    const statusText = row?.status || '未生成';
    chip.textContent = statusText;
    chip.classList.toggle('is-generated', chip.textContent !== '未生成');
    chip.classList.toggle('is-empty', chip.textContent === '未生成');
  });
}

function syncModuleButtons() {
  const premixBtn = document.getElementById('batchMaterialPremixBtn');
  const totalBtn = document.getElementById('batchMaterialTotalBtn');

  premixBtn?.classList.toggle('is-active', batchMaterialMode === 'premix');
  totalBtn?.classList.toggle('is-active', batchMaterialMode === 'total');
}

function renderBatchModulePreviews() {
  const snapshot = batchFormSnapshot();
  setPreviewContent('baseModulePreview', buildBaseModuleJson(snapshot));
  setPreviewContent('outputModulePreview', buildOutputModuleJson(snapshot));
  setPreviewContent('materialModulePreview', buildMaterialModuleJson(batchMaterialMode));
  setPreviewContent('fullModulePreview', buildFullBatchModuleJson(snapshot, batchMaterialMode));
}

function markBatchModuleGenerated(key, status) {
  updateJsonDbRow(key, status);
  renderJsonDbTable();
  syncModuleStatusChips();
}

function getBatchMaterialTextarea() {
  return document.querySelector('textarea[name="materialList"]');
}

function renderBatchMaterialSample() {
  const textarea = getBatchMaterialTextarea();
  if (!textarea) {
    return;
  }

  const sample = batchMaterialSamples[batchMaterialMode] || batchMaterialSamples.total;
  textarea.value = JSON.stringify(sample, null, 2);
}

function setBatchMaterialMode(mode) {
  batchMaterialMode = mode === 'total' ? 'total' : 'premix';
  syncModuleButtons();
  renderBatchModulePreviews();
}

function renderBatchTable(batches) {
  const tbody = document.getElementById('batchTableBody');
  tbody.innerHTML = batches.length
    ? batches
        .map((batch) => {
          const materialCount = Array.isArray(batch.materialList) ? batch.materialList.length : 0;
          const premixCount = Array.isArray(batch.materialList)
            ? batch.materialList.filter((item) => item.isPremix).length
            : 0;
          const operationType = batch.batchType === 'premix' ? 'premixFeed' : 'directFeed';
          const batchEncoded = escapeHtml(encodeURIComponent(JSON.stringify(batch)));
          const materialRows = Array.isArray(batch.materialList) && batch.materialList.length
            ? batch.materialList
                .map(
                  (item) => {
                    const materialEncoded = escapeHtml(encodeURIComponent(JSON.stringify(item)));
                    const weight = deriveMaterialWeight(item) || '-';
                    return `
                    <tr>
                      <td>${escapeHtml(item.id || '-')}</td>
                      <td>${escapeHtml(item.name || '-')}</td>
                      <td>${escapeHtml(weight)} ${escapeHtml(item.unitName || '')}</td>
                      <td>${escapeHtml(item.netContentAllowGt ?? '-')} - ${escapeHtml(item.netContentAllowLt ?? '-')}</td>
                      <td><span class="table-badge">${item.isPremix ? 'premix' : 'direct'}</span></td>
                      <td>${escapeHtml(item.belongOutputProductId || '-')}</td>
                      <td>${escapeHtml(item.varietyPackUnitId || '-')}</td>
                      <td>
                        <div class="table-actions">
                          <button
                            type="button"
                            class="ghost-btn export-qr-btn"
                            data-fill-material-qr="1"
                            data-export-batch="${batchEncoded}"
                            data-export-material="${materialEncoded}"
                          >
                            填入扫码表单
                          </button>
                          <button
                            type="button"
                            class="ghost-btn export-qr-btn"
                            data-export-material-qr="1"
                            data-export-batch="${batchEncoded}"
                            data-export-material="${materialEncoded}"
                          >
                            下载二维码
                          </button>
                        </div>
                      </td>
                    </tr>`;
                  }
                )
                .join('')
            : '<tr><td colspan="8" class="table-empty">暂无物料明细</td></tr>';

          return `
            <tr class="batch-main-row" data-batch-row="${escapeHtml(batch.id || batch.batch || '')}">
              <td>
                <button class="table-expand-btn" type="button" data-toggle-batch-detail="${escapeHtml(batch.id || batch.batch || '')}">展开</button>
              </td>
              <td>${escapeHtml(batch.id || '-')}</td>
              <td>${escapeHtml(batch.batch || '-')}</td>
              <td><span class="table-badge">${escapeHtml(batch.batchType || 'total')}</span></td>
              <td>${escapeHtml(batch.maxSerialNum ?? 0)}</td>
              <td>
                <div class="table-stack">
                  <strong>${escapeHtml(batch.outputProduct?.name || '-')}</strong>
                  <span>${escapeHtml(batch.outputProduct?.id || '-')} / 数量 ${escapeHtml(batch.outputProduct?.num ?? 0)}</span>
                </div>
              </td>
              <td>
                <div class="table-stack">
                  <strong>${escapeHtml(materialCount)} 项</strong>
                  <span>预混 ${escapeHtml(premixCount)} / 直投 ${escapeHtml(materialCount - premixCount)}</span>
                </div>
              </td>
              <td>
                <button
                  type="button"
                  class="ghost-btn export-qr-btn"
                  data-fill-material-qr="1"
                  data-export-batch="${batchEncoded}"
                  data-export-material="${escapeHtml(encodeURIComponent(JSON.stringify(batch.materialList?.[0] || {})))}"
                  ${materialCount ? '' : 'disabled'}
                >
                  生成首个物料 traceCode
                </button>
              </td>
            </tr>
            <tr class="batch-detail-row" data-batch-detail-row="${escapeHtml(batch.id || batch.batch || '')}" hidden>
              <td colspan="8">
                <div class="batch-detail-shell">
                  <div class="batch-detail-head">
                    <strong>materialList 物料子明细</strong>
                    <span>operationTypeCode: ${escapeHtml(operationType)} / relationInfoId: ${escapeHtml(batch.id || '-')}</span>
                  </div>
                  <div class="table-wrap detail-table-wrap">
                    <table class="data-table detail-table">
                      <thead>
                        <tr>
                          <th>物料ID</th>
                          <th>物料名称</th>
                          <th>扫码重量</th>
                          <th>允许范围</th>
                          <th>投料类型</th>
                          <th>关联产出品ID</th>
                          <th>包装单位ID</th>
                          <th>操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        ${materialRows}
                      </tbody>
                    </table>
                  </div>
                </div>
              </td>
            </tr>`;
        })
        .join('')
    : '<tr><td colspan="8" class="table-empty">暂无批次数据</td></tr>';
}

async function loadState() {
  const [stateResult, usersResult] = await Promise.all([
    api('/api/admin/state'),
    api('/api/admin/users')
  ]);
  const state = stateResult.data;
  const users = usersResult.data.users;
  const usageSummary = state.qrUsageSummary || [];
  window.__latestBatches = state.batches;

  document.getElementById('batchCount').textContent = state.batches.length;
  document.getElementById('userCount').textContent = users.length;
  document.getElementById('recordCount').textContent = state.launchRecords.length;
  document.getElementById('usedCount').textContent = state.usedCodes.length;
  renderUsageSummary(usageSummary);
  renderErpTraceability(state);
  renderPdaPayloadPreviews(latestQrPayload, latestTraceCode);

  const userList = document.getElementById('userList');
  userList.innerHTML = users.length
    ? users
        .map(
          (user) => `
            <article class="record-card user-card">
              <strong>${escapeHtml(user.username)}</strong>
              <p>密码：${escapeHtml(user.password)}</p>
              <p>名称：${escapeHtml(user.name || '-')}</p>
              <div class="card-actions">
                <button class="ghost-btn" data-edit-user="${escapeHtml(user.username)}">编辑</button>
                <button class="ghost-btn danger" data-delete-user="${escapeHtml(user.username)}">删除</button>
              </div>
            </article>`
        )
        .join('')
    : '<div class="endpoint">暂无用户</div>';

  const batchList = document.getElementById('batchList');
  batchList.innerHTML = state.batches
    .map(
      (batch) => `
        <article class="card">
          <div class="card-head">
            <strong>${escapeHtml(batch.batch)}</strong>
            <span class="badge">${escapeHtml(batch.batchType || 'total')}</span>
          </div>
          <p>产出品：${escapeHtml(batch.outputProduct?.name || '-')} / 数量 ${escapeHtml(batch.outputProduct?.num ?? 0)}</p>
          ${materialMarkup(batch.materialList || [])}
        </article>`
    )
    .join('');

  renderBatchTable(state.batches);

  const recordList = document.getElementById('recordList');
  recordList.innerHTML = state.launchRecords.length
    ? state.launchRecords
        .map(
          (record) => `
            <article class="record-card">
              <strong>${escapeHtml(record.batch)}</strong>
              <p>${escapeHtml(record.createdAt)}</p>
              <code>${escapeHtml(record.code)}</code>
              <div class="record-meta">
                <span>物料：${escapeHtml(record.parsed?.businessVarietyId || record.payload?.businessVarietyId || '-')}</span>
                <span>产出批次：${escapeHtml(record.parsed?.belongProduceBatch || record.batch || '-')}</span>
                <span>锅次：${escapeHtml(record.parsed?.serialNumber || '-')} / 批号：${escapeHtml(record.parsed?.inventoryBatch || '-')}</span>
              </div>
            </article>`
        )
        .join('')
    : '<div class="endpoint">暂无提交记录</div>';
}

function parseMaterialList(value) {
  if (!value.trim()) {
    return [];
  }

  const parsed = JSON.parse(value);
  if (!Array.isArray(parsed)) {
    throw new Error('materialList 必须是数组');
  }

  return parsed;
}

function fillUserForm(user) {
  const form = document.getElementById('userForm');
  form.username.value = user.username || '';
  form.password.value = user.password || '';
  form.name.value = user.name || '';
}

document.getElementById('userRefreshBtn').addEventListener('click', () => {
  loadState().catch((error) => alert(error.message));
});

document.getElementById('userList').addEventListener('click', async (event) => {
  const editUsername = event.target.closest('[data-edit-user]')?.dataset.editUser;
  const deleteUsername = event.target.closest('[data-delete-user]')?.dataset.deleteUser;

  if (editUsername) {
    const result = await api('/api/admin/users');
    const user = result.data.users.find((item) => item.username === editUsername);
    if (user) {
      fillUserForm(user);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
    return;
  }

  if (deleteUsername) {
    if (!confirm(`确定删除用户 ${deleteUsername} 吗？`)) {
      return;
    }

    await api(`/api/admin/users/${encodeURIComponent(deleteUsername)}`, {
      method: 'DELETE'
    });
    await loadState();
  }
});

document.getElementById('refreshBtn').addEventListener('click', () => {
  loadState().catch((error) => alert(error.message));
});

document.getElementById('batchTableRefreshBtn').addEventListener('click', () => {
  loadState().catch((error) => alert(error.message));
});

document.getElementById('usageSummaryRefreshBtn').addEventListener('click', () => {
  loadState().catch((error) => alert(error.message));
});

document.getElementById('erpTraceRefreshBtn').addEventListener('click', () => {
  loadState().catch((error) => alert(error.message));
});

document.getElementById('qrJsonSubmitBtn').addEventListener('click', async () => {
  try {
    const rawValue = document.getElementById('qrJsonInput').value;
    const payload = buildQrPayloadFromJson(rawValue);
    fillQrFormFromPayload(payload);
    await generateAndRenderQr(payload);
  } catch (error) {
    alert(error.message);
  }
});

document.getElementById('qrCopyBtn')?.addEventListener('click', async () => {
  if (!latestTraceCode) {
    return;
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(latestTraceCode);
    } else {
      const textarea = document.createElement('textarea');
      textarea.value = latestTraceCode;
      textarea.setAttribute('readonly', 'readonly');
      textarea.style.position = 'fixed';
      textarea.style.left = '-9999px';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      textarea.remove();
    }

    const button = document.getElementById('qrCopyBtn');
    const originalText = button.textContent;
    button.textContent = '已复制';
    window.setTimeout(() => {
      button.textContent = originalText;
    }, 1200);
  } catch (error) {
    alert(`复制失败：${error.message}`);
  }
});

document.getElementById('batchTableBody').addEventListener('click', (event) => {
  const toggleButton = event.target.closest('[data-toggle-batch-detail]');
  if (!toggleButton) {
    const fillButton = event.target.closest('[data-fill-material-qr]');
    const exportButton = event.target.closest('[data-export-material-qr]');
    if (!fillButton && !exportButton) {
      return;
    }

    const actionButton = fillButton || exportButton;
    const batchJson = actionButton.dataset.exportBatch || '';
    const materialJson = actionButton.dataset.exportMaterial || '';

    try {
      const batch = JSON.parse(decodeURIComponent(batchJson));
      const material = JSON.parse(decodeURIComponent(materialJson));
      if (fillButton) {
        focusMaterialQr(batch, material);
      } else {
        exportMaterialQr(batch, material).catch((error) => alert(error.message));
      }
    } catch (error) {
      alert('二维码数据解析失败');
    }

    return;
  }

  const batchKey = toggleButton.dataset.toggleBatchDetail;
  const detailRow = document.querySelector(`[data-batch-detail-row="${CSS.escape(batchKey)}"]`);
  if (!detailRow) {
    return;
  }

  const isHidden = detailRow.hasAttribute('hidden');
  if (isHidden) {
    detailRow.removeAttribute('hidden');
    toggleButton.textContent = '收起';
    toggleButton.closest('.batch-main-row')?.classList.add('is-expanded');
  } else {
    detailRow.setAttribute('hidden', 'hidden');
    toggleButton.textContent = '展开';
    toggleButton.closest('.batch-main-row')?.classList.remove('is-expanded');
  }
});

document.getElementById('resetBtn').addEventListener('click', async () => {
  if (!confirm('确定恢复示例数据吗？')) {
    return;
  }

  await api('/api/admin/reset', { method: 'POST', body: '{}' });
  await loadState();
});

document.getElementById('userForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const payload = Object.fromEntries(form.entries());

  await api('/api/admin/users', {
    method: 'POST',
    body: JSON.stringify(payload)
  });

  event.currentTarget.reset();
  await loadState();
});

document.getElementById('batchForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const outputProduct = {
    id: form.get('outputProductId') || '',
    name: form.get('outputProductName') || '',
    num: Number(form.get('outputProductNum') || 0)
  };

  const payload = {
    id: form.get('id'),
    batch: form.get('batch'),
    maxSerialNum: form.get('maxSerialNum') || '0',
    batchType: form.get('batchType') || 'total',
    outputProduct,
    materialList: parseMaterialList(String(form.get('materialList') || '[]'))
  };

  await api('/api/admin/batches', {
    method: 'POST',
    body: JSON.stringify(payload)
  });

  event.currentTarget.reset();
  await loadState();
});

document.getElementById('batchMaterialTotalBtn').addEventListener('click', () => {
  setBatchMaterialMode('total');
  renderBatchMaterialSample();
});

document.getElementById('batchMaterialPremixBtn').addEventListener('click', () => {
  setBatchMaterialMode('premix');
  renderBatchMaterialSample();
});

document.getElementById('batchMaterialFillBtn').addEventListener('click', () => {
  renderBatchMaterialSample();
  renderBatchModulePreviews();
});

document.getElementById('jsonModuleRefreshBtn').addEventListener('click', () => {
  renderBatchModulePreviews();
  renderJsonDbTable();
});

document.getElementById('baseModuleGenBtn').addEventListener('click', () => {
  const snapshot = batchFormSnapshot();
  setPreviewContent('baseModulePreview', buildBaseModuleJson(snapshot));
  markBatchModuleGenerated('base', '已生成');
});

document.getElementById('outputModuleGenBtn').addEventListener('click', () => {
  const snapshot = batchFormSnapshot();
  setPreviewContent('outputModulePreview', buildOutputModuleJson(snapshot));
  markBatchModuleGenerated('output', '已生成');
});

document.getElementById('materialModuleGenBtn').addEventListener('click', () => {
  setPreviewContent('materialModulePreview', buildMaterialModuleJson(batchMaterialMode));
  markBatchModuleGenerated(batchMaterialMode === 'premix' ? 'material-premix' : 'material-total', '已生成');
});

document.getElementById('fullModuleGenBtn').addEventListener('click', () => {
  const snapshot = batchFormSnapshot();
  setPreviewContent('fullModulePreview', buildFullBatchModuleJson(snapshot, batchMaterialMode));
  markBatchModuleGenerated('full', '已生成');
});

document.getElementById('jsonDbBody').addEventListener('click', (event) => {
  const row = event.target.closest('tr');
  if (!row) {
    return;
  }
});

setBatchMaterialMode('premix');
renderBatchMaterialSample();
renderBatchModulePreviews();
renderJsonDbTable();
syncModuleStatusChips();
fillQrFormFromPayload(defaultQrPayload);

document.getElementById('qrForm').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const payload = normalizeQrPayload(Object.fromEntries(form.entries()));
  fillQrFormFromPayload(payload);
  await generateAndRenderQr(payload);
});

loadState().catch((error) => {
  console.error(error);
  alert(error.message);
});
