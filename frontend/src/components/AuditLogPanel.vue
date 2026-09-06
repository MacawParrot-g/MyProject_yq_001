<!-- 文件路径: src/main/frontend/src/components/AuditLogPanel.vue -->
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { searchAuditLogs, cleanAuditLogs } from '../api/index.js'

const emit = defineEmits(['error'])

const list = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(15)
const pageSizeInput = ref(15)
const queried = ref(false)

const filters = reactive({
  operator: '',
  action: '',
  dateFrom: '',
  dateTo: ''
})

const cleaning = ref(false)
const cleanDays = ref(90)

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))

async function fetchData(resetPage = false) {
  if (resetPage) currentPage.value = 1
  loading.value = true
  queried.value = true
  try {
    const json = await searchAuditLogs({
      operator: filters.operator.trim() || null,
      action: filters.action.trim() || null,
      dateFrom: filters.dateFrom || null,
      dateTo: filters.dateTo || null,
      page: currentPage.value,
      size: pageSize.value
    })
    if (json.success) {
      list.value = json.data || []
      total.value = json.total || 0
    } else {
      emit('error', json.message || '查询失败')
      list.value = []
    }
  } catch (e) {
    emit('error', '查询请求失败：' + e.message)
    list.value = []
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.operator = ''
  filters.action = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  currentPage.value = 1
  fetchData(true)
}

function prevPage() { if (currentPage.value > 1) { currentPage.value--; fetchData() } }
function nextPage() { if (currentPage.value < totalPages()) { currentPage.value++; fetchData() } }

function applyPageSize() {
  const v = parseInt(pageSizeInput.value)
  if (v > 0) { pageSize.value = v; currentPage.value = 1; fetchData(true) }
}

async function handleClean() {
  if (!confirm(`确定要清理 ${cleanDays.value} 天前的审计日志吗？此操作不可撤销！`)) return
  cleaning.value = true
  try {
    const json = await cleanAuditLogs(cleanDays.value)
    if (json.success) {
      alert('✅ ' + json.message)
      await fetchData()
    } else {
      emit('error', json.message || '清理失败')
    }
  } catch (e) {
    emit('error', '清理请求失败：' + e.message)
  } finally {
    cleaning.value = false
  }
}

function formatDuration(ms) {
  if (ms == null) return '-'
  if (ms < 1000) return ms + 'ms'
  return (ms / 1000).toFixed(2) + 's'
}

onMounted(() => { fetchData() })
</script>

<template>
  <div class="audit-page">
    <div class="page-header">
      <h2>📋 操作审计日志</h2>
      <div class="page-header-sub">追踪所有用户操作记录</div>
    </div>

    <div class="filter-card">
      <div class="filter-grid">
        <div class="filter-field">
          <label class="field-label">操作人</label>
          <input v-model="filters.operator" class="filter-input" placeholder="用户名搜索" @keyup.enter="fetchData(true)" />
        </div>
        <div class="filter-field">
          <label class="field-label">操作描述</label>
          <input v-model="filters.action" class="filter-input" placeholder="操作名称搜索" @keyup.enter="fetchData(true)" />
        </div>
        <div class="filter-field">
          <label class="field-label">开始日期</label>
          <input v-model="filters.dateFrom" type="date" class="filter-input filter-date" />
        </div>
        <div class="filter-field">
          <label class="field-label">结束日期</label>
          <input v-model="filters.dateTo" type="date" class="filter-input filter-date" />
        </div>
      </div>
      <div class="filter-action-row">
        <button class="btn-action btn-search" @click="fetchData(true)" :disabled="loading">
          {{ loading ? '查询中...' : '🔍 查询' }}
        </button>
        <button class="btn-action btn-reset" @click="resetFilters">🔄 重置</button>
        <div class="clean-section">
          <span class="clean-label">清理</span>
          <input v-model.number="cleanDays" type="number" class="clean-input" min="1" max="365" />
          <span class="clean-label">天前的日志</span>
          <button class="btn-action btn-clean" @click="handleClean" :disabled="cleaning">
            {{ cleaning ? '清理中...' : '🗑 清理' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="queried" class="result-toolbar">
      <span class="result-count">共 <strong>{{ total }}</strong> 条 · 第 {{ currentPage }}/{{ totalPages() }} 页</span>
    </div>

    <div v-if="loading && !queried" class="state-block">
      <div class="state-spinner"></div>
      <div class="state-text">正在查询...</div>
    </div>
    <div v-if="!loading && queried && list.length === 0" class="state-block">
      <div class="state-icon">📭</div>
      <div class="state-text">没有符合条件的审计记录</div>
    </div>

    <div v-if="list.length > 0" class="table-wrapper">
      <table class="data-table">
        <thead>
        <tr>
          <th>#</th><th>操作人</th><th>角色</th><th>操作</th><th>接口</th>
          <th>IP</th><th>耗时</th><th>结果</th><th>时间</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(item, index) in list" :key="item.id">
          <td class="cell-index">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
          <td><span class="operator-tag">{{ item.operator }}</span></td>
          <td><span class="role-tag" :class="'role-' + (item.operatorType || 'UNKNOWN').toLowerCase()">{{ item.operatorType || '-' }}</span></td>
          <td class="cell-action">{{ item.action || '-' }}</td>
          <td class="cell-uri" :title="item.uri">{{ item.uri || '-' }}</td>
          <td class="cell-mono">{{ item.ip || '-' }}</td>
          <td class="cell-num">{{ formatDuration(item.durationMs) }}</td>
          <td>
            <span :class="item.success ? 'tag-ok' : 'tag-fail'">{{ item.success ? '成功' : '失败' }}</span>
          </td>
          <td class="cell-time">{{ item.createdAt || '-' }}</td>
        </tr>
        </tbody>
      </table>
    </div>

    <div v-if="total > 0" class="pagination">
      <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">‹ 上一页</button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages() }} 页，共 {{ total }} 条</span>
      <button class="page-btn" :disabled="currentPage >= totalPages()" @click="nextPage">下一页 ›</button>
      <label class="page-size-label">
        每页 <input class="page-size-input" type="number" v-model="pageSizeInput" @keydown.enter="applyPageSize" @blur="applyPageSize" min="1" max="200" /> 条
      </label>
    </div>
  </div>
</template>

<style scoped>
.audit-page { width: 100%; max-width: 1600px; margin: 0 auto; padding: 0 20px 40px; box-sizing: border-box; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.page-header h2 { margin: 0; font-size: 22px; color: #1a1a2e; font-weight: 700; }
.page-header-sub { font-size: 13px; color: #999; }

.filter-card { background: #fafbfc; border-radius: 14px; padding: 20px; margin-bottom: 16px; border: 1px solid #eaeaea; }
.filter-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 12px; margin-bottom: 14px; }
.filter-field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 11px; font-weight: 700; color: #999; text-transform: uppercase; letter-spacing: 0.3px; }
.filter-input { padding: 8px 14px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 10px; outline: none; transition: border-color 0.2s; background: #fff; }
.filter-input:focus { border-color: #667eea; box-shadow: 0 0 0 3px rgba(102,126,234,0.12); }
.filter-date { cursor: pointer; }
.filter-action-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.btn-action { border: none; padding: 10px 24px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600; transition: transform 0.15s, box-shadow 0.15s; }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-search { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.btn-search:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(102,126,234,0.35); }
.btn-reset { background: linear-gradient(135deg, #f87171, #ef4444); color: #fff; padding: 8px 20px; font-size: 12px; }
.btn-clean { background: linear-gradient(135deg, #f59e0b, #f97316); color: #fff; padding: 8px 16px; font-size: 12px; }
.clean-section { display: flex; align-items: center; gap: 6px; margin-left: auto; }
.clean-label { font-size: 12px; color: #666; font-weight: 600; }
.clean-input { width: 60px; padding: 6px 8px; border: 2px solid #e8e8e8; border-radius: 8px; font-size: 13px; text-align: center; outline: none; }

.result-toolbar { display: flex; align-items: center; padding: 10px 16px; background: #f8f9fc; border-radius: 10px; margin-bottom: 12px; border: 1px solid #eaeaea; }
.result-count { font-size: 13px; color: #666; }
.result-count strong { color: #667eea; font-weight: 800; }

.state-block { text-align: center; padding: 50px 20px; }
.state-icon { font-size: 48px; margin-bottom: 12px; }
.state-text { color: #aaa; font-size: 14px; }
.state-spinner { width: 32px; height: 32px; border: 3px solid #e0e0e0; border-top-color: #667eea; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 12px; }
@keyframes spin { to { transform: rotate(360deg); } }

.table-wrapper { overflow-x: auto; border-radius: 10px; border: 1px solid #e8e8e8; margin-bottom: 16px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 900px; }
.data-table th { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 11px 14px; white-space: nowrap; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px; }
.data-table td { padding: 10px 14px; border-bottom: 1px solid #f0f0f0; color: #333; vertical-align: middle; }
.data-table tbody tr:hover td { background: #f8f9ff; }
.cell-index { color: #aaa; font-weight: 600; width: 40px; text-align: center; }
.cell-action { font-weight: 600; color: #333; max-width: 160px; }
.cell-uri { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-family: 'SF Mono', monospace; font-size: 11px; color: #555; }
.cell-mono { font-family: 'SF Mono', monospace; font-size: 11px; color: #555; }
.cell-num { font-weight: 700; text-align: center; }
.cell-time { font-size: 11px; color: #888; white-space: nowrap; }
.operator-tag { background: #eef2ff; color: #3730a3; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; }
.role-tag { padding: 3px 10px; border-radius: 20px; font-size: 10px; font-weight: 700; }
.role-user { background: #dcfce7; color: #166534; }
.role-admin { background: #fef3c7; color: #92400e; }
.role-developer { background: #e0e7ff; color: #3730a3; }
.role-unknown { background: #f3f4f6; color: #6b7280; }
.tag-ok { background: #dcfce7; color: #166534; padding: 3px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; }
.tag-fail { background: #fef2f2; color: #991b1b; padding: 3px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 14px 0; }
.page-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; padding: 8px 22px; border-radius: 10px; cursor: pointer; font-weight: 600; font-size: 13px; transition: transform 0.15s; }
.page-btn:hover { transform: translateY(-2px); }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }
.page-info { font-size: 13px; color: #666; font-weight: 600; }
.page-size-label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #666; }
.page-size-input { width: 60px; padding: 6px 8px; border: 2px solid #e8e8e8; border-radius: 8px; font-size: 13px; text-align: center; outline: none; }
</style>