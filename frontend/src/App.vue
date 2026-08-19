<template>
  <div class="app-wrapper" v-if="loggedIn">
    <h1>"我看行"游戏测试数据可视化&自动入库系统(CI/CD构建版)</h1>
    <div class="subtitle">由深圳市慧动创想科技有限公司--蓝黄金刚鹦鹉开发</div>

    <div class="guide-trigger-bar">
      <span class="guide-trigger-link" @click="showGuide = true">新人指南</span>
    </div>

    <div class="tab-switcher">
      <button v-for="tab in tabs" :key="tab.key" class="tab-btn" :class="{ active: mode === tab.key }" @click="switchMode(tab.key)">
        {{ tab.label }}
      </button>
    </div>

    <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
    <keep-alive>
      <component :is="currentComponent" @error="setError" />
    </keep-alive>
  </div>
  <NewbieGuide v-if="showGuide" @close="showGuide = false" />
  <div class="login-overlay" v-if="!loggedIn">
    <div class="login-card">
      <h1>我看行"游戏测试数据可视化&自动入库系统3.0</h1>
      <div class="login-subtitle">请输入您的姓名开始使用</div>
      <div class="login-form">
        <input v-model="loginName" placeholder="请输入姓名" @keyup.enter="doLogin" class="login-input" />
        <button class="login-btn" @click="doLogin" :disabled="!loginName.trim()">进入系统</button>
      </div>
      <div class="subtitle">由深圳市慧动创想科技有限公司--蓝黄金刚鹦鹉开发</div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, reactive, computed} from 'vue'
import AutoMode from './components/AutoMode.vue'
import ManualMode from './components/ManualMode.vue'
import ExportMode from './components/ExportMode.vue'
import DataVisitable from "./components/DataVisitable.vue";
import QRCodeBuilderByMan from "./components/QRCodeBuilderByMan.vue";
import NewbieGuide from "./components/NewbieGuide.vue";

const mode = ref('auto')
const errorMsg = ref('')
const loggedIn = ref(false)
const loginName = ref('')
const showGuide = ref(false)

const componentMap = {
  auto: AutoMode,
  manual: ManualMode,
  export: ExportMode,
  qrcode: QRCodeBuilderByMan,
  data: DataVisitable
}
const currentComponent = computed(() => componentMap[mode.value])

const tabs = [
  { key: 'auto', label: '自动' },
  { key: 'manual', label: '手动' },
  { key: 'qrcode', label: '二维码' },
  { key: 'export', label: '导出' },
  { key: 'data', label: '数据看板' }
]

onMounted(() => {
  const saved = localStorage.getItem('userName')
  if (saved) {
    loggedIn.value = true
  }
})

function doLogin() {
  const name = loginName.value.trim()
  if (!name) return
  localStorage.setItem('userName', name)
  loggedIn.value = true

}


// function fetchDataByName(){
//   const json=await.fetchCountByRecorder(form);
//   if(json.success){
//     alert("目标100条,已完成,"+json.number+"条")
//   }else{
//     alert("获取事件数失败，请联系工作人员")
//   }
// }

function setError(msg) { errorMsg.value = msg }

