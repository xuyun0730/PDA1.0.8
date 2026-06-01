# ERP Mock Server

这是一个给 PDA 离线联调用的 ERP Mock Server。它不是真 ERP，但接口路径、参数风格和返回结构尽量贴近 PDA 当前依赖的接口，方便在本地完成登录、取批次、扫码校验、提交投料记录的完整闭环。

## 已实现接口

- POST /echain/public/mslJoint.do?action=generateToken4Msl&tenantName=mslscsyxt
- POST /echain/thirdParty/mslJoint.do?action=getBatchList
- POST /echain/thirdParty/mslJoint.do?action=getFormulaDetail
- POST /echain/thirdParty/mslJoint.do?action=checkExistCode
- POST /echain/thirdParty/mslJoint.do?action=saveMaterialLaunchRecord
- POST /echain/thirdParty/mslJoint.do?action=batchSaveMaterialLaunchRecord
- POST /echain/thirdParty/mslJoint.do?action=getReceiptStatus
- POST /echain/thirdParty/mslJoint.do?action=confirmReceipt

## 管理页

启动后打开 http://localhost:3000，可以：

- 维护登录用户、账号和密码
- 查看批次和物料
- 新增或更新批次
- 生成二维码测试内容
- 预览二维码图片
- 查看 PDA 提交记录
- 查看回执状态、转换后的投料流水、原始接口 JSON 日志
- 清空示例数据

## 启动方式

1. 安装依赖：npm install
2. 启动服务：npm start
3. 浏览器打开：http://localhost:3000

## 数据文件

数据保存在 data/mock-db.json。管理页或 PDA 接口写入后会立即落盘，不需要重启。当前保留原始 JSON 收发内容，同时额外生成轻量 ERP 内部数据：

- `receipts`：每次 check / save 的回执，包含 `receiptId`、状态、握手状态、原始请求、原始响应、转换结果
- `interfaceLogs`：接口级原始 JSON 收发日志
- `erpMaterialMovements`：由 PDA JSON 转换出的标准化投料流水

## PDA 接入说明

- 默认监听 0.0.0.0:3000
- 如果 PDA 代码里写死了 BASE_URL = "http://192.168.0.132"，可以把本机 IP 改成这个地址，或者把 PDA 的基础地址改成当前电脑 IP
- 二维码测试内容建议使用完整 URL，格式类似：

```text
https://unify.id-cas.cn/lotinfo?businessVarietyId=mat001&weight=10&unitName=kg&inventoryBatch=INV001&validDate=2026-12-31&belongProduceBatch=PB20260525001&serialNumber=1
```

管理页也提供 `POST /api/admin/qrcodes/generate`，返回里的 `data.traceCode` 可直接作为 PDA 扫码内容。

## 轻量 ERP 回执

`checkExistCode`、`saveMaterialLaunchRecord`、`batchSaveMaterialLaunchRecord` 会在原有返回字段基础上追加：

```json
{
  "receiptId": "receipt_20260529_000001",
  "receiptStatus": "saved",
  "handshakeStatus": "RECEIVED",
  "serverTime": "2026-05-29T00:00:00.000Z"
}
```

PDA 或联调工具可以用 `getReceiptStatus` 查询，也可以用 `confirmReceipt` 回握手确认：

```json
{
  "action": "confirmReceipt",
  "receiptId": "receipt_20260529_000001",
  "clientStatus": "ACKED"
}
```
