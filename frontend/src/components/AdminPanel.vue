<script setup>import { ref, computed, onMounted, reactive, onUnmounted } from 'vue'
import {
  fetchUserList,
  createUser,
  deleteUser,
  fetchSystemInfo,
  resetUserPassword,
  kickUser,
  fetchRecordListByDate,
  fetchDailyReport,
  adminRecordSearch,
  adminRecordStats,
  adminBatchDelete,
  adminRecordSummary,
  adminBatchImport,
  executeExportByDate,
  executeExportByHashes,
  executeExportAll,
  fetchUnexportedByUser,
  fetchExportStatus,
  getExportDownloadUrl
} from '../api/index.js'

const emit = defineEmits(['error'])
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const advFilters = reactive({
  dateFrom: '',
  dateTo: '',
  bundleId: '',
  keyword: '',
  exceptionType: '',
  isOutput: null,
  recorder: ''
})
const advExpanded = ref(true)
const statsData = ref(null)
const selectedUrls = ref([])
const batchDeleting = ref(false)
const exceptionOptions = ref([])
const recorderOptions = ref([])
const ATTR_OPTIONS = ['appflyer', 'adjust', 'singular', 'tenjin']

const activeTab = ref('data')
const selectedHashes = ref([])
const selectedAscribe = ref('')
const frozenOnly = ref(false)
const list = ref([])
const loading = ref(false)
const queried = ref(false)
const viewType = ref('ALL')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)
const recorderSearch = ref('')
const dateSearch = ref('')
const summaryData = ref(null)
const userList = ref([])
const userLoading = ref(false)
const createForm = reactive({ name: '', pwd: '', type: 'USER' })
const creating = ref(false)
const createMsg = ref('')

const exporting = ref(false)
const exportPolling = ref(false)
const exportFileReady = ref(false)
const exportFileName = ref('')
const exportResultMsg = ref('')
const exportResultSuccess = ref(false)
const currentUser = ref(localStorage.getItem('userName') || '')
let exportPollTimer = null
const unexportedTotal = ref(0)
let unexportedTimer = null

const hasSelection = computed(() => selectedHashes.value.length > 0)

const sysInfo = ref(null)
const sysLoading = ref(false)

const resetPwdUid = ref('')
const resetPwdNew = ref('')
const resetting = ref(false)
const resetMsg = ref('')
const kickBanSeconds = ref(300)
let userPollTimer = null

const importRawText = ref('')
const importing = ref(false)
const importMsg = ref('')
const importResult = ref(null)
const importExpanded = ref(false)

const reportVisible = ref(false)
const reportDate = ref('')
const reportLoading = ref(false)
const reportData = ref(null)



const viewTypeLabel = {
  ALL: '全部数据', APPFLYER: 'appflyer', ADJUST: 'adjust',
  SINGULAR: 'singular', TENJIN: 'tenjin', FROZEN: '已冻结数据',
  APPFLYER_FROZEN: 'appflyer · 已冻结', ADJUST_FROZEN: 'adjust · 已冻结',
  SINGULAR_FROZEN: 'singular · 已冻结', TENJIN_FROZEN: 'tenjin · 已冻结'
}

async function fetchData(resetPage = false) {
  if (resetPage) currentPage.value = 1
  list.value = []
  viewType.value = ''
  total.value = 0
  loading.value = true
  queried.value = true
  selectedUrls.value = []
  selectedHashes.value = []
  const filterParams = {
    dateFrom: advFilters.dateFrom,
    dateTo: advFilters.dateTo,
    bundleId: advFilters.bundleId.trim(),
    keyword: advFilters.keyword.trim(),
    exceptionType: advFilters.exceptionType,
    ascribe: selectedAscribe.value,
    frozenOnly: frozenOnly.value,
    recorder: advFilters.recorder.trim() || recorderSearch.value.trim(),
    isOutput: advFilters.isOutput
  }
  try {
    const [searchJson, summaryJson] = await Promise.all([
      adminRecordSearch({ ...filterParams, page: currentPage.value, size: pageSize.value }),
      adminRecordSummary(filterParams)
    ])
    if (searchJson.success) {
      list.value = searchJson.data || []
      viewType.value = searchJson.viewType || 'ALL'
      total.value = searchJson.total || 0
    } else {
      emit('error', searchJson.message || '查询失败')
    }
    if (summaryJson.success) {
      summaryData.value = summaryJson.data
    }
  } catch (e) {
    emit('error', '查询请求失败：' + e.message)
  } finally {
    loading.value = false
  }
}

function getSummaryAttrPercent(count) {
  if (!summaryData.value || summaryData.value.qualifiedCount === 0) return '0.0'
  return (count * 100 / summaryData.value.qualifiedCount).toFixed(1)
}

function startUnexportedPolling() {
  loadUnexportedCount()
  unexportedTimer = setInterval(loadUnexportedCount, 30000)
}

function stopUnexportedPolling() {
  if (unexportedTimer) { clearInterval(unexportedTimer); unexportedTimer = null }
}


function toggleSelectAll(event) {
  if (event.target.checked) {
    selectedHashes.value = list.value.map(item => item.hash)
  } else {
    selectedHashes.value = []
  }
}

function getTodayDateStr() {
  const d = new Date()
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
}

function formatDateForQuery(dateStr) {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  return parts[0] + '/' + parseInt(parts[1]) + '/' + parseInt(parts[2])
}

async function doExportAll() {
  if (exporting.value || exportPolling.value) return
  exporting.value = true
  exportResultMsg.value = ''
  exportResultSuccess.value = false
  exportFileReady.value = false
  exportFileName.value = ''
  try {
    const json = await executeExportAll(currentUser.value)
    exportResultSuccess.value = json.success
    exportResultMsg.value = json.message || ''
    if (json.success) {
      exportFileName.value = json.data?.fileName || ''
      startExportPolling()
    }
  } catch (e) {
    exportResultSuccess.value = false
    exportResultMsg.value = '导出请求失败：' + e.message
  } finally {
    exporting.value = false
  }
}

async function doExportByDate() {
  if (exporting.value || exportPolling.value) return
  exporting.value = true
  exportResultMsg.value = ''
  exportResultSuccess.value = false
  exportFileReady.value = false
  exportFileName.value = ''
  try {
    const dateVal = advFilters.dateFrom || getTodayDateStr()
    const json = await executeExportByDate(currentUser.value, formatDateForQuery(dateVal))
    exportResultSuccess.value = json.success
    exportResultMsg.value = json.message || ''
    if (json.success) {
      exportFileName.value = json.data?.fileName || ''
      startExportPolling()
    }
  } catch (e) {
    exportResultSuccess.value = false
    exportResultMsg.value = '导出请求失败：' + e.message
  } finally {
    exporting.value = false
  }
}

async function doExportByHashes() {
  if (selectedHashes.value.length === 0) {
    emit('error', '请先选择要导出的数据')
    return
  }
  if (exporting.value || exportPolling.value) return
  exporting.value = true
  exportResultMsg.value = ''
  exportResultSuccess.value = false
  exportFileReady.value = false
  exportFileName.value = ''
  try {
    const json = await executeExportByHashes(currentUser.value, selectedHashes.value)
    exportResultSuccess.value = json.success
    exportResultMsg.value = json.message || ''
    if (json.success) {
      exportFileName.value = json.data?.fileName || ''
      startExportPolling()
    }
  } catch (e) {
    exportResultSuccess.value = false
    exportResultMsg.value = '导出请求失败：' + e.message
  } finally {
    exporting.value = false
  }
}

function startExportPolling() {
  exportPolling.value = true
  exportPollTimer = setInterval(async () => {
    try {
      const json = await fetchExportStatus(currentUser.value)
      if (json.success && json.data?.ready) {
        stopExportPolling()
        exportFileReady.value = true
        exportFileName.value = json.data.fileName || exportFileName.value
      }
    } catch (e) { /* ignore */ }
  }, 2000)
}

