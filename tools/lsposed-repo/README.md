# FuckClip

> 新一代现代化 Android 剪贴板安全防护与隐私拦截 Xposed 模块。
> A modern, powerful clipboard privacy & protection Xposed module for Android.

[![Source](https://img.shields.io/badge/Source-Cail--Gainey%2FFuckClip-blue?logo=github)](https://github.com/Cail-Gainey/FuckClip)
[![API](https://img.shields.io/badge/Android-10%20--%2015%20(API%2029%2B)-green.svg)](https://developer.android.com)
[![LibXposed](https://img.shields.io/badge/LibXposed-API%20102-purple.svg)](https://github.com/libxposed)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://github.com/Cail-Gainey/FuckClip/blob/master/LICENSE)

FuckClip 是一款基于 Kotlin 2.0、Jetpack Compose、Material 3 与 [LibXposed](https://github.com/libxposed)（API 102）构建的剪贴板隐私防护模块。

模块直接在 Android 系统服务级进程（`system_server`）中对系统剪贴板服务（`IClipboard`，包含原生 Android 与三星专有剪贴板服务）的 `setPrimaryClip()` 与 `getPrimaryClip()` 进行动态代理与拦截，帮助用户彻底杜绝各类应用未经授权静默扫描、窃取或污染剪贴板的行为。

---

## 核心特性

- **读写双向独立控制**：写入拦截（防剪贴板污染与淘口令暗桩）、读取拦截（防隐私刺探与口令嗅探）。
- **三种策略精细管理**：询问 (Ask)、允许 (Allow)、拒绝 (Deny)，支持按应用独立记忆。
- **流氓应用自适应防死循环引擎**：频控冷却 (Deny Cooldown)、授权猝发窗口 (Grant Burst Window)、高频访问降频。
- **隐私审计日志**：完整记录访问应用图标、时间、读写流向、文本内容预览，支持一键清空与筛选。
- **系统级原生询问弹窗**：在 `system_server` 端直接渲染极具美感的系统悬浮授权弹窗，支持全量多语言。
- **Dynamic Material You**：原生支持 Monet 壁纸取色、5 款内置色彩方案、纯黑 AMOLED 深色模式。

---

## 作用域与运行环境

- **Xposed 作用域**：`android`（系统框架 / System Framework，即 `system_server`）
- **推荐框架**：LSPosed / LSPosed_mod / Vector 等现代 API 102 兼容框架
- **系统要求**：Android 10 至 Android 15（API 29+），全面兼容 AOSP 与三星 One UI

---

## 源代码与问题反馈

完整源代码、开发文档、更新日志及问题反馈请访问主仓库：
  **https://github.com/Cail-Gainey/FuckClip**

- [更新日志 (CHANGELOG.md)](https://github.com/Cail-Gainey/FuckClip/blob/master/docs/CHANGELOG.md)
- [常见问题与使用指南 (FAQ.md)](https://github.com/Cail-Gainey/FuckClip/blob/master/docs/FAQ.md)
- [架构设计 (ARCHITECTURE.md)](https://github.com/Cail-Gainey/FuckClip/blob/master/docs/ARCHITECTURE.md)
