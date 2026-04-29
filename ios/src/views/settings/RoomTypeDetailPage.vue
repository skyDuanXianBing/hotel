<template>
  <ion-page>
    <ion-header translucent>
      <ion-toolbar class="app-page-header__toolbar">
        <ion-buttons slot="start">
          <ion-back-button class="app-page-header__back-btn" :default-href="ROUTE_PATHS.settingsRoomTypes" />
        </ion-buttons>
        <ion-title class="app-page-header__title">房型详情</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content fullscreen class="mobile-page room-type-detail-page">
      <ion-refresher slot="fixed" @ionRefresh="handleRefresh">
        <ion-refresher-content pulling-text="下拉刷新详情" refreshing-spinner="crescent" />
      </ion-refresher>

      <section class="mobile-hero room-type-detail-hero">
        <p class="mobile-note room-type-detail-hero__eyebrow">房型详情</p>
        <h1 class="mobile-title">{{ roomType?.name || '房型详情' }}</h1>
        <div class="mobile-chip-row">
          <span class="mobile-chip">{{ shortNameText }}</span>
          <span class="mobile-chip">{{ getBasePriceText(roomType || emptyRoomType) }}</span>
          <span class="mobile-chip">房间 {{ roomNumbers.length }}</span>
        </div>
      </section>

      <div class="mobile-stack">
        <section class="mobile-card room-type-detail-card">
          <div class="mobile-inline-row room-type-detail-card__header">
            <div>
              <h2 class="mobile-section-title">基本信息</h2>
            </div>
            <ion-spinner v-if="loading" name="crescent" />
          </div>

          <div v-if="roomType" class="detail-info-grid">
            <div class="detail-info-item">
              <span>房型名称</span>
              <strong>{{ roomType.name }}</strong>
            </div>
            <div class="detail-info-item">
              <span>简称</span>
              <strong>{{ shortNameText }}</strong>
            </div>
            <div class="detail-info-item">
              <span>房型代码</span>
              <strong>{{ roomType.code }}</strong>
            </div>
            <div class="detail-info-item">
              <span>最大入住人数</span>
              <strong>{{ roomType.maxGuests || 1 }} 人</strong>
            </div>
            <div class="detail-info-item">
              <span>儿童最大入住</span>
              <strong>{{ roomType.maxChildOccupancy ?? 0 }} 人</strong>
            </div>
            <div class="detail-info-item detail-info-item--full">
              <span>房型地址</span>
              <strong>{{ roomType.roomTypeAddress || '未设置' }}</strong>
            </div>
            <div class="detail-info-item detail-info-item--full">
              <span>附近车站</span>
              <strong>{{ roomType.nearbyStation || '未设置' }}</strong>
            </div>
            <div class="detail-info-item">
              <span>面积</span>
              <strong>{{ sizeText }}</strong>
            </div>
            <div class="detail-info-item detail-info-item--full">
              <span>房型描述</span>
              <p>{{ roomDescriptionText }}</p>
            </div>
            <div class="detail-info-item detail-info-item--full">
              <span>入住指南</span>
              <a v-if="roomType.checkInGuideLink" :href="roomType.checkInGuideLink" target="_blank" rel="noreferrer">
                {{ roomType.checkInGuideLink }}
              </a>
              <strong v-else>未设置</strong>
            </div>
          </div>
        </section>

        <section class="mobile-card room-type-detail-card">
          <div>
            <h2 class="mobile-section-title">价格信息</h2>
          </div>

          <div class="detail-price-grid">
            <div class="detail-price-item">
              <span>默认价</span>
              <strong>{{ formatPriceValue(roomType?.defaultPrice) }}</strong>
            </div>
            <div v-for="field in ROOM_TYPE_DAILY_PRICE_FIELDS" :key="field.key" class="detail-price-item">
              <span>{{ field.label }}</span>
              <strong>{{ formatPriceValue(resolveDailyPrice(roomType || emptyRoomType, field.key)) }}</strong>
            </div>
          </div>
        </section>

        <section class="mobile-card room-type-detail-card">
          <div>
            <h2 class="mobile-section-title">设施与房间号</h2>
          </div>

          <div class="detail-tags">
            <span v-for="facility in facilities" :key="facility" class="detail-tag">{{ facility }}</span>
            <span v-if="facilities.length === 0" class="mobile-note">未设置设施信息</span>
          </div>

          <div class="detail-tags detail-tags--rooms">
            <span v-for="roomNumber in roomNumbers" :key="roomNumber" class="detail-tag detail-tag--room">
              {{ roomNumber }}
            </span>
            <span v-if="roomNumbers.length === 0" class="mobile-note">未设置房间号</span>
          </div>
        </section>

        <section class="mobile-card room-type-detail-card">
          <div>
            <h2 class="mobile-section-title">图片信息</h2>
          </div>

          <div v-if="photoUrls.length > 0" class="detail-photo-grid">
            <figure v-for="photoUrl in photoUrls" :key="photoUrl" class="detail-photo-item">
              <img :src="photoUrl" :alt="roomType?.name || '房型图片'" loading="lazy" />
              <figcaption>{{ photoUrl }}</figcaption>
            </figure>
          </div>
          <p v-else class="mobile-note">未设置图片信息</p>
        </section>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import {
  IonBackButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonPage,
  IonRefresher,
  IonRefresherContent,
  IonSpinner,
  IonTitle,
  IonToolbar,
  onIonViewWillEnter,
} from '@ionic/vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getAllRoomTypesWithRooms,
  getRoomTypeById,
  type RoomTypeDTO,
} from '@/api/roomType'
import { ROUTE_PATHS } from '@/router/guards'
import { showWarningToast } from '@/utils/notify'
import {
  formatPriceValue,
  getBasePriceText,
  mergeRoomTypePhotoUrls,
  resolveDailyPrice,
  resolveRoomTypeLongDescription,
  resolveRoomTypeShortName,
  ROOM_TYPE_DAILY_PRICE_FIELDS,
} from '@/utils/roomType'
import { isHandledRequestError } from '@/utils/request'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const roomType = ref<RoomTypeDTO | null>(null)
const roomNumbers = ref<string[]>([])

