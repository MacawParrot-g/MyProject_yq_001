<script setup>
import { ref } from 'vue'

const emit = defineEmits(['close'])
const displayName = ref(localStorage.getItem('userName') || '新用户')
const accType = ref(localStorage.getItem('accType') || 'USER')
const activeRole = ref(accType.value === 'DEVELOPER' ? 'developer' : accType.value === 'ADMIN' ? 'admin' : 'user')
const expandedSections = ref({})

function close() { emit('close') }

function toggleSection(key) {
  expandedSections.value[key] = !expandedSections.value[key]
}

function isExpanded(key) {
  return expandedSections.value[key] !== false
}
</script>

<template>
  <div class="guide-overlay" @click.self="close">
    <div class="guide-modal">
      <div class="guide-header">
        <div class="guide-header-left">
          <span class="guide-header-icon">📘</span>
          <div>
            <h2>新人指南</h2>
            <div class="guide-header-sub">游戏测试数据管理平台 · 完整使用手册</div>
          </div>
        </div>
        <button class="guide-close-btn" @click="close">✕</button>
      </div>

      <div class="guide-body">
        <div class="guide-welcome">
          <p>亲爱的 <strong>{{ displayName }}</strong>，欢迎您加入<strong>深圳市慧动创想科技有限公司</strong>！</p>
          <p>本系统是专为游戏归因测试打造的一站式数据管理平台，涵盖自动化测试流程、数据入库、归因查询、数据导出、用户管理等核心功能。为了帮助您快速上手，请根据您的账户角色阅读对应的使用教程。</p>
          <p>您当前的角色为：<span class="role-badge" :class="'role-' + accType.toLowerCase()">{{ accType === 'ADMIN' ? '管理员' : accType === 'DEVELOPER' ? 'root账户' : '普通用户' }}</span></p>
        </div>

        <div class="guide-role-tabs">
          <button class="guide-role-tab" :class="{ active: activeRole === 'user' }" @click="activeRole = 'user'">
            <span class="role-tab-icon">👤</span><span>普通用户</span>
          </button>
          <button class="guide-role-tab" :class="{ active: activeRole === 'admin' }" @click="activeRole = 'admin'">
            <span class="role-tab-icon">🛡️</span><span>管理员</span>
          </button>
          <button class="guide-role-tab" :class="{ active: activeRole === 'developer' }" @click="activeRole = 'developer'">
            <span class="role-tab-icon">🛠️</span><span>开发者</span>
          </button>
        </div>

        <!-- ======================== 普通用户教程 ======================== -->
        <div v-if="activeRole === 'user'" class="guide-content">
          <div class="guide-toc">
            <h3>📑 目录</h3>
            <ul>
              <li><a @click="toggleSection('u-overview')">一、系统概览与界面介绍</a></li>
              <li><a @click="toggleSection('u-login')">二、登录与账户安全</a></li>
              <li><a @click="toggleSection('u-auto')">三、自动模式 —— 核心工作流</a></li>
              <li><a @click="toggleSection('u-manual')">四、手动模式 —— 特殊场景处理</a></li>
              <li><a @click="toggleSection('u-qrcode')">五、二维码生成器</a></li>
              <li><a @click="toggleSection('u-export')">六、数据导出</a></li>
              <li><a @click="toggleSection('u-data')">七、数据看板</a></li>
              <li><a @click="toggleSection('u-dedup')">八、自动去重机制</a></li>
              <li><a @click="toggleSection('u-rules')">九、测试规范与冻结标准</a></li>
              <li><a @click="toggleSection('u-faq')">十、常见问题与排障</a></li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('u-overview')">
            <h3>一、系统概览与界面介绍</h3>
            <p>本系统采用前后端分离架构，前端基于 Vue 3 + Vite 构建，后端基于 Spring Boot 3.2，数据存储使用 MySQL 数据库配合 Redis 缓存。系统面向三类用户角色——普通用户（USER）、管理员（ADMIN）、开发者（DEVELOPER），不同角色登录后会看到不同的功能菜单。</p>

            <h4>1.1 整体布局</h4>
            <p>系统采用经典的左侧导航栏 + 右侧内容区布局。左侧导航栏包含以下元素：</p>
            <ul>
              <li><strong>顶部 Logo 区域</strong>：显示欢迎语，右侧有折叠/展开按钮，点击可收起导航栏以获得更大的工作区域。</li>
              <li><strong>功能导航区</strong>：以图标 + 文字的方式列出所有可用功能模块，点击即可切换。当前选中的模块会高亮显示，并在左侧有一条紫色指示条。</li>
              <li><strong>今日统计卡片</strong>：实时显示您今天已入库的数据条数，方便您掌握工作进度。</li>
              <li><strong>底部用户信息区</strong>：显示您的姓名、角色标签，以及「新人指南」和「退出登录」两个快捷操作链接。</li>
            </ul>

            <h4>1.2 普通用户可用功能一览</h4>
            <p>作为普通用户，您可以使用以下功能模块：</p>
            <table class="guide-table">
              <thead><tr><th>功能模块</th><th>导航标签</th><th>核心用途</th></tr></thead>
              <tbody>
              <tr><td>自动模式</td><td>A</td><td>自动拉取下载任务，扫码测试，查询事件，一键入库</td></tr>
              <tr><td>手动模式</td><td>M</td><td>手动输入 App ID 获取任务，适用于特殊场景</td></tr>
              <tr><td>二维码生成</td><td>Q</td><td>将任意文本或网址转换为二维码图片</td></tr>
              <tr><td>数据导出</td><td>E</td><td>将您名下未导出的数据打包导出并下载</td></tr>
              <tr><td>数据看板</td><td>D</td><td>查看、编辑、删除已入库记录，支持筛选和复测</td></tr>
              </tbody>
            </table>

            <h4>1.3 技术架构简述</h4>
            <p>系统后端运行在 Spring Boot 容器上，通过 JdbcTemplate 操作 MySQL 数据库，使用 Redis 进行会话管理、去重控制和缓存加速。RabbitMQ 消息队列用于异步处理导出等耗时任务。前端通过 RESTful API 与后端通信，所有请求均携带认证信息。了解这些技术背景有助于您在遇到问题时更好地定位原因。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('u-login')">
            <h3>二、登录与账户安全</h3>

            <h4>2.1 登录流程</h4>
            <p>打开系统地址后，您会看到登录页面。请输入管理员为您分配的账号（UID）和密码，点击「进入系统」按钮即可完成登录。登录成功后，系统会自动创建会话并跳转至主界面，默认进入「自动模式」页面。</p>

            <h4>2.2 会话机制</h4>
            <p>系统采用服务端 Session 会话机制，会话有效期为 30 天。在会话有效期内，即使关闭浏览器再次打开系统地址，也会自动恢复登录状态，无需重新输入账号密码。但如果会话过期或被管理员强制踢下线，则需要重新登录。</p>

            <h4>2.3 封禁机制</h4>
            <p>如果管理员在踢您下线时设置了封禁时间，那么在封禁期间内您将无法登录，系统会提示剩余封禁秒数。这是正常的管理操作，请耐心等待封禁结束后再试。</p>

            <h4>2.4 安全建议</h4>
            <ul>
              <li>不要将您的账号密码分享给他人，每个账户的操作记录都会关联到您的姓名。</li>
              <li>如果发现账户异常（如非本人操作记录），请立即联系管理员重置密码。</li>
              <li>长时间不使用时建议主动退出登录，特别是在共享设备上。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('u-auto')">
            <h3>三、自动模式 —— 核心工作流</h3>
            <p>自动模式是您日常工作的主要入口，整个流程设计为「拉取任务 → 扫码安装 → 测试应用 → 查询事件 → 入库」五步闭环。</p>

            <h4>3.1 拉取下载任务</h4>
            <p>进入自动模式页面后，系统会显示当前待测试的应用信息。您可以点击页面右上角的「刷新数据」按钮，系统会向后端请求一条新的下载任务。每条任务包含以下关键信息：</p>
            <ul>
              <li><strong>下载链接（Download URL）</strong>：用于生成二维码的应用安装包下载地址。</li>
              <li><strong>Bundle ID</strong>：应用的唯一标识符，用于后续的事件查询和归因查询。</li>
              <li><strong>原始 CurrentTargetNum</strong>：测试前的基准事件数，后续通过对比此值判断是否有新事件产生。</li>
            </ul>
            <p>如果当前没有待测试的任务，页面会显示「当前没有任何测试条目」的提示，此时您可以开启自动轮询来持续等待新任务。</p>

            <h4>3.2 自动轮询</h4>
            <p>当任务队列为空或您希望持续获取新任务时，可以点击「继续轮询」开启自动轮询模式。系统会每 10 秒自动向服务端请求一次新任务，页面上方会显示绿色的轮询状态条，包含当前轮询次数。轮询期间一旦获取到新任务，会自动停止轮询并展示二维码。您也可以随时点击「停止轮询」手动终止。</p>

            <h4>3.3 扫描二维码安装应用</h4>
            <p>获取到任务后，页面左侧会显示一个二维码。请使用测试设备（手机或平板）的相机或扫码工具扫描该二维码，系统会跳转到应用下载页面，按提示完成安装即可。</p>

            <h4>3.4 测试应用</h4>
            <p>安装完成后打开应用进行测试。测试时间要求如下：</p>
            <ul>
              <li><strong>游戏类应用</strong>（能正常运行的）：请至少游玩 <strong>10 分钟以上</strong>，确保事件数据充分上报到归因平台。</li>
              <li><strong>非游戏类应用</strong>（商城类、工具类、需要登录的应用等）：<strong>5 分钟以内</strong>能出事件即可入库。</li>
            </ul>
            <p>测试过程中，系统默认每 10 秒自动查询一次事件，但您可以使用「定时查询」功能自定义查询间隔。</p>

            <h4>3.5 定时查询功能</h4>
            <p>定时查询是一个自动化辅助功能，让您无需反复手动点击「查询事件」按钮。使用方法：</p>
            <ol>
              <li>在「定时查询」输入框中填入期望的查询间隔秒数（支持 1 ~ 3600 秒）。</li>
              <li>点击「开始定时」按钮，系统开始倒计时。</li>
              <li>倒计时结束后，系统会自动执行一次事件查询，结果会显示在右侧面板。</li>
              <li>如需取消，点击「取消」按钮即可。</li>
            </ol>

            <h4>3.6 查询事件与结果解读</h4>
            <p>点击「查询事件」按钮，系统会同时请求事件接口和四个归因平台（AppFlyer、Adjust、Singular、Tenjin）的归因数据。查询结果会显示在页面右侧面板：</p>
            <ul>
              <li><strong>有事件</strong>：显示最新的 currentTargetNum 值，并与原始值进行对比。如果数值发生变化，说明有新事件上报。同时会显示归因标签（如 appflyer、adjust 等），表示该应用在哪些归因平台有记录。</li>
              <li><strong>无事件</strong>：显示「✅ 无事件」，表示 currentTargetNum 未变化。此时您可以选择「冻结应用」将其标记为无事件状态。</li>
              <li><strong>查看 JSON</strong>：点击此按钮可以查看事件接口返回的原始 JSON 数据，用于调试和详细分析。</li>
            </ul>

            <h4>3.7 填写入库表单</h4>
            <p>查询到事件后，页面下方会出现入库表单，需要填写以下字段：</p>
            <ul>
              <li><strong>异常类型</strong>：从下拉列表中选择，包括「正常」「iOS16闪退」「iOS13/14/16均闪退」「需要iOS18以上」「地区不支持」「硬件版本过低」「超过10分钟0上报」「越狱检测」「其他」「验证已解决」等选项。请根据实际测试情况如实选择。</li>
              <li><strong>备注</strong>：可以手动输入文字描述，也可以从预设模板中选择。注意：如果使用了模板，手动输入的内容会被覆盖。</li>
              <li><strong>记录人</strong>：默认填充为您的登录名，一般无需修改。</li>
              <li><strong>记录日期</strong>：默认填充为当天日期，如需补录历史数据可手动修改。</li>
            </ul>
            <p>填写完毕后点击「直接入库」按钮，数据会立即写入 MySQL 数据库。入库成功后会显示绿色成功提示。</p>

            <h4>3.8 复测流程</h4>
            <p>当自动模式的库存耗尽时，系统会弹出提示框，您可以选择：</p>
            <ul>
              <li><strong>继续轮询</strong>：继续保持每 10 秒轮询一次等待新任务。</li>
              <li><strong>复测</strong>：从已测过的应用中随机抽取一条进行重新测试。复测会打开一个新的弹窗，显示该应用的二维码，您可以重新扫码测试并入库。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('u-manual')">
            <h3>四、手动模式 —— 特殊场景处理</h3>
            <p>手动模式适用于自动模式无法覆盖的特殊场景，例如：群里临时发的测试链接、需要指定 App ID 测试的应用等。</p>

            <h4>4.1 使用流程</h4>
            <ol>
              <li>进入「手动模式」页面，顶部会有一条黄色提示：「请注意！该模式下自动去重不生效」。</li>
              <li>在输入框中粘贴或输入 App ID（通常是在工作群里收到的那串标识符）。</li>
              <li>点击「生成」按钮或按回车键，系统会根据该 App ID 请求下载链接。</li>
              <li>后续流程与自动模式完全一致：扫码安装 → 测试 → 查询事件 → 填写表单 → 入库。</li>
            </ol>

            <h4>4.2 与自动模式的区别</h4>
            <table class="guide-table">
              <thead><tr><th>对比项</th><th>自动模式</th><th>手动模式</th></tr></thead>
              <tbody>
              <tr><td>任务来源</td><td>系统自动从任务队列拉取</td><td>用户手动输入 App ID</td></tr>
              <tr><td>自动去重</td><td>支持（需开启）</td><td>不生效</td></tr>
              <tr><td>轮询功能</td><td>支持</td><td>不支持</td></tr>
              <tr><td>复测功能</td><td>支持</td><td>不支持</td></tr>
              <tr><td>适用场景</td><td>日常批量测试</td><td>临时/指定应用测试</td></tr>
              </tbody>
            </table>

            <h4>4.3 请求地址说明</h4>
            <p>手动模式输入 App ID 后，系统会向以下地址发起请求：<code>https://d-reporter.de123.net/ad/play/task?appleid=您输入的AppID</code>。页面上会显示完整的请求地址供您确认。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('u-qrcode')">
            <h3>五、二维码生成器</h3>
            <p>这是一个独立的工具模块，用于将任意文本内容（通常是网址链接）转换为二维码图片。</p>

            <h4>5.1 使用方法</h4>
            <ol>
              <li>进入「二维码生成」页面。</li>
              <li>在文本输入框中粘贴或输入要转换的内容（如一个下载链接）。</li>
              <li>系统会实时自动生成二维码并显示在下方，无需点击任何按钮。</li>
              <li>您可以通过「二维码尺寸」输入框调整二维码的像素大小（128px ~ 512px，步进 32px）。</li>
            </ol>

            <h4>5.2 使用场景</h4>
            <ul>
              <li>临时需要将某个链接转为二维码供设备扫码安装。</li>
              <li>验证某个下载链接是否可以正常访问和安装。</li>
              <li>非标准化测试场景下快速生成二维码。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('u-export')">
            <h3>六、数据导出</h3>
            <p>数据导出功能用于将您名下已入库但尚未导出的记录打包生成文件并下载。导出流程采用异步处理机制，确保大数据量时也不会超时。</p>

            <h4>6.1 导出流程</h4>
            <ol>
              <li>进入「数据导出」页面，系统会自动查询您名下所有未导出的数据条数。</li>
              <li>如果有未导出数据，页面会显示条数和「一键导出」按钮。</li>
              <li>点击「一键导出」，后端会通过 RabbitMQ 消息队列异步处理导出任务。</li>
              <li>文件生成期间，页面会显示「文件生成中，请稍候...」的闪烁提示，系统每 2 秒轮询一次检查文件是否就绪。</li>
              <li>文件就绪后，页面会显示文件名和「下载文件」按钮，点击下载即可。</li>
              <li>下载完成后，服务器端的文件会自动删除以节省存储空间。</li>
            </ol>

            <h4>6.2 注意事项</h4>
            <ul>
              <li>导出是按用户隔离的，您只能导出自己名下的数据。</li>
              <li>如果当前没有未导出的数据，页面会显示「当前用户所有数据均已导出并下载」。</li>
              <li>导出文件为 HTML 和 TXT 两种格式，文件名包含时间戳以便区分。</li>
              <li>如果您使用的是测试环境，请务必及时导出数据！测试环境一旦出现数据损坏，概不负责。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('u-data')">
            <h3>七、数据看板</h3>
            <p>数据看板是您查看和管理已入库记录的核心页面。普通用户的数据看板提供以下功能：</p>

            <h4>7.1 数据浏览</h4>
            <p>进入数据看板后，系统会自动加载您名下的入库记录列表。每条记录显示以下字段：</p>
            <ul>
              <li>Bundle ID、应用名称、下载链接、归因平台、异常类型、备注、记录人、记录日期、是否冻结、是否已导出等。</li>
            </ul>
            <p>列表支持分页显示，默认每页 15 条，可通过翻页按钮浏览更多数据。页面底部会显示总记录数和当前页码。</p>

            <h4>7.2 筛选功能</h4>
            <p>数据看板提供多维度筛选能力：</p>
            <ul>
              <li><strong>归因平台筛选</strong>：可按 AppFlyer、Adjust、Singular、Tenjin 等归因平台过滤。</li>
              <li><strong>冻结状态筛选</strong>：可单独查看所有已冻结的记录。</li>
              <li><strong>记录人搜索</strong>：输入记录人姓名进行精确匹配。</li>
              <li><strong>日期搜索</strong>：按记录日期筛选。</li>
            </ul>

            <h4>7.3 编辑与删除</h4>
            <p>对于已有的记录，您可以：</p>
            <ul>
              <li><strong>编辑</strong>：点击编辑按钮后，该行数据变为可编辑状态，您可以修改异常类型、备注等字段，修改完毕后点击保存。</li>
              <li><strong>删除</strong>：点击删除按钮，系统会弹出确认对话框，确认后该条记录将被永久删除。此操作不可撤销，请谨慎操作。</li>
            </ul>

            <h4>7.4 重新测试（复测）</h4>
            <p>点击记录的「重新测试」按钮，系统会打开一个弹窗，显示该应用的二维码和当前事件状态。您可以重新扫码测试、查询事件变化、修改入库信息后重新入库。复测弹窗同样支持定时查询和冻结操作。</p>

            <h4>7.5 去重开关</h4>
            <p>在数据看板页面，您可以看到「自动去重」开关按钮。去重功能默认关闭，需要时点击开启，系统会要求您输入姓名进行确认。开启后，自动模式拉取任务时会自动跳过与您姓名关联的已测应用，防止重复测试。关闭后则不做去重检查。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('u-dedup')">
            <h3>八、自动去重机制</h3>
            <p>自动去重是系统的一项重要功能，用于防止同一个应用被多人重复测试，从而节省测试资源并提高数据质量。</p>

            <h4>8.1 工作原理</h4>
            <p>去重机制基于 Redis 实现。当某位用户开启去重后，系统会在 Redis 中记录该用户已测试过的应用 Bundle ID。后续拉取任务时，系统会自动跳过这些已测应用，直接获取下一个未测任务。</p>

            <h4>8.2 使用注意</h4>
            <ul>
              <li>去重功能仅在自动模式下生效，手动模式下不生效。</li>
              <li>去重是按用户维度隔离的，A 用户的去重记录不会影响 B 用户。</li>
              <li>去重开关的状态在服务端维护，即使刷新页面或重新登录也会保持。</li>
              <li>如果页面提示「⚠️ 重复应用」，说明当前拉取到的任务已经在您的去重记录中，请继续轮询获取下一个任务。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('u-rules')">
            <h3>九、测试规范与冻结标准</h3>

            <h4>9.1 游戏类应用测试规范</h4>
            <p>对于能够正常运行的游戏类应用，请务必玩够 <strong>10 分钟以上</strong> 再进行入库操作。这是为了确保事件数据能够充分上报到归因平台。如果玩了 10 分钟仍然 0 事件，可以直接冻结该应用。</p>

            <h4>9.2 非游戏类应用测试规范</h4>
            <p>对于无法运行的应用、商城类应用、需要登录的应用、工具类应用等，<strong>5 分钟以内能出事件就可以入库</strong>。这类应用通常不需要长时间测试。</p>

            <h4>9.3 直接冻结标准</h4>
            <p>以下情况无需抢救，请直接点击「冻结应用」按钮：</p>
            <ul>
              <li>需要登录但无法注册的应用</li>
              <li>必须付费才能使用的应用</li>
              <li>版本不兼容的应用（如要求 iOS 17 以上但设备不满足）</li>
              <li>地区不支持的应用（打开后提示不在服务区等）</li>
              <li>越狱检测的应用（检测到设备环境异常拒绝运行）</li>
              <li>网络太差完全进不去的应用</li>
              <li>玩了很久（超过 10 分钟）仍然 0 事件的应用</li>
            </ul>
            <p>冻结后，该记录会被标记为冻结状态，在数据看板中可以通过「仅看冻结」筛选快速找到。</p>

            <h4>9.4 测试环境特别提醒</h4>
            <p>如果您使用的是测试服务器（页面标题栏会标注「测试服」字样），请务必在完成测试后<strong>及时导出数据</strong>。测试环境的数据一旦损坏，将无法恢复，概不负责！</p>
          </div>

          <div class="guide-section" v-show="isExpanded('u-faq')">
            <h3>十、常见问题与排障</h3>

            <h4>Q1：页面显示「当前没有任何测试条目」怎么办？</h4>
            <p>这说明当前任务队列已空。您可以点击「刷新数据」再试一次，或者开启自动轮询等待新任务入库。如果长时间无任务，请联系项目负责人确认是否有新的测试需求。</p>

            <h4>Q2：扫描二维码后无法下载应用？</h4>
            <p>请检查：① 设备是否联网；② 设备系统版本是否满足应用要求；③ 应用是否已下架。如果确认设备和网络正常但仍无法下载，可能是链接已失效，请冻结该应用并获取下一条任务。</p>

            <h4>Q3：查询事件一直是 0 怎么办？</h4>
            <p>首先确认测试时间是否足够（游戏类至少 10 分钟）。如果时间足够仍无事件，可以尝试：① 关闭应用重新打开；② 切换网络后重试；③ 确认应用是否正常安装和运行。如果确认无法出事件，直接冻结即可。</p>

            <h4>Q4：入库失败怎么办？</h4>
            <p>入库操作会直接写入 MySQL 数据库，如果提示入库失败，通常是网络问题或后端服务异常。请：① 检查网络连接；② 刷新页面重试；③ 如果持续失败，联系开发者（邮箱：2448248501@qq.com）。</p>

            <h4>Q5：导出文件下载后打不开？</h4>
            <p>导出文件为 HTML 和 TXT 格式，请使用浏览器打开 HTML 文件，使用文本编辑器打开 TXT 文件。如果文件损坏，可以尝试重新导出一次。</p>

            <h4>Q6：被踢下线了怎么办？</h4>
            <p>被踢下线通常是管理员的操作，可能是因为长时间未操作、账户异常等原因。如果页面提示封禁，请等待封禁时间结束后重新登录。如有疑问请联系管理员。</p>
          </div>
        </div>

        <!-- ======================== 管理员教程 ======================== -->
        <div v-if="activeRole === 'admin'" class="guide-content">
          <div class="guide-toc">
            <h3>📑 目录</h3>
            <ul>
              <li><a @click="toggleSection('a-overview')">一、管理员角色概述</a></li>
              <li><a @click="toggleSection('a-panel')">二、管理员后台入口</a></li>
              <li><a @click="toggleSection('a-user-create')">三、创建用户</a></li>
              <li><a @click="toggleSection('a-user-manage')">四、用户列表管理</a></li>
              <li><a @click="toggleSection('a-user-kick')">五、踢下线与会话管理</a></li>
              <li><a @click="toggleSection('a-user-reset')">六、重置密码</a></li>
              <li><a @click="toggleSection('a-data-manage')">七、数据管理</a></li>
              <li><a @click="toggleSection('a-stats')">八、统计报表</a></li>
              <li><a @click="toggleSection('a-batch')">九、批量操作</a></li>
              <li><a @click="toggleSection('a-report')">十、日报查询</a></li>
              <li><a @click="toggleSection('a-best')">十一、管理最佳实践</a></li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('a-overview')">
            <h3>一、管理员角色概述</h3>
            <p>管理员（ADMIN）是系统中权限最高的常规角色，拥有普通用户的全部功能，并额外拥有以下管理能力：</p>
            <ul>
              <li>创建、删除系统用户（包括普通用户和其他管理员）</li>
              <li>查看在线用户状态，强制踢下线任意用户</li>
              <li>重置任意用户的密码</li>
              <li>管理所有入库数据（查看所有用户的记录、批量删除等）</li>
              <li>查看统计报表和日报数据</li>
            </ul>
            <p>管理员的导航栏中，「数据看板」会显示为「管理员看板」，点击进入的是功能更强大的管理员后台管理系统。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('a-panel')">
            <h3>二、管理员后台入口</h3>
            <p>管理员登录后，左侧导航栏的「数据看板」会自动变为「管理员看板」。点击进入后，您会看到两个 Tab 页签：</p>
            <ul>
              <li><strong>数据管理</strong>：用于查看和管理所有用户的入库数据，支持高级筛选、统计、批量操作等。</li>
              <li><strong>用户管理</strong>：用于创建新用户、查看用户列表、管理在线状态、重置密码等。</li>
            </ul>
            <p>管理员后台默认进入「数据管理」Tab，您可以随时点击切换。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('a-user-create')">
            <h3>三、创建用户</h3>

            <h4>3.1 操作步骤</h4>
            <ol>
              <li>进入管理员看板，切换到「用户管理」Tab。</li>
              <li>在「创建新用户」卡片中，依次填写以下信息：
                <ul>
                  <li><strong>姓名</strong>：用户的真实姓名，将用于系统内显示和操作记录关联。</li>
                  <li><strong>密码</strong>：用户的登录密码，建议使用强密码（包含大小写字母、数字和特殊字符）。</li>
                  <li><strong>账户类型</strong>：选择「普通用户 (USER)」或「管理员 (ADMIN)」。</li>
                </ul>
              </li>
              <li>点击「创建用户」按钮，系统会生成一个唯一的 UID（格式为 U + 10位随机字符）。</li>
              <li>创建成功后，页面会显示创建的 UID，请将 UID 和密码告知对应用户。</li>
            </ol>

            <h4>3.2 注意事项</h4>
            <ul>
              <li>UID 由系统自动生成，不可自定义，格式为 U 开头加 10 位大写字母和数字的随机组合。</li>
              <li>创建后请立即告知用户其 UID 和密码，因为 UID 无法再次查看（除非在用户列表中查找）。</li>
              <li>账户类型一旦创建，后续无法修改。如需变更，只能删除后重新创建。</li>
              <li>创建用户时会使用 BCrypt 算法对密码进行加密存储，即使数据库泄露也无法还原明文密码。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('a-user-manage')">
            <h3>四、用户列表管理</h3>

            <h4>4.1 查看用户列表</h4>
            <p>在「用户管理」Tab 下方的「用户列表」区域，您可以看到系统中所有用户的详细信息，包括：</p>
            <ul>
              <li><strong>UID</strong>：用户的唯一标识符。</li>
              <li><strong>姓名</strong>：用户注册时填写的姓名。</li>
              <li><strong>类型</strong>：账户角色（管理员 / 用户）。</li>
              <li><strong>在线状态</strong>：实时显示用户当前是否在线（绿色圆点表示在线，灰色表示离线）。</li>
            </ul>
            <p>用户列表每 15 秒自动刷新一次，确保在线状态的实时性。</p>

            <h4>4.2 删除用户</h4>
            <p>点击用户行中的「🗑 删除」按钮，系统会弹出确认对话框。确认后该用户将被永久删除，其账户将无法再登录。请注意：</p>
            <ul>
              <li>删除用户不会删除该用户已入库的数据记录。</li>
              <li>删除操作不可撤销，请谨慎操作。</li>
              <li>如果用户当前在线，删除后其会话不会立即失效（建议先踢下线再删除）。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('a-user-kick')">
            <h3>五、踢下线与会话管理</h3>

            <h4>5.1 踢下线操作</h4>
            <p>对于在线用户，您可以点击「⚡ 踢下线」按钮强制终止其会话。踢下线时可以选择是否同时封禁账户：</p>
            <ul>
              <li>在用户列表右上角的「踢下线封禁」输入框中设置封禁秒数（默认 300 秒）。</li>
              <li>如果封禁秒数设为 0，则仅踢下线不封禁，用户可以立即重新登录。</li>
              <li>如果封禁秒数大于 0，用户在封禁期间内无法登录，系统会提示剩余封禁时间。</li>
            </ul>

            <h4>5.2 会话管理原理</h4>
            <p>系统使用双重会话管理机制：服务端的 HttpSession 和 Redis 中的会话映射。踢下线操作会同时执行以下动作：</p>
            <ol>
              <li>使服务端的 HttpSession 失效。</li>
              <li>删除 Redis 中的会话映射记录。</li>
              <li>如果设置了封禁时间，在 Redis 中写入封禁键（ban:uid:xxx），带 TTL 自动过期。</li>
            </ol>
            <p>这意味着被踢下线的用户会立即失去操作权限，页面会跳转到登录页面。</p>

            <h4>5.3 使用场景</h4>
            <ul>
              <li>用户长时间挂机不操作，需要释放资源。</li>
              <li>发现用户有违规操作行为。</li>
              <li>需要强制用户重新登录以刷新权限。</li>
              <li>安全审计需要临时冻结某个账户。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('a-user-reset')">
            <h3>六、重置密码</h3>

            <h4>6.1 操作步骤</h4>
            <ol>
              <li>在「用户管理」Tab 的「重置账户密码」卡片中操作。</li>
              <li>输入要重置的用户 UID。</li>
              <li>输入新密码。</li>
              <li>点击「重置密码并踢下线」按钮。</li>
              <li>系统会立即更新该用户的密码（BCrypt 加密存储），同时将其踢下线。</li>
            </ol>

            <h4>6.2 设计说明</h4>
            <p>重置密码操作会同时踢用户下线，这是一个安全设计：确保旧密码的会话立即失效，用户必须使用新密码重新登录。重置完成后，请及时将新密码告知对应用户。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('a-data-manage')">
            <h3>七、数据管理</h3>

            <h4>7.1 高级搜索</h4>
            <p>管理员的数据管理页面提供强大的高级搜索功能，支持以下筛选条件组合：</p>
            <ul>
              <li><strong>日期范围</strong>：设置起止日期，查看指定时间段内的记录。</li>
              <li><strong>Bundle ID</strong>：精确搜索特定应用的记录。</li>
              <li><strong>关键词搜索</strong>：在备注等字段中模糊搜索。</li>
              <li><strong>异常类型</strong>：按异常类型精确筛选。</li>
              <li><strong>归因平台</strong>：按 AppFlyer / Adjust / Singular / Tenjin 筛选。</li>
              <li><strong>记录人</strong>：查看特定用户录入的数据。</li>
              <li><strong>冻结状态</strong>：可单独查看已冻结或仅未冻结的记录。</li>
              <li><strong>导出状态</strong>：筛选已导出或未导出的记录。</li>
            </ul>
            <p>所有筛选条件可以自由组合，实现精确的数据定位。点击「重置」按钮可一键清空所有筛选条件。</p>

            <h4>7.2 数据汇总</h4>
            <p>搜索结果上方会显示数据汇总信息，包括总记录数、已导出数、待导出数等关键指标。搜索结果下方还会显示归因统计，如各归因平台的记录占比等。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('a-stats')">
            <h3>八、统计报表</h3>
            <p>管理员后台会自动加载全局统计数据，包括：</p>
            <ul>
              <li><strong>总记录数</strong>：系统中所有入库记录的总数。</li>
              <li><strong>已导出数</strong>：已经完成导出的记录数。</li>
              <li><strong>待处理数</strong>：尚未导出的记录数。</li>
              <li><strong>异常类型分布</strong>：各类异常类型的记录数量分布。</li>
              <li><strong>记录人分布</strong>：各用户录入数据的数量统计。</li>
            </ul>
            <p>这些数据帮助您全面掌握团队的测试进度和数据质量。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('a-batch')">
            <h3>九、批量操作</h3>

            <h4>9.1 批量删除</h4>
            <p>管理员支持批量删除记录：在数据列表中勾选要删除的记录（可以点击表头的全选框一次性选中当前页所有记录），然后点击「批量删除」按钮。系统会弹出确认对话框，显示选中的记录数量，确认后执行批量删除。</p>
            <p><strong>重要提醒</strong>：批量删除操作不可撤销！执行前请务必确认选中的记录是正确的。</p>

            <h4>9.2 全选与反选</h4>
            <p>点击表头的复选框可以全选/取消全选当前页的所有记录。配合分页功能，可以逐页选择要操作的记录。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('a-report')">
            <h3>十、日报查询</h3>
            <p>管理员可以按日期查询日报统计数据，了解特定日期的测试成果。</p>

            <h4>10.1 操作步骤</h4>
            <ol>
              <li>在数据管理页面找到日报查询区域。</li>
              <li>选择要查询的日期。</li>
              <li>点击「查询日报」按钮。</li>
              <li>系统会弹出一个模态窗口，显示该日期的详细统计数据，包括：
                <ul>
                  <li>合格记录数（qualifiedCount）</li>
                  <li>各归因平台的记录数和占比</li>
                  <li>各异常类型的分布</li>
                  <li>记录人维度的统计</li>
                </ul>
              </li>
            </ol>

            <h4>10.2 合格标准</h4>
            <p>日报中的「合格记录」指的是满足以下条件的记录：异常类型为「正常」或「验证已解决」，且未被冻结。这些记录代表测试通过并成功上报了事件的应用。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('a-best')">
            <h3>十一、管理最佳实践</h3>
            <ul>
              <li><strong>定期审查用户列表</strong>：检查是否有已离职员工的账户需要删除，是否有异常登录行为。</li>
              <li><strong>及时清理数据</strong>：对于已导出的历史数据，可以考虑定期归档或清理，保持数据库精简。</li>
              <li><strong>合理设置封禁时间</strong>：对于轻微违规的用户，短时间封禁（如 300 秒）即可；对于严重违规，可以设置较长的封禁时间。</li>
              <li><strong>密码管理</strong>：创建用户时设置强密码，并在首次登录后要求用户修改。定期提醒用户更换密码。</li>
              <li><strong>关注统计数据</strong>：通过统计报表监控团队的测试效率，发现异常数据（如某用户入库量异常低或异常高）及时沟通。</li>
              <li><strong>备份意识</strong>：虽然系统有完善的数据存储机制，但管理员仍应定期关注数据备份情况，防患于未然。</li>
            </ul>
          </div>
        </div>

        <!-- ======================== 开发者教程 ======================== -->
        <div v-if="activeRole === 'developer'" class="guide-content">
          <div class="guide-toc">
            <h3>📑 目录</h3>
            <ul>
              <li><a @click="toggleSection('d-overview')">一、开发者角色概述</a></li>
              <li><a @click="toggleSection('d-permissions')">二、权限范围与功能矩阵</a></li>
              <li><a @click="toggleSection('d-devmode')">三、开发者模式详解</a></li>
              <li><a @click="toggleSection('d-protest')">四、专业测试工作流</a></li>
              <li><a @click="toggleSection('d-attr')">五、四平台归因查询</a></li>
              <li><a @click="toggleSection('d-history')">六、今日测试历史</a></li>
              <li><a @click="toggleSection('d-governance')">七、数据建表与治理</a></li>
              <li><a @click="toggleSection('d-monitor')">八、系统监控面板</a></li>
              <li><a @click="toggleSection('d-admin')">九、管理员功能（用户管理）</a></li>
              <li><a @click="toggleSection('d-arch')">十、系统架构与技术栈</a></li>
              <li><a @click="toggleSection('d-deploy')">十一、部署与运维</a></li>
              <li><a @click="toggleSection('d-trouble')">十二、故障排查手册</a></li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('d-overview')">
            <h3>一、开发者角色概述</h3>
            <p>开发者（DEVELOPER）是系统中的最高权限角色，拥有普通用户和管理员的全部功能，并额外享有以下专属能力：</p>
            <ul>
              <li><strong>开发者模式</strong>：包含专业测试工具（合并了自动/手动模式的高级功能）和四平台独立归因查询面板。</li>
              <li><strong>数据建表与治理</strong>：可以直接在数据库中创建表、删除表、添加/修改/删除列、执行自定义 SQL。</li>
              <li><strong>系统监控面板</strong>：实时查看服务器 CPU/内存状态、Redis 数据库监控、应用日志。</li>
              <li><strong>管理员全部权限</strong>：包括用户管理、数据管理、统计报表等所有管理员功能。</li>
            </ul>
            <p>开发者角色在用户列表中显示为绿色的「root账户」标签，代表其最高权限地位。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('d-permissions')">
            <h3>二、权限范围与功能矩阵</h3>
            <p>以下是三种角色的完整功能权限对比：</p>
            <table class="guide-table">
              <thead><tr><th>功能模块</th><th>普通用户</th><th>管理员</th><th>开发者</th></tr></thead>
              <tbody>
              <tr><td>自动模式</td><td>✅</td><td>✅</td><td>✅</td></tr>
              <tr><td>手动模式</td><td>✅</td><td>✅</td><td>✅</td></tr>
              <tr><td>二维码生成器</td><td>✅</td><td>✅</td><td>✅</td></tr>
              <tr><td>数据导出</td><td>✅（仅自己）</td><td>✅（仅自己）</td><td>✅（仅自己）</td></tr>
              <tr><td>数据看板</td><td>✅（基础）</td><td>✅（管理员看板）</td><td>✅（管理员看板）</td></tr>
              <tr><td>自动去重开关</td><td>✅</td><td>❌</td><td>✅</td></tr>
              <tr><td>用户管理</td><td>❌</td><td>✅</td><td>✅</td></tr>
              <tr><td>数据批量操作</td><td>❌</td><td>✅</td><td>✅</td></tr>
              <tr><td>统计报表/日报</td><td>❌</td><td>✅</td><td>✅</td></tr>
              <tr><td>开发者模式</td><td>❌</td><td>❌</td><td>✅</td></tr>
              <tr><td>数据建表与治理</td><td>❌</td><td>❌</td><td>✅</td></tr>
              <tr><td>系统监控面板</td><td>❌</td><td>❌</td><td>✅</td></tr>
              </tbody>
            </table>
            <p>开发者登录后，导航栏会额外显示「开发者模式」和「监控面板」两个入口。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('d-devmode')">
            <h3>三、开发者模式详解</h3>
            <p>开发者模式是专为开发者设计的高级功能模块，包含两个子 Tab：</p>
            <ul>
              <li><strong>专业测试</strong>：整合了自动模式和手动模式的全部功能，并增加了独立的四平台归因查询面板和今日测试历史记录。</li>
              <li><strong>数据建表和治理</strong>：提供数据库表结构管理功能，支持 DDL 操作。</li>
            </ul>
            <p>开发者模式页面顶部会显示一个紫色的「🛠️ 开发者模式」徽章，以区分普通用户界面。右上角还有「📖 使用指南」按钮，可以快速查看开发者模式的使用说明。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('d-protest')">
            <h3>四、专业测试工作流</h3>
            <p>专业测试 Tab 融合了自动模式和手动模式的所有能力，并提供更多调试信息。</p>

            <h4>4.1 获取任务</h4>
            <p>您可以通过两种方式获取测试任务：</p>
            <ul>
              <li><strong>刷新数据</strong>：与自动模式相同，从任务队列中拉取下一条待测应用。</li>
              <li><strong>输入 App ID</strong>：与手动模式相同，在输入框中填入 App ID 后点击生成。</li>
            </ul>
            <p>两种方式可以混合使用，灵活度更高。</p>

            <h4>4.2 测试与事件查询</h4>
            <p>与自动/手动模式一致，包括二维码展示、定时查询、事件对比、归因标签显示等。额外地，专业测试模式会显示更详细的事件信息，方便调试。</p>

            <h4>4.3 入库表单</h4>
            <p>入库表单与自动模式完全一致，包括异常类型选择、备注（支持模板）、记录人和记录日期。开发者模式下同样支持复测流程。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('d-attr')">
            <h3>五、四平台归因查询</h3>
            <p>这是开发者模式的核心特色功能。在专业测试页面下方，有四个独立的归因查询面板，分别对应四大归因平台：</p>

            <h4>5.1 AppFlyer 查询</h4>
            <p>输入 Bundle ID 后点击查询，系统会请求 AppFlyer 的归因 API，返回该应用在 AppFlyer 平台上的归因记录。返回数据包括归因类型、归因来源、时间戳等字段。查询结果会以卡片形式展示，每条记录显示所有字段和对应的值。</p>

            <h4>5.2 Adjust 查询</h4>
            <p>操作方式与 AppFlyer 相同。Adjust 平台的归因数据格式略有不同，返回的字段包括 tracker、network、campaign 等 Adjust 特有的归因维度。</p>

            <h4>5.3 Singular 查询</h4>
            <p>Singular 平台的归因查询，返回数据包含 Singular 的归因模型相关字段。每个平台的查询是独立的，可以分别查询不同 Bundle ID 的归因数据。</p>

            <h4>5.4 Tenjin 查询</h4>
            <p>Tenjin 平台的归因查询。四个平台的面板完全独立，您可以同时对比同一应用在四个平台上的归因情况，这对于排查归因差异问题非常有帮助。</p>

            <h4>5.5 使用技巧</h4>
            <ul>
              <li>四个面板的 Bundle ID 输入框是独立的，可以同时查询不同应用在不同平台上的归因。</li>
              <li>查询结果会显示记录条数（如 "3条"），方便快速判断数据量。</li>
              <li>如果某个平台返回「无数据」，说明该应用在对应平台上没有归因记录，可能是该应用未接入此归因平台。</li>
              <li>每个面板支持独立查询，互不影响。可以只查其中一两个平台，无需全部查询。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('d-history')">
            <h3>六、今日测试历史</h3>
            <p>专业测试页面下方有一个「今日测试历史」区域，记录您今天在开发者模式下所有测试过的应用。每条历史记录包含应用信息、测试结果、入库状态等。</p>

            <h4>6.1 功能说明</h4>
            <ul>
              <li><strong>自动记录</strong>：每次测试并入库后，记录会自动追加到历史列表中。</li>
              <li><strong>Redis 存储</strong>：历史数据存储在 Redis 中（非 MySQL），按天自动清理。</li>
              <li><strong>Redis 状态指示</strong>：历史区域右上角会显示 Redis 连接状态（绿色 "ok" 或黄色 "warn"），帮助您判断历史功能是否正常。</li>
              <li><strong>刷新/清空</strong>：可以手动刷新历史列表，或点击「清空」按钮清除所有历史记录。</li>
              <li><strong>删除单条</strong>：每条历史记录右侧有删除按钮，可以删除特定的历史条目。</li>
            </ul>

            <h4>6.2 使用场景</h4>
            <ul>
              <li>回顾今天已经测试过哪些应用，避免遗漏。</li>
              <li>查看某个应用的测试时间线和事件变化。</li>
              <li>在 Redis 异常时通过 Redis 状态指示器快速发现问题。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('d-governance')">
            <h3>七、数据建表与治理</h3>
            <p>这是开发者模式中最强大的功能之一，允许您直接管理数据库的表结构。切换到「数据建表和治理」Tab 后可以使用。</p>

            <h4>7.1 查看表列表</h4>
            <p>进入该 Tab 后，系统会自动加载当前数据库中的所有表。每个表显示表名和基本信息，点击可以查看详情。</p>

            <h4>7.2 查看表结构</h4>
            <p>点击某个表后，可以查看该表的完整结构定义（DESCRIBE），包括列名、数据类型、是否允许为空、默认值、索引等信息。</p>

            <h4>7.3 创建新表</h4>
            <p>提供可视化的建表界面，您需要指定：</p>
            <ul>
              <li><strong>表名</strong>：新表的名称。</li>
              <li><strong>列定义</strong>：每列需要指定列名、数据类型（如 VARCHAR、INT、TEXT、DATETIME 等）、是否允许为空、默认值等。</li>
            </ul>
            <p>系统会将您的定义转换为 CREATE TABLE SQL 语句并执行。</p>

            <h4>7.4 删除表</h4>
            <p>选择要删除的表，系统会执行 DROP TABLE 操作。<strong>注意：此操作不可逆，表中的数据将全部丢失！</strong></p>

            <h4>7.5 添加列</h4>
            <p>为已有表添加新的列，需要指定列名和数据类型定义。系统会执行 ALTER TABLE ADD COLUMN 语句。</p>

            <h4>7.6 修改列</h4>
            <p>修改已有列的数据类型或其他属性。系统会执行 ALTER TABLE MODIFY COLUMN 语句。修改列时请注意数据兼容性，避免数据丢失。</p>

            <h4>7.7 删除列</h4>
            <p>删除已有表中的某一列。系统会执行 ALTER TABLE DROP COLUMN 语句。<strong>该列的数据将永久丢失！</strong></p>

            <h4>7.8 执行自定义 SQL</h4>
            <p>这是最灵活的功能，允许您输入任意 SQL 语句并执行。支持 SELECT、INSERT、UPDATE、DELETE、ALTER 等所有 SQL 操作。执行结果会以表格形式展示。</p>
            <p><strong>⚠️ 安全警告</strong>：自定义 SQL 功能非常强大，但也意味着误操作可能造成严重后果。执行前请务必仔细检查 SQL 语句，特别是 DROP、DELETE、TRUNCATE 等破坏性操作。建议在生产环境中使用此功能前先做好数据备份。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('d-monitor')">
            <h3>八、系统监控面板</h3>
            <p>系统监控面板是开发者的运维利器，提供三个子 Tab：</p>

            <h4>8.1 服务器状态</h4>
            <p>实时展示服务器的硬件和运行状态信息：</p>
            <ul>
              <li><strong>操作系统</strong>：显示服务器 OS 名称和版本。</li>
              <li><strong>系统架构</strong>：显示 CPU 架构（如 amd64、aarch64）。</li>
              <li><strong>CPU 信息</strong>：显示 CPU 型号和核心数。</li>
              <li><strong>CPU 占用</strong>：实时 CPU 使用率百分比，超过 80% 会标红警告。底层使用 oshi 库采集系统指标。</li>
              <li><strong>内存信息</strong>：显示总内存、已用内存、可用内存和使用率百分比。超过 85% 会标红警告。</li>
              <li><strong>服务运行时长</strong>：显示 Java 进程自启动以来的运行时间。</li>
            </ul>
            <p>页面下方还有内存使用率和 CPU 使用率的可视化进度条，直观展示资源消耗情况。</p>

            <h4>8.2 实时日志</h4>
            <p>实时查看后端应用的运行日志，功能包括：</p>
            <ul>
              <li><strong>自动刷新</strong>：默认每 3 秒自动拉取最新日志，保持实时监控状态。</li>
              <li><strong>日志级别筛选</strong>：可以按 ALL / INFO / WARN / ERROR / DEBUG 级别过滤日志。</li>
              <li><strong>暂停/恢复</strong>：点击暂停按钮可以冻结日志刷新，方便查看某段特定日志。</li>
              <li><strong>自动滚动</strong>：勾选后日志会自动滚动到最新行。</li>
              <li><strong>语法高亮</strong>：不同级别的日志会以不同颜色显示（ERROR 红色、WARN 黄色、INFO 蓝色、DEBUG 紫色），方便快速定位问题。</li>
              <li><strong>行数统计</strong>：底部显示当前加载的日志总行数。</li>
            </ul>
            <p>日志数据来源于服务器上的日志文件（默认路径 /data/logs/app.log），系统会从文件末尾读取最新的 N 行日志。</p>

            <h4>8.3 Redis 监控</h4>
            <p>全面监控 Redis 数据库的运行状态：</p>
            <ul>
              <li><strong>服务器信息</strong>：显示 Redis 版本、已用内存、峰值内存、当前连接数、运行时长。</li>
              <li><strong>数据库概览</strong>：以卡片网格形式展示所有 16 个 Redis 数据库（DB0 ~ DB15），每个卡片显示该数据库中的 key 数量。空数据库会以半透明样式显示。</li>
              <li><strong>键查看器</strong>：点击任意数据库卡片，会展开该数据库的键列表，显示每个键的名称、数据类型（string/list/set/zset/hash）和 TTL（过期时间）。不同类型会以不同颜色的标签区分。</li>
            </ul>
            <p>Redis 监控功能对于排查缓存问题、检查会话状态、调试去重功能等场景非常有用。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('d-admin')">
            <h3>九、管理员功能（用户管理）</h3>
            <p>开发者拥有管理员的全部权限，包括用户管理。在管理员看板中可以执行所有管理员操作。特别地，开发者在创建用户时可以选择三种账户类型：普通用户（USER）、管理员（ADMIN）、开发者（DEVELOPER）。</p>
            <p>这意味着开发者可以创建新的开发者账户，请谨慎使用此权限。建议仅在确实需要时才创建新的开发者账户。</p>
          </div>

          <div class="guide-section" v-show="isExpanded('u-rules')">
            <h3>测试规范与冻结标准</h3>

            <div class="rules-card">
              <h4>📋 测试规范（重要）</h4>

              <div class="rule-group">
                <h5>🎮 游戏类应用（能正常运行）</h5>
                <p>请玩够 <strong>10 分钟以上</strong>再进行入库操作，确保事件数据充分上报。</p>
              </div>

              <div class="rule-group">
                <h5>📦 其他类型应用</h5>
                <p>如无法运行的应用、商城类、需要登录的应用、其他工具类应用等，<strong>5 分钟以内能出事件就可以入库</strong>。</p>
              </div>

              <div class="rule-group rule-warning">
                <h5>⚠️ 测试服特别提醒</h5>
                <p>如果您使用的是测试服（在标题一栏会用括号标记测试服三字），请记得一定及时导出数据，测试环境一旦出现数据损坏概不负责！！！</p>
              </div>
            </div>

            <div class="rules-card rules-card-danger">
              <h4>🧊 直接冻结（无需抢救）</h4>
              <p class="freeze-intro">以下情况不用抢救，直接冻结：</p>
              <ul class="freeze-list">
                <li>需要登录但无法注册的应用</li>
                <li>必须付费才能使用的应用</li>
                <li>版本不兼容（如操作系统要求 iOS 17 以上）的应用</li>
                <li>地区不支持的应用</li>
                <li>越狱检测的应用</li>
                <li>网络太撇了进不去的应用</li>
                <li>耍了很久没得事件的应用（10分钟0事件）</li>
              </ul>
            </div>
          </div>

          <div class="guide-section" v-show="isExpanded('d-arch')">
            <h3>十、系统架构与技术栈</h3>
            <p>作为开发者，了解系统的整体架构对于日常维护和故障排查至关重要。</p>

            <h4>10.1 前端架构</h4>
            <ul>
              <li><strong>框架</strong>：Vue 3（Composition API + &lt;script setup&gt; 语法）</li>
              <li><strong>构建工具</strong>：Vite</li>
              <li><strong>HTTP 客户端</strong>：原生 fetch API（封装了 fetchWithTimeout 和 safeJson 工具函数）</li>
              <li><strong>二维码生成</strong>：qrcode 库</li>
              <li><strong>路由</strong>：无路由库，采用组件动态切换（component :is）</li>
              <li><strong>样式</strong>：纯 CSS（scoped），CSS 变量定义主题色</li>
            </ul>

            <h4>10.2 后端架构</h4>
            <ul>
              <li><strong>框架</strong>：Spring Boot 3.2.0</li>
              <li><strong>JDK</strong>：OpenJDK 26</li>
              <li><strong>数据库访问</strong>：JdbcTemplate + MyBatis-Plus 3.5.5</li>
              <li><strong>数据库</strong>：MySQL 8.0</li>
              <li><strong>缓存/会话</strong>：Spring Data Redis（Lettuce 客户端）</li>
              <li><strong>消息队列</strong>：RabbitMQ（Spring AMQP）</li>
              <li><strong>连接池</strong>：HikariCP</li>
              <li><strong>系统监控</strong>：oshi 6.4.10</li>
              <li><strong>API 文档</strong>：SpringDoc OpenAPI（Swagger UI）</li>
              <li><strong>密码加密</strong>：BCrypt（Spring Security Crypto）</li>
              <li><strong>限流</strong>：Bucket4j 8.15</li>
              <li><strong>本地缓存</strong>：Caffeine 3.1.8</li>
              <li><strong>AOP</strong>：AspectJ（用于日志执行时间注解）</li>
            </ul>

            <h4>10.3 关键设计模式</h4>
            <ul>
              <li><strong>拦截器链</strong>：AuthInterceptor（认证）→ RateLimitInterceptor（限流）→ UserContextInterceptor（用户上下文），按 order 顺序执行。</li>
              <li><strong>消息队列异步处理</strong>：导出功能通过 RabbitMQ 实现异步处理，避免 HTTP 请求超时。</li>
              <li><strong>分布式锁</strong>：使用 Redis 实现分布式锁（RedisDistributedLock），用于多 Pod 环境下的并发控制。</li>
              <li><strong>多 Pod 会话一致性</strong>：通过 Redis 存储会话映射，确保多实例部署下的会话一致性。</li>
              <li><strong>缓存策略</strong>：CacheStrategyService 提供多级缓存策略，结合 Caffeine 本地缓存和 Redis 分布式缓存。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('d-deploy')">
            <h3>十一、部署与运维</h3>

            <h4>11.1 项目构建</h4>
            <p>后端使用 Maven 构建，执行 <code>mvn clean package</code> 生成可执行 JAR 文件。前端使用 Vite 构建，执行 <code>npm run build</code> 生成静态资源到 dist 目录。</p>

            <h4>11.2 配置文件</h4>
            <p>核心配置文件为 <code>application.yml</code>，包含以下关键配置：</p>
            <ul>
              <li><strong>服务端口</strong>：默认 8080</li>
              <li><strong>MySQL 数据源</strong>：数据库名 test_data，使用 HikariCP 连接池（最大 20 连接）</li>
              <li><strong>Redis</strong>：默认连接 10.1.13.140:6379，使用 DB 1</li>
              <li><strong>RabbitMQ</strong>：默认连接 10.1.13.140:5672</li>
              <li><strong>日志</strong>：输出到 /data/logs/app.log，滚动策略为单文件最大 100MB，保留 30 天，总容量上限 3GB</li>
              <li><strong>导出目录</strong>：/data/export</li>
            </ul>

            <h4>11.3 Docker 部署</h4>
            <p>项目在 <code>src/main/resources/static/</code> 目录下提供了 Dockerfile、docker-compose.yml 和 nginx.conf，支持容器化部署。nginx 用于反向代理前端静态资源和后端 API 请求。</p>

            <h4>11.4 日常运维要点</h4>
            <ul>
              <li><strong>日志监控</strong>：定期检查日志文件大小，虽然已配置自动滚动，但磁盘空间仍需关注。</li>
              <li><strong>Redis 内存</strong>：通过监控面板关注 Redis 内存使用，避免内存溢出。</li>
              <li><strong>数据库备份</strong>：建议定期 mysqldump 备份 test_data 数据库。</li>
              <li><strong>导出文件清理</strong>：系统配置了 ExportFileCleaner 自动清理过期的导出文件，但建议定期检查 /data/export 目录。</li>
              <li><strong>RabbitMQ 队列</strong>：关注队列积压情况，如果导出消息堆积，可能需要增加消费者并发数。</li>
            </ul>
          </div>

          <div class="guide-section" v-show="isExpanded('d-trouble')">
            <h3>十二、故障排查手册</h3>

            <h4>12.1 用户反馈「无法登录」</h4>
            <ol>
              <li>检查 Redis 连接是否正常（通过监控面板查看）。</li>
              <li>检查该用户是否被封禁：在 Redis 中查看 ban:uid:xxx 键。</li>
              <li>检查数据库中的用户记录是否存在：在数据治理中执行 SELECT * FROM sys_user WHERE uid='xxx'。</li>
              <li>检查会话是否过期：查看 Redis 中的 session:uid:xxx 键。</li>
            </ol>

            <h4>12.2 用户反馈「入库失败」</h4>
            <ol>
              <li>检查 MySQL 连接是否正常：查看日志中是否有数据库连接异常。</li>
              <li>检查 HikariCP 连接池状态：是否达到最大连接数。</li>
              <li>检查目标表是否存在、表结构是否匹配。</li>
              <li>查看后端日志中的具体错误信息。</li>
            </ol>

            <h4>12.3 导出功能异常</h4>
            <ol>
              <li>检查 RabbitMQ 连接状态和队列健康情况。</li>
              <li>检查消费者是否正常运行：查看日志中是否有消费者异常。</li>
              <li>检查 /data/export 目录的磁盘空间是否充足。</li>
              <li>如果文件已生成但下载失败，检查 nginx 配置中的静态文件路径。</li>
            </ol>

            <h4>12.4 CPU/内存告警</h4>
            <ol>
              <li>通过监控面板查看具体的资源使用情况。</li>
              <li>查看实时日志中是否有异常循环或内存泄漏的线索。</li>
              <li>如果是正常业务高峰导致的短暂升高，可以观察是否自行回落。</li>
              <li>如果持续高位，考虑重启服务或扩容。</li>
            </ol>

            <h4>12.5 Redis 连接异常</h4>
            <ol>
              <li>检查 Redis 服务是否运行：通过监控面板查看 Redis 状态。</li>
              <li>检查网络连通性：确认应用服务器能访问 Redis 端口。</li>
              <li>检查 Redis 内存使用：如果达到 maxmemory，Redis 会拒绝写入。</li>
              <li>检查 Redis 配置：确认密码（如有）和数据库编号配置正确。</li>
            </ol>

            <h4>12.6 归因查询失败</h4>
            <ol>
              <li>检查外部归因 API 的可达性：归因查询依赖外部服务（d-reporter.de123.net），如果外部服务不可用会导致查询失败。</li>
              <li>检查 Bundle ID 格式是否正确。</li>
              <li>查看后端日志中是否有外部 API 调用的超时或错误信息。</li>
            </ol>
          </div>
        </div>

        <div class="guide-divider"></div>

        <div class="guide-footer">
          <div class="guide-footer-signature">
            <p>本系统由</p>
            <p class="guide-dev-name">蓝黄金刚鹦鹉</p>
            <p>开发维护</p>
            <div class="guide-contact">
              <p>📧 联系开发者：2448248501@qq.com</p>
              <p>🏢 深圳市慧动创想科技有限公司</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>.guide-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 15, 26, 0.6);
  backdrop-filter: blur(6px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: guideFadeIn 0.3s ease;
}

