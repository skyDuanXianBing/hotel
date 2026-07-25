<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowRight, RefreshRight } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import {
  getPublicIndependentSite,
  getPublicIndependentSitePage,
  listIndependentSites,
} from '@/api/independentSite'
import type {
  IndependentSitePageFormat,
  IndependentSitePageSchema,
  PublicIndependentSitePageNavItem,
  PublicIndependentSite,
} from '@/types/independentSite'
import BookingFlow from '@/views/independent-site/booking/BookingFlow.vue'
import CanvasRenderer from '@/views/independent-site/canvas/CanvasRenderer.vue'
import IndependentSitePageRenderer from '@/views/independent-site/components/IndependentSitePageRenderer.vue'
import {
  canvasTreeHasSlot,
  collectCanvasNodeIds,
  createCanvasSlot,
  isCanvasElementNode,
  normalizeCanvasSchema,
  normalizeIndependentSitePageFormat,
  type IndependentSiteCanvasSchema,
} from '@/views/independent-site/canvasSchema'
import {
  buildIndependentSiteCssVars,
  normalizeIndependentSiteThemeKey,
} from '@/views/independent-site/themes'
import { normalizeIndependentSiteSchema } from '@/views/independent-site/pageSchema'
import { PMS_CURRENT_STORE_KEY, PMS_TOKEN_KEY } from '@/utils/cleanerSession'

const hasStoredManagementSession = () => {
  if (typeof window === 'undefined') {
    return false
  }
  const token = localStorage.getItem(PMS_TOKEN_KEY)
  const currentStore = localStorage.getItem(PMS_CURRENT_STORE_KEY)
  if (!token || !currentStore) {
    return false
  }
  try {
    const storeId = Number((JSON.parse(currentStore) as { id?: unknown }).id)
    return Number.isInteger(storeId) && storeId > 0
  } catch {
    return false
  }
}

const route = useRoute()
const siteLoading = ref(true)
const siteError = ref('')
const site = ref<PublicIndependentSite | null>(null)
const activePageSchema = ref<IndependentSitePageSchema | null>(null)
// 当前页面格式（缺省 BLOCKS）与 CANVAS 页面归一化后的节点树；BLOCKS 页面二者为 BLOCKS/null
const activePageFormat = ref<IndependentSitePageFormat>('BLOCKS')
const activeCanvasSchema = ref<IndependentSiteCanvasSchema | null>(null)
const pageLoading = ref(false)
const pageError = ref('')
// 传给页底订房流程的默认选中房型：HOME 页为 null，ROOM_DETAIL 子页取页面响应的 roomTypeId
const currentRoomTypeId = ref<number | null>(null)
const managementSessionPresent = ref(hasStoredManagementSession())
const managementPreviewAuthorized = ref(false)
const previewAuthorizationLoading = ref(false)
const previewAuthorizationError = ref('')
let siteRequestSequence = 0
let previewAuthorizationSequence = 0
let pageRequestSequence = 0

const slug = computed(() => String(route.params.slug || '').trim())
// 当前页面路径：/stay/:slug → '/'（HOME）；/stay/:slug/p/a/b → '/a/b'
const currentPagePath = computed(() => {
  const raw = route.params.pagePath
  const segments = (Array.isArray(raw) ? raw : raw ? [raw] : [])
    .map((segment) => String(segment).trim())
    .filter(Boolean)
  return segments.length ? `/${segments.join('/')}` : '/'
})
const previewRequested = computed(() => {
  const preview = route.query.preview
  return Array.isArray(preview) ? preview.includes('1') : preview === '1'
})
// CANVAS 页面所见 100% 由节点树负责：应用侧固定 chrome（header/page-nav/footer/页尾 BookingFlow）不渲染
const isCanvasPage = computed(() => activePageFormat.value === 'CANVAS')
const managementPreviewAvailable = computed(
  () => previewRequested.value && managementPreviewAuthorized.value,
)

const siteThemeKey = computed(() => normalizeIndependentSiteThemeKey(site.value?.themeKey))

const safeSchema = computed(() =>
  activePageSchema.value ? normalizeIndependentSiteSchema(activePageSchema.value) : null,
)

// 站点级主题变量注入页面根元素，页面导航等渲染器之外的区域也可消费
const siteCssVars = computed(() =>
  buildIndependentSiteCssVars(siteThemeKey.value, safeSchema.value?.theme),
)

const pageNavItems = computed<PublicIndependentSitePageNavItem[]>(() =>
  Array.isArray(site.value?.pages) ? site.value.pages : [],
)

