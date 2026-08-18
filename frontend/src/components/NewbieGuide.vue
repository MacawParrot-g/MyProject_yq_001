<script setup>
import { ref } from 'vue'

const emit = defineEmits(['close'])

function close() {
  emit('close')
}
</script>

<template>
  <div class="guide-overlay" @click.self="close">
    <div class="guide-modal">
      <div class="guide-header">
        <h2>📘 新人指南</h2>
        <button class="guide-close-btn" @click="close">✕</button>
      </div>

      <div class="guide-body">

        <div class="guide-section">
          <h3>✨ 系统亮点</h3>
          <ul class="guide-highlight-list">
            <li>🚀 <strong>自动化测试流程</strong> — 自动拉取任务、生成二维码、查询事件、一键入库，大幅减少人工操作</li>
            <li>📊 <strong>数据看板</strong> — 实时查看入库数据、服务器状态，支持归因筛选与分页管理</li>
            <li>🔄 <strong>多归因平台支持</strong> — 同时支持 AppFlyer、Adjust、Singular、Tenjin 四大归因平台</li>
            <li>⏱ <strong>定时查询</strong> — 自定义倒计时自动查询事件，解放双手</li>
            <li>🧊 <strong>冻结机制</strong> — 对无事件应用一键冻结，快速标记处理状态</li>
            <li>🔁 <strong>去重功能</strong> — 自动去重开关，防止重复入库</li>
          </ul>
        </div>

        <div class="guide-divider"></div>

        <div class="guide-section">
          <h3>📖 使用教程</h3>

          <div class="guide-step">
            <h4>一、自动模式（主要工作流）</h4>
            <ol>
              <li>进入 <strong>「自动」</strong> 标签页，系统会自动轮询拉取下载任务</li>
              <li>扫描页面上生成的 <strong>二维码</strong>，在设备上安装应用</li>
              <li>安装完成后开始测试应用，系统会每 10 秒自动查询事件</li>
              <li>测试完成后，点击 <strong>「查询事件」</strong> 确认事件数变化</li>
              <li>在下方表单中填写 <strong>异常类型</strong> 和 <strong>备注</strong>，点击 <strong>「直接入库MySQL」</strong> 完成入库</li>
            </ol>
          </div>

          <div class="guide-step">
            <h4>二、手动模式</h4>
            <ol>
              <li>进入 <strong>「手动」</strong> 标签页，手动粘贴下载链接</li>
              <li>系统生成二维码，后续流程与自动模式一致</li>
              <li>适用于自动模式未覆盖到的特殊场景</li>
            </ol>
          </div>

          <div class="guide-step">
            <h4>三、数据看板</h4>
            <ol>
              <li>进入 <strong>「数据看板」</strong> 标签页查看所有入库记录</li>
              <li>支持按 <strong>归因平台</strong>、<strong>记录人</strong>、<strong>冻结状态</strong> 筛选</li>
              <li>可对已有记录进行 <strong>编辑</strong>、<strong>删除</strong>、<strong>重新测试</strong></li>
            </ol>
          </div>

          <div class="guide-step">
            <h4>四、二维码 & 导出</h4>
            <ul>
              <li><strong>二维码标签页</strong>：手动输入链接生成二维码，用于快速安装</li>
              <li><strong>导出标签页</strong>：将入库数据导出为报告文件</li>
            </ul>
          </div>
        </div>

        <div class="guide-divider"></div>

        <div class="guide-section">
          <h3>⚠️ 测试规范（重要）</h3>
          <div class="guide-rules-card">
            <div class="guide-rule-item">
              <span class="guide-rule-icon">🎮</span>
              <div>
                <strong>游戏类应用（能正常运行）</strong>
                <p>请<strong>玩够 10 分钟以上</strong>再进行入库操作，确保事件数据充分上报。</p>
              </div>
            </div>
            <div class="guide-rule-item">
              <span class="guide-rule-icon">📦</span>
              <div>
                <strong>其他类型应用</strong>
                <p>如无法运行的应用、商城类、需要登录的应用、其他工具类应用等，<strong>5 分钟以内能出事件就可以入库</strong>。</p>
              </div>
            </div>
            <div class="guide-rule-item guide-rule-danger">
              <span class="guide-rule-icon">🧊</span>
              <div>
                <strong>直接冻结（无需抢救）</strong>
                <p>以下情况<strong>不用抢救，直接冻结</strong>：</p>
                <ul>
                  <li>需要登录但无法注册的应用</li>
                  <li>必须付费才能使用的应用</li>
                  <li>版本不兼容（如操作系统要求 iOS 17 以上）的应用</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <div class="guide-divider"></div>

        <div class="guide-footer-signature">
          <p>本系统由</p>
          <p class="guide-dev-name">🦜 蓝黄金刚鹦鹉</p>
          <p>开发维护</p>
          <p class="guide-company">深圳市慧动创想科技有限公司</p>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.guide-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: guideFadeIn 0.25s ease;
}

