<template>
  <div class="auto-mode">
    <div class="top-bar">
      <div v-if="duplicateTip" class="duplicate-tip">⚠️ {{ duplicateTip }}</div>
      <div v-if="polling" class="polling-section">
        <span class="polling-status">🔄 正在每10秒自动获取下载任务...（第{{ pollCount }}次）</span>
        <button class="btn-timer-cancel" @click="stopPolling">停止轮询</button>
      </div>
      <div class="top-actions">
        <button class="btn-refresh" @click="comfirm()" :disabled="loading">
          {{ loading ? '加载中...' : '刷新数据' }}
        </button>
      </div>
    </div>

    <div v-if="loading && !downloadUrl" class="loading">正在获取数据...</div>

    、<div class="empty-placeholder" v-if="!downloadUrl && !loading">
    <div class="empty-icon">404 NO FOUND</div>
    <div class="empty-text">当前没有任何测试条目，请点击刷新按钮刷新第一条数据</div>
  </div>

    <div class="qr-info-section" v-if="downloadUrl">
      <div class="qr-card">
        <div class="qr-card-header">
          <span class="qr-card-icon">📱</span>
          <span class="qr-card-title">请扫描二维码下载APP进行测试</span>
        </div>
        <div class="qrcode-container">
          <canvas ref="qrCanvas"></canvas>
        </div>
      </div>
      <div class="side-info">
        <div class="info-card">
          <div class="info-card-label">Bundle ID</div>
          <div class="info-card-value">{{ bundleId }}</div>
        </div>
        <div class="info-card">
          <div class="info-card-label">原始CurrentTargetNum数</div>
          <div class="info-card-value">{{ originalCurrentTargetNum }}</div>
        </div>

        <div class="info-card grade-card">
          <div class="info-card-label">应用评级</div>
          <div class="info-card-value" v-if="gradeLoading" style="font-size:12px;color:#94a3b8">加载中...</div>
          <template v-else-if="gradeData">
            <div class="grade-badge" :class="'grade-' + gradeData.grade.toLowerCase()">{{ gradeData.grade }}级</div>
            <div class="grade-remark" v-if="gradeData.remark">测试要求：{{ gradeData.remark }}</div>
          </template>
          <div v-else class="grade-none">无评级</div>
        </div>

      </div>
    </div>

    <div class="action-row" v-if="bundleId">
      <button class="btn-event" @click="queryEvent" :disabled="eventLoading">
        {{ eventLoading ? '查询中...' : '查询事件' }}
      </button>
      <button class="btn-event" @click="queryEventWithFullMsg()" :disabled="eventLoading">
        查看JSON
      </button>
    </div>

    <div class="timer-panel" v-if="bundleId">
      <div class="timer-left">
        <div class="timer-row">
          <label class="timer-label">定时查询</label>
          <input class="timer-input" type="number" v-model.number="timerSeconds" min="1" max="3600" placeholder="秒数" />
          <button class="btn-timer" @click="startTimer" :disabled="timerCountdown > 0 || !timerSeconds || timerSeconds < 1">
            {{ timerCountdown > 0 ? timerCountdown + 's' : '开始定时' }}
          </button>
          <button class="btn-timer-cancel" v-if="timerCountdown > 0" @click="cancelTimer">取消</button>
        </div>
        <div class="timer-status" v-if="timerMsg">{{ timerMsg }}</div>
      </div>
      <div class="timer-divider"></div>
      <div class="timer-right">
        <template v-if="eventResult">
          <div class="event-result no-event" v-if="eventResult === 'no_event'">
            <div class="event-label">查询结果</div>
            <div class="event-value">✅ 无事件</div>
            <button class="btn-frozen" @click="doFrozen" :disabled="frozenLoading">
              {{ frozenLoading ? '冻结中...' : '冻结应用' }}
            </button>
            <div class="frozen-result" v-if="frozenMsg">
              <div class="frozen-label">冻结结果</div>
              <div class="frozen-value">{{ frozenMsg }}</div>
            </div>
          </div>
          <div class="event-result" v-if="eventResult === 'has_event'">
            <div class="event-label">最新 currentTargetNum</div>
            <div class="event-value highlight">{{ newCurrentTargetNum }}</div>
            <div class="event-compare">
              原始值：<span class="diff">{{ originalCurrentTargetNum }}</span> → 最新值：<span class="diff">{{ newCurrentTargetNum }}</span>
            </div>
            <div class="attribution-tags" v-if="attributions.length > 0">
              <span class="attr-tag" v-for="attr in attributions" :key="attr" :class="attr">{{ attr }}</span>
            </div>
            <div class="no-attribution" v-if="attributions.length === 0">⚠️ 无归因</div>
          </div>
        </template>
        <div class="event-placeholder" v-else>
          <span class="event-placeholder-icon">🔍</span>
          <span>查询的事件会在这里显示</span>
        </div>
      </div>
    </div>

    <div class="modal-overlay" v-if="showRetestModal" @click.self="showRetestModal = false">
      <div class="modal-box">
        <div class="modal-title">库存已耗尽</div>
        <div class="modal-desc">当前没有可用的下载任务，请选择操作：</div>
        <div class="modal-actions">
          <button class="btn-modal btn-poll" @click="showRetestModal = false; startPolling()">继续轮询</button>
          <button class="btn-modal btn-retest" @click="retestFlow" :disabled="retestLoading">
            {{ retestLoading ? '获取中...' : '复测' }}
          </button>
          <button class="btn-modal btn-cancel" @click="showRetestModal = false">取消</button>
        </div>
      </div>
    </div>

    <div class="form-section" v-if="downloadUrl && eventResult">
      <h4>填写入库信息（直接写入数据库）</h4>
      <div v-if="urlAlreadyGraded" class="graded-warning">⚠️ 该应用已存在评级记录，<strong>无法评分</strong></div>
      <div class="form-group">
        <label>异常类型</label>
        <select v-model="form.exception_type">
          <option value="" disabled>请选择异常类型</option>
          <option v-for="opt in exceptionOptions" :key="opt" :value="opt">{{ opt }}</option>
        </select>
      </div>
      <div class="form-group">
        <label>备注</label>
        <textarea v-model="form.remark" placeholder="手动输入(如果用了模板此处禁填)"></textarea>
        <br/>
        <select v-model="form.remark">
          <option value="" disabled>模板(可选)</option>
          <option v-for="opt in template" :key="opt" :value="opt">{{ opt }}</option>
        </select>
      </div>
      <div style="display:flex; gap:12px;">
        <div class="form-group" style="flex:1">
          <label>记录人</label>
          <input v-model="form.recorder" />
        </div>
        <div class="form-group" style="flex:1">
          <label>记录日期</label>
          <input type="date" v-model="form.record_data" />
        </div>
      </div>
      <div class="form-btn-row">
        <button class="btn-save" @click="saveToMySQL" :disabled="saving">
          {{ saving ? '入库中...' : '直接入库' }}
        </button>
        <button class="btn-grade" @click="showGradeModal = true" :disabled="urlAlreadyGraded">
          📝 应用评分
        </button>
      </div>
      <div class="save-success" v-if="saveMsg">{{ saveMsg }}</div>
    </div>

    <div class="modal-overlay" v-if="showGradeModal" @click.self="showGradeModal = false">
      <div class="modal-box grade-modal-box">
        <div class="modal-title">📝 应用评级</div>
        <div class="grade-modal-url" :title="downloadUrl">{{ downloadUrl }}</div>
        <div class="grade-form-group">
          <label>评级等级</label>
          <div class="grade-options">
            <button v-for="g in ['A','B','C','D']" :key="g" class="grade-option-btn" :class="['grade-' + g.toLowerCase(), { active: gradeForm.grade === g }]" @click="gradeForm.grade = g">{{ g }}级</button>
          </div>
        </div>
        <div class="grade-form-group">
          <label>测试要求 / 备注</label>
          <textarea v-model="gradeForm.remark" placeholder="输入该应用的测试要求..." rows="3"></textarea>
        </div>
        <div class="grade-form-group">
          <label>评分人</label>
          <input :value="currentUserName" disabled class="grade-disabled-input" />
        </div>
        <div class="grade-modal-actions">
          <button class="btn-modal btn-cancel" @click="showGradeModal = false">取消</button>
          <button class="btn-modal btn-grade-save" @click="submitGrade" :disabled="gradeSaving">
            {{ gradeSaving ? '保存中...' : '✅ 保存评级' }}
          </button>
        </div>
        <div v-if="gradeMsg" class="feedback" :class="{ 'feedback-ok': gradeMsg.startsWith('✅'), 'feedback-err': gradeMsg.startsWith('❌') }" style="margin-top:10px">
          {{ gradeMsg }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>.auto-mode {
  max-width: 860px;
  margin: 0 auto;
}
.top-bar {
  margin-bottom: 20px;
}
.top-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.empty-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  background: var(--bg-card);
  border: 2px dashed var(--border-color);
  border-radius: 16px;
  margin-bottom: 20px;
}
.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}
.empty-text {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
  text-align: center;
  line-height: 1.6;
}

