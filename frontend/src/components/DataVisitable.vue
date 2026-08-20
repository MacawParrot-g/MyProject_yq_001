<script setup>
import {ref, computed, onMounted, nextTick, reactive, onUnmounted} from 'vue'
import {fetchRecordList, updateRecord, deleteRecord, fetchCountByRecorder, fetchSystemInfo, fetchEvent, fetchAllAttributions, fetchFrozen, insertRecord, enableDedup, disableDedup, fetchDedupStatus} from '../api/index.js'
import QRCode from 'qrcode'

const emit = defineEmits(['error'])

const ATTR_OPTIONS = ['appflyer', 'adjust', 'singular', 'tenjin']

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
const retestVisible = ref(false)
const retestRow = ref(null)
const retestDownloadUrl = ref('')
const retestBundleId = ref('')
const retestOriginalNum = ref(null)
const retestEventResult = ref('')
const retestNewCurrentTargetNum = ref(null)
const retestAttributions = ref([])
const retestEventId = ref(null)
const retestFrozenMsg = ref('')
const retestEventLoading = ref(false)
const retestFrozenLoading = ref(false)
const retestSaving = ref(false)
const retestSaveMsg = ref('')
const retestIsFrozen = ref('')
const retestQrCanvas = ref(null)
const retestTimerSeconds = ref(60)
const retestTimerCountdown = ref(0)
const retestTimerMsg = ref('')
let retestTimerInterval = null

const exceptionOptions = ['正常','iOS16闪退','iOS13/14/16均闪退','需要iOS18以上','地区不支持','硬件版本过低','超过10分钟0上报','越狱检测','其他','验证已解决']

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const editingUrl = ref(null)
const editForm = ref({})

const sysInfo = ref(null)
const sysLoading = ref(false)

const dedupEnabled = ref(false)
const dedupLoading = ref(false)
let sysInfoTimer = null
let dedupStatusTimer = null


const retestForm = reactive({
  exception_type: '',
  remark: '',
  recorder: localStorage.getItem('userName') || '',
  record_data: getTodayStr()
})
async function openRetest(row) {
  retestRow.value = row
  retestDownloadUrl.value = row.URL || ''
  retestBundleId.value = row.bundleId || ''
  try {
    const json = await fetchEvent(retestBundleId.value)
    if (json.success) {
      retestOriginalNum.value = json.data.currentTargetNum ?? null
    }
  } catch (e) {
    alert('服务器无响应，请联系技术人员')
  }
  retestEventResult.value = ''
  retestNewCurrentTargetNum.value = null
  retestAttributions.value = []
  retestEventId.value = null
  retestFrozenMsg.value = ''
  retestSaveMsg.value = ''
  retestIsFrozen.value = ''
  retestForm.exception_type = ''
  retestForm.remark = ''
  retestForm.recorder = localStorage.getItem('userName') || ''
  retestForm.record_data = getTodayStr()
  retestTimerMsg.value = '计时结束后内容会在这里显示'
  retestVisible.value = true
  await nextTick(() => void renderRetestQR())
}

function getTodayStr() {
  const d = new Date()
  return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate()
}

async function toggleDedup() {
  if (dedupLoading.value) return
  dedupLoading.value = true
  try {
    if (!dedupEnabled.value) {
      const name = prompt('请输入您的姓名以开启自动去重功能：')
      if (!name || !name.trim()) {
        dedupLoading.value = false
        return
      }
      const json = await enableDedup(name.trim())
      if (json.success) {
        dedupEnabled.value = true
        alert('✅ 自动去重功能已开启，操作人：' + name.trim())
      } else {
        emit('error', json.message || '开启去重失败')
      }
    } else {
      const json = await disableDedup()
      if (json.success) {
        dedupEnabled.value = false
        alert('🔒 自动去重功能已关闭')
      } else {
        emit('error', json.message || '关闭去重失败')
      }
    }
  } catch (e) {
    emit('error', '操作失败：' + e.message)
  } finally {
    dedupLoading.value = false
  }
}

async function checkDedupStatus() {
  try {
    const json = await fetchDedupStatus()
    if (json.success) {
      dedupEnabled.value = json.data?.enabled || false
    }
  } catch (e) { /* silent */ }
}

async function loadSystemInfo() {
  sysLoading.value = true
  try {
    const json = await fetchSystemInfo()
    if (json.success) {
      sysInfo.value = json.data
    } else {
      emit('error', json.message || '获取系统信息失败')
    }
  } catch (e) {
    emit('error', '获取系统信息失败：' + e.message)
  } finally {
    sysLoading.value = false
  }
}

