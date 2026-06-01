# PDA 投料工序 ERP 对接 JSON 文档

本文档按当前 PDA 工程实际调用整理，供 ERP 方实现接口或做字段映射。

## 1. 通讯约定

PDA 与 ERP 之间使用 HTTP/HTTPS 请求-响应模式，不使用 WebSocket、MQTT 或服务端主动推送。

当前 PDA 请求格式为：

```http
Content-Type: application/x-www-form-urlencoded
```

说明：

- URL 上通过 `action` 区分接口。
- 登录以外的业务接口通过 Query 参数 `ecafeToken` 传登录令牌。
- Body 是表单字段。
- 部分表单字段的值本身是 JSON 字符串，例如 `launchDetailJson`、`codeContentsJson`。
- ERP 响应统一返回 JSON。

统一成功响应外层结构：

```json
{
  "code": "0",
  "errcode": 0,
  "message": "success",
  "errmsg": "success",
  "data": {}
}
```

统一失败响应建议：

```json
{
  "code": "1",
  "errcode": 1,
  "message": "错误原因",
  "errmsg": "错误原因",
  "data": null
}
```

登录失效或无权限建议：

```json
{
  "code": "99",
  "errcode": 99,
  "message": "登录失效，请重新登录",
  "errmsg": "登录失效，请重新登录",
  "data": null
}
```

## 2. 登录

接口：

```http
POST /echain/public/mslJoint.do?action=generateToken4Msl&tenantName=mslscsyxt
Content-Type: application/x-www-form-urlencoded
```

请求 Body：

