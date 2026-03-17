<template>
  <StatisticsLayout>
    <div class="wrap">
      <div class="top">
        <div class="title">登记详情</div>
        <div class="actions">
          <el-button @click="back">返回</el-button>
          <el-button :loading="loading" @click="load">刷新</el-button>
          <el-button type="primary" :disabled="!detail" @click="downloadPdf">下载PDF</el-button>
        </div>
      </div>

      <div v-if="!detail" class="empty">暂无数据</div>

      <div v-else class="card">
        <div class="meta">
          <div><b>订单号：</b>{{ detail.channelOrderNumber || detail.orderNumber }}</div>
          <div><b>客人：</b>{{ detail.guestName }}</div>
          <div><b>日期：</b>{{ detail.checkInDate }} ~ {{ detail.checkOutDate }}</div>
          <div><b>预订房型：</b>{{ detail.roomTypeName || '-' }}</div>
          <div><b>房间号：</b>{{ detail.roomNumber || '-' }}</div>
          <div><b>状态：</b>{{ detail.status }}</div>
          <div v-if="detail.reviewNote"><b>备注：</b>{{ detail.reviewNote }}</div>
        </div>

        <div class="section">
          <div class="section-title">人员信息</div>
          <el-table :data="detail.guests" border stripe style="width: 100%">
            <el-table-column prop="sortOrder" label="#" width="60" />
            <el-table-column label="姓名" min-width="160">
              <template #default="{ row }">{{ row.lastName }} {{ row.firstName }}</template>
            </el-table-column>
            <el-table-column prop="nationality" label="国籍" min-width="120" />
            <el-table-column prop="residenceType" label="居住地" min-width="110" />
            <el-table-column prop="passportNumber" label="Passport" min-width="150" />
            <el-table-column prop="priorStay" label="前泊地" min-width="120" />
            <el-table-column prop="nextDestination" label="行先" min-width="120" />
          </el-table>
        </div>

        <div class="section" v-if="detail.attachments && detail.attachments.length">
          <div class="section-title">附件</div>
          <el-table :data="detail.attachments" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="90" />
            <el-table-column label="Guest" min-width="120">
              <template #default="{ row }">{{ guestLabel(row.guestId) }}</template>
            </el-table-column>
            <el-table-column prop="type" label="类型" min-width="120" />
            <el-table-column prop="originalName" label="文件名" min-width="220" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="previewAttachment(row)">预览</el-button>
                <el-button size="small" type="primary" @click="downloadAttachment(row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="section">
          <div class="section-title">审核操作</div>
          <el-input v-model="note" type="textarea" :rows="3" placeholder="备注（可选）" />
          <div class="btns">
            <el-button type="danger" :loading="acting" @click="reject">驳回</el-button>
            <el-button type="success" :loading="acting" @click="approve">通过</el-button>
          </div>
        </div>

        <div class="section">
          <div class="section-title">审核历史</div>
          <el-table :data="detail.reviewLogs" border stripe style="width: 100%">
            <el-table-column prop="createdAt" label="时间" min-width="170" />
            <el-table-column prop="action" label="动作" min-width="110" />
            <el-table-column prop="operatorUserId" label="用户ID" min-width="110" />
            <el-table-column prop="note" label="备注" min-width="220" />
          </el-table>
        </div>

        <div class="section" v-if="false">
          <div class="section-title">发送消息给客人</div>
          <div class="msg-grid">
            <div class="msg-row">
              <div class="msg-label">消息类型</div>
              <el-select v-model="sendType" placeholder="请选择" style="width: 240px">
                <el-option label="通过后入住信息" value="APPROVED_INFO" />
                <el-option label="驳回重新填写" value="REJECT_REQUEST" />
                <el-option label="提醒填写" value="REMINDER" />
              </el-select>
            </div>

            <div class="msg-row">
              <div class="msg-label">快捷回复</div>
              <el-select
                v-model="quickReplyId"
                filterable
                clearable
                placeholder="选择一条快捷回复插入"
                style="width: 420px"
                :loading="quickReplyLoading"
                @change="applyQuickReply"
              >
                <el-option v-for="q in quickReplies" :key="q.id" :label="q.title" :value="q.id" />
              </el-select>
              <el-button @click="loadQuickReplies" :loading="quickReplyLoading">刷新</el-button>
            </div>

            <div class="msg-row">
              <div class="msg-label">消息内容</div>
              <el-input v-model="sendContent" type="textarea" :rows="5" placeholder="可使用变量：{{guest_name}} {{order_number}} {{checkin_date}} {{checkout_date}} {{room_number}} {{registration_link}}" />
            </div>

            <div class="msg-actions">
              <el-button :disabled="!detail" @click="fillDefaultTemplate">填充默认模板</el-button>
              <el-button type="primary" :loading="sending" :disabled="!detail" @click="sendMessage">发送</el-button>
            </div>
            <div class="msg-hint">
              <div v-if="lastSendStatus" class="msg-status">最近一次发送结果：{{ lastSendStatus }}</div>
              <div class="msg-status-sub">如显示 WAITING_*，表示缺 thread/listingId，需要等 Su webhook 入库后再点发送；可先复制内容用其它渠道发送。</div>
            </div>
          </div>
        </div>

        <div class="section" v-if="detail.messageLogs && detail.messageLogs.length">
          <div class="section-title">消息历史</div>
          <el-table :data="detail.messageLogs" border stripe style="width: 100%">
            <el-table-column prop="createdAt" label="时间" min-width="170" />
            <el-table-column prop="type" label="类型" min-width="140" />
            <el-table-column prop="sendStatus" label="状态" min-width="150" />
            <el-table-column prop="errorMessage" label="错误/原因" min-width="260" />
            <el-table-column label="内容" min-width="360">
              <template #default="{ row }">
                <div class="msg-content">{{ row.content }}</div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" @click="copyText(row.content)">复制</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import axios from 'axios'