function closeRetest() {
  retestVisible.value = false
  cancelRetestTimer()
  retestRow.value = null
}
async function renderRetestQR() {
  await nextTick()
  if (retestDownloadUrl.value && retestQrCanvas.value) {
    try {
      await QRCode.toCanvas(retestQrCanvas.value, retestDownloadUrl.value, { width: 220, margin: 2, color: { dark: '#000000', light: '#ffffff' } })
    } catch (e) { console.error('QR码生成失败:', e) }
  }
}
async function retestQueryEvent() {
  if (!retestBundleId.value) return
  retestEventLoading.value = true
  retestEventResult.value = ''
  retestNewCurrentTargetNum.value = null
  retestAttributions.value = []
  retestEventId.value = null
  retestFrozenMsg.value = ''
  try {
    const [eventJson, attrResults] = await Promise.all([
      fetchEvent(retestBundleId.value),
      fetchAllAttributions(retestBundleId.value)
    ])
    if (eventJson.success && eventJson.data) {
      const newCurrent = eventJson.data.currentTargetNum
      retestEventId.value = eventJson.data.id ?? null
      if (newCurrent !== retestOriginalNum.value) {
        retestEventResult.value = 'has_event'
        retestNewCurrentTargetNum.value = newCurrent
      } else {
        retestEventResult.value = 'no_event'
      }
    } else {
      emit('error', '事件接口返回异常：' + (eventJson.resultMsg || '未知错误'))
    }
    const found = []
    for (const { type, json } of attrResults) {
      if (json.success && json.data) {
        if (Array.isArray(json.data) && json.data.length > 0) {
          found.push(type)
        } else if (!Array.isArray(json.data) && Object.keys(json.data).length > 0) {
          found.push(type)
        }
      }
    }
    retestAttributions.value = found
  } catch (e) {
    emit('error', '事件查询失败：' + e.message)
  } finally {
    retestEventLoading.value = false
  }
}
async function retestQueryEventWithFullMsg() {
  try {
    const data = await fetchEvent(retestBundleId.value)
    if (data.success) {
      alert(JSON.stringify(data, null, 2))
    }
  } catch (e) {
    alert('服务器无响应，请联系技术人员')
  }
}
async function retestDoFrozen() {
  if (!retestEventId.value) return
  retestFrozenLoading.value = true
  try {
    const json = await fetchFrozen(retestEventId.value)
    if (json.success) {
      retestFrozenMsg.value = json.resultMsg || '操作完成'
      retestIsFrozen.value = ',已冻结'
    } else {
      emit('error', '冻结接口返回异常：' + (json.resultMsg || '未知错误'))
    }
  } catch (e) {
    emit('error', '冻结请求失败：' + e.message)
  } finally {
    retestFrozenLoading.value = false
  }
}
async function retestSaveToMySQL() {
  if (!retestForm.exception_type.trim()) { emit('error', '请选择异常类型'); return }
  retestSaving.value = true
  retestSaveMsg.value = ''
  if (retestNewCurrentTargetNum.value === 0 || retestNewCurrentTargetNum.value === null) {
    retestAttributions.value = []
    alert('无新增事件token，即使原本的token有归因，也不会被设置在字段内')
  }
  let finalRemark = retestForm.remark.trim()
  if (retestIsFrozen.value) {
    finalRemark += retestIsFrozen.value
  }
  if (retestNewCurrentTargetNum.value > 0 && retestAttributions.value.length === 0) {
    finalRemark += ',无事件归因'
  }
  try {
    const json = await insertRecord({
      URL: retestDownloadUrl.value,
      bundleId: retestBundleId.value,
      ascribe: (retestAttributions.value || []).join(';'),
      event_number: retestNewCurrentTargetNum.value,
      exception_type: retestForm.exception_type.trim(),
      record_data: retestForm.record_data,
      recorder: retestForm.recorder,
      remark: finalRemark,
      isOutput: 0
    })
    if (json.success) { retestSaveMsg.value = '✅ ' + (json.resultMsg || '入库成功') }
    else { emit('error', json.resultMsg || '入库失败') }
  } catch (e) { emit('error', '入库请求失败：' + e.message) }
  finally { retestSaving.value = false }
}