function stopExportPolling() {
  exportPolling.value = false
  if (exportPollTimer) { clearInterval(exportPollTimer); exportPollTimer = null }
}

function doExportDownload() {
  const url = getExportDownloadUrl(currentUser.value)
  const a = document.createElement('a')
  a.href = url
  a.download = exportFileName.value
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  setTimeout(() => {
    exportFileReady.value = false
    exportFileName.value = ''
    exportResultMsg.value = '文件已下载'
    exportResultSuccess.value = true
  }, 1500)
}

function isAllSelected() {
  return list.value.length > 0 && selectedHashes.value.length === list.value.length
}

async function loadStats() {
  try {
    const json = await adminRecordStats()
    if (json.success) {
      statsData.value = json.data
      exceptionOptions.value = json.data.exceptionTypes || []
      recorderOptions.value = json.data.recorders || []
    }
  } catch (e) { /* silent */ }
}

function resetFilters() {
  advFilters.dateFrom = ''
  advFilters.dateTo = ''
  advFilters.bundleId = ''
  advFilters.keyword = ''
  advFilters.exceptionType = ''
  advFilters.isOutput = null
  advFilters.recorder = ''
  selectedAscribe.value = ''
  frozenOnly.value = false
  recorderSearch.value = ''
  dateSearch.value = ''
  fetchData(true)
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
      await loadStats()
    } else {
      emit('error', json.message || '批量删除失败')
    }
  } catch (e) {
    emit('error', '批量删除请求失败：' + e.message)
  } finally {
    batchDeleting.value = false
  }
}


async function queryDailyReport() {
  if (!reportDate.value) {
    alert('请选择日期')
    return
  }
  reportLoading.value = true
  reportData.value = null
  try {
    const json = await fetchDailyReport(reportDate.value)
    if (json.success) {
      reportData.value = json.data
    } else {
      emit('error', json.message || '查询日报失败')
    }
  } catch (e) {
    emit('error', '查询日报失败：' + e.message)
  } finally {
    reportLoading.value = false
  }
}

function closeReport() {
  reportVisible.value = false
  reportData.value = null
}
async function loadUnexportedCount() {
  try {
    const json = await fetchUnexportedByUser(currentUser.value)
    if (json.success && json.data) {
      unexportedTotal.value = json.data.total || 0
    }
  } catch (e) { /* silent */ }
}
function getAttrPercent(count) {
  if (!reportData.value || reportData.value.qualifiedCount === 0) return '0.0'
  return (count * 100 / reportData.value.qualifiedCount).toFixed(1)
}
function prevPage() { if (currentPage.value > 1) { currentPage.value--; fetchData() } }
function nextPage() { if (currentPage.value < totalPages.value) { currentPage.value++; fetchData() } }

async function loadUsers() {
  userLoading.value = true
  try {
    const json = await fetchUserList()
    if (json.success) {
      userList.value = json.data || []
    } else {
      emit('error', json.message || '获取用户列表失败')
    }
  } catch (e) {
    emit('error', '获取用户列表失败：' + e.message)
  } finally {
    userLoading.value = false
  }
}
function sup(){
  alert('谢谢你，成都。谢谢你，我的同桌：吴雨芹。谢谢我自己：完整的完成了这一切！！！')
}

async function handleBatchImport() {
  if (!importRawText.value.trim()) {
    importMsg.value = '❌ 请粘贴要导入的数据'
    return
  }
  const lineCount = importRawText.value.split('\n').filter(l => l.trim()).length
  if (!confirm(`确定要导入 ${lineCount} 条数据吗？\n所有数据的 isOutput 将设置为 1（已导出）。`)) return
  importing.value = true
  importMsg.value = ''
  importResult.value = null
  try {
    const json = await adminBatchImport(importRawText.value)
    if (json.success) {
      importMsg.value = '✅ ' + json.message
      importResult.value = json.data
      importRawText.value = ''
      await fetchData()
      await loadStats()
    } else {
      importMsg.value = '❌ ' + (json.message || '导入失败')
    }
  } catch (e) {
    importMsg.value = '❌ 导入请求失败：' + e.message
  } finally {
    importing.value = false
  }
}


async function handleCreateUser() {
  if (!createForm.name.trim() || !createForm.pwd.trim()) {
    emit('error', '姓名和密码不能为空')
    return
  }
  creating.value = true
  createMsg.value = ''
  try {
    const json = await createUser(createForm.name.trim(), createForm.pwd.trim(), createForm.type)
    if (json.success) {
      createMsg.value = '✅ ' + json.message + '，账号UID：' + (json.data?.uid || '')
      createForm.name = ''
      createForm.pwd = ''
      createForm.type = 'USER'
      await loadUsers()
    } else {
      createMsg.value = '❌ ' + (json.message || '创建失败')
    }
  } catch (e) {
    createMsg.value = '❌ 创建请求失败：' + e.message
  } finally {
    creating.value = false
  }
}

async function handleDeleteUser(uid) {
  if (!confirm('确定要删除用户 ' + uid + ' 吗？此操作不可撤销！')) return
  try {
    const json = await deleteUser(uid)
    if (json.success) {
      await loadUsers()
    } else {
      emit('error', json.message || '删除失败')
    }
  } catch (e) {
    emit('error', '删除请求失败：' + e.message)
  }
}

async function handleResetPassword() {
  if (!resetPwdUid.value.trim()) {
    resetMsg.value = '❌ 请输入要重置的UID'
    return
  }
  if (!resetPwdNew.value.trim()) {
    resetMsg.value = '❌ 请输入新密码'
    return
  }
  resetting.value = true
  resetMsg.value = ''
  try {
    const json = await resetUserPassword(resetPwdUid.value.trim(), resetPwdNew.value.trim())
    if (json.success) {
      resetMsg.value = '✅ ' + json.message
      resetPwdUid.value = ''
      resetPwdNew.value = ''
      await loadUsers()
    } else {
      resetMsg.value = '❌ ' + (json.message || '重置失败')
    }
  } catch (e) {
    resetMsg.value = '❌ 重置请求失败：' + e.message
  } finally {
    resetting.value = false
  }
}

async function handleKickUser(uid) {
  const ban = kickBanSeconds.value || 0
  if (!confirm(`确定要踢用户 ${uid} 下线吗？${ban > 0 ? '封禁 ' + ban + ' 秒' : '不封禁'}`)) return
  try {
    const json = await kickUser(uid, ban)
    if (json.success) {
      alert('✅ ' + json.message)
      await loadUsers()
    } else {
      emit('error', json.message || '踢人失败')
    }
  } catch (e) {
    emit('error', '踢人请求失败：' + e.message)
  }
}

function startUserPolling() {
  stopUserPolling()
  userPollTimer = setInterval(() => {
    if (activeTab.value === 'users') loadUsers()
  }, 15000)
}

function stopUserPolling() {
  if (userPollTimer) {
    clearInterval(userPollTimer)
    userPollTimer = null
  }
}

async function loadSystemInfo() {
  sysLoading.value = true
  try {
    const json = await fetchSystemInfo()
    if (json.success) sysInfo.value = json.data
  } catch (e) { /* silent */ }
  finally { sysLoading.value = false }
}
onMounted(() => {
  fetchData()
  loadUsers()
  loadSystemInfo()
  loadStats()
  startUserPolling()
  startUnexportedPolling()
})

onUnmounted(() => {
  stopUserPolling()
  stopExportPolling()
  stopUnexportedPolling()
})
</script>

