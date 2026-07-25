<script setup lang="ts">
import { computed } from 'vue'
import type { PublicIndependentSiteRoomType } from '@/types/independentSite'
import { safeIndependentSiteImageUrl } from '../../pageSchema'

const props = withDefaults(
  defineProps<{
    roomTypes?: PublicIndependentSiteRoomType[]
    layout?: 'grid' | 'list'
  }>(),
  {
    roomTypes: () => [],
    layout: 'grid',
  },
)

const emit = defineEmits<{
  bookingRequest: []
  selectRoomType: [roomTypeId: number]
}>()

interface RoomTypeCard {
  id: number
  name: string
  imageUrl: string
  sizeText: string
  guestsText: string
}

// 消费公开站点 roomTypes（发布范围内真实在售房型），字段缺失时只显示有的部分
const cards = computed<RoomTypeCard[]>(() =>
  (Array.isArray(props.roomTypes) ? props.roomTypes : []).map((roomType) => {
    const photoCandidates = [
      ...(roomType.desktopPhotoUrls || []),
      ...(roomType.mobilePhotoUrls || []),
    ]
    const imageUrl = photoCandidates.map(safeIndependentSiteImageUrl).find(Boolean) || ''
    const sizeText = roomType.size
      ? `${roomType.size}${roomType.sizeUnit ? ` ${roomType.sizeUnit}` : ''}`
      : ''
    const guestParts: string[] = []
    if (roomType.maxGuests) {
      guestParts.push(`可住 ${roomType.maxGuests} 人`)
    }
    if (roomType.maxChildren) {
      guestParts.push(`儿童 ${roomType.maxChildren} 名`)
    }
    return {
      id: roomType.id,
      name: roomType.name || '房型',
      imageUrl,
      sizeText,
      guestsText: guestParts.join(' · '),
    }
  }),
)

// 卡片与按钮同一行为：预选该房型（父级经 initial-room-type-id 传给订房流程）并滚动到 #booking
const handleSelect = (roomTypeId: number) => {
  emit('selectRoomType', roomTypeId)
  emit('bookingRequest')
}
</script>

<template>
  <section class="slot-room-list" :class="`is-${layout}`" aria-label="房型列表">
    <div v-if="cards.length === 0" class="room-list-empty">
      <p>房型信息即将上线，请稍后再来查看。</p>
    </div>
    <ul v-else class="room-list">
      <li v-for="card in cards" :key="card.id" class="room-card">
        <button type="button" class="room-card-body" @click="handleSelect(card.id)">
          <img
            v-if="card.imageUrl"
            class="room-card-image"
            :src="card.imageUrl"
            :alt="card.name"
            loading="lazy"
            decoding="async"
          />
          <span v-else class="room-card-image room-card-image--placeholder" aria-hidden="true">
            {{ card.name.slice(0, 1) }}
          </span>
          <span class="room-card-info">
            <strong class="room-card-name">{{ card.name }}</strong>
            <span v-if="card.sizeText || card.guestsText" class="room-card-meta">
              <span v-if="card.sizeText">{{ card.sizeText }}</span>
              <span v-if="card.sizeText && card.guestsText" aria-hidden="true"> · </span>
              <span v-if="card.guestsText">{{ card.guestsText }}</span>
            </span>
            <span class="room-card-cta">查看价格 / 预订</span>
          </span>
        </button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.slot-room-list {
  width: 100%;
  padding: 32px max(20px, calc((100vw - 1120px) / 2));
}

.room-list-empty {
  display: grid;
  place-content: center;
  min-height: 160px;
  padding: 24px;
  border: 1px dashed color-mix(in srgb, var(--site-text, #1f2a28) 24%, transparent);
  border-radius: var(--site-radius, 16px);
  color: color-mix(in srgb, var(--site-text, #1f2a28) 62%, transparent);
  text-align: center;
}

.room-list-empty p {
  margin: 0;
}

.room-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.is-list .room-list {
  grid-template-columns: 1fr;
}

.room-card {
  min-width: 0;
}

.room-card-body {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  padding: 0;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--site-text, #1f2a28) 12%, transparent);
  border-radius: var(--site-image-radius, 16px);
  background: var(--site-surface, #fff);
  cursor: pointer;
  text-align: left;
  font: inherit;
  transition:
    box-shadow 0.2s,
    transform 0.2s;
}

.room-card-body:hover {
  box-shadow: 0 10px 28px color-mix(in srgb, var(--site-text, #1f2a28) 14%, transparent);
  transform: translateY(-2px);
}

.room-card-body:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--site-accent, #d19a66) 70%, white);
  outline-offset: 2px;
}

.is-list .room-card-body {
  flex-direction: row;
  align-items: stretch;
}

.room-card-image {
  width: 100%;
  aspect-ratio: var(--site-image-ratio, 4 / 3);
  object-fit: cover;
  background: color-mix(in srgb, var(--site-primary, #214e46) 10%, var(--site-surface, #fff));
}

.is-list .room-card-image {
  flex-shrink: 0;
  width: 220px;
  height: 100%;
  aspect-ratio: auto;
}

.room-card-image--placeholder {
  display: grid;
  place-content: center;
  color: var(--site-primary, #214e46);
  font-family: var(--site-font-heading, Georgia, serif);
  font-size: 40px;
}

.room-card-info {
  display: grid;
  gap: 6px;
  padding: 16px 18px 18px;
}

.room-card-name {
  color: var(--site-text, #1f2a28);
  font-family: var(--site-font-heading, inherit);
  font-size: 18px;
}

.room-card-meta {
  color: color-mix(in srgb, var(--site-text, #1f2a28) 66%, transparent);
  font-size: 13px;
}

.room-card-cta {
  margin-top: 6px;
  color: var(--site-primary, #214e46);
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .is-list .room-card-body {
    flex-direction: column;
  }

  .is-list .room-card-image {
    width: 100%;
    height: auto;
    aspect-ratio: var(--site-image-ratio, 4 / 3);
  }
}
</style>
