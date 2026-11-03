#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# FuckClip -> Xposed-Modules-Repo 同步工具脚本
#
# 功能：
# 1. 验证目标 LSPosed 官方模块仓库权限与连通性。
# 2. 同步基础元数据文件（README.md, SUMMARY, SCOPE, SOURCE_URL）。
# 3. 按 LSPosed 规范自动发布格式为 <versionCode>-<versionName> 的 Release 并上传 APK。
# ==============================================================================

SOURCE_REPO="Cail-Gainey/FuckClip"
DEFAULT_TARGET_REPO="Xposed-Modules-Repo/io.github.cail_gainey.fuckclip"
TARGET_REPO="${1:-$DEFAULT_TARGET_REPO}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
LSP_DATA_DIR="$SCRIPT_DIR/lsposed-repo"

echo "=========================================================="
echo "  FuckClip -> LSPosed 官方模块仓库同步工具"
echo "  源仓库:   $SOURCE_REPO"
echo "  目标仓库: $TARGET_REPO"
echo "=========================================================="

# 1. 检查 GitHub CLI 与登录状态
if ! command -v gh &>/dev/null; then
  echo "[ERROR] 未检测到 GitHub CLI (gh)，请先安装: brew install gh"
  exit 1
fi

if ! gh auth status &>/dev/null; then
  echo "[ERROR] GitHub CLI 尚未登录，请先运行: gh auth login"
  exit 1
fi

# 2. 检查目标仓库是否存在且有权限访问
echo "[INFO] 正在检查目标仓库访问权限: $TARGET_REPO ..."
if ! gh repo view "$TARGET_REPO" &>/dev/null; then
  echo "[WARN] 无法访问仓库 $TARGET_REPO"
  echo "可能原因："
  echo "1. 该仓库尚未被 LSPosed 机器人创建或仍在审核中。"
  echo "   请检查提交申请 Issue: https://github.com/Xposed-Modules-Repo/submission"
  echo "2. 机器人已创建仓库，但你尚未在 GitHub 接受协作者/管理员邀请："
  echo "   请访问并接受邀请: https://github.com/$TARGET_REPO/invitations"
  exit 1
fi

echo "[SUCCESS] 成功连通目标仓库 $TARGET_REPO"

# 3. 同步元数据文件 (README.md, SUMMARY, SCOPE, SOURCE_URL)
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

echo "[INFO] 正在克隆目标仓库元数据..."
gh repo clone "$TARGET_REPO" "$TMP_DIR/target" -- --depth=1

echo "[INFO] 正在复制元数据配置文件..."
cp "$LSP_DATA_DIR/README.md" "$TMP_DIR/target/README.md"
cp "$LSP_DATA_DIR/SUMMARY" "$TMP_DIR/target/SUMMARY"
cp "$LSP_DATA_DIR/SCOPE" "$TMP_DIR/target/SCOPE"
cp "$LSP_DATA_DIR/SOURCE_URL" "$TMP_DIR/target/SOURCE_URL"

(
  cd "$TMP_DIR/target"
  git config user.name "$(git -C "$PROJECT_ROOT" config user.name || echo 'Cail-Gainey')"
  git config user.email "$(git -C "$PROJECT_ROOT" config user.email || echo 'gainey@users.noreply.github.com')"
  git add README.md SUMMARY SCOPE SOURCE_URL
  if git diff --staged --quiet; then
    echo "[INFO] 元数据文件无变动，无需提交变更。"
  else
    git commit -m "chore: sync module metadata, scope and documentation"
    echo "[INFO] 正在推送最新元数据至目标仓库..."
    git push origin HEAD
    echo "[SUCCESS] 元数据已成功同步。"
  fi
)

# 4. 获取源仓库最新 Release 信息
echo "[INFO] 正在解析源仓库 ($SOURCE_REPO) 最新 Release..."
LATEST_RELEASE_JSON=$(gh release view --repo "$SOURCE_REPO" --json tagName,name,body,assets)
LATEST_TAG=$(echo "$LATEST_RELEASE_JSON" | grep -o '"tagName":"[^"]*"' | head -1 | cut -d'"' -f4)
CLEAN_VERSION="${LATEST_TAG#v}"

# 计算 VersionCode (根据项目规则: Major * 10000 + Minor * 100 + Patch)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CLEAN_VERSION"
VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))
LSP_TAG="${VERSION_CODE}-${CLEAN_VERSION}"

echo "[INFO] 检测到源版本: $LATEST_TAG (versionName=$CLEAN_VERSION, versionCode=$VERSION_CODE)"
echo "[INFO] LSPosed 规范 Release Tag: $LSP_TAG"

# 5. 检查目标仓库是否已存在该 Release
if gh release view "$LSP_TAG" --repo "$TARGET_REPO" &>/dev/null; then
  echo "[INFO] 目标仓库已存在 Release $LSP_TAG，无需重复发布。"
else
  echo "[INFO] 正在从源仓库下载发布资产..."
  APK_DOWNLOAD_DIR="$TMP_DIR/apks"
  mkdir -p "$APK_DOWNLOAD_DIR"
  gh release download "$LATEST_TAG" --repo "$SOURCE_REPO" --dir "$APK_DOWNLOAD_DIR"

  echo "[INFO] 正在向目标仓库发布 Release ($LSP_TAG)..."
  gh release create "$LSP_TAG" "$APK_DOWNLOAD_DIR"/* \
    --repo "$TARGET_REPO" \
    --title "$CLEAN_VERSION" \
    --notes "Release for FuckClip $CLEAN_VERSION.
Source: https://github.com/$SOURCE_REPO/releases/tag/$LATEST_TAG"

  echo "[SUCCESS] 成功在 $TARGET_REPO 发布 Release: $LSP_TAG"
fi

echo ""
echo "=========================================================="
echo "同步流程已全部执行完毕。"
echo ""
echo "[重要提示]："
echo "请务必在网页浏览器中访问: https://github.com/$TARGET_REPO"
echo "点击右上角齿轮编辑仓库 Description，填写模块名称（如: FuckClip - 剪贴板安全防护模块）。"
echo "（LSPosed 机器人严格要求在浏览器手动设置 Description，缺失会导致客户端无法索引模块名）"
echo "=========================================================="
