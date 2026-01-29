<template>
  <StatisticsLayout>
    <div class="wrap">
      <div class="header">
        <div class="title">人员信息审查</div>
        <div class="actions">
          <el-select v-model="status" placeholder="状态" clearable style="width: 160px" @change="load">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe style="width: 100%" @row-click="go">
        <el-table-column prop="orderNumber" label="订单号" min-width="150" />
        <el-table-column prop="guestName" label="客人" min-width="120" />
        <el-table-column prop="checkInDate" label="入住" min-width="110" />
        <el-table-column prop="checkOutDate" label="退房" min-width="110" />
        <el-table-column prop="status" label="状态" min-width="110" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="170" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="170" />
      </el-table>
    </div>
  </StatisticsLayout>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

type Row = {
  formId: number
  orderNumber: string
  guestName: string
  checkInDate: string
  checkOutDate: string
  status: string
  submittedAt?: string
  updatedAt?: string
}

const router = useRouter()
const rows = ref<Row[]>([])
const loading = ref(false)
const status = ref<string | null>(null)

async function load() {
  loading.value = true
  try {
    const resp = await request.get('/registrations', { params: status.value ? { status: status.value } : {} })
    rows.value = (resp.data || []) as Row[]
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function go(row: Row) {
  router.push({ name: 'DataCenterRegistrationDetail', params: { formId: row.formId } })
}

onMounted(load)
</script>

<style scoped>
.wrap {
  padding: 16px;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.title {
  font-size: 16px;
  font-weight: 700;
}
.actions {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
