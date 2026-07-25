<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Calendar, Check, Lock, User } from '@element-plus/icons-vue'
import { loadStripe } from '@stripe/stripe-js/pure'
import type { Stripe, StripeCardElement } from '@stripe/stripe-js'
import {
  confirmPublicIndependentSitePayment,
  createPublicIndependentSiteHold,
  createPublicIndependentSitePaymentIntent,
  getPublicIndependentSitePayment,
  getPublicIndependentSiteQuote,
} from '@/api/independentSite'
import type {
  IndependentSitePaymentAttempt,
  IndependentSiteQuote,
  PublicIndependentSiteRoomType,
  PublicIndependentSite,
} from '@/types/independentSite'
import { safeIndependentSiteImageUrl } from '@/views/independent-site/pageSchema'

interface SearchForm {
  checkInDate: string
  checkOutDate: string
  adults: number
  children: number
  roomCount: number
}

interface GuestForm {
  firstName: string
  lastName: string
  email: string
  phone: string
  specialRequests: string
}

interface RoomQuoteView extends IndependentSiteQuote {
  description?: string
  imageUrls?: string[]
  maxGuests?: number
}

interface QuoteCollection {
  currency: string
  checkInDate: string
  checkOutDate: string
  expiresAt?: string
  roomTypes: RoomQuoteView[]
}

const props = withDefaults(
  defineProps<{
    // 站点公开信息（房型/房间/货币/支付配置）
    site: PublicIndependentSite
    // ROOM_DETAIL 页传入该页房型 id，报价加载完成后自动选中（等同用户点击对应房型卡片）
    initialRoomTypeId?: number | null
    // 管理测试预览模式（?preview=1）与授权结果由父组件注入；
    // 当前订房流程对游客与已授权预览行为一致（门禁只看站点支付渠道配置），
    // 子组件不直接调用任何管理端 API
    preview?: boolean
    previewAuthorized?: boolean
  }>(),
  {
    initialRoomTypeId: null,
    preview: false,
    previewAuthorized: false,
  },
)

const searchFormRef = ref<FormInstance>()
const guestFormRef = ref<FormInstance>()
const quoteLoading = ref(false)
const quoteError = ref('')
const quote = ref<QuoteCollection | null>(null)
const selectedRoomTypeId = ref<number | null>(null)
const checkoutLoading = ref(false)
const checkoutError = ref('')
const checkout = ref<IndependentSitePaymentAttempt | null>(null)
const paymentLoading = ref(false)
const paymentError = ref('')
const paymentResult = ref<IndependentSitePaymentAttempt | null>(null)
const idempotencyKey = ref('')
let quoteRequestSequence = 0

// Stripe 卡支付状态（provider=STRIPE 时启用；卡号只进入 Stripe Elements iframe，不经过本站）
const stripeCardContainer = ref<HTMLElement | null>(null)
const stripeIntentLoading = ref(false)
const stripeIntentError = ref('')
const stripeCardError = ref('')
const stripeCardComplete = ref(false)
const stripeReady = ref(false)
let stripeInstance: Stripe | null = null
let stripeCardElement: StripeCardElement | null = null
let stripeClientSecret = ''
let stripePollingCancelled = false

// confirm 成功后 webhook 异步推进 attempt：每 2s 轮询一次，最多 10 次
const PAYMENT_POLL_INTERVAL_MS = 2000
const PAYMENT_POLL_MAX_ATTEMPTS = 10

const searchForm = reactive<SearchForm>({
  checkInDate: '',
  checkOutDate: '',
  adults: 1,
  children: 0,
  roomCount: 1,
})

const guestForm = reactive<GuestForm>({
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  specialRequests: '',
})

// 游客在线订房门禁：STRIPE 站点始终可在线收卡支付；其余渠道需开启模拟支付
const canBookOnline = computed(() =>
  props.site.paymentProvider === 'STRIPE' ? true : Boolean(props.site.simulatedPaymentEnabled),
)

// 支付渠道以 hold 返回的 attempt.provider 为准（契约：= 站点配置），缺省回退站点公开配置
const activePaymentProvider = computed(
  () => checkout.value?.provider || props.site.paymentProvider || 'SIMULATED',
)
const isStripePayment = computed(() => activePaymentProvider.value === 'STRIPE')

const selectedQuote = computed(
  () => quote.value?.roomTypes.find((item) => item.roomTypeId === selectedRoomTypeId.value) || null,
)

const hasBookableQuotes = computed(
  () =>
    quote.value?.roomTypes.some((roomQuote) => roomQuote.availableRooms >= searchForm.roomCount) ??
    false,
)

const paymentStatus = computed(() => paymentResult.value?.status || checkout.value?.status)

const bookingUnavailableTitle = computed(() => {
  return '当前站点未开放在线支付'
})

const bookingUnavailableDescription = computed(() => {
  return (
    props.site.paymentNotice ||
    '您可以查询实时房量与价格，但暂时无法在线下单支付。请联系门店完成预订。'
  )
})

const currentStep = computed(() => {
  if (paymentStatus.value === 'SUCCEEDED') {
    return 4
  }
  if (checkout.value) {
    return 3
  }
  if (selectedQuote.value && canBookOnline.value) {
    return 2
  }
  return 1
})

const bookingReference = computed(() => {
  const attempt = paymentResult.value || checkout.value
  if (attempt?.groupOrderNo) {
    return attempt.groupOrderNo
  }
  if (attempt?.reservationOrderNumbers?.length) {
    return attempt.reservationOrderNumbers.join('、')
  }
  return '处理中'
})

