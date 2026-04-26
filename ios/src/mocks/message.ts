import type { ApiResponse } from '@/types/api'
import type {
  ChatMessageRequest,
  ChatMessageResponse,
  MessageDTO,
  MessageSendRequest,
  MessageThreadDTO,
} from '@/types/message'
import { MessageSenderType } from '@/types/message'

const MESSAGE_MOCK_DELAY_MS = 120

export const MESSAGE_API_MOCK_ENABLED = import.meta.env.VITE_USE_MESSAGE_MOCK === 'true'

type MessageMap = Record<number, MessageDTO[]>

const sleep = (ms = MESSAGE_MOCK_DELAY_MS) => new Promise((resolve) => setTimeout(resolve, ms))

const toIsoDaysAgo = (daysAgo: number, hour = 10, minute = 0) => {
  const value = new Date()
  value.setDate(value.getDate() - daysAgo)
  value.setHours(hour, minute, 0, 0)
  return value.toISOString()
}

const toIsoMinutesAgo = (minutesAgo: number) => {
  const value = new Date()
  value.setMinutes(value.getMinutes() - minutesAgo)
  value.setSeconds(0, 0)
  return value.toISOString()
}

const initialConversationTimestamps = {
  thread101: {
    first: toIsoMinutesAgo(58),
    second: toIsoMinutesAgo(46),
    third: toIsoMinutesAgo(18),
  },
  thread102: {
    first: toIsoMinutesAgo(132),
    second: toIsoMinutesAgo(118),
    third: toIsoMinutesAgo(94),
  },
  thread103: {
    first: toIsoMinutesAgo(202),
    second: toIsoMinutesAgo(176),
  },
}

const sortByTimestampAsc = (items: MessageDTO[]) => {
  return [...items].sort((firstItem, secondItem) => {
    return new Date(firstItem.timestamp).getTime() - new Date(secondItem.timestamp).getTime()
  })
}

const sortThreadsByActivityDesc = (items: MessageThreadDTO[]) => {
  return [...items].sort((firstItem, secondItem) => {
    return new Date(secondItem.lastActivity).getTime() - new Date(firstItem.lastActivity).getTime()
  })
}

const cloneMessage = (message: MessageDTO): MessageDTO => ({ ...message })

const cloneThread = (thread: MessageThreadDTO): MessageThreadDTO => ({ ...thread })

const buildSuccessResponse = <T>(data: T, message = 'Success'): ApiResponse<T> => ({
  success: true,
  message,
  data,
})

const createInitialThreads = (): MessageThreadDTO[] => [
  {
    id: 101,
    channelId: 19,
    channelName: '美团民宿',
    guestName: '小幸运',
    bookingId: 'MT-240712',
    threadId: 'meituan-thread-240712',
    listingId: 'listing-garden-01',
    listingName: '花园景观房',
    roomTypeName: '花园景观房',
    checkInDate: '2026-07-08',
    checkOutDate: '2026-07-11',
    lastMessage: '我对您的民宿很感兴趣，请问还有可用日期吗？',
    lastActivity: initialConversationTimestamps.thread101.third,
    unreadCount: 2,
    closed: false,
  },
  {
    id: 102,
    channelId: 244,
    channelName: 'Airbnb',
    guestName: '杨子轩',
    bookingId: 'AB-240712',
    threadId: 'airbnb-thread-240712',
    listingId: 'listing-sea-02',
    listingName: '海景套房',
    roomTypeName: '海景套房',
    checkInDate: '2026-07-12',
    checkOutDate: '2026-07-28',
    lastMessage: '我正在计划带宠物一起入住，您的民宿接受宠物吗？',
    lastActivity: initialConversationTimestamps.thread102.third,
    unreadCount: 1,
    closed: false,
  },
  {
    id: 103,
    channelId: 19,
    channelName: 'Booking.com',
    guestName: '陈欣怡',
    bookingId: 'BK-240818',
    threadId: 'booking-thread-240818',
    listingId: 'listing-loft-03',
    listingName: '顶层阁楼房',
    roomTypeName: '顶层阁楼房',
    checkInDate: '2026-08-18',
    checkOutDate: '2026-08-21',
    lastMessage: '当然，附近有多个公交站和地铁站，交通非常方便。',
    lastActivity: initialConversationTimestamps.thread103.second,
    unreadCount: 0,
    closed: false,
  },
  {
    id: 104,
    channelId: 244,
    channelName: 'Airbnb',
    guestName: 'Mina Park',
    bookingId: 'AB-240401',
    threadId: 'airbnb-thread-240401',
    listingId: 'listing-station-05',
    listingName: '车站公寓',
    roomTypeName: '车站公寓',
    checkInDate: '2026-04-12',
    checkOutDate: '2026-04-18',
    lastMessage: '谢谢这次接待，我们已经顺利退房。',
    lastActivity: toIsoDaysAgo(4, 11, 45),
    unreadCount: 0,
    closed: true,
  },
]

