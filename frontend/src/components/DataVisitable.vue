<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  fetchRecordList,
  deleteRecord,
  fetchUnexportedByUser,
  adminBatchDelete,
  fetchCountByRecorder,
  executeExportByDate,
  executeExportByHashes,
  fetchUnexportedToday,
  fetchExportStatus,
  getExportDownloadUrl
} from '../api/index.js'

const emit = defineEmits(['error'])
const ATTR_OPTIONS = ['appflyer', 'adjust', 'singular', 'tenjin']

const selectedAscribe = ref('')
const dateSearch = ref(getTodayDateStr())
const list = ref([])
const loading = ref(false)
const queried = ref(false)
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const selectedHashes = ref([])
const batchDeleting = ref(false)
const exporting = ref(false)
const polling = ref(false)
const fileReady = ref(false)
const fileName = ref('')
const resultMsg = ref('')
const resultSuccess = ref(false)
const currentUser = ref(localStorage.getItem('userName') || '')
const todayUnexported = ref(null)
const totalUnexported = ref(null)
const todayUnexportedLoading = ref(false)
let pollTimer = null

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const hasSelection = computed(() => selectedHashes.value.length > 0)

function getTodayDateStr() {
  const d = new Date()
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

function formatDateForQuery(dateStr) {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  return parts[0] + '/' + parseInt(parts[1]) + '/' + parseInt(parts[2])
}

async function fetchData(resetPage = false) {
  if (resetPage) currentPage.value = 1
  list.value = []
  total.value = 0
  loading.value = true
  queried.value = true
  selectedHashes.value = []
  try {
    const json = await fetchRecordList(
        selectedAscribe.value || null,
        false,
        currentPage.value,
        pageSize.value,
        currentUser.value,
        formatDateForQuery(dateSearch.value)
    )
    if (json.success) {
      list.value = json.data || []
      total.value = json.total || 0
    } else {
      emit('error', json.message || '查询失败')
      list.value = []
      total.value = 0
    }
  } catch (e) {
    emit('error', '查询请求失败：' + e.message)
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function prevPage() {
  if (currentPage.value > 1) { currentPage.value--; fetchData() }
}

async function refreshTodayUnexported() {
  todayUnexportedLoading.value = true
  todayUnexported.value = null
  try {
    const [statsJson, todayJson] = await Promise.all([
      fetchUnexportedByUser(currentUser.value),
      fetchUnexportedToday(currentUser.value, formatDateForQuery(getTodayDateStr()))
    ])
    if (statsJson.success && statsJson.data) {
      unexportedTotal.value = statsJson.data.total || 0
    }
    if (todayJson.success && todayJson.data) {
      todayUnexported.value = todayJson.data.count
    }
  } catch (e) { /* silent */ }
  finally { todayUnexportedLoading.value = false }
}

function nextPage() {
  if (currentPage.value < totalPages.value) { currentPage.value++; fetchData() }
}

function toggleSelectAll(event) {
  if (event.target.checked) {
    selectedHashes.value = list.value.map(item => item.hash)
  } else {
    selectedHashes.value = []
  }
}

function isAllSelected() {
  return list.value.length > 0 && selectedHashes.value.length === list.value.length
}

async function doExportByDate() {
  if (exporting.value || polling.value) return
  exporting.value = true
  resultMsg.value = ''
  resultSuccess.value = false
  fileReady.value = false
  fileName.value = ''
  try {
    const json = await executeExportByDate(currentUser.value, formatDateForQuery(dateSearch.value))
    resultSuccess.value = json.success
    resultMsg.value = json.message || ''
    if (json.success) {
      fileName.value = json.data?.fileName || ''
      startPolling()
    }
  } catch (e) {
    resultSuccess.value = false
    resultMsg.value = '导出请求失败：' + e.message
  } finally {
    exporting.value = false
  }
}

async function doExportByHashes() {
  if (selectedHashes.value.length === 0) {
    emit('error', '请先选择要导出的数据')
    return
  }
  if (exporting.value || polling.value) return
  exporting.value = true
  resultMsg.value = ''
  resultSuccess.value = false
  fileReady.value = false
  fileName.value = ''
  try {
    const json = await executeExportByHashes(currentUser.value, selectedHashes.value)
    resultSuccess.value = json.success
    resultMsg.value = json.message || ''
    if (json.success) {
      fileName.value = json.data?.fileName || ''
      startPolling()
    }
  } catch (e) {
    resultSuccess.value = false
    resultMsg.value = '导出请求失败：' + e.message
  } finally {
    exporting.value = false
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
      }
    } catch (e) { /* ignore */ }
  }, 2000)
}

