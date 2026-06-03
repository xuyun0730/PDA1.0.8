# PDA 界面控件使用与 JSON 接口说明

本文档按当前 PDA 工程的实际界面和控制流程整理。若存在同级页面或分支页面，使用 `序号_n` 表示，例如 `3_1`、`3_2`、`3_3`。

## 1. 登录界面

Activity：`LoginActivity`

Layout：`activity_login.xml`

用途：操作员输入账号密码，登录 ERP 并获取 token。

```json
{
  "screenNo": "1",
  "screenName": "登录界面",
  "activity": "LoginActivity",
  "controls": [
    {
      "id": "nameEditText",
      "name": "账号输入框",
      "usage": "输入 ERP/PDA 登录账号"
    },
    {
      "id": "passwordEditText",
      "name": "密码输入框",
      "usage": "输入登录密码"
    },
    {
      "id": "登录按钮",
      "method": "reqLogin",
      "usage": "调用 ERP 登录接口，成功后进入主菜单"
    },
    {
      "id": "configText",
      "name": "服务器配置",
      "usage": "进入服务器 IP 与端口配置界面"
    },
    {
      "id": "switchUserText",
      "name": "切换用户",
      "usage": "清空本地账号、token 与密码，重新输入账号"
    },
    {
      "id": "versionText",
      "name": "版本号",
      "usage": "显示当前 APP 版本"
    }
  ],
  "mainInterface": "generateToken4Msl"
}
```

## 1_1. 服务器配置界面

Activity：`ConfigServerActivity`

Layout：`activity_config_server.xml`

用途：配置 ERP/mock server 的 IP 和端口。

```json
{
  "screenNo": "1_1",
  "screenName": "服务器配置界面",
  "activity": "ConfigServerActivity",
  "controls": [
    {
      "id": "ipView",
      "name": "服务器 IP",
      "usage": "填写 ERP 或 mock server 的局域网 IP"
    },
    {
      "id": "portView",
      "name": "服务器端口",
      "usage": "填写 ERP 或 mock server 的端口，例如 3000"
    },
    {
      "id": "configBtn",
      "name": "保存配置",
      "usage": "校验 IP 和端口，保存到本地配置，并重置 Retrofit"
    },
    {
      "id": "返回按钮",
      "method": "finishActivity",
      "usage": "返回登录界面"
    }
  ],
  "localStorage": {
    "serverHost": "ipView",
    "serverPort": "portView"
  }
}
```

## 2. 主菜单界面

Activity：`MainActivity`

Layout：`activity_main.xml`

用途：选择投料业务类型。

```json
{
  "screenNo": "2",
  "screenName": "主菜单",
  "activity": "MainActivity",
  "controls": [
    {
      "id": "totalMixBtn",
      "name": "总混投料",
      "usage": "进入批次列表，type=overallFeed"
    },
    {
      "id": "directFeedBtn",
      "name": "直投投料",
      "usage": "进入批次列表，type=directFeed"
    },
    {
      "id": "premixBtn",
      "name": "预混投料",
      "usage": "进入批次列表，type=premixFeed"
    },
    {
      "id": "logoutBtn",
      "name": "退出登录",
      "usage": "清空 token 和账号信息，返回登录界面"
    }
  ]
}
```

## 3_1. 批次列表：总混入口

Activity：`BatchListActivity`

Layout：`activity_batch_list.xml`

进入参数：

```json
{
  "type": "overallFeed"
}
```

控件：

```json
{
  "screenNo": "3_1",
  "screenName": "总混批次列表",
  "activity": "BatchListActivity",
  "controls": [
    {
      "id": "searchView",
      "name": "批次搜索框",
      "usage": "输入批次号或关键字"
    },
    {
      "id": "searchBtn",
      "name": "搜索",
      "usage": "按关键字查询批次"
    },
    {
      "id": "refreshLayout",
      "name": "刷新/加载更多",
      "usage": "下拉刷新，上拉分页加载"
    },
    {
      "id": "recyclerView",
      "name": "批次列表",
      "usage": "显示 ERP 返回的生产批次"
    },
    {
      "id": "adapter_batch_list_item.topPanel",
      "name": "批次行",
      "usage": "点击后进入总混投料界面 MaterialListActivity"
    }
  ],
  "mainInterface": "getBatchList",
  "operationTypeCode": "overallFeed"
}
```

## 3_2. 批次列表：直投入口

Activity：`BatchListActivity`

Layout：`activity_batch_list.xml`

进入参数：

```json
{
  "type": "directFeed"
}
```

