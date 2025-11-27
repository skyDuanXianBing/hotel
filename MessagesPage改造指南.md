# MessagesPage.vue 改造指南

## 📋 改造概述

将MessagesPage.vue从使用`realTimeChat` API改为使用`virtualMailbox` API,保持UI界面不变。

---

## 🔄 改造步骤

### 步骤1: 替换导入语句

**原代码 (第158-171行):**
```typescript
import {
  sendRealTimeMessage,
  getRoomMessages,
  pollNewMessages,
  getActiveChatRooms,
  closeChatRoom,
  type RealTimeChatRequest,
  type RealTimeChatResponse,
  type ChatRoomInfo,
  SenderType,
  MessageType,
  ChatStatus,
} from '@/api/realTimeChat'
```

**新代码:**
```typescript
import {
  getActiveMailboxes,
  getMailboxMessages,
  pollMailboxMessages,
  sendMailboxMessage,
  closeMailbox,
  type VirtualMailboxDTO,
  type EmailMessageDTO,
  EmailSenderType,
  MailboxStatus
} from '@/api/virtualMailbox'
```

---

### 步骤2: 更新类型定义

**原代码 (第173-179行):**
```typescript
interface Message {
  id: number
  senderType: SenderType
  content: string
  timestamp: Date
  senderName?: string
}
```

**新代码:**
```typescript
interface Message {
  id: number
  senderType: EmailSenderType
  content: string
  timestamp: Date
  senderName?: string
}
```

**原代码 (第194行):**
```typescript
const conversations = ref<ChatRoomInfo[]>([])
```

**新代码:**
```typescript
const conversations = ref<VirtualMailboxDTO[]>([])
```

---

### 步骤3: 更新变量名

**所有 `roomId` 改为 `mailboxId`:**

| 位置 | 原代码 | 新代码 |
|------|--------|--------|
| 第24行 | `conversation.roomId` | `conversation.id` |
| 第26行 | `activeRoomId === conversation.roomId` | `activeMailboxId === conversation.id` |
| 第27行 | `selectConversation(conversation.roomId)` | `selectConversation(conversation.id)` |
| 第185行 | `const activeRoomId = ref<string \| null>(null)` | `const activeMailboxId = ref<number \| null>(null)` |
| 第226行 | `conv.roomId === activeRoomId.value` | `conv.id === activeMailboxId.value` |

---

### 步骤4: 更新API调用

#### 4.1 刷新列表 (第263-274行)

**原代码:**
```typescript
const refreshChatRooms = async () => {
  try {
    const response = await getActiveChatRooms()
    if (response.success && response.data) {
      conversations.value = response.data
    }
  } catch (error) {
    console.error('刷新聊天室列表失败:', error)
    ElMessage.error('刷新失败')
  }
}
```

**新代码:**
```typescript
const refreshMailboxes = async () => {
  try {
    const response = await getActiveMailboxes()
    if (response.success && response.data) {
      conversations.value = response.data
    }
  } catch (error) {
    console.error('刷新邮箱列表失败:', error)
    ElMessage.error('刷新失败')
  }
}
```

#### 4.2 选择会话 (第276-281行)

**原代码:**
```typescript
const selectConversation = async (roomId: string) => {
  activeRoomId.value = roomId
  await loadRoomMessages(roomId)
  lastPollTime.value = new Date().toISOString()
}
```

**新代码:**
```typescript
const selectConversation = async (mailboxId: number) => {
  activeMailboxId.value = mailboxId
  await loadMailboxMessages(mailboxId)
  lastPollTime.value = new Date().toISOString()
}
```

#### 4.3 加载消息 (第283-301行)

**原代码:**
```typescript
const loadRoomMessages = async (roomId: string) => {
  try {
    const response = await getRoomMessages(roomId)
    if (response.success && response.data) {
      messages.value = response.data.map((msg: RealTimeChatResponse) => ({
        id: msg.messageId,
        senderType: msg.senderType,
        content: msg.messageContent,
        timestamp: new Date(msg.sentAt),
        senderName: msg.senderName,
      }))
      await scrollToBottom()
    }
  } catch (error) {
    console.error('加载聊天室消息失败:', error)
    ElMessage.error('加载消息失败')
  }
}
```

**新代码:**
```typescript
const loadMailboxMessages = async (mailboxId: number) => {
  try {
    const response = await getMailboxMessages(mailboxId)
    if (response.success && response.data) {
      messages.value = response.data.map((msg: EmailMessageDTO) => ({
        id: msg.id,
        senderType: msg.senderType,
        content: msg.content,
        timestamp: new Date(msg.timestamp),
        senderName: msg.senderName,
      }))
      await scrollToBottom()
    }
  } catch (error) {
    console.error('加载邮箱消息失败:', error)
    ElMessage.error('加载消息失败')
  }
}
```

