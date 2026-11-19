#!/bin/bash
# 生成 App 图标 (iOS AppIcon + Android mipmap)
#
# 用法: bash tool/generate_app_icons.sh
# 依赖: qlmanage (SVG → PNG), sips (缩放), macOS
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SVG="$ROOT/assets/branding/jade-cart-app-icon.svg"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

if [[ ! -f "$SVG" ]]; then
  echo "找不到图标母版: $SVG" >&2
  exit 1
fi

echo "→ SVG 光栅化为 1024 PNG"
qlmanage -t -s 1024 -o "$TMP" "$SVG" >/dev/null 2>&1
SRC="$(find "$TMP" -name '*.png' | head -1)"
if [[ -z "$SRC" ]]; then
  echo "SVG 光栅化失败 (需要 qlmanage)" >&2
  exit 1
fi
cp "$SRC" "$TMP/icon-1024.png"

resize() {
  # resize <src> <size> <dest>
  sips -z "$2" "$2" "$1" --out "$3" >/dev/null
}

echo "→ iOS AppIcon.appiconset"
IOS_DIR="$ROOT/ios/Runner/Assets.xcassets/AppIcon.appiconset"
mkdir -p "$IOS_DIR"
cat > "$IOS_DIR/Contents.json" <<'JSON'
{
  "images" : [
    { "size" : "20x20", "scale" : "2x", "idiom" : "iphone", "filename" : "icon-40.png" },
    { "size" : "20x20", "scale" : "3x", "idiom" : "iphone", "filename" : "icon-60.png" },
    { "size" : "29x29", "scale" : "1x", "idiom" : "iphone", "filename" : "icon-29.png" },
    { "size" : "29x29", "scale" : "2x", "idiom" : "iphone", "filename" : "icon-58.png" },
    { "size" : "29x29", "scale" : "3x", "idiom" : "iphone", "filename" : "icon-87.png" },
    { "size" : "40x40", "scale" : "2x", "idiom" : "iphone", "filename" : "icon-80.png" },
    { "size" : "40x40", "scale" : "3x", "idiom" : "iphone", "filename" : "icon-120.png" },
    { "size" : "60x60", "scale" : "2x", "idiom" : "iphone", "filename" : "icon-120.png" },
    { "size" : "60x60", "scale" : "3x", "idiom" : "iphone", "filename" : "icon-180.png" },
    { "size" : "1024x1024", "scale" : "1x", "idiom" : "ios-marketing", "filename" : "icon-1024.png" }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
JSON
for size in 40 60 29 58 87 80 120 180 1024; do
  resize "$TMP/icon-1024.png" "$size" "$IOS_DIR/icon-$size.png"
done

echo "→ Android mipmap"
for dir in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
  case "$dir" in
    mdpi) s=48 ;;
    hdpi) s=72 ;;
    xhdpi) s=96 ;;
    xxhdpi) s=144 ;;
    xxxhdpi) s=192 ;;
  esac
  out="$ROOT/android/app/src/main/res/mipmap-$dir"
  mkdir -p "$out"
  resize "$TMP/icon-1024.png" "$s" "$out/ic_launcher.png"
done

echo "✓ 图标生成完成"