<template>
  <div class="admin-page">
    <div class="page-header">
      <h2>管理员后台管理系统</h2>
      <div class="page-header-sub">自动化测试数据管理平台</div>
    </div>

    <div class="admin-tabs">
      <button class="admin-tab" :class="{ active: activeTab === 'data' }" @click="activeTab = 'data'">
        <span class="tab-icon">📊</span><span class="tab-text">数据管理</span>
      </button>
      <button class="admin-tab" :class="{ active: activeTab === 'users' }" @click="activeTab = 'users'">
        <span class="tab-icon">👥</span><span class="tab-text">用户管理</span>
      </button>
<!--      <button class="admin-tab" :class="{ active: activeTab === 'system' }" @click="activeTab = 'system'; loadSystemInfo()">-->
<!--        <span class="tab-icon">🖥️</span><span class="tab-text">系统状态</span>-->
<!--      </button>-->
    </div>

    <!-- ==================== 数据管理 Tab ==================== -->
    <div v-if="activeTab === 'data'" class="admin-section">

      <!-- 顶部统计卡片 -->
      <div class="stats-row" v-if="statsData">
        <div class="stat-card">
          <div class="stat-card-icon">📦</div>
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statsData.totalCount }}</div>
            <div class="stat-card-label">总记录数</div>
          </div>
        </div>
        <div class="stat-card stat-card-exported">
          <div class="stat-card-icon">✅</div>
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statsData.exportedCount }}</div>
            <div class="stat-card-label">已导出</div>
          </div>
        </div>
        <div class="stat-card stat-card-pending">
          <div class="stat-card-icon">⏳</div>
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statsData.unexportedCount }}</div>
            <div class="stat-card-label">未导出</div>
          </div>
        </div>
        <div class="stat-card stat-card-frozen">
          <div class="stat-card-icon">❄️</div>
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statsData.frozenCount }}</div>
            <div class="stat-card-label">已冻结</div>
          </div>
        </div>
      </div>

      <div v-if="summaryData" class="quality-panel">
        <div class="quality-header">
          <span class="quality-title">📊 质量分析</span>
          <span class="quality-scope">{{ total > 0 ? '当前筛选范围' : '全量数据' }}</span>
        </div>
        <div class="quality-summary">
          <div class="quality-stat">
            <div class="quality-stat-value">{{ summaryData.totalCount }}</div>
            <div class="quality-stat-label">总记录数</div>
          </div>
          <div class="quality-stat quality-stat-ok">
            <div class="quality-stat-value">{{ summaryData.qualifiedCount }}</div>
            <div class="quality-stat-label">合格数（有归因）</div>
          </div>
          <div class="quality-stat quality-stat-bad">
            <div class="quality-stat-value">{{ summaryData.unqualifiedCount }}</div>
            <div class="quality-stat-label">不合格数</div>
          </div>
          <div class="quality-stat quality-stat-rate">
            <div class="quality-stat-value">{{ summaryData.qualifyRate }}%</div>
            <div class="quality-stat-label">合格率</div>
          </div>
        </div>
        <div class="quality-attr-section">
          <div class="quality-attr-title">归因占比（基于合格数据 {{ summaryData.qualifiedCount }} 条）</div>
          <div class="quality-attr-bars">
            <div class="quality-attr-bar-item" v-for="type in ['appflyer', 'adjust', 'singular', 'tenjin']" :key="type">
              <div class="quality-attr-bar-header">
                <span class="quality-attr-bar-name" :class="type">{{ type }}</span>
                <span class="quality-attr-bar-count">{{ summaryData.attributions[type] }} 条</span>
                <span class="quality-attr-bar-percent">{{ getSummaryAttrPercent(summaryData.attributions[type]) }}%</span>
              </div>
              <div class="quality-attr-bar-track">
                <div class="quality-attr-bar-fill" :class="type" :style="{ width: (summaryData.qualifiedCount > 0 ? summaryData.attributions[type] * 100 / summaryData.qualifiedCount : 0) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 高级搜索面板 -->
      <div class="filter-card">
        <div class="filter-top-row">
          <div class="filter-section">
            <div class="filter-section-title">归因筛选</div>
            <div class="radio-group">
              <label class="radio-tag radio-all" :class="{ active: selectedAscribe === '' }">
                <input type="radio" name="adminAscribe" value="" :checked="selectedAscribe === ''" @change="selectedAscribe = ''" />
                <span>全部</span>
              </label>
              <label v-for="opt in ATTR_OPTIONS" :key="opt" class="radio-tag" :class="[opt, { active: selectedAscribe === opt }]">
                <input type="radio" name="adminAscribe" :value="opt" :checked="selectedAscribe === opt" @change="selectedAscribe = opt" />
                <span>{{ opt }}</span>
              </label>
            </div>
          </div>
          <div class="filter-top-actions">
            <label class="checkbox-tag" :class="{ active: frozenOnly }">
              <input type="checkbox" v-model="frozenOnly" />
              <span>仅冻结</span>
            </label>
            <button class="btn-link" @click="advExpanded = !advExpanded">
              {{ advExpanded ? '收起筛选 ▲' : '展开筛选 ▼' }}
            </button>
          </div>
        </div>

        <div v-if="advExpanded" class="filter-advanced">
          <div class="filter-grid">
            <div class="filter-field">
              <label class="field-label">起始日期</label>
              <input v-model="advFilters.dateFrom" type="date" class="filter-input filter-date" />
            </div>
            <div class="filter-field">
              <label class="field-label">结束日期</label>
              <input v-model="advFilters.dateTo" type="date" class="filter-input filter-date" />
            </div>
            <div class="filter-field">
              <label class="field-label">Bundle ID</label>
              <input v-model="advFilters.bundleId" class="filter-input" placeholder="精确匹配" @keyup.enter="fetchData(true)" />
            </div>
            <div class="filter-field">
              <label class="field-label">关键词</label>
              <input v-model="advFilters.keyword" class="filter-input" placeholder="URL/Bundle/备注" @keyup.enter="fetchData(true)" />
            </div>
            <div class="filter-field">
              <label class="field-label">异常类型</label>
              <select v-model="advFilters.exceptionType" class="filter-input">
                <option value="">全部</option>
                <option v-for="opt in exceptionOptions" :key="opt" :value="opt">{{ opt }}</option>
              </select>
            </div>
            <div class="filter-field">
              <label class="field-label">记录人</label>
              <select v-model="advFilters.recorder" class="filter-input">
                <option value="">全部</option>
                <option v-for="opt in recorderOptions" :key="opt" :value="opt">{{ opt }}</option>
              </select>
            </div>
            <div class="filter-field">
              <label class="field-label">导出状态</label>
              <select v-model="advFilters.isOutput" class="filter-input">
                <option :value="null">全部</option>
                <option :value="1">已导出</option>
                <option :value="0">未导出</option>
              </select>
            </div>
          </div>
          <div class="filter-action-row">
            <button class="btn-query" @click="fetchData(true)" :disabled="loading">
              {{ loading ? '查询中...' : '🔍 查询' }}
            </button>
            <button class="btn-reset" @click="resetFilters">↻ 重置</button>
            <span class="filter-tip">支持回车键快速查询</span>
          </div>
        </div>
      </div>

      <div class="export-action-bar">
        <div class="export-action-left">
          <button class="btn-action btn-export-date" @click="doExportByDate" :disabled="exporting || exportPolling || hasSelection">
            {{ exporting ? '导出中...' : '📤 按日期导出' + (advFilters.dateFrom ? '(' + advFilters.dateFrom + ')' : '(今日)') }}
          </button>
          <button class="btn-action btn-export-hash" @click="doExportByHashes" :disabled="exporting || exportPolling || !hasSelection">
            {{ exporting ? '导出中...' : '📤 导出选中行 (' + selectedHashes.length + ')' }}
          </button>
          <button class="btn-action btn-export-all" @click="doExportAll" :disabled="exporting || exportPolling || hasSelection || unexportedTotal === 0">
            {{ exporting ? '导出中...' : '📤 导出全部未导出' }}
          </button>
          <span class="export-tip" v-if="hasSelection">⚠️ 已切换为选中行导出模式</span>
        </div>
        <div class="export-action-right">
          <div class="unexported-badge" :class="{ 'unexported-zero': unexportedTotal === 0 }">
            <span class="unexported-badge-dot"></span>
            <span class="unexported-badge-text">未导出: <strong>{{ unexportedTotal }}</strong> 条</span>
          </div>
          <div class="download-section-inline" v-if="exportFileReady">
            <span class="download-file-name">📄 {{ exportFileName }}</span>
            <button class="btn-action btn-download-sm" @click="doExportDownload">⬇ 下载文件</button>
          </div>
        </div>
      </div>

      <div v-if="exportResultMsg" class="feedback" :class="{ 'feedback-ok': exportResultSuccess, 'feedback-err': !exportResultSuccess }">
        {{ exportResultMsg }}
      </div>

      <div v-if="exportPolling" class="polling-tip">
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

      <div v-if="loading && !queried" class="state-block">
        <div class="state-spinner"></div>
        <div class="state-text">正在查询数据...</div>
      </div>
      <div v-if="!loading && queried && list.length === 0" class="state-block">
        <div class="state-icon">📭</div>
        <div class="state-text">没有符合条件的数据</div>
      </div>

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

      <div v-if="total > 0" class="pagination">
        <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">‹ 上一页</button>
        <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
        <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">下一页 ›</button>
      </div>

      <div class="daily-report-trigger">
        <button class="btn-action btn-report" @click="reportVisible = true">
          📊 查看日报统计
        </button>
      </div>
    </div>

    <div class="import-section">
      <div class="import-toggle" @click="importExpanded = !importExpanded">
        <span class="import-toggle-icon">{{ importExpanded ? '▼' : '▶' }}</span>
        <span class="import-toggle-icon-emoji">📥</span>
        <span class="import-toggle-text">一键导入数据（从WPS表格粘贴）</span>
      </div>
      <div v-if="importExpanded" class="import-body">
        <div class="import-help">
          <p>将 WPS 表格中的数据直接粘贴到下方输入框，系统会自动按 Tab 分隔解析为多条记录。</p>
          <p>数据格式（Tab分隔）：<code>URL · bundleId · 归因 · 事件数 · 异常类型 · 日期 · 记录人 · 已冻结(可选)</code></p>
          <p>导入的数据将自动设置 <code>isOutput = 1</code>（已导出），并自动生成唯一 hash。</p>
        </div>
        <textarea
            v-model="importRawText"
            class="import-textarea"
            placeholder="在此粘贴WPS表格数据...&#10;例如：&#10;https://apps.apple.com/...	com.example	appflyer	35	正常	2026/8/27	张三&#10;https://apps.apple.com/...	com.example2		5	超过10分钟0上报	2026/8/27	李四	已冻结"
            rows="8"
        ></textarea>
        <div class="import-actions">
          <div class="import-hint" v-if="importRawText.trim()">
            已检测到 <strong>{{ importRawText.split('\n').filter(l => l.trim()).length }}</strong> 行数据
          </div>
          <div class="import-btns">
            <button class="btn-action btn-import-clear" @click="importRawText = ''; importMsg = ''; importResult = null" v-if="importRawText">
              清空
            </button>
            <button class="btn-action btn-import" @click="handleBatchImport" :disabled="importing || !importRawText.trim()">
              {{ importing ? '导入中...' : '📥 一键导入' }}
            </button>
          </div>
        </div>
        <div v-if="importMsg" class="feedback" :class="{ 'feedback-ok': importMsg.startsWith('✅'), 'feedback-err': importMsg.startsWith('❌') }">
          {{ importMsg }}
        </div>
        <div v-if="importResult" class="import-result">
          <div class="import-result-row">
            <span class="import-result-label">总行数</span>
            <span class="import-result-value">{{ importResult.totalLines }}</span>
          </div>
          <div class="import-result-row">
            <span class="import-result-label">成功解析</span>
            <span class="import-result-value import-result-ok">{{ importResult.parsedCount }} 条</span>
          </div>
          <div class="import-result-row">
            <span class="import-result-label">入库成功</span>
            <span class="import-result-value import-result-ok">{{ importResult.successCount }} 条</span>
          </div>
          <div v-if="importResult.errorCount > 0" class="import-result-row">
            <span class="import-result-label">解析失败</span>
            <span class="import-result-value import-result-bad">{{ importResult.errorCount }} 条</span>
          </div>
          <div v-if="importResult.errors && importResult.errors.length > 0" class="import-errors">
            <div class="import-error-title">失败详情：</div>
            <div v-for="(err, idx) in importResult.errors" :key="idx" class="import-error-item">{{ err }}</div>
          </div>
        </div>
      </div>
    </div>

    <div @click="sup" class="sup-area">点我试试</div>

    <!-- ==================== 日报统计弹窗 ==================== -->
    <div class="report-overlay" v-if="reportVisible" @click.self="closeReport">
      <div class="report-modal">
        <div class="report-modal-header">
          <div class="report-modal-title">
            <span class="report-modal-icon">📊</span>
            <h3>日报统计</h3>
          </div>
          <button class="report-close-btn" @click="closeReport">✕</button>
        </div>
        <div class="report-modal-body">
          <div class="report-date-picker">
            <label class="report-date-label">选择日期</label>
            <div class="report-date-row">
              <input v-model="reportDate" type="date" class="form-input report-date-input" />
              <button class="btn-action btn-query" @click="queryDailyReport" :disabled="reportLoading || !reportDate">
                {{ reportLoading ? '查询中...' : '🔍 查询' }}
              </button>
            </div>
          </div>

          <div v-if="reportData" class="report-result">
            <div class="report-summary">
              <div class="report-stat">
                <div class="stat-number">{{ reportData.totalCount }}</div>
                <div class="stat-label">总记录数</div>
              </div>
              <div class="report-stat stat-qualified">
                <div class="stat-number">{{ reportData.qualifiedCount }}</div>
                <div class="stat-label">合格数</div>
              </div>
              <div class="report-stat stat-unqualified">
                <div class="stat-number">{{ reportData.unqualifiedCount }}</div>
                <div class="stat-label">不合格数</div>
              </div>
              <div class="report-stat stat-rate">
                <div class="stat-number">{{ reportData.qualifyRate }}%</div>
                <div class="stat-label">合格率</div>
              </div>
            </div>

            <div class="report-attr-section">
              <div class="attr-section-title">归因占比（基于合格数据 {{ reportData.qualifiedCount }} 条）</div>
              <div class="attr-bar-list">
                <div class="attr-bar-item" v-for="type in ['appflyer', 'adjust', 'singular', 'tenjin']" :key="type">
                  <div class="attr-bar-header">
                    <span class="attr-bar-name" :class="type">{{ type }}</span>
                    <span class="attr-bar-count">{{ reportData.attributions[type] }} 条</span>
                    <span class="attr-bar-percent">{{ getAttrPercent(reportData.attributions[type]) }}%</span>
                  </div>
                  <div class="attr-bar-track">
                    <div class="attr-bar-fill" :class="type" :style="{ width: (reportData.qualifiedCount > 0 ? reportData.attributions[type] * 100 / reportData.qualifiedCount : 0) + '%' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="!reportData && !reportLoading" class="report-placeholder">
            <div class="placeholder-icon">📈</div>
            <div class="placeholder-text">选择日期后点击查询，查看当日数据统计</div>
          </div>
          <div v-if="reportLoading" class="report-loading">
            <div class="state-spinner"></div>
            <div class="state-text">正在查询统计数据...</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 用户管理 Tab ==================== -->
    <div v-if="activeTab === 'users'" class="admin-section">

      <div class="user-top-grid">
        <!-- 创建新用户 -->
        <div class="card">
          <div class="card-header">
            <span class="card-icon">➕</span>
            <h3>创建新用户</h3>
          </div>
          <div class="card-body">
            <div class="form-row">
              <div class="form-group">
                <label>姓名</label>
                <input v-model="createForm.name" placeholder="用户姓名" class="form-input" />
              </div>
              <div class="form-group">
                <label>密码</label>
                <input v-model="createForm.pwd" type="password" placeholder="登录密码" class="form-input" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group form-group-stretch">
                <label>账户类型</label>
                <select v-model="createForm.type" class="form-input">
                  <option value="USER">普通用户 (USER)</option>
                  <option value="ADMIN">管理员 (ADMIN)</option>
                </select>
              </div>
            </div>
            <button class="btn-action btn-create" @click="handleCreateUser" :disabled="creating">
              {{ creating ? '创建中...' : '创建用户' }}
            </button>
            <div v-if="createMsg" class="feedback" :class="{ 'feedback-ok': createMsg.startsWith('✅'), 'feedback-err': createMsg.startsWith('❌') }">
              {{ createMsg }}
            </div>
          </div>
        </div>

        <!-- 重置密码 -->
        <div class="card">
          <div class="card-header">
            <span class="card-icon">🔑</span>
            <h3>重置账户密码</h3>
          </div>
          <div class="card-body">
            <div class="form-row">
              <div class="form-group">
                <label>用户 UID</label>
                <input v-model="resetPwdUid" placeholder="输入UID" class="form-input" />
              </div>
              <div class="form-group">
                <label>新密码</label>
                <input v-model="resetPwdNew" type="password" placeholder="输入新密码" class="form-input" />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group form-group-stretch">
                <label>&nbsp;</label>
                <button class="btn-action btn-reset" @click="handleResetPassword" :disabled="resetting">
                  {{ resetting ? '重置中...' : '重置密码并踢下线' }}
                </button>
              </div>
            </div>
            <div v-if="resetMsg" class="feedback" :class="{ 'feedback-ok': resetMsg.startsWith('✅'), 'feedback-err': resetMsg.startsWith('❌') }">
              {{ resetMsg }}
            </div>
          </div>
        </div>
      </div>

      <!-- 用户列表 -->
      <div class="card">
        <div class="card-header card-header-between">
          <div class="card-header-left">
            <span class="card-icon">📋</span>
            <h3>用户列表</h3>
            <span class="user-count" v-if="userList.length > 0">{{ userList.length }} 个账户</span>
          </div>
          <div class="card-header-right">
            <span class="kick-label">踢下线封禁</span>
            <div class="kick-input-wrap">
              <input v-model.number="kickBanSeconds" type="number" min="0" class="kick-input" />
              <span class="kick-unit">秒</span>
            </div>
          </div>
        </div>
        <div class="card-body">
          <div v-if="userLoading && userList.length === 0" class="state-block">
            <div class="state-spinner"></div>
            <div class="state-text">加载中...</div>
          </div>
          <div v-if="!userLoading && userList.length === 0" class="state-block">
            <div class="state-icon">👤</div>
            <div class="state-text">暂无用户</div>
          </div>
          <div v-if="userList.length > 0" class="table-wrapper table-wrapper-user">
            <table class="data-table user-table">
              <thead>
              <tr>
                <th>UID</th>
                <th>姓名</th>
                <th>类型</th>
                <th>在线状态</th>
                <th>操作</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="u in userList" :key="u.uid" :class="{ 'row-online': u.online }">
                <td class="uid-cell">{{ u.uid }}</td>
                <td class="name-cell">{{ u.name }}</td>
                <td>
                  <span class="role-tag" :class="u.type === 'ADMIN' ? 'role-admin' : u.type === 'DEVELOPER' ? 'role-developer' : 'role-user'">
                    {{ u.type === 'ADMIN' ? '管理员' : u.type === 'DEVELOPER' ? 'root账户' : '用户' }}
                  </span>
                </td>
                <td>
                  <span class="online-tag" :class="u.online ? 'online-yes' : 'online-no'">
                    <span class="online-dot"></span>
                    {{ u.online ? '在线' : '离线' }}
                  </span>
                </td>
                <td class="action-cell">
                  <button class="btn-sm btn-kick" @click="handleKickUser(u.uid)" :disabled="!u.online" :title="u.online ? '踢下线' : '用户不在线'">
                    ⚡ 踢下线
                  </button>
                  <button class="btn-sm btn-del" @click="handleDeleteUser(u.uid)" title="删除用户">
                    🗑 删除
                  </button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>/* ========== 页面骨架 ========== */