const pageNavLink = (item: PublicIndependentSitePageNavItem) => {
  const tail = item.path
    .split('/')
    .filter(Boolean)
    .map((segment) => encodeURIComponent(segment))
    .join('/')
  const base = tail
    ? `/stay/${encodeURIComponent(slug.value)}/p/${tail}`
    : `/stay/${encodeURIComponent(slug.value)}`
  return {
    path: base,
    query: previewRequested.value ? { preview: '1' } : {},
  }
}

const homeNavLink = computed(() => ({
  path: `/stay/${encodeURIComponent(slug.value)}`,
  query: previewRequested.value ? { preview: '1' } : {},
}))

const termsUrl = computed(() => safeLegalUrl(undefined, '/legal/terms'))
const privacyUrl = computed(() => safeLegalUrl(undefined, '/legal/privacy'))

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

const verifyManagementPreview = async () => {
  const sequence = ++previewAuthorizationSequence
  managementSessionPresent.value = hasStoredManagementSession()
  managementPreviewAuthorized.value = false
  previewAuthorizationError.value = ''
  previewAuthorizationLoading.value = false

  if (!previewRequested.value) {
    return
  }
  if (!managementSessionPresent.value) {
    previewAuthorizationError.value = '未检测到有效的 PMS 登录与门店上下文'
    return
  }

  const requestedSlug = slug.value
  previewAuthorizationLoading.value = true
  try {
    const response = await listIndependentSites()
    if (
      sequence !== previewAuthorizationSequence ||
      requestedSlug !== slug.value ||
      !previewRequested.value
    ) {
      return
    }
    if (!response.success || !Array.isArray(response.data)) {
      throw new Error(response.message || '无法验证当前门店的独立站配置')
    }
    // 多站点后预览鉴权：slug 属于当前门店任一站点即可
    if (!response.data.some((item) => item.slug === requestedSlug)) {
      throw new Error('当前登录门店与此独立站不匹配')
    }
    managementPreviewAuthorized.value = true
  } catch (error) {
    if (
      sequence !== previewAuthorizationSequence ||
      requestedSlug !== slug.value ||
      !previewRequested.value
    ) {
      return
    }
    previewAuthorizationError.value = getErrorMessage(error, '管理测试预览验证失败')
  } finally {
    if (sequence === previewAuthorizationSequence) {
      previewAuthorizationLoading.value = false
    }
  }
}

const handleManagementSessionStorageChange = (event: StorageEvent) => {
  if (event.key === PMS_TOKEN_KEY || event.key === PMS_CURRENT_STORE_KEY) {
    verifyManagementPreview()
  }
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

const loadSite = async () => {
  const requestSequence = ++siteRequestSequence
  const requestedSlug = slug.value
  pageRequestSequence += 1
  siteLoading.value = true
  pageLoading.value = false
  siteError.value = ''
  pageError.value = ''
  site.value = null
  activePageSchema.value = null
  activePageFormat.value = 'BLOCKS'
  activeCanvasSchema.value = null
  currentRoomTypeId.value = null

  if (!requestedSlug || requestedSlug.length > 80) {
    siteError.value = '此订房页面不可用，请检查链接后重试。'
    siteLoading.value = false
    return
  }

  try {
    const response = await getPublicIndependentSite(requestedSlug)
    if (requestSequence !== siteRequestSequence || requestedSlug !== slug.value) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || '订房页面不可用')
    }
    // HOME 页 schema 结构随 format 变化，按对应契约归一化（缺省 BLOCKS，旧站点零变化）
    const homeFormat = normalizeIndependentSitePageFormat(response.data.format)
    const homeSchemaValid =
      homeFormat === 'CANVAS'
        ? Boolean(normalizeCanvasSchema(response.data.pageSchema))
        : Boolean(normalizeIndependentSiteSchema(response.data.pageSchema))
    if (!homeSchemaValid) {
      throw new Error('页面配置暂不可用')
    }
    site.value = {
      ...response.data,
      currency: response.data.currency || 'CNY',
      roomTypes: response.data.roomTypes || [],
      rooms: response.data.rooms || [],
    }
  } catch {
    if (requestSequence !== siteRequestSequence || requestedSlug !== slug.value) {
      return
    }
    siteError.value = '此订房页面不存在、尚未启用或暂时无法访问。请稍后重试。'
  } finally {
    if (requestSequence === siteRequestSequence && requestedSlug === slug.value) {
      siteLoading.value = false
    }
  }

  if (site.value && requestSequence === siteRequestSequence && requestedSlug === slug.value) {
    await loadPageContent()
  }
}

