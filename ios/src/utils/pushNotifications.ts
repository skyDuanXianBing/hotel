import { App } from '@capacitor/app'
import { Capacitor } from '@capacitor/core'
import { LocalNotifications } from '@capacitor/local-notifications'
import type { ActionPerformed as LocalActionPerformed } from '@capacitor/local-notifications'
import { PushNotifications } from '@capacitor/push-notifications'
import type { ActionPerformed, PushNotificationSchema, Token } from '@capacitor/push-notifications'
import { buildMessageDetailPath, buildRegistrationReviewPath, ROUTE_PATHS } from '@/router/guards'
import { registerPushDevice, unregisterPushDevice, type PushPlatform } from '@/api/push'
import { getStoredCurrentStoreId, getStoredToken, readStoredValue, writeStoredValue } from '@/utils/storage'

/**
 * 手机推送（远程 APNs / 预留 FCM）与前台本地横幅。
 *
 * 生命周期：
 * - 登录且有门店上下文后调用 syncPushRegistration()（AppNotificationOverlay 的 watch 触发）；
 * - 远程推送到达：前台 → 转本地系统横幅；后台/未打开 → APNs 直接弹系统通知；
 * - 点击横幅/通知 → 按 data.type 路由（chat→会话，order→订单详情，task→系统通知页）；
 * - 退出登录 → unregisterPushDeviceOnLogout() 解绑设备。
 */

const PUSH_TOKEN_STORAGE_KEY = 'push_device_token'
const LOCAL_NOTIFICATION_ID_BASE = 910000000

let listenersReady = false
let registering = false
let lastUploadedToken: string | null = null
let localNotificationSeq = 0

type PushPayloadType = 'chat' | 'order' | 'task'

export interface PushPayload {
  type?: string
  threadId?: string
  reservationId?: string
  formId?: string
  orderNumber?: string
}

const isNative = () => Capacitor.isNativePlatform()

const resolvePlatform = (): PushPlatform => {
  return Capacitor.getPlatform() === 'android' ? 'ANDROID' : 'IOS'
}

const hasAuthContext = () => Boolean(getStoredToken() && getStoredCurrentStoreId())

const normalizePayload = (data: unknown): PushPayload => {
  if (!data || typeof data !== 'object') {
    return {}
  }
  return data as PushPayload
}

export const resolvePushTargetPath = (payload: PushPayload): string | null => {
  const type = (payload.type || '').trim().toLowerCase() as PushPayloadType | ''
  if (type === 'chat') {
    const threadId = (payload.threadId || '').trim()
    return threadId ? buildMessageDetailPath(threadId) : ROUTE_PATHS.messages
  }
  if (type === 'order') {
    const reservationId = (payload.reservationId || '').trim()
    return reservationId
      ? `/tabs/orders/reservations/${encodeURIComponent(reservationId)}`
      : ROUTE_PATHS.orderNotifications
  }
  if (type === 'task') {
    const formId = (payload.formId || '').trim()
    return formId ? buildRegistrationReviewPath(formId) : ROUTE_PATHS.reviews
  }
  return null
}

const navigateForPayload = async (payload: PushPayload) => {
  const targetPath = resolvePushTargetPath(payload)
  if (!targetPath || !hasAuthContext()) {
    return
  }
  try {
    // 动态导入避免模块加载期拉入整个路由（单测与首屏都更轻）
    const { default: router } = await import('@/router')
    await router.push(targetPath)
  } catch {
    // 路由失败不影响通知本身
  }
}

const showForegroundBanner = async (notification: PushNotificationSchema) => {
  const title = notification.title?.trim() || ''
  const body = notification.body?.trim() || ''
  if (!title && !body) {
    return
  }
  localNotificationSeq = (localNotificationSeq + 1) % 1000
  try {
    await LocalNotifications.schedule({
      notifications: [
        {
          id: LOCAL_NOTIFICATION_ID_BASE + localNotificationSeq,
          title: title || body,
          body,
          extra: normalizePayload(notification.data),
        },
      ],
    })
  } catch {
    // 本地横幅调度失败不阻断主流程
  }
}

