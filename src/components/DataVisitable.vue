<script setup>
import { ref, onMounted } from 'vue'
import { fetchRecordList, updateRecord, deleteRecord } from '../api/index.js'

const emit = defineEmits(['error'])

const ATTR_OPTIONS = ['appflyer', 'adjust', 'singular', 'tenjin']

const selectedAscribe = ref('')
const frozenOnly = ref(false)
const list = ref([])
const loading = ref(false)
const queried = ref(false)
const viewType = ref('ALL')

const editingUrl = ref(null)
const editForm = ref({})

async function fetchData() {
  loading.value = true
  queried.value = true
  if(list.value!==null){
    list.value=[];
  }
  try {
    const json = await fetchRecordList(selectedAscribe.value || null, frozenOnly.value)
    if (json.success) {
      list.value = json.data || []
      viewType.value = json.viewType || 'ALL'
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

function startEdit(row) {
  editingUrl.value = row.URL
  editForm.value = { ...row }
}

function cancelEdit() {
  editingUrl.value = null
  editForm.value = {}
}

async function saveEdit() {
  try {
    const json = await updateRecord(editForm.value)
    if (json.success) {
      editingUrl.value = null
      await fetchData()
    } else {
      emit('error', json.message || '更新失败')
    }
  } catch (e) {
    emit('error', '更新请求失败：' + e.message)
  }
}

async function handleDelete(row) {
  if (!confirm('确定要删除这条记录吗？\nURL: ' + row.URL)) return
  try {
    const json = await deleteRecord(row.URL)
    if (json.success) {
      await fetchData()
    } else {
      emit('error', json.message || '删除失败')
    }
  } catch (e) {
    emit('error', '删除请求失败：' + e.message)
  }
}

const viewTypeLabel = {
  ALL: '全部数据',
  APPFLYER: 'appflyer',
  ADJUST: 'adjust',
  SINGULAR: 'singular',
  TENJIN: 'tenjin',
  FROZEN: '已冻结数据',
  APPFLYER_FROZEN: 'appflyer · 已冻结',
  ADJUST_FROZEN: 'adjust · 已冻结',
  SINGULAR_FROZEN: 'singular · 已冻结',
  TENJIN_FROZEN: 'tenjin · 已冻结'
}

onMounted(() => { fetchData() })
</script>

<template>
  <div>
    <h2>数据看板</h2>

    <div class="dv-filter">
      <div class="dv-filter-row">
        <span class="dv-filter-label">归因筛选：</span>
        <div class="dv-radio-group">
          <label class="dv-radio dv-radio-all" :class="{ active: selectedAscribe === '' }">
            <input type="radio" name="ascribe" value="" :checked="selectedAscribe === ''" @change="selectedAscribe = ''" />
            <span>全部</span>
          </label>
          <label v-for="opt in ATTR_OPTIONS" :key="opt" class="dv-radio" :class="[opt, { active: selectedAscribe === opt }]">
            <input type="radio" name="ascribe" :value="opt" :checked="selectedAscribe === opt" @change="selectedAscribe = opt" />
            <span>{{ opt }}</span>
          </label>
        </div>
      </div>
      <div class="dv-filter-row">
        <label class="dv-checkbox frozen-check" :class="{ active: frozenOnly }">
          <input type="checkbox" v-model="frozenOnly" />
          <span>仅查看已冻结数据（remark含"已冻结"）</span>
        </label>
      </div>
      <button class="btn-refresh" @click="fetchData" :disabled="loading">
        {{ loading ? '查询中...' : '查询' }}
      </button>
    </div>

    <div v-if="queried && viewType" class="dv-view-type-tag">
      当前视图：<strong>{{ viewTypeLabel[viewType] || viewType }}</strong>
      <span v-if="list.length > 0"> — 共 {{ list.length }} 条</span>
    </div>

    <div v-if="loading && !queried" class="loading">正在查询数据库...</div>

    <div v-if="!loading && queried && list.length === 0" class="dv-empty">
      <div class="empty-icon">📭</div>
      <div>没有符合条件的数据</div>
    </div>

    <div v-if="list.length > 0" class="dv-table-wrapper">
      <table class="dv-table">
        <thead>
        <tr>
          <th>#</th>
          <th>URL</th>
          <th>Bundle ID</th>
          <th>归因</th>
          <th>事件数</th>
          <th>异常类型</th>
          <th>记录日期</th>
          <th>记录人</th>
          <th>备注</th>
          <th>导出</th>
          <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(item, index) in list" :key="item.URL">
          <template v-if="editingUrl === item.URL">
            <td>{{ index + 1 }}</td>
            <td :title="item.URL" class="dv-cell-url">{{ item.URL }}</td>
            <td><input v-model="editForm.bundleId" class="dv-edit-input" /></td>
            <td><input v-model="editForm.ascribe" class="dv-edit-input" /></td>
            <td><input v-model="editForm.event_number" type="number" class="dv-edit-input dv-edit-num" /></td>
            <td><input v-model="editForm.exception_type" class="dv-edit-input" /></td>
            <td><input v-model="editForm.record_data" class="dv-edit-input" /></td>
            <td><input v-model="editForm.recorder" class="dv-edit-input" /></td>
            <td><textarea v-model="editForm.remark" class="dv-edit-input dv-edit-textarea"></textarea></td>
            <td>{{ editForm.isOutput === 1 ? '已导出' : '未导出' }}</td>
            <td class="dv-action-cell">
              <button class="dv-btn-save" @click="saveEdit">保存</button>
              <button class="dv-btn-cancel" @click="cancelEdit">取消</button>
            </td>
          </template>
          <template v-else>
            <td>{{ index + 1 }}</td>
            <td :title="item.URL" class="dv-cell-url">{{ item.URL }}</td>
            <td>{{ item.bundleId }}</td>
            <td>{{ item.ascribe || '-' }}</td>
            <td>{{ item.event_number }}</td>
            <td>{{ item.exception_type|| '-' }}</td>
            <td>{{ item.record_data || '-' }}</td>
            <td>{{ item.recorder || '-' }}</td>
            <td :title="item.remark" class="dv-cell-remark">{{ item.remark || '-' }}</td>
            <td>
                <span :class="item.isOutput === 1 ? 'dv-tag-exported' : 'dv-tag-unexported'">
                  {{ item.isOutput === 1 ? '已导出' : '未导出' }}
                </span>
            </td>
            <td class="dv-action-cell">
              <button class="dv-btn-edit" @click="startEdit(item)">编辑</button>
              <button class="dv-btn-del" @click="handleDelete(item)">删除</button>
            </td>
          </template>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.dv-filter {
  text-align: left;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px 18px;
  margin-bottom: 16px;
}
.dv-filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
.dv-filter-label {
  font-weight: 600;
  color: #555;
  font-size: 13px;
  white-space: nowrap;
}
.dv-radio-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.dv-radio {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  color: #fff;
  transition: all 0.2s;
  opacity: 0.4;
  border: 2px solid transparent;
}
.dv-radio input { display: none; }
.dv-radio.active { opacity: 1; box-shadow: 0 2px 8px rgba(0,0,0,0.2); }
.dv-radio-all { background: #607D8B; }
.dv-radio.appflyer { background: #4CAF50; }
.dv-radio.adjust { background: #FF9800; }
.dv-radio.singular { background: #2196F3; }
.dv-radio.tenjin { background: #9C27B0; }

.frozen-check {
  background: #e3f2fd;
  color: #1976d2;
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 13px;
  opacity: 0.5;
  transition: all 0.2s;
  border: 2px solid transparent;
}
.frozen-check.active { opacity: 1; border-color: #1976d2; }
.frozen-check input { display: inline; margin-right: 6px; }

.dv-view-type-tag {
  text-align: left;
  font-size: 13px;
  color: #667eea;
  margin-bottom: 12px;
  padding: 8px 14px;
  background: #f0f4ff;
  border-radius: 8px;
}

.dv-empty {
  text-align: center;
  padding: 50px 20px;
  color: #aaa;
  font-size: 15px;
}
.dv-empty .empty-icon { font-size: 48px; margin-bottom: 12px; }

.dv-table-wrapper {
  overflow-x: auto;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}
.dv-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  text-align: left;
  min-width: 900px;
}
.dv-table th {
  background: #667eea;
  color: #fff;
  padding: 10px;
  white-space: nowrap;
}
.dv-table td {
  padding: 8px 10px;
  border-bottom: 1px solid #eee;
  color: #333;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dv-table tr:hover td { background: #f5f7ff; }
.dv-table tr:last-child td { border-bottom: none; }

.dv-cell-url { max-width: 200px; overflow: hidden; text-overflow: ellipsis; }
.dv-cell-remark { max-width: 140px; overflow: hidden; text-overflow: ellipsis; }

.dv-action-cell {
  white-space: nowrap;
  display: flex;
  gap: 6px;
}

.dv-edit-input {
  width: 100%;
  padding: 4px 8px;
  font-size: 12px;
  border: 2px solid #667eea;
  border-radius: 4px;
  outline: none;
  background: #fff;
}
.dv-edit-num { width: 60px; }
.dv-edit-textarea {
  resize: vertical;
  min-height: 40px;
  font-family: inherit;
}

.dv-btn-edit, .dv-btn-save {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
  color: #fff;
  border: none;
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
}
.dv-btn-del, .dv-btn-cancel {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: #fff;
  border: none;
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 600;
}
.dv-btn-edit:hover, .dv-btn-save:hover, .dv-btn-del:hover, .dv-btn-cancel:hover {
  opacity: 0.85;
}

.dv-tag-exported {
  background: #e8f5e9;
  color: #2e7d32;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
.dv-tag-unexported {
  background: #fff3e0;
  color: #e65100;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}
</style>