// 加载当前路由对应的页面 schema：HOME 直接用站点响应里的 pageSchema，子页调 /pages/{path}
const loadPageContent = async () => {
  const loadedSite = site.value
  if (!loadedSite) {
    return
  }
  const requestSequence = ++pageRequestSequence
  const requestedSlug = slug.value
  const requestedPath = currentPagePath.value
  const isCurrentPage = () =>
    requestSequence === pageRequestSequence &&
    requestedSlug === slug.value &&
    requestedPath === currentPagePath.value &&
    site.value === loadedSite

  // 订房流程状态由 BookingFlow 随 :key 重建自行重置，这里只重置页面层状态
  pageError.value = ''
  currentRoomTypeId.value = null

  if (requestedPath === '/') {
    // HOME：format 来自站点响应；CANVAS 走节点树归一化，BLOCKS 走旧区块归一化
    activePageFormat.value = normalizeIndependentSitePageFormat(loadedSite.format)
    if (activePageFormat.value === 'CANVAS') {
      const canvasSchema = normalizeCanvasSchema(loadedSite.pageSchema)
      activeCanvasSchema.value = canvasSchema ? ensureCanvasBookingFlowSlot(canvasSchema) : null
      activePageSchema.value = null
    } else {
      activeCanvasSchema.value = null
      activePageSchema.value = normalizeIndependentSiteSchema(loadedSite.pageSchema)
    }
    pageLoading.value = false
    return
  }

  pageLoading.value = true
  activePageSchema.value = null
  activeCanvasSchema.value = null
  try {
    const response = await getPublicIndependentSitePage(requestedSlug, requestedPath)
    if (!isCurrentPage()) {
      return
    }
    if (!response.success || !response.data) {
      throw new Error(response.message || '页面不可用')
    }
    activePageFormat.value = normalizeIndependentSitePageFormat(response.data.format)
    if (activePageFormat.value === 'CANVAS') {
      const canvasSchema = normalizeCanvasSchema(response.data.schema)
      if (!canvasSchema) {
        throw new Error('页面配置暂不可用')
      }
      activeCanvasSchema.value = ensureCanvasBookingFlowSlot(canvasSchema)
    } else {
      const schema = normalizeIndependentSiteSchema(response.data.schema)
      if (!schema) {
        throw new Error('页面配置暂不可用')
      }
      activePageSchema.value = schema
    }
    // ROOM_DETAIL 页把该页房型传给订房流程做默认选中；其余类型后端不返回 roomTypeId
    currentRoomTypeId.value = response.data.roomTypeId ?? null
  } catch {
    if (!isCurrentPage()) {
      return
    }
    activePageSchema.value = null
    activeCanvasSchema.value = null
    pageError.value = '此页面不存在、尚未发布或暂时无法访问。'
  } finally {
    if (isCurrentPage()) {
      pageLoading.value = false
    }
  }
}

// CANVAS 公开渲染兜底：旧已发布树（canvas-qa 等）没有 booking-flow 插槽时，
// 在 root.children 末尾自动补一个，保证预订区不消失；只影响公开渲染，不回写草稿
const ensureCanvasBookingFlowSlot = (
  schema: IndependentSiteCanvasSchema,
): IndependentSiteCanvasSchema => {
  if (!isCanvasElementNode(schema.root) || canvasTreeHasSlot(schema.root, 'booking-flow')) {
    return schema
  }
  const id = collectCanvasNodeIds(schema.root).includes('slot-booking')
    ? undefined
    : 'slot-booking'
  return {
    ...schema,
    root: {
      ...schema.root,
      children: [...(schema.root.children ?? []), createCanvasSlot('booking-flow', { id })],
    },
  }
}

const scrollToBooking = () => {
  document.querySelector('#booking')?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}

// CANVAS room-list 插槽点选房型：经 initial-room-type-id 预选（已出报价立即选中，未查询则查询后选中），并滚动到订房区
const handleSelectRoomType = (roomTypeId: number) => {
  currentRoomTypeId.value = roomTypeId
  scrollToBooking()
}

watch(
  slug,
  () => {
    loadSite()
  },
  { immediate: true },
)

watch(currentPagePath, () => {
  if (site.value) {
    loadPageContent()
  }
})

watch([previewRequested, slug], verifyManagementPreview, { immediate: true })

onMounted(() => window.addEventListener('storage', handleManagementSessionStorageChange))
onBeforeUnmount(() => window.removeEventListener('storage', handleManagementSessionStorageChange))
</script>

