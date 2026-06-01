# Java 代码总说明

本文档按包对 `app/src/main/java` 下的全部 Java 文件做集中说明，重点回答 4 个问题：

1. 这个类负责什么业务。
2. 它和哪些页面、网络接口、JSON 模型有关。
3. JSON 参数从哪里来、到哪里去。
4. 后续如果切换另一家 ERP，应该改哪一层。

## 1. 整体分层

当前 Java 代码可以分成 10 层：

1. `material`：页面 Activity 和应用入口。
2. `material/adapter`：RecyclerView 列表展示层。
3. `material/widget`：弹窗组件。
4. `material/util`：工具类、网络入口、SharedPreferences、字符串处理。
5. `material/data`：ERP 响应 DTO。
6. `material/entity`：页面使用的数据对象和提交对象。
7. `material/service`：当前 ERP 的 Retrofit 接口定义。
8. `material/erp`：统一 ERP 网关接口和错误工具。
9. `material/erp/current`：当前测试 ERP 的适配实现。
10. `material/erp/model`：统一业务模型，供页面层使用。

推荐理解顺序：

1. `LoginActivity -> MainActivity -> BatchListActivity`
2. `MaterialListActivity / PremixListActivity / PremixSubListActivity`
3. `ErpGateway -> CurrentErpGateway -> MaterialService`
4. `data / entity / erp/model`

## 2. 页面流转

### 2.1 登录与主页

- `ConfigServerActivity`
  作用：配置 ERP 服务器地址和端口。
  关键输入：`SERVER_HOST`、`SERVER_PORT`。
  关键输出：写入 SharedPreferences，重置 Retrofit 和 `ErpGateway`。

- `LoginActivity`
  作用：登录、校验 token 是否过期。
  关键输入：用户名、密码。
  关键输出：`token` 写入 `CacheData` 和 `SharedPreferences`。

- `MainActivity`
  作用：选择总混或预混业务入口。
  关键输入：无。
  关键输出：跳转到 `BatchListActivity`，并附带 `type=overallFeed` 或 `type=premixFeed`。

### 2.2 总混流程

1. `BatchListActivity` 查询批次。
2. 选择总混后跳到 `MaterialListActivity`。
3. `MaterialListActivity` 拉取物料列表。
4. `ScannerReceiverActivity` 打开扫码头。
5. 扫码后：
   - 本地解析二维码 URL。
   - 调 `ErpGateway.checkQrcode()` 走 ERP 校验。
   - 通过后累加到 `MaterialItem.inputAmount`。
6. 点击提交后：
   - 页面把 `MaterialResponseItem` 列表转成 `launchDetailJson`。
   - 把已扫码二维码列表转成 `codeContentsJson`。
   - 调 `ErpGateway.saveLaunchRecord()`。

### 2.3 预混流程

1. `BatchListActivity` 查询批次。
2. 选择预混后跳到 `PremixListActivity`。
3. `PremixListActivity` 从批次物料中筛出 `isPremix=true` 的预混项。
4. 点击某个预混项跳到 `PremixSubListActivity`。
5. `PremixSubListActivity` 拉取预混子配方明细。
6. 扫码后：
   - 本地解析二维码 URL。
   - 调 `ErpGateway.checkQrcode()` 走 ERP 校验。
   - 通过后累加到 `MaterialItem.inputAmount`。
7. 点击提交后：
   - 页面把 `PremixResponseItem` 列表转成 `launchDetailJson`。
   - 二维码列表转成 `codeContentsJson`。
   - 调 `ErpGateway.saveLaunchRecord()`。

## 3. 按包说明全部 Java 文件

### 3.1 `cn.starhelix.material`

#### `ConfigServerActivity.java`

作用：
- 让 PDA 录入 ERP 的 IP 和端口。
- 保存后重置网络层缓存。

关键参数：
- `ip`
- `port`

和 JSON 的关系：
- 不直接处理 JSON。
- 只影响后续所有接口请求的目标地址。

#### `LoginActivity.java`

作用：
- 调用统一网关登录。
- 应用启动时校验已缓存 token 是否仍有效。

关键数据：
- 输入：`username`、`password`
- 输出：`LoginResult.token`

JSON 来源：
- 通过 `ErpGateway.login()` 间接消费 `LoginResponse.data.token`