@keyframes guideFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.guide-modal {
  background: #fff;
  border-radius: 16px;
  max-width: 720px;
  width: 92%;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.25);
  animation: guideSlideUp 0.3s ease;
}

@keyframes guideSlideUp {
  from { transform: translateY(30px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.guide-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px 16px;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  background: #fff;
  border-radius: 16px 16px 0 0;
  z-index: 1;
}

.guide-header h2 {
  font-size: 20px;
  color: #333;
  margin: 0;
}

.guide-close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
  padding: 4px 8px;
  border-radius: 6px;
}
.guide-close-btn:hover {
  color: #333;
  background: #f0f0f0;
}

.guide-body {
  padding: 20px 28px 28px;
}

.guide-section {
  margin-bottom: 8px;
}

.guide-section h3 {
  font-size: 16px;
  color: #333;
  margin-bottom: 14px;
  padding-left: 2px;
}

.guide-highlight-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.guide-highlight-list li {
  padding: 8px 12px;
  margin-bottom: 8px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: 13px;
  color: #444;
  line-height: 1.7;
  border-left: 3px solid #667eea;
}
.guide-highlight-list li strong {
  color: #333;
}

.guide-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #ddd, transparent);
  margin: 20px 0;
}

.guide-step {
  margin-bottom: 18px;
}
.guide-step h4 {
  font-size: 14px;
  color: #667eea;
  margin-bottom: 8px;
  font-weight: 700;
}
.guide-step ol, .guide-step ul {
  padding-left: 20px;
  margin: 0;
}
.guide-step li {
  font-size: 13px;
  color: #444;
  line-height: 1.8;
  margin-bottom: 4px;
}
.guide-step li strong {
  color: #333;
}

.guide-rules-card {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 16px 18px;
  border: 1px solid #e8e8e8;
}

.guide-rule-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}
.guide-rule-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}
.guide-rule-item:first-child {
  padding-top: 0;
}

.guide-rule-icon {
  font-size: 28px;
  flex-shrink: 0;
  margin-top: 2px;
}

.guide-rule-item p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #555;
  line-height: 1.6;
}
.guide-rule-item ul {
  margin: 6px 0 0;
  padding-left: 18px;
}
.guide-rule-item li {
  font-size: 13px;
  color: #555;
  line-height: 1.7;
}

.guide-rule-danger {
  background: #fff8f0;
  border-radius: 8px;
  padding: 12px 14px;
  margin: 4px -14px 0;
  border-bottom: none !important;
}
.guide-rule-danger strong {
  color: #e65100;
}

.guide-footer-signature {
  text-align: center;
  padding: 24px 0 8px;
  color: #999;
  font-size: 13px;
  line-height: 1.8;
}
.guide-dev-name {
  font-size: 18px;
  font-weight: 700;
  color: #667eea;
  margin: 4px 0;
}
.guide-company {
  font-size: 11px;
  color: #bbb;
  margin-top: 2px;
}
</style>