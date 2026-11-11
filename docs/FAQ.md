# 常见问题与排查指南 (FAQ & Troubleshooting)

本文档汇总了 FuckClip 使用过程中的常见问题与排查方案。

[English Version Below](#english-version)

---

## 目录
- [1. 模块激活与运行状态](#1-模块激活与运行状态)
- [2. 权限拦截与授权弹窗](#2-权限拦截与授权弹窗)
- [3. 毒瘤应用高频读取限制](#3-毒瘤应用高频读取限制)
- [4. 定制 ROM 与设备兼容性](#4-定制-rom-与设备兼容性)
- [5. 数据备份与恢复](#5-数据备份与恢复)

---

## 1. 模块激活与运行状态

### Q: 为什么 FuckClip 首页显示 "模块未激活"？
**A**:
1. 检查 Xposed 管理器（如 LSPosed、KernelSU、APatch）中是否已启用 FuckClip 模块。
2. **确认作用域勾选**：FuckClip 工作在系统服务进程中，必须在管理器中将 **"系统框架" (android / system_server)** 纳入模块的作用域。
3. 勾选作用域后，请执行**软重启系统**或**完整重启手机**，使 Hook 代码在 `system_server` 进程启动时完成注入。

---

## 2. 权限拦截与授权弹窗

### Q: 为什么某些应用读取剪贴板时没有弹出询问对话框？
**A**:
可能由以下几种情况导致：
1. **已有明确规则**：请在 "应用规则" 列表中检查该应用是否已被设置为 `允许 (Grant)` 或 `拒绝 (Deny)`。若已配置，系统将直接依据规则放行或拦截，不再重复弹窗。
2. **毒瘤限流触发**：若该应用在极短时间内连续数十次疯狂轮询读取剪贴板，会触发内置的毒瘤应用限流保护，多余请求将被直接丢弃静默拦截。
3. **系统前台应用免打扰**：若在设置中开启了相关免打扰策略，前台获得输入焦点的合法系统组件可能不会触发弹窗。

### Q: 为什么询问弹窗中的 "剪贴板内容预览" 有时显示为空？
**A**:
1. 当前系统剪贴板中确实没有复制任何数据（或刚被其他工具清空）。
2. 当前剪贴板存放的是非文本格式（如图像 URI、文件流或私有 Intent 结构）。为保障安全性与解析性能，对话框主要展示纯文本与富文本预览。

### Q: 授权弹窗如果长时间不点击，会导致系统卡死吗？
**A**:
不会。FuckClip 的 `:hook` 模块专门设计了**防死锁异步调度队列**与安全超时机制。如果超过指定等待时间未获得用户响应，模块会自动按照默认策略安全兜底返回，绝不会无限期挂起 `system_server` 的 Binder 线程池。

---

## 3. 毒瘤应用高频读取限制

### Q: 什么是 "毒瘤应用限流保护 (Rogue App Limiter)"？
**A**:
国内部分电商、社交类应用在进入前台、返回后台或锁屏唤醒时，常以数百毫秒的极高频率不间断轮询读取剪贴板，试图抓取用户的淘口令、分享链接或聊天痕迹。这不仅侵犯隐私，还会导致手机发热、掉电并频繁卡顿。

FuckClip 内置了滑动时间窗口限流器：
- 当同一应用在滑动窗口内的读取频次突破安全阈值时，限流器立即介入。
- 在后续的冷却时间内，对该应用的所有读取操作直接返回空数据，同时阻止弹窗频繁弹出打扰用户。

---

## 4. 定制 ROM 与设备兼容性

### Q: 那些系统的剪贴板拦截支持拦截？
**A**:
- MIUI 与澎湃 HyperOS 在 `miui-services.jar` / `services.jar` 中通过 `ClipboardServiceStubImpl` 扩展了剪贴板服务（包含带 `boolean userOperate` 参数的重载、跨进程唤醒路径 `checkProviderWakePathForClipboard` 及内部安全校验闸门）。FuckClip 内置了专门的 `MiuiClipboardHook` 引擎与防重入作用域，不仅完整拦截 MIUI / HyperOS 下的各类剪贴板读写与存在性探测，还能避免系统安全中心重复弹出二次授权框或误拦截用户已在 FuckClip 中放行的第三方输入法。
- Vivo 系统通过 `vivo-services.jar` 中的 `VivoClipboardServiceImpl`、`VivoSupClipboardServiceImpl` 以及 `ClipboardService` 内部的 `mVivoClipboardService` 委托处理超级剪贴板、跨屏协同与隐私权限检查。FuckClip 内置了 `OriginOsClipboardHook` 引擎，自动定位并挂钩 Vivo 剪贴板实现类及 `checkPolicyPerm`、`checkPrivacyAccessAllowed`、`showClipboardPrivacy` 校验闸门。
- Oplus 系固件通过 `oplus-services.jar` 中的 `ClipboardServiceExtImpl` 以及 `ClipboardService` 内部的 `mClipboardServiceExt` 委托实现智能防窥与剪贴板扩展。FuckClip 内置了 `ColorOsClipboardHook` 引擎，自动定位并挂钩 Oplus 扩展实现类及 `adjustClipData`、`hookClipboardAccessAllowed`、`showAccessNotification` 闸门。
- 三星 One UI 具有一套专有的侧屏剪贴板面板与服务（`SemClipboardService` / `ClipService`）。FuckClip 特别编写了针对三星专有服务的反射代理挂钩引擎，能够同等拦截三星系统下的剪贴板读取行为。

### Q: 支持哪些 Android 系统版本？
**A**:
FuckClip 基于 LibXposed API 102 现代规范构建，全面支持 **Android 10 至 Android 15**（API 级别 29 至 35）。对于早于 Android 10 的远古系统版本，建议使用旧版归档。

---

## 5. 数据备份与恢复

### Q: 如何迁移我的应用规则列表？
**A**:
1. 进入 FuckClip "设置" 页面，点击 "导出规则备份"。
2. 系统将生成标准的 `.json` 格式规则文件，可自由保存至本地存储或跨设备传输。
3. 在新设备或重刷系统后，在同一界面点击 "导入规则"，选择备份的 JSON 文件即可一键恢复，规则变更会立即热同步至拦截层，无需重启手机。

---

<span id="english-version"></span>

## English Version

### Frequently Asked Questions & Troubleshooting

#### 1. Module Activation
- **Why does the dashboard show "Module Inactive"?**
  - Verify FuckClip is enabled in your Xposed framework manager (LSPosed, KernelSU, or APatch).
  - Ensure the **System Framework (`android` / `system_server`)** is selected in the scope.
  - Reboot your device or restart the system server to load the hook.

#### 2. Interception & Dialogs
- **Why is there no prompt dialog when an app accesses the clipboard?**
  - Check whether the application already has a specific rule (`Grant` or `Deny`) in the Whitelist screen.
  - The application might have triggered the Rogue App Limiter cooldown due to rapid polling.
- **Will the prompt dialog freeze my phone if not clicked?**
  - No. FuckClip implements an asynchronous queue with safety timeouts to prevent Binder thread deadlock in `system_server`.

#### 3. Rogue App Limiter
- **What is the Rogue App Limiter?**
  - Certain aggressive apps query the clipboard continuously. The built-in sliding-window limiter blocks frequent polling, saving battery and preventing notification spam.

#### 4. Custom ROMs
- **Does it support Xiaomi MIUI and HyperOS?**
  - Yes. FuckClip includes `MiuiClipboardHook` targeting Xiaomi's `ClipboardServiceStubImpl`, `userOperate` method overloads, provider wake-path checks, and internal privacy gates.
- **Does it support Vivo OriginOS and Funtouch OS?**
  - Yes. FuckClip includes `OriginOsClipboardHook` targeting `VivoClipboardServiceImpl`, `VivoSupClipboardServiceImpl`, `mVivoClipboardService` delegates, and Vivo privacy policy gates (`checkPolicyPerm`, `checkPrivacyAccessAllowed`, `showClipboardPrivacy`).
- **Does it support OPPO ColorOS, OnePlus OxygenOS, and Realme UI?**
  - Yes. FuckClip includes `ColorOsClipboardHook` targeting `ClipboardServiceExtImpl`, `mClipboardServiceExt` delegates, and Oplus privacy/anti-peeping gates (`adjustClipData`, `hookClipboardAccessAllowed`, `showAccessNotification`).
- **Does it support Samsung One UI?**
  - Yes. FuckClip includes a dedicated hook for Samsung's proprietary `SemClipboardService` / `ClipService`.
- **Supported Android Versions**: Android 10 to Android 15 (API 29-35).

#### 5. Data Backup & Migration
- **How do I migrate my application rules to a new device?**
  1. Go to FuckClip "Settings" and tap "Export rule backup".
  2. The system generates a standard `.json` rule backup file that can be saved locally or transferred across devices.
  3. On the new device or after re-flashing, tap "Import rules" in the same screen and select the JSON backup file. Changes are immediately hot-synced to the hook layer without requiring a reboot.
