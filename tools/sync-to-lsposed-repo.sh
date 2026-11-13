#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# FuckClip -> Xposed-Modules-Repo 同步工具脚本
#
# 功能：
# 1. sync: 自动发布格式为 <versionCode>-<versionName> 的 Release 并上传 APK，同步元数据。
# 2. delete: 自动从 LSPosed 仓库删除指定版本的 Release 与 Tag。
# 3. metadata: 仅同步基础元数据文件 (README.md, SUMMARY, SCOPE, SOURCE_URL)。
# ==============================================================================

SOURCE_REPO="${SOURCE_REPO:-Cail-Gainey/FuckClip}"
DEFAULT_TARGET_REPO="Xposed-Modules-Repo/io.github.cail_gainey.fuckclip"
TARGET_REPO="${TARGET_REPO:-$DEFAULT_TARGET_REPO}"
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
  echo "[ERROR] GitHub CLI 尚未登录，请先运行: gh auth login 或设置 GH_TOKEN"
  exit 1
fi

# 2. 检查目标仓库访问权限
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

# 计算 LSPosed Tag 格式: <versionCode>-<versionName>
calc_lsp_tag() {
  local TAG="$1"
  if echo "$TAG" | grep -Eq "^[0-9]+-"; then
    echo "$TAG"
    return 0
  fi
  local CLEAN_TAG="${TAG#v}"
  local VERSION_NAME
  VERSION_NAME="$(echo "$CLEAN_TAG" | grep -Eo "^[0-9]+\.[0-9]+\.[0-9]+" || echo "")"
  if [ -z "$VERSION_NAME" ]; then
    echo ""
    return 1
  fi
  IFS="." read -r MAJOR MINOR PATCH <<< "$VERSION_NAME"
  local VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))
  echo "${VERSION_CODE}-${CLEAN_TAG}"
}

