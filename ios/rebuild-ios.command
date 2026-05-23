#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
IOS_PLATFORM_DIR="$PROJECT_DIR/ios"

echo "==> 进入 iOS 项目"
echo "==> 项目目录: $PROJECT_DIR"
cd "$PROJECT_DIR"

echo "==> 重新构建前端资源"
npm run build

if [ ! -d "$IOS_PLATFORM_DIR" ]; then
  echo "==> iOS 平台不存在，开始添加"
  npx cap add ios
else
  echo "==> iOS 平台已存在，跳过 add"
fi

echo "==> 同步 Capacitor iOS 资源"
npx cap sync ios

echo "==> 完成"
