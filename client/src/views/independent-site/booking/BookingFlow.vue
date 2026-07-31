<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Calendar, Check, Lock, User } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
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
import { isStoreClosedError } from '@/utils/publicRequest'
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

const { locale, t } = useI18n()

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

// 门店权益失效暂停接单（P9）：信息型端点 closed=true 或交易端点 403 时切换维护态
const storeClosedByRequest = ref(false)
const siteClosed = computed(() => props.site.closed === true || storeClosedByRequest.value)

/** 交易请求命中 403「该店铺暂停接单」→ 切维护态并吞掉原错误（不再透传固定中文 message）。 */
const markClosedIfStoreClosed = (error: unknown): boolean => {
  if (isStoreClosedError(error)) {
    storeClosedByRequest.value = true
    return true
  }
  return false
}

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
  return t('independentSite.booking.unavailableTitle')
})

const bookingUnavailableDescription = computed(() => {
  return (
    props.site.paymentNotice ||
    t('independentSite.booking.unavailableDescription')
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
    return attempt.reservationOrderNumbers.join(t('independentSite.booking.referenceSeparator'))
  }
  return t('independentSite.booking.processing')
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
  checkInDate: [
    { required: true, message: t('independentSite.booking.validation.checkInRequired'), trigger: 'change' },
  ],
  checkOutDate: [
    { required: true, message: t('independentSite.booking.validation.checkOutRequired'), trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value && searchForm.checkInDate && String(value) <= String(searchForm.checkInDate)) {
          callback(new Error(t('independentSite.booking.validation.checkOutAfterCheckIn')))
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
          callback(new Error(t('independentSite.booking.validation.adultsPerRoom')))
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
    { required: true, message: t('independentSite.booking.validation.firstNameRequired'), trigger: 'blur' },
    { min: 1, max: 49, message: t('independentSite.booking.validation.firstNameLength'), trigger: 'blur' },
  ],
  lastName: [
    { required: true, message: t('independentSite.booking.validation.lastNameRequired'), trigger: 'blur' },
    { min: 1, max: 49, message: t('independentSite.booking.validation.lastNameLength'), trigger: 'blur' },
  ],
  email: [
    { required: true, message: t('independentSite.booking.validation.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('independentSite.booking.validation.emailInvalid'), trigger: ['blur', 'change'] },
  ],
  phone: [
    { required: true, message: t('independentSite.booking.validation.phoneRequired'), trigger: 'blur' },
    {
      pattern: /^[+()\-\s0-9]{6,30}$/,
      message: t('independentSite.booking.validation.phoneInvalid'),
      trigger: ['blur', 'change'],
    },
  ],
  specialRequests: [
    { max: 500, message: t('independentSite.booking.validation.requestsLength'), trigger: 'blur' },
  ],
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
    return new Intl.NumberFormat(locale.value, {
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
    : new Intl.DateTimeFormat(locale.value, {
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
    : new Intl.DateTimeFormat(locale.value, {
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
      throw new Error(t('independentSite.booking.errors.noPublishedRooms'))
    }

    const quoteResults = await Promise.allSettled(
      requestedSite.roomTypes.map(async (roomType: PublicIndependentSiteRoomType) => {
        const response = await getPublicIndependentSiteQuote(requestedSlug, {
          roomTypeId: roomType.id,
          ...searchSnapshot,
        })
        if (!response.success || !response.data) {
          throw new Error(
            response.message ||
              t('independentSite.booking.errors.roomQuoteUnavailable', { name: roomType.name }),
          )
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
      throw firstFailure?.reason || new Error(t('independentSite.booking.errors.quoteUnavailable'))
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
      quoteError.value = t('independentSite.booking.errors.partialQuote')
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
    if (markClosedIfStoreClosed(error)) {
      return
    }
    quoteError.value = getErrorMessage(error, t('independentSite.booking.errors.loadQuotes'))
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
    checkoutError.value = t('independentSite.booking.errors.selectRoom')
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
      throw new Error(response.message || t('independentSite.booking.errors.holdFailed'))
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
    if (markClosedIfStoreClosed(error)) {
      checkoutError.value = ''
    } else {
      checkoutError.value = getErrorMessage(error, t('independentSite.booking.errors.priceChanged'))
    }
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
    throw new Error(response.message || t('independentSite.booking.errors.statusFailed'))
  }
  paymentResult.value = response.data
}

const handleSimulatedPayment = async () => {
  if (!checkout.value || !canBookOnline.value) {
    paymentError.value = t('independentSite.booking.unavailableTitle')
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
      throw new Error(response.message || t('independentSite.booking.errors.simulatedPaymentFailed'))
    }
    paymentResult.value = response.data
    await refreshPaymentStatus()
  } catch (error) {
    if (markClosedIfStoreClosed(error)) {
      paymentError.value = ''
    } else {
      paymentError.value = getErrorMessage(error, t('independentSite.booking.errors.simulatedPaymentRetry'))
    }
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
      throw new Error(response.message || t('independentSite.booking.errors.stripeInit'))
    }
    const intent = response.data
    // attempt 已非 PENDING（如 15 分钟保留过期）：直接同步服务端最新状态，走现有终态提示
    if (intent.status && intent.status !== 'PENDING') {
      await refreshPaymentStatus()
      return
    }
    if (!intent.clientSecret || !intent.publishableKey) {
      throw new Error(t('independentSite.booking.errors.stripeResponse'))
    }
    const stripe = await loadStripe(intent.publishableKey)
    if (!stripe) {
      throw new Error(t('independentSite.booking.errors.stripeLoad'))
    }
    stripeInstance = stripe
    stripeClientSecret = intent.clientSecret
    await mountStripeCardElement()
  } catch (error) {
    if (markClosedIfStoreClosed(error)) {
      stripeIntentError.value = ''
    } else {
      stripeIntentError.value = getErrorMessage(error, t('independentSite.booking.errors.stripeRetry'))
    }
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
  paymentError.value = t('independentSite.booking.errors.paymentPending')
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
      paymentError.value = error.message || t('independentSite.booking.errors.cardPayment')
      return
    }
    await pollPaymentStatusUntilFinal()
  } catch (error) {
    paymentError.value = getErrorMessage(error, t('independentSite.booking.errors.confirmPayment'))
  } finally {
    paymentLoading.value = false
  }
}

const handleRefreshPaymentStatus = async () => {
  paymentError.value = ''
  try {
    await refreshPaymentStatus()
  } catch (error) {
    paymentError.value = getErrorMessage(error, t('independentSite.booking.errors.refreshPayment'))
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
    quoteError.value = t('independentSite.booking.errors.searchChanged')
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
    <!-- 门店权益失效暂停接单（P9）：closed=true 或交易 403 后整段切换为维护态 -->
    <div v-if="siteClosed" class="store-closed" role="status">
      <span class="store-closed-eyebrow">{{ t('independentSite.booking.headingEyebrow') }}</span>
      <h2>{{ t('independentSite.booking.storeClosedTitle') }}</h2>
      <p>{{ t('independentSite.booking.storeClosedDescription') }}</p>
    </div>

    <div v-if="!siteClosed" class="booking-heading">
      <div>
        <span>{{ t('independentSite.booking.headingEyebrow') }}</span>
        <h2>{{ t('independentSite.booking.heading') }}</h2>
      </div>
      <ol class="booking-progress" :aria-label="t('independentSite.booking.progress')">
        <li :class="{ active: currentStep >= 1 }">1 {{ t('independentSite.booking.stepSearch') }}</li>
        <li :class="{ active: currentStep >= 2 }">2 {{ t('independentSite.booking.stepDetails') }}</li>
        <li :class="{ active: currentStep >= 3 }">3 {{ t('independentSite.booking.stepPayment') }}</li>
        <li :class="{ active: currentStep >= 4 }">4 {{ t('independentSite.booking.stepComplete') }}</li>
      </ol>
    </div>

    <el-form
      v-if="!siteClosed"
      ref="searchFormRef"
      :model="searchForm"
      :rules="searchRules"
      label-position="top"
      class="search-panel"
      :aria-busy="quoteLoading"
    >
      <el-form-item :label="t('independentSite.booking.checkIn')" prop="checkInDate">
        <el-date-picker
          v-model="searchForm.checkInDate"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          :placeholder="t('independentSite.booking.selectDate')"
          :disabled-date="disableCheckInDate"
          :prefix-icon="Calendar"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('independentSite.booking.checkOut')" prop="checkOutDate">
        <el-date-picker
          v-model="searchForm.checkOutDate"
          type="date"
          value-format="YYYY-MM-DD"
          format="YYYY-MM-DD"
          :placeholder="t('independentSite.booking.selectDate')"
          :disabled-date="disableCheckOutDate"
          :prefix-icon="Calendar"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('independentSite.booking.adults')" prop="adults">
        <el-input-number
          v-model="searchForm.adults"
          :min="searchForm.roomCount"
          :max="20"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('independentSite.booking.children')">
        <el-input-number
          v-model="searchForm.children"
          :min="0"
          :max="20"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="t('independentSite.booking.rooms')">
        <el-input-number
          v-model="searchForm.roomCount"
          :min="1"
          :max="10"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>
      <el-button class="search-action" type="primary" :loading="quoteLoading" @click="handleSearch">
        {{ t('independentSite.booking.searchPrices') }}
      </el-button>
    </el-form>

    <el-alert
      v-if="quoteError && !siteClosed"
      class="booking-alert"
      type="warning"
      :title="quoteError"
      show-icon
      :closable="false"
    />

    <section v-if="quote && !siteClosed" id="availability-results" class="availability-results">
      <div class="result-heading">
        <div>
          <span>{{ t('independentSite.booking.resultsEyebrow') }}</span>
          <h3>
            {{ formatDate(quote.checkInDate || searchForm.checkInDate) }} –
            {{ formatDate(quote.checkOutDate || searchForm.checkOutDate) }}
          </h3>
        </div>
        <p>
          {{
            t(
              searchForm.children
                ? 'independentSite.booking.staySummary'
                : 'independentSite.booking.staySummaryWithoutChildren',
              {
                nights: numberOfNights,
                adults: searchForm.adults,
                children: searchForm.children,
                rooms: searchForm.roomCount,
              },
            )
          }}
        </p>
      </div>

      <el-empty
        v-if="quote.roomTypes.length === 0 || !hasBookableQuotes"
        :description="t('independentSite.booking.emptyResults')"
      />

      <div v-else class="room-quote-list">
        <article
          v-for="roomQuote in quote.roomTypes"
          :key="roomQuote.roomTypeId"
          class="room-quote-card"
          :class="{ selected: selectedRoomTypeId === roomQuote.roomTypeId }"
        >
          <div v-if="roomImage(roomQuote)" class="room-image-shell">
            <img
              :src="roomImage(roomQuote)"
              :alt="t('independentSite.booking.roomImageAlt', { name: roomQuote.roomTypeName })"
            />
          </div>
          <div v-else class="room-image-placeholder" aria-hidden="true">
            <span></span>
          </div>
          <div class="room-quote-copy">
            <div>
              <h4>{{ roomQuote.roomTypeName }}</h4>
              <p v-if="roomQuote.description">{{ roomQuote.description }}</p>
              <div class="room-meta">
                <span v-if="roomQuote.maxGuests">
                  {{ t('independentSite.booking.maxGuests', { count: roomQuote.maxGuests }) }}
                </span>
                <span>{{ t('independentSite.booking.availableRooms', { count: roomQuote.availableRooms }) }}</span>
              </div>
            </div>
            <div class="room-price">
              <small>{{ t('independentSite.booking.totalNights', { count: numberOfNights }) }}</small>
              <strong>{{ formatMoney(roomQuote.totalAmount, quote.currency) }}</strong>
              <button
                type="button"
                :disabled="roomQuote.availableRooms < searchForm.roomCount"
                @click="selectRoomQuote(roomQuote)"
              >
                {{
                  roomQuote.availableRooms < searchForm.roomCount
                    ? t('independentSite.booking.insufficientAvailability')
                    : selectedRoomTypeId === roomQuote.roomTypeId
                      ? t('independentSite.booking.selected')
                      : canBookOnline
                        ? t('independentSite.booking.selectRoomType')
                        : t('independentSite.booking.viewBookingNotice')
                }}
              </button>
            </div>
          </div>
        </article>
      </div>
      <p v-if="quote.expiresAt && hasBookableQuotes" class="quote-expiry">
        {{ t('independentSite.booking.quoteExpiry', { time: formatDateTime(quote.expiresAt) }) }}
      </p>
    </section>

    <section v-if="selectedQuote && !checkout && !siteClosed" id="guest-details" class="checkout-panel">
      <div class="checkout-heading">
        <span>{{ t(canBookOnline ? 'independentSite.booking.guestDetails' : 'independentSite.booking.bookingNotice') }}</span>
        <h3>{{ t(canBookOnline ? 'independentSite.booking.guestDetailsHeading' : 'independentSite.booking.quoteReady') }}</h3>
        <p v-if="canBookOnline">
          {{ t('independentSite.booking.selectedRoomNotice', { name: selectedQuote.roomTypeName }) }}
        </p>
        <p v-else>{{ t('independentSite.booking.quoteViewedNotice', { name: selectedQuote.roomTypeName }) }}</p>
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
            <el-form-item :label="t('independentSite.booking.firstName')" prop="firstName">
              <el-input v-model.trim="guestForm.firstName" autocomplete="given-name" />
            </el-form-item>
            <el-form-item :label="t('independentSite.booking.lastName')" prop="lastName">
              <el-input v-model.trim="guestForm.lastName" autocomplete="family-name" />
            </el-form-item>
          </div>
          <el-form-item :label="t('independentSite.booking.email')" prop="email">
            <el-input v-model.trim="guestForm.email" type="email" autocomplete="email" />
          </el-form-item>
          <el-form-item :label="t('independentSite.booking.phone')" prop="phone">
            <el-input v-model.trim="guestForm.phone" type="tel" autocomplete="tel" />
          </el-form-item>
          <el-form-item :label="t('independentSite.booking.specialRequests')" prop="specialRequests">
            <el-input
              v-model="guestForm.specialRequests"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <p class="consent-copy">
            {{ t('independentSite.booking.consentPrefix') }}
            <a :href="termsUrl" target="_blank" rel="noopener noreferrer">{{ t('independentSite.booking.terms') }}</a>
            {{ t('independentSite.booking.consentJoiner') }}
            <a :href="privacyUrl" target="_blank" rel="noopener noreferrer">{{ t('independentSite.booking.privacy') }}</a>{{ t('independentSite.booking.consentSuffix') }}
          </p>
          <el-button type="primary" size="large" :loading="checkoutLoading" @click="handleCheckout">
            {{ t('independentSite.booking.createHold') }}
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

        <aside class="booking-summary" :aria-label="t('independentSite.booking.summary')">
          <el-icon><User /></el-icon>
          <span>{{ t('independentSite.booking.summary') }}</span>
          <h4>{{ selectedQuote.roomTypeName }}</h4>
          <dl>
            <div>
              <dt>{{ t('independentSite.booking.summaryCheckIn') }}</dt>
              <dd>{{ formatDate(searchForm.checkInDate) }}</dd>
            </div>
            <div>
              <dt>{{ t('independentSite.booking.summaryCheckOut') }}</dt>
              <dd>{{ formatDate(searchForm.checkOutDate) }}</dd>
            </div>
            <div>
              <dt>{{ t('independentSite.booking.summaryRooms') }}</dt>
              <dd>{{ t('independentSite.booking.roomCount', { count: searchForm.roomCount }) }}</dd>
            </div>
            <div>
              <dt>{{ t('independentSite.booking.currentQuote') }}</dt>
              <dd>{{ formatMoney(selectedQuote.totalAmount, quote?.currency) }}</dd>
            </div>
          </dl>
          <small>{{ t('independentSite.booking.recalculationNote') }}</small>
        </aside>
      </div>
    </section>

    <section v-if="checkout && !siteClosed" id="payment-step" class="payment-panel">
      <template v-if="paymentStatus === 'SUCCEEDED'">
        <div class="success-mark" aria-hidden="true">
          <el-icon><Check /></el-icon>
        </div>
        <span>{{ t('independentSite.booking.confirmedEyebrow') }}</span>
        <h3>{{ t('independentSite.booking.confirmedTitle') }}</h3>
        <p>{{ t('independentSite.booking.reference', { reference: bookingReference }) }}</p>
        <p>{{ t('independentSite.booking.confirmedDescription') }}</p>
        <el-button @click="resetBooking">{{ t('independentSite.booking.bookAnother') }}</el-button>
      </template>

      <template v-else>
        <div class="payment-heading">
          <el-icon><Lock /></el-icon>
          <div>
            <span>{{ t('independentSite.booking.paymentEyebrow') }}</span>
            <h3>{{ t('independentSite.booking.paymentTitle') }}</h3>
            <p>{{ t('independentSite.booking.reference', { reference: bookingReference }) }}</p>
          </div>
          <strong>{{ formatMoney(checkout.amount, checkout.currency) }}</strong>
        </div>

        <el-alert
          v-if="!canBookOnline"
          type="error"
          show-icon
          :closable="false"
          :title="t('independentSite.booking.paymentClosedTitle')"
          :description="t('independentSite.booking.paymentClosedDescription')"
        />
        <el-alert
          v-else-if="isStripePayment"
          type="info"
          show-icon
          :closable="false"
          :title="t('independentSite.booking.cardPaymentTitle')"
          :description="
            site.paymentNotice || t('independentSite.booking.cardPaymentDescription')
          "
        />
        <el-alert
          v-else
          type="warning"
          show-icon
          :closable="false"
          :title="t('independentSite.booking.simulatedPaymentTitle')"
          :description="
            site.paymentNotice || t('independentSite.booking.simulatedPaymentDescription')
          "
        />

        <div
          v-if="isStripePayment && paymentStatus !== 'FAILED' && paymentStatus !== 'EXPIRED'"
          class="stripe-payment"
        >
          <p v-if="stripeIntentLoading" class="stripe-payment-status">
            {{ t('independentSite.booking.initializingCardPayment') }}
          </p>
          <template v-else-if="stripeIntentError">
            <el-alert type="error" :title="stripeIntentError" show-icon :closable="false" />
            <el-button class="stripe-retry-action" @click="setupStripePayment">
              {{ t('independentSite.booking.reinitializePayment') }}
            </el-button>
          </template>
          <template v-else>
            <span class="stripe-card-label">{{ t('independentSite.booking.cardDetails') }}</span>
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
                {{ t('independentSite.booking.confirmPayment', { amount: formatMoney(checkout.amount, checkout.currency) }) }}
              </el-button>
              <el-button :disabled="paymentLoading" @click="handleRefreshPaymentStatus">
                {{ t('independentSite.booking.refreshPayment') }}
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
            {{ t('independentSite.booking.confirmSimulatedPayment') }}
          </el-button>
        </div>
        <el-alert
          v-if="paymentStatus === 'FAILED'"
          class="booking-alert"
          type="error"
          :title="
            paymentResult?.failureReason ||
            checkout.failureReason ||
            t('independentSite.booking.paymentFailed')
          "
          show-icon
          :closable="false"
        />
        <el-alert
          v-if="paymentStatus === 'EXPIRED'"
          class="booking-alert"
          type="warning"
          :title="t('independentSite.booking.paymentExpired')"
          show-icon
          :closable="false"
        />
        <el-button
          v-if="paymentStatus === 'FAILED' || paymentStatus === 'EXPIRED'"
          class="restart-booking-action"
          @click="resetBooking"
        >
          {{ t('independentSite.booking.restartBooking') }}
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
          {{ t('independentSite.booking.holdExpiry', { time: formatDateTime(checkout.expiresAt) }) }}
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

.store-closed {
  padding: 72px 24px;
  border: 1px solid #dce4e1;
  border-radius: 18px;
  background: #fff;
  text-align: center;
}

.store-closed-eyebrow {
  color: #b47b46;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.17em;
}

.store-closed h2 {
  margin: 10px 0 8px;
  color: var(--site-primary, #214e46);
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(26px, 4vw, 38px);
  font-weight: 500;
}

.store-closed p {
  max-width: 520px;
  margin: 0 auto;
  color: #6f7976;
  font-size: 14px;
  line-height: 1.7;
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
