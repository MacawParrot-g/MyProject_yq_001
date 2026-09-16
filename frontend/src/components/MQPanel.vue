<!-- 文件路径: src/main/frontend/src/components/MQPanel.vue -->
<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { fetchMQOverview, fetchDlqRecords, retryDlqMessage, clearDlqRecords, fetchDlqSummary } from '../api/index.js'

const emit = defineEmits(['error'])

const activeTab = ref('overview')
const overview = ref(null)
const overviewLoading = ref(false)

const dlqType = ref('insert')
const dlqRecords = ref([])
const dlqLoading = ref(false)

const dlqSummary = ref({ insertDlqCount: 0, updateDlqCount: 0, deleteDlqCount: 0, totalDlqCount: 0 })
const summaryLoading = ref(false)

const retryingIndex = ref(-1)
const autoRefresh = ref(true)
let refreshTimer = null

async function loadOverview() {
  overviewLoading.value = true
  try {
    const json = await fetchMQOverview()
    if (json.success) overview.value = json.data
  } catch (e) { emit('error', 'MQ队列信息获取失败') }
  finally { overviewLoading.value = false }
}

async function loadDlqRecords() {
  dlqLoading.value = true
  try {
    const json = await fetchDlqRecords(dlqType.value)
    if (json.success) dlqRecords.value = json.data || []
  } catch (e) { emit('error', 'DLQ记录获取失败') }
  finally { dlqLoading.value = false }
}

async function loadDlqSummary() {
  summaryLoading.value = true
  try {
    const json = await fetchDlqSummary()
    if (json.success) dlqSummary.value = json.data || {}
  } catch (e) { /* silent */ }
  finally { summaryLoading.value = false }
}

async function handleRetry(index) {
  if (!confirm('确定要重试这条死信消息吗？')) return
  retryingIndex.value = index
  try {
    const json = await retryDlqMessage(dlqType.value, index)
    if (json.success) {
      alert('重试成功')
      loadDlqRecords()
      loadDlqSummary()
    } else {
      alert('重试失败: ' + (json.message || '未知错误'))
    }
  } catch (e) { alert('重试请求失败: ' + e.message) }
  finally { retryingIndex.value = -1 }
}

async function handleClear() {
  if (!confirm('确定要清除 ' + dlqType.value + ' 类型的所有死信记录吗？')) return
  try {
    const json = await clearDlqRecords(dlqType.value)
    if (json.success) {
      dlqRecords.value = []
      loadDlqSummary()
    }
  } catch (e) { alert('清除失败: ' + e.message) }
}

function switchDlqType(type) {
  dlqType.value = type
  loadDlqRecords()
}

function getQueueStatusClass(status) {
  if (status === 'healthy') return 'queue-healthy'
  if (status === 'error') return 'queue-error'
  return 'queue-unknown'
}

function formatTimestamp(ts) {
  if (!ts) return ''
  return ts
}

function startAutoRefresh() {
  stopAutoRefresh()
  refreshTimer = setInterval(() => {
    if (autoRefresh.value) {
      loadOverview()
      loadDlqSummary()
      if (activeTab.value === 'dlq') loadDlqRecords()
    }
  }, 5000)
}

function stopAutoRefresh() {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
}

