# 安全策略 (Security Policy)

本项目高度重视安全性与用户隐私。FuckClip 作为工作在 Android `system_server` 系统服务层的核心挂钩模块，直接接触系统的剪贴板数据与跨进程 IPC 通信通道。

[English Version Below](#english-version)

---

## 支持的版本

当前仅对以下主版本提供安全更新与漏洞修复支持：

| 版本  | 是否支持安全更新 | 备注 |
|:----| :--- | :--- |
| 1.x | 支持 | 基于 Kotlin 与 LibXposed API 102 的全新现代架构 |

---

## 漏洞报告流程

如果您在 FuckClip 中发现了潜在的安全漏洞（包括但不限于：剪贴板数据泄露隐患、系统服务死锁风险、跨进程提权漏洞、白名单篡改绕过等），**请切勿通过公开的 GitHub Issue 直接发帖披露**。

请按照以下流程进行安全负责任的私密披露（Responsible Disclosure）：

1. **GitHub Security Advisory（推荐）**：
   - 访问本仓库的 `Security` 选项卡。
   - 点击 `Report a vulnerability` 发起私密安全通报。
   - 详细描述漏洞成因、影响范围、受影响的 Android / ROM 版本以及概念验证（PoC）或复现步骤。

2. **电子邮件联系**：
   - 若无法通过 GitHub 通报，请直接向仓库维护者发送加密或私密邮件。
   - 请在邮件标题中注明 `[SECURITY] FuckClip Vulnerability Report`。

### 响应与处理时效
- **初步确认**：我们将在收到漏洞报告后的 48 小时内确认报告内容并评估严重等级。
- **修复与发布**：在确认漏洞属实后，我们将在 7 至 14 个工作日内完成修复并在隔离分支进行测试验证。
- **公开致谢**：在包含修复补丁的新版本正式发布后，我们将协同报告者公开发布安全公告，并对负责任披露的贡献者予以致谢。

---

<span id="english-version"></span>

## English Version

Security and user privacy are foundational to FuckClip. As a module operating within the Android `system_server` process, it directly handles system clipboard transactions and inter-process communications (IPC).

### Supported Versions

Security updates and patches are actively provided for the following versions:

| Version | Supported | Notes |
|:--------| :--- | :--- |
| 1.x     | Yes | Kotlin & LibXposed API 102 modern architecture |

### Reporting a Vulnerability

If you discover a security vulnerability in FuckClip (such as data leakage risks, IPC privilege issues, or whitelist tampering), **please do not disclose it via public GitHub Issues**.

Please use one of the following methods for responsible disclosure:

1. **GitHub Private Vulnerability Reporting (Preferred)**:
   - Navigate to the `Security` tab of this repository.
   - Click `Report a vulnerability` to open a private disclosure.
   - Include vulnerability details, affected Android versions, proof of concept (PoC), and reproduction steps.

2. **Response Time**:
   - We will acknowledge receipt within 48 hours.
   - Fixes will be prepared, verified, and released within 7 to 14 days depending on severity.
   - A coordinated public security advisory will be published upon release, with proper attribution to the reporter.