function stopPolling() {
  polling.value = false
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

function doDownload() {
  const url = getExportDownloadUrl(currentUser.value)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName.value
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => {
    fileReady.value = false
    fileName.value = ''
    resultMsg.value = '✅ 文件已下载，服务器端文件已自动删除'
    resultSuccess.value = true
  }, 1500)
}

async function doBatchDelete() {
  if (selectedHashes.value.length === 0) {
    emit('error', '请先选择要删除的记录')
    return
  }
  if (!confirm(`确定要删除选中的 ${selectedHashes.value.length} 条记录吗？此操作不可撤销！`)) return
  batchDeleting.value = true
  try {
    const json = await adminBatchDelete(selectedHashes.value)
    if (json.success) {
      alert('✅ ' + json.message)
      selectedHashes.value = []
      await fetchData()
    } else {
      emit('error', json.message || '批量删除失败')
    }
  } catch (e) {
    emit('error', '批量删除请求失败：' + e.message)
  } finally {
    batchDeleting.value = false
  }
}

async function fetchDataByName() {
  const json = await fetchCountByRecorder()
  if (json.success) {
    alert("用户" + currentUser.value + "已入库" + json.data + "条")
  } else {
    alert("获取事件数失败，请联系工作人员")
  }
}

function sup() {
  alert('谢谢你，成都。谢谢你，我的同桌：吴雨芹。谢谢我自己：完整的完成了这一切！！！')
}