#### 4.4 发送消息 (第303-343行)

**原代码:**
```typescript
const sendMessage = async () => {
  if (!newMessage.value.trim() || !activeRoomId.value || isLoading.value) return

  const messageContent = newMessage.value.trim()
  isLoading.value = true

  try {
    const chatRequest: RealTimeChatRequest = {
      roomId: activeRoomId.value,
      message: messageContent,
      senderType: SenderType.STAFF,
      senderName: '客服',
      messageType: MessageType.TEXT,
    }

    const response = await sendRealTimeMessage(chatRequest)

    if (response.success && response.data) {
      messages.value.push({
        id: response.data.messageId,
        senderType: SenderType.STAFF,
        content: messageContent,
        timestamp: new Date(response.data.sentAt),
        senderName: response.data.senderName,
      })

      newMessage.value = ''
      await scrollToBottom()
      lastPollTime.value = new Date().toISOString()
    } else {
      ElMessage.error('消息发送失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('网络错误,消息发送失败')
  } finally {
    isLoading.value = false
  }
}
```

**新代码:**
```typescript
const sendMessage = async () => {
  if (!newMessage.value.trim() || !activeMailboxId.value || isLoading.value) return

  const messageContent = newMessage.value.trim()
  isLoading.value = true

  try {
    const response = await sendMailboxMessage(activeMailboxId.value, {
      content: messageContent,
      senderName: '客服'
    })

    if (response.success && response.data) {
      messages.value.push({
        id: response.data.id,
        senderType: response.data.senderType,
        content: response.data.content,
        timestamp: new Date(response.data.timestamp),
        senderName: response.data.senderName,
      })

      newMessage.value = ''
      await scrollToBottom()
      lastPollTime.value = new Date().toISOString()
    } else {
      ElMessage.error('消息发送失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('网络错误,消息发送失败')
  } finally {
    isLoading.value = false
  }
}
```

#### 4.5 AI回复 (第345-395行)

**保持AI回复部分不变**,只需更新最后发送消息的部分:

```typescript
// 在 sendAiReply 函数中,替换:
const aiReplyRequest: RealTimeChatRequest = {
  roomId: activeRoomId.value,
  message: response.data.reply,
  senderType: SenderType.STAFF,
  senderName: 'AI客服',
  messageType: MessageType.TEXT,
}

const realtimeResponse = await sendRealTimeMessage(aiReplyRequest)

// 改为:
const mailboxResponse = await sendMailboxMessage(activeMailboxId.value!, {
  content: response.data.reply,
  senderName: 'AI客服'
})
```

#### 4.6 关闭会话 (第397-421行)

**原代码:**
```typescript
const closeChatRoomDialog = async () => {
  if (!activeRoomId.value) return

  try {
    await ElMessageBox.confirm('确认关闭此会话吗?', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const response = await closeChatRoom(activeRoomId.value)
    if (response.success) {
      ElMessage.success('会话已关闭')
      await refreshChatRooms()
      activeRoomId.value = null
      messages.value = []
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('关闭会话失败:', error)
      ElMessage.error('关闭会话失败')
    }
  }
}
```

**新代码:**
```typescript
const closeMailboxDialog = async () => {
  if (!activeMailboxId.value) return

  try {
    await ElMessageBox.confirm('确认关闭此会话吗?', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const response = await closeMailbox(activeMailboxId.value)
    if (response.success) {
      ElMessage.success('会话已关闭')
      await refreshMailboxes()
      activeMailboxId.value = null
      messages.value = []
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('关闭会话失败:', error)
      ElMessage.error('关闭会话失败')
    }
  }
}
```

#### 4.7 轮询新消息 (第423-461行)

**原代码:**
```typescript
const pollMessages = async () => {
  if (!activeRoomId.value) return

  try {
    const response = await pollNewMessages(activeRoomId.value, lastPollTime.value)

    if (response.success && response.data && response.data.length > 0) {
      let hasNewMessages = false

      response.data.forEach((msg: RealTimeChatResponse) => {
        const exists = messages.value.find((m) => m.id === msg.messageId)
        if (!exists) {
          messages.value.push({
            id: msg.messageId,
            senderType: msg.senderType,
            content: msg.messageContent,
            timestamp: new Date(msg.sentAt),
            senderName: msg.senderName,
          })
          hasNewMessages = true
        }
      })

      if (hasNewMessages) {
        await scrollToBottom()
      }
    }

    lastPollTime.value = new Date().toISOString()
    await refreshChatRooms()
  } catch (error) {
    console.error('轮询消息失败:', error)
  }
}
```

