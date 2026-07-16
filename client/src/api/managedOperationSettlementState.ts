export interface ManagedOperationPersistenceFields {
  propertyName?: string | null
  selectedRoomIds?: number[] | null
  ownerCompanyName?: string | null
  issuerCompanyName?: string | null
}

export interface ManagedOperationFileIdentity {
  name: string
  size: number
  lastModified: number
}

const hasText = (value: string | null | undefined) => Boolean(value?.trim())

export const resolveManagedOperationSettingsPersisted = (
  persisted: boolean | undefined,
  settings: ManagedOperationPersistenceFields | null | undefined,
): boolean => {
  if (typeof persisted === 'boolean') return persisted
  if (!settings) return false

  return Boolean(
    hasText(settings.propertyName) &&
      settings.selectedRoomIds?.length &&
      hasText(settings.ownerCompanyName) &&
      hasText(settings.issuerCompanyName),
  )
}

export const managedOperationFileFingerprint = (
  file: ManagedOperationFileIdentity | null,
  revision: number,
) => ({
  identity: file ? `${file.name}:${file.size}:${file.lastModified}` : '',
  revision,
})
