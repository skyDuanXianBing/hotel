import { describe, expect, it } from 'vitest'
import {
  detectGuestMessageLanguageCode,
  resolveGuestMessageTargetLanguage,
} from '@/utils/messageTranslation'

describe('detectGuestMessageLanguageCode', () => {
  it('检测日语（假名）', () => {
    expect(detectGuestMessageLanguageCode('チェックイン時間を教えてください')).toBe('ja')
  })

  it('检测韩语（谚文）', () => {
    expect(detectGuestMessageLanguageCode('체크인 시간이 어떻게 되나요?')).toBe('ko')
  })

  it('检测中文（汉字）', () => {
    expect(detectGuestMessageLanguageCode('请问几点可以入住？')).toBe('zh')
  })

  it('拉丁字母与空文本默认英语', () => {
    expect(detectGuestMessageLanguageCode('Can I check in early?')).toBe('en')
    expect(detectGuestMessageLanguageCode('')).toBe('en')
    expect(detectGuestMessageLanguageCode(undefined)).toBe('en')
  })
})

describe('resolveGuestMessageTargetLanguage', () => {
  it('中文映射为 zh-CN，其余语言保持原代码', () => {
    expect(resolveGuestMessageTargetLanguage('请问几点可以入住？')).toBe('zh-CN')
    expect(resolveGuestMessageTargetLanguage('チェックイン時間を教えてください')).toBe('ja')
    expect(resolveGuestMessageTargetLanguage('체크인 시간이 어떻게 되나요?')).toBe('ko')
    expect(resolveGuestMessageTargetLanguage('Can I check in early?')).toBe('en')
  })
})
