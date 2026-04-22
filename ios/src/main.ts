import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { IonicVue } from '@ionic/vue'
import App from './App.vue'
import router from './router'
import { applyLightTheme } from '@/utils/theme'

const pinia = createPinia()

/* Core CSS required for Ionic components to work properly */
import '@ionic/vue/css/core.css'

/* Basic CSS for apps built with Ionic */
import '@ionic/vue/css/normalize.css'
import '@ionic/vue/css/structure.css'
import '@ionic/vue/css/typography.css'

/* Optional CSS utils that can be commented out */
import '@ionic/vue/css/padding.css'
import '@ionic/vue/css/float-elements.css'
import '@ionic/vue/css/text-alignment.css'
import '@ionic/vue/css/text-transformation.css'
import '@ionic/vue/css/flex-utils.css'
import '@ionic/vue/css/display.css'

/**
 * Ionic 8 默认浅色调色板已内置，无需额外导入 default.always.css。
 * 当前版本仅提供 dark / high-contrast palette 文件，直接使用项目主题变量即可。
 */

/* Theme variables */
import './theme/variables.css'

applyLightTheme()

const app = createApp(App)

app.use(IonicVue, {
  mode: 'ios',
  backButtonText: '返回',
})
app.use(pinia)
app.use(router)

router.isReady().then(() => {
  app.mount('#app')
})
