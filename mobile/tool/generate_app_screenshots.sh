#!/bin/bash
# 生成 App Store / Google Play 截图
#
# 用法:
#   bash tool/generate_app_screenshots.sh            # 自动选设备 (优先 Android release)
#   bash tool/generate_app_screenshots.sh android
#   bash tool/generate_app_screenshots.sh ios
#
# 前置:
#   1. 后端在跑 (http://localhost:8090; Android 模拟器走 10.0.2.2:8090)
#   2. 设备已安装 App
#      - Android: flutter build apk --release && adb install -r app-release.apk
#        (release 包无 DEBUG 角标, 是商店截图的正确来源)
#      - iOS: 模拟器只能跑 debug (带 DEBUG 角标, 需裁掉, 见 CROP_DEBUG_BANNER)
#   3. App 支持 jadecart:// 深链 (见 README)
#
# 产出: build/screenshots/{play,ios}/<页名>.png
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT/build/screenshots"
PLATFORM="${1:-auto}"

ADB="${ADB:-/opt/homebrew/share/android-commandlinetools/platform-tools/adb}"
PKG="com.jadecart.jade_cart_mobile"
IOS_BUNDLE="com.jadecart.jadeCartMobile"

# 路由 → 输出名 (深链三斜杠, path 与网页路由一致)
ROUTES=(
  "home:/home"
  "shop:/shop"
  "product:/shop/ice-smoke-fair-cup-master-cup"
  "cart:/cart"
  "account:/account"
)

pick_platform() {
  if [[ "$PLATFORM" != "auto" ]]; then echo "$PLATFORM"; return; fi
  if $ADB devices 2>/dev/null | grep -q "^emulator-.*device"; then echo android; else echo ios; fi
}

PLATFORM="$(pick_platform)"
mkdir -p "$OUT/$PLATFORM"

if [[ "$PLATFORM" == "android" ]]; then
  SERIAL="$($ADB devices | awk '/^emulator-.*device$/{print $1; exit}')"
  if [[ -z "$SERIAL" ]]; then
    echo "未找到运行中的 Android 模拟器" >&2
    exit 1
  fi
  echo "使用 Android 模拟器: $SERIAL"
  $ADB -s "$SERIAL" shell am start -n "$PKG/.MainActivity" >/dev/null 2>&1 || true
  sleep 6

  capture() {
    local route="$1" dest="$2"
    $ADB -s "$SERIAL" shell am start -a android.intent.action.VIEW -d "jadecart://$route" >/dev/null 2>&1
    sleep 4
    $ADB -s "$SERIAL" exec-out screencap -p > "$dest"
    echo "  ✓ $(basename "$dest")"
  }
else
  UDID="${ANDROID_UDID:-$(xcrun simctl list devices booted | grep -E 'iPhone' | head -1 | sed -E 's/.*\(([0-9A-F-]{36})\).*/\1/')}"
  if [[ -z "$UDID" ]]; then
    echo "未找到已启动的 iOS 模拟器" >&2
    exit 1
  fi
  echo "使用 iOS 模拟器: $UDID"
  xcrun simctl launch "$UDID" "$IOS_BUNDLE" >/dev/null 2>&1 || true
  sleep 6

  capture() {
    local route="$1" dest="$2"
    xcrun simctl openurl "$UDID" "jadecart://$route" >/dev/null 2>&1
    sleep 4
    xcrun simctl io "$UDID" screenshot --type=png "$dest" >/dev/null 2>&1
    echo "  ✓ $(basename "$dest")"
  }
fi

echo "→ 抓取截图"
for entry in "${ROUTES[@]}"; do
  name="${entry%%:*}"
  route="${entry#*:}"
  capture "$route" "$OUT/$PLATFORM/$name.png"
done

if [[ "$PLATFORM" == "ios" ]]; then
  echo "→ 裁掉 DEBUG 角标 (iOS 模拟器只能跑 debug 构建)"
  for f in "$OUT/$PLATFORM"/*.png; do
    w=$(sips -g pixelWidth "$f" | awk '/pixelWidth/{print $2}')
    h=$(sips -g pixelHeight "$f" | awk '/pixelHeight/{print $2}')
    # 右上角 DEBUG 斜标约占右上 12% × 8%
    sips -c "$((h - h / 14))" "$w" "$f" --out "$f" >/dev/null
  done
fi

echo "✓ 截图已生成: $OUT/$PLATFORM"
echo "  提示: 截图里的购物车/订单需有内容才有说服力 (先在 App 里加购)"
