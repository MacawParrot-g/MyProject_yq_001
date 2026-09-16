<template>
  <div>
    <div style="margin-bottom: 20px;">
      <button class="btn-refresh" @click="fetchList" :disabled="loading">{{ loading ? '查询中...' : '刷新未导出数据' }}</button>
    </div>

    <div v-if="loading && !queried" class="loading">正在查询数据库...</div>

    <div v-if="!loading && queried && list.length === 0 && !fileReady" class="export-empty">
      <div class="empty-icon">这里什么都木有啊QwQ</div>
      <div>当前用户所有数据均已导出并下载，暂无未下载文件</div>
    </div>

    <div v-if="list.length > 0">
      <div class="export-header">
        <div class="export-count">用户 <strong>{{ currentUser }}</strong> 共 <span>{{ list.length }}</span> 条未导出数据</div>
        <button class="btn-export-exec" @click="doExport" :disabled="executing">
          {{ executing ? '导出中...' : '一键导出' }}
        </button>
      </div>
    </div>

    <div v-if="resultMsg" class="export-result-box" :class="resultSuccess ? 'success' : 'fail'">
      <div class="result-title">{{ resultSuccess ? '✅ 导出成功' : '❌ 导出失败' }}</div>
      <div>{{ resultMsg }}</div>
    </div>

    <div v-if="fileReady" class="download-section">
      <div class="download-info">文件已生成：{{ fileName }}</div>
      <button class="btn-download" @click="doDownload" :disabled="downloading">
        {{ downloading ? '下载中...' : '⬇ 下载文件' }}
      </button>
    </div>

    <div v-if="polling" class="polling-tip">文件生成中，请稍候...</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { fetchUnexportedByUser, executeExportByUser, fetchExportStatus, getExportDownloadUrl } from '../api/index.js'

const emit = defineEmits(['error'])
const list = ref([])
const loading = ref(false)
const queried = ref(false)
const executing = ref(false)
const downloading = ref(false)
const resultMsg = ref('')
const resultSuccess = ref(false)
const fileReady = ref(false)
const fileName = ref('')
const polling = ref(false)
const currentUser = ref(localStorage.getItem('userName') || '')

let pollTimer = null

async function fetchList() {
  loading.value = true; queried.value = false; resultMsg.value = ''
  try {
    const json = await fetchUnexportedByUser(currentUser.value)
    if (json.success) {
      list.value = json.data?.list || []
    } else {
      emit('error', json.message || '查询失败'); list.value = []
    }
  } catch (e) {
    emit('error', '查询请求失败：' + e.message); list.value = []
  } finally {
    loading.value = false; queried.value = true
  }
}

async function doExport() {
  executing.value = true; resultMsg.value = ''; resultSuccess.value = false
  fileReady.value = false; fileName.value = ''
  try {
    const json = await executeExportByUser(currentUser.value)
    resultSuccess.value = json.success
    resultMsg.value = json.message || ''
    if (json.success) {
      fileName.value = json.data?.fileName || ''
      startPolling()
    }
  } catch (e) {
    resultSuccess.value = false; resultMsg.value = '请求失败：' + e.message
  } finally {
    executing.value = false
  }
}

function startPolling() {
  polling.value = true
  pollTimer = setInterval(async () => {
    try {
      const json = await fetchExportStatus(currentUser.value)
      if (json.success && json.data?.ready) {
        stopPolling()
        fileReady.value = true
        fileName.value = json.data.fileName || fileName.value
        await fetchList()
      }
    } catch (e) { /* ignore */ }
  }, 2000)
}

function stopPolling() {
  polling.value = false
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

function doDownload() {
  downloading.value = true
  const url = getExportDownloadUrl(currentUser.value)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName.value
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => {
    downloading.value = false
    fileReady.value = false
    fileName.value = ''
    resultMsg.value = '文件已下载，服务器端文件已自动删除'
    resultSuccess.value = true
  }, 1500)
}

onMounted(() => { fetchList() })
onUnmounted(() => { stopPolling() })
</script>

<style scoped>
.download-section {
  margin-top: 20px;
  padding: 16px;
  background: #e8f5e9;
  border-radius: 8px;
  border-left: 4px solid #43a047;
}
.download-info {
  font-size: 14px;
  color: #2e7d32;
  margin-bottom: 12px;
  font-weight: 600;
}
.btn-download {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
  color: #fff;
  border: none;
  padding: 12px 32px;
  font-size: 15px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: transform 0.2s, box-shadow 0.2s;
}
.btn-download:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(67, 233, 123, 0.4);
}
.btn-download:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.polling-tip {
  margin-top: 14px;
  font-size: 13px;
  color: #667eea;
  font-weight: 600;
  animation: blink 1.2s infinite;
}
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