function startRetestTimer() {
  if (!retestTimerSeconds.value || retestTimerSeconds.value < 1) return
  retestTimerCountdown.value = retestTimerSeconds.value
  retestTimerMsg.value = '将在 ' + retestTimerCountdown.value + ' 秒后自动查询事件'
  retestTimerInterval = setInterval(() => {
    retestTimerCountdown.value--
    if (retestTimerCountdown.value <= 0) {
      clearInterval(retestTimerInterval)
      retestTimerInterval = null
      retestTimerCountdown.value = 0
      retestTimerMsg.value = '定时已到，正在自动查询事件...'
      retestQueryEvent().then(() => {
        retestTimerMsg.value = '✅ 定时查询已完成'
        setTimeout(() => { retestTimerMsg.value = '计时结束后内容会在这里显示' }, 5000)
      })
    }
  }, 1000)
}

function cancelRetestTimer() {
  if (retestTimerInterval) {
    clearInterval(retestTimerInterval)
    retestTimerInterval = null
  }
  retestTimerCountdown.value = 0
  retestTimerMsg.value = ''
}

async function fetchData(resetPage = false) {
      if(list.value!==null||viewType.value!==null||total.value!==0){
        list.value=[]
        viewType.value=''
        total.value=0
      }
  if (resetPage) currentPage.value = 1
  loading.value = true
  queried.value = true
  try {
    const json = await fetchRecordList(
        selectedAscribe.value || null,
        frozenOnly.value,
        currentPage.value,
        pageSize.value,
        recorderSearch.value.trim()
    )
    if (json.success) {
      list.value = json.data || []
      viewType.value = json.viewType || 'ALL'
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
  }}


function prevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchData()
  }
}

function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
    fetchData()
  }
}

function searchByRecorder() {
  fetchData(true)
}

function startEdit(row) {
  editingUrl.value = row.URL
  editForm.value = { ...row }
}

function cancelEdit() {
  editingUrl.value = null
  editForm.value = {}
}

