export interface DevicePhotoTextState {
  desktopPhotoUrlsText: string
  mobilePhotoUrlsText: string
}

export function createDevicePhotoTextState(
  desktopPhotoUrls?: string[],
  mobilePhotoUrls?: string[],
): DevicePhotoTextState {
  return {
    desktopPhotoUrlsText: (desktopPhotoUrls || []).filter(Boolean).join('\n'),
    mobilePhotoUrlsText: (mobilePhotoUrls || []).filter(Boolean).join('\n'),
  }
}

export function mapDevicePhotoTextState<T>(
  state: DevicePhotoTextState,
  mapper: (value: string) => T,
) {
  return {
    desktop: mapper(state.desktopPhotoUrlsText),
    mobile: mapper(state.mobilePhotoUrlsText),
  }
}