const createInitialMessages = (): MessageMap => ({
  101: sortByTimestampAsc([
    {
      id: 1001,
      threadId: 101,
      senderType: MessageSenderType.GUEST,
      senderName: '小幸运',
      content: '我对您的民宿很感兴趣，想先了解一下可入住日期。',
      timestamp: initialConversationTimestamps.thread101.first,
      deliveryStatus: 'SENT',
    },
    {
      id: 1002,
      threadId: 101,
      senderType: MessageSenderType.STAFF,
      senderName: '酒店客服',
      content: '您好，当前这套房源还有部分日期可订，您可以告诉我计划入住的时间。',
      timestamp: initialConversationTimestamps.thread101.second,
      deliveryStatus: 'SENT',
    },
    {
      id: 1003,
      threadId: 101,
      senderType: MessageSenderType.GUEST,
      senderName: '小幸运',
      content: '我对您的民宿很感兴趣，请问还有可用日期吗？',
      timestamp: initialConversationTimestamps.thread101.third,
      deliveryStatus: 'SENT',
    },
  ]),
  102: sortByTimestampAsc([
    {
      id: 2001,
      threadId: 102,
      senderType: MessageSenderType.GUEST,
      senderName: '杨子轩',
      content: '我在看这套房子，想问一下能不能带宠物入住？',
      timestamp: initialConversationTimestamps.thread102.first,
      deliveryStatus: 'SENT',
    },
    {
      id: 2002,
      threadId: 102,
      senderType: MessageSenderType.STAFF,
      senderName: '酒店客服',
      content: '您好，这套房源可接受小型宠物入住，入住前请提前说明。',
      timestamp: initialConversationTimestamps.thread102.second,
      deliveryStatus: 'SENT',
    },
    {
      id: 2003,
      threadId: 102,
      senderType: MessageSenderType.GUEST,
      senderName: '杨子轩',
      content: '我正在计划带宠物一起入住，您的民宿接受宠物吗？',
      timestamp: initialConversationTimestamps.thread102.third,
      deliveryStatus: 'SENT',
    },
  ]),
  103: sortByTimestampAsc([
    {
      id: 3001,
      threadId: 103,
      senderType: MessageSenderType.GUEST,
      senderName: '陈欣怡',
      content: '请问房子附近交通方便吗？离地铁站远不远？',
      timestamp: initialConversationTimestamps.thread103.first,
      deliveryStatus: 'SENT',
    },
    {
      id: 3002,
      threadId: 103,
      senderType: MessageSenderType.STAFF,
      senderName: '酒店客服',
      content: '当然，附近有多个公交站和地铁站，交通非常方便。',
      timestamp: initialConversationTimestamps.thread103.second,
      deliveryStatus: 'SENT',
    },
  ]),
  104: sortByTimestampAsc([
    {
      id: 4001,
      threadId: 104,
      senderType: MessageSenderType.GUEST,
      senderName: 'Mina Park',
      content: '谢谢这次接待，我们已经顺利退房。',
      timestamp: toIsoDaysAgo(4, 11, 45),
      deliveryStatus: 'SENT',
    },
  ]),
})