onMounted(() => { fetchData() })
onUnmounted(() => { stopPolling() })
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>数据看板</h2>
      <div class="page-header-sub">当前用户：{{ currentUser }}</div>
    </div>

    <h5 @click="fetchDataByName()" style="cursor:pointer;color:#667eea;margin:0 0 16px;">点我查看今日入库数量</h5>

    <!-- 筛选区 -->
    <div class="filter-card">
      <div class="filter-top-row">
        <div class="filter-section">
          <div class="filter-section-title">归因筛选</div>
          <div class="radio-group">
            <label class="radio-tag radio-all" :class="{ active: selectedAscribe === '' }">
              <input type="radio" name="ascribe" value="" :checked="selectedAscribe === ''" @change="selectedAscribe = ''; fetchData(true)" />
              <span>全部</span>
            </label>
            <label v-for="opt in ATTR_OPTIONS" :key="opt" class="radio-tag" :class="[opt, { active: selectedAscribe === opt }]">
              <input type="radio" name="ascribe" :value="opt" :checked="selectedAscribe === opt" @change="selectedAscribe = opt; fetchData(true)" />
              <span>{{ opt }}</span>
            </label>
          </div>
        </div>
        <div class="filter-top-actions">
          <div class="filter-field">
            <label class="field-label">日期</label>
            <input v-model="dateSearch" type="date" class="filter-input filter-date" @change="fetchData(true)" />
          </div>
        </div>
      </div>
    </div>

    <!-- 导出操作区 -->
    <div class="export-action-bar">
      <div class="export-action-left">
        <button class="btn-action btn-export-date" @click="doExportByDate" :disabled="exporting || polling || hasSelection">
          {{ exporting ? '导出中...' : '📤 按当前日期导出' }}
        </button>
        <button class="btn-action btn-export-hash" @click="doExportByHashes" :disabled="exporting || polling || !hasSelection">
          {{ exporting ? '导出中...' : '📤 导出选中行 (' + selectedHashes.length + ')' }}
        </button>
        <button class="btn-action btn-refresh-unexported" @click="refreshTodayUnexported" :disabled="todayUnexportedLoading">
          {{ todayUnexportedLoading ? '查询中...' : '🔄 刷新未导出' }}
        </button>
        <span class="export-tip" v-if="hasSelection">⚠️ 已切换为选中行导出模式，按日期导出已禁用</span>
        <span class="total-unexported-badge" v-if="totalUnexported !== null" :class="{ 'badge-zero': totalUnexported === 0 }">
          总未导出: <strong>{{ totalUnexported }}</strong> 条
        </span>
        <span class="today-unexported-badge" v-if="todayUnexported !== null" :class="{ 'badge-zero': todayUnexported === 0 }">
          今日未导出: <strong>{{ todayUnexported }}</strong> 条
        </span>
      </div>
      <div class="export-action-right" v-if="fileReady">
        <div class="download-section-inline">
          <span class="download-file-name">📄 {{ fileName }}</span>
          <button class="btn-action btn-download" @click="doDownload">⬇ 下载文件</button>
        </div>
      </div>
    </div>

    <div v-if="resultMsg" class="feedback" :class="{ 'feedback-ok': resultSuccess, 'feedback-err': !resultSuccess }">
      {{ resultMsg }}
    </div>

    <div v-if="polling" class="polling-tip">
      <div class="state-spinner" style="width:18px;height:18px;border-width:2px;margin:0;display:inline-block;vertical-align:middle;margin-right:8px;"></div>
      文件生成中，请稍候...
    </div>

    <!-- 结果工具栏 -->
    <div v-if="queried" class="result-toolbar">
      <div class="result-toolbar-left">
        <span class="result-count">共 <strong>{{ total }}</strong> 条 · 第 {{ currentPage }}/{{ totalPages }} 页</span>
      </div>
      <div class="result-toolbar-right">
        <label class="batch-select-label" v-if="list.length > 0">
          <input type="checkbox" :checked="isAllSelected()" @change="toggleSelectAll" />
          <span>全选</span>
        </label>
        <span class="selected-count" v-if="selectedHashes.length > 0">已选 {{ selectedHashes.length }} 条</span>
        <button class="btn-batch-del" v-if="selectedHashes.length > 0" @click="doBatchDelete" :disabled="batchDeleting">
          {{ batchDeleting ? '删除中...' : '🗑 批量删除' }}
        </button>
      </div>
    </div>

    <!-- 加载/空状态 -->
    <div v-if="loading && !queried" class="state-block">
      <div class="state-spinner"></div>
      <div class="state-text">正在查询数据...</div>
    </div>
    <div v-if="!loading && queried && list.length === 0" class="state-block">
      <div class="state-icon">📭</div>
      <div class="state-text">没有符合条件的数据</div>
    </div>

    <!-- 数据表格 -->
    <div v-if="list.length > 0" class="table-wrapper">
      <table class="data-table">
        <thead>
        <tr>
          <th class="th-check"><input type="checkbox" :checked="isAllSelected()" @change="toggleSelectAll" /></th>
          <th>#</th><th>URL</th><th>Bundle ID</th><th>归因</th><th>事件数</th>
          <th>异常类型</th><th>记录日期</th><th>记录人</th><th>备注</th><th>导出</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(item, index) in list" :key="item.hash" :class="{ 'row-selected': selectedHashes.includes(item.hash) }">
          <td class="td-check"><input type="checkbox" :value="item.hash" v-model="selectedHashes" /></td>
          <td class="cell-index">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
          <td :title="item.URL" class="cell-url">{{ item.URL }}</td>
          <td class="cell-mono">{{ item.bundleId }}</td>
          <td>{{ item.ascribe || '-' }}</td>
          <td class="cell-num">{{ item.event_number }}</td>
          <td>
            <span v-if="item.exception_type" class="exception-tag" :class="'ex-' + item.exception_type">{{ item.exception_type }}</span>
            <span v-else>-</span>
          </td>
          <td>{{ item.record_data || '-' }}</td>
          <td>{{ item.recorder || '-' }}</td>
          <td :title="item.remark" class="cell-remark">{{ item.remark || '-' }}</td>
          <td><span :class="item.isOutput === 1 ? 'tag-exported' : 'tag-unexported'">{{ item.isOutput === 1 ? '已导出' : '未导出' }}</span></td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- 分页 -->
    <div v-if="total > 0" class="pagination">
      <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">‹ 上一页</button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">下一页 ›</button>
    </div>

    <div @click="sup" class="sup-area">点我试试</div>
  </div>