```json
{
  "screenNo": "3_2",
  "screenName": "直投批次列表",
  "sameLevelWith": "3_1",
  "rowClickResult": "进入 DirectFeedActivity",
  "mainInterface": "getBatchList",
  "operationTypeCode": "directFeed"
}
```

## 3_3. 批次列表：预混入口

Activity：`BatchListActivity`

Layout：`activity_batch_list.xml`

进入参数：

```json
{
  "type": "premixFeed"
}
```

```json
{
  "screenNo": "3_3",
  "screenName": "预混批次列表",
  "sameLevelWith": "3_1",
  "rowClickResult": "进入 PremixListActivity",
  "mainInterface": "getBatchList",
  "operationTypeCode": "premixFeed"
}
```

## 4_1. 总混投料界面

Activity：`MaterialListActivity`

Layout：`activity_material_list.xml`

用途：总混物料扫码、核对、批号查看、提交投料。

```json
{
  "screenNo": "4_1",
  "screenName": "总混投料界面",
  "activity": "MaterialListActivity",
  "controls": [
    {
      "id": "titleView",
      "name": "页面标题",
      "usage": "显示当前投料页面标题"
    },
    {
      "id": "serialNumView",
      "name": "锅次显示",
      "usage": "显示第几锅/共几锅"
    },
    {
      "id": "recyclerView",
      "name": "物料列表",
      "usage": "显示配方物料、已投重量、上下限、批号摘要"
    },
    {
      "id": "adapter_material_list_item.topPanel",
      "name": "物料行",
      "usage": "点击后弹出已扫批号选择"
    },
    {
      "id": "batchSummaryView",
      "name": "已扫批号摘要",
      "usage": "显示库存批号与重量摘要，例如 INV001 10kg"
    },
    {
      "id": "submitBtn",
      "name": "提交",
      "usage": "提交前弹出总核对，确认后保存投料记录"
    }
  ],
  "scanGuard": [
    "duplicate traceCode check",
    "businessVarietyId in current formula check",
    "belongProduceBatch check",
    "serialNumber check",
    "validDate expiry check",
    "overweight check"
  ],
  "interfaces": {
    "query": "getBatchList",
    "scanCheck": "checkExistCode",
    "submit": "saveMaterialLaunchRecord"
  }
}
```

## 4_2. 直投投料界面

Activity：`DirectFeedActivity`

Layout：复用 `activity_material_list.xml`

用途：直投物料扫码、核对、批号查看、批量提交。

```json
{
  "screenNo": "4_2",
  "screenName": "直投投料界面",
  "activity": "DirectFeedActivity",
  "sameLayoutWith": "4_1",
  "differences": [
    "标题显示为直投称量列表",
    "提交接口使用 batchSaveMaterialLaunchRecord",
    "launchDetailJson 每条明细带 serialNumber"
  ],
  "controls": [
    "serialNumView",
    "recyclerView",
    "batchSummaryView",
    "submitBtn",
    "adapter_material_list_item.topPanel"
  ],
  "interfaces": {
    "query": "getBatchList",
    "scanCheck": "checkExistCode",
    "submit": "batchSaveMaterialLaunchRecord"
  }
}
```

## 4_3. 预混列表界面

Activity：`PremixListActivity`

Layout：`activity_premix_list.xml`

用途：展示当前批次下的预混料，选择后进入预混子料投料。

```json
{
  "screenNo": "4_3",
  "screenName": "预混列表界面",
  "activity": "PremixListActivity",
  "controls": [
    {
      "id": "refreshLayout",
      "name": "刷新",
      "usage": "下拉刷新预混料列表"
    },
    {
      "id": "recyclerView",
      "name": "预混料列表",
      "usage": "只显示 isPremix=true 的物料"
    },
    {
      "id": "adapter_premix_list_item.topPanel",
      "name": "预混料行",
      "usage": "点击进入预混子料投料界面"
    }
  ],
  "mainInterface": "getBatchList",
  "operationTypeCode": "premixFeed"
}
```

## 5. 预混子料投料界面

Activity：`PremixSubListActivity`

Layout：`activity_premix_sub_list.xml`

用途：预混子配方物料扫码、核对、提交。

```json
{
  "screenNo": "5",
  "screenName": "预混子料投料界面",
  "activity": "PremixSubListActivity",
  "controls": [
    {
      "id": "serialNumView",
      "name": "锅次显示",
      "usage": "显示当前预混锅次"
    },
    {
      "id": "recyclerView",
      "name": "子物料列表",
      "usage": "显示预混子配方物料"
    },
    {
      "id": "adapter_material_list_item.topPanel",
      "name": "子物料行",
      "usage": "点击查看该物料已投重量和上下限"
    },
    {
      "id": "submitBtn",
      "name": "提交",
      "usage": "提交前总核对，确认后保存"
    }
  ],
  "interfaces": {
    "query": "getFormulaDetail",
    "scanCheck": "checkExistCode",
    "submit": "saveMaterialLaunchRecord"
  }
}
```