function switchMode(newMode) {
  mode.value = newMode
  errorMsg.value = ''
}
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh; display: flex; justify-content: center; align-items: flex-start; padding: 30px 20px;
}
.floating-text{
  padding-bottom: 17px;
}
.subtitle { font-size: 12px; color: #999; margin-bottom: 22px; }

.guide-trigger-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}
.guide-trigger-link {
  font-size: 13px;
  color: #667eea;
  cursor: pointer;
  padding: 6px 16px;
  border-radius: 20px;
  background: #f0f4ff;
  font-weight: 600;
  transition: all 0.25s ease;
  user-select: none;
}
.guide-trigger-link:hover {
  background: #667eea;
  color: #fff;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.3);
  transform: translateY(-1px);
}
.app-wrapper {
  background: #fff; border-radius: 16px; padding: 36px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15); text-align: center; max-width: 900px; width: 100%;
}
h1 { font-size: 22px; color: #333; margin-bottom: 6px; }
.subtitle { font-size: 12px; color: #999; margin-bottom: 22px; }

.tab-switcher { display: flex; background: #f0f0f0; border-radius: 10px; padding: 4px; margin-bottom: 24px; }
.tab-btn { flex: 1; padding: 10px 0; font-size: 14px; border: none; border-radius: 8px; cursor: pointer; background: transparent; color: #888; font-weight: 500; transition: all 0.25s ease; position: relative; }
.tab-btn.active { background: #fff; color: #333; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.tab-btn:not(.active):hover { color: #555; }

.error-msg { color: #e74c3c; background: #fdecea; padding: 10px 14px; border-radius: 8px; margin-bottom: 14px; font-size: 13px; text-align: left; }
.duplicate-tip { color: #f57f17; background: #fff8e1; border-left: 4px solid #ffc107; padding: 12px 16px; border-radius: 8px; margin-bottom: 16px; font-size: 13px; font-weight: 600; text-align: left; }
.loading { color: #888; font-size: 13px; padding: 16px 0; }

.btn-refresh { background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none; padding: 11px 28px; font-size: 15px; border-radius: 8px; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; margin-bottom: 20px; }
.btn-refresh:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(102,126,234,0.4); }
.btn-refresh:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.btn-today-count {
  background: linear-gradient(135deg, #6a11cb, #2575fc);
  color: #fff;
  border: none;
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  margin-bottom: 16px;
  letter-spacing: 0.5px;
}
.btn-today-count:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(37,117,252,0.4);
}
.btn-today-count:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(37,117,252,0.3);
}
.btn-event { background: linear-gradient(135deg, #f093fb, #f5576c); color: #fff; border: none; padding: 11px 28px; font-size: 15px; border-radius: 8px; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; margin-bottom: 14px; }
.btn-event:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(245,87,108,0.4); }
.btn-event:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.timer-section { margin-bottom: 14px; }
.timer-row { display: flex; align-items: center; gap: 10px; justify-content: center; }
.timer-label { font-size: 14px; font-weight: 600; color: #555; white-space: nowrap; }
.timer-input { width: 80px; padding: 8px 10px; font-size: 14px; border: 2px solid #ddd; border-radius: 8px; outline: none; text-align: center; transition: border-color 0.2s; }
.timer-input:focus { border-color: #667eea; }
.btn-timer { background: linear-gradient(135deg, #ffa751, #ffe259); color: #fff; border: none; padding: 8px 18px; font-size: 13px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: transform 0.2s, box-shadow 0.2s; white-space: nowrap; }
.btn-timer:hover { transform: translateY(-2px); box-shadow: 0 4px 14px rgba(255,167,81,0.4); }
.btn-timer:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }
.btn-timer-cancel { background: linear-gradient(135deg, #ff6b6b, #ee5a24); color: #fff; border: none; padding: 8px 14px; font-size: 13px; border-radius: 8px; cursor: pointer; font-weight: 600; transition: transform 0.2s; white-space: nowrap; }
.btn-timer-cancel:hover { transform: translateY(-2px); }
.timer-status { margin-top: 8px; font-size: 13px; color: #667eea; font-weight: 600; }

.btn-frozen { background: linear-gradient(135deg, #4facfe, #00f2fe); color: #fff; border: none; padding: 11px 28px; font-size: 15px; border-radius: 8px; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; margin-top: 10px; margin-bottom: 14px; }
.btn-frozen:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(79,172,254,0.4); }
.btn-frozen:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.btn-save { background: linear-gradient(135deg, #43e97b, #38f9d7); color: #fff; border: none; padding: 11px 28px; font-size: 15px; border-radius: 8px; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; margin-top: 10px; margin-bottom: 6px; }
.btn-save:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(67,233,123,0.4); }
.btn-save:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.info-section { text-align: left; background: #f8f9fa; border-radius: 8px; padding: 14px 18px; margin-bottom: 16px; }
.info-item { margin-bottom: 10px; word-break: break-all; }
.info-item:last-child { margin-bottom: 0; }
.info-label { font-weight: 600; color: #555; font-size: 13px; margin-bottom: 3px; }
.info-value { color: #333; font-size: 13px; line-height: 1.6; }
.info-value a { color: #667eea; text-decoration: none; }
.info-value a:hover { text-decoration: underline; }

.input-group { display: flex; gap: 10px; margin-bottom: 20px; }
.input-group input { flex: 1; padding: 11px 14px; font-size: 15px; border: 2px solid #ddd; border-radius: 8px; outline: none; transition: border-color 0.2s; }
.input-group input:focus { border-color: #667eea; }

.preview-url { text-align: left; background: #f0f4ff; border-radius: 8px; padding: 10px 14px; margin-bottom: 16px; font-size: 13px; color: #555; word-break: break-all; }
.preview-url strong { color: #667eea; }

.qrcode-container { display: flex; justify-content: center; margin-bottom: 20px; }
.qrcode-container canvas { border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

.event-result { text-align: left; background: #fff3e0; border-radius: 8px; padding: 14px 18px; margin-top: 14px; }
.event-result.no-event { background: #e8f5e9; }
.event-result .event-label { font-weight: 600; color: #555; font-size: 13px; margin-bottom: 3px; }
.event-result .event-value { color: #333; font-size: 13px; line-height: 1.6; }
.event-result .event-value.highlight { color: #f5576c; font-weight: 700; font-size: 17px; }
.event-compare { text-align: left; background: #f8f9fa; border-radius: 8px; padding: 10px 14px; margin-top: 10px; font-size: 12px; color: #888; }
.event-compare .diff { color: #f5576c; font-weight: 600; }

.attribution-tags { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.attr-tag { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600; color: #fff; }
.attr-tag.appflyer { background: #4CAF50; }
.attr-tag.adjust { background: #FF9800; }
.attr-tag.singular { background: #2196F3; }
.attr-tag.tenjin { background: #9C27B0; }
.no-attribution { color: #e65100; font-weight: 600; font-size: 13px; margin-top: 8px; background: #fff3e0; padding: 4px 12px; border-radius: 8px; display: inline-block; }

.frozen-result { text-align: left; background: #e3f2fd; border-radius: 8px; padding: 14px 18px; margin-top: 10px; }
.frozen-result .frozen-label { font-weight: 600; color: #1976d2; font-size: 13px; margin-bottom: 3px; }
.frozen-result .frozen-value { color: #333; font-size: 13px; line-height: 1.6; }

.save-success { text-align: left; background: #e8f5e9; border-radius: 8px; padding: 10px 14px; margin-top: 10px; color: #2e7d32; font-size: 13px; font-weight: 600; }

.form-section { text-align: left; background: #f8f9fa; border-radius: 8px; padding: 16px 18px; margin-top: 16px; }
.form-section h4 { font-size: 14px; color: #333; margin-bottom: 12px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-weight: 600; color: #555; font-size: 13px; margin-bottom: 5px; }
.form-group input, .form-group textarea, .form-group select { width: 100%; padding: 10px 12px; font-size: 13px; border: 2px solid #ddd; border-radius: 8px; outline: none; transition: border-color 0.2s; font-family: inherit; }
.form-group input:focus, .form-group textarea:focus, .form-group select:focus { border-color: #667eea; }
.form-group textarea { resize: vertical; min-height: 60px; }

.export-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.export-count { font-size: 14px; color: #555; font-weight: 600; }
.export-count span { color: #f5576c; font-size: 18px; font-weight: 700; }
.btn-export-exec { background: linear-gradient(135deg, #f093fb, #f5576c); color: #fff; border: none; padding: 11px 28px; font-size: 15px; border-radius: 8px; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; }
.btn-export-exec:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(245,87,108,0.4); }
.btn-export-exec:disabled { opacity: 0.6; cursor: not-allowed; }
.export-table-wrapper { overflow-x: auto; margin-bottom: 20px; border-radius: 8px; border: 1px solid #e0e0e0; }
.export-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 700px; }
.export-table th { background: #667eea; color: #fff; padding: 10px; white-space: nowrap; }
.export-table td { padding: 8px 10px; border-bottom: 1px solid #eee; color: #333; max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.export-table tr:hover td { background: #f5f7ff; }
.export-table tr:last-child td { border-bottom: none; }
.export-empty { text-align: center; padding: 50px 20px; color: #aaa; font-size: 15px; }
.export-empty .empty-icon { font-size: 48px; margin-bottom: 12px; }
.export-result-box { text-align: left; border-radius: 8px; padding: 14px 18px; margin-top: 16px; font-size: 13px; color: #333; }
.export-result-box.success { background: #e8f5e9; border-left: 4px solid #43a047; }
.export-result-box.fail { background: #fdecea; border-left: 4px solid #e74c3c; }

.login-overlay {
  display: flex; justify-content: center; align-items: center; min-height: 80vh; width: 100%;
}
.login-card {
  background: #fff; border-radius: 16px; padding: 48px 40px; text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2); max-width: 420px; width: 100%;
}
.login-card h1 { font-size: 20px; color: #333; margin-bottom: 8px; }
.login-subtitle { font-size: 14px; color: #888; margin-bottom: 28px; }
.login-form { display: flex; flex-direction: column; gap: 16px; }
.login-input {
  padding: 14px 16px; font-size: 16px; border: 2px solid #ddd; border-radius: 10px;
  outline: none; text-align: center; transition: border-color 0.2s;
}
.login-input:focus { border-color: #667eea; }
.login-btn {
  background: linear-gradient(135deg, #667eea, #764ba2); color: #fff; border: none;
  padding: 14px; font-size: 16px; border-radius: 10px; cursor: pointer;
  font-weight: 600; transition: transform 0.2s, box-shadow 0.2s;
}
.login-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(102,126,234,0.4); }
.login-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
.login-notice { font-size: 11px; color: #bbb; margin-top: 24px; }

.modal-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.modal-box {
  background: #fff;
  border-radius: 10px;
  padding: 28px 32px;
  min-width: 340px;
  box-shadow: 0 6px 24px rgba(0,0,0,0.18);
  text-align: center;
}
.modal-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #333;
}
.modal-desc {
  font-size: 14px;
  color: #666;
  margin-bottom: 22px;
}
.modal-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.btn-modal {
  padding: 10px 0;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: bold;
  cursor: pointer;
  transition: opacity 0.2s;
}
.btn-modal:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-poll {
  background: #ffc107;
  color: #856404;
}
.btn-retest {
  background: #28a745;
  color: #fff;
}
.btn-cancel {
  background: #e2e3e5;
  color: #383d41;
}
</style>
