## 变更概述 (Summary of Changes)

请简述本次 Pull Request 的核心变更内容与目的。

---

## 变更类型 (Type of Change)

请勾选适用的项目：
- [ ] `feat`: 新增功能特性
- [ ] `fix`: Bug 修复
- [ ] `refactor`: 代码重构（不改变外部功能的架构或文件拆分）
- [ ] `perf`: 性能优化
- [ ] `i18n`: 国际化翻译补全或修订
- [ ] `docs`: 文档变动
- [ ] `chore`: 构建系统、依赖更新或工作流配置调整

---

## 涉及模块 (Affected Modules)

- [ ] `:core` (通用数据模型、限流器与常量)
- [ ] `:hook` (LibXposed API 102 系统服务拦截与弹窗)
- [ ] `:app` (Jetpack Compose 用户界面与设置)
- [ ] `build / CI` (Gradle、GitHub Actions 流水线)
- [ ] `docs` (文档及多语言词条)

---

## 关联 Issue (Related Issues)

- Closes #
- Relates to #

---

## 贡献合规自检清单 (Checklist)

提交 PR 前请务必确认满足以下规范：
- [ ] 已仔细阅读并遵守 [CONTRIBUTING.md](../docs/CONTRIBUTING.md) 贡献指南。
- [ ] **文件长度限制**：所有新增或修改的 Kotlin 文件行数均严格控制在 500 行以内。
- [ ] **双语 KDoc 注释**：所有新增的 `public` 与 `internal` 顶层成员均配备规范的中英双语 KDoc。
- [ ] **无 Emoji**：代码、字符串资源、提交说明中均无任何 Emoji 字符。
- [ ] **多语言检查通过**：本地执行 `python3 tools/check_translations.py` 输出通过。
- [ ] **单元测试通过**：本地执行 `./gradlew test` 全部通过。
- [ ] **编译打包通过**：本地执行 `./gradlew assembleRelease` 顺利生成发布包。
