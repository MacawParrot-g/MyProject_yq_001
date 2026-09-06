<!-- 文件路径: src/main/frontend/src/components/ScheduledTaskPanel.vue -->
<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { fetchTaskList, createTask, toggleTask, deleteTask, executeTaskNow } from '../api/index.js'

const emit = defineEmits(['error'])

const list = ref([])
const loading = ref(false)
const showCreateForm = ref(false)
const creating = ref(false)
const createMsg = ref('')

const newTask = reactive({
  name: '',
  taskType: 'AUDIT_LOG_CLEAN',
  cronExpression: '24h',
  params: ''
})

const taskTypeOptions = [
  { value: 'AUDIT_LOG_CLEAN', label: '📋 审计日志清理', desc: '自动清理过期的审计日志' },
  { value: 'DATA_ARCHIVE', label: '📦 数据归档', desc: '将老数据迁移至归档表' },
  { value: 'EXPORT_MARK_RESET', label: '🔄 导出标记重置', desc: '重置已导出数据的标记' }
]

const cronPresets = [
  { value: '1h', label: '每 1 小时' },
  { value: '6h', label: '每 6 小时' },
  { value: '12h', label: '每 12 小时' },
  { value: '24h', label: '每 24 小时' },
  { value: '7d', label: '每 7 天' },
  { value: '30d', label: '每 30 天' }
]

async function loadTasks() {
  loading.value = true
  try {
    const json = await fetchTaskList()
    if (json.success) {
      list.value = json.data || []
    } else {
      emit('error', json.message || '加载失败')
    }
  } catch (e) {
    emit('error', '加载失败：' + e.message)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!newTask.name.trim()) { createMsg.value = '❌ 请输入任务名称'; return }
  creating.value = true
  createMsg.value = ''
  try {
    const json = await createTask({
      name: newTask.name.trim(),
      taskType: newTask.taskType,
      cronExpression: newTask.cronExpression,
      params: newTask.params.trim()
    })
    if (json.success) {
      createMsg.value = '✅ ' + json.message
      newTask.name = ''
      newTask.params = ''
      showCreateForm.value = false
      await loadTasks()
    } else {
      createMsg.value = '❌ ' + (json.message || '创建失败')
    }
  } catch (e) {
    createMsg.value = '❌ 创建请求失败：' + e.message
  } finally {
    creating.value = false
  }
}

async function handleToggle(task) {
  try {
    const json = await toggleTask(task.id, !task.enabled)
    if (json.success) {
      task.enabled = !task.enabled
    } else {
      emit('error', json.message || '操作失败')
    }
  } catch (e) {
    emit('error', '操作失败：' + e.message)
  }
}

async function handleDelete(task) {
  if (!confirm(`确定要删除任务「${task.name}」吗？`)) return
  try {
    const json = await deleteTask(task.id)
    if (json.success) await loadTasks()
    else emit('error', json.message || '删除失败')
  } catch (e) {
    emit('error', '删除请求失败：' + e.message)
  }
}

async function handleExecuteNow(task) {
  if (!confirm(`确定要立即执行任务「${task.name}」吗？`)) return
  try {
    const json = await executeTaskNow(task.id)
    if (json.success) {
      alert('✅ ' + json.message)
      await loadTasks()
    } else {
      emit('error', json.message || '执行失败')
    }
  } catch (e) {
    emit('error', '执行请求失败：' + e.message)
  }
}

function getTaskTypeLabel(type) {
  const found = taskTypeOptions.find(t => t.value === type)
  return found ? found.label : type
}