@keyframes guideFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes guideSlideUp {
  from { transform: translateY(40px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

@keyframes guidePulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(99, 102, 241, 0.3); }
  50% { box-shadow: 0 0 0 8px rgba(99, 102, 241, 0); }
}

.guide-modal {
  background: #fff;
  border-radius: 20px;
  max-width: 800px;
  width: 94%;
  max-height: 88vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 32px 80px rgba(0, 0, 0, 0.3), 0 0 1px rgba(0, 0, 0, 0.1);
  animation: guideSlideUp 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.guide-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 28px 18px;
  border-bottom: 1px solid #e8eaef;
  background: linear-gradient(135deg, #f8f9fc 0%, #fff 100%);
  flex-shrink: 0;
}

.guide-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.guide-header-icon {
  font-size: 32px;
  line-height: 1;
}

.guide-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
  letter-spacing: -0.3px;
}

.guide-header-sub {
  font-size: 12px;
  color: #64748b;
  margin-top: 3px;
  font-weight: 500;
}

.guide-close-btn {
  background: none;
  border: 1px solid transparent;
  font-size: 18px;
  cursor: pointer;
  color: #94a3b8;
  transition: all 0.2s ease;
  padding: 6px 10px;
  border-radius: 8px;
  line-height: 1;
}

.guide-close-btn:hover {
  color: #ef4444;
  background: #fef2f2;
  border-color: #fecaca;
}

.guide-body {
  padding: 20px 28px 28px;
  overflow-y: auto;
  flex: 1;
}

.guide-body::-webkit-scrollbar {
  width: 6px;
}

.guide-body::-webkit-scrollbar-track {
  background: transparent;
}

.guide-body::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.guide-body::-webkit-scrollbar-thumb:hover {
  background: #9ca3af;
}

.guide-welcome {
  background: linear-gradient(135deg, #eef2ff 0%, #faf5ff 100%);
  border: 1px solid #e0e7ff;
  border-radius: 14px;
  padding: 20px 22px;
  margin-bottom: 20px;
  line-height: 1.8;
  font-size: 14px;
  color: #374151;
}

.guide-welcome p {
  margin-bottom: 8px;
}

.guide-welcome p:last-child {
  margin-bottom: 0;
}

.guide-welcome strong {
  color: #1a1a2e;
}

.role-badge {
  display: inline-block;
  padding: 2px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.role-user {
  background: #dbeafe;
  color: #1e40af;
}

.role-admin {
  background: #fef3c7;
  color: #92400e;
}

.role-developer {
  background: #dcfce7;
  color: #166534;
}

.guide-role-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 22px;
}

.guide-role-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  border: 2px solid #e8eaef;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: #64748b;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.guide-role-tab:hover {
  border-color: #c7d2fe;
  background: #f8f9fc;
  color: #4f46e5;
}

.guide-role-tab.active {
  border-color: #6366f1;
  background: linear-gradient(135deg, #eef2ff 0%, #faf5ff 100%);
  color: #4338ca;
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.15);
}

.role-tab-icon {
  font-size: 18px;
}

.guide-content {
  animation: guideContentFade 0.25s ease;
}

@keyframes guideContentFade {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.guide-toc {
  background: #f8f9fc;
  border: 1px solid #e8eaef;
  border-radius: 12px;
  padding: 18px 22px;
  margin-bottom: 22px;
}

.guide-toc h3 {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 12px 0;
}

.guide-toc ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 20px;
}

.guide-toc li a {
  display: block;
  padding: 6px 10px;
  font-size: 13px;
  color: #4f46e5;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s ease;
  text-decoration: none;
  line-height: 1.5;
}

.guide-toc li a:hover {
  background: #eef2ff;
  color: #3730a3;
}

.guide-section {
  margin-bottom: 16px;
  background: #fff;
  border: 1px solid #e8eaef;
  border-radius: 12px;
  padding: 20px 22px;
  transition: box-shadow 0.2s ease;
}

.guide-section:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.guide-section h3 {
  font-size: 17px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 14px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #eef2ff;
  letter-spacing: -0.2px;
}

.guide-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #4f46e5;
  margin: 18px 0 8px 0;
}

