export type NotificationPreviewSoundType = 'order' | 'chat'

interface NotificationPreviewStep {
  frequency: number
  durationMs: number
  pauseAfterMs: number
  oscillatorType: OscillatorType
}

const PREVIEW_START_DELAY_MS = 20
const PREVIEW_END_BUFFER_MS = 120
const PREVIEW_VOLUME = 0.12

const ORDER_PREVIEW_SEQUENCE: NotificationPreviewStep[] = [
  {
    frequency: 880,
    durationMs: 140,
    pauseAfterMs: 60,
    oscillatorType: 'triangle',
  },
  {
    frequency: 988,
    durationMs: 140,
    pauseAfterMs: 0,
    oscillatorType: 'triangle',
  },
]

const CHAT_PREVIEW_SEQUENCE: NotificationPreviewStep[] = [
  {
    frequency: 660,
    durationMs: 110,
    pauseAfterMs: 70,
    oscillatorType: 'sine',
  },
  {
    frequency: 660,
    durationMs: 110,
    pauseAfterMs: 0,
    oscillatorType: 'sine',
  },
]

let activeAudioContext: AudioContext | null = null

function resolveAudioContextConstructor() {
  if (typeof window === 'undefined') {
    return null
  }

  const browserWindow = window as Window & {
    webkitAudioContext?: typeof AudioContext
  }

  return window.AudioContext ?? browserWindow.webkitAudioContext ?? null
}

function getPreviewSequence(soundType: NotificationPreviewSoundType) {
  if (soundType === 'order') {
    return ORDER_PREVIEW_SEQUENCE
  }

  return CHAT_PREVIEW_SEQUENCE
}

function getPreviewDurationMs(sequence: NotificationPreviewStep[]) {
  let totalDurationMs = PREVIEW_START_DELAY_MS + PREVIEW_END_BUFFER_MS

  for (const step of sequence) {
    totalDurationMs += step.durationMs + step.pauseAfterMs
  }

  return totalDurationMs
}

async function closeAudioContext(context: AudioContext | null) {
  if (!context || context.state === 'closed') {
    if (activeAudioContext === context) {
      activeAudioContext = null
    }
    return
  }

  try {
    await context.close()
  } finally {
    if (activeAudioContext === context) {
      activeAudioContext = null
    }
  }
}

function waitForMs(durationMs: number) {
  return new Promise<void>((resolve) => {
    window.setTimeout(() => {
      resolve()
    }, durationMs)
  })
}

export async function playNotificationPreviewSound(soundType: NotificationPreviewSoundType) {
  const AudioContextConstructor = resolveAudioContextConstructor()

  if (!AudioContextConstructor) {
    throw new Error('当前环境暂不支持试听提示音')
  }

  await closeAudioContext(activeAudioContext)

  const audioContext = new AudioContextConstructor()
  activeAudioContext = audioContext

  if (audioContext.state === 'suspended') {
    await audioContext.resume()
  }

  const masterGainNode = audioContext.createGain()
  masterGainNode.gain.value = PREVIEW_VOLUME
  masterGainNode.connect(audioContext.destination)

  const sequence = getPreviewSequence(soundType)
  let currentTime = audioContext.currentTime + PREVIEW_START_DELAY_MS / 1000

  for (const step of sequence) {
    const oscillatorNode = audioContext.createOscillator()
    const gainNode = audioContext.createGain()
    const stopTime = currentTime + step.durationMs / 1000
    const fadeDurationSeconds = Math.min(step.durationMs / 1000 / 2, 0.03)

    oscillatorNode.type = step.oscillatorType
    oscillatorNode.frequency.setValueAtTime(step.frequency, currentTime)

    gainNode.gain.setValueAtTime(0.0001, currentTime)
    gainNode.gain.linearRampToValueAtTime(1, currentTime + fadeDurationSeconds)
    gainNode.gain.setValueAtTime(1, Math.max(stopTime - fadeDurationSeconds, currentTime))
    gainNode.gain.linearRampToValueAtTime(0.0001, stopTime)

    oscillatorNode.connect(gainNode)
    gainNode.connect(masterGainNode)

    oscillatorNode.start(currentTime)
    oscillatorNode.stop(stopTime)

    currentTime = stopTime + step.pauseAfterMs / 1000
  }

  try {
    await waitForMs(getPreviewDurationMs(sequence))
  } finally {
    await closeAudioContext(audioContext)
  }
}
