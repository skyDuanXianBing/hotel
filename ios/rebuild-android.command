#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
ANDROID_PLATFORM_DIR="$PROJECT_DIR/android"

echo "==> 项目目录: $PROJECT_DIR"
cd "$PROJECT_DIR"

echo "==> 重新构建前端资源"
npm run build

if [ ! -d "$ANDROID_PLATFORM_DIR" ]; then
  echo "==> Android 平台不存在，开始添加"
  npx cap add android
else
  echo "==> Android 平台已存在，跳过 add"
fi

echo "==> 同步 Capacitor Android 资源"
npx cap sync android

echo "==> 完成（用 Android Studio 打开 android/ 运行；FCM 需放置 google-services.json，见 docs/ios-push-notifications.md）"