```json
{
  "username": "pda",
  "password": "123456"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | String | 是 | PDA 登录账号 |
| password | String | 是 | PDA 登录密码 |

成功响应：

```json
{
  "code": "0",
  "errcode": 0,
  "message": "success",
  "errmsg": "success",
  "data": {
    "token": "erp-token-value"
  }
}
```

PDA 会保存 `data.token`，后续接口作为 `ecafeToken` 传递。

## 3. 生产批次列表 / 投料工序 / 批次分页查询

接口：

```http
POST /echain/thirdParty/mslJoint.do?action=getBatchList&ecafeToken=${token}
Content-Type: application/x-www-form-urlencoded
```

请求 Body：

```json
{
  "id": "batch-id-001",
  "batch": "PB20260525002",
  "rp": 10,
  "page": 1,
  "operationTypeCode": "overallFeed",
  "buildControls": "needBuildMaxSerialNum"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| id | String | 否 | 生产批次主键，精确查询时使用 |
| batch | String | 否 | 生产批次号，支持模糊匹配 |
| rp | Long | 否 | 分页大小 |
| page | Long | 否 | 当前页，从 1 开始 |
| operationTypeCode | String | 是 | 操作类型，见第 8 节 |
| buildControls | String | 否 | 需要返回已完成锅次时固定传 `needBuildMaxSerialNum` |

成功响应：

```json
{
  "code": "0",
  "errcode": 0,
  "message": "success",
  "errmsg": "success",
  "data": {
    "content": [
      {
        "id": "batch-id-001",
        "batch": "PB20260525002",
        "startDate": "2026-05-25",
        "endDate": "2026-05-29",
        "maxSerialNum": "0",
        "outputProduct": {
          "id": "product-id-001",
          "name": "700g 儿童成长配方奶粉",
          "num": "22",
          "unitName": "料"
        },
        "materialList": [
          {
            "id": "material-id-001",
            "name": "乳清蛋白粉WPC35",
            "formulaWeight": "10",
            "netContentAllowLt": "11",
            "netContentAllowGt": "9",
            "isPremix": true,
            "unitName": "kg",
            "netContentAllowDeviation": 10,
            "belongOutputProductId": "",
            "materialFlag": "A"
          }
        ]
      }
    ],
    "empty": false,
    "first": true,
    "last": true,
    "number": 0,
    "numberOfElements": 1,
    "size": 10,
    "totalElements": "1",
    "totalPages": 1
  }
}
```

PDA 使用字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| data.content[].id | String | 批次主键，后续作为 `relationInfoId` 使用 |
| data.content[].batch | String | 批次号，后续保存投料记录时回传 |
| data.content[].maxSerialNum | String | 已完成锅次，PDA 会加 1 得到当前锅次 |
| data.content[].outputProduct.id | String | 产出品 id，保存投料记录时回传 |
| data.content[].outputProduct.num | Number/String | 总锅次数 |
| data.content[].materialList[].id | String | 物料 id，对应二维码里的 `businessVarietyId` |
| data.content[].materialList[].netContentAllowLt | Number/String | 允许投料最大值 |
| data.content[].materialList[].netContentAllowGt | Number/String | 允许投料最小值 |
| data.content[].materialList[].isPremix | Boolean | 是否预混料 |

## 4. 预混明细查询

接口：

```http
POST /echain/thirdParty/mslJoint.do?action=getFormulaDetail&ecafeToken=${token}
Content-Type: application/x-www-form-urlencoded
```

请求 Body：

```json
{
  "businessVarietyId": "premix-material-id",
  "relationInfoId": "batch-id-001",
  "operationTypeCode": "premixFeed",
  "buildControls": "needBuildMaxSerialNum"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| businessVarietyId | String | 是 | 预混物料 id |
| relationInfoId | String | 是 | 生产批次主键 |
| operationTypeCode | String | 是 | 固定为 `premixFeed` |
| buildControls | String | 否 | 需要返回已完成锅次时固定传 `needBuildMaxSerialNum` |

成功响应：

```json
{
  "code": "0",
  "errcode": 0,
  "message": "success",
  "errmsg": "success",
  "data": {
    "id": "batch-id-001",
    "batch": "PB20260525002",
    "maxSerialNum": "0",
    "outputProduct": {
      "id": "premix-output-id",
      "name": "预混产出品",
      "num": "22",
      "unitName": "料"
    },
    "materialList": [
      {
        "id": "child-material-id-001",
        "name": "预混子物料A",
        "formulaWeight": "5",
        "netContentAllowLt": "5.5",
        "netContentAllowGt": "4.5",
        "isPremix": false,
        "unitName": "kg",
        "netContentAllowDeviation": 10
      }
    ]
  }
}
```

注意：当前 PDA 期望 `data` 直接包含 `id/batch/maxSerialNum/outputProduct/materialList`，不要再嵌套到 `content.formulaDetail`。

## 5. 二维码校验

接口：

```http
POST /echain/thirdParty/mslJoint.do?action=checkExistCode&ecafeToken=${token}
Content-Type: application/x-www-form-urlencoded
```

请求 Body：

```json
{
  "traceCode": "https://example/qrcode?businessVarietyId=material-id-001&weight=10kg&inventoryBatch=IB20260501&validDate=2027-05-01&belongProduceBatch=PB20260525002&serialNumber=1"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| traceCode | String | 是 | PDA 扫描到的二维码原文 |

成功响应：

```json
{
  "code": "0",
  "errcode": 0,
  "message": "success",
  "errmsg": "success",
  "data": {
    "exists": true,
    "code": "https://example/qrcode?...",
    "batch": "PB20260525002",
    "parsed": {
      "businessVarietyId": "material-id-001",
      "weight": "10kg",
      "inventoryBatch": "IB20260501",
      "validDate": "2027-05-01",
      "belongProduceBatch": "PB20260525002",
      "serialNumber": "1"
    }
  }
}
```

PDA 后续会从二维码原文中解析这些参数：

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| businessVarietyId | String | 是 | 物料 id，必须能在当前物料列表中找到 |
| weight | String | 是 | 当前二维码代表的投料重量，支持 `10`、`10kg`、`1000g` |
| inventoryBatch | String | 否 | 库存批次 |
| validDate | String | 否 | 有效期 |
| belongProduceBatch | String | 否 | 二维码所属生产批次，传了就必须等于当前生产批次 |
| serialNumber | String/Number | 否 | 标签锅次，传了就必须等于 PDA 当前锅次 |

## 6. 保存投料记录：总混 / 预混

接口：

```http
POST /echain/thirdParty/mslJoint.do?action=saveMaterialLaunchRecord&ecafeToken=${token}
Content-Type: application/x-www-form-urlencoded
```

请求 Body：

```json
{
  "relationInfoId": "batch-id-001",
  "batch": "PB20260525002",
  "outputProductId": "product-id-001",
  "serialNumber": 1,
  "operationTypeCode": "overallFeed",
  "operationDate": "2026-05-29 14:30:00",
  "launchDetailJson": "[{\"businessVarietyId\":\"material-id-001\",\"inventoryBatch\":\"IB20260501\",\"num\":\"10\",\"unitName\":\"kg\",\"varietyPackUnitId\":\"pack-unit-id\",\"validDate\":\"2027-05-01\",\"field0\":\"A\"}]",
  "codeContentsJson": "[{\"traceCode\":\"https://example/qrcode?...\",\"scanTimestamp\":\"1780045800000\",\"scanTime\":\"2026-05-29 14:30:00\"}]"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| relationInfoId | String | 是 | 生产批次主键 |
| batch | String | 是 | 生产批次号 |
| outputProductId | String | 是 | 产出品 id |
| serialNumber | Number | 是 | 当前投料锅次 |
| operationTypeCode | String | 是 | `overallFeed` 或 `premixFeed` |
| operationDate | String | 是 | PDA 提交时间，格式 `yyyy-MM-dd HH:mm:ss` |
| launchDetailJson | String | 是 | 投料明细数组序列化后的 JSON 字符串 |
| codeContentsJson | String | 是 | 本次提交涉及的二维码数组序列化后的 JSON 字符串 |

总混 `launchDetailJson` 反序列化后结构：

```json
[
  {
    "businessVarietyId": "material-id-001",
    "inventoryBatch": "IB20260501",
    "num": "10",
    "unitName": "kg",
    "varietyPackUnitId": "pack-unit-id",
    "validDate": "2027-05-01",
    "field0": "A"
  }
]
```

预混 `launchDetailJson` 反序列化后结构：

```json
[
  {
    "businessVarietyId": "child-material-id-001",
    "formulaWeight": "5"
  }
]
```

`codeContentsJson` 反序列化后结构：

```json
[
  {
    "traceCode": "https://example/qrcode?...",
    "scanTimestamp": "1780045800000",
    "scanTime": "2026-05-29 14:30:00"
  }
]
```

二维码时间字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| traceCode | String | 是 | 二维码原文 |
| scanTimestamp | String/Long | 是 | PDA 接收到扫码结果时的毫秒时间戳 |
| scanTime | String | 是 | PDA 接收到扫码结果时的可读时间 |

成功响应：

```json
{
  "code": "0",
  "errcode": 0,
  "message": "success",
  "errmsg": "success",
  "data": {
    "recordId": "record_001",
    "saved": true,
    "batch": "PB20260525002"
  }
}
```

## 7. 批量保存投料记录：直投称量

接口：

```http
POST /echain/thirdParty/mslJoint.do?action=batchSaveMaterialLaunchRecord&ecafeToken=${token}
Content-Type: application/x-www-form-urlencoded
```

请求 Body：

```json
{
  "relationInfoId": "batch-id-001",
  "batch": "PB20260525002",
  "outputProductId": "product-id-001",
  "operationTypeCode": "directFeed",
  "launchDetailJson": "[{\"serialNumber\":1,\"businessVarietyId\":\"material-id-001\",\"inventoryBatch\":\"IB20260501\",\"num\":\"10\",\"unitName\":\"kg\",\"varietyPackUnitId\":\"pack-unit-id\",\"validDate\":\"2027-05-01\"}]",
  "codeContentsJson": "[{\"traceCode\":\"https://example/qrcode?...\",\"scanTimestamp\":\"1780045800000\",\"scanTime\":\"2026-05-29 14:30:00\"}]"
}
```

字段说明：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| relationInfoId | String | 是 | 生产批次主键 |
| batch | String | 是 | 生产批次号 |
| outputProductId | String | 是 | 产出品 id |
| operationTypeCode | String | 是 | 固定为 `directFeed` |
| launchDetailJson | String | 是 | 批量称量明细数组序列化后的 JSON 字符串 |
| codeContentsJson | String | 是 | 本次提交涉及的二维码数组序列化后的 JSON 字符串 |

`launchDetailJson` 反序列化后结构：

```json
[
  {
    "serialNumber": 1,
    "businessVarietyId": "material-id-001",
    "inventoryBatch": "IB20260501",
    "num": "10",
    "unitName": "kg",
    "varietyPackUnitId": "pack-unit-id",
    "validDate": "2027-05-01"
  }
]
```

`codeContentsJson` 反序列化后结构：

```json
[
  {
    "traceCode": "https://example/qrcode?...",
    "scanTimestamp": "1780045800000",
    "scanTime": "2026-05-29 14:30:00"
  }
]
```

说明：

- PDA 会按 `物料 id + 库存批次` 聚合重量。
- `launchDetailJson` 只表示聚合后的投料业务明细，不再包含 `traceCode`、`scanTimestamp`、`scanTime`、`codeContents`。
- 二维码原文和扫码时间统一放在 `codeContentsJson` 中，ERP 如需判断二维码重复、保存扫码时间，应读取该字段。
- 如果同一物料同一库存批次由多次扫码组成，`launchDetailJson` 会聚合重量，`codeContentsJson` 会保留全部二维码记录。

成功响应：

```json
{
  "code": "0",
  "errcode": 0,
  "message": "success",
  "errmsg": "success",
  "data": {
    "recordId": "batch_record_001",
    "saved": true,
    "batch": "PB20260525002",
    "savedCount": 1
  }
}
```

## 8. operationTypeCode 约定

| 值 | 说明 | PDA 使用位置 |
| --- | --- | --- |
| overallFeed | 总混投料 | 批次列表、总混保存投料记录 |
| premixFeed | 预混投料 | 批次列表、预混明细、预混保存投料记录 |
| directFeed | 直投称量 | 批次列表、批量保存投料记录 |
| materialWeigh | 兼容称量流程 | 当前 PDA 可兼容跳转到直投称量 |

## 9. ERP 方实现要点

1. `checkExistCode` 必须读取 PDA 传入的 `traceCode`，不能只从其他字段取码。
2. `getFormulaDetail` 返回结构必须是 `data.id/data.batch/data.maxSerialNum/data.outputProduct/data.materialList`。
3. `saveMaterialLaunchRecord` 必须读取并保存 `codeContentsJson`，其中包含每个二维码的扫码时间。
4. `batchSaveMaterialLaunchRecord` 必须读取并保存 `codeContentsJson`，直投的 `launchDetailJson` 不再包含二维码原文和扫码时间。
5. 保存成功后，ERP 应更新该批次的 `maxSerialNum`，否则 PDA 下一次进入会重复显示旧锅次。
