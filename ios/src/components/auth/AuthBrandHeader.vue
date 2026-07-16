<template>
  <header :class="['auth-brand-header', { 'auth-brand-header--with-tabs': showTabs || backLabel }]">
    <div class="auth-brand-header__lockup">
      <img class="auth-brand-header__mark" src="/auth-logo.png" alt="" aria-hidden="true" />
      <span class="auth-brand-header__name">THE HOST HUB</span>
    </div>

    <nav v-if="showTabs" class="auth-page-tabs" aria-label="账户入口">
      <button
        :class="['auth-page-tabs__button', { 'auth-page-tabs__button--active': activeTab === 'login' }]"
        :aria-current="activeTab === 'login' ? 'page' : undefined"
        type="button"
        @click="emit('select-login')"
      >
        登录
      </button>
      <button
        :class="['auth-page-tabs__button', { 'auth-page-tabs__button--active': activeTab === 'register' }]"
        :aria-current="activeTab === 'register' ? 'page' : undefined"
        type="button"
        @click="emit('select-register')"
      >
        注册
      </button>
    </nav>

    <nav v-else-if="backLabel" class="auth-page-tabs auth-page-tabs--back" aria-label="返回登录">
      <button
        class="auth-page-tabs__button auth-page-tabs__button--active auth-page-tabs__button--back"
        type="button"
        @click="emit('select-back')"
      >
        <span class="auth-page-tabs__back-icon" aria-hidden="true">‹</span>
        <span>{{ backLabel }}</span>
      </button>
      <span class="auth-page-tabs__back-spacer" aria-hidden="true" />
    </nav>
  </header>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    activeTab?: 'login' | 'register'
    backLabel?: string
    showTabs?: boolean
  }>(),
  {
    activeTab: 'login',
    backLabel: '',
    showTabs: false,
  },
)

const emit = defineEmits<{
  (event: 'select-back'): void
  (event: 'select-login'): void
  (event: 'select-register'): void
}>()
</script>

<style scoped>
.auth-brand-header {
  position: relative;
  flex: 0 0 264px;
  height: 264px;
  overflow: hidden;
  background: linear-gradient(145deg, #edf9ff 0%, #eff7ff 56%, #f5f9ff 100%);
}

.auth-brand-header__lockup {
  position: absolute;
  top: calc(104px + var(--app-safe-top));
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 0 24px;
  color: #0789f5;
  white-space: nowrap;
}

.auth-brand-header__mark {
  width: 34px;
  height: 40px;
  object-fit: contain;
  flex-shrink: 0;
}

.auth-brand-header__name {
  font-size: 29px;
  font-weight: 400;
  line-height: 1;
  letter-spacing: 0;
}

.auth-page-tabs {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  height: 50px;
  background: rgba(240, 247, 253, 0.88);
}

.auth-page-tabs__button {
  min-width: 0;
  border: 0;
  border-radius: 22px 22px 0 0;
  background: transparent;
  color: #999da3;
  font: inherit;
  font-size: 19px;
  font-weight: 400;
  letter-spacing: 0;
}

.auth-page-tabs__button--active {
  background: #ffffff;
  color: #078ff4;
}

.auth-page-tabs--back {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.auth-page-tabs__button--back {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 4px;
  padding: 0 20px 0 34px;
}

.auth-page-tabs__back-icon {
  margin-top: -2px;
  font-size: 28px;
  line-height: 1;
}

.auth-page-tabs__back-spacer {
  background: transparent;
}

@media (max-width: 360px) {
  .auth-brand-header__name {
    font-size: 26px;
  }

  .auth-brand-header__mark {
    width: 31px;
    height: 37px;
  }
}
</style>
