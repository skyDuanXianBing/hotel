import { shallowRef } from 'vue'
import type { RouteLocationNormalizedLoaded, Router } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { i18n } from '@/locales'
import { registerRouterGuards, updateDocumentTitle } from '@/router/guards'

const createRoute = (
  name: string,
  titleKey: string,
): RouteLocationNormalizedLoaded =>
  ({
    name,
    meta: {
      titleKey,
    },
  }) as RouteLocationNormalizedLoaded

describe('localized router titles', () => {
  beforeEach(() => {
    i18n.global.locale.value = 'zh-CN'
    document.title = ''
  })

  it('builds the document title from the active locale', () => {
    const route = createRoute('Settings', 'routes.Settings')

    i18n.global.locale.value = 'en'
    updateDocumentTitle(route)
    expect(document.title).toBe('Settings - THE HOST HUB')

    i18n.global.locale.value = 'ja'
    updateDocumentTitle(route)
    expect(document.title).toContain('THE HOST HUB')
    expect(document.title).not.toBe('Settings - THE HOST HUB')
  })

  it('updates the current route title immediately when the locale changes', () => {
    const currentRoute = shallowRef(createRoute('Orders', 'routes.Orders'))
    const routerStub = {
      beforeEach: vi.fn(),
      afterEach: vi.fn(),
      currentRoute,
    } as unknown as Router

    const disposeGuards = registerRouterGuards(routerStub)
    i18n.global.locale.value = 'en'

    expect(document.title).toBe('Reservations - THE HOST HUB')
    disposeGuards()
  })
})