.guide-section h4:first-of-type {
  margin-top: 4px;
}

.guide-section p {
  font-size: 13.5px;
  color: #374151;
  line-height: 1.8;
  margin: 0 0 10px 0;
}

.guide-section ul,
.guide-section ol {
  padding-left: 20px;
  margin: 0 0 10px 0;
}

.guide-section li {
  font-size: 13.5px;
  color: #374151;
  line-height: 1.8;
  margin-bottom: 4px;
}

.guide-section li strong {
  color: #1a1a2e;
}

.guide-section code {
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #6366f1;
  font-family: 'Cascadia Code', 'Fira Code', monospace;
}

.guide-table {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
  font-size: 13px;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #e8eaef;
}

.guide-table thead tr {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: #fff;
}

.guide-table th {
  padding: 10px 14px;
  text-align: left;
  font-weight: 600;
  font-size: 12px;
  letter-spacing: 0.3px;
}

.guide-table td {
  padding: 9px 14px;
  border-bottom: 1px solid #f1f5f9;
  color: #374151;
}

.guide-table tbody tr:hover {
  background: #f8f9fc;
}

.guide-table tbody tr:last-child td {
  border-bottom: none;
}

.rules-card {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 100%);
  border: 1px solid #bbf7d0;
  border-radius: 12px;
  padding: 20px 22px;
  margin-bottom: 16px;
}

