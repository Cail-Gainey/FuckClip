# 更新日志 (Changelog)

本项目所有重要更新与版本变动均记录于此文件中。
格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/) 规范，版本号遵循 [语义化版本 2.0.0](https://semver.org/lang/zh-CN/)。

[English Version Below](#english-version)

---

## [1.0.1] - 2026-11-11

### 新增 (Added)
- **小米 MIUI / 澎湃 HyperOS 专有剪贴板服务拦截 (`MiuiClipboardHook`)**：
  - 挂钩 `com.android.server.clipboard.ClipboardServiceStubImpl`、`ClipboardServiceStub` 与 `ClipboardServiceInjector`，支持 `userOperate` 等厂商扩展方法签名与专有读取入口。
  - 协调 `clipboardAccessResult`、`waitUserChoice`、`modifyClipDataIfNeed`、`checkProviderWakePathForClipboard` 与 `clipboardAccessAllowed` 内部权限闸门，拦截被拒绝应用的唤醒与读取，同时消除用户放行后的安全中心重复弹窗与第三方输入法误杀。
- **Vivo OriginOS / Funtouch OS 专有剪贴板服务拦截 (`OriginOsClipboardHook`)**：
  - 挂钩 `vivo-services.jar` 中的 `VivoClipboardServiceImpl`、`VivoSupClipboardServiceImpl` 及 `mVivoClipboardService` 委托实例，并探测 Vivo 专有剪贴板 Binder 服务。
  - 协调 `checkPolicyPerm`、`checkPrivacyAccessAllowed`、`checkClipboardAccess` 与 `showClipboardPrivacy` 权限与隐私弹窗闸门。
- **OPPO ColorOS / 一加 OxygenOS / Realme UI 专有剪贴板服务拦截 (`ColorOsClipboardHook`)**：
  - 挂钩 `oplus-services.jar` 中的 `ClipboardServiceExtImpl` 及 `mClipboardServiceExt` 扩展委托实例，并探测 Oplus 专有剪贴板 Binder 服务。
  - 协调 `adjustClipData`、`hookClipboardAccessAllowed`、`isAllowedToReadClipboard` 与 `showAccessNotification` 智能防窥及通知闸门。
- **剪贴板元数据防探测 (`ClipboardMetadataHook`)**：对规则设为「拒绝」的应用隐藏 `hasPrimaryClip`、`hasClipboardText` 与 `getPrimaryClipDescription` 探测结果。
- **重载委托防重入作用域 (`ClipboardAccessScope`)**：确保多参数重载相互调用时单次 IPC 仅执行一次规则判定与弹窗询问。

### 变更 (Changed)
- **Hook 模块解耦与拆分**：引入 `ClipboardHookContext` 与 `ClipboardSystemHook` 契约，并将 `VendorClassDiscovery`、`VendorGateSupport` 与各厂商拦截器拆分为独立子包文件。

### 计划中
- 引入更灵活的规则标签与分组搜索能力。

---

## [1.0.0] - 2026-11-03

本次版本为里程碑式的全新重构版本，从老旧单模块 Java 架构全面升级为模块化 Kotlin 现代架构。

### 新增 (Added)
- **多模块架构升级**：
  - `:core`：独立数据领域模型与限流算法逻辑。
  - `:hook`：基于 LibXposed API 102 现代规范的系统层挂钩实现。
  - `:app`：基于 Jetpack Compose 与 Material 3 构建的管理配置界面。
- **三态权限拦截机制**：
  - 支持对每个应用独立配置 `询问 (Request)`、`拒绝 (Deny)`、`允许 (Grant)` 三种权限状态。
  - 系统底层异步拦截未授权读取请求，并弹出可交互授权对话框。
  - 授权弹窗实时解析并展示剪贴板文本预览内容与请求应用信息。
- **毒瘤应用高频读取限制 (Rogue App Limiter)**：
  - 内置基于滑动时间窗口与频次阈值的自动限流引擎。
  - 防止恶意应用在后台死循环轮询剪贴板导致系统主线程卡死或耗电。
- **双引擎挂钩兼容**：
  - 原生 AOSP `ClipboardService` 拦截支持。
  - 三星定制系统 `SemClipboardService` / `ClipService` 专有服务拦截支持。
- **现代用户界面与动态主题**：
  - 采用 Material 3 设计语言与 Material You 动态色彩系统。
  - 提供多套调色风格（Tonal Spot、Neutral、Vibrant、Expressive、Monochrome）。
  - 支持暗色模式跟随系统或手动常开/常关。
- **审计日志与使用记录**：
  - 完整记录剪贴板读写历史，清晰区分读取（Read）与写入（Write）行为。
  - 提供详细的单次记录详情对话框，展示调用包名、时间戳及拦截处理结果。
- **规则备份与恢复**：
  - 支持将应用规则导出为标准 JSON 格式文件。
  - 支持从外部存储导入规则并热同步至 Hook 拦截层。
- **国际化与多语言**：
  - 完整覆盖 209 处多语言词条（英语、简体中文、繁体中文）。
  - 内置多语言自动化校验脚本 (`tools/check_translations.py`)。
- **自动化构建与发布流水线**：
  - GitHub Actions 标签推送自动触发 Release 构建。
  - 自动提取并汇总版本之间的 Pull Request 作为发布说明。
  - 仓库内置永久签名密钥，实现免配置自动签署发布版 APK。

### 变更 (Changed)
- 挂钩框架由过时的 Xposed API 82 升级至 LibXposed API 102，全面适配现代 Android 10 至 Android 15 系统。
- 数据存储机制由老旧 SQLite 升级为 Kotlin Coroutines + Flow 驱动的响应式存储。
- 替换老旧的 View 界面布局为声明式 Compose UI。

### 移除 (Removed)
- 移除不再适用的历史 Java 代码与旧式依赖库。
- 移除老旧的 XML 复杂嵌套界面与无用静态图标资源。

---

<span id="english-version"></span>

## English Version

All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### [1.0.1] - 2026-11-11

#### Added
- **Xiaomi MIUI / HyperOS Support (`MiuiClipboardHook`)**: Hooks `ClipboardServiceStubImpl`, `ClipboardServiceStub`, `ClipboardServiceInjector`, vendor read overloads, and internal access/wake-path/choice gates.
- **Vivo OriginOS / Funtouch OS Support (`OriginOsClipboardHook`)**: Hooks `VivoClipboardServiceImpl`, `VivoSupClipboardServiceImpl`, `mVivoClipboardService` delegates, Vivo Binder services, and privacy gates.
- **OPPO ColorOS / OnePlus OxygenOS / Realme UI Support (`ColorOsClipboardHook`)**: Hooks `ClipboardServiceExtImpl`, `mClipboardServiceExt` delegates, Oplus Binder services, `adjustClipData`, and access/notification gates.
- **Metadata Guard & Re-entrancy Scope**: Hides clipboard existence/metadata probes for denied packages via `ClipboardMetadataHook` and prevents duplicate prompting via `ClipboardAccessScope`.

#### Changed
- Decoupled `:hook` system installers and vendor gate interceptors through `ClipboardHookContext`, `ClipboardSystemHook`, `VendorClassDiscovery`, and `VendorGateSupport`.

### [1.0.0] - 2026-11-03

Major rewrite and modernization from legacy monolithic Java into a modular Kotlin architecture.

#### Added
- **Multi-Module Modern Architecture**: Split into `:core`, `:hook`, and `:app` modules.
- **LibXposed API 102**: Modern system-level hooking on Android 10 through Android 15.
- **Three-State Permission System**: Per-application configuration of Request, Deny, and Grant rules.
- **Real-Time Authorization Dialog**: Asynchronous system-level dialog with live clipboard content preview.
- **Rogue App Throttle Limiter**: Dynamic rate-limiting engine preventing aggressive clipboard polling.
- **Samsung ROM Support**: Dedicated hook integration for Samsung proprietary clipboard services.
- **Material 3 & Dynamic Theming**: Declarative Jetpack Compose interface with multiple palette styles.
- **Audit Logging**: Comprehensive clipboard read/write activity logging.
- **Rule Backup & Restore**: Standard JSON export and import capabilities.
- **Full Localization (i18n)**: English, Simplified Chinese, and Traditional Chinese support.
- **Automated CI/CD Release Pipeline**: Automatic Release creation on tag push with PR summaries and permanent APK signing.

#### Changed
- Upgraded compile target to SDK 35 (Android 15).
- Replaced legacy View system with Jetpack Compose.
- Replaced synchronous storage with Flow-driven repositories.

#### Removed
- Removed legacy Java codebase and obsolete API 82 dependencies.