const emptyRoomType: RoomTypeDTO = {
  id: 0,
  name: '',
  code: '',
  totalRooms: 0,
  createdAt: '',
  updatedAt: '',
}

const shortNameText = computed(() => {
  if (!roomType.value) {
    return '简称待设置'
  }

  const shortName = resolveRoomTypeShortName(roomType.value)
  if (!shortName) {
    return '简称待设置'
  }

  return shortName
})

const roomDescriptionText = computed(() => {
  if (!roomType.value) {
    return '未设置描述'
  }

  const description = resolveRoomTypeLongDescription(roomType.value)
  if (!description) {
    return '未设置描述'
  }

  return description
})

const sizeText = computed(() => {
  if (roomType.value?.sizeMeasurement === undefined || roomType.value?.sizeMeasurement === null) {
    return '未设置'
  }

  const unit = roomType.value.sizeMeasurementUnit || ''
  if (!unit) {
    return `${roomType.value.sizeMeasurement}`
  }

  return `${roomType.value.sizeMeasurement} ${unit}`
})

const facilities = computed(() => {
  if (!roomType.value?.facilities || roomType.value.facilities.length === 0) {
    return [] as string[]
  }

  const items: string[] = []
  for (const facility of roomType.value.facilities) {
    const group = facility.group?.trim()
    const name = facility.name?.trim()
    if (!name) {
      continue
    }

    if (!group) {
      items.push(name)
      continue
    }

    items.push(`${group}: ${name}`)
  }
  return items
})

const photoUrls = computed(() => {
  if (!roomType.value) {
    return [] as string[]
  }

  return mergeRoomTypePhotoUrls(roomType.value)
})

function resolveWarningMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

async function loadRoomTypeDetails() {
  const roomTypeId = Number(route.params.id)
  if (!roomTypeId) {
    showWarningToast('缺少房型 ID，已返回房型列表')
    await router.replace(ROUTE_PATHS.settingsRoomTypes)
    return
  }

  loading.value = true
  try {
    const [detailResponse, roomListResponse] = await Promise.all([
      getRoomTypeById(roomTypeId),
      getAllRoomTypesWithRooms(),
    ])

    if (!detailResponse.success || !detailResponse.data) {
      throw new Error(detailResponse.message || '加载房型详情失败')
    }

    roomType.value = detailResponse.data

    if (roomListResponse.success && roomListResponse.data) {
      const matchedRoomType = roomListResponse.data.find((item) => item.id === roomTypeId)
      roomNumbers.value = matchedRoomType?.rooms?.map((room) => room.roomNumber) || []
    } else {
      roomNumbers.value = []
    }
  } catch (error) {
    if (!isHandledRequestError(error)) {
      showWarningToast(resolveWarningMessage(error, '加载房型详情失败'))
    }
  } finally {
    loading.value = false
  }
}

async function handleRefresh(event: CustomEvent) {
  try {
    await loadRoomTypeDetails()
  } finally {
    event.detail.complete()
  }
}

onIonViewWillEnter(async () => {
  await loadRoomTypeDetails()
})
</script>

<style scoped>
.room-type-detail-page {
  display: block;
}

.room-type-detail-hero {
  margin-top: 4px;
}

.room-type-detail-hero__eyebrow {
  color: var(--ion-color-primary);
  font-weight: 700;
}

.room-type-detail-card {
  display: grid;
  gap: 14px;
}

.room-type-detail-card__header {
  align-items: flex-start;
}

.detail-info-grid,
.detail-price-grid,
.detail-photo-grid {
  display: grid;
  gap: 12px;
}

.detail-info-grid,
.detail-price-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-info-item,
.detail-price-item {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--app-border);
}

.detail-info-item span,
.detail-price-item span {
  color: var(--app-muted);
  font-size: 12px;
}

.detail-info-item strong,
.detail-price-item strong,
.detail-info-item p,
.detail-info-item a {
  margin: 0;
  word-break: break-word;
}

.detail-info-item a {
  color: var(--ion-color-primary);
  text-decoration: none;
}

.detail-info-item--full {
  grid-column: 1 / -1;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detail-tag {
  padding: 8px 12px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--ion-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.detail-tag--room {
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
}

.detail-photo-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-photo-item {
  margin: 0;
  display: grid;
  gap: 8px;
}

.detail-photo-item img {
  width: 100%;
  aspect-ratio: 1.2;
  object-fit: cover;
  border-radius: 18px;
  background: var(--app-border);
}

.detail-photo-item figcaption {
  color: var(--app-muted);
  font-size: 12px;
  line-height: 1.5;
  word-break: break-word;
}

@media (max-width: 680px) {
  .detail-info-grid,
  .detail-price-grid,
  .detail-photo-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