onMounted(() => {
  loadOverview()
  loadDlqSummary()
  loadDlqRecords()
  startAutoRefresh()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<template>
  <div class="mq-panel">
    <div class="mq-tabs">
      <button :class="{ active: activeTab === 'overview' }" @click="activeTab = 'overview'">
        📊 队列总览
      </button>
      <button :class="{ active: activeTab === 'dlq' }" @click="activeTab = 'dlq'">
        🚨 死信队列
        <span class="dlq-badge" v-if="dlqSummary.totalDlqCount > 0">{{ dlqSummary.totalDlqCount }}</span>
      </button>
    </div>

    <div class="mq-toolbar">
      <label class="auto-refresh-toggle">
        <input type="checkbox" v-model="autoRefresh" />
        自动刷新 (5s)
      </label>
      <button class="btn-mq-refresh" @click="loadOverview(); loadDlqSummary()">🔄 刷新</button>
    </div>

    <!-- 队列总览 -->
    <div v-if="activeTab === 'overview'" class="overview-section">
      <div v-if="overviewLoading && !overview" class="loading">加载中...</div>
      <div v-else-if="overview" class="queue-grid">
        <div v-for="q in overview.queues" :key="q.queueName" class="queue-card" :class="getQueueStatusClass(q.status)">
          <div class="queue-header">
            <span class="queue-label">{{ q.label }}</span>
            <span class="queue-status-dot" :class="getQueueStatusClass(q.status)"></span>
          </div>
          <div class="queue-body">
            <div class="queue-stat">
              <span class="queue-stat-label">消息数</span>
              <span class="queue-stat-value" :class="{ 'text-danger': q.messageCount > 100 }">
                {{ q.messageCount >= 0 ? q.messageCount : 'N/A' }}
              </span>
            </div>
            <div class="queue-stat">
              <span class="queue-stat-label">队列名</span>
              <span class="queue-stat-name">{{ q.queueName }}</span>
            </div>
            <div class="queue-stat" v-if="q.dlqRecordCount !== undefined">
              <span class="queue-stat-label">死信记录</span>
              <span class="queue-stat-value" :class="{ 'text-danger': q.dlqRecordCount > 0 }">{{ q.dlqRecordCount }}</span>
            </div>
            <div class="queue-stat" v-if="q.status === 'error'">
              <span class="queue-stat-label">错误</span>
              <span class="queue-stat-error">{{ q.error }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="dlq-summary-cards">
        <div class="summary-card summary-insert">
          <div class="summary-icon">📥</div>
          <div class="summary-label">Insert DLQ</div>
          <div class="summary-count">{{ dlqSummary.insertDlqCount || 0 }}</div>
        </div>
        <div class="summary-card summary-update">
          <div class="summary-icon">📝</div>
          <div class="summary-label">Update DLQ</div>
          <div class="summary-count">{{ dlqSummary.updateDlqCount || 0 }}</div>
        </div>
        <div class="summary-card summary-delete">
          <div class="summary-icon">🗑️</div>
          <div class="summary-label">Delete DLQ</div>
          <div class="summary-count">{{ dlqSummary.deleteDlqCount || 0 }}</div>
        </div>
        <div class="summary-card summary-total">
          <div class="summary-icon">📊</div>
          <div class="summary-label">总计</div>
          <div class="summary-count">{{ dlqSummary.totalDlqCount || 0 }}</div>
        </div>
      </div>
    </div>

    <!-- 死信队列详情 -->
    <div v-if="activeTab === 'dlq'" class="dlq-section">
      <div class="dlq-type-tabs">
        <button :class="{ active: dlqType === 'insert' }" @click="switchDlqType('insert')">
          Insert
          <span class="type-badge" v-if="dlqSummary.insertDlqCount > 0">{{ dlqSummary.insertDlqCount }}</span>
        </button>
        <button :class="{ active: dlqType === 'update' }" @click="switchDlqType('update')">
          Update
          <span class="type-badge" v-if="dlqSummary.updateDlqCount > 0">{{ dlqSummary.updateDlqCount }}</span>
        </button>
        <button :class="{ active: dlqType === 'delete' }" @click="switchDlqType('delete')">
          Delete
          <span class="type-badge" v-if="dlqSummary.deleteDlqCount > 0">{{ dlqSummary.deleteDlqCount }}</span>
        </button>
        <button class="btn-clear-dlq" @click="handleClear">🗑️ 清空</button>
      </div>

      <div v-if="dlqLoading" class="loading">加载中...</div>
      <div v-else-if="dlqRecords.length === 0" class="dlq-empty">
        <div class="empty-icon">✅</div>
        <div class="empty-text">暂无死信消息，一切正常！</div>
      </div>
      <div v-else class="dlq-list">
        <div v-for="(record, index) in dlqRecords" :key="index" class="dlq-item">
          <div class="dlq-item-header">
            <span class="dlq-item-index">#{{ index + 1 }}</span>
            <span class="dlq-item-time">{{ record.timestamp }}</span>
          </div>
          <div class="dlq-item-body">
            <div class="dlq-field">
              <span class="dlq-field-label">队列</span>
              <span class="dlq-field-value">{{ record.queueName }}</span>
            </div>
            <div class="dlq-field">
              <span class="dlq-field-label">Exchange</span>
              <span class="dlq-field-value">{{ record.exchange || 'N/A' }}</span>
            </div>
            <div class="dlq-field">
              <span class="dlq-field-label">RoutingKey</span>
              <span class="dlq-field-value">{{ record.routingKey || 'N/A' }}</span>
            </div>
            <div class="dlq-field">
              <span class="dlq-field-label">消息内容</span>
              <pre class="dlq-field-body">{{ record.body }}</pre>
            </div>
          </div>
          <div class="dlq-item-actions">
            <button class="btn-retry" @click="handleRetry(index)" :disabled="retryingIndex === index">
              {{ retryingIndex === index ? '重试中...' : '🔄 重试' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mq-panel {
  max-width: 1200px;
  margin: 0 auto;
}

.mq-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}
.mq-tabs button {
  padding: 10px 24px;
  border: 2px solid var(--border-color);
  border-radius: 10px;
  background: var(--bg-card);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
}
.mq-tabs button.active {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-glow);
}

.dlq-badge {
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 10px;
  font-weight: 700;
}

.mq-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.auto-refresh-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}
.auto-refresh-toggle input {
  accent-color: var(--accent);
}
.btn-mq-refresh {
  background: linear-gradient(135deg, var(--accent), #a855f7);
  color: #fff;
  border: none;
  padding: 9px 20px;
  font-size: 13px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: 600;
  transition: var(--transition);
}
.btn-mq-refresh:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(99,102,241,0.35);
}

.queue-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  margin-bottom: 28px;
}
.queue-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 18px 20px;
  transition: var(--transition);
  border-left: 4px solid var(--success);
}
.queue-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.queue-card.queue-error {
  border-left-color: var(--danger);
}
.queue-card.queue-unknown {
  border-left-color: var(--text-secondary);
}

