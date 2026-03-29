export const copyTextToClipboard = async (text: string) => {
  if (!text) {
    throw new Error('复制内容为空')
  }

  if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }

  if (typeof document === 'undefined') {
    throw new Error('当前环境不支持复制')
  }

  const textArea = document.createElement('textarea')
  textArea.value = text
  textArea.setAttribute('readonly', 'true')
  textArea.style.position = 'fixed'
  textArea.style.left = '-9999px'
  document.body.appendChild(textArea)
  textArea.select()

  const didCopy = document.execCommand('copy')
  document.body.removeChild(textArea)

  if (!didCopy) {
    throw new Error('复制失败，请稍后重试')
  }
}

export const downloadBlobFile = (blob: Blob, fileName: string) => {
  if (typeof document === 'undefined' || typeof window === 'undefined') {
    throw new Error('当前环境不支持下载文件')
  }

  const objectUrl = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  window.URL.revokeObjectURL(objectUrl)
}

export const openBlobPreview = (blob: Blob) => {
  if (typeof window === 'undefined') {
    return false
  }

  const objectUrl = window.URL.createObjectURL(blob)
  const previewWindow = window.open(objectUrl, '_blank', 'noopener,noreferrer')

  if (!previewWindow) {
    window.URL.revokeObjectURL(objectUrl)
    return false
  }

  window.setTimeout(() => {
    window.URL.revokeObjectURL(objectUrl)
  }, 30000)

  return true
}
