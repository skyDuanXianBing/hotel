<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IndependentSitePageSchema, IndependentSiteThemeKey } from '@/types/independentSite'
import { normalizeIndependentSiteSchema } from '../pageSchema'
import { buildIndependentSiteCssVars, normalizeIndependentSiteThemeKey } from '../themes'

const props = withDefaults(
  defineProps<{
    schema: IndependentSitePageSchema
    preview?: boolean
    themeKey?: IndependentSiteThemeKey | string
  }>(),
  {
    preview: false,
  },
)

const emit = defineEmits<{
  bookingRequest: []
}>()

const { t } = useI18n()

const safeSchema = computed(() => normalizeIndependentSiteSchema(props.schema))

const normalizedThemeKey = computed(() => normalizeIndependentSiteThemeKey(props.themeKey))

// 主题 token 提供基础 CSS 变量；页面 schema 的 4 色作为覆盖层优先于 token
const themeStyle = computed(() =>
  buildIndependentSiteCssVars(normalizedThemeKey.value, safeSchema.value?.theme),
)

const themeClasses = computed(() => {
  const theme = safeSchema.value?.theme
  const classes = [`theme-${normalizedThemeKey.value}`]
  if (theme) {
    classes.push(
      `typography-${theme.typography.toLowerCase()}`,
      `corners-${theme.cornerStyle.toLowerCase()}`,
    )
  }
  return classes
})
</script>

<template>
  <div
    class="brand-renderer"
    :class="[themeClasses, { 'is-preview': preview }]"
    :style="themeStyle"
  >
    <template v-if="safeSchema">
      <section
        v-for="(section, index) in safeSchema.sections"
        :key="section.id || `${section.type}-${index}`"
        class="brand-section"
        :class="[
          `brand-section--${section.type.toLowerCase()}`,
          `is-${section.alignment.toLowerCase()}`,
        ]"
      >
        <template v-if="section.type === 'HERO'">
          <div class="hero-copy">
            <p class="hero-eyebrow">{{ t('independentSite.renderer.directStay') }}</p>
            <h1>{{ section.title }}</h1>
            <p v-if="section.body" class="hero-subtitle">{{ section.body }}</p>
            <button type="button" class="hero-action" @click="emit('bookingRequest')">
              {{ t('independentSite.renderer.viewAvailableRooms') }}
            </button>
          </div>
          <img
            v-if="section.imageUrl"
            class="hero-image"
            :src="section.imageUrl"
            :alt="section.title"
          />
          <div v-else class="hero-art" aria-hidden="true">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </template>

        <template v-else-if="section.type === 'ABOUT'">
          <div class="section-heading">
            <span>{{ t('independentSite.renderer.about') }}</span>
            <h2>{{ section.title }}</h2>
          </div>
          <div class="section-body">
            <p v-if="section.body" class="intro-body">{{ section.body }}</p>
            <img
              v-if="section.imageUrl"
              class="about-image"
              :src="section.imageUrl"
              :alt="section.title"
              loading="lazy"
            />
          </div>
        </template>

        <template v-else-if="section.type === 'HIGHLIGHTS'">
          <div class="section-heading">
            <span>{{ t('independentSite.renderer.highlights') }}</span>
            <h2>{{ section.title }}</h2>
          </div>
          <div class="highlight-grid">
            <article v-for="item in section.items || []" :key="item" class="highlight-card">
              <span class="highlight-mark" aria-hidden="true"></span>
              <h3>{{ item }}</h3>
            </article>
          </div>
        </template>

        <template v-else-if="section.type === 'AMENITIES'">
          <div class="section-heading">
            <span>{{ t('independentSite.renderer.amenities') }}</span>
            <h2>{{ section.title }}</h2>
          </div>
          <ul class="amenity-grid">
            <li v-for="item in section.items || []" :key="item">
              <span aria-hidden="true">✓</span>
              {{ item }}
            </li>
          </ul>
        </template>

        <template v-else-if="section.type === 'LOCATION'">
          <div class="section-heading">
            <span>{{ t('independentSite.renderer.location') }}</span>
            <h2>{{ section.title }}</h2>
          </div>
          <p v-if="section.body" class="intro-body">{{ section.body }}</p>
        </template>

        <template v-else-if="section.type === 'HOUSE_RULES'">
          <div class="section-heading">
            <span>{{ t('independentSite.renderer.houseRules') }}</span>
            <h2>{{ section.title }}</h2>
          </div>
          <div class="policy-list">
            <article v-for="item in section.items || []" :key="item">
              <h3>{{ item }}</h3>
            </article>
            <p v-if="section.body" class="rules-body">{{ section.body }}</p>
          </div>
        </template>

        <template v-else-if="section.type === 'GALLERY'">
          <div class="section-heading">
            <span>{{ t('independentSite.renderer.gallery') }}</span>
            <h2>{{ section.title }}</h2>
          </div>
          <div class="section-body">
            <p v-if="section.body" class="intro-body">{{ section.body }}</p>
            <div class="gallery-grid">
              <figure v-for="(image, imageIndex) in section.images || []" :key="`${image.url}-${imageIndex}`">
                <img :src="image.url" :alt="image.alt || section.title" loading="lazy" />
                <figcaption v-if="image.alt">{{ image.alt }}</figcaption>
              </figure>
            </div>
          </div>
        </template>

        <template v-else-if="section.type === 'BOOKING'">
          <div class="section-heading">
            <span>{{ t('independentSite.renderer.bookDirect') }}</span>
            <h2>{{ section.title }}</h2>
          </div>
          <div class="booking-cta">
            <h3>{{ section.title }}</h3>
            <p v-if="section.body">{{ section.body }}</p>
            <button type="button" class="booking-cta-action" @click="emit('bookingRequest')">
              {{ t('independentSite.renderer.searchAvailableRooms') }}
            </button>
          </div>
        </template>
      </section>

      <div v-if="safeSchema.sections.length === 0" class="brand-empty">
        <p>{{ t('independentSite.renderer.emptyTitle') }}</p>
        <span>{{ t('independentSite.renderer.emptyDescription') }}</span>
      </div>
    </template>
  </div>