## 6_1. 扫码核对弹窗

Dialog：`ScanConfirmDialog`

Layout：`fragment_scan_confirm_dialog.xml`

触发：扫码成功后自动弹出。

```json
{
  "screenNo": "6_1",
  "screenName": "扫码核对弹窗",
  "dialog": "ScanConfirmDialog",
  "controls": [
    {
      "id": "scanDetailTextView",
      "name": "扫码明细",
      "usage": "显示二维码字段、本地核对结果、物料、重量、批号、生产日期、有效期"
    },
    {
      "id": "confirmBtn",
      "name": "确定",
      "usage": "继续 ERP checkExistCode 校验并入账"
    },
    {
      "id": "damageBtn",
      "name": "报损坏",
      "usage": "本次扫码不入账，不提交投料记录"
    }
  ]
}
```

## 6_2. 批号选择弹窗

触发页面：`MaterialListActivity`、`DirectFeedActivity`

触发方式：点击已投物料行。

```json
{
  "screenNo": "6_2",
  "screenName": "批号选择弹窗",
  "trigger": "点击已投物料行",
  "displayFields": [
    "inventoryBatch",
    "weight",
    "unitName",
    "productionDate",
    "validDate"
  ],
  "usage": "查看该物料已扫库存批号和日期明细"
}
```

## 6_3. 提交前核对弹窗

触发页面：总混、直投、预混子料。

触发方式：点击 `submitBtn`。

```json
{
  "screenNo": "6_3",
  "screenName": "提交前核对弹窗",
  "trigger": "点击提交按钮",
  "displayFields": [
    "currentSerialNumber",
    "materialName",
    "plannedRange",
    "inputAmount",
    "status",
    "batchSummary"
  ],
  "buttons": [
    {
      "name": "取消",
      "usage": "返回页面继续扫码"
    },
    {
      "name": "确定提交",
      "usage": "调用 ERP 保存投料记录"
    }
  ]
}
```

## 6_4. 通用弹窗

Dialog：`MessageDialog`、`ValidateSuccessDialog`、`LoadingDialog`

```json
{
  "screenNo": "6_4",
  "screenName": "通用弹窗",
  "dialogs": [
    {
      "name": "MessageDialog",
      "usage": "显示错误、提示、批号详情"
    },
    {
      "name": "ValidateSuccessDialog",
      "usage": "显示提交成功"
    },
    {
      "name": "LoadingDialog",
      "usage": "登录、查询、提交过程中的加载中状态"
    }
  ]
}
```

## 7. 总体控制流

```json
{
  "flow": [
    "1 登录界面",
    "1_1 服务器配置界面",
    "2 主菜单",
    "3_1 总混批次列表",
    "3_2 直投批次列表",
    "3_3 预混批次列表",
    "4_1 总混投料界面",
    "4_2 直投投料界面",
    "4_3 预混列表界面",
    "5 预混子料投料界面",
    "6_1 扫码核对弹窗",
    "6_2 批号选择弹窗",
    "6_3 提交前核对弹窗",
    "6_4 通用弹窗"
  ]
}
```

## 8. JSON Interface Field Notes

本节用于给 ERP 或接口对接人员查看。`englishNote` 是英文字段备注；这些 JSON 是字段说明，不是完整请求体。

### 8.1 Common Request Fields

```json
{
  "action": {
    "englishNote": "ERP action name. It determines which ERP operation will be executed.",
    "example": "getBatchList"
  },
  "ecafeToken": {
    "englishNote": "Authentication token returned by the login interface.",
    "example": "mock-token-001"
  },
  "token": {
    "englishNote": "Alternative token field accepted for compatibility.",
    "example": "mock-token-001"
  },
  "relationInfoId": {
    "englishNote": "ERP batch relation ID. PDA uses it as the current production batch identifier.",
    "example": "batch001"
  },
  "batch": {
    "englishNote": "Production batch number displayed and selected by the PDA user.",
    "example": "PB20260525001"
  },
  "operationTypeCode": {
    "englishNote": "Operation type code. Used to distinguish overall feed, direct feed, and premix feed.",
    "example": "overallFeed"
  },
  "serialNumber": {
    "englishNote": "Current kettle/work sequence number. Used to restore and submit the correct operation round.",
    "example": "1"
  }
}
```