#### `MainActivity.java`

作用：
- 总混和预混入口页。
- 提供退出登录操作。

JSON 关系：
- 不直接处理 JSON。

#### `BatchListActivity.java`

作用：
- 查询总混/预混批次列表。
- 根据 `type` 决定跳转到总混页还是预混页。

关键参数：
- `type`：`overallFeed` / `premixFeed`
- `page`
- `rp`
- `batchId`
- `batchName`

JSON 来源：
- `ErpGateway.getBatchList()`
- 实际底层来自 `BatchListResponse.data.content`

#### `MaterialListActivity.java`

作用：
- 总混物料列表页。
- 扫码、累加重量、提交总混投料。

关键 JSON 输入：
- 批次列表接口中的 `materialList`、`maxSerialNum`、`outputProduct`

关键 JSON 输出：
- `launchDetailJson`
  每项对应 `MaterialResponseItem`
- `codeContentsJson`
  每项是 `{"traceCode":"二维码原文"}`

二维码解析字段：
- `businessVarietyId`
- `weight`
- `inventoryBatch`
- `validDate`
- `belongProduceBatch`
- `serialNumber`

#### `PremixListActivity.java`

作用：
- 预混物料列表页。
- 从批次物料中过滤出 `isPremix=true` 的项。

JSON 来源：
- `BatchListResponse.data.content[0].materialList`

#### `PremixSubListActivity.java`

作用：
- 预混子配方明细页。
- 扫码、累加重量、提交预混投料。

关键 JSON 输入：
- `PremixSubListResponse.data`

关键 JSON 输出：
- `launchDetailJson`
  每项对应 `PremixResponseItem`
- `codeContentsJson`

二维码解析字段：
- `businessVarietyId`
- `weight`
- `belongProduceBatch`
- `serialNumber`

#### `ChooseMixActivity.java`

作用：
- 旧版页面分流入口。
- 当前项目里作用较弱，更多是历史页面。

备注：
- 可以保留做兼容，也可以后续评估是否删除。

#### `ScannerReceiverActivity.java`

作用：
- PDA 扫码硬件基类。
- 负责打开扫码头、注册广播、接收扫码结果。

不处理 ERP JSON，只负责硬件层。

#### `LoadingWidgetActivity.java`

作用：
- 所有需要 loading 弹窗的 Activity 基类。

#### `MaterialApplication.java`

作用：
- 全局 Application。
- 统一管理登录失效后的跳转。
- 跟踪当前活跃 Activity 列表。

与 JSON 的关系：
- 不解析 JSON。
- 但 `CurrentErpGateway` 会在 `PERMISSION_DENY` 时调用它清理登录态。

#### `TestScannerReceiverActivity.java`

作用：
- 无 PDA 硬件时的调试基类。
- 当前项目中主要作为测试辅助或历史保留。

### 3.2 `cn.starhelix.material.adapter`

#### `OnItemClickListener.java`

作用：
- 列表点击回调接口。

#### `BatchListAdapter.java`

作用：
- 展示批次列表。
- 消费 `BatchItem`。

#### `MaterialListAdapter.java`

作用：
- 展示物料列表。
- 根据 `inputAmount` 是否在 `[netContentAllowGt, netContentAllowLt]` 区间内决定显示成功图标。

#### `PremixListAdapter.java`

作用：
- 展示预混物料列表。

#### `FlowListAdapter.java`

作用：
- 展示投料流转明细。

备注：
- 当前主流程里不是核心入口，更偏展示型历史组件。

### 3.3 `cn.starhelix.material.widget`

#### `CloseClickListener.java`

作用：
- 对话框关闭回调接口。

#### `LoadingDialog.java`

作用：
- 加载中弹窗。

#### `MessageDialog.java`

作用：
- 通用消息提示弹窗。

#### `ValidateSuccessDialog.java`

作用：
- 校验成功专用弹窗。

### 3.4 `cn.starhelix.material.util`

#### `HttpRequestUtil.java`

作用：
- 创建 Retrofit / OkHttpClient。
- 提供底层 GET / POST JSON 方法。

关键点：
- 这里只做通用网络传输。
- 不再写死当前 ERP 的 `code/message/data` 解析规则。

#### `ConvertUtil.java`