# 同步元数据文件
sync_metadata() {
  echo "[INFO] 正在同步元数据文件 (README.md, SUMMARY, SCOPE, SOURCE_URL)..."
  local TMP_DIR
  TMP_DIR="$(mktemp -d)"

  gh repo clone "$TARGET_REPO" "$TMP_DIR/target" -- --depth=1

  cp "$LSP_DATA_DIR/README.md" "$TMP_DIR/target/README.md"
  cp "$LSP_DATA_DIR/SUMMARY" "$TMP_DIR/target/SUMMARY"
  cp "$LSP_DATA_DIR/SCOPE" "$TMP_DIR/target/SCOPE"
  cp "$LSP_DATA_DIR/SOURCE_URL" "$TMP_DIR/target/SOURCE_URL"

  (
    cd "$TMP_DIR/target"
    git config user.name "$(git -C "$PROJECT_ROOT" config user.name || echo 'Cail-Gainey')"
    git config user.email "$(git -C "$PROJECT_ROOT" config user.email || echo 'gainey@users.noreply.github.com')"

    local AUTH_TOKEN
    AUTH_TOKEN="${GH_TOKEN:-${GITHUB_TOKEN:-$(gh auth token 2>/dev/null || echo '')}}"
    if [ -n "$AUTH_TOKEN" ]; then
      git remote set-url origin "https://x-access-token:${AUTH_TOKEN}@github.com/${TARGET_REPO}.git"
    fi

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
  rm -rf "$TMP_DIR"
}

# 执行发布同步
do_sync() {
  local TAG="${1:-}"
  local LOCAL_APK_DIR="${2:-}"

  sync_metadata

  if [ -z "$TAG" ]; then
    echo "[INFO] 未指定 Release Tag，正在获取源仓库 ($SOURCE_REPO) 最新 Release..."
    local LATEST_RELEASE_JSON
    LATEST_RELEASE_JSON=$(gh release view --repo "$SOURCE_REPO" --json tagName,name,body,assets)
    TAG=$(echo "$LATEST_RELEASE_JSON" | grep -o '"tagName":"[^"]*"' | head -1 | cut -d'"' -f4)
  fi

  if [ -z "$TAG" ]; then
    echo "[ERROR] 未能解析到有效的 Release Tag。"
    exit 1
  fi

  local LSP_TAG
  LSP_TAG=$(calc_lsp_tag "$TAG")
  if [ -z "$LSP_TAG" ]; then
    echo "[ERROR] 无法将 Tag '$TAG' 解析为 LSPosed 规范版本号 (<versionCode>-<versionName>)"
    exit 1
  fi

  local CLEAN_VERSION="${TAG#v}"
  if echo "$TAG" | grep -Eq "^[0-9]+-"; then
    CLEAN_VERSION="${TAG#*-}"
  fi

  echo "[INFO] 源版本 Tag: $TAG"
  echo "[INFO] LSPosed 规范 Release Tag: $LSP_TAG"

  # 检查目标仓库是否已存在该 Release
  if gh release view "$LSP_TAG" --repo "$TARGET_REPO" &>/dev/null; then
    echo "[INFO] 目标仓库已存在 Release $LSP_TAG，无需重复发布。"
    return 0
  fi

  local APK_DIR=""
  local TMP_DOWNLOAD_DIR=""
  if [ -n "$LOCAL_APK_DIR" ] && [ -d "$LOCAL_APK_DIR" ] && ls "$LOCAL_APK_DIR"/*.apk &>/dev/null; then
    echo "[INFO] 使用本地已编译生成的发布资产: $LOCAL_APK_DIR"
    APK_DIR="$LOCAL_APK_DIR"
  else
    TMP_DOWNLOAD_DIR="$(mktemp -d)"
    APK_DIR="$TMP_DOWNLOAD_DIR"
    echo "[INFO] 正在从源仓库 ($SOURCE_REPO) 下载 Release $TAG 资产..."
    gh release download "$TAG" --repo "$SOURCE_REPO" --dir "$APK_DIR"
  fi

  echo "[INFO] 正在向目标仓库 ($TARGET_REPO) 发布 Release ($LSP_TAG)..."
  local RELEASE_FILES=()
  for f in "$APK_DIR"/*; do
    if [ -f "$f" ]; then
      RELEASE_FILES+=("$f")
    fi
  done

  if [ ${#RELEASE_FILES[@]} -eq 0 ]; then
    echo "[ERROR] 未找到任何待发布的 APK 或校验和文件！"
    [ -n "$TMP_DOWNLOAD_DIR" ] && rm -rf "$TMP_DOWNLOAD_DIR"
    exit 1
  fi

  gh release create "$LSP_TAG" "${RELEASE_FILES[@]}" \
    --repo "$TARGET_REPO" \
    --title "$CLEAN_VERSION" \
    --notes "Release for FuckClip $CLEAN_VERSION.
Source: https://github.com/$SOURCE_REPO/releases/tag/$TAG"

  [ -n "$TMP_DOWNLOAD_DIR" ] && rm -rf "$TMP_DOWNLOAD_DIR"
  echo "[SUCCESS] 成功在 $TARGET_REPO 发布 Release: $LSP_TAG"
}

# 执行删除同步
do_delete() {
  local TAG="$1"
  if [ -z "$TAG" ]; then
    echo "[ERROR] 删除操作必须指定 Tag (如 v1.0.1 或 10001-1.0.1)"
    exit 1
  fi

  local LSP_TAG
  LSP_TAG=$(calc_lsp_tag "$TAG")
  if [ -z "$LSP_TAG" ]; then
    echo "[ERROR] 无法将 Tag '$TAG' 解析为 LSPosed 规范版本号 (<versionCode>-<versionName>)"
    exit 1
  fi

  echo "[INFO] 准备从目标仓库 ($TARGET_REPO) 删除 Release 及 Tag: $LSP_TAG"

  local DELETED=0
  if gh release view "$LSP_TAG" --repo "$TARGET_REPO" &>/dev/null; then
    echo "[INFO] 正在删除 Release $LSP_TAG 及关联 Tag..."
    gh release delete "$LSP_TAG" --repo "$TARGET_REPO" --yes --cleanup-tag
    DELETED=1
  fi

  # 检查 Tag 是否仍残留在仓库中（防御性清理）
  if gh api "/repos/$TARGET_REPO/git/refs/tags/$LSP_TAG" &>/dev/null; then
    echo "[INFO] 正在清理残留的 Git Tag: $LSP_TAG ..."
    gh api --method DELETE "/repos/$TARGET_REPO/git/refs/tags/$LSP_TAG" || true
    DELETED=1
  fi

  if [ "$DELETED" -eq 1 ]; then
    echo "[SUCCESS] 已成功从 $TARGET_REPO 删除 Release 及 Tag: $LSP_TAG"
  else
    echo "[INFO] 目标仓库中未发现 Release 或 Tag $LSP_TAG，无需删除。"
  fi
}

ACTION="${1:-sync}"

case "$ACTION" in
  sync)
    shift || true
    do_sync "${1:-}" "${2:-}"
    ;;
  delete)
    shift || true
    do_delete "${1:-}"
    ;;
  metadata)
    sync_metadata
    ;;
  */*)
    TARGET_REPO="$ACTION"
    do_sync "" ""
    ;;
  *)
    if echo "$ACTION" | grep -Eq '^(v?[0-9]+\.[0-9]+|[0-9]+-)'; then
      do_sync "$ACTION" "${2:-}"
    else
      echo "用法: $0 [sync|delete|metadata] [tag] [apk_dir]"
      exit 1
    fi
    ;;
esac

echo ""
echo "=========================================================="
echo "同步流程已全部执行完毕。"
echo "=========================================================="
