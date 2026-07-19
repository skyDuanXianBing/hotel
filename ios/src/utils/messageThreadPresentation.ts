import type { CSSProperties } from 'vue'
import type { MessageThreadDTO } from '@/types/message'

interface MessageThreadBrand {
  accent: string
  soft: string
  border: string
  avatarStart: string
  avatarEnd: string
}

const DEFAULT_MESSAGE_THREAD_BRAND: MessageThreadBrand = {
  accent: '#4a98ff',
  soft: 'rgba(74, 152, 255, 0.12)',
  border: 'rgba(74, 152, 255, 0.18)',
  avatarStart: '#8dbdff',
  avatarEnd: '#4a98ff',
}

function resolveMessageThreadBrand(thread: MessageThreadDTO): MessageThreadBrand {
  const channelName = (thread.channelName || '').trim()
  const normalizedChannelName = channelName.toLowerCase()

  if (normalizedChannelName.includes('meituan') || channelName.includes('\u7f8e\u56e2')) {
    return {
      accent: '#38c2a4',
      soft: 'rgba(56, 194, 164, 0.14)',
      border: 'rgba(56, 194, 164, 0.24)',
      avatarStart: '#87ead4',
      avatarEnd: '#38c2a4',
    }
  }

  if (normalizedChannelName.includes('airbnb')) {
    return {
      accent: '#ff6f7a',
      soft: 'rgba(255, 111, 122, 0.14)',
      border: 'rgba(255, 111, 122, 0.2)',
      avatarStart: '#ffb0b8',
      avatarEnd: '#ff6f7a',
    }
  }

  if (normalizedChannelName.includes('booking')) {
    return {
      accent: '#4f72dc',
      soft: 'rgba(79, 114, 220, 0.14)',
      border: 'rgba(79, 114, 220, 0.2)',
      avatarStart: '#9cadff',
      avatarEnd: '#4f72dc',
    }
  }

  return DEFAULT_MESSAGE_THREAD_BRAND
}

export function resolveMessageThreadTitle(thread: MessageThreadDTO) {
  if (thread.guestName) {
    return thread.guestName
  }

  if (thread.listingName) {
    return thread.listingName
  }

  return thread.channelName || '\u672a\u547d\u540d\u4f1a\u8bdd'
}

export function resolveMessageThreadAvatarLabel(thread: MessageThreadDTO) {
  const source = resolveMessageThreadTitle(thread).trim()
  if (!source) {
    return '\u5ba2'
  }

  const firstCharacter = source.slice(0, 1)
  if (/[a-z]/i.test(firstCharacter)) {
    return firstCharacter.toUpperCase()
  }

  return firstCharacter
}

export function resolveMessageThreadAvatarVars(thread: MessageThreadDTO): CSSProperties {
  const brand = resolveMessageThreadBrand(thread)

  return {
    '--thread-accent': brand.accent,
    '--thread-soft': brand.soft,
    '--thread-border': brand.border,
    '--thread-avatar-start': brand.avatarStart,
    '--thread-avatar-end': brand.avatarEnd,
  } as CSSProperties
}
