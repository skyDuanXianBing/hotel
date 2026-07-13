import { Request, Response } from 'express'
import express from 'express'
import chalk from 'chalk'

import tokenValidator from './tokenValidator'

const router = express.Router()

interface StoredAttachment {
  attachmentId: string
  fileContent: string
  mimeType: 'image/jpeg' | 'image/png'
}

const storedAttachments = new Map<string, StoredAttachment>()

const requiredAttachmentFields = [
  'hotel_id',
  'channel_id',
  'channel_hotel_id',
  'thread_id',
] as const

const detectMimeType = (content: Buffer): StoredAttachment['mimeType'] | null => {
  if (content.length >= 3 && content[0] === 0xff && content[1] === 0xd8 && content[2] === 0xff) {
    return 'image/jpeg'
  }
  if (
    content.length >= 8 &&
    content[0] === 0x89 &&
    content[1] === 0x50 &&
    content[2] === 0x4e &&
    content[3] === 0x47 &&
    content[4] === 0x0d &&
    content[5] === 0x0a &&
    content[6] === 0x1a &&
    content[7] === 0x0a
  ) {
    return 'image/png'
  }
  return null
}

/**
 * POST /SUAPI/jservice/messagingAB
 *
 * 模拟 Su API 接收消息（Guest Messaging）回复。
 * - Bearer Token 校验
 * - 打印消息回复内容到控制台
 * - 固定返回 { status: 'Success', response: 'Successfully posted' }
 */
router.post('/SUAPI/jservice/messagingAB', tokenValidator, (req: Request, res: Response) => {
  // eslint-disable-next-line no-console
  console.log(chalk.cyan('[mock-su-api/messagingAB] received message:'))
  // eslint-disable-next-line no-console
  console.log(chalk.gray(JSON.stringify(req.body, null, 2)))

  res.json({
    status: 'Success',
    response: 'Successfully posted',
  })
})

router.post('/SUAPI/jservice/message/sendAttachment', tokenValidator, (req: Request, res: Response) => {
  for (const field of requiredAttachmentFields) {
    if (req.body?.[field] === undefined || String(req.body[field]).trim() === '') {
      res.status(400).json({ Status: 'Failed', Message: `${field} is required` })
      return
    }
  }
  if (Number(req.body.channel_id) !== 19) {
    res.status(400).json({ Status: 'Failed', Message: 'Only Booking.com channel_id=19 is supported' })
    return
  }
  if (typeof req.body.file_content !== 'string' || !req.body.file_content.trim()) {
    res.status(400).json({ Status: 'Failed', Message: 'file_content is required' })
    return
  }

  let bytes: Buffer
  try {
    bytes = Buffer.from(req.body.file_content, 'base64')
  } catch {
    res.status(400).json({ Status: 'Failed', Message: 'file_content must be Base64' })
    return
  }
  const mimeType = detectMimeType(bytes)
  if (!mimeType || bytes.length === 0 || bytes.length > 10 * 1024 * 1024) {
    res.status(400).json({ Status: 'Failed', Message: 'Only JPEG/PNG up to 10 MB is supported' })
    return
  }

  const attachmentId = `mock-attachment-${Date.now()}-${storedAttachments.size + 1}`
  storedAttachments.set(attachmentId, {
    attachmentId,
    fileContent: req.body.file_content,
    mimeType,
  })
  res.json({
    Status: 'Success',
    Data: {
      message_id: `mock-message-${Date.now()}-${storedAttachments.size}`,
      attachment_id: attachmentId,
    },
  })
})

router.post('/SUAPI/jservice/message/downloadAttachment', tokenValidator, (req: Request, res: Response) => {
  for (const field of requiredAttachmentFields) {
    if (req.body?.[field] === undefined || String(req.body[field]).trim() === '') {
      res.status(400).json({ Status: 'Failed', Message: `${field} is required` })
      return
    }
  }
  const requested = Array.isArray(req.body.attachments) ? req.body.attachments : []
  if (!requested.length) {
    res.status(400).json({ Status: 'Failed', Message: 'attachments is required' })
    return
  }

  const attachmentUrlPath = requested.flatMap((item: unknown) => {
    const attachmentId =
      item && typeof item === 'object' && 'attachment_id' in item
        ? String((item as { attachment_id: unknown }).attachment_id)
        : ''
    const stored = storedAttachments.get(attachmentId)
    if (!stored) {
      return []
    }
    return [
      {
        attachment_id: stored.attachmentId,
        attachment: stored.fileContent,
        mime_type: stored.mimeType,
      },
    ]
  })
  if (!attachmentUrlPath.length) {
    res.status(404).json({ Status: 'Failed', Message: 'Attachment not found' })
    return
  }
  res.json({ Status: 'Success', Data: { attachment_url_path: attachmentUrlPath } })
})

export default router
