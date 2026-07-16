import { describe, expect, test } from 'bun:test'
import {
  managedOperationFileFingerprint,
  resolveManagedOperationSettingsPersisted,
} from '../src/api/managedOperationSettlementState'

const completeSettings = {
  propertyName: 'Local E2E Hotel',
  selectedRoomIds: [101, 102],
  ownerCompanyName: 'Local Owner',
  issuerCompanyName: 'Local Issuer',
}

describe('resolveManagedOperationSettingsPersisted', () => {
  test('uses the explicit backend signal when available', () => {
    expect(resolveManagedOperationSettingsPersisted(false, completeSettings)).toBe(false)
    expect(resolveManagedOperationSettingsPersisted(true, null)).toBe(true)
  })

  test('cautiously infers legacy persisted responses from all required fields and rooms', () => {
    expect(resolveManagedOperationSettingsPersisted(undefined, completeSettings)).toBe(true)
    expect(
      resolveManagedOperationSettingsPersisted(undefined, {
        ...completeSettings,
        selectedRoomIds: [],
      }),
    ).toBe(false)
    expect(
      resolveManagedOperationSettingsPersisted(undefined, {
        ...completeSettings,
        issuerCompanyName: ' ',
      }),
    ).toBe(false)
    expect(resolveManagedOperationSettingsPersisted(undefined, null)).toBe(false)
  })
})

describe('managedOperationFileFingerprint', () => {
  test('changes when a same-metadata file is selected again', () => {
    const file = { name: 'airbnb.csv', size: 2048, lastModified: 1720000000000 }

    expect(managedOperationFileFingerprint(file, 1)).not.toEqual(
      managedOperationFileFingerprint(file, 2),
    )
  })

  test('keeps removals revision-sensitive', () => {
    expect(managedOperationFileFingerprint(null, 3)).not.toEqual(
      managedOperationFileFingerprint(null, 4),
    )
  })
})