const numberOfNights = computed(() => {
  if (!searchForm.checkInDate || !searchForm.checkOutDate) {
    return 0
  }
  const checkIn = new Date(`${searchForm.checkInDate}T00:00:00`)
  const checkOut = new Date(`${searchForm.checkOutDate}T00:00:00`)
  return Math.max(0, Math.round((checkOut.getTime() - checkIn.getTime()) / 86400000))
})

const termsUrl = computed(() => safeLegalUrl(undefined, '/legal/terms'))
const privacyUrl = computed(() => safeLegalUrl(undefined, '/legal/privacy'))

const searchRules: FormRules = {
  checkInDate: [{ required: true, message: '请选择入住日期', trigger: 'change' }],
  checkOutDate: [
    { required: true, message: '请选择离店日期', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value && searchForm.checkInDate && String(value) <= String(searchForm.checkInDate)) {
          callback(new Error('离店日期必须晚于入住日期'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
  adults: [
    {
      validator: (_rule, value, callback) => {
        if (Number(value) < searchForm.roomCount) {
          callback(new Error('每间房至少需要一位成人'))
          return
        }
        callback()
      },
      trigger: 'change',
    },
  ],
}

const guestRules: FormRules = {
  firstName: [
    { required: true, message: '请输入名字', trigger: 'blur' },
    { min: 1, max: 49, message: '名字不能超过 49 个字符', trigger: 'blur' },
  ],
  lastName: [
    { required: true, message: '请输入姓氏', trigger: 'blur' },
    { min: 1, max: 49, message: '姓氏不能超过 49 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效邮箱', trigger: ['blur', 'change'] },
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    {
      pattern: /^[+()\-\s0-9]{6,30}$/,
      message: '请输入有效联系电话',
      trigger: ['blur', 'change'],
    },
  ],
  specialRequests: [{ max: 500, message: '特殊需求不能超过 500 个字符', trigger: 'blur' }],
}

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === 'object') {
    const message = (
      error as {
        response?: { data?: { message?: unknown } }
        message?: unknown
      }
    ).response?.data?.message
    if (typeof message === 'string' && message.trim()) {
      return message
    }
    const localMessage = (error as { message?: unknown }).message
    if (typeof localMessage === 'string' && localMessage.trim()) {
      return localMessage
    }
  }
  return fallback
}

function safeLegalUrl(value: unknown, fallback: string): string {
  if (typeof value !== 'string' || !value.trim()) {
    return fallback
  }
  const normalized = value.trim()
  if (normalized.startsWith('/') && !normalized.startsWith('//')) {
    return normalized
  }
  try {
    const parsed = new URL(normalized)
    return parsed.protocol === 'https:' || parsed.protocol === 'http:'
      ? parsed.toString()
      : fallback
  } catch {
    return fallback
  }
}

const formatMoney = (amount: unknown, currency?: string) => {
  const numericAmount = Number(amount)
  if (!Number.isFinite(numericAmount)) {
    return '—'
  }
  try {
    return new Intl.NumberFormat('zh-CN', {
      style: 'currency',
      currency: currency || props.site.currency || 'CNY',
      minimumFractionDigits: 2,
    }).format(numericAmount)
  } catch {
    return `${numericAmount.toFixed(2)} ${currency || props.site.currency || ''}`.trim()
  }
}

const formatDate = (value?: string) => {
  if (!value) {
    return ''
  }
  const date = new Date(`${value.slice(0, 10)}T00:00:00`)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('zh-CN', {
        month: 'short',
        day: 'numeric',
        weekday: 'short',
      }).format(date)
}

const formatDateTime = (value?: string) => {
  if (!value) {
    return ''
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('zh-CN', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(date)
}

const todayStart = () => {
  const now = new Date()
  return new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

const disableCheckInDate = (date: Date) => date.getTime() < todayStart().getTime()

const disableCheckOutDate = (date: Date) => {
  if (date.getTime() <= todayStart().getTime()) {
    return true
  }
  if (!searchForm.checkInDate) {
    return false
  }
  return date.getTime() <= new Date(`${searchForm.checkInDate}T00:00:00`).getTime()
}

const createIdempotencyKey = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return `stay-${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

const handleSearch = async () => {
  const requestSequence = ++quoteRequestSequence
  const valid = await searchFormRef.value?.validate().catch(() => false)
  if (!valid || requestSequence !== quoteRequestSequence) {
    return
  }

  const requestedSite = props.site
  const requestedSlug = requestedSite.slug
  const searchSnapshot = {
    checkInDate: searchForm.checkInDate,
    checkOutDate: searchForm.checkOutDate,
    rooms: searchForm.roomCount,
    adults: searchForm.adults,
    children: searchForm.children,
  }
  const isCurrentSearch = () =>
    requestSequence === quoteRequestSequence && requestedSite === props.site

  quoteLoading.value = true
  quoteError.value = ''
  checkoutError.value = ''
  checkout.value = null
  paymentResult.value = null
  selectedRoomTypeId.value = null
  idempotencyKey.value = ''
  try {
    if (!requestedSite.roomTypes.length) {
      throw new Error('此站点尚未发布可订房型')
    }

    const quoteResults = await Promise.allSettled(
      requestedSite.roomTypes.map(async (roomType: PublicIndependentSiteRoomType) => {
        const response = await getPublicIndependentSiteQuote(requestedSlug, {
          roomTypeId: roomType.id,
          ...searchSnapshot,
        })
        if (!response.success || !response.data) {
          throw new Error(response.message || `${roomType.name} 暂时无法报价`)
        }
        return {
          ...response.data,
          description: roomType.description,
          imageUrls: [...(roomType.desktopPhotoUrls || []), ...(roomType.mobilePhotoUrls || [])],
          maxGuests: roomType.maxGuests,
        } satisfies RoomQuoteView
      }),
    )
    if (!isCurrentSearch()) {
      return
    }

    const availableQuotes: RoomQuoteView[] = []
    for (const result of quoteResults) {
      if (result.status === 'fulfilled') {
        availableQuotes.push(result.value)
      }
    }

    if (availableQuotes.length === 0) {
      const firstFailure = quoteResults.find(
        (result): result is PromiseRejectedResult => result.status === 'rejected',
      )
      throw firstFailure?.reason || new Error('当前条件下没有可订房型')
    }

    const firstQuote = availableQuotes[0]
    quote.value = {
      currency: firstQuote.currency || requestedSite.currency,
      checkInDate: firstQuote.checkInDate,
      checkOutDate: firstQuote.checkOutDate,
      expiresAt: availableQuotes
        .map((item) => item.expiresAt)
        .filter((item): item is string => Boolean(item))
        .sort()[0],
      roomTypes: availableQuotes,
    }
    if (availableQuotes.length < quoteResults.length) {
      quoteError.value = '部分房型暂时无法报价，以下仅显示服务端已成功核算的房型。'
    }
    await nextTick()
    if (!isCurrentSearch()) {
      return
    }
    document.querySelector('#availability-results')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
    await applyInitialRoomTypeSelection()
  } catch (error) {
    if (!isCurrentSearch()) {
      return
    }
    quote.value = null
    quoteError.value = getErrorMessage(error, '暂时无法获取实时房量与报价，请重试。')
  } finally {
    if (isCurrentSearch()) {
      quoteLoading.value = false
    }
  }
}

const selectRoomQuote = async (roomQuote: RoomQuoteView) => {
  if (roomQuote.availableRooms < searchForm.roomCount) {
    return
  }
  selectedRoomTypeId.value = roomQuote.roomTypeId
  checkout.value = null
  paymentResult.value = null
  checkoutError.value = ''
  idempotencyKey.value = createIdempotencyKey()
  await nextTick()
  document.querySelector('#guest-details')?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}

// ROOM_DETAIL 页默认选中：报价加载完成后选中该房型报价（等同用户点击）；
// 该房型报价失败/房量不足时不选中，报价列表正常展示
const applyInitialRoomTypeSelection = async () => {
  const targetRoomTypeId = props.initialRoomTypeId
  if (!targetRoomTypeId || !quote.value) {
    return
  }
  const targetQuote = quote.value.roomTypes.find((item) => item.roomTypeId === targetRoomTypeId)
  if (!targetQuote || targetQuote.availableRooms < searchForm.roomCount) {
    return
  }
  await selectRoomQuote(targetQuote)
}

const handleCheckout = async () => {
  if (!quote.value || !selectedQuote.value) {
    checkoutError.value = '请先选择可订房型'
    return
  }
  if (!canBookOnline.value) {
    checkoutError.value = bookingUnavailableDescription.value
    return
  }
  const valid = await guestFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  checkoutLoading.value = true
  checkoutError.value = ''
  if (!idempotencyKey.value) {
    idempotencyKey.value = createIdempotencyKey()
  }
  try {
    const response = await createPublicIndependentSiteHold(props.site.slug, {
      idempotencyKey: idempotencyKey.value,
      roomTypeId: selectedQuote.value.roomTypeId,
      checkInDate: searchForm.checkInDate,
      checkOutDate: searchForm.checkOutDate,
      rooms: searchForm.roomCount,
      adults: searchForm.adults,
      children: searchForm.children,
      guest: {
        name: `${guestForm.firstName.trim()} ${guestForm.lastName.trim()}`.trim(),
        email: guestForm.email.trim(),
        phone: guestForm.phone.trim(),
        specialRequests: guestForm.specialRequests?.trim() || undefined,
      },
    })
    if (!response.success || !response.data) {
      throw new Error(response.message || '暂时无法保留房间')
    }
    checkout.value = response.data
    paymentResult.value = null
    if (isStripePayment.value) {
      await setupStripePayment()
    }
    await nextTick()
    document.querySelector('#payment-step')?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
  } catch (error) {
    checkoutError.value = getErrorMessage(error, '房量或价格可能已变化，请重新查询后再提交。')
  } finally {
    checkoutLoading.value = false
  }
}

const refreshPaymentStatus = async () => {
  if (!checkout.value) {
    return
  }
  const response = await getPublicIndependentSitePayment(
    props.site.slug,
    checkout.value.paymentAttemptId,
  )
  if (!response.success || !response.data) {
    throw new Error(response.message || '无法确认支付状态')
  }
  paymentResult.value = response.data
}

const handleSimulatedPayment = async () => {
  if (!checkout.value || !canBookOnline.value) {
    paymentError.value = '当前站点未开放在线支付'
    return
  }

  paymentLoading.value = true
  paymentError.value = ''
  try {
    const response = await confirmPublicIndependentSitePayment(
      props.site.slug,
      checkout.value.paymentAttemptId,
    )
    if (!response.success || !response.data) {
      throw new Error(response.message || '模拟支付处理失败')
    }
    paymentResult.value = response.data
    await refreshPaymentStatus()
  } catch (error) {
    paymentError.value = getErrorMessage(error, '模拟支付处理失败，请重试。')
  } finally {
    paymentLoading.value = false
  }
}

const destroyStripeElements = () => {
  if (stripeCardElement) {
    try {
      stripeCardElement.destroy()
    } catch {
      // 元素可能已随支付面板卸载
    }
    stripeCardElement = null
  }
  stripeInstance = null
  stripeClientSecret = ''
  stripeReady.value = false
  stripeCardComplete.value = false
  stripeCardError.value = ''
}

const mountStripeCardElement = async () => {
  await nextTick()
  const stripe = stripeInstance
  const container = stripeCardContainer.value
  if (!stripe || !container || !checkout.value) {
    return
  }
  // 跟随站点主题主色（--site-primary 由公开页根节点注入）
  const primaryColor =
    getComputedStyle(container).getPropertyValue('--site-primary').trim() || '#214e46'
  const card = stripe.elements().create('card', {
    style: {
      base: {
        color: '#202b28',
        fontSize: '16px',
        iconColor: primaryColor,
        '::placeholder': { color: '#9aa6a2' },
      },
      invalid: {
        color: '#c4564f',
        iconColor: '#c4564f',
      },
    },
  })
  card.mount(container)
  card.on('change', (event) => {
    stripeCardComplete.value = event.complete
    stripeCardError.value = event.error?.message || ''
  })
  stripeCardElement = card
  stripeReady.value = true
}

// hold 创建成功后初始化 Stripe：取/复用 PaymentIntent → 动态加载 Stripe.js → 挂载 Card Element
const setupStripePayment = async () => {
  if (!checkout.value) {
    return
  }
  destroyStripeElements()
  stripeIntentLoading.value = true
  stripeIntentError.value = ''
  paymentError.value = ''
  try {
    const response = await createPublicIndependentSitePaymentIntent(
      props.site.slug,
      checkout.value.paymentAttemptId,
    )
    if (!response.success || !response.data) {
      throw new Error(response.message || '暂时无法初始化银行卡支付')
    }
    const intent = response.data
    // attempt 已非 PENDING（如 15 分钟保留过期）：直接同步服务端最新状态，走现有终态提示
    if (intent.status && intent.status !== 'PENDING') {
      await refreshPaymentStatus()
      return
    }
    if (!intent.clientSecret || !intent.publishableKey) {
      throw new Error('支付初始化响应不完整，请重试')
    }
    const stripe = await loadStripe(intent.publishableKey)
    if (!stripe) {
      throw new Error('Stripe.js 加载失败，请检查网络后重试')
    }
    stripeInstance = stripe
    stripeClientSecret = intent.clientSecret
    await mountStripeCardElement()
  } catch (error) {
    stripeIntentError.value = getErrorMessage(error, '暂时无法初始化银行卡支付，请重试。')
  } finally {
    stripeIntentLoading.value = false
  }
}

// confirm 成功不代表预订已确认（webhook 异步）：轮询 attempt 至 SUCCEEDED/FAILED/EXPIRED
const pollPaymentStatusUntilFinal = async () => {
  if (!checkout.value) {
    return
  }
  for (let attempt = 0; attempt < PAYMENT_POLL_MAX_ATTEMPTS; attempt += 1) {
    if (stripePollingCancelled) {
      return
    }
    try {
      const response = await getPublicIndependentSitePayment(
        props.site.slug,
        checkout.value.paymentAttemptId,
      )
      if (response.success && response.data) {
        paymentResult.value = response.data
        if (response.data.status !== 'PENDING') {
          return
        }
      }
    } catch {
      // 单次轮询失败继续重试，由次数上限兜底
    }
    if (attempt < PAYMENT_POLL_MAX_ATTEMPTS - 1) {
      await new Promise((resolve) => window.setTimeout(resolve, PAYMENT_POLL_INTERVAL_MS))
    }
  }
  paymentError.value = '支付结果确认中，请稍后点击"刷新支付状态"查看最新结果。'
}

// 确认 Stripe 卡支付；卡被拒等错误保留原 intent，可修正后重试，不新建 hold
const handleStripePayment = async () => {
  if (!checkout.value || !stripeInstance || !stripeCardElement || !stripeClientSecret) {
    return
  }
  paymentLoading.value = true
  paymentError.value = ''
  stripePollingCancelled = false
  try {
    const { error } = await stripeInstance.confirmCardPayment(stripeClientSecret, {
      payment_method: {
        card: stripeCardElement,
        billing_details: {
          name: `${guestForm.firstName.trim()} ${guestForm.lastName.trim()}`.trim() || undefined,
          email: guestForm.email.trim() || undefined,
        },
      },
    })
    if (error) {
      paymentError.value = error.message || '银行卡支付失败，请检查卡片信息后重试。'
      return
    }
    await pollPaymentStatusUntilFinal()
  } catch (error) {
    paymentError.value = getErrorMessage(error, '支付确认失败，请重试。')
  } finally {
    paymentLoading.value = false
  }
}

const handleRefreshPaymentStatus = async () => {
  paymentError.value = ''
  try {
    await refreshPaymentStatus()
  } catch (error) {
    paymentError.value = getErrorMessage(error, '暂时无法获取支付状态，请稍后重试。')
  }
}

onBeforeUnmount(() => {
  stripePollingCancelled = true
  destroyStripeElements()
})

const resetBooking = () => {
  quote.value = null
  selectedRoomTypeId.value = null
  checkout.value = null
  paymentResult.value = null
  quoteError.value = ''
  checkoutError.value = ''
  paymentError.value = ''
  idempotencyKey.value = ''
  guestForm.firstName = ''
  guestForm.lastName = ''
  guestForm.email = ''
  guestForm.phone = ''
  guestForm.specialRequests = ''
  nextTick(() => scrollToBooking())
}

const scrollToBooking = () => {
  document.querySelector('#booking')?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}

const roomImage = (roomQuote: RoomQuoteView) => {
  const imageUrl = roomQuote.imageUrls?.find((item) => safeIndependentSiteImageUrl(item))
  return safeIndependentSiteImageUrl(imageUrl)
}

watch(
  () => [
    searchForm.checkInDate,
    searchForm.checkOutDate,
    searchForm.adults,
    searchForm.children,
    searchForm.roomCount,
  ],
  () => {
    quoteRequestSequence += 1
    quoteLoading.value = false
    if (!quote.value) {
      return
    }
    quote.value = null
    selectedRoomTypeId.value = null
    checkout.value = null
    paymentResult.value = null
    quoteError.value = '搜索条件已变化，请重新查询实时房量与价格。'
  },
)

// hold 被清空（重新查询/换房型/预订完成重来等所有出口）时同步释放 Stripe Elements
watch(checkout, (value) => {
  if (!value) {
    destroyStripeElements()
  }
})

// 切页导致 initialRoomTypeId 变化（组件未被 key 重建的兜底）：重置到搜索后的选房状态，
// 清除上一页遗留的选房/hold/支付状态，已有报价时按新房型重新应用默认选中
watch(
  () => props.initialRoomTypeId,
  () => {
    checkout.value = null
    paymentResult.value = null
    checkoutError.value = ''
    paymentError.value = ''
    idempotencyKey.value = ''
    selectedRoomTypeId.value = null
    applyInitialRoomTypeSelection()
  },
)
</script>

<template>
  <section id="booking" class="booking-section">
    <div class="booking-heading">
      <div>
        <span>BOOK YOUR STAY</span>
        <h2>查询实时可订房间</h2>
      </div>
      <ol class="booking-progress" aria-label="预订进度">
        <li :class="{ active: currentStep >= 1 }">1 查询</li>
        <li :class="{ active: currentStep >= 2 }">2 资料</li>
        <li :class="{ active: currentStep >= 3 }">3 支付</li>
        <li :class="{ active: currentStep >= 4 }">4 完成</li>
      </ol>
    </div>

    <el-form
      ref="searchFormRef"
      :model="searchForm"
      :rules="searchRules"
      label-position="top"
      class="search-panel"
      :aria-busy="quoteLoading"
    >
      <el-form-item label="入住日期" prop="checkInDate">
        <el-date-picker
          v-model="searchForm.checkInDate"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          placeholder="选择日期"
          :disabled-date="disableCheckInDate"
          :prefix-icon="Calendar"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="离店日期" prop="checkOutDate">
        <el-date-picker
          v-model="searchForm.checkOutDate"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          placeholder="选择日期"
          :disabled-date="disableCheckOutDate"
          :prefix-icon="Calendar"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="成人" prop="adults">
        <el-input-number
          v-model="searchForm.adults"
          :min="searchForm.roomCount"
          :max="20"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="儿童">
        <el-input-number
          v-model="searchForm.children"
          :min="0"
          :max="20"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="房间数">
        <el-input-number
          v-model="searchForm.roomCount"
          :min="1"
          :max="10"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-button class="search-action" type="primary" :loading="quoteLoading" @click="handleSearch">
        查询实时价格
      </el-button>
    </el-form>

    <el-alert
      v-if="quoteError"
      class="booking-alert"
      type="warning"
      :title="quoteError"
      show-icon
      :closable="false"
    />

    <section v-if="quote" id="availability-results" class="availability-results">
      <div class="result-heading">
        <div>
          <span>REAL-TIME AVAILABILITY</span>
          <h3>
            {{ formatDate(quote.checkInDate || searchForm.checkInDate) }} –
            {{ formatDate(quote.checkOutDate || searchForm.checkOutDate) }}
          </h3>
        </div>
        <p>
          {{ numberOfNights }} 晚 · {{ searchForm.adults }} 位成人
          <template v-if="searchForm.children">· {{ searchForm.children }} 位儿童</template>
          · {{ searchForm.roomCount }} 间房
        </p>
      </div>

      <el-empty
        v-if="quote.roomTypes.length === 0 || !hasBookableQuotes"
        description="所选日期与人数下没有可订房型，请调整条件后重新查询"
      />

      <div v-else class="room-quote-list">
        <article
          v-for="roomQuote in quote.roomTypes"
          :key="roomQuote.roomTypeId"
          class="room-quote-card"
          :class="{ selected: selectedRoomTypeId === roomQuote.roomTypeId }"
        >
          <div v-if="roomImage(roomQuote)" class="room-image-shell">
            <img :src="roomImage(roomQuote)" :alt="`${roomQuote.roomTypeName} 房型图片`" />
          </div>
          <div v-else class="room-image-placeholder" aria-hidden="true">
            <span></span>
          </div>
          <div class="room-quote-copy">
            <div>
              <h4>{{ roomQuote.roomTypeName }}</h4>
              <p v-if="roomQuote.description">{{ roomQuote.description }}</p>
              <div class="room-meta">
                <span v-if="roomQuote.maxGuests">最多 {{ roomQuote.maxGuests }} 人</span>
                <span>剩余 {{ roomQuote.availableRooms }} 间</span>
              </div>
            </div>
            <div class="room-price">
              <small>{{ numberOfNights }} 晚合计</small>
              <strong>{{ formatMoney(roomQuote.totalAmount, quote.currency) }}</strong>
              <button
                type="button"
                :disabled="roomQuote.availableRooms < searchForm.roomCount"
                @click="selectRoomQuote(roomQuote)"
              >
                {{
                  roomQuote.availableRooms < searchForm.roomCount
                    ? '房量不足'
                    : selectedRoomTypeId === roomQuote.roomTypeId
                      ? '已选择'
                      : canBookOnline
                        ? '选择房型'
                        : '查看预订说明'
                }}
              </button>
            </div>
          </div>
        </article>
      </div>
      <p v-if="quote.expiresAt && hasBookableQuotes" class="quote-expiry">
        此报价有效至 {{ formatDateTime(quote.expiresAt) }}；提交时服务端会重新核价与检查房量。
      </p>
    </section>

    <section v-if="selectedQuote && !checkout" id="guest-details" class="checkout-panel">
      <div class="checkout-heading">
        <span>{{ canBookOnline ? 'GUEST DETAILS' : 'BOOKING NOTICE' }}</span>
        <h3>{{ canBookOnline ? '填写预订信息' : '实时报价已就绪' }}</h3>
        <p v-if="canBookOnline">
          已选 {{ selectedQuote.roomTypeName }}，下单时不会采用浏览器传入的金额。
        </p>
        <p v-else>已查看 {{ selectedQuote.roomTypeName }} 的服务端实时报价。</p>
      </div>

      <el-alert
        v-if="!canBookOnline"
        class="payment-disabled-alert"
        type="warning"
        :closable="false"
        show-icon
        :title="bookingUnavailableTitle"
        :description="bookingUnavailableDescription"
      />

      <div v-else class="checkout-layout">
        <el-form
          ref="guestFormRef"
          :model="guestForm"
          :rules="guestRules"
          label-position="top"
          class="guest-form"
        >
          <div class="guest-name-grid">
            <el-form-item label="名字" prop="firstName">
              <el-input v-model.trim="guestForm.firstName" autocomplete="given-name" />
            </el-form-item>
            <el-form-item label="姓氏" prop="lastName">
              <el-input v-model.trim="guestForm.lastName" autocomplete="family-name" />
            </el-form-item>
          </div>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model.trim="guestForm.email" type="email" autocomplete="email" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model.trim="guestForm.phone" type="tel" autocomplete="tel" />
          </el-form-item>
          <el-form-item label="特殊需求（可选）" prop="specialRequests">
            <el-input
              v-model="guestForm.specialRequests"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <p class="consent-copy">
            提交即表示同意
            <a :href="termsUrl" target="_blank" rel="noopener noreferrer">预订条款</a>
            与
            <a :href="privacyUrl" target="_blank" rel="noopener noreferrer">隐私政策</a>。
          </p>
          <el-button type="primary" size="large" :loading="checkoutLoading" @click="handleCheckout">
            重新核价并创建预订保留
          </el-button>
          <el-alert
            v-if="checkoutError"
            class="booking-alert"
            type="error"
            :title="checkoutError"
            show-icon
            :closable="false"
          />
        </el-form>

        <aside class="booking-summary" aria-label="预订摘要">
          <el-icon><User /></el-icon>
          <span>预订摘要</span>
          <h4>{{ selectedQuote.roomTypeName }}</h4>
          <dl>
            <div>
              <dt>入住</dt>
              <dd>{{ formatDate(searchForm.checkInDate) }}</dd>
            </div>
            <div>
              <dt>离店</dt>
              <dd>{{ formatDate(searchForm.checkOutDate) }}</dd>
            </div>
            <div>
              <dt>房间</dt>
              <dd>{{ searchForm.roomCount }} 间</dd>
            </div>
            <div>
              <dt>当前报价</dt>
              <dd>{{ formatMoney(selectedQuote.totalAmount, quote?.currency) }}</dd>
            </div>
          </dl>
          <small>最终金额与可售以提交后的服务端重算结果为准。</small>
        </aside>
      </div>
    </section>

    <section v-if="checkout" id="payment-step" class="payment-panel">
      <template v-if="paymentStatus === 'SUCCEEDED'">
        <div class="success-mark" aria-hidden="true">
          <el-icon><Check /></el-icon>
        </div>
        <span>BOOKING CONFIRMED</span>
        <h3>预订已确认</h3>
        <p>预订编号：{{ bookingReference }}</p>
        <p>服务端已确认支付状态并推进预订；请保存此编号以便后续查询。</p>
        <el-button @click="resetBooking">预订其他日期</el-button>
      </template>

      <template v-else>
        <div class="payment-heading">
          <el-icon><Lock /></el-icon>
          <div>
            <span>PAYMENT</span>
            <h3>完成支付</h3>
            <p>预订编号：{{ bookingReference }}</p>
          </div>
          <strong>{{ formatMoney(checkout.amount, checkout.currency) }}</strong>
        </div>

        <el-alert
          v-if="!canBookOnline"
          type="error"
          show-icon
          :closable="false"
          title="在线支付未开放"
          description="当前站点未开放在线支付，请联系门店完成预订。"
        />
        <el-alert
          v-else-if="isStripePayment"
          type="info"
          show-icon
          :closable="false"
          title="银行卡安全支付"
          :description="
            site.paymentNotice || '银行卡信息由 Stripe 安全处理与加密传输，本站不读取或存储卡号。'
          "
        />
        <el-alert
          v-else
          type="warning"
          show-icon
          :closable="false"
          title="模拟支付"
          :description="
            site.paymentNotice || '当前为模拟支付，不会产生真实扣款；成功状态由服务端确认。'
          "
        />

        <div
          v-if="isStripePayment && paymentStatus !== 'FAILED' && paymentStatus !== 'EXPIRED'"
          class="stripe-payment"
        >
          <p v-if="stripeIntentLoading" class="stripe-payment-status">正在初始化安全支付…</p>
          <template v-else-if="stripeIntentError">
            <el-alert type="error" :title="stripeIntentError" show-icon :closable="false" />
            <el-button class="stripe-retry-action" @click="setupStripePayment">
              重新初始化支付
            </el-button>
          </template>
          <template v-else>
            <span class="stripe-card-label">银行卡信息</span>
            <div ref="stripeCardContainer" class="stripe-card-element"></div>
            <p v-if="stripeCardError" class="stripe-card-error">{{ stripeCardError }}</p>
            <div class="stripe-payment-actions">
              <el-button
                type="primary"
                size="large"
                :loading="paymentLoading"
                :disabled="!stripeReady || !stripeCardComplete"
                @click="handleStripePayment"
              >
                确认支付 {{ formatMoney(checkout.amount, checkout.currency) }}
              </el-button>
              <el-button :disabled="paymentLoading" @click="handleRefreshPaymentStatus">
                刷新支付状态
              </el-button>
            </div>
          </template>
        </div>
        <div
          v-else-if="canBookOnline && paymentStatus !== 'FAILED' && paymentStatus !== 'EXPIRED'"
          class="simulation-actions"
        >
          <el-button
            type="success"
            size="large"
            :loading="paymentLoading"
            @click="handleSimulatedPayment"
          >
            确认模拟支付成功
          </el-button>
        </div>
        <el-alert
          v-if="paymentStatus === 'FAILED'"
          class="booking-alert"
          type="error"
          :title="
            paymentResult?.failureReason ||
            checkout.failureReason ||
            '支付尝试失败，服务端已更新预订状态'
          "
          show-icon
          :closable="false"
        />
        <el-alert
          v-if="paymentStatus === 'EXPIRED'"
          class="booking-alert"
          type="warning"
          title="支付尝试已过期，服务端已释放本次房量保留"
          show-icon
          :closable="false"
        />
        <el-button
          v-if="paymentStatus === 'FAILED' || paymentStatus === 'EXPIRED'"
          class="restart-booking-action"
          @click="resetBooking"
        >
          重新查询并预订
        </el-button>
        <el-alert
          v-if="paymentError"
          class="booking-alert"
          type="error"
          :title="paymentError"
          show-icon
          :closable="false"
        />
        <p v-if="checkout.expiresAt" class="hold-expiry">
          房间保留至 {{ formatDateTime(checkout.expiresAt) }}。
        </p>
      </template>
    </section>
  </section>
</template>

<style scoped>
.booking-section {
  padding: 84px max(20px, calc((100vw - 1120px) / 2)) 96px;
  color: #202b28;
  background: #eef2f0;
  scroll-margin-top: 70px;
}

.booking-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 30px;
}

.booking-heading span,
.result-heading span,
.checkout-heading > span,
.payment-panel > span,
.payment-heading span {
  color: #b47b46;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.17em;
}

.booking-heading h2 {
  margin: 8px 0 0;
  color: var(--site-primary, #214e46);
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(34px, 5vw, 52px);
  font-weight: 500;
}

.booking-progress {
  display: flex;
  gap: 7px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.booking-progress li {
  padding: 7px 11px;
  border-radius: 999px;
  color: #89918f;
  background: #dfe5e2;
  font-size: 11px;
  font-weight: 700;
}

.booking-progress li.active {
  color: #fff;
  background: #356f64;
}

.search-panel {
  display: grid;
  grid-template-columns: 1.25fr 1.25fr repeat(3, minmax(90px, 0.7fr)) auto;
  gap: 12px;
  align-items: end;
  padding: 22px;
  border: 1px solid #dce4e1;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 16px 48px rgba(35, 62, 56, 0.08);
}

.search-panel :deep(.el-form-item) {
  margin-bottom: 0;
}

.search-action {
  min-height: 40px;
  margin-bottom: 1px;
}

.booking-alert {
  margin-top: 18px;
}

.availability-results,
.checkout-panel,
.payment-panel {
  margin-top: 56px;
  scroll-margin-top: 96px;
}

.result-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.result-heading h3,
.checkout-heading h3,
.payment-panel h3 {
  margin: 7px 0 0;
  color: var(--site-primary, #214e46);
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 32px;
  font-weight: 500;
}

.result-heading p {
  margin: 0;
  color: #6f7976;
  font-size: 13px;
}

.room-quote-list {
  display: grid;
  gap: 16px;
}

.room-quote-card {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #dae2df;
  border-radius: 18px;
  background: #fff;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.room-quote-card.selected {
  border-color: #4d8a7f;
  box-shadow: 0 14px 42px rgba(38, 103, 91, 0.12);
}

.room-image-shell,
.room-image-placeholder {
  min-height: 210px;
  background: #315f57;
}

.room-image-shell img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.room-image-placeholder {
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 70% 25%, var(--site-accent, #d19a66) 0 9%, transparent 9.5%),
    linear-gradient(145deg, #376b61, #1e4941);
}

.room-image-placeholder span {
  width: 120px;
  height: 70px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 60px 60px 10px 10px;
}

.room-quote-copy {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
  padding: 26px;
}

.room-quote-copy h4 {
  margin: 0 0 9px;
  color: var(--site-primary, #214e46);
  font-size: 21px;
}

.room-quote-copy p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0 0 14px;
  color: #6d7774;
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.room-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-meta span {
  padding: 5px 9px;
  border-radius: 999px;
  color: #42635d;
  background: #eef5f3;
  font-size: 11px;
  font-weight: 650;
}

.room-price {
  display: flex;
  min-width: 150px;
  flex-direction: column;
  align-items: flex-end;
}

.room-price small {
  color: #7d8583;
}

.room-price strong {
  margin: 4px 0 14px;
  color: var(--site-primary, #214e46);
  font-size: 22px;
}

.room-price button {
  padding: 10px 16px;
  border: 1px solid var(--site-primary, #214e46);
  border-radius: 999px;
  color: #fff;
  background: var(--site-primary, #214e46);
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
}

.room-price button:disabled {
  border-color: #cbd2d0;
  color: #89908e;
  background: #e8eceb;
  cursor: not-allowed;
}

.room-price button:focus-visible {
  outline: 3px solid rgba(209, 154, 102, 0.7);
  outline-offset: 3px;
}

.quote-expiry,
.hold-expiry {
  margin: 13px 0 0;
  color: #7b8381;
  font-size: 12px;
  text-align: right;
}

.checkout-panel {
  padding: 32px;
  border: 1px solid #dae2df;
  border-radius: 20px;
  background: #fff;
}

.checkout-heading p {
  margin: 7px 0 0;
  color: #727c79;
  font-size: 13px;
}

.checkout-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: 34px;
  margin-top: 28px;
}

.guest-name-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.consent-copy {
  margin: 0 0 18px;
  color: #747d7a;
  font-size: 12px;
  line-height: 1.6;
}

.consent-copy a {
  color: #28695e;
}

.payment-disabled-alert {
  margin-bottom: 16px;
}

.booking-summary {
  align-self: start;
  padding: 22px;
  border-radius: 16px;
  color: #ecf5f2;
  background: var(--site-primary, #214e46);
}

.booking-summary > span {
  margin-left: 7px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.booking-summary h4 {
  margin: 18px 0;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 24px;
  font-weight: 500;
}

.booking-summary dl {
  margin: 0;
}

.booking-summary dl > div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.14);
}

.booking-summary dt {
  color: rgba(255, 255, 255, 0.67);
  font-size: 12px;
}

.booking-summary dd {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  text-align: right;
}

.booking-summary small {
  display: block;
  margin-top: 18px;
  color: rgba(255, 255, 255, 0.58);
  line-height: 1.55;
}

.payment-panel {
  padding: 34px;
  border: 1px solid #d9e2df;
  border-radius: 20px;
  background: #fff;
}

.payment-heading {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  margin-bottom: 24px;
}

.payment-heading > .el-icon {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  color: var(--site-primary, #214e46);
  background: #eaf3f0;
  font-size: 20px;
}

.payment-heading h3 {
  margin: 4px 0;
}

.payment-heading p {
  margin: 0;
  color: #77807e;
  font-size: 12px;
}

.payment-heading > strong {
  color: var(--site-primary, #214e46);
  font-size: 24px;
}

.simulation-actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.stripe-payment {
  margin-top: 20px;
}

.stripe-payment-status {
  margin: 0;
  color: #5f6b67;
  font-size: 13px;
}

.stripe-retry-action {
  margin-top: 12px;
}

.stripe-card-label {
  display: block;
  margin-bottom: 8px;
  color: #4b5753;
  font-size: 12px;
  font-weight: 700;
}

.stripe-card-element {
  padding: 13px 14px;
  border: 1px solid #d3dcd9;
  border-radius: 12px;
  background: #fff;
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.stripe-card-element:focus-within {
  border-color: var(--site-primary, #214e46);
  box-shadow: 0 0 0 3px rgba(33, 78, 70, 0.12);
}

.stripe-card-error {
  margin: 8px 0 0;
  color: #c4564f;
  font-size: 12px;
}

.stripe-payment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.restart-booking-action {
  margin-top: 18px;
}

.success-mark {
  display: grid;
  width: 64px;
  height: 64px;
  margin-bottom: 18px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #2d806e;
  font-size: 28px;
}

@media (prefers-reduced-motion: reduce) {
  * {
    scroll-behavior: auto !important;
  }
}

@media (max-width: 1000px) {
  .search-panel {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .search-action {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .booking-section {
    padding: 58px 14px 70px;
  }

  .booking-heading,
  .result-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .booking-progress {
    overflow-x: auto;
    width: 100%;
  }

  .search-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    padding: 16px;
  }

  .search-action {
    grid-column: 1 / -1;
  }

  .room-quote-card {
    grid-template-columns: 1fr;
  }

  .room-image-shell,
  .room-image-placeholder {
    min-height: 230px;
  }

  .room-quote-copy,
  .checkout-layout,
  .payment-heading {
    grid-template-columns: 1fr;
  }

  .room-price {
    align-items: flex-start;
  }

  .checkout-panel,
  .payment-panel {
    padding: 22px 18px;
  }

  .payment-heading > strong {
    justify-self: start;
  }
}

@media (max-width: 480px) {
  .search-panel,
  .guest-name-grid {
    grid-template-columns: 1fr;
  }

  .simulation-actions,
  .stripe-payment-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
