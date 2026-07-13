import { describe, expect, test } from 'bun:test'
import { resolveInternalTaskCapabilities } from '../internalTaskCapabilities'

describe('resolveInternalTaskCapabilities', () => {
  test.each([
    [false, false, false, false, false],
    [true, false, true, true, false],
    [false, true, false, true, true],
    [true, true, true, true, true],
  ])(
    'canCreate=%s canManage=%s 时能力保持拆分',
    (canCreate, canManage, expectedCreate, expectedAssignees, expectedManage) => {
      const result = resolveInternalTaskCapabilities(canCreate, canManage)

      expect(result.canCreate).toBe(expectedCreate)
      expect(result.canLoadAssignees).toBe(expectedAssignees)
      expect(result.canLoadAllTasks).toBe(expectedManage)
      expect(result.canAssign).toBe(expectedManage)
      expect(result.canArchive).toBe(expectedManage)
    }
  )
})
