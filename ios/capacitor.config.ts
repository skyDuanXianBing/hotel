import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'jp.thehost.pms',
  appName: 'The Host PMS',
  webDir: 'dist',
  plugins: {
    // 前台收到远程推送时仍弹出系统横幅（iOS）
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'banner', 'list']
    },
    // 前台本地横幅（Android 远程推送前台兜底）
    LocalNotifications: {
      presentationOptions: ['badge', 'sound', 'banner', 'list']
    }
  }
};

export default config;