.rules-card h4 {
  font-size: 15px;
  font-weight: 700;
  color: #166534;
  margin: 0 0 14px 0;
}

.rules-card-danger {
  background: linear-gradient(135deg, #fff7ed 0%, #fefce8 100%);
  border-color: #fed7aa;
}

.rules-card-danger h4 {
  color: #c2410c;
}

.rule-group {
  padding: 12px 0;
  border-bottom: 1px dashed #d1d5db;
}

.rule-group:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.rule-group:first-child {
  padding-top: 0;
}

.rule-group h5 {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 6px 0;
}

.rule-group p {
  font-size: 13.5px;
  color: #374151;
  line-height: 1.8;
  margin: 0;
}

.rule-warning {
  background: #fefce8;
  border: 1px solid #fde68a;
  border-radius: 8px;
  padding: 12px 14px;
  margin-top: 4px;
}

.rule-warning h5 {
  color: #92400e;
}

.freeze-intro {
  font-size: 13.5px;
  color: #374151;
  margin: 0 0 10px 0;
}

.freeze-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.freeze-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: #374151;
  line-height: 1.6;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  border: 1px solid #fed7aa;
}

.freeze-list li::before {
  content: '🧊';
  flex-shrink: 0;
  font-size: 14px;
}

.guide-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, #d1d5db, transparent);
  margin: 24px 0;
}

.guide-footer {
  margin-top: 8px;
}

.guide-footer-signature {
  text-align: center;
  padding: 20px 0 12px;
  color: #94a3b8;
  font-size: 13px;
  line-height: 1.8;
}

.guide-footer-signature p {
  margin: 0;
}

.guide-dev-name {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 4px 0;
}

.guide-contact {
  margin-top: 10px;
  font-size: 12px;
  color: #94a3b8;
}

.guide-contact p {
  margin: 2px 0;
}

@media (max-width: 768px) {
  .guide-modal {
    width: 98%;
    max-height: 92vh;
    border-radius: 16px;
  }

  .guide-header {
    padding: 16px 18px 14px;
  }

  .guide-body {
    padding: 14px 18px 20px;
  }

  .guide-toc ul {
    grid-template-columns: 1fr;
  }

  .guide-role-tabs {
    flex-direction: column;
    gap: 8px;
  }

  .freeze-list {
    grid-template-columns: 1fr;
  }

  .guide-table {
    font-size: 12px;
  }

  .guide-table th,
  .guide-table td {
    padding: 7px 10px;
  }
}
</style>