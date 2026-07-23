import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { describe, expect, it, vi } from 'vitest'
import WorkspaceSelectionModal from '@/views/auth/WorkspaceSelectionModal.vue'
import type { SupportedLocale } from '@/locales'
import { createTestI18n } from './helpers/i18n'

vi.mock('@ionic/vue', () => {
  const createStub = (name: string, tag = 'div') =>
    defineComponent({
      name,
      inheritAttrs: false,
      setup(_, { attrs, slots }) {
        return () => h(tag, attrs, slots.default ? slots.default() : [])
      },
    })

  return {
    IonButton: createStub('IonButton', 'button'),
    IonContent: createStub('IonContent'),
    IonHeader: createStub('IonHeader'),
    IonModal: createStub('IonModal'),
    IonTitle: createStub('IonTitle'),
    IonToolbar: createStub('IonToolbar'),
  }
})

const expectations: Record<SupportedLocale, string[]> = {
  en: [
    'Choose workspace',
    'This account can access multiple workspaces. Choose where to continue.',
    'Housekeeping workspace',
    'View housekeeping tasks and room cleaning schedules',
    'Hotel management workspace',
    'Manage properties, rooms, reservations, and business data',
    'Back to sign in',
  ],
  ja: [
    'ワークスペースを選択',
    'このアカウントでは複数の業務画面を利用できます。今回使用するワークスペースを選択してください。',
    '清掃ワークスペース',
    '清掃タスクと客室清掃スケジュールを確認します',
    'ホテル管理ワークスペース',
    '施設、客室状況、予約、経営データを管理します',
    'ログインへ戻る',
  ],
  'zh-CN': [
    '选择工作台',
    '该账号可以进入多个工作台，请选择本次要进入的工作空间。',
    '保洁工作台',
    '查看保洁任务与房间清洁安排',
    '酒店管理工作台',
    '管理门店、房态、订单与经营数据',
    '返回登录',
  ],
  'zh-TW': [
    '選擇工作台',
    '此帳號可進入多個工作台，請選擇本次要進入的工作空間。',
    '清潔工作台',
    '查看清潔任務與房間清潔安排',
    '飯店管理工作台',
    '管理門店、房態、訂單與營運資料',
    '返回登入',
  ],
}

describe('WorkspaceSelectionModal i18n', () => {
  it.each(Object.entries(expectations) as Array<[SupportedLocale, string[]]>)(
    'renders %s workspace content',
    (locale, labels) => {
      const wrapper = mount(WorkspaceSelectionModal, {
        props: {
          open: true,
          targets: ['CLEANER', 'PMS'],
          loading: false,
        },
        global: {
          plugins: [createTestI18n(locale)],
        },
      })

      for (const label of labels) {
        expect(wrapper.text()).toContain(label)
      }
    },
  )
})