### 8.2 generateToken4Msl

```json
{
  "username": {
    "englishNote": "PDA login username.",
    "example": "pda"
  },
  "password": {
    "englishNote": "PDA login password.",
    "example": "123456"
  },
  "tenantName": {
    "englishNote": "Tenant code required by the ERP login endpoint.",
    "example": "mslscsyxt"
  },
  "data.token": {
    "englishNote": "Token returned by ERP. PDA must store it and send it in later requests.",
    "example": "mock-token-001"
  }
}
```

### 8.3 getBatchList

```json
{
  "rp": {
    "englishNote": "Page size.",
    "example": 15
  },
  "page": {
    "englishNote": "Page number.",
    "example": 1
  },
  "buildControls": {
    "englishNote": "Optional ERP control flag. PDA uses needBuildMaxSerialNum when it needs current serial recovery.",
    "example": "needBuildMaxSerialNum"
  },
  "data.content[].id": {
    "englishNote": "Batch relation ID used as relationInfoId in later PDA requests.",
    "example": "batch001"
  },
  "data.content[].batch": {
    "englishNote": "Production batch number.",
    "example": "PB20260525001"
  },
  "data.content[].maxSerialNum": {
    "englishNote": "Maximum completed serial number returned by ERP.",
    "example": "0"
  },
  "data.content[].outputProduct": {
    "englishNote": "Output product information for the selected production batch."
  },
  "data.content[].materialList": {
    "englishNote": "Material formula list displayed by PDA."
  }
}
```

### 8.4 getFormulaDetail

```json
{
  "businessVarietyId": {
    "englishNote": "Premix material ID selected from the premix list.",
    "example": "premix001"
  },
  "relationInfoId": {
    "englishNote": "Current production batch relation ID.",
    "example": "batch001"
  },
  "data.materialList": {
    "englishNote": "Premix child material list. PDA restores scanned state from inputAmount, inventoryBatch, productionDate, and validDate."
  },
  "data.maxSerialNum": {
    "englishNote": "Maximum completed serial number for this premix operation."
  }
}
```

### 8.5 traceCode QR Content

```json
{
  "businessVarietyId": {
    "englishNote": "Material ID encoded in the QR code. It must exist in the current PDA formula list.",
    "example": "mat001"
  },
  "weight": {
    "englishNote": "Scanned material weight.",
    "example": "10"
  },
  "unitName": {
    "englishNote": "Weight unit.",
    "example": "kg"
  },
  "inventoryBatch": {
    "englishNote": "Inventory batch number from the QR code.",
    "example": "INV001"
  },
  "productionDate": {
    "englishNote": "Production date of the scanned inventory batch.",
    "example": "2026-01-01"
  },
  "produceDate": {
    "englishNote": "Compatible alias of productionDate.",
    "example": "2026-01-01"
  },
  "validDate": {
    "englishNote": "Expiration date of the scanned inventory batch.",
    "example": "2026-12-31"
  },
  "belongProduceBatch": {
    "englishNote": "Production batch number encoded in the QR code. PDA checks it against the current batch.",
    "example": "PB20260525001"
  },
  "serialNumber": {
    "englishNote": "Work serial number encoded in the QR code. PDA checks it against the current serial number.",
    "example": "1"
  }
}
```

### 8.6 checkExistCode

```json
{
  "traceCode": {
    "englishNote": "Full original QR code content. PDA should send the complete string instead of manually rebuilding fields.",
    "example": "https://unify.id-cas.cn/lotinfo?businessVarietyId=mat001&weight=10&unitName=kg"
  },
  "code": {
    "englishNote": "Compatible alias of traceCode."
  },
  "qrCode": {
    "englishNote": "Compatible alias of traceCode."
  },
  "barcode": {
    "englishNote": "Compatible alias of traceCode."
  },
  "data.exists": {
    "englishNote": "Whether the QR code has already been used."
  },
  "data.parsed": {
    "englishNote": "Parsed QR code fields returned by ERP/mock server."
  }
}
```

### 8.7 Material Item Returned to PDA