</template>

<style scoped>
.admin-page { width: 100%; max-width: 1600px; margin: 0 auto; padding: 0 20px 40px; box-sizing: border-box; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 8px; padding-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.page-header h2 { margin: 0; font-size: 22px; color: #1a1a2e; font-weight: 700; }
.page-header-sub { font-size: 13px; color: #999; }

.sup-area { color: white; font-size: 10px; background-size: 15px; width: 20px; height: 15px; }

/* ========== 筛选区 ========== */
.filter-card { background: #fafbfc; border-radius: 14px; padding: 20px; margin-bottom: 16px; border: 1px solid #eaeaea; }
.filter-top-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
.filter-top-actions { display: flex; align-items: flex-end; gap: 12px; flex-shrink: 0; }
.filter-section { margin-bottom: 14px; }
.filter-section:last-child { margin-bottom: 0; }
.filter-section-title { font-size: 12px; font-weight: 700; color: #999; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; }
.filter-field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 11px; font-weight: 700; color: #999; text-transform: uppercase; letter-spacing: 0.3px; }
.filter-input { padding: 8px 14px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 10px; outline: none; width: 180px; transition: border-color 0.2s, box-shadow 0.2s; background: #fff; }
.filter-input:focus { border-color: #667eea; box-shadow: 0 0 0 3px rgba(102,126,234,0.12); }
.filter-date { cursor: pointer; font-family: inherit; color-scheme: light; }
.filter-date::-webkit-calendar-picker-indicator { cursor: pointer; opacity: 0.6; transition: opacity 0.2s; }
.filter-date::-webkit-calendar-picker-indicator:hover { opacity: 1; }

.btn-export-hash:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(67,233,123,0.35); }
.btn-refresh-unexported { background: linear-gradient(135deg, #60a5fa, #3b82f6); color: #fff; }
.btn-refresh-unexported:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(59,130,246,0.35); }
.today-unexported-badge { display: inline-flex; align-items: center; gap: 4px; background: #fef3c7; color: #92400e; padding: 6px 16px; border-radius: 20px; font-size: 13px; font-weight: 600; border: 1px solid #fde68a; }
.today-unexported-badge.badge-zero { background: #dcfce7; color: #166534; border-color: #bbf7d0; }
.today-unexported-badge strong { font-weight: 800; font-size: 15px; }

.today-unexported-badge strong { font-weight: 800; font-size: 15px; }
.total-unexported-badge { display: inline-flex; align-items: center; gap: 4px; background: #e0e7ff; color: #3730a3; padding: 6px 16px; border-radius: 20px; font-size: 13px; font-weight: 600; border: 1px solid #c7d2fe; }
.total-unexported-badge.badge-zero { background: #dcfce7; color: #166534; border-color: #bbf7d0; }
.total-unexported-badge strong { font-weight: 800; font-size: 15px; }

.radio-group { display: flex; gap: 8px; flex-wrap: wrap; }
.radio-tag { display: flex; align-items: center; gap: 4px; padding: 6px 16px; border-radius: 20px; font-size: 12px; font-weight: 600; cursor: pointer; color: #fff; transition: all 0.2s; opacity: 0.35; border: 2px solid transparent; }
.radio-tag input { display: none; }
.radio-tag.active { opacity: 1; box-shadow: 0 2px 10px rgba(0,0,0,0.18); }
.radio-tag.radio-all { background: #607D8B; }
.radio-tag.appflyer { background: #4CAF50; }
.radio-tag.adjust { background: #FF9800; }
.radio-tag.singular { background: #2196F3; }
.radio-tag.tenjin { background: #9C27B0; }

/* ========== 导出操作区 ========== */
.export-action-bar { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; background: #f8f9fc; border-radius: 12px; margin-bottom: 14px; border: 1px solid #eaeaea; flex-wrap: wrap; gap: 10px; }
.export-action-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.export-action-right { display: flex; align-items: center; gap: 10px; }
.export-tip { font-size: 12px; color: #e65100; font-weight: 600; background: #fff3e0; padding: 4px 12px; border-radius: 8px; }

.btn-action { border: none; padding: 10px 24px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600; transition: transform 0.15s, box-shadow 0.15s; }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }
.btn-export-date { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.btn-export-date:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(102,126,234,0.35); }
.btn-export-hash { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #fff; }
.btn-export-hash:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(67,233,123,0.35); }
.btn-download { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #fff; padding: 8px 20px; font-size: 12px; }
.btn-download:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(67,233,123,0.35); }

.download-section-inline { display: flex; align-items: center; gap: 10px; background: #e8f5e9; padding: 8px 16px; border-radius: 10px; border-left: 3px solid #43a047; }
.download-file-name { font-size: 13px; font-weight: 600; color: #2e7d32; }

.feedback { font-size: 13px; font-weight: 600; padding: 10px 14px; border-radius: 10px; margin-bottom: 12px; }
.feedback-ok { background: #dcfce7; color: #166534; }
.feedback-err { background: #fef2f2; color: #991b1b; }

.polling-tip { display: flex; align-items: center; font-size: 13px; color: #667eea; font-weight: 600; margin-bottom: 12px; padding: 10px 16px; background: #eef2ff; border-radius: 10px; animation: blink 1.2s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

/* ========== 结果工具栏 ========== */
.result-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background: #f8f9fc; border-radius: 10px; margin-bottom: 12px; border: 1px solid #eaeaea; }
.result-toolbar-left { display: flex; align-items: center; gap: 12px; }
.result-count { font-size: 13px; color: #666; }
.result-count strong { color: #667eea; font-weight: 800; }
.result-toolbar-right { display: flex; align-items: center; gap: 12px; }
.batch-select-label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #666; cursor: pointer; }
.selected-count { font-size: 12px; font-weight: 700; color: #667eea; background: #eef2ff; padding: 3px 10px; border-radius: 20px; }
.btn-batch-del { background: linear-gradient(135deg, #f87171, #ef4444); color: #fff; border: none; padding: 6px 16px; font-size: 12px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.2s; }
.btn-batch-del:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(239,68,68,0.4); }
.btn-batch-del:disabled { opacity: 0.5; cursor: not-allowed; }

/* ========== 状态占位 ========== */
.state-block { text-align: center; padding: 50px 20px; }
.state-icon { font-size: 48px; margin-bottom: 12px; }
.state-text { color: #aaa; font-size: 14px; }
.state-spinner { width: 32px; height: 32px; border: 3px solid #e0e0e0; border-top-color: #667eea; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 12px; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ========== 表格 ========== */
.table-wrapper { overflow-x: auto; border-radius: 10px; border: 1px solid #e8e8e8; margin-bottom: 16px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 1000px; }
.data-table th { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 11px 14px; white-space: nowrap; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px; }
.data-table td { padding: 10px 14px; border-bottom: 1px solid #f0f0f0; color: #333; vertical-align: middle; }
.data-table tbody tr:hover td { background: #f8f9ff; }
.data-table tr:last-child td { border-bottom: none; }
.cell-index { color: #aaa; font-weight: 600; width: 40px; text-align: center; }
.cell-url { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cell-remark { max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cell-mono { font-family: 'SF Mono', 'Fira Code', monospace; font-size: 11px; color: #555; }
.cell-num { font-weight: 700; text-align: center; }
.tag-exported { background: #dcfce7; color: #166534; padding: 3px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; }
.tag-unexported { background: #fef3c7; color: #92400e; padding: 3px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; }

/* ========== 表格增强 ========== */
.th-check { width: 40px; text-align: center; }
.td-check { text-align: center; }
.td-check input { cursor: pointer; width: 16px; height: 16px; accent-color: #667eea; }
.row-selected td { background: #eef2ff !important; }

.exception-tag { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; background: #fef3c7; color: #92400e; }
.exception-tag.ex-正常 { background: #dcfce7; color: #166534; }
.exception-tag.ex-验证已解决 { background: #dcfce7; color: #166534; }

/* ========== 分页 ========== */
.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 14px 0; }
.page-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; padding: 8px 22px; border-radius: 10px; cursor: pointer; font-weight: 600; font-size: 13px; transition: transform 0.15s; }
.page-btn:hover { transform: translateY(-1px); }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }
.page-info { font-size: 13px; color: #666; font-weight: 600; }
</style>