function sup(){
  alert('谢谢你，成都。谢谢你，我的同桌：吴雨芹。谢谢我自己：完整的完成了这一切！！！')
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

async function fetchDataByName(){
  const json = await fetchCountByRecorder()
  if(json.success){
    alert("用户"+localStorage.getItem('userName') +"已入库"+json.data+"条")
  }else{
    alert("获取事件数失败，请联系工作人员")
  }
}
onMounted(() => { fetchData(); loadSystemInfo() })
onUnmounted(() => { cancelRetestTimer() })
</script>

<template>
  <div class="dv-page-container">
    <h2>数据看板</h2>
    <h5 @click="fetchDataByName()">点我查看今日入库数量</h5>
    <br/>
    <div class="dedup-toggle-bar">
      <span class="dedup-label">自动去重：</span>
      <button
          class="dedup-toggle-btn"
          :class="{ 'dedup-on': dedupEnabled, 'dedup-off': !dedupEnabled }"
          @click="toggleDedup"
          :disabled="dedupLoading"
      >
        {{ dedupLoading ? '处理中...' : (dedupEnabled ? '🟢 已开启' : '⚪ 已关闭') }}
      </button>
    </div>
    

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
      <div class="dv-filter-row">
        <span class="dv-filter-label">记录人查询：</span>
        <input
            v-model="recorderSearch"
            class="dv-recorder-input"
            placeholder="输入记录人姓名"
            @keyup.enter="searchByRecorder"
        />
        <button class="btn-refresh" @click="searchByRecorder" :disabled="loading">
          {{ loading ? '查询中...' : '查询' }}
        </button>
      </div>
    </div>

    <div v-if="queried && viewType" class="dv-view-type-tag">
      当前视图：<strong>{{ viewTypeLabel[viewType] || viewType }}</strong>
      <span v-if="total > 0"> — 共 {{ total }} 条，第 {{ currentPage }} / {{ totalPages }} 页</span>
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
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
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
            <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
            <td :title="item.URL" class="dv-cell-url">{{ item.URL }}</td>
            <td>{{ item.bundleId }}</td>
            <td>{{ item.ascribe || '-' }}</td>
            <td>{{ item.event_number }}</td>
            <td>{{ item.exception_type || '-' }}</td>
            <td>{{ item.record_data || '-' }}</td>
            <td>{{ item.recorder || '-' }}</td>
            <td :title="item.remark" class="dv-cell-remark">{{ item.remark || '-' }}</td>
            <td>
              <span :class="item.isOutput === 1 ? 'dv-tag-exported' : 'dv-tag-unexported'">
                {{ item.isOutput === 1 ? '已导出' : '未导出' }}
              </span>
            </td>
            <td class="dv-action-cell">
              <button class="dv-btn-retest" @click="openRetest(item)">重新测试</button>
            </td>
          </template>
        </tr>
        </tbody>
      </table>
    </div>

    <div v-if="total > 0" class="dv-pagination">
      <button class="dv-page-btn" :disabled="currentPage <= 1" @click="prevPage">‹ 上一页</button>
      <span class="dv-page-info">第 {{ currentPage }} / {{ totalPages }} 页，共 {{ total }} 条</span>
      <button class="dv-page-btn" :disabled="currentPage >= totalPages" @click="nextPage">下一页 ›</button>
    </div>
  </div>
  <div @click="sup" class="sup-area">点我试试</div>
  <div class="retest-overlay" v-if="retestVisible" @click.self="closeRetest">
    <div class="retest-modal">
      <div class="retest-modal-header">
        <h3>🔄 重新测试</h3>
        <button class="retest-close-btn" @click="closeRetest">✕</button>
      </div>
      <div class="retest-modal-body">
        <div class="qrcode-container" v-if="retestDownloadUrl">
          <canvas ref="retestQrCanvas"></canvas>
        </div>

        <div class="info-section" v-if="retestDownloadUrl">
          <div class="info-item">
            <div class="info-label">Download URL</div>
            <div class="info-value"><a :href="retestDownloadUrl" target="_blank">{{ retestDownloadUrl }}</a></div>
          </div>
          <div class="info-item">
            <div class="info-label">Bundle ID</div>
            <div class="info-value">{{ retestBundleId }}</div>
            <br/>
            <div class="info-label">原始CurrentTargetNum数</div>
            <div class="info-value">{{ retestOriginalNum }}</div>
          </div>
        </div>

        <button class="btn-event" v-if="retestBundleId" @click="retestQueryEvent" :disabled="retestEventLoading">
          {{ retestEventLoading ? '查询中...' : '查询事件' }}
        </button>
        <button class="btn-event" @click="retestQueryEventWithFullMsg()" :disabled="retestEventLoading" style="margin-left:5px;">
          查看JSON
        </button>

        <div class="timer-section" v-if="retestBundleId">
          <div class="timer-row">
            <label class="timer-label">定时查询</label>
            <input class="timer-input" type="number" v-model.number="retestTimerSeconds" min="1" max="3600" placeholder="秒数" />
            <button class="btn-timer" @click="startRetestTimer" :disabled="retestTimerCountdown > 0 || !retestTimerSeconds || retestTimerSeconds < 1">
              {{ retestTimerCountdown > 0 ? retestTimerCountdown + 's' : '开始定时' }}
            </button>
            <button class="btn-timer-cancel" v-if="retestTimerCountdown > 0" @click="cancelRetestTimer">取消</button>
          </div>
          <div class="timer-status" v-if="retestTimerMsg">{{ retestTimerMsg }}</div>
        </div>

        <div class="event-result no-event" v-if="retestEventResult === 'no_event'">
          <div class="event-label">查询结果</div>
          <div class="event-value">✅ 无事件</div>
          <button class="btn-frozen" @click="retestDoFrozen" :disabled="retestFrozenLoading">
            {{ retestFrozenLoading ? '冻结中...' : '冻结应用' }}
          </button>
          <div class="frozen-result" v-if="retestFrozenMsg">
            <div class="frozen-label">冻结结果</div>
            <div class="frozen-value">{{ retestFrozenMsg }}</div>
          </div>
        </div>

        <div class="event-result" v-if="retestEventResult === 'has_event'">
          <div class="event-label">最新 currentTargetNum</div>
          <div class="event-value highlight">{{ retestNewCurrentTargetNum }}</div>
          <div class="event-compare">
            原始值：<span class="diff">{{ retestOriginalNum }}</span> → 最新值：<span class="diff">{{ retestNewCurrentTargetNum }}</span>
          </div>
          <div class="attribution-tags" v-if="retestAttributions.length > 0">
            <span class="attr-tag" v-for="attr in retestAttributions" :key="attr" :class="attr">{{ attr }}</span>
          </div>
          <div class="no-attribution" v-if="retestAttributions.length === 0">⚠️ 无归因</div>
        </div>

        <div class="form-section" v-if="retestDownloadUrl && retestEventResult">
          <h4>填写入库信息（直接写入MySQL）</h4>
          <div class="form-group">
            <label>异常类型</label>
            <select v-model="retestForm.exception_type">
              <option value="" disabled>请选择异常类型</option>
              <option v-for="opt in exceptionOptions" :key="opt" :value="opt">{{ opt }}</option>
            </select>
          </div>
          <div class="form-group">
            <label>备注</label>
            <textarea v-model="retestForm.remark" placeholder="请输入备注信息"></textarea>
          </div>
          <div style="display:flex; gap:12px;">
            <div class="form-group" style="flex:1">
              <label>记录人</label>
              <input v-model="retestForm.recorder" />
            </div>
            <div class="form-group" style="flex:1">
              <label>记录日期</label>
              <input v-model="retestForm.record_data" />
            </div>
          </div>
          <button class="btn-save" @click="retestSaveToMySQL" :disabled="retestSaving">
            {{ retestSaving ? '入库中...' : '直接入库MySQL' }}
          </button>
          <div class="save-success" v-if="retestSaveMsg">{{ retestSaveMsg }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

.dedup-toggle-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #f8f9fa;
  border-radius: 10px;
  padding: 12px 18px;
  margin-bottom: 18px;
  border: 1px solid #e0e0e0;
}
.dedup-label {
  font-size: 14px;
  font-weight: 700;
  color: #333;
}
.dedup-toggle-btn {
  border: none;
  padding: 8px 24px;
  font-size: 14px;
  font-weight: 700;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  letter-spacing: 1px;
}
.dedup-toggle-btn.dedup-on {
  background: linear-gradient(135deg, #43e97b, #38f9d7);
  color: #fff;
  box-shadow: 0 4px 14px rgba(67, 233, 123, 0.4);
}
.dedup-toggle-btn.dedup-off {
  background: #e0e0e0;
  color: #888;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.dedup-toggle-btn:hover {
  transform: translateY(-2px);
}
.dedup-toggle-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
.dv-page-container {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 16px;
  box-sizing: border-box;
}
.sys-monitor-card {
  text-align: left;
  background: #f8f9fa;
  border-radius: 10px;
  padding: 16px 18px;
  margin-bottom: 18px;
  border: 1px solid #e0e0e0;
}
.sys-monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}
.sys-monitor-title {
  font-size: 15px;
  font-weight: 700;
  color: #333;
}
.sys-refresh-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  padding: 5px 14px;
  font-size: 12px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}
