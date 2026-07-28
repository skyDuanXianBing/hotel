import { describe, expect, it } from 'vitest'
import {
  createDevicePhotoTextState,
  mapDevicePhotoTextState,
} from '@/utils/devicePhotos'

describe('device photo form helpers', () => {
  it('keeps desktop and mobile photo collections independent', () => {
    const state = createDevicePhotoTextState(
      ['/media/1/store-desktop/desktop.jpg'],
      ['/media/1/store-mobile/mobile.jpg'],
    )
    const payload = mapDevicePhotoTextState(state, (value) => value.split('\n').filter(Boolean))

    expect(payload.desktop).toEqual(['/media/1/store-desktop/desktop.jpg'])
    expect(payload.mobile).toEqual(['/media/1/store-mobile/mobile.jpg'])
  })
})