</template>

<style scoped>
.brand-renderer {
  color: var(--site-text, #1f2a28);
  background: color-mix(in srgb, var(--site-surface, #fff) 82%, var(--site-primary, #214e46));
  font-family: var(
    --site-font-body,
    'Avenir Next',
    'PingFang SC',
    'Microsoft YaHei',
    sans-serif
  );
}

.typography-classic {
  font-family: Georgia, 'Times New Roman', 'Songti SC', serif;
}

.typography-friendly {
  font-family: 'Avenir Next', 'Trebuchet MS', 'PingFang SC', sans-serif;
}

.corners-square :is(.hero-art, .hero-image, .about-image, .highlight-card, .amenity-grid li, .policy-list article, .gallery-grid img, .booking-cta) {
  border-radius: 0;
}

.corners-pill :is(.highlight-card, .amenity-grid li, .policy-list article, .booking-cta) {
  border-radius: 999px;
}

.brand-section {
  width: min(1120px, calc(100% - 40px));
  margin: 0 auto;
  padding: var(--site-section-spacing, 72px) 0;
}

.brand-section--hero {
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(300px, 0.95fr);
  gap: 48px;
  min-height: 520px;
  align-items: center;
  padding-top: 56px;
}

.hero-copy h1 {
  max-width: 720px;
  margin: 10px 0 18px;
  color: var(--site-primary, #214e46);
  font-family: var(--site-font-heading, Georgia, 'Times New Roman', serif);
  font-size: clamp(42px, 7vw, 78px);
  font-weight: 500;
  letter-spacing: -0.04em;
  line-height: 0.98;
}

.hero-eyebrow,
.section-heading > span {
  margin: 0;
  color: var(--site-accent, #d19a66);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hero-subtitle {
  max-width: 620px;
  margin: 0;
  font-size: 18px;
  line-height: 1.75;
  opacity: 0.82;
  white-space: pre-line;
}

.hero-action {
  margin-top: 30px;
  padding: 13px 22px;
  border: 1px solid var(--site-primary, #214e46);
  border-radius: 999px;
  color: #fff;
  background: var(--site-primary, #214e46);
  cursor: pointer;
  font: inherit;
  font-weight: 650;
}

.hero-action:hover {
  filter: brightness(1.08);
}

.hero-action:focus-visible,
.booking-cta-action:focus-visible,
summary:focus-visible {
  outline: 3px solid color-mix(in srgb, var(--site-accent, #d19a66) 70%, white);
  outline-offset: 3px;
}

.hero-image,
.hero-art {
  width: 100%;
  min-height: 410px;
  border-radius: 180px 180px var(--site-image-radius, 24px) var(--site-image-radius, 24px);
  box-shadow: 0 30px 80px rgba(31, 42, 40, 0.16);
}

.hero-image {
  display: block;
  object-fit: cover;
}

.hero-art {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 70% 18%, var(--site-accent) 0 8%, transparent 8.5%),
    linear-gradient(145deg, color-mix(in srgb, var(--site-primary) 84%, white), var(--site-primary));
}

.hero-art span {
  position: absolute;
  left: -5%;
  bottom: -17%;
  width: 82%;
  aspect-ratio: 1;
  border: 1px solid rgba(255, 255, 255, 0.38);
  border-radius: 50%;
}

.hero-art span:nth-child(2) {
  left: 36%;
  bottom: -8%;
  width: 60%;
}

.hero-art span:nth-child(3) {
  left: 53%;
  bottom: 27%;
  width: 24%;
  background: color-mix(in srgb, var(--site-accent) 76%, white);
  border: 0;
}

.brand-section:not(.brand-section--hero) {
  display: grid;
  grid-template-columns: minmax(180px, 0.32fr) minmax(0, 0.68fr);
  gap: 56px;
  border-top: 1px solid color-mix(in srgb, var(--site-text) 14%, transparent);
}

.section-heading h2 {
  margin: 8px 0 0;
  color: var(--site-primary, #214e46);
  font-family: var(--site-font-heading, Georgia, 'Times New Roman', serif);
  font-size: clamp(30px, 4vw, 48px);
  font-weight: 500;
  line-height: 1.08;
}

.section-body {
  min-width: 0;
}

.intro-body,
.location-copy {
  margin: 0;
  font-size: 18px;
  line-height: 1.85;
  white-space: pre-line;
}

.about-image {
  display: block;
  width: 100%;
  margin-top: 26px;
  aspect-ratio: var(--site-image-ratio, 4 / 3);
  border-radius: var(--site-image-radius, 24px);
  object-fit: cover;
  box-shadow: 0 18px 48px rgba(31, 42, 40, 0.12);
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.highlight-card,
.policy-list article {
  padding: 24px;
  border-radius: var(--site-radius, 18px);
  background: var(--site-surface, #fff);
  box-shadow: 0 10px 36px rgba(31, 42, 40, 0.06);
}

.brand-section.is-center:not(.brand-section--hero) {
  grid-template-columns: 1fr;
  text-align: center;
}

.brand-section--hero.is-center .hero-copy {
  text-align: center;
}

.brand-section--hero.is-center .hero-subtitle {
  margin-right: auto;
  margin-left: auto;
}

.highlight-mark {
  display: block;
  width: 30px;
  height: 4px;
  margin-bottom: 20px;
  border-radius: 999px;
  background: var(--site-accent, #d19a66);
}

.highlight-card h3,
.policy-list h3 {
  margin: 0 0 8px;
  color: var(--site-primary, #214e46);
  font-size: 17px;
}

.highlight-card p,
.policy-list p {
  margin: 0;
  line-height: 1.65;
  opacity: 0.76;
  white-space: pre-line;
}

.amenity-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.amenity-grid li {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 48px;
  padding: 10px 14px;
  border-radius: 12px;
  background: var(--site-surface, #fff);
}

.amenity-grid span {
  color: var(--site-accent, #d19a66);
  font-weight: 800;
}

.faq-list details {
  border-bottom: 1px solid color-mix(in srgb, var(--site-text) 15%, transparent);
}

.faq-list summary {
  padding: 18px 4px;
  cursor: pointer;
  font-size: 17px;
  font-weight: 650;
}

.faq-list p {
  margin: 0;
  padding: 0 4px 20px;
  line-height: 1.72;
  opacity: 0.78;
  white-space: pre-line;
}

.policy-list {
  display: grid;
  gap: 14px;
}

.rules-body {
  margin: 14px 0 0;
  line-height: 1.7;
  white-space: pre-line;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 22px;
}

.section-body > .gallery-grid:first-child {
  margin-top: 0;
}

.gallery-grid figure {
  margin: 0;
}

.gallery-grid img {
  display: block;
  width: 100%;
  aspect-ratio: var(--site-image-ratio, 4 / 3);
  border-radius: var(--site-image-radius, 24px);
  object-fit: cover;
  box-shadow: 0 12px 32px rgba(31, 42, 40, 0.1);
}

.gallery-grid figcaption {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.5;
  opacity: 0.72;
}

.booking-cta {
  padding: 38px;
  border-radius: var(--site-radius, 18px);
  color: #fff;
  text-align: center;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--site-primary, #214e46) 86%, black),
    var(--site-primary, #214e46)
  );
  box-shadow: 0 20px 56px rgba(31, 42, 40, 0.2);
}

.booking-cta h3 {
  margin: 0;
  font-family: var(--site-font-heading, Georgia, 'Times New Roman', serif);
  font-size: clamp(26px, 3.4vw, 38px);
  font-weight: 500;
  line-height: 1.15;
}

.booking-cta p {
  max-width: 620px;
  margin: 12px auto 0;
  line-height: 1.7;
  opacity: 0.85;
  white-space: pre-line;
}

.booking-cta-action {
  margin-top: 24px;
  padding: 13px 26px;
  border: 1px solid var(--site-accent, #d19a66);
  border-radius: 999px;
  color: var(--site-primary, #214e46);
  background: var(--site-accent, #d19a66);
  cursor: pointer;
  font: inherit;
  font-weight: 700;
}

.booking-cta-action:hover {
  filter: brightness(1.06);
}

.brand-empty {
  width: min(1120px, calc(100% - 40px));
  margin: 0 auto;
  padding: 52px 0;
  text-align: center;
}

.brand-empty p {
  margin: 0 0 6px;
  font-weight: 700;
}

.brand-empty span {
  opacity: 0.64;
}

.is-preview .brand-section {
  width: min(100% - 28px, 900px);
  padding: calc(var(--site-section-spacing, 72px) * 0.62) 0;
}

.is-preview .brand-section--hero {
  min-height: 390px;
}

.is-preview .hero-copy h1 {
  font-size: clamp(34px, 6vw, 58px);
}

.is-preview .hero-image,
.is-preview .hero-art {
  min-height: 300px;
}

@media (max-width: 760px) {
  .brand-section {
    width: min(100% - 28px, 1120px);
    padding: calc(var(--site-section-spacing, 72px) * 0.67) 0;
  }

  .brand-section--hero,
  .brand-section:not(.brand-section--hero) {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .brand-section--hero {
    min-height: auto;
    padding-top: 36px;
  }

  .hero-copy h1 {
    font-size: clamp(38px, 13vw, 58px);
  }

  .hero-image,
  .hero-art {
    min-height: 330px;
    border-radius: 120px 120px var(--site-image-radius, 20px) var(--site-image-radius, 20px);
  }

  .highlight-grid,
  .amenity-grid {
    grid-template-columns: 1fr;
  }

  .gallery-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }

  .booking-cta {
    padding: 28px 20px;
  }
}
</style>