<template>
  <main class="public-site" :style="siteCssVars">
    <div v-if="siteLoading" class="site-state" aria-live="polite">
      <div class="loading-mark" aria-hidden="true"></div>
      <h1>正在加载订房页面</h1>
      <p>正在读取站点配置与发布内容…</p>
    </div>

    <div v-else-if="siteError || !site" class="site-state site-state--error">
      <span class="state-code">STAY</span>
      <h1>此页面暂不可用</h1>
      <p>{{ siteError }}</p>
      <el-button :icon="RefreshRight" @click="loadSite">重新加载</el-button>
    </div>

    <template v-else>
      <div
        v-if="previewRequested"
        class="management-preview-banner"
        :class="{ 'is-unavailable': !managementPreviewAvailable && !previewAuthorizationLoading }"
        role="status"
        aria-live="polite"
      >
        <strong>
          {{
            managementPreviewAvailable
              ? '管理测试预览'
              : previewAuthorizationLoading
                ? '正在验证管理测试预览'
                : '管理测试预览不可用'
          }}
        </strong>
        <span v-if="managementPreviewAvailable">
          当前页面来自已登录管理端；只有此模式可调用受保护的模拟支付接口。
        </span>
        <span v-else-if="previewAuthorizationLoading">
          正在核对登录门店与独立站，完成前不会创建房量保留或支付尝试。
        </span>
        <span v-else>
          {{
            previewAuthorizationError ||
            '请从已登录的独立站设置页重新打开。报价仍可查看，但下单和模拟支付保持禁用。'
          }}
        </span>
      </div>

      <header v-if="!isCanvasPage" class="public-header">
        <a class="brand-name" href="#" aria-label="返回页面顶部">{{ site.name }}</a>
        <nav aria-label="页面导航">
          <button type="button" @click="scrollToBooking">查询房间</button>
          <a :href="termsUrl">条款</a>
          <a :href="privacyUrl">隐私</a>
        </nav>
        <button type="button" class="header-booking-action" @click="scrollToBooking">
          立即预订
          <el-icon><ArrowRight /></el-icon>
        </button>
      </header>

      <nav v-if="!isCanvasPage && pageNavItems.length > 1" class="page-nav" aria-label="站点页面导航">
        <router-link
          v-for="item in pageNavItems"
          :key="item.path"
          :to="pageNavLink(item)"
          class="page-nav-link"
          :class="{ 'is-active': item.path === currentPagePath }"
        >
          {{ item.title }}
        </router-link>
      </nav>

      <div v-if="pageLoading" class="page-state" aria-live="polite">
        <div class="loading-mark" aria-hidden="true"></div>
        <p>正在加载页面…</p>
      </div>

      <div v-else-if="pageError" class="page-state" role="alert">
        <p>{{ pageError }}</p>
        <router-link class="page-state-link" :to="homeNavLink">返回首页</router-link>
      </div>

      <IndependentSitePageRenderer
        v-else-if="activePageFormat === 'BLOCKS' && safeSchema"
        :schema="safeSchema"
        :theme-key="siteThemeKey"
        @booking-request="scrollToBooking"
      />

      <CanvasRenderer
        v-else-if="activePageFormat === 'CANVAS' && activeCanvasSchema"
        :schema="activeCanvasSchema"
        :theme-key="siteThemeKey"
        :room-types="site.roomTypes"
        :booking-site="site"
        :booking-preview="previewRequested"
        :booking-preview-authorized="managementPreviewAuthorized"
        :booking-initial-room-type-id="currentRoomTypeId"
        :booking-key="`${slug}:${currentPagePath}`"
        @booking-request="scrollToBooking"
        @select-room-type="handleSelectRoomType"
      />

      <BookingFlow
        v-if="!isCanvasPage"
        :key="`${slug}:${currentPagePath}`"
        :site="site"
        :preview="previewRequested"
        :preview-authorized="managementPreviewAuthorized"
        :initial-room-type-id="currentRoomTypeId"
      />

      <footer v-if="!isCanvasPage" class="public-footer">
        <strong>{{ site.name }}</strong>
        <p>实时库存与安全预订由 THE HOST HUB 提供</p>
        <nav aria-label="页脚链接">
          <a :href="termsUrl">预订条款</a>
          <a :href="privacyUrl">隐私政策</a>
        </nav>
      </footer>
    </template>
  </main>
</template>

<style scoped>
.public-site {
  min-height: 100vh;
  color: #1f2a28;
  background: #f5f1e8;
}