.queue-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.queue-label {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}
.queue-status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--success);
}
.queue-status-dot.queue-error {
  background: var(--danger);
}
.queue-status-dot.queue-unknown {
  background: var(--text-secondary);
}

.queue-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.queue-stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.queue-stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}
.queue-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}
.queue-stat-value.text-danger {
  color: var(--danger);
}
.queue-stat-name {
  font-size: 11px;
  color: var(--text-secondary);
  font-family: monospace;
}
.queue-stat-error {
  font-size: 11px;
  color: var(--danger);
  word-break: break-all;
}

.dlq-summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.summary-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  transition: var(--transition);
}
.summary-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.summary-icon {
  font-size: 28px;
  margin-bottom: 8px;
}
.summary-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.summary-count {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}
.summary-total .summary-count {
  color: var(--accent);
}

.dlq-type-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  align-items: center;
}
.dlq-type-tabs button {
  padding: 8px 18px;
  border: 2px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-card);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: var(--transition);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.dlq-type-tabs button.active {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-glow);
}
.type-badge {
  background: var(--danger);
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 8px;
  font-weight: 700;
}
.btn-clear-dlq {
  margin-left: auto;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  border: none;
  padding: 8px 16px;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: var(--transition);
}
.btn-clear-dlq:hover {
  transform: translateY(-1px);
}

.dlq-empty {
  text-align: center;
  padding: 60px 20px;
  background: var(--bg-card);
  border-radius: 12px;
  border: 1px solid var(--border-color);
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
.empty-text {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 600;
}

.dlq-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.dlq-item {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px 20px;
  border-left: 4px solid var(--danger);
  transition: var(--transition);
}
.dlq-item:hover {
  box-shadow: var(--shadow-sm);
}
.dlq-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.dlq-item-index {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent);
}
.dlq-item-time {
  font-size: 12px;
  color: var(--text-secondary);
}
.dlq-item-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.dlq-field {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.dlq-field-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  min-width: 80px;
  flex-shrink: 0;
  padding-top: 2px;
}
.dlq-field-value {
  font-size: 13px;
  color: var(--text-primary);
  word-break: break-all;
}
.dlq-field-body {
  font-size: 12px;
  color: var(--text-primary);
  background: #f1f5f9;
  padding: 10px 12px;
  border-radius: 8px;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 120px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  margin: 0;
  flex: 1;
}
.dlq-item-actions {
  display: flex;
  gap: 8px;
}
.btn-retry {
  background: linear-gradient(135deg, #f59e0b, #f97316);
  color: #fff;
  border: none;
  padding: 7px 18px;
  font-size: 12px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: var(--transition);
}
.btn-retry:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(245,158,11,0.35);
}
.btn-retry:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .queue-grid {
    grid-template-columns: 1fr;
  }
  .dlq-summary-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>