.sys-refresh-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.sys-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 14px;
}
.sys-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sys-label {
  font-size: 11px;
  color: #888;
  font-weight: 600;
}
.sys-value {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}
.sys-warn { color: #e74c3c; font-weight: 700; }
.sys-progress-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.sys-progress-label {
  font-size: 12px;
  color: #555;
  font-weight: 600;
  white-space: nowrap;
  width: 70px;
}
.sys-progress-track {
  flex: 1;
  height: 10px;
  background: #e0e0e0;
  border-radius: 5px;
  overflow: hidden;
}
.sys-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #43e97b, #38f9d7);
  border-radius: 5px;
  transition: width 0.5s ease;
}
.sys-progress-fill.sys-progress-danger {
  background: linear-gradient(90deg, #ff6b6b, #ee5a24);
}
.sys-progress-text {
  font-size: 12px;
  font-weight: 700;
  color: #333;
  width: 50px;
  text-align: right;
}
.sys-loading {
  text-align: center;
  color: #888;
  font-size: 13px;
  padding: 12px 0;
}
.sup-area {
  color: white;
  font-size: 10px;
  background-size: 15px;
  width: 20px;
  height: 15px;
}
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

.dv-recorder-input {
  padding: 6px 14px;
  font-size: 13px;
  border: 2px solid #ddd;
  border-radius: 8px;
  outline: none;
  width: 200px;
  transition: border-color 0.2s;
}
.dv-recorder-input:focus { border-color: #667eea; }

.btn-refresh {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  padding: 7px 20px;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.btn-refresh:disabled { opacity: 0.6; cursor: not-allowed; }

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
  min-width: 1300px;
}
.dv-table th {
  background: #667eea;
  color: #fff;
  padding: 10px 12px;
  white-space: nowrap;
}
.dv-table td {
  padding: 8px 12px;
  border-bottom: 1px solid #eee;
  color: #333;
  vertical-align: middle;
}
.dv-table tr:hover td { background: #f5f7ff; }
.dv-table tr:last-child td { border-bottom: none; }

.dv-cell-url {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dv-cell-remark {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dv-action-cell {
  white-space: nowrap;
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
  margin-right: 6px;
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

.dv-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
  padding: 12px 0;
}
.dv-page-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  padding: 7px 18px;
  font-size: 13px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.dv-page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.dv-page-info {
  font-size: 13px;
  color: #555;
  font-weight: 600;
}
</style>