.site-state {
  display: grid;
  min-height: 100vh;
  place-content: center;
  padding: 28px;
  text-align: center;
}

.site-state h1 {
  margin: 18px 0 6px;
  color: #214e46;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(34px, 6vw, 56px);
  font-weight: 500;
}

.site-state p {
  max-width: 520px;
  margin: 0 auto 22px;
  color: #6c7673;
  line-height: 1.7;
}

.loading-mark {
  width: 44px;
  height: 44px;
  margin: 0 auto;
  border: 3px solid rgba(33, 78, 70, 0.18);
  border-top-color: #214e46;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

.state-code {
  color: #c0854d;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.22em;
}

.management-preview-banner {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  color: #174c43;
  font-size: 13px;
  line-height: 1.5;
  text-align: center;
  background: #dff4ee;
  border-bottom: 1px solid #b5ddd2;
}

.management-preview-banner.is-unavailable {
  color: #795019;
  background: #fff2d8;
  border-bottom-color: #ebd3a7;
}

.public-header {
  position: sticky;
  z-index: 20;
  top: 0;
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 24px;
  align-items: center;
  min-height: 70px;
  padding: 0 max(24px, calc((100vw - 1120px) / 2));
  border-bottom: 1px solid rgba(31, 42, 40, 0.1);
  background: rgba(245, 241, 232, 0.92);
  backdrop-filter: blur(14px);
}

.brand-name {
  overflow: hidden;
  color: #214e46;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 20px;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.public-header nav,
.public-footer nav {
  display: flex;
  gap: 24px;
  align-items: center;
}

.public-header nav button,
.public-header nav a,
.public-footer nav a {
  padding: 0;
  border: 0;
  color: #485652;
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  text-decoration: none;
}

.header-booking-action {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  justify-self: end;
  padding: 10px 18px;
  border: 1px solid #214e46;
  border-radius: 999px;
  color: #fff;
  background: #214e46;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 700;
}

.page-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
  padding: 10px max(24px, calc((100vw - 1120px) / 2));
  border-bottom: 1px solid color-mix(in srgb, var(--site-text, #1f2a28) 10%, transparent);
  background: color-mix(in srgb, var(--site-surface, #fff) 90%, var(--site-primary, #214e46));
}

.page-nav-link {
  padding: 7px 14px;
  border-radius: 999px;
  color: var(--site-text, #485652);
  font-size: 13px;
  text-decoration: none;
  transition:
    color 0.2s,
    background-color 0.2s;
}

.page-nav-link:hover {
  color: var(--site-primary, #214e46);
}

.page-nav-link.is-active {
  color: #fff;
  background: var(--site-primary, #214e46);
}

.page-nav-link:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--site-accent, #d19a66) 70%, white);
  outline-offset: 2px;
}

.page-state {
  display: grid;
  min-height: 320px;
  place-content: center;
  justify-items: center;
  gap: 14px;
  padding: 40px 20px;
  text-align: center;
}

.page-state p {
  max-width: 480px;
  margin: 0;
  color: var(--site-text, #485652);
  line-height: 1.7;
}

.page-state-link {
  padding: 9px 18px;
  border: 1px solid var(--site-primary, #214e46);
  border-radius: 999px;
  color: var(--site-primary, #214e46);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.public-header button:focus-visible,
.public-header a:focus-visible {
  outline: 3px solid rgba(209, 154, 102, 0.7);
  outline-offset: 3px;
}

.public-footer {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 24px;
  align-items: center;
  min-height: 110px;
  padding: 24px max(20px, calc((100vw - 1120px) / 2));
  color: rgba(255, 255, 255, 0.78);
  background: #173c36;
}

.public-footer strong {
  color: #fff;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 18px;
}

.public-footer p {
  margin: 0;
  font-size: 11px;
}

.public-footer nav {
  justify-self: end;
}

.public-footer nav a {
  color: rgba(255, 255, 255, 0.72);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .loading-mark {
    animation: none;
  }

  * {
    scroll-behavior: auto !important;
  }
}

@media (max-width: 760px) {
  .public-header {
    grid-template-columns: minmax(0, 1fr) auto;
    padding: 0 16px;
  }

  .public-header nav {
    display: none;
  }

  .page-nav {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding: 8px 16px;
  }

  .page-nav-link {
    flex-shrink: 0;
  }

  .public-footer {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .public-footer nav {
    justify-self: center;
  }
}

@media (max-width: 480px) {
  .header-booking-action {
    padding: 9px 13px;
  }
}
</style>
