<!-- 文件路径: src/main/frontend/src/components/NotificationManager.vue -->
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { sendNotification, fetchAllNotifications, deleteNotification } from '../api/index.js'

const emit = defineEmits(['error'])

const list = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(15)
const pageSizeInput = ref(15)

const showSendForm = ref(false)
const sending = ref(false)
const sendMsg = ref('')

const newNotification = reactive({
  receiver: '',
  type: 'SYSTEM',
  title: '',
  content: ''
})

const typeOptions = [
  { value: 'SYSTEM', label: '📢 系统通知' },
  { value: 'EXPORT', label: '📤 导出通知' },
  { value: 'ALERT', label: '⚠️ 告警通知' }
]

const totalPages = () => Math.max(1, Math.ceil(total.value / pageSize.value))

async function loadAll() {
  loading.value = true
  try {
    const json = await fetchAllNotifications(currentPage.value, pageSize.value)
    if (json.success) {
      list.value = json.data || []
      total.value = json.total || 0
    } else {
      emit('error', json.message || '加载失败')
    }
  } catch (e) {
    emit('error', '加载失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function handleSend() {
  if (!newNotification.receiver.trim()) { sendMsg.value = '❌ 请输入接收人'; return }
  if (!newNotification.title.trim()) { sendMsg.value = '❌ 请输入标题'; return }
  sending.value = true
  sendMsg.value = ''
  try {
    const json = await sendNotification(
        newNotification.receiver.trim(),
        newNotification.type,
        newNotification.title.trim(),
        newNotification.content.trim()
    )
    if (json.success) {
      sendMsg.value = '✅ ' + json.message
      newNotification.receiver = ''
      newNotification.title = ''
      newNotification.content = ''
      showSendForm.value = false
      await loadAll()
    } else {
      sendMsg.value = '❌ ' + (json.message || '发送失败')
    }
  } catch (e) {
    sendMsg.value = '❌ 发送请求失败：' + e.message
  } finally {
    sending.value = false
  }
}

async function handleDelete(n) {
  if (!confirm(`确定要删除通知「${n.title}」吗？`)) return
  try {
    const json = await deleteNotification(n.id)
    if (json.success) {
      await loadAll()
    } else {
      emit('error', json.message || '删除失败')
    }
  } catch (e) {
    emit('error', '删除请求失败：' + e.message)
  }
}

function prevPage() { if (currentPage.value > 1) { currentPage.value--; loadAll() } }
function nextPage() { if (currentPage.value < totalPages()) { currentPage.value++; loadAll() } }
function applyPageSize() {
  const v = parseInt(pageSizeInput.value)
  if (v > 0) { pageSize.value = v; currentPage.value = 1; loadAll() }
}

function getTypeIcon(type) {
  switch (type) {
    case 'SYSTEM': return '📢'
    case 'EXPORT': return '📤'
    case 'ALERT': return '⚠️'
    default: return '🔔'
  }
}

onMounted(() => { loadAll() })
</script>

<template>
  <div class="notify-page">
    <div class="page-header">
      <h2>📨 通知管理</h2>
      <div class="page-header-sub">发送和管理系统通知</div>
    </div>

    <div class="notify-actions-bar">
      <button class="btn-action btn-send" @click="showSendForm = !showSendForm">
        {{ showSendForm ? '✕ 取消' : '✉️ 发送通知' }}
      </button>
      <button class="btn-action btn-refresh-n" @click="loadAll" :disabled="loading">
        {{ loading ? '加载中...' : '🔄 刷新' }}
      </button>
    </div>

    <div v-if="showSendForm" class="send-card">
      <h3>发送新通知</h3>
      <div class="send-grid">
        <div class="send-field">
          <label class="field-label">接收人</label>
          <input v-model="newNotification.receiver" class="send-input" placeholder="用户名" />
        </div>
        <div class="send-field">
          <label class="field-label">通知类型</label>
          <select v-model="newNotification.type" class="send-input">
            <option v-for="opt in typeOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
          </select>
        </div>
        <div class="send-field send-field-full">
          <label class="field-label">标题</label>
          <input v-model="newNotification.title" class="send-input" placeholder="通知标题" />
        </div>
        <div class="send-field send-field-full">
          <label class="field-label">内容</label>
          <textarea v-model="newNotification.content" class="send-textarea" placeholder="通知内容（可选）" rows="3"></textarea>
        </div>
      </div>
      <div v-if="sendMsg" class="send-msg" :class="{ 'send-msg-ok': sendMsg.startsWith('✅'), 'send-msg-err': sendMsg.startsWith('❌') }">{{ sendMsg }}</div>
      <button class="btn-action btn-submit" @click="handleSend" :disabled="sending">
        {{ sending ? '发送中...' : '📤 确认发送' }}
      </button>
    </div>

    <div v-if="total > 0" class="result-toolbar">
      <span class="result-count">共 <strong>{{ total }}</strong> 条 · 第 {{ currentPage }}/{{ totalPages() }} 页</span>
    </div>

    <div v-if="list.length === 0 && !loading" class="state-block">
      <div class="state-icon">📭</div>
      <div class="state-text">暂无通知记录</div>
    </div>

    <div v-if="list.length > 0" class="table-wrapper">
      <table class="data-table">
        <thead>
        <tr>
          <th>#</th><th>类型</th><th>接收人</th><th>标题</th><th>内容</th>
          <th>状态</th><th>时间</th><th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(n, index) in list" :key="n.id">
          <td class="cell-index">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
          <td><span class="type-tag">{{ getTypeIcon(n.type) }} {{ n.type }}</span></td>
          <td><span class="receiver-tag">{{ n.receiver }}</span></td>
          <td class="cell-title">{{ n.title }}</td>
          <td class="cell-content" :title="n.content">{{ n.content || '-' }}</td>
          <td>
            <span :class="n.isRead ? 'tag-read' : 'tag-unread'">{{ n.isRead ? '已读' : '未读' }}</span>
          </td>
          <td class="cell-time">{{ n.createdAt || '-' }}</td>
          <td>
            <button class="btn-sm-del" @click="handleDelete(n)">🗑 删除</button>
          </td>
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
.notify-page { width: 100%; max-width: 1400px; margin: 0 auto; padding: 0 20px 40px; box-sizing: border-box; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.page-header h2 { margin: 0; font-size: 22px; color: #1a1a2e; font-weight: 700; }
.page-header-sub { font-size: 13px; color: #999; }

.notify-actions-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.btn-action { border: none; padding: 10px 24px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600; transition: transform 0.15s, box-shadow 0.15s; }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-send { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.btn-send:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(102,126,234,0.35); }
.btn-refresh-n { background: linear-gradient(135deg, #60a5fa, #3b82f6); color: #fff; }

.send-card { background: #fafbfc; border-radius: 14px; padding: 24px; margin-bottom: 20px; border: 2px solid #e0e7ff; }
.send-card h3 { margin: 0 0 16px; font-size: 16px; color: #1a1a2e; }
.send-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 14px; }
.send-field { display: flex; flex-direction: column; gap: 4px; }
.send-field-full { grid-column: 1 / -1; }
.field-label { font-size: 11px; font-weight: 700; color: #999; text-transform: uppercase; letter-spacing: 0.3px; }
.send-input { padding: 10px 14px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 10px; outline: none; background: #fff; transition: border-color 0.2s; }
.send-input:focus { border-color: #667eea; }
.send-textarea { padding: 10px 14px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 10px; outline: none; background: #fff; resize: vertical; font-family: inherit; transition: border-color 0.2s; }
.send-textarea:focus { border-color: #667eea; }
.send-msg { font-size: 13px; font-weight: 600; padding: 8px 14px; border-radius: 8px; margin-bottom: 12px; }
.send-msg-ok { background: #dcfce7; color: #166534; }
.send-msg-err { background: #fef2f2; color: #991b1b; }
.btn-submit { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }

.result-toolbar { display: flex; align-items: center; padding: 10px 16px; background: #f8f9fc; border-radius: 10px; margin-bottom: 12px; border: 1px solid #eaeaea; }
.result-count { font-size: 13px; color: #666; }
.result-count strong { color: #667eea; font-weight: 800; }

.state-block { text-align: center; padding: 50px 20px; }
.state-icon { font-size: 48px; margin-bottom: 12px; }
.state-text { color: #aaa; font-size: 14px; }

.table-wrapper { overflow-x: auto; border-radius: 10px; border: 1px solid #e8e8e8; margin-bottom: 16px; }
.data-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 800px; }
.data-table th { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; padding: 11px 14px; white-space: nowrap; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.3px; }
.data-table td { padding: 10px 14px; border-bottom: 1px solid #f0f0f0; color: #333; vertical-align: middle; }
.data-table tbody tr:hover td { background: #f8f9ff; }
.cell-index { color: #aaa; font-weight: 600; width: 40px; text-align: center; }
.cell-title { font-weight: 600; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cell-content { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #666; }
.cell-time { font-size: 11px; color: #888; white-space: nowrap; }
.type-tag { background: #e0e7ff; color: #3730a3; padding: 3px 10px; border-radius: 20px; font-size: 10px; font-weight: 700; }
.receiver-tag { background: #dcfce7; color: #166534; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; }
.tag-read { background: #f3f4f6; color: #6b7280; padding: 3px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; }
.tag-unread { background: #fef3c7; color: #92400e; padding: 3px 12px; border-radius: 20px; font-size: 11px; font-weight: 700; }
.btn-sm-del { background: #fef2f2; color: #991b1b; border: none; padding: 5px 12px; border-radius: 8px; cursor: pointer; font-size: 11px; font-weight: 600; }
.btn-sm-del:hover { background: #fecaca; }

.pagination { display: flex; justify-content: center; align-items: center; gap: 16px; padding: 14px 0; }
.page-btn { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; padding: 8px 22px; border-radius: 10px; cursor: pointer; font-weight: 600; font-size: 13px; transition: transform 0.15s; }
.page-btn:hover { transform: translateY(-2px); }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; transform: none; }
.page-info { font-size: 13px; color: #666; font-weight: 600; }
.page-size-label { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #666; }
.page-size-input { width: 60px; padding: 6px 8px; border: 2px solid #e8e8e8; border-radius: 8px; font-size: 13px; text-align: center; outline: none; }
</style>