```json
{
  "id": {
    "englishNote": "Material ID. It must match businessVarietyId from the QR code.",
    "example": "mat001"
  },
  "name": {
    "englishNote": "Material display name.",
    "example": "Corn"
  },
  "unitName": {
    "englishNote": "Material unit name.",
    "example": "kg"
  },
  "formulaWeight": {
    "englishNote": "Planned formula weight if ERP provides it."
  },
  "netContentAllowGt": {
    "englishNote": "Minimum allowed input weight.",
    "example": "9.5"
  },
  "netContentAllowLt": {
    "englishNote": "Maximum allowed input weight.",
    "example": "10.5"
  },
  "isPremix": {
    "englishNote": "Whether this material is a premix material.",
    "example": false
  },
  "varietyPackUnitId": {
    "englishNote": "Package unit ID returned to ERP during launch submission."
  },
  "belongOutputProductId": {
    "englishNote": "Output product relation ID. PDA uses it to distinguish direct material and premix-owned material."
  },
  "materialFlag": {
    "englishNote": "ERP extension flag. PDA sends it back as field0."
  },
  "inputAmount": {
    "englishNote": "Already scanned/input weight restored from ERP.",
    "example": "10"
  },
  "inventoryBatch": {
    "englishNote": "Map of inventory batch number to accumulated scanned weight.",
    "example": {
      "INV001": "10"
    }
  },
  "productionDate": {
    "englishNote": "Map of inventory batch number to production date.",
    "example": {
      "INV001": "2026-01-01"
    }
  },
  "validDate": {
    "englishNote": "Map of inventory batch number to expiration date.",
    "example": {
      "INV001": "2026-12-31"
    }
  }
}
```

### 8.8 launchDetailJson Item

```json
{
  "serialNumber": {
    "englishNote": "Current work serial number. Required for batchSaveMaterialLaunchRecord.",
    "example": 1
  },
  "businessVarietyId": {
    "englishNote": "Material ID to be launched.",
    "example": "mat001"
  },
  "inventoryBatch": {
    "englishNote": "Inventory batch number accumulated by PDA.",
    "example": "INV001"
  },
  "num": {
    "englishNote": "Input weight for the material and inventory batch.",
    "example": "10"
  },
  "unitName": {
    "englishNote": "Weight unit.",
    "example": "kg"
  },
  "varietyPackUnitId": {
    "englishNote": "Package unit ID from the material formula.",
    "example": "unit001"
  },
  "productionDate": {
    "englishNote": "Production date of the inventory batch.",
    "example": "2026-01-01"
  },
  "validDate": {
    "englishNote": "Expiration date of the inventory batch.",
    "example": "2026-12-31"
  },
  "field0": {
    "englishNote": "ERP extension field. PDA maps MaterialItem.materialFlag to this field."
  }
}
```

### 8.9 codeContentsJson Item

```json
{
  "traceCode": {
    "englishNote": "Full original scanned QR code content.",
    "example": "https://unify.id-cas.cn/lotinfo?businessVarietyId=mat001&weight=10"
  },
  "scanTimestamp": {
    "englishNote": "PDA scan timestamp in milliseconds.",
    "example": "1780381200000"
  },
  "scanTime": {
    "englishNote": "PDA scan time formatted for ERP logs.",
    "example": "2026-06-02 09:00:00"
  }
}
```

### 8.10 saveMaterialLaunchRecord

```json
{
  "outputProductId": {
    "englishNote": "Output product ID of the selected production batch.",
    "example": "prod001"
  },
  "operationDate": {
    "englishNote": "PDA operation time.",
    "example": "2026-06-02 09:00:00"
  },
  "launchDetailJson": {
    "englishNote": "JSON string array of material launch detail items."
  },
  "codeContentsJson": {
    "englishNote": "JSON string array of scanned QR code records."
  },
  "data.recordId": {
    "englishNote": "ERP saved launch record ID."
  },
  "data.receiptId": {
    "englishNote": "ERP receipt ID used for PDA acknowledgement."
  },
  "data.handshakeStatus": {
    "englishNote": "ERP receipt handshake status.",
    "example": "RECEIVED"
  }
}
```

### 8.11 batchSaveMaterialLaunchRecord

```json
{
  "launchDetailJson": {
    "englishNote": "JSON string array of batch material launch items. Each item should include serialNumber."
  },
  "codeContentsJson": {
    "englishNote": "JSON string array of scanned QR code records."
  },
  "data.savedCount": {
    "englishNote": "Number of detail items saved by ERP."
  },
  "data.recordId": {
    "englishNote": "ERP saved batch launch record ID."
  },
  "data.receiptId": {
    "englishNote": "ERP receipt ID used for PDA acknowledgement."
  }
}
```

### 8.12 confirmReceipt

```json
{
  "receiptId": {
    "englishNote": "Receipt ID returned by ERP after saveMaterialLaunchRecord or batchSaveMaterialLaunchRecord.",
    "example": "receipt_20260602_000001"
  },
  "clientStatus": {
    "englishNote": "PDA acknowledgement status.",
    "example": "ACKED"
  }
}
```