onMounted(() => {
  loadTasks()
})
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<template>
  <div class="task-page">
    <div class="page-header">
      <h2>⏰ 定时任务管理</h2>
      <div class="page-header-sub">管理和监控系统自动化任务</div>
    </div>

    <div class="task-actions-bar">
      <button class="btn-action btn-create" @click="showCreateForm = !showCreateForm">
        {{ showCreateForm ? '✕ 取消' : '➕ 创建任务' }}
      </button>
      <button class="btn-action btn-refresh-task" @click="loadTasks" :disabled="loading">
        {{ loading ? '加载中...' : '🔄 刷新' }}
      </button>
    </div>

    <div v-if="showCreateForm" class="create-card">
      <h3>创建新任务</h3>
      <div class="create-grid">
        <div class="create-field">
          <label class="field-label">任务名称</label>
          <input v-model="newTask.name" class="create-input" placeholder="例如：每日清理审计日志" />
        </div>
        <div class="create-field">
          <label class="field-label">任务类型</label>
          <select v-model="newTask.taskType" class="create-input">
            <option v-for="opt in taskTypeOptions" :key="opt.value" :value="opt.value">{{ opt.label }} - {{ opt.desc }}</option>
          </select>
        </div>
        <div class="create-field">
          <label class="field-label">执行间隔</label>
          <div class="cron-row">
            <select v-model="newTask.cronExpression" class="create-input cron-select">
              <option v-for="p in cronPresets" :key="p.value" :value="p.value">{{ p.label }}</option>
            </select>
            <span class="cron-or">或</span>
            <input v-model="newTask.cronExpression" class="create-input cron-custom" placeholder="自定义：如 2h, 30m, 7d" />
          </div>
        </div>
        <div class="create-field">
          <label class="field-label">参数（可选）</label>
          <input v-model="newTask.params" class="create-input" placeholder="如：保留天数 90" />
        </div>
      </div>
      <div v-if="createMsg" class="create-msg" :class="{ 'create-msg-ok': createMsg.startsWith('✅'), 'create-msg-err': createMsg.startsWith('❌') }">{{ createMsg }}</div>
      <button class="btn-action btn-submit" @click="handleCreate" :disabled="creating">
        {{ creating ? '创建中...' : '✅ 确认创建' }}
      </button>
    </div>

    <div v-if="list.length === 0 && !loading" class="state-block">
      <div class="state-icon">📭</div>
      <div class="state-text">暂无定时任务，点击上方按钮创建</div>
    </div>

    <div v-if="list.length > 0" class="task-list">
      <div v-for="task in list" :key="task.id" class="task-card" :class="{ 'task-disabled': !task.enabled }">
        <div class="task-card-header">
          <div class="task-card-title-row">
            <span class="task-status-dot" :class="task.enabled ? 'dot-on' : 'dot-off'"></span>
            <span class="task-card-name">{{ task.name }}</span>
            <span class="task-type-tag">{{ getTaskTypeLabel(task.taskType) }}</span>
          </div>
          <div class="task-card-actions">
            <button class="btn-sm btn-sm-run" @click="handleExecuteNow(task)" title="立即执行">▶ 执行</button>
            <button class="btn-sm" :class="task.enabled ? 'btn-sm-stop' : 'btn-sm-start'" @click="handleToggle(task)">
              {{ task.enabled ? '⏸ 停用' : '▶ 启用' }}
            </button>
            <button class="btn-sm btn-sm-del" @click="handleDelete(task)">🗑</button>
          </div>
        </div>
        <div class="task-card-body">
          <div class="task-info-row">
            <span class="task-info-label">间隔：</span>
            <span class="task-info-value">{{ task.cronExpression }}</span>
          </div>
          <div class="task-info-row" v-if="task.params">
            <span class="task-info-label">参数：</span>
            <span class="task-info-value">{{ task.params }}</span>
          </div>
          <div class="task-info-row">
            <span class="task-info-label">已执行：</span>
            <span class="task-info-value">{{ task.totalExecutions || 0 }} 次</span>
          </div>
          <div class="task-info-row" v-if="task.lastExecutedAt">
            <span class="task-info-label">上次执行：</span>
            <span class="task-info-value">{{ task.lastExecutedAt }}</span>
          </div>
          <div class="task-info-row" v-if="task.lastResult">
            <span class="task-info-label">上次结果：</span>
            <span class="task-info-value task-result" :class="{ 'task-result-err': task.lastResult.includes('异常') }">{{ task.lastResult }}</span>
          </div>
          <div class="task-info-row" v-if="task.createdBy">
            <span class="task-info-label">创建人：</span>
            <span class="task-info-value">{{ task.createdBy }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-page { width: 100%; max-width: 1200px; margin: 0 auto; padding: 0 20px 40px; box-sizing: border-box; }
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.page-header h2 { margin: 0; font-size: 22px; color: #1a1a2e; font-weight: 700; }
.page-header-sub { font-size: 13px; color: #999; }

.task-actions-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.btn-action { border: none; padding: 10px 24px; font-size: 13px; border-radius: 10px; cursor: pointer; font-weight: 600; transition: transform 0.15s, box-shadow 0.15s; }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-create { background: linear-gradient(135deg, #22c55e, #10b981); color: #fff; }
.btn-create:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(34,197,94,0.35); }
.btn-refresh-task { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; }
.btn-refresh-task:hover:not(:disabled) { transform: translateY(-2px); }

.create-card { background: #fafbfc; border-radius: 14px; padding: 24px; margin-bottom: 20px; border: 2px solid #e8f5e9; }
.create-card h3 { margin: 0 0 16px; font-size: 16px; color: #1a1a2e; }
.create-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 14px; }
.create-field { display: flex; flex-direction: column; gap: 4px; }
.field-label { font-size: 11px; font-weight: 700; color: #999; text-transform: uppercase; letter-spacing: 0.3px; }
.create-input { padding: 10px 14px; font-size: 13px; border: 2px solid #e8e8e8; border-radius: 10px; outline: none; background: #fff; transition: border-color 0.2s; }
.create-input:focus { border-color: #667eea; }
.cron-row { display: flex; align-items: center; gap: 8px; }
.cron-select { flex: 1; }
.cron-or { font-size: 11px; color: #999; }
.cron-custom { flex: 1; }
.create-msg { font-size: 13px; font-weight: 600; padding: 8px 14px; border-radius: 8px; margin-bottom: 12px; }
.create-msg-ok { background: #dcfce7; color: #166534; }
.create-msg-err { background: #fef2f2; color: #991b1b; }
.btn-submit { background: linear-gradient(135deg, #22c55e, #10b981); color: #fff; }

.state-block { text-align: center; padding: 50px 20px; }
.state-icon { font-size: 48px; margin-bottom: 12px; }
.state-text { color: #aaa; font-size: 14px; }

.task-list { display: flex; flex-direction: column; gap: 14px; }
.task-card { background: #fff; border-radius: 14px; border: 1px solid #e8e8e8; overflow: hidden; transition: box-shadow 0.2s; }
.task-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
.task-disabled { opacity: 0.6; }
.task-card-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; background: linear-gradient(135deg, #f8f9fc, #f0f2ff); border-bottom: 1px solid #eaeaea; }
.task-card-title-row { display: flex; align-items: center; gap: 10px; }
.task-status-dot { width: 10px; height: 10px; border-radius: 50%; }
.dot-on { background: #22c55e; box-shadow: 0 0 6px rgba(34,197,94,0.5); }
.dot-off { background: #d1d5db; }
.task-card-name { font-size: 15px; font-weight: 700; color: #1a1a2e; }
.task-type-tag { background: #e0e7ff; color: #3730a3; padding: 3px 10px; border-radius: 20px; font-size: 10px; font-weight: 700; }
.task-card-actions { display: flex; gap: 8px; }
.btn-sm { border: none; padding: 6px 14px; font-size: 11px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: all 0.15s; }
.btn-sm-run { background: #dbeafe; color: #1d4ed8; }
.btn-sm-run:hover { background: #bfdbfe; }
.btn-sm-start { background: #dcfce7; color: #166534; }
.btn-sm-start:hover { background: #bbf7d0; }
.btn-sm-stop { background: #fef3c7; color: #92400e; }
.btn-sm-stop:hover { background: #fde68a; }
.btn-sm-del { background: #fef2f2; color: #991b1b; }
.btn-sm-del:hover { background: #fecaca; }
.task-card-body { padding: 14px 20px; display: flex; flex-wrap: wrap; gap: 8px 24px; }
.task-info-row { display: flex; align-items: center; gap: 4px; }
.task-info-label { font-size: 11px; color: #999; font-weight: 600; }
.task-info-value { font-size: 12px; color: #333; font-weight: 500; }
.task-result { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.task-result-err { color: #991b1b; }
</style>