type Detail = {
  formId: number
  orderNumber: string
  channelOrderNumber?: string
  status: string
  guestName: string
  checkInDate: string
  checkOutDate: string
  roomTypeName?: string
  roomNumber?: string
  reviewNote?: string
  guests: any[]
  attachments?: Array<{ id: number; guestId?: number; type: string; originalName?: string }>
  reviewLogs: any[]
  messageLogs: Array<{
    id: number
    type: string
    channel?: string
    toIdentifier?: string
    content: string
    sendStatus: string
    errorMessage?: string
    createdAt: string
  }>
}

type QuickReplyItem = { id: number; title: string; message: string }

const route = useRoute()
const router = useRouter()

const detail = ref<Detail | null>(null)
const loading = ref(false)
const acting = ref(false)
const note = ref('')

const sending = ref(false)
const sendType = ref<'APPROVED_INFO' | 'REJECT_REQUEST' | 'REMINDER'>('APPROVED_INFO')
const sendContent = ref('')
const lastSendStatus = ref<string>('')

const quickReplies = ref<QuickReplyItem[]>([])
const quickReplyLoading = ref(false)
const quickReplyId = ref<number | null>(null)

function formId(): string {
  return route.params.formId as string
}

async function load() {
  loading.value = true
  try {
    const resp = await request.get(`/registrations/${formId()}`)
    detail.value = resp.data as Detail
    note.value = detail.value?.reviewNote || ''
    // keep last status hint from newest message log
    lastSendStatus.value = detail.value?.messageLogs?.[0]?.sendStatus || ''
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function loadQuickReplies() {
  quickReplyLoading.value = true
  try {
    const resp = await request.get('/quick-replies')
    quickReplies.value = (resp.data || []) as QuickReplyItem[]
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载快捷回复失败')
  } finally {
    quickReplyLoading.value = false
  }
}

function applyQuickReply(id: number | null) {
  if (!id) return
  const found = quickReplies.value.find((q) => q.id === id)
  if (!found) return
  // append with a blank line separator
  const base = sendContent.value?.trim() ? sendContent.value.trim() + '\n\n' : ''
  sendContent.value = base + (found.message || '')
  quickReplyId.value = null
}

function fillDefaultTemplate() {
  if (!detail.value) return
  if (sendType.value === 'APPROVED_INFO') {
    sendContent.value =
      '您好 {{guest_name}}，您的入住登记已通过。\n' +
      '订单号：{{order_number}}\n' +
      '入住：{{checkin_date}}，退房：{{checkout_date}}\n' +
      '如需补充信息请回复本消息。\n\n' +
      '登记链接：{{registration_link}}'
  } else if (sendType.value === 'REJECT_REQUEST') {
    sendContent.value =
      '您好 {{guest_name}}，您的入住登记信息需要补充/修正。\n' +
      '请点击链接重新填写并提交：{{registration_link}}\n' +
      '订单号：{{order_number}}'
  } else {
    sendContent.value =
      '您好 {{guest_name}}，入住前请完成入住登记：{{registration_link}}\n' +
      '订单号：{{order_number}}\n' +
      '入住日期：{{checkin_date}}'
  }
}

async function sendMessage() {
  if (!detail.value) return
  const content = sendContent.value?.trim()
  if (!content) {
    ElMessage.warning('请填写消息内容')
    return
  }
  sending.value = true
  try {
    const resp = await request.post(`/registrations/${detail.value.formId}/messages/send`, {
      type: sendType.value,
      content,
      senderName: '前台',
    })
    const dto = resp.data
    lastSendStatus.value = dto?.sendStatus || ''
    ElMessage.success(`已提交发送：${dto?.sendStatus || 'OK'}`)
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text || '')
    ElMessage.success('已复制')
  } catch {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = text || ''
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    ta.remove()
    ElMessage.success('已复制')
  }
}

function back() {
  router.back()
}

async function approve() {
  if (!detail.value) return
  acting.value = true
  try {
    await request.post(`/registrations/${detail.value.formId}/approve`, { note: note.value })
    ElMessage.success('已通过')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

async function reject() {
  if (!detail.value) return
  acting.value = true
  try {
    await request.post(`/registrations/${detail.value.formId}/reject`, { note: note.value })
    ElMessage.success('已驳回')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    acting.value = false
  }
}

function downloadPdf() {
  if (!detail.value) return
  const base = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'
  const url = `${base}/registrations/${detail.value.formId}/pdf`

  const token = localStorage.getItem('token')
  const currentStore = localStorage.getItem('currentStore')
  let storeId: string | undefined
  if (currentStore) {
    try {
      const parsed = JSON.parse(currentStore)
      if (parsed?.id) storeId = String(parsed.id)
    } catch {
      // ignore
    }
  }

  axios
    .get(url, {
      responseType: 'blob',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(storeId ? { 'X-Store-Id': storeId } : {}),
      },
    })
    .then((res) => {
      const blobUrl = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = `registration-${detail.value?.orderNumber || detail.value?.formId}.pdf`
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(blobUrl)
    })
    .catch((e) => {
      ElMessage.error(e?.response?.data?.message || e?.message || '下载失败')
    })
}

function guestLabel(guestId?: number): string {
  if (!detail.value || !guestId) return '-'
  const idx = (detail.value.guests || []).findIndex((g: any) => g.id === guestId)
  return idx >= 0 ? `#${idx + 1}` : String(guestId)
}

function authHeaders(): Record<string, string> {
  const token = localStorage.getItem('token')
  const currentStore = localStorage.getItem('currentStore')
  let storeId: string | undefined
  if (currentStore) {
    try {
      const parsed = JSON.parse(currentStore)
      if (parsed?.id) storeId = String(parsed.id)
    } catch {
      // ignore
    }
  }
  return {
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(storeId ? { 'X-Store-Id': storeId } : {}),
  }
}

function attachmentUrl(attId: number): string {
  const base = (import.meta.env.VITE_API_BASE_URL as string) || '/api/v1'
  return `${base}/registrations/${formId()}/attachments/${attId}`
}

function previewAttachment(att: { id: number; originalName?: string }) {
  if (!detail.value) return
  axios
    .get(attachmentUrl(att.id), {
      responseType: 'blob',
      headers: authHeaders(),
    })
    .then((res) => {
      const blobUrl = URL.createObjectURL(res.data)
      window.open(blobUrl, '_blank')
      // best-effort revoke later
      setTimeout(() => URL.revokeObjectURL(blobUrl), 30_000)
    })
    .catch((e) => {
      ElMessage.error(e?.response?.data?.message || e?.message || '预览失败')
    })
}

function downloadAttachment(att: { id: number; originalName?: string }) {
  if (!detail.value) return
  axios
    .get(attachmentUrl(att.id), {
      responseType: 'blob',
      headers: authHeaders(),
    })
    .then((res) => {
      const blobUrl = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = att.originalName || `attachment-${att.id}`
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(blobUrl)
    })
    .catch((e) => {
      ElMessage.error(e?.response?.data?.message || e?.message || '下载失败')
    })
}

onMounted(load)
onMounted(loadQuickReplies)
</script>

<style scoped>
.wrap {
  padding: 16px;
}
.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.title {
  font-size: 16px;
  font-weight: 700;
}
.actions {
  display: flex;
  gap: 10px;
}
.card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.meta {
  display: grid;
  grid-template-columns: 1fr;
  gap: 6px;
  margin-bottom: 12px;
}
.section {
  margin-top: 14px;
}
.section-title {
  font-weight: 700;
  margin-bottom: 8px;
}
.btns {
  margin-top: 10px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.empty {
  padding: 20px;
  color: #666;
}

.msg-grid {
  display: grid;
  gap: 10px;
}

.msg-row {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 10px;
  align-items: start;
}

.msg-label {
  color: #333;
  padding-top: 6px;
}

.msg-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.msg-hint {
  color: #666;
  font-size: 12px;
  line-height: 1.4;
}

.msg-status {
  color: #333;
  margin-bottom: 4px;
}

.msg-status-sub {
  color: #888;
}

.msg-content {
  white-space: pre-wrap;
}
</style>