**新代码:**
```typescript
const pollMessages = async () => {
  if (!activeMailboxId.value) return

  try {
    const response = await pollMailboxMessages(activeMailboxId.value, lastPollTime.value)

    if (response.success && response.data && response.data.length > 0) {
      let hasNewMessages = false

      response.data.forEach((msg: EmailMessageDTO) => {
        const exists = messages.value.find((m) => m.id === msg.id)
        if (!exists) {
          messages.value.push({
            id: msg.id,
            senderType: msg.senderType,
            content: msg.content,
            timestamp: new Date(msg.timestamp),
            senderName: msg.senderName,
          })
          hasNewMessages = true
        }
      })

      if (hasNewMessages) {
        await scrollToBottom()
      }
    }

    lastPollTime.value = new Date().toISOString()
    await refreshMailboxes()
  } catch (error) {
    console.error('轮询消息失败:', error)
  }
}
```

---

### 步骤5: 更新模板引用

在`<template>`部分更新函数调用:

| 原函数名 | 新函数名 |
|---------|---------|
| `refreshChatRooms` | `refreshMailboxes` |
| `closeChatRoomDialog` | `closeMailboxDialog` |

---

### 步骤6: 更新初始化函数

**原代码 (第517-528行):**
```typescript
const initialize = async () => {
  await refreshChatRooms()

  if (conversations.value.length > 0) {
    await selectConversation(conversations.value[0].roomId)
  }

  startPolling()
}
```

**新代码:**
```typescript
const initialize = async () => {
  await refreshMailboxes()

  if (conversations.value.length > 0) {
    await selectConversation(conversations.value[0].id)
  }

  startPolling()
}
```

---

### 步骤7: 更新状态相关函数

**原代码 (第235-261行):**
```typescript
const getStatusType = (status: ChatStatus) => {
  switch (status) {
    case ChatStatus.ACTIVE:
      return 'success'
    case ChatStatus.WAITING:
      return 'warning'
    case ChatStatus.CLOSED:
      return 'info'
    default:
      return 'info'
  }
}

const getStatusText = (status: ChatStatus) => {
  switch (status) {
    case ChatStatus.ACTIVE:
      return '活跃'
    case ChatStatus.WAITING:
      return '等待中'
    case ChatStatus.CLOSED:
      return '已关闭'
    default:
      return '未知'
  }
}
```

**新代码:**
```typescript
const getStatusType = (status: MailboxStatus) => {
  switch (status) {
    case MailboxStatus.ACTIVE:
      return 'success'
    case MailboxStatus.CLOSED:
      return 'info'
    default:
      return 'info'
  }
}

const getStatusText = (status: MailboxStatus) => {
  switch (status) {
    case MailboxStatus.ACTIVE:
      return '活跃'
    case MailboxStatus.CLOSED:
      return '已关闭'
    default:
      return '未知'
  }
}
```

---

### 步骤8: 更新最后一条客人消息

**原代码 (第229-233行):**
```typescript
const lastGuestMessage = computed(() => {
  const guestMessages = messages.value.filter(m => m.senderType === SenderType.GUEST)
  return guestMessages.length > 0 ? guestMessages[guestMessages.length - 1].content : ''
})
```

**新代码:**
```typescript
const lastGuestMessage = computed(() => {
  const guestMessages = messages.value.filter(m => m.senderType === EmailSenderType.GUEST)
  return guestMessages.length > 0 ? guestMessages[guestMessages.length - 1].content : ''
})
```

---

## ✅ 改造完成检查清单

- [ ] 导入语句已替换为virtualMailbox API
- [ ] 类型定义已更新(SenderType → EmailSenderType, ChatRoomInfo → VirtualMailboxDTO)
- [ ] roomId 已全部改为 mailboxId
- [ ] refreshChatRooms → refreshMailboxes
- [ ] loadRoomMessages → loadMailboxMessages
- [ ] closeChatRoomDialog → closeMailboxDialog
- [ ] 所有API调用已替换
- [ ] 模板中的函数引用已更新
- [ ] 状态枚举已更新(ChatStatus → MailboxStatus)
- [ ] 运行 `bun run type-check` 确认无类型错误

---

## 📝 注意事项

1. **UI保持不变**: 改造只涉及API调用,前端UI完全不变
2. **邮件延迟**: 虚拟邮箱系统通过邮件收发,可能有30秒-1分钟延迟
3. **轮询间隔**: 保持2秒轮询间隔,与原系统一致
4. **错误处理**: 保持原有的错误提示逻辑

---

## 🎯 测试步骤

改造完成后,需要进行以下测试:

1. ✅ 前端类型检查: `bun run type-check`
2. ✅ 打开收件箱页面,确认无控制台错误
3. ✅ 选择一个会话,查看消息列表
4. ✅ 发送一条消息,确认发送成功
5. ✅ 等待30秒,确认轮询正常工作
6. ✅ 测试关闭会话功能

---

## 🚀 后续优化建议

1. 添加"邮件发送中"状态提示
2. 显示邮件延迟时间
3. 支持重新打开已关闭的会话
4. 添加邮件附件支持(未来版本)