.polling-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
  padding: 12px 18px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  border-radius: 12px;
}
.polling-status {
  color: #92400e;
  font-weight: 600;
  font-size: 13px;
}
.qr-info-section {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 20px;
  margin-bottom: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  box-shadow: var(--shadow-sm);
}
.qr-card {
  text-align: center;
}
.qr-card-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 16px;
}
.qr-card-icon {
  font-size: 20px;
}
.qr-card-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
}
.side-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  justify-content: center;
}
.info-card {
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 16px 20px;
  background: var(--bg-primary);
}
.info-card-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}
.info-card-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  word-break: break-all;
}
.action-row {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.timer-panel {
  display: flex;
  align-items: center;
  gap: 0;
  background: #fdf2f8;
  border: 1px solid #fbcfe8;
  border-radius: 14px;
  padding: 16px 20px;
  margin-bottom: 16px;
  min-height: 80px;
}
.timer-left {
  flex-shrink: 0;
  min-width: 200px;
}
.timer-divider {
  width: 1px;
  align-self: stretch;
  background: #f9a8d4;
  margin: 0 18px;
  flex-shrink: 0;
}
.timer-right {
  flex: 1;
  min-width: 0;
}
.event-placeholder {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
  font-size: 13px;
  font-weight: 500;
}

/* ========== 应用评级卡片 ========== */
.grade-card { position: relative; }
.grade-badge { display: inline-block; padding: 4px 16px; border-radius: 8px; font-size: 16px; font-weight: 700; letter-spacing: 1px; }
.grade-a { background: #dcfce7; color: #166534; }
.grade-b { background: #dbeafe; color: #1e40af; }
.grade-c { background: #fef3c7; color: #92400e; }
.grade-d { background: #fee2e2; color: #991b1b; }
.grade-none { font-size: 13px; color: #94a3b8; font-weight: 500; }
.grade-remark { margin-top: 8px; font-size: 12px; color: #64748b; line-height: 1.6; word-break: break-all; }

/* ========== 已评级警告 ========== */
.graded-warning { background: #fef3c7; border: 1px solid #fde68a; border-radius: 8px; padding: 10px 14px; margin-bottom: 14px; font-size: 13px; color: #92400e; }

/* ========== 入库+评分按钮行 ========== */
.form-btn-row { display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap; }
.btn-grade { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; border: none; padding: 10px 20px; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-grade:hover:not(:disabled) { box-shadow: 0 4px 16px rgba(245,158,11,0.35); transform: translateY(-1px); }
.btn-grade:disabled { opacity: 0.5; cursor: not-allowed; }

/* ========== 评分弹窗 ========== */
.grade-modal-box { max-width: 480px; }
.grade-modal-url { font-size: 11px; color: #64748b; background: #f1f5f9; padding: 8px 12px; border-radius: 8px; margin: 10px 0 16px; word-break: break-all; line-height: 1.5; max-height: 60px; overflow-y: auto; }
.grade-form-group { margin-bottom: 14px; }
.grade-form-group label { display: block; font-size: 13px; font-weight: 600; color: #374151; margin-bottom: 6px; }
.grade-options { display: flex; gap: 10px; }
.grade-option-btn { flex: 1; padding: 10px 0; border: 2px solid #e5e7eb; border-radius: 10px; font-size: 15px; font-weight: 700; cursor: pointer; background: #fff; transition: all 0.2s; }
.grade-option-btn:hover { border-color: #d1d5db; }
.grade-option-btn.active.grade-a { border-color: #16a34a; background: #dcfce7; color: #166534; box-shadow: 0 2px 8px rgba(22,163,74,0.2); }
.grade-option-btn.active.grade-b { border-color: #2563eb; background: #dbeafe; color: #1e40af; box-shadow: 0 2px 8px rgba(37,99,235,0.2); }
.grade-option-btn.active.grade-c { border-color: #d97706; background: #fef3c7; color: #92400e; box-shadow: 0 2px 8px rgba(217,119,6,0.2); }
.grade-option-btn.active.grade-d { border-color: #dc2626; background: #fee2e2; color: #991b1b; box-shadow: 0 2px 8px rgba(220,38,38,0.2); }
.grade-form-group textarea { width: 100%; box-sizing: border-box; padding: 10px 12px; border: 2px solid #e5e7eb; border-radius: 8px; font-size: 13px; resize: vertical; transition: border-color 0.2s; font-family: inherit; }
.grade-form-group textarea:focus { outline: none; border-color: #f59e0b; box-shadow: 0 0 0 3px rgba(245,158,11,0.1); }
.grade-disabled-input { width: 100%; box-sizing: border-box; padding: 8px 12px; border: 1px solid #e5e7eb; border-radius: 8px; font-size: 13px; background: #f9fafb; color: #6b7280; }
.grade-modal-actions { display: flex; gap: 10px; justify-content: flex-end; margin-top: 18px; }
.btn-grade-save { background: linear-gradient(135deg, #f59e0b, #d97706); color: #fff; border: none; padding: 10px 24px; border-radius: 10px; font-size: 14px; font-weight: 600; cursor: pointer; transition: all 0.2s; }
.btn-grade-save:hover:not(:disabled) { box-shadow: 0 4px 16px rgba(245,158,11,0.35); }
.btn-grade-save:disabled { opacity: 0.5; cursor: not-allowed; }

/* ... existing code ... */

.event-placeholder-icon {
  font-size: 18px;
}
.timer-panel .event-result {
  padding: 10px 14px;
  margin: 0;
}
.timer-panel .event-label {
  margin-bottom: 2px;
}
.timer-panel .event-value {
  font-size: 14px;
}
.timer-panel .event-value.highlight {
  font-size: 16px;
}
.timer-panel .event-compare {
  margin-top: 6px;
  padding: 6px 10px;
}
.timer-panel .attribution-tags {
  margin-top: 6px;
}
.timer-panel .frozen-result {
  margin-top: 6px;
  padding: 8px 12px;
}
@media (max-width: 700px) {
  .qr-info-section {
    grid-template-columns: 1fr;
  }
  .qr-card {
    justify-self: center;
  }
  .timer-panel {
    flex-direction: column;
    align-items: stretch;
  }
  .timer-divider {
    width: 100%;
    height: 1px;
    margin: 12px 0;
  }
  .timer-left {
    min-width: auto;
  }
}
</style>
<script setup>import { ref, reactive, onMounted, watch, nextTick, onUnmounted } from 'vue'
import QRCode from 'qrcode'
import {
  fetchTask,
  fetchEvent,
  fetchAllAttributions,
  fetchFrozen,
  insertRecord,
  fetchCountByRecorder,
  fetchRandomForRetest,
  fetchAppGrade,
  saveAppGrade
} from '../api/index.js'

const emit = defineEmits(['error'])
const exceptionOptions = ['正常','iOS16闪退','iOS13/14/16均闪退','需要iOS18以上','地区不支持','硬件版本过低','超过10分钟0上报','越狱检测','其他','验证已解决','测试']
const template=['需要iOS17以上','需要登陆后使用，无法注册','卡死在加载页进不去','非英语汉语软件，看不懂','网络检测，无法进入','需要订阅后使用','禁止入库']
const downloadUrl = ref('')
const bundleId = ref('')
const originalCurrentTargetNum = ref(null)
const eventResult = ref('')
const newCurrentTargetNum = ref(null)
let attributions = ref([])
const eventId = ref(null)
const frozenMsg = ref('')
const duplicateTip = ref('')
const loading = ref(false)
const eventLoading = ref(false)
const frozenLoading = ref(false)
const saving = ref(false)
const saveMsg = ref('')
const showRetestModal = ref(false)
const retestLoading = ref(false)
const qrCanvas = ref(null)
const timerSeconds = ref(60)
const timerCountdown = ref(0)
const polling = ref(false)
const pollCount = ref(0)
let pollTimer = null
let timerMsg = ref('')
let timerInterval = null
let isFrozen=ref('')
let isSubmit=true
const MAX_RETRIES = 5
let retryCount = 0
const gradeData = ref(null)
const gradeLoading = ref(false)
const showGradeModal = ref(false)
const gradeSaving = ref(false)
const gradeMsg = ref('')
const urlAlreadyGraded = ref(false)
const currentUserName = ref(localStorage.getItem('userName') || '')
const gradeForm = reactive({ grade: '', remark: '' })

const form = reactive({
  exception_type: '',
  remark: '',
  recorder: localStorage.getItem('userName') || '',
  record_data: getTodayStr()
})


function getTodayStr() {
  const d = new Date()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return d.getFullYear() + '-' + mm + '-' + dd
}

function toStorageDate(isoDate) {
  if (!isoDate) return ''
  const parts = isoDate.split('-')
  return parts[0] + '/' + parseInt(parts[1]) + '/' + parseInt(parts[2])
}

async function fetchDataByName(){
  const json = await fetchCountByRecorder()
  if(json.success){
    alert("用户"+localStorage.getItem('userName') +"已入库"+json.data+"条")
  }else{
    alert("获取事件数失败，请联系工作人员")
  }
}

async function renderQR(url) {
  await nextTick()
  if (url && qrCanvas.value) {
    try {
      await QRCode.toCanvas(qrCanvas.value, url, { width: 256, margin: 2, color: { dark: '#000000', light: '#ffffff' } })
    } catch (e) { console.error('QR码生成失败:', e) }
  }
}

watch(downloadUrl, (val) => { renderQR(val) })

function comfirm(){
  if(isSubmit===false&&downloadUrl.value!==null){
    let com=confirm("请确认是否已入库该条测试数据,刷新会导致数据丢失！")
    if(com){
      fetchData()
    }else{
      return
    }
  }else{
    fetchData()
  }
}

// async function fetchData() {
//   loading.value = true
//   emit('error', '')
//   resetState()
//   try {
//     const json = await fetchTask()
//     if (json.data) {
//       if (json.duplicate) {
//         retryCount++
//         if (retryCount > MAX_RETRIES) {
//           emit('error', '已连续 ' + MAX_RETRIES + ' 次检测到重复URL，请稍后再试')
//           duplicateTip.value = ''
//           return
//         }
//         duplicateTip.value = '检测到重复URL（第' + retryCount + '次），正在为您自动刷新...'
//         setTimeout(() => fetchData(), 1500)
//         return
//       }
//       retryCount = 0
//       duplicateTip.value = ''
//       downloadUrl.value = json.data.downloadUrl || ''
//       bundleId.value = json.data.bundleId || ''
//       originalCurrentTargetNum.value = json.data.currentTargetNum ?? null
//       isSubmit=false;
//       if(originalCurrentTargetNum.value<=1||originalCurrentTargetNum.value===0){
//         alert("初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意")
//       }
//     } else {
//       showRetestModal.value = true
//     }
//   } catch (e) {
//     if (e.name === 'AbortError') { emit('error', '请求超时，远程服务器响应太慢') }
//     else { emit('error', '请求失败：' + e.message) }
//   } finally {
//     loading.value = false
//   }
// }

async function fetchData() {
  loading.value = true
  emit('error', '')
  resetState()
  try {
    const json = await fetchTask()
    if (json.data) {
      if (json.duplicate) {
        retryCount++
        if (retryCount > MAX_RETRIES) {
          emit('error', '已连续 ' + MAX_RETRIES + ' 次检测到重复URL，请稍后再试')
          duplicateTip.value = ''
          return
        }
        duplicateTip.value = '检测到重复URL（第' + retryCount + '次），正在为您自动刷新...'
        setTimeout(() => fetchData(), 1500)
        return
      }
      retryCount = 0
      duplicateTip.value = ''
      downloadUrl.value = json.data.downloadUrl || ''
      bundleId.value = json.data.bundleId || ''
      originalCurrentTargetNum.value = json.data.currentTargetNum ?? null
      isSubmit=false;
      if(originalCurrentTargetNum.value<=1||originalCurrentTargetNum.value===0){
        alert("初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意")
      }
      loadGradeInfo(downloadUrl.value)
    } else {
      showRetestModal.value = true
    }
  } catch (e) {
    if (e.name === 'AbortError') { emit('error', '请求超时，远程服务器响应太慢') }
    else { emit('error', '请求失败：' + e.message) }
  } finally {
    loading.value = false
  }
}

async function retestFlow() {
  retestLoading.value = true
  resetState()
  try {
    const dates = getPast3DaysDates()
    const json = await fetchRandomForRetest(dates)
    if (json.success && json.data) {
      showRetestModal.value = false
      downloadUrl.value = json.data.downloadUrl || ''
      bundleId.value = json.data.bundleId || ''
      const jsons = await fetchEvent(bundleId.value)
      try{
      if (jsons.success) {
        originalCurrentTargetNum.value = jsons.data.currentTargetNum ?? null
      }}catch (e) {
        alert('服务器无响应，请联系技术人员')
      }
      isSubmit = false
    } else {
      emit('error', json.message || '获取复测数据失败')
    }
  } catch (e) {
    emit('error', '复测请求失败：' + e.message)
  } finally {
    retestLoading.value = false
  }
}

function getPast3DaysDates() {
  const dates = []
  const d = new Date()
  for (let i = 0; i < 3; i++) {
    const t = new Date(d)
    t.setDate(d.getDate() - i)
    dates.push(t.getFullYear() + '/' + (t.getMonth() + 1) + '/' + t.getDate())
  }
  return dates
}

function startPolling() {
  polling.value = true
  pollCount.value = 0
  pollTimer = setInterval(async () => {
    pollCount.value++
    try {
      const json = await fetchTask()
      if (json.data && !json.duplicate) {
        // 获取到有效数据，立即停止轮询
        clearInterval(pollTimer)
        polling.value = false
        pollCount.value = 0
        downloadUrl.value = json.data.downloadUrl || ''
        bundleId.value = json.data.bundleId || ''
        originalCurrentTargetNum.value = json.data.currentTargetNum ?? null
        isSubmit = false
        if (originalCurrentTargetNum.value <= 1 || originalCurrentTargetNum.value === 0) {
          alert("初始originalCurrentTargetNum数小于等于1或等于0，后续测试期间可能会没有归因，请注意")
        }
      }
    } catch (e) {
      console.error('轮询请求失败:', e)
    }
  }, 10000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  polling.value = false
  pollCount.value = 0
}

async function queryEventWithFullMsg() {
  try {
    const data = await fetchEvent(bundleId.value)
    if(data.success){
      alert(JSON.stringify(data, null, 2))
    }
  }catch (e) {
    alert('服务器无响应，请联系技术人员')
  }
}

// async function queryEvent() {
//   if (!bundleId.value) return
//   eventLoading.value = true
//   emit('error', '')
//   eventResult.value = ''; newCurrentTargetNum.value = null; attributions.value = []; eventId.value = null; frozenMsg.value = ''
//   try {
//     const [eventJson, attrResults] = await Promise.all([
//       fetchEvent(bundleId.value),
//       fetchAllAttributions(bundleId.value)
//     ])
//     if (eventJson.success && eventJson.data) {
//       const newCurrent = eventJson.data.currentTargetNum
//       eventId.value = eventJson.data.id ?? null
//       if (newCurrent !== originalCurrentTargetNum.value) {
//         eventResult.value = 'has_event'
//         newCurrentTargetNum.value = newCurrent
//       } else {
//         eventResult.value = 'no_event'
//       }
//     } else {
//       emit('error', '事件接口返回异常：' + (eventJson.resultMsg || '未知错误'))
//     }
//     const found = []
//     for (const { type, json } of attrResults) {
//       if (json.success && json.data) {
//         if (Array.isArray(json.data) && json.data.length > 0) {
//           found.push(type)
//         } else if (!Array.isArray(json.data) && Object.keys(json.data).length > 0) {
//           found.push(type)
//         }
//       }
//     }
//     attributions.value = found
//   } catch (e) {
//     emit('error', '事件查询失败：' + e.message)
//   } finally {
//     eventLoading.value = false
//   }

async function queryEvent() {
  if (!bundleId.value) return
  eventLoading.value = true
  emit('error', '')
  eventResult.value = ''; newCurrentTargetNum.value = null; attributions.value = []; eventId.value = null; frozenMsg.value = ''
  try {
    const [eventJson, attrResults] = await Promise.all([
      fetchEvent(bundleId.value),
      fetchAllAttributions(bundleId.value)
    ])
    if (eventJson.success && eventJson.data) {
      const newCurrent = eventJson.data.currentTargetNum
      eventId.value = eventJson.data.id ?? null
      if (originalCurrentTargetNum.value !== null) {
        if (newCurrent !== originalCurrentTargetNum.value) {
          eventResult.value = 'has_event'
          newCurrentTargetNum.value = newCurrent
        } else {
          eventResult.value = 'no_event'
        }
      } else {
        newCurrentTargetNum.value = newCurrent
        eventResult.value = newCurrent != null && newCurrent > 0 ? 'has_event' : 'no_event'
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
    attributions.value = found
  } catch (e) {
    emit('error', '事件查询失败：' + e.message)
  } finally {
    eventLoading.value = false
  }
}

function resetState() {
  downloadUrl.value = '';
  bundleId.value = '';
  originalCurrentTargetNum.value = null
  eventResult.value = '';
  newCurrentTargetNum.value = null;
  attributions.value = []
  eventId.value = null;
  frozenMsg.value = '';
  duplicateTip.value = '';
  saveMsg.value = ''
  form.exception_type = '';
  form.remark = '';
  form.recorder = localStorage.getItem('userName') || '';
  //form.record_data = getTodayStr()
  isFrozen.value=''
  gradeData.value = null
  urlAlreadyGraded.value = false
  gradeMsg.value = ''
  gradeForm.grade = ''
  gradeForm.remark = ''
}

async function doFrozen() {
  if (!eventId.value) return
  frozenLoading.value = true; emit('error', '')
  try {
    const json = await fetchFrozen(eventId.value)
    if (json.success) {
      frozenMsg.value = json.resultMsg || '操作完成'
      isFrozen.value=',已冻结'
    }
    else {
      emit('error', '冻结接口返回异常：' + (json.resultMsg || '未知错误'))
    }
  } catch (e) {
    emit('error', '冻结请求失败：' + e.message)
  }
  finally {
    frozenLoading.value = false
  }
}

async function saveToMySQL() {
  emit('record-saved')
  if (!form.exception_type.trim()) { emit('error', '请选择异常类型'); return }
  saving.value = true; saveMsg.value = ''
  if(newCurrentTargetNum.value===0||newCurrentTargetNum.value===null){
    attributions.value=[]
    alert('无新增事件token，即使原本的token有归因，也不会被设置在字段内')
  }
  let finalRemark = form.remark.trim()
  if (isFrozen.value) {
    finalRemark += isFrozen.value
  }
  if (newCurrentTargetNum.value > 0 && attributions.value.length === 0) {
    finalRemark += ',无事件归因'
  }
  try {
    const json = await insertRecord({
      URL: downloadUrl.value,
      bundleId: bundleId.value,
      ascribe: (attributions.value || []).join(';'),
      event_number: newCurrentTargetNum.value,
      exception_type: form.exception_type.trim(),
      record_data: toStorageDate(form.record_data),
      recorder: form.recorder,
      remark: finalRemark,
      isOutput: 0
    })
    if (json.success) {
      saveMsg.value = '✅ ' + (json.resultMsg || '入库成功')
      isSubmit=true;
    }
    else { emit('error', json.resultMsg || '入库失败') }
  } catch (e) { emit('error', '入库请求失败：' + e.message) }
  finally { saving.value = false }
}

function startTimer() {
  if (!timerSeconds.value || timerSeconds.value < 1) return
  timerCountdown.value = timerSeconds.value
  timerMsg.value = '将在 ' + timerCountdown.value + ' 秒后自动查询事件'
  timerInterval = setInterval(() => {
    timerCountdown.value--
    if (timerCountdown.value <= 0) {
      clearInterval(timerInterval)
      timerInterval = null
      timerCountdown.value = 0
      timerMsg.value = '定时已到，正在自动查询事件...'
      queryEvent().then(() => {
        timerMsg.value = '✅ 定时查询已完成'
        setTimeout(() => {
          timerMsg.value = '计时结束后内容会在这里显示'
        }, 5000);
      })
    }
  }, 1000)
}

function cancelTimer() {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
  timerCountdown.value = 0
  timerMsg.value = ''
}

async function loadGradeInfo(url) {
  if (!url) return
  gradeLoading.value = true
  gradeData.value = null
  urlAlreadyGraded.value = false
  try {
    const json = await fetchAppGrade(url)
    if (json.success && json.data) {
      gradeData.value = json.data
    }
    urlAlreadyGraded.value = json.urlAlreadyGraded === true
  } catch (e) {
    console.warn('评级查询失败:', e)
  } finally {
    gradeLoading.value = false
  }
}

async function submitGrade() {
  if (!gradeForm.grade) {
    gradeMsg.value = '❌ 请选择评级等级（A/B/C/D）'
    return
  }
  if (!downloadUrl.value) {
    gradeMsg.value = '❌ 当前无有效的下载链接'
    return
  }
  gradeSaving.value = true
  gradeMsg.value = ''
  try {
    const json = await saveAppGrade(
        downloadUrl.value,
        gradeForm.grade,
        currentUserName.value,
        gradeForm.remark.trim()
    )
    if (json.success) {
      gradeMsg.value = '✅ ' + (json.message || '评级保存成功')
      gradeData.value = { grade: gradeForm.grade, recorder: currentUserName.value, remark: gradeForm.remark.trim() }
      urlAlreadyGraded.value = true
      setTimeout(() => { showGradeModal.value = false; gradeMsg.value = '' }, 1500)
    } else {
      gradeMsg.value = '❌ ' + (json.message || '评级保存失败')
    }
  } catch (e) {
    gradeMsg.value = '❌ 评级请求失败：' + e.message
  } finally {
    gradeSaving.value = false
  }
}

onUnmounted(() => { cancelTimer() })
onUnmounted(() => { cancelTimer(); stopPolling() })

onMounted(() => {
  timerMsg.value='计时结束后内容会在这里显示' }
)
</script>