.admin-page { width: 100%; max-width: 1600px; margin: 0 auto; padding: 0 20px 40px; box-sizing: border-box; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.page-header h2 { margin: 0; font-size: 22px; color: #1a1a2e; font-weight: 700; }
.page-header-sub { font-size: 13px; color: #999; }

/* ========== Tab 栏 ========== */
.admin-tabs { display: flex; gap: 4px; background: #f4f5f7; border-radius: 12px; padding: 4px; margin-bottom: 24px; }
.admin-tab { flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px; padding: 10px 0; font-size: 14px; border: none; border-radius: 10px; cursor: pointer; background: transparent; color: #888; font-weight: 600; transition: all 0.25s ease; }
.admin-tab.active { background: #fff; color: #1a1a2e; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.admin-tab:not(.active):hover { color: #555; background: rgba(255,255,255,0.5); }
.tab-icon { font-size: 16px; }

.admin-section { animation: fadeUp 0.3s ease; }
@keyframes fadeUp { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }

/* ========== 通用卡片 ========== */
.card { background: #fff; border-radius: 14px; padding: 0; margin-bottom: 20px; border: 1px solid #eaeaea; box-shadow: 0 1px 4px rgba(0,0,0,0.04); overflow: hidden; }
.card-header { display: flex; align-items: center; gap: 8px; padding: 16px 20px; border-bottom: 1px solid #f0f0f0; }
.card-header h3 { margin: 0; font-size: 15px; color: #333; font-weight: 700; }
.card-header-between { justify-content: space-between; }
.card-header-left { display: flex; align-items: center; gap: 8px; }
.card-header-right { display: flex; align-items: center; gap: 8px; }
.card-icon { font-size: 18px; }
.card-body { padding: 20px; }

/* ========== 日报统计 ========== */
.daily-report-trigger {
  margin-top: 20px;
  text-align: center;
}
.btn-report {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  padding: 12px 32px;
  font-size: 15px;
  border-radius: 12px;
}
.btn-report:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(99,102,241,0.35);
}

/* ... existing code ... */
.btn-export-hash:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(67,233,123,0.35); }
.btn-export-all { background: linear-gradient(135deg, #f59e0b, #f97316); color: #fff; }
.btn-export-all:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(245,158,11,0.35); }
.unexported-badge { display: flex; align-items: center; gap: 8px; background: #fef3c7; padding: 6px 16px; border-radius: 20px; border: 1px solid #fde68a; font-size: 13px; color: #92400e; font-weight: 600; }
.unexported-badge.unexported-zero { background: #dcfce7; border-color: #bbf7d0; color: #166534; }
.unexported-badge-dot { width: 8px; height: 8px; border-radius: 50%; background: #f59e0b; animation: pulse-dot 2s ease-in-out infinite; }
.unexported-zero .unexported-badge-dot { background: #22c55e; }
.unexported-badge strong { font-weight: 800; font-size: 15px; }
.download-section-inline { display: flex; align-items: center; gap: 10px; background: #e8f5e9; padding: 8px 16px; border-radius: 10px; border-left: 3px solid #43a047; }
/* ... existing code ... */

.report-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  animation: reportFadeIn 0.2s ease;
}
@keyframes reportFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
/* ... existing code ... */
.role-tag { padding: 3px 14px; border-radius: 20px; font-size: 11px; font-weight: 700; display: inline-block; }
.role-admin { background: #fef3c7; color: #92400e; }
.role-developer { background: #dcfce7; color: #166534; }
.role-user { background: #e0e7ff; color: #3730a3; }
/* ... existing code ... */
.report-modal {
  background: #fff;
  border-radius: 20px;
  max-width: 640px;
  width: 92%;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 24px 80px rgba(0,0,0,0.2);
  animation: reportSlideUp 0.3s ease;
}
@keyframes reportSlideUp {
  from { transform: translateY(24px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
.report-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 28px 16px;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  background: #fff;
  border-radius: 20px 20px 0 0;
  z-index: 1;
}
.report-modal-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.report-modal-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
}
.report-modal-icon {
  font-size: 22px;
}
.report-close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.2s;
}
.report-close-btn:hover {
  color: #333;
  background: #f0f0f0;
}
.report-modal-body {
  padding: 20px 28px 28px;
}
.report-date-picker {
  margin-bottom: 20px;
}
.report-date-label {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 8px;
}
.report-date-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.report-date-input {
  flex: 1;
  cursor: pointer;
  color-scheme: light;
}
.report-date-input::-webkit-calendar-picker-indicator {
  cursor: pointer;
  opacity: 0.6;
  transition: opacity 0.2s;
}
.report-date-input::-webkit-calendar-picker-indicator:hover {
  opacity: 1;
}

.report-result {
  animation: fadeUp 0.3s ease;
}
.report-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
@media (max-width: 600px) {
  .report-summary {
    grid-template-columns: repeat(2, 1fr);
  }
}
.report-stat {
  background: #f8f9fc;
  border-radius: 14px;
  padding: 16px;
  text-align: center;
  border: 1px solid #e8eaef;
}
.report-stat .stat-number {
  font-size: 24px;
  font-weight: 800;
  color: #1a1a2e;
  margin-bottom: 4px;
}
.report-stat .stat-label {
  font-size: 11px;
  font-weight: 600;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}
.stat-qualified {
  border-color: #bbf7d0;
  background: #f0fdf4;
}
.stat-qualified .stat-number { color: #166534; }
.stat-unqualified {
  border-color: #fecaca;
  background: #fef2f2;
}
.stat-unqualified .stat-number { color: #991b1b; }
.stat-rate {
  border-color: #c7d2fe;
  background: #eef2ff;
}
.stat-rate .stat-number { color: #4338ca; }

.report-attr-section {
  margin-top: 8px;
}
.attr-section-title {
  font-size: 13px;
  font-weight: 700;
  color: #555;
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}
.attr-bar-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.attr-bar-item {
  padding: 0;
}
.attr-bar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.attr-bar-name {
  font-size: 12px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 20px;
  color: #fff;
  text-transform: uppercase;
}
.attr-bar-name.appflyer { background: #22c55e; }
.attr-bar-name.adjust { background: #f59e0b; }
.attr-bar-name.singular { background: #3b82f6; }
.attr-bar-name.tenjin { background: #a855f7; }
.attr-bar-count {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}
.attr-bar-percent {
  margin-left: auto;
  font-size: 13px;
  font-weight: 700;
  color: #666;
}
.attr-bar-track {
  height: 10px;
  background: #f0f0f0;
  border-radius: 5px;
  overflow: hidden;
}
.attr-bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
  min-width: 0;
}
.attr-bar-fill.appflyer { background: linear-gradient(90deg, #22c55e, #4ade80); }
.attr-bar-fill.adjust { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.attr-bar-fill.singular { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
.attr-bar-fill.tenjin { background: linear-gradient(90deg, #a855f7, #c084fc); }

.report-placeholder {
  text-align: center;
  padding: 48px 20px;
}
.placeholder-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.placeholder-text {
  font-size: 14px;
  color: #999;
}
.report-loading {
  text-align: center;
  padding: 40px 20px;
}

.export-action-bar { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; background: #f8f9fc; border-radius: 12px; margin-bottom: 14px; border: 1px solid #eaeaea; flex-wrap: wrap; gap: 10px; }
.export-action-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.export-action-right { display: flex; align-items: center; gap: 10px; }
.export-tip { font-size: 12px; color: #e65100; font-weight: 600; background: #fff3e0; padding: 4px 12px; border-radius: 8px; }
.btn-export-date { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.btn-export-date:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(102,126,234,0.35); }
.btn-export-hash { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #fff; }
.btn-export-hash:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(67,233,123,0.35); }
.btn-download-sm { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #fff; padding: 8px 20px; font-size: 12px; }
.btn-download-sm:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(67,233,123,0.35); }
.download-section-inline { display: flex; align-items: center; gap: 10px; background: #e8f5e9; padding: 8px 16px; border-radius: 10px; border-left: 3px solid #43a047; }
.download-file-name { font-size: 13px; font-weight: 600; color: #2e7d32; }
.polling-tip { display: flex; align-items: center; font-size: 13px; color: #667eea; font-weight: 600; margin-bottom: 12px; padding: 10px 16px; background: #eef2ff; border-radius: 10px; animation: blink 1.2s infinite; }
@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

/* ========== 一键导入 ========== */
.import-section { margin-top: 24px; background: #fff; border: 1px solid #e8eaef; border-radius: 12px; overflow: hidden; }
.import-toggle { display: flex; align-items: center; gap: 8px; padding: 14px 20px; cursor: pointer; user-select: none; transition: background 0.2s; }
.import-toggle:hover { background: #f8f9fc; }
.import-toggle-icon { font-size: 12px; color: #94a3b8; }
.import-toggle-icon-emoji { font-size: 18px; }
.import-toggle-text { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.import-body { padding: 0 20px 20px; border-top: 1px solid #f0f0f0; }
.import-help { background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 10px; padding: 14px 16px; margin-top: 16px; }
.import-help p { margin: 0 0 6px 0; font-size: 13px; color: #374151; line-height: 1.7; }
.import-help p:last-child { margin-bottom: 0; }
.import-help code { background: #dcfce7; padding: 1px 6px; border-radius: 4px; font-size: 12px; color: #166534; }
.import-textarea { width: 100%; box-sizing: border-box; margin-top: 14px; padding: 12px 14px; border: 2px solid #e8eaef; border-radius: 10px; font-family: 'Cascadia Code', 'Fira Code', monospace; font-size: 12.5px; line-height: 1.6; resize: vertical; transition: border-color 0.2s; color: #1a1a2e; }
.import-textarea:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }
.import-textarea::placeholder { color: #c0c4cc; }
.import-actions { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.import-hint { font-size: 13px; color: #64748b; }
.import-hint strong { color: #6366f1; font-size: 16px; }
.import-btns { display: flex; gap: 8px; }
.btn-import { background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; border: none; padding: 10px 24px; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-import:hover:not(:disabled) { box-shadow: 0 4px 16px rgba(99,102,241,0.35); transform: translateY(-1px); }
.btn-import:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-import-clear { background: #f1f5f9; color: #64748b; border: 1px solid #e2e8f0; padding: 10px 18px; border-radius: 10px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.btn-import-clear:hover { background: #e2e8f0; }
.import-result { margin-top: 14px; background: #f8f9fc; border: 1px solid #e8eaef; border-radius: 10px; padding: 14px 16px; }
.import-result-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px dashed #e8eaef; }
.import-result-row:last-child { border-bottom: none; }
.import-result-label { font-size: 13px; color: #64748b; }
.import-result-value { font-size: 14px; font-weight: 600; color: #1a1a2e; }
.import-result-ok { color: #16a34a; }
.import-result-bad { color: #dc2626; }
.import-errors { margin-top: 10px; padding: 10px 12px; background: #fef2f2; border: 1px solid #fecaca; border-radius: 8px; }
.import-error-title { font-size: 12px; font-weight: 600; color: #dc2626; margin-bottom: 6px; }
.import-error-item { font-size: 12px; color: #991b1b; line-height: 1.6; padding: 2px 0; }

/* ... existing code ... */

/* ========== 筛选区 ========== */
.filter-card { background: #fafbfc; border-radius: 14px; padding: 20px; margin-bottom: 16px; border: 1px solid #eaeaea; }
.filter-section { margin-bottom: 14px; }
.filter-section:last-child { margin-bottom: 0; }
.filter-section-title { font-size: 12px; font-weight: 700; color: #999; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; }
.filter-search-row { display: flex; align-items: flex-end; gap: 14px; flex-wrap: wrap; }
.filter-field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 11px; font-weight: 700; color: #999; text-transform: uppercase; letter-spacing: 0.3px; }
.filter-input { padding: 8px 14px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 10px; outline: none; width: 180px; transition: border-color 0.2s, box-shadow 0.2s; background: #fff; }
.filter-input:focus { border-color: #667eea; box-shadow: 0 0 0 3px rgba(102,126,234,0.12); }
/* ... existing code ... */
.filter-input { padding: 8px 14px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 10px; outline: none; width: 180px; transition: border-color 0.2s, box-shadow 0.2s; background: #fff; }
.filter-input:focus { border-color: #667eea; box-shadow: 0 0 0 3px rgba(102,126,234,0.12); }
.filter-date { cursor: pointer; font-family: inherit; color-scheme: light; }
.filter-date::-webkit-calendar-picker-indicator { cursor: pointer; opacity: 0.6; transition: opacity 0.2s; }
.filter-date::-webkit-calendar-picker-indicator:hover { opacity: 1; }
/* ... existing code ... */
.radio-group { display: flex; gap: 8px; flex-wrap: wrap; }
.radio-tag { display: flex; align-items: center; gap: 4px; padding: 6px 16px; border-radius: 20px; font-size: 12px; font-weight: 600; cursor: pointer; color: #fff; transition: all 0.2s; opacity: 0.35; border: 2px solid transparent; }
.radio-tag input { display: none; }
.radio-tag.active { opacity: 1; box-shadow: 0 2px 10px rgba(0,0,0,0.18); }
.radio-tag.radio-all { background: #607D8B; }
.radio-tag.appflyer { background: #4CAF50; }
.radio-tag.adjust { background: #FF9800; }
.radio-tag.singular { background: #2196F3; }
.radio-tag.tenjin { background: #9C27B0; }

.sup-area { color: white; font-size: 10px; background-size: 15px; width: 20px; height: 15px; }

.checkbox-tag { background: #eef2ff; color: #4f46e5; padding: 7px 16px; border-radius: 10px; font-size: 13px; opacity: 0.45; transition: all 0.2s; border: 2px solid transparent; display: inline-flex; align-items: center; gap: 6px; cursor: pointer; font-weight: 500; }
.checkbox-tag.active { opacity: 1; border-color: #4f46e5; background: #e0e7ff; }
.checkbox-tag input { margin-right: 4px; }

.btn-query { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; padding: 8px 24px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600; transition: transform 0.15s, box-shadow 0.15s; white-space: nowrap; }
.btn-query:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(102,126,234,0.4); }
.btn-query:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

/* ========== 视图标签 ========== */
.view-type-bar { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #555; margin-bottom: 14px; padding: 10px 16px; background: #f0f4ff; border-radius: 10px; border-left: 3px solid #667eea; }
.view-type-label { font-size: 11px; color: #999; font-weight: 600; text-transform: uppercase; }
.view-type-count { margin-left: auto; font-size: 12px; color: #888; }

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

/* ========== 分页 ========== */
.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 14px 0; }
.page-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; padding: 8px 22px; border-radius: 10px; cursor: pointer; font-weight: 600; font-size: 13px; transition: transform 0.15s; }
.page-btn:hover { transform: translateY(-1px); }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }
.page-info { font-size: 13px; color: #666; font-weight: 600; }

/* ========== 用户管理 - 顶部双栏 ========== */
.user-top-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 0; }
@media (max-width: 900px) { .user-top-grid { grid-template-columns: 1fr; } }

/* ========== 表单 ========== */
.form-row { display: flex; gap: 12px; margin-bottom: 12px; }
.form-group { flex: 1; min-width: 0; }
.form-group-stretch { flex: 1 1 100%; }
.form-group label { display: block; font-weight: 600; color: #666; font-size: 11px; margin-bottom: 5px; text-transform: uppercase; letter-spacing: 0.3px; }
.form-input { width: 100%; padding: 9px 14px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 10px; outline: none; transition: border-color 0.2s, box-shadow 0.2s; box-sizing: border-box; background: #fff; }
.form-input:focus { border-color: #667eea; box-shadow: 0 0 0 3px rgba(102,126,234,0.1); }
select.form-input { cursor: pointer; }

/* ========== 按钮 ========== */
.btn-action { border: none; padding: 10px 24px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600; transition: transform 0.15s, box-shadow 0.15s; }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }
.btn-create { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #fff; }
.btn-create:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(67,233,123,0.35); }
.btn-reset { background: linear-gradient(135deg, #f093fb, #f5576c); color: #fff; width: 100%; }
.btn-reset:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(245,87,108,0.35); }
.btn-refresh { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 6px 18px; font-size: 12px; }
.btn-refresh:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(102,126,234,0.35); }

.feedback { font-size: 13px; font-weight: 600; padding: 10px 14px; border-radius: 10px; margin-top: 4px; }
.feedback-ok { background: #dcfce7; color: #166534; }
.feedback-err { background: #fef2f2; color: #991b1b; }

/* ========== 用户列表表格 ========== */
.user-count { font-size: 12px; color: #999; font-weight: 500; background: #f4f5f7; padding: 2px 10px; border-radius: 20px; }
.kick-label { font-size: 12px; color: #888; font-weight: 500; }
.kick-input-wrap { display: flex; align-items: center; gap: 4px; }
.kick-input { width: 72px; padding: 5px 8px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 8px; outline: none; text-align: center; transition: border-color 0.2s; }
.kick-input:focus { border-color: #667eea; }
.kick-unit { font-size: 12px; color: #888; }

.table-wrapper-user { min-width: 0; }
.user-table { min-width: 600px; }
.uid-cell { font-family: 'SF Mono', 'Fira Code', monospace; font-weight: 600; color: #667eea; letter-spacing: 0.5px; font-size: 12px; }
.name-cell { font-weight: 600; color: #333; }
.row-online td { background: rgba(34,197,94,0.03); }

.role-tag { padding: 3px 14px; border-radius: 20px; font-size: 11px; font-weight: 700; display: inline-block; }
.role-admin { background: #fef3c7; color: #92400e; }
.role-user { background: #e0e7ff; color: #3730a3; }

.online-tag { display: inline-flex; align-items: center; gap: 6px; padding: 4px 14px; border-radius: 20px; font-size: 12px; font-weight: 700; }
.online-yes { background: #dcfce7; color: #166534; }
.online-no { background: #f4f5f7; color: #999; }
.online-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.online-yes .online-dot { background: #22c55e; box-shadow: 0 0 6px rgba(34,197,94,0.5); animation: pulse-dot 2s ease-in-out infinite; }
.online-no .online-dot { background: #ccc; }
@keyframes pulse-dot { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }

.action-cell { display: flex; gap: 8px; align-items: center; }
.btn-sm { border: none; padding: 6px 14px; font-size: 12px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: transform 0.15s, box-shadow 0.15s; white-space: nowrap; }
.btn-sm:hover:not(:disabled) { transform: translateY(-1px); }
.btn-sm:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }
.btn-kick { background: linear-gradient(135deg, #fbbf24, #f59e0b); color: #fff; }
.btn-kick:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(245,158,11,0.4); }
.btn-del { background: linear-gradient(135deg, #f87171, #ef4444); color: #fff; }
.btn-del:hover:not(:disabled) { box-shadow: 0 4px 12px rgba(239,68,68,0.4); }

/* ========== 系统状态 ========== */
.sys-body { }
.sys-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
@media (max-width: 900px) { .sys-grid { grid-template-columns: 1fr 1fr; } }
.sys-item { display: flex; flex-direction: column; gap: 4px; padding: 12px 16px; background: #f8f9fc; border-radius: 10px; }
.sys-label { font-size: 11px; color: #999; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
.sys-val { font-size: 14px; color: #333; font-weight: 600; }
.val-warn { color: #ef4444; }

.meter { margin-bottom: 12px; }
.meter-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.meter-label { font-size: 12px; color: #666; font-weight: 600; }
.meter-value { font-size: 13px; font-weight: 700; color: #333; }
.meter-track { height: 10px; background: #f0f0f0; border-radius: 5px; overflow: hidden; }
.meter-fill { height: 100%; background: linear-gradient(90deg, #43e97b, #38f9d7); border-radius: 5px; transition: width 0.5s ease; }
.meter-fill.meter-danger { background: linear-gradient(90deg, #f87171, #ef4444); }

/* ========== 统计卡片 ========== */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
@media (max-width: 900px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }
.stat-card { display: flex; align-items: center; gap: 14px; background: #fff; border: 1px solid #eaeaea; border-radius: 14px; padding: 18px 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); transition: transform 0.2s, box-shadow 0.2s; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.08); }
.stat-card-icon { font-size: 28px; flex-shrink: 0; }
.stat-card-value { font-size: 26px; font-weight: 800; color: #1a1a2e; line-height: 1.2; }
.stat-card-label { font-size: 11px; font-weight: 600; color: #999; text-transform: uppercase; letter-spacing: 0.3px; margin-top: 2px; }
.stat-card-exported { border-left: 3px solid #22c55e; }
.stat-card-pending { border-left: 3px solid #f59e0b; }
.stat-card-frozen { border-left: 3px solid #3b82f6; }

/* ========== 高级搜索 ========== */
.filter-top-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
.filter-top-actions { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.btn-link { background: none; border: none; color: #667eea; font-size: 13px; font-weight: 600; cursor: pointer; padding: 4px 0; transition: color 0.2s; }
.btn-link:hover { color: #764ba2; }

.filter-advanced { margin-top: 16px; padding-top: 16px; border-top: 1px solid #f0f0f0; animation: fadeUp 0.25s ease; }
.filter-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
@media (max-width: 1100px) { .filter-grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 700px) { .filter-grid { grid-template-columns: repeat(2, 1fr); } }
.filter-field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 11px; font-weight: 700; color: #999; text-transform: uppercase; letter-spacing: 0.3px; }
.filter-date { cursor: pointer; font-family: inherit; color-scheme: light; }
.filter-date::-webkit-calendar-picker-indicator { cursor: pointer; opacity: 0.6; }
.filter-date::-webkit-calendar-picker-indicator:hover { opacity: 1; }
select.filter-input { cursor: pointer; appearance: auto; }

.filter-action-row { display: flex; align-items: center; gap: 12px; }
.btn-reset { background: #f4f5f7; color: #666; border: 1px solid #e0e0e0; padding: 8px 20px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600; transition: all 0.2s; }
.btn-reset:hover { background: #e8e8e8; color: #333; }
.filter-tip { font-size: 11px; color: #bbb; margin-left: auto; }

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

/* ========== 表格增强 ========== */
.th-check { width: 40px; text-align: center; }
.td-check { text-align: center; }
.td-check input { cursor: pointer; width: 16px; height: 16px; accent-color: #667eea; }
.row-selected td { background: #eef2ff !important; }

.exception-tag { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; background: #fef3c7; color: #92400e; }
.exception-tag.ex-正常 { background: #dcfce7; color: #166534; }
.exception-tag.ex-验证已解决 { background: #dcfce7; color: #166534; }
.exception-tag.ex-测试 { background: #e0e7ff; color: #3730a3; }
.exception-tag.ex-iOS16闪退,
.exception-tag.ex-iOS13\/14\/16均闪退 { background: #fef2f2; color: #991b1b; }
.exception-tag.ex-禁止入库 { background: #fef2f2; color: #991b1b; }


/* ========== 质量分析面板 ========== */
.quality-panel { background: #fff; border: 1px solid #eaeaea; border-radius: 14px; padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.quality-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }
.quality-title { font-size: 15px; font-weight: 700; color: #1a1a2e; }
.quality-scope { font-size: 11px; font-weight: 600; color: #667eea; background: #eef2ff; padding: 3px 12px; border-radius: 20px; }

.quality-summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
@media (max-width: 700px) { .quality-summary { grid-template-columns: repeat(2, 1fr); } }
.quality-stat { background: #f8f9fc; border-radius: 12px; padding: 14px 16px; text-align: center; border: 1px solid #e8eaef; }
.quality-stat-value { font-size: 22px; font-weight: 800; color: #1a1a2e; margin-bottom: 2px; }
.quality-stat-label { font-size: 11px; font-weight: 600; color: #999; text-transform: uppercase; letter-spacing: 0.3px; }
.quality-stat-ok { border-color: #bbf7d0; background: #f0fdf4; }
.quality-stat-ok .quality-stat-value { color: #166534; }
.quality-stat-bad { border-color: #fecaca; background: #fef2f2; }
.quality-stat-bad .quality-stat-value { color: #991b1b; }
.quality-stat-rate { border-color: #c7d2fe; background: #eef2ff; }
.quality-stat-rate .quality-stat-value { color: #4338ca; }

.quality-attr-section { padding-top: 4px; }
.quality-attr-title { font-size: 13px; font-weight: 700; color: #555; margin-bottom: 14px; padding-bottom: 8px; border-bottom: 1px solid #f0f0f0; }
.quality-attr-bars { display: flex; flex-direction: column; gap: 12px; }
.quality-attr-bar-item { }
.quality-attr-bar-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.quality-attr-bar-name { font-size: 12px; font-weight: 700; padding: 2px 10px; border-radius: 20px; color: #fff; text-transform: uppercase; }
.quality-attr-bar-name.appflyer { background: #22c55e; }
.quality-attr-bar-name.adjust { background: #f59e0b; }
.quality-attr-bar-name.singular { background: #3b82f6; }
.quality-attr-bar-name.tenjin { background: #a855f7; }
.quality-attr-bar-count { font-size: 13px; font-weight: 600; color: #333; }
.quality-attr-bar-percent { margin-left: auto; font-size: 13px; font-weight: 700; color: #666; }
.quality-attr-bar-track { height: 10px; background: #f0f0f0; border-radius: 5px; overflow: hidden; }
.quality-attr-bar-fill { height: 100%; border-radius: 5px; transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1); min-width: 0; }
.quality-attr-bar-fill.appflyer { background: linear-gradient(90deg, #22c55e, #4ade80); }
.quality-attr-bar-fill.adjust { background: linear-gradient(90deg, #f59e0b, #fbbf24); }
.quality-attr-bar-fill.singular { background: linear-gradient(90deg, #3b82f6, #60a5fa); }
.quality-attr-bar-fill.tenjin { background: linear-gradient(90deg, #a855f7, #c084fc); }
</style>
