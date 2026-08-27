<template>
  <div class="grade-page">
    <div class="grade-header">
      <h2>📝 应用评级管理</h2>
      <span class="grade-header-sub">管理所有应用的评级与测试要求</span>
    </div>

    <div class="grade-filter">
      <div class="filter-row">
        <div class="filter-field">
          <label>评级等级</label>
          <select v-model="filters.grade" class="filter-input">
            <option value="">全部</option>
            <option v-for="g in ['A','B','C','D']" :key="g" :value="g">{{ g }}级</option>
          </select>
        </div>
        <div class="filter-field">
          <label>记录人</label>
          <input v-model="filters.recorder" class="filter-input" placeholder="输入记录人姓名" @keyup.enter="doSearch(true)" />
        </div>
        <div class="filter-field">
          <label>备注关键字</label>
          <input v-model="filters.keyword" class="filter-input" placeholder="模糊搜索备注" @keyup.enter="doSearch(true)" />
        </div>
        <div class="filter-actions">
          <button class="btn-search" @click="doSearch(true)" :disabled="loading">
            {{ loading ? '查询中...' : '🔍 查询' }}
          </button>
          <button class="btn-reset" @click="resetFilters">↻ 重置</button>
        </div>
      </div>
    </div>

    <div v-if="queried" class="result-bar">
      <span>共 <strong>{{ total }}</strong> 条评级记录 · 第 {{ currentPage }}/{{ totalPages }} 页</span>
    </div>

    <div v-if="loading && list.length === 0" class="state-block">
      <div class="state-spinner"></div>
      <div class="state-text">正在查询...</div>
    </div>

    <div v-if="!loading && queried && list.length === 0" class="state-block">
      <div class="state-icon">📭</div>
      <div class="state-text">没有找到评级记录</div>
    </div>

    <div v-if="list.length > 0" class="grade-table-wrap">
      <table class="grade-table">
        <thead>
        <tr>
          <th>#</th>
          <th>URL</th>
          <th>评级</th>
          <th>记录人</th>
          <th>备注</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(item, idx) in list" :key="item.url">
          <td>{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
          <td class="cell-url" :title="item.url">{{ item.url }}</td>
          <td>
            <span v-if="editingUrl !== item.url" class="grade-badge" :class="'grade-' + item.grade.toLowerCase()">{{ item.grade }}级</span>
            <select v-else v-model="editForm.grade" class="edit-select">
              <option v-for="g in ['A','B','C','D']" :key="g" :value="g">{{ g }}级</option>
            </select>
          </td>
          <td>{{ item.recorder || '-' }}</td>
          <td class="cell-remark">
            <span v-if="editingUrl !== item.url">{{ item.remark || '-' }}</span>
            <input v-else v-model="editForm.remark" class="edit-input" placeholder="备注" />
          </td>
          <td class="cell-actions">
            <template v-if="editingUrl !== item.url">
              <button class="btn-sm btn-edit" @click="startEdit(item)">✏️ 编辑</button>
              <button class="btn-sm btn-del" @click="handleDelete(item)" :disabled="editingUrl !== null">🗑 删除</button>
            </template>
            <template v-else>
              <button class="btn-sm btn-save" @click="handleUpdate(item.url)" :disabled="saving">
                {{ saving ? '保存中...' : '💾 保存' }}
              </button>
              <button class="btn-sm btn-cancel" @click="cancelEdit">取消</button>
            </template>
          </td>
        </tr>
        </tbody>
      </table>
    </div>

    <div v-if="total > 1" class="pagination">
      <button class="page-btn" :disabled="currentPage <= 1" @click="prevPage">‹ 上一页</button>
      <span class="page-info">第 {{ currentPage }} / {{ totalPages }} 页</span>
      <button class="page-btn" :disabled="currentPage >= totalPages" @click="nextPage">下一页 ›</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { gradeManageSearch, gradeManageDelete, gradeManageUpdate } from '../api/index.js'

const emit = defineEmits(['error'])

const filters = reactive({ grade: '', recorder: '', keyword: '' })
const list = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(15)
const loading = ref(false)
const queried = ref(false)

const editingUrl = ref(null)
const editForm = reactive({ grade: '', remark: '' })
const saving = ref(false)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

async function doSearch(resetPage = false) {
  if (resetPage) currentPage.value = 1
  loading.value = true
  queried.value = true
  try {
    const json = await gradeManageSearch({
      grade: filters.grade || null,
      recorder: filters.recorder.trim() || null,
      keyword: filters.keyword.trim() || null,
      page: currentPage.value,
      size: pageSize.value
    })
    if (json.success && json.data) {
      list.value = json.data.list || []
      total.value = json.data.total || 0
    } else {
      emit('error', json.message || '查询失败')
      list.value = []
      total.value = 0
    }
  } catch (e) {
    emit('error', '查询请求失败：' + e.message)
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.grade = ''
  filters.recorder = ''
  filters.keyword = ''
  doSearch(true)
}

function startEdit(item) {
  editingUrl.value = item.url
  editForm.grade = item.grade
  editForm.remark = item.remark || ''
}

function cancelEdit() {
  editingUrl.value = null
  editForm.grade = ''
  editForm.remark = ''
}

async function handleUpdate(url) {
  if (!editForm.grade) {
    emit('error', '请选择评级等级')
    return
  }
  saving.value = true
  try {
    const json = await gradeManageUpdate(url, editForm.grade, editForm.remark.trim())
    if (json.success) {
      editingUrl.value = null
      await doSearch()
    } else {
      emit('error', json.message || '更新失败')
    }
  } catch (e) {
    emit('error', '更新请求失败：' + e.message)
  } finally {
    saving.value = false
  }
}

async function handleDelete(item) {
  if (!confirm(`确定要删除该评级记录吗？\nURL: ${item.url}\n评级: ${item.grade}级\n\n此操作将同时从数据库和Redis缓存中删除。`)) return
  try {
    const json = await gradeManageDelete(item.url)
    if (json.success) {
      await doSearch()
    } else {
      emit('error', json.message || '删除失败')
    }
  } catch (e) {
    emit('error', '删除请求失败：' + e.message)
  }
}

function prevPage() { if (currentPage.value > 1) { currentPage.value--; doSearch() } }
function nextPage() { if (currentPage.value < totalPages.value) { currentPage.value++; doSearch() } }

onMounted(() => { doSearch(true) })
</script>

<style scoped>
.grade-page { max-width: 1400px; margin: 0 auto; padding: 0 0 40px; }
.grade-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; padding-bottom: 16px; border-bottom: 2px solid #f0f0f0; }
.grade-header h2 { margin: 0; font-size: 22px; color: #1a1a2e; font-weight: 700; }
.grade-header-sub { font-size: 13px; color: #999; }

.grade-filter { background: #fff; border: 1px solid #e8eaef; border-radius: 12px; padding: 18px 20px; margin-bottom: 20px; }
.filter-row { display: flex; gap: 16px; align-items: flex-end; flex-wrap: wrap; }
.filter-field { display: flex; flex-direction: column; gap: 4px; min-width: 160px; }
.filter-field label { font-size: 12px; font-weight: 600; color: #64748b; }
.filter-input { padding: 8px 12px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; color: #1a1a2e; background: #fff; transition: border-color 0.2s; }
.filter-input:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }
.filter-actions { display: flex; gap: 8px; align-items: flex-end; }
.btn-search { padding: 8px 20px; background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; border: none; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-search:hover:not(:disabled) { box-shadow: 0 2px 10px rgba(99,102,241,0.3); }
.btn-search:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-reset { padding: 8px 16px; background: #f1f5f9; color: #64748b; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 13px; cursor: pointer; }
.btn-reset:hover { background: #e2e8f0; }

.result-bar { font-size: 13px; color: #64748b; margin-bottom: 14px; padding: 0 4px; }
.result-bar strong { color: #6366f1; font-size: 16px; }

.state-block { display: flex; flex-direction: column; align-items: center; padding: 50px 20px; }
.state-spinner { width: 32px; height: 32px; border: 3px solid #e2e8f0; border-top-color: #6366f1; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.state-text { margin-top: 12px; font-size: 14px; color: #94a3b8; }
.state-icon { font-size: 48px; margin-bottom: 12px; }

.grade-table-wrap { background: #fff; border: 1px solid #e8eaef; border-radius: 12px; overflow: hidden; }
.grade-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.grade-table thead tr { background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%); color: #fff; }
.grade-table th { padding: 10px 14px; text-align: left; font-weight: 600; font-size: 12px; letter-spacing: 0.3px; }
.grade-table td { padding: 10px 14px; border-bottom: 1px solid #f1f5f9; color: #374151; vertical-align: middle; }
.grade-table tbody tr:hover { background: #f8f9fc; }
.grade-table tbody tr:last-child td { border-bottom: none; }
.cell-url { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 12px; color: #64748b; }
.cell-remark { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cell-actions { white-space: nowrap; }

.grade-badge { display: inline-block; padding: 3px 12px; border-radius: 6px; font-size: 12px; font-weight: 700; letter-spacing: 0.5px; }
.grade-a { background: #dcfce7; color: #166534; }
.grade-b { background: #dbeafe; color: #1e40af; }
.grade-c { background: #fef3c7; color: #92400e; }
.grade-d { background: #fee2e2; color: #991b1b; }

.edit-select { padding: 5px 8px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 12px; }
.edit-input { padding: 5px 8px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 12px; width: 160px; }

.btn-sm { padding: 5px 10px; border: none; border-radius: 6px; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.15s; margin-right: 4px; }
.btn-sm:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-edit { background: #eef2ff; color: #4338ca; }
.btn-edit:hover { background: #e0e7ff; }
.btn-del { background: #fef2f2; color: #dc2626; }
.btn-del:hover { background: #fee2e2; }
.btn-save { background: #dcfce7; color: #166534; }
.btn-save:hover { background: #bbf7d0; }
.btn-cancel { background: #f1f5f9; color: #64748b; }
.btn-cancel:hover { background: #e2e8f0; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 20px; }
.page-btn { padding: 8px 16px; border: 1px solid #e2e8f0; border-radius: 8px; background: #fff; font-size: 13px; cursor: pointer; color: #374151; transition: all 0.2s; }
.page-btn:hover:not(:disabled) { background: #f8f9fc; border-color: #6366f1; color: #6366f1; }
.page-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.page-info { font-size: 13px; color: #64748b; }
</style>