// Keep mock state mutable so list/detail/send interactions behave like a real inbox during styling work.
let mockThreads = createInitialThreads()
let mockMessages = createInitialMessages()
let nextMockMessageId = 5000

const getThreadById = (threadId: number) => {
  return mockThreads.find((thread) => thread.id === threadId) ?? null
}

const markThreadAsRead = (threadId: number) => {
  const thread = getThreadById(threadId)
  if (!thread) {
    return
  }

  thread.unreadCount = 0
}

const getThreadMessagesOrThrow = (threadId: number) => {
  const thread = getThreadById(threadId)
  if (!thread) {
    throw new Error('Thread not found')
  }

  return sortByTimestampAsc(mockMessages[threadId] ?? [])
}

const syncThreadPreview = (threadId: number, content: string, timestamp: string) => {
  const thread = getThreadById(threadId)
  if (!thread) {
    return
  }

  thread.lastMessage = content
  thread.lastActivity = timestamp
}

export const getMockMessageThreads = async () => {
  await sleep()

  return buildSuccessResponse(sortThreadsByActivityDesc(mockThreads).map(cloneThread))
}

export const getMockThreadMessages = async (threadId: number) => {
  await sleep()

  const items = getThreadMessagesOrThrow(threadId)
  markThreadAsRead(threadId)

  return buildSuccessResponse(items.map(cloneMessage))
}

export const pollMockThreadMessages = async (threadId: number, since: string) => {
  await sleep(80)

  const items = getThreadMessagesOrThrow(threadId)
  if (!since) {
    return buildSuccessResponse([])
  }

  const sinceTime = new Date(since).getTime()
  const nextItems = items.filter((item) => {
    return new Date(item.timestamp).getTime() > sinceTime
  })

  return buildSuccessResponse(nextItems.map(cloneMessage))
}

export const sendMockThreadMessage = async (threadId: number, data: MessageSendRequest) => {
  await sleep()

  const thread = getThreadById(threadId)
  if (!thread) {
    throw new Error('Thread not found')
  }

  if (thread.closed) {
    throw new Error('This thread is closed')
  }

  const content = data.content?.trim()
  if (!content) {
    throw new Error('Message content cannot be empty')
  }

  const timestamp = new Date().toISOString()
  const message: MessageDTO = {
    id: nextMockMessageId,
    threadId,
    senderType: MessageSenderType.STAFF,
    senderName: data.senderName?.trim() || 'Hotel Desk',
    content,
    timestamp,
    deliveryStatus: 'SENT',
  }

  nextMockMessageId += 1
  mockMessages[threadId] = sortByTimestampAsc([...(mockMessages[threadId] ?? []), message])
  syncThreadPreview(threadId, content, timestamp)

  return buildSuccessResponse(cloneMessage(message))
}

export const sendMockAiChatMessage = async (data: ChatMessageRequest) => {
  await sleep(180)

  const instruction = data.message
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
    .pop()

  const normalizedInstruction = instruction?.toLowerCase() || ''
  const draft = normalizedInstruction.includes('parking')
    ? 'Hello, we have confirmed the parking details for you. On-site parking is limited, and a public lot nearby is also available. We can send the exact location again before arrival.'
    : 'Hello, we have received your message and are confirming the details for you now. We will get back to you shortly. Feel free to share your arrival time or any other requests.'

  const response: ChatMessageResponse = {
    reply: [
      '[CONTEXT]',
      'This is a frontend-only mock AI response for styling and interaction testing.',
      '[/CONTEXT]',
      '[DRAFT]',
      draft,
      '[/DRAFT]',
    ].join('\n'),
    timestamp: new Date().toISOString(),
    sessionId: data.sessionId || 'mock-message-ai-session',
    status: 'mocked',
  }

  return buildSuccessResponse(response)
}