作用：
- Gson 序列化和反序列化工具。

重点：
- `toJson()` 用于把 `launchDetailJson`、`codeContentsJson` 转成字符串。

#### `PreferenceUtil.java`

作用：
- 读写 token、账号、服务器地址和端口。

#### `StrUtil.java`

作用：
- 字符串判空、数值判断、手机号判断。
- 解析二维码 URL 的 query 参数。

重点方法：
- `parseUrlQueryParams()`
  把二维码 URL 转成 `Map<String, String>`。

#### `DialogFragmentUtil.java`

作用：
- 统一弹出消息弹窗和成功弹窗。

#### `AppUtil.java`

作用：
- 获取版本号。

#### `SocketClient.java`

作用：
- 老的 Socket 通讯工具。

备注：
- 当前项目主流程已不再依赖它，属于历史保留。

### 3.5 `cn.starhelix.material.data`

#### `CommonResponse.java`

作用：
- ERP 通用响应包裹层。

字段：
- `code`：返回码
- `message`：返回信息

#### `LoginResponse.java`

作用：
- 登录响应 DTO。

JSON 结构：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "mock-token-001"
  }
}
```

#### `BatchListResponse.java`

作用：
- 批次列表响应 DTO。

JSON 重点：
- `data.content[]`

#### `PremixSubListResponse.java`

作用：
- 预混子配方响应 DTO。

#### `MapDataResponse.java`

作用：
- 承接 `data` 结构不固定的接口。

典型接口：
- `checkExistCode`
- `saveMaterialLaunchRecord`

#### `CacheData.java`

作用：
- 运行时内存缓存。
- 和 `PreferenceUtil` 配合管理 token、账号信息。

### 3.6 `cn.starhelix.material.entity`

#### `AccountBasicInfo.java`

作用：
- 账号基础信息。

#### `ApiException.java`

作用：
- ERP 业务异常封装。
- 网关层统一抛给页面处理。

#### `BatchItem.java`

作用：
- 批次列表页的简化展示模型。

#### `BatchListContent.java`

作用：
- 对应 `BatchListResponse.data.content[]` 单项。

#### `MaterialItem.java`

作用：
- 物料节点模型。

字段来源分两类：

1. ERP 原始 JSON：
   - `id`
   - `formulaWeight`
   - `netContentAllowLt`
   - `netContentAllowGt`
   - `name`
   - `unitName`
   - `isPremix`
   - `varietyPackUnitId`
   - `belongOutputProductId`
   - `materialFlag`
2. PDA 本地运行态：
   - `inputAmount`
   - `inventoryBatch`
   - `validDate`

#### `OutputProduct.java`

作用：
- 成品/产出信息。

#### `MaterialResponseItem.java`

作用：
- 总混提交时 `launchDetailJson` 数组中的单项。

#### `PremixResponseItem.java`

作用：
- 预混提交时 `launchDetailJson` 数组中的单项。

#### `PremixSubListData.java`

作用：
- 预混子列表详情 `data` 节点。

#### `FlowDetail.java`

作用：
- 流程明细展示对象，供 `FlowListAdapter` 使用。

#### `MaterialPutInRequest.java`

作用：
- 老版本总混提交请求模型。

备注：
- 当前主流程更偏向直接拼 `launchDetailJson` 字符串，属于历史兼容对象。

#### `MaterialListResponse.java`

作用：
- 老版本物料列表响应模型。

备注：
- 当前核心流程已转到 `BatchListResponse` / `PremixSubListResponse`。

#### `PremixSaveRequest.java`

作用：
- 老版本预混提交请求模型。

#### `PremixChildListResponse.java`

作用：
- 老版本预混子列表响应模型。

#### `ValidateResponse.java`

作用：
- 老版本扫码校验响应模型。

### 3.7 `cn.starhelix.material.service`

#### `MaterialService.java`

作用：
- 当前 ERP 的 Retrofit 接口定义。

接口与 JSON 参数：

##### `login()`
- `username`
- `password`

##### `getBatchList()`
- `ecafeToken`
- `id`
- `batch`
- `rp`
- `page`
- `operationTypeCode`
- `buildControls`

##### `getPremixSubList()`
- `ecafeToken`
- `businessVarietyId`
- `relationInfoId`
- `operationTypeCode`
- `buildControls`

##### `saveLaunchRecord()`
- `ecafeToken`
- `relationInfoId`
- `batch`
- `outputProductId`
- `serialNumber`
- `operationTypeCode`
- `operationDate`
- `launchDetailJson`
- `codeContentsJson`

##### `checkQrcode()`
- `ecafeToken`
- `traceCode`

### 3.8 `cn.starhelix.material.erp`

#### `ErpGateway.java`

作用：
- 页面层唯一应该依赖的 ERP 统一接口。

意义：
- 后续换 ERP 时，页面层不需要改 Retrofit、URL、DTO。

#### `ErpGatewayProvider.java`

作用：
- 提供当前 ERP 实现。
- 切服务器后可以 reset。

#### `ErpErrorUtil.java`

作用：
- 判断异常是不是 ERP 业务异常。
- 统一提取可展示错误信息。

### 3.9 `cn.starhelix.material.erp.current`

#### `CurrentErpGateway.java`

作用：
- 当前测试 ERP 的适配实现。

职责：
1. 调用 `MaterialService`
2. 校验 `CommonResponse.code`
3. 处理 `PERMISSION_DENY`
4. 把 DTO 映射成统一模型

这是未来切换另一家 ERP 时最关键的替换点。

### 3.10 `cn.starhelix.material.erp.model`

#### `LoginResult.java`

作用：
- 页面层统一登录结果。

#### `BatchInfo.java`

作用：
- 页面层统一批次模型。

#### `FormulaDetail.java`

作用：
- 页面层统一预混配方详情模型。

#### `ScanCheckResult.java`

作用：
- 页面层统一扫码校验结果。

#### `LaunchRecordResult.java`

作用：
- 页面层统一投料提交结果。

## 4. JSON 参数流向

### 4.1 扫码校验

来源：
- `ScannerReceiverActivity` 收到扫码字符串
- `MaterialListActivity` / `PremixSubListActivity` 解析二维码 URL

提交给 ERP：
- `traceCode`

ERP 返回：
- `exists`
- `code`
- `parsed`
- `batch`

页面实际使用：
- 当前主要依赖 `code == 0` 判断可继续
- 二维码业务字段主要还是本地再次解析 URL

### 4.2 总混提交

来源：
- `MaterialItem` 本地累加后的数据

组装方式：
- 页面构造 `List<MaterialResponseItem>`
- `ConvertUtil.toJson()` -> `launchDetailJson`

单项示意：
```json
{
  "businessVarietyId": "mat001",
  "inventoryBatch": "INV001",
  "num": "10",
  "unitName": "kg",
  "varietyPackUnitId": "unit001",
  "validDate": "2026-12-31",
  "field0": "flag"
}
```

二维码明细：
```json
[
  {
    "traceCode": "https://unify.id-cas.cn/lotinfo?... "
  }
]
```

### 4.3 预混提交

来源：
- `MaterialItem.inputAmount`

组装方式：
- 页面构造 `List<PremixResponseItem>`
- `ConvertUtil.toJson()` -> `launchDetailJson`

单项示意：
```json
{
  "businessVarietyId": "mat101",
  "formulaWeight": "2"
}
```

## 5. 如果要对接另一家 ERP

推荐只动以下位置：

1. 新增新的 Retrofit 接口，例如 `VendorBService`
2. 新增新的 DTO，例如 `vendorb/data/...`
3. 新增新的网关实现，例如 `VendorBErpGateway`
4. 在 `ErpGatewayProvider` 切换实现

尽量不要直接改这些页面：

- `LoginActivity`
- `BatchListActivity`
- `MaterialListActivity`
- `PremixListActivity`
- `PremixSubListActivity`

这些页面现在应该只消费统一模型和统一网关。

## 6. 历史/兼容类

以下类仍在代码中，但不是当前主流程最核心的调用路径：

- `ChooseMixActivity`
- `SocketClient`
- `FlowListAdapter`
- `MaterialPutInRequest`
- `MaterialListResponse`
- `PremixSaveRequest`
- `PremixChildListResponse`
- `ValidateResponse`

保留原因通常是：

1. 历史版本兼容
2. 备用实现
3. 后续可能还会复用

如果后续确认完全不用，可以再做一次“死代码清理”。
