// Keep this first so the Su Config CORS proxy is installed before third-party scripts load.
import './utils/suConfigProxy'

import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import { i18n } from './locales'
import router from './router'
import { pinia } from './stores/pinia'
import './styles/global.css'

const app = createApp(App)

// Register Element Plus icons.
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(i18n)
app.use(router)
app.use(ElementPlus)

app.mount('#app')