const uploadTokenIfPossible = async (token: string) => {
  if (!token || token === lastUploadedToken || !hasAuthContext()) {
    return
  }
  try {
    const response = await registerPushDevice(resolvePlatform(), token)
    if (response.success !== false) {
      lastUploadedToken = token
    }
  } catch {
    // 网络失败时保留本地 token，下次 sync 重试
  }
}

const ensureListeners = () => {
  if (listenersReady || !isNative()) {
    return
  }
  listenersReady = true

  void PushNotifications.addListener('registration', (token: Token) => {
    writeStoredValue(PUSH_TOKEN_STORAGE_KEY, token.value)
    void uploadTokenIfPossible(token.value)
  })

  void PushNotifications.addListener('registrationError', (error) => {
    console.warn('[Push] registration error:', error)
  })

  // 前台收到远程推送：iOS 由系统按 presentationOptions 直接弹横幅；
  // Android 前台不弹系统通知，转本地通知横幅兜底
  void PushNotifications.addListener('pushNotificationReceived', (notification) => {
    if (Capacitor.getPlatform() === 'android') {
      void showForegroundBanner(notification)
    }
  })

  // 点击远程推送（后台/冷启动）
  void PushNotifications.addListener('pushNotificationActionPerformed', (action: ActionPerformed) => {
    void navigateForPayload(normalizePayload(action.notification.data))
  })

  // 点击前台本地横幅
  void LocalNotifications.addListener('localNotificationActionPerformed', (action: LocalActionPerformed) => {
    void navigateForPayload(normalizePayload(action.notification.extra))
  })

  // 回到前台时重新同步注册（令牌可能已轮换，或服务端解绑失败需补注册）
  void App.addListener('appStateChange', ({ isActive }) => {
    if (isActive) {
      void syncPushRegistration()
    }
  })
}

/**
 * 登录且选定门店后调用：申请权限并注册推送，成功回调里自动上传令牌。
 * 非原生环境（浏览器开发）直接跳过。
 */
export const syncPushRegistration = async () => {
  if (!isNative() || !hasAuthContext() || registering) {
    return
  }
  ensureListeners()

  try {
    let permission = await PushNotifications.checkPermissions()
    if (permission.receive === 'prompt' || permission.receive === 'prompt-with-rationale') {
      permission = await PushNotifications.requestPermissions()
    }
    if (permission.receive !== 'granted') {
      return
    }

    // 前台横幅用的本地通知权限（iOS 与远程推送共用授权弹窗）
    try {
      const localPermission = await LocalNotifications.checkPermissions()
      if (localPermission.display === 'prompt' || localPermission.display === 'prompt-with-rationale') {
        await LocalNotifications.requestPermissions()
      }
    } catch {
      // 本地通知权限失败不影响远程推送注册
    }

    registering = true
    await PushNotifications.register()

    // register 事件可能因缓存不回调，用本地存留 token 兜底补传
    const cachedToken = readStoredValue(PUSH_TOKEN_STORAGE_KEY)
    if (cachedToken) {
      await uploadTokenIfPossible(cachedToken)
    }
  } catch (error) {
    console.warn('[Push] sync registration failed:', error)
  } finally {
    registering = false
  }
}

/**
 * 退出登录时调用：解绑服务端设备令牌并清理本地记录。
 */
export const unregisterPushDeviceOnLogout = async () => {
  if (!isNative()) {
    return
  }
  const token = readStoredValue(PUSH_TOKEN_STORAGE_KEY)
  lastUploadedToken = null
  writeStoredValue(PUSH_TOKEN_STORAGE_KEY, null)
  if (!token) {
    return
  }
  try {
    await unregisterPushDevice(token)
  } catch {
    // 解绑失败可接受：令牌已失效或下次登录时会被 upsert 覆盖
  }
}
