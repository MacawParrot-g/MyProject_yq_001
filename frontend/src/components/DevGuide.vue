<!-- C:\Users\EDY\IdeaProjects\Automatic_test_script\src\main\frontend\src\components\DevGuide.vue -->
<template>
  <div class="guide-overlay" v-if="visible" @click.self="$emit('close')">
    <div class="guide-modal">
      <div class="guide-header">
        <h2 class="guide-title">📖 开发者模式使用指南</h2>
        <button class="guide-close-btn" @click="$emit('close')">✕</button>
      </div>

      <div class="guide-tabs">
        <button class="guide-tab" :class="{ active: activeSection === 'dev' }" @click="activeSection = 'dev'">
          <span class="guide-tab-icon">🛠️</span><span>开发者操作指南</span>
        </button>
        <button class="guide-tab" :class="{ active: activeSection === 'mmp' }" @click="activeSection = 'mmp'">
          <span class="guide-tab-icon">📊</span><span>四大归因平台说明</span>
        </button>
      </div>

      <div class="guide-body">
        <!-- ==================== 开发者操作指南 ==================== -->
        <div v-if="activeSection === 'dev'" class="guide-content">
          <div class="guide-warning-banner">
            <span class="warning-icon">⚠️</span>
            <span class="warning-text">以下操作均为高危操作，请仔细阅读后再执行。任何误操作可能导致数据丢失、服务中断或生产事故！</span>
          </div>

          <section class="guide-section">
            <h3 class="guide-section-title">一、专业测试模块</h3>

            <div class="guide-item">
              <h4 class="guide-item-title">1.1 获取下载任务</h4>
              <p class="guide-item-desc">点击「 刷新数据」按钮，系统会从远程服务器获取当前可用的测试下载任务。获取成功后会生成二维码供扫码下载 APP 进行测试。</p>
              <div class="guide-risk risk-high">
                <strong> 高危警告：</strong>刷新操作会清除当前未入库的测试数据。如果当前已有下载链接但尚未入库，刷新后原数据将永久丢失，无法恢复！请务必先完成入库操作再刷新。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">1.2 手动生成下载链接</h4>
              <p class="guide-item-desc">在输入框中输入 App ID（Apple ID），点击「生成」按钮可手动获取指定应用的下载链接。适用于需要测试特定应用的场景。</p>
              <div class="guide-risk risk-medium">
                <strong>⚠️ 注意：</strong>手动生成同样会覆盖当前已有的下载任务，请确认当前数据已入库后再操作。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">1.3 查询事件</h4>
              <p class="guide-item-desc">点击「查询事件」按钮，系统会调用归因平台接口查询当前 Bundle ID 的最新 currentTargetNum 值，并与原始值进行对比。同时会自动查询四大归因平台（AppsFlyer、Adjust、Singular、Tenjin）的归因数据。</p>
              <div class="guide-risk risk-low">
                <strong>ℹ️ 提示：</strong>查询操作为只读操作，不会修改任何数据，可放心使用。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">1.4 冻结应用</h4>
              <p class="guide-item-desc">当查询结果显示「无事件」时，可点击「冻结应用」按钮对当前应用进行冻结操作。冻结后该应用的 remark 字段会被标记为「已冻结」。</p>
              <div class="guide-risk risk-high">
                <strong>⛔ 高危警告：</strong>冻结操作会直接修改远程平台的应用状态，可能导致该应用在归因平台中被标记为异常。请确认确实需要冻结后再操作，冻结后可能无法撤销！
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">1.5 定时查询</h4>
              <p class="guide-item-desc">设置倒计时秒数后点击「开始定时」，系统会在倒计时结束后自动执行事件查询。适用于需要等待一段时间后再检查归因结果的场景（如等待 SKAdNetwork 回传）。</p>
              <div class="guide-risk risk-low">
                <strong>ℹ️ 提示：</strong>定时查询为只读操作，可设置 1~3600 秒，建议设置为 60 秒以上以等待归因平台处理。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">1.6 数据入库</h4>
              <p class="guide-item-desc">查询事件后，填写异常类型、备注、记录人、记录日期等信息，点击「直接入库」将测试数据写入数据库。入库后数据会标记为未导出状态，等待后续导出流程。</p>
              <div class="guide-risk risk-medium">
                <strong>️ 注意：</strong>入库操作会向数据库写入一条新记录。请确保填写的信息准确完整，特别是异常类型和备注字段。入库后的数据如需修改需通过管理员面板操作。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">1.7 自动去重功能</h4>
              <p class="guide-item-desc">开启自动去重后，系统会在入库前自动检测是否存在重复的 URL + Bundle ID 组合，避免重复数据写入。去重状态通过 Redis 会话管理，有效期 8 小时。</p>
              <div class="guide-risk risk-medium">
                <strong>⚠️ 注意：</strong>去重功能依赖 Redis 服务，如果 Redis 不可用，去重检查将自动降级失效，可能导致重复数据入库。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">1.8 轮询模式</h4>
              <p class="guide-item-desc">系统支持每 10 秒自动轮询获取新的下载任务。当当前任务入库完成后，轮询会自动获取下一个待测试任务，实现连续测试流程。</p>
              <div class="guide-risk risk-medium">
                <strong>⚠️ 注意：</strong>轮询模式下每次获取新任务都会覆盖旧任务。如果旧任务尚未入库，数据将丢失。请确保在轮询获取新任务前完成入库操作。可随时点击「停止轮询」终止自动获取。
              </div>
            </div>
          </section>

          <section class="guide-section">
            <h3 class="guide-section-title">二、数据建表和治理模块</h3>

            <div class="guide-item">
              <h4 class="guide-item-title">2.1 查看表列表</h4>
              <p class="guide-item-desc">进入数据建表和治理 Tab 后，系统会自动加载当前数据库中的所有表。点击表名可查看表结构详情（字段名、类型、注释等）。</p>
              <div class="guide-risk risk-low">
                <strong>ℹ️ 提示：</strong>查看操作为只读，不会修改任何数据。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">2.2 创建数据表</h4>
              <p class="guide-item-desc">输入表名和字段定义（SQL 格式），点击「创建表」即可在当前数据库中创建新表。系统会自动添加 utf8mb4 字符集支持。</p>
              <div class="guide-risk risk-high">
                <strong>⛔ 高危警告：</strong>建表操作会直接在数据库中创建新表。请确保表名不与现有表冲突，字段定义语法正确。一旦创建成功，如需删除需手动执行 DROP TABLE 操作。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">2.3 删除数据表</h4>
              <p class="guide-item-desc">在表列表中点击表名右侧的删除图标，确认后执行 DROP TABLE 操作。</p>
              <div class="guide-risk risk-critical">
                <strong> 极度危险：</strong>删除表操作不可逆！表中的所有数据将被永久删除，无法恢复！执行前请务必确认该表确实不再需要，并建议提前备份数据。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">2.4 新增/修改/删除字段</h4>
              <p class="guide-item-desc">选中目标表后，可使用「新增字段」「修改字段」「删除字段」功能对表结构进行 ALTER TABLE 操作。</p>
              <div class="guide-risk risk-high">
                <strong> 高危警告：</strong>修改或删除字段可能导致现有数据丢失或查询异常。特别是删除字段，该字段的所有数据将永久丢失。修改字段类型可能导致数据截断或转换错误。操作前请充分评估影响范围。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">2.5 SQL 执行器</h4>
              <p class="guide-item-desc">输入任意 SQL 语句，可选择对指定表或全局执行。支持 SELECT 查询和 DML 操作（INSERT/UPDATE/DELETE）。</p>
              <div class="guide-risk risk-critical">
                <strong>🚨 极度危险：</strong>SQL 执行器拥有完整的数据库操作权限！执行 DELETE 或 UPDATE 语句前请务必先用 SELECT 确认影响范围。错误的 SQL 可能导致大量数据被误删或误改。系统会过滤部分危险关键词（如 DROP DATABASE），但不能完全防止误操作。建议仅在测试环境使用此功能。
              </div>
            </div>

            <div class="guide-item">
              <h4 class="guide-item-title">2.6 批量导入</h4>
              <p class="guide-item-desc">配置源数据库连接信息（地址、用户名、密码），选择要导入的表，可一键将其他数据库中的数据批量导入到当前数据库。支持导入前清空目标表选项。</p>
              <div class="guide-risk risk-high">
                <strong>⛔ 高危警告：</strong>批量导入会向目标表写入大量数据。如果勾选了「导入前清空」，目标表的现有数据将被全部删除后再导入新数据。请确认源数据库连接信息正确，导入的表结构与目标表兼容。导入过程中如发生错误，可能导致部分数据丢失。
              </div>
            </div>
          </section>

          <section class="guide-section">
            <h3 class="guide-section-title">三、通用安全规范</h3>
            <div class="guide-risk risk-critical">
              <strong>🚨 开发者模式安全守则：</strong>
              <ul class="guide-risk-list">
                <li>开发者模式仅限授权人员在测试环境中使用，严禁在生产环境开启。</li>
                <li>所有 DDL 操作（建表、删表、改字段）执行前必须经过代码审查和审批流程。</li>
                <li>批量导入/导出操作应避开业务高峰期，避免影响线上服务性能。</li>
                <li>SQL 执行器禁止执行 DROP DATABASE、TRUNCATE 等破坏性语句。</li>
                <li>定期备份数据库，确保在误操作后能够快速恢复数据。</li>
                <li>操作日志会被完整记录，所有高危操作均可追溯。</li>
                <li>如发现异常数据或操作失误，应立即上报并配合排查，切勿擅自修改掩盖。</li>
              </ul>
            </div>
          </section>
        </div>

        <!-- ==================== 四大归因平台说明 ==================== -->
        <div v-if="activeSection === 'mmp'" class="guide-content">
          <div class="mmp-overview">
            <h3 class="guide-section-title">四大广告归因平台（MMP）返回数据结构详解与横向对比</h3>

            <section class="mmp-section">
              <h4 class="mmp-subtitle">一、概述</h4>

              <h5 class="mmp-h5">1.1 MMP 的概念与核心作用</h5>
              <p class="mmp-text">MMP（Mobile Measurement Partner，移动归因合作伙伴）是移动应用生态系统中至关重要的第三方数据监测与归因平台。在复杂的数字营销环境中，MMP 充当了广告主、广告网络（Ad Networks）和应用开发者之间的中立桥梁。其核心作用在于：</p>
              <ul class="mmp-list">
                <li><strong>精准归因：</strong>通过确定性匹配（如 IDFA、GAID）或概率匹配（设备指纹、IP 地址等），准确识别用户是从哪个广告渠道、哪个广告活动安装并激活应用的。</li>
                <li><strong>防作弊与数据清洗：</strong>过滤虚假安装、点击注入、设备农场等作弊流量，确保营销预算的有效利用。</li>
                <li><strong>数据整合与分析：</strong>提供跨渠道的 ROI、ROAS、LTV 等关键业务指标的统一视图，支持数据仓库对接。</li>
              </ul>

              <h5 class="mmp-h5">1.2 多 MMP 对接的业务背景</h5>
              <p class="mmp-text">在实际的全球化投放业务中，企业通常需要同时对接 AppsFlyer、Adjust、Singular、Tenjin 等多个 MMP 平台，主要原因包括：</p>
              <ul class="mmp-list">
                <li><strong>多渠道投放策略：</strong>不同的广告网络可能与特定的 MMP 有更深度的集成或数据回传优势。</li>
                <li><strong>数据冗余与容灾备份：</strong>单一平台故障可能导致归因数据全面丢失，多平台并行可作为数据灾备方案。</li>
                <li><strong>业务线与区域差异：</strong>不同业务线或出海区域可能因历史原因或合规要求选择了不同的 MMP。</li>
                <li><strong>功能互补：</strong>各平台在反作弊、事件级归因、收入聚合等方面各有侧重，多平台结合可实现能力互补。</li>
              </ul>

              <h5 class="mmp-h5">1.3 文档目的与适用范围</h5>
              <p class="mmp-text">本文档旨在为数据处理平台的开发团队提供详尽的技术参考，深入解析 AppsFlyer、Adjust、Singular、Tenjin 四大主流 MMP 平台归因接口返回的数据结构。文档详细阐述了各字段的含义、数据类型、取值范围及其在归因链路中的业务意义，并对四大平台的设计差异进行了横向对比。最后，针对数据处理平台的统一模型设计、数据同步策略及异常处理提供了工程化建议。本文档适用于后端开发工程师、数据工程师、数据分析师及系统架构师。</p>
            </section>

            <section class="mmp-section">
              <h4 class="mmp-subtitle">二、AppsFlyer 平台字段详解</h4>
              <p class="mmp-text">AppsFlyer 作为全球市场份额领先的 MMP，其数据结构设计高度模块化，注重安全认证与生态集成。以下是基于实际归因接口返回对象的字段深度解析：</p>

              <h5 class="mmp-h5">2.1 基础记录字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">id</div>
                <div class="mmp-field-desc">系统内部自增主键，数据类型为 Integer。示例值：1135265。作为数据处理平台本地数据库的唯一标识，用于增量同步时的游标定位及数据去重。确保每条归因记录在本地系统中的绝对唯一性，支撑高效的数据检索与更新。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">status</div>
                <div class="mmp-field-desc">记录状态标识，数据类型为 Integer。示例值：1（通常表示有效/已处理）。标记该条归因数据的生命周期状态（如待处理、已归因、已作废、延迟归因等）。支持数据状态机流转，便于排查异常数据和重试失败任务。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">createTime & updateTime</div>
                <div class="mmp-field-desc">记录创建与最后更新时间，数据类型为 String (ISO 8601)。示例：2026-08-10T10:34:24。追踪数据在本地系统中的流转耗时，监控 ETL 任务的处理延迟。updateTime 晚于 createTime 通常意味着发生了延迟归因（Late Attribution）或数据补全，是评估归因时效性的关键指标。</div>
              </div>

              <h5 class="mmp-h5">2.2 设备标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">idfa</div>
                <div class="mmp-field-desc">iOS 设备的广告标识符（Identifier for Advertisers），数据类型为 String (UUID)。示例：00000000-0000-0000-0000-000000000000。iOS 生态中最核心的确定性归因标识，用于将应用安装事件与广告点击事件进行精确匹配。<br/><strong>ATT 机制：</strong>自 iOS 14.5 引入 ATT（App Tracking Transparency）框架后，应用必须获得用户明确授权才能获取真实 IDFA。全零 IDFA 表示用户拒绝了追踪授权或系统限制了广告追踪。<br/><strong>降级方案：</strong>当 IDFA 不可用时，AppsFlyer 会自动降级至概率归因模型（Probabilistic Modeling），结合 IP 地址、设备型号、操作系统版本、User-Agent 等信号进行模糊匹配，或依赖 SKAdNetwork (SKAN) 进行隐私保护下的归因。</div>
              </div>

              <h5 class="mmp-h5">2.3 应用标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">bundleId</div>
                <div class="mmp-field-desc">应用包名（如 com.tsum.Outlet），iOS 应用的唯一标识符，用于区分不同应用及环境。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">bundleName</div>
                <div class="mmp-field-desc">应用显示名称（如 Outlet），便于业务人员直观识别，但不具备唯一性。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">appVersion</div>
                <div class="mmp-field-desc">应用构建号（Build Number，如 1173），内部版本控制标识，用于定位特定版本的 Bug 或归因异常。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">appShortVersionStr</div>
                <div class="mmp-field-desc">面向用户的营销版本号（如 1.42），用于业务层面的版本数据分析。</div>
              </div>

              <h5 class="mmp-h5">2.4 AppsFlyer 认证与配置字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">appsFlyerDevKey</div>
                <div class="mmp-field-desc">开发者专属密钥（如 BBfvLpP5FFWv6CRJshmSdE）。在 SDK 初始化时传入，用于将上报的数据绑定到特定的 AppsFlyer 账户和应用。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">appsFlyerAesKey</div>
                <div class="mmp-field-desc">AES 加密密钥。用于对敏感数据进行端到端加密传输，确保数据在 SDK 到服务器链路中的安全性，防止中间人攻击。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">appsFlyerAppId</div>
                <div class="mmp-field-desc">应用在 App Store 中的数字 ID（如 6450009110），用于 AppsFlyer 后台与 App Store Connect 的数据校验和自动同步。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">appsFlyerCdnToken</div>
                <div class="mmp-field-desc">CDN 通信令牌。AppsFlyer 使用全球 CDN 加速数据上报，该 Token 用于验证上报请求的合法性并路由至最优节点。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">appsFlyerSdkVersion</div>
                <div class="mmp-field-desc">集成的 SDK 版本号（如 version: 6.17.0 (build 2)）。用于排查因 SDK 版本过旧导致的兼容性问题或新特性缺失。</div>
              </div>

              <h5 class="mmp-h5">2.5 事件上报相关字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">eventToken</div>
                <div class="mmp-field-desc">事件标识符。若当前记录为应用内事件（In-App Event），该字段包含对应事件的 Token；若为安装事件，则为空。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">hostPrefix</div>
                <div class="mmp-field-desc">自定义上报域名前缀。部分企业为满足合规或网络优化需求，会配置专属的上报域名。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">eventParam & tempEventParam</div>
                <div class="mmp-field-desc">事件自定义参数。以 JSON 字符串形式传递业务数据（如购买金额、商品 ID）。tempEventParam 通常用于暂存未成功发送的事件，待网络恢复后重传。</div>
              </div>

              <h5 class="mmp-h5">2.6 时间戳字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">lastReportTime</div>
                <div class="mmp-field-desc">最后一次成功上报或服务端处理的时间。在延迟归因场景下，该时间会晚于实际安装时间，是计算归因窗口（Lookback Window）的重要依据。</div>
              </div>
            </section>

            <section class="mmp-section">
              <h4 class="mmp-subtitle">三、Adjust 平台字段详解</h4>
              <p class="mmp-text">Adjust 以其强大的反作弊能力和设备级标识稳定性著称，其数据结构在签名验证和自有标识符方面具有鲜明特色。</p>

              <h5 class="mmp-h5">3.1 基础记录字段</h5>
              <p class="mmp-text">与 AppsFlyer 类似，包含 id、status、createTime、updateTime。在 Adjust 的数据流中，updateTime 的变动常用于触发重归因（Re-attribution）逻辑，例如用户卸载重装或深度链接唤醒。</p>

              <h5 class="mmp-h5">3.2 设备标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">idfa</div>
                <div class="mmp-field-desc">iOS 广告标识符。示例：81CF5F9F-491E-4BC0-BCA4-E59108EEF17A。与 AppsFlyer 示例中的全零 IDFA 不同，此处的有效 IDFA 表明用户已授予 ATT 权限。在数据处理平台中，应针对有效 IDFA 和全零 IDFA 建立不同的归因处理分支。有效 IDFA 走确定性归因，全零 IDFA 需触发 Adjust 的概率模型或 SKAN 归因链路。</div>
              </div>

              <h5 class="mmp-h5">3.3 应用标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">bundleId / bundleName / appVersion / appShortVersionStr</div>
                <div class="mmp-field-desc">与 AppsFlyer 一致，用于应用维度的数据隔离与版本分析。Adjust 对 Bundle ID 的校验极为严格，不匹配的 Bundle ID 会导致归因直接失败。</div>
              </div>

              <h5 class="mmp-h5">3.4 Adjust SDK 与签名相关字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">adjustClientSdkVer</div>
                <div class="mmp-field-desc">客户端 SDK 版本。示例 react-native5.4.2@ios5.4.3 采用了特殊的复合版本号格式，表明该应用使用了 React Native 跨平台框架，且底层封装了 iOS 原生 SDK。这种格式有助于快速定位跨平台桥接层的问题。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">adjustSignVer</div>
                <div class="mmp-field-desc">防作弊签名版本号（如 3.47.0）。Adjust 的核心竞争力在于其设备级签名机制。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">adjustAdSignType</div>
                <div class="mmp-field-desc">广告签名类型（如 0）。用于标识当前归因所使用的签名算法版本。Adjust 通过持续升级签名算法来对抗不断演进的作弊手段，该字段帮助后端判断是否需要应用新的反作弊规则。</div>
              </div>

              <h5 class="mmp-h5">3.5 Adjust 核心标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">adjustToken</div>
                <div class="mmp-field-desc">Adjust 设备令牌（如 j0a9bl3zi39c），数据类型为 String。这是 Adjust 独有的、与 IDFA 解耦的设备级标识符。即使 IDFA 因 ATT 限制变为全零，Adjust Token 依然能够保持相对稳定。在隐私限制日益严格的环境下，Adjust Token 提供了比纯概率归因更高的准确率，是 Adjust 在 iOS 14+ 时代保持归因精度的关键武器。</div>
              </div>

              <h5 class="mmp-h5">3.6 事件上报相关字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">eventToken</div>
                <div class="mmp-field-desc">Adjust 的事件追踪同样依赖 Token 机制，每个自定义事件在 Adjust 后台配置后生成唯一 Token。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">eventCount</div>
                <div class="mmp-field-desc">事件触发计数。Adjust 在单次会话中可能会对高频事件进行聚合上报，该字段表示本次请求中包含的事件触发次数，用于准确计算事件总量。</div>
              </div>

              <h5 class="mmp-h5">3.7 时间戳字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">lastReportTime</div>
                <div class="mmp-field-desc">Adjust 的时间戳精度极高，且严格遵循 UTC 标准。在处理跨时区数据时，需统一转换为业务所在时区。</div>
              </div>
            </section>

            <section class="mmp-section">
              <h4 class="mmp-subtitle">四、Singular 平台字段详解</h4>
              <p class="mmp-text">Singular 定位为"营销数据与分析平台"，其数据结构设计更侧重于事件级别的深度追踪和跨渠道数据整合。</p>

              <h5 class="mmp-h5">4.1 基础记录字段</h5>
              <p class="mmp-text">包含标准的 id、status、createTime、updateTime。Singular 的 status 字段常用于标记数据是否已完成跨渠道的匹配与清洗。</p>

              <h5 class="mmp-h5">4.2 设备标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">idfa</div>
                <div class="mmp-field-desc">示例 8AFE2DCA-3AF9-44F6-BDE6-4B628478F0D7。Singular 在处理 IDFA 时，除了用于归因，还会将其哈希化后用于跨设备图谱（Cross-Device Graph）的构建，以支持多设备用户的统一画像。</div>
              </div>

              <h5 class="mmp-h5">4.3 应用标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">bundleId / appName</div>
                <div class="mmp-field-desc">bundleId (com.bebopbee.match3.animalcrush)。差异点：Singular 使用 appName 而非 bundleName，在字段映射时需特别注意命名差异。</div>
              </div>

              <h5 class="mmp-h5">4.4 事件追踪字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">eventName</div>
                <div class="mmp-field-desc">事件名称，数据类型为 String。示例：login。Singular 支持高度自定义的事件命名，不强制依赖 Token。这使得业务人员可以直接通过事件名称理解用户行为。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">eventParam</div>
                <div class="mmp-field-desc">事件参数，数据类型为 JSON String。示例：{"eventParam":{"facebook_id":null,"login_type":"","apple_name":null...}}。Singular 的事件参数结构非常灵活。示例中的 login 事件包含了多种第三方登录方式的标识（Facebook、Apple、Google）。这些参数在 ROAS 计算中至关重要，例如可以分析"通过 Apple 登录的用户"与"通过 Facebook 登录的用户"在后续付费转化率上的差异。同时，这些丰富的参数也是 Singular 数据仓库（Data Warehouse）导出的核心资产，支持在 Redshift/BigQuery 中进行深度 BI 分析。</div>
              </div>

              <h5 class="mmp-h5">4.5 时间戳字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">lastReportTime</div>
                <div class="mmp-field-desc">Singular 的事件时间戳通常与服务器接收时间高度一致，延迟较低，适合对实时性要求较高的 ROAS 监控看板。</div>
              </div>
            </section>

            <section class="mmp-section">
              <h4 class="mmp-subtitle">五、Tenjin 平台字段详解</h4>
              <p class="mmp-text">Tenjin 以"全栈归因"和"双源收入追踪"为核心卖点，其数据结构在收入归因和 SKAN 支持方面表现突出。</p>

              <h5 class="mmp-h5">5.1 基础记录字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">id / status / createTime / updateTime</div>
                <div class="mmp-field-desc">特殊说明：在 Tenjin 的数据中，updateTime 与 createTime 的差异往往比其它平台更为显著。这是因为 Tenjin 广泛使用概率归因和 SKAN，这些归因方式通常需要等待更长的归因窗口（如 SKAN 的 24-48 小时延迟），导致记录在初始创建后，随着新数据的到来而频繁更新。</div>
              </div>

              <h5 class="mmp-h5">5.2 设备标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">idfa</div>
                <div class="mmp-field-desc">示例 00000000-0000-0000-0000-000000000000（全零）。面对全零 IDFA，Tenjin 的概率归因引擎会接管处理。Tenjin 结合了设备指纹、IP、运营商信息及 SKAdNetwork 的 Conversion Value 进行综合推断。对于开发者而言，Tenjin 的全零 IDFA 归因准确率在业界处于第一梯队。</div>
              </div>

              <h5 class="mmp-h5">5.3 应用标识字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">bundleId / appName</div>
                <div class="mmp-field-desc">bundleId (com.ball.blast.tmball.ios)，appName (BallBlastPlus)。与 Singular 类似，使用 appName 字段。</div>
              </div>

              <h5 class="mmp-h5">5.4 事件追踪字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">eventName</div>
                <div class="mmp-field-desc">示例 TMBALL_TM__10000。Tenjin 的事件命名通常带有业务前缀和层级标识（如 TMBALL 代表项目，TM 代表模块，10000 代表事件 ID）。这种结构化命名便于在后台进行批量管理和自动化规则配置。</div>
              </div>
              <div class="mmp-field">
                <div class="mmp-field-name">eventParam</div>
                <div class="mmp-field-desc">示例为空字符串 ""。Tenjin 在事件参数传递上相对轻量，更侧重于通过事件名称本身来传递核心信息。在 ROAS 计算中，Tenjin 的优势在于其能够自动聚合广告网络 API 收入与应用内购（IAP）收入，即使 eventParam 为空，其后台依然能计算出准确的 ROAS。</div>
              </div>

              <h5 class="mmp-h5">5.5 时间戳字段</h5>
              <div class="mmp-field">
                <div class="mmp-field-name">lastReportTime</div>
                <div class="mmp-field-desc">在 SKAN 归因场景下，该时间戳反映的是 Apple 回传 Conversion Update 的时间，而非用户实际发生行为的时间。数据处理平台需特别注意此时间戳的语义差异，避免在实时报表中产生误导。</div>
              </div>
            </section>

            <section class="mmp-section">
              <h4 class="mmp-subtitle">六、四大平台横向对比</h4>

              <h5 class="mmp-h5">6.1 设备标识机制对比</h5>
              <div class="mmp-table-wrap">
                <table class="mmp-table">
                  <thead><tr><th>对比维度</th><th>AppsFlyer</th><th>Adjust</th><th>Singular</th><th>Tenjin</th></tr></thead>
                  <tbody>
                  <tr><td>核心标识符</td><td>IDFA, AF ID</td><td>IDFA, Adjust Token</td><td>IDFA, Singular ID</td><td>IDFA, Tenjin ID</td></tr>
                  <tr><td>IDFA 不可用降级</td><td>概率模型 + SKAN</td><td>概率模型 + Adjust Token</td><td>概率模型 + 跨设备图谱</td><td>概率模型 + SKAN + 设备指纹</td></tr>
                  <tr><td>自有标识符优势</td><td>AF ID 覆盖率高，生态广</td><td>Adjust Token 稳定性极强，抗重置</td><td>侧重跨设备关联与用户画像</td><td>侧重全栈归因与收入聚合</td></tr>
                  <tr><td>跨设备归因能力</td><td>强（People-Based ID）</td><td>中（设备图谱）</td><td>极强（核心卖点）</td><td>中（概率与行为模型）</td></tr>
                  </tbody>
                </table>
              </div>

              <h5 class="mmp-h5">6.2 事件追踪体系对比</h5>
              <div class="mmp-table-wrap">
                <table class="mmp-table">
                  <thead><tr><th>对比维度</th><th>AppsFlyer</th><th>Adjust</th><th>Singular</th><th>Tenjin</th></tr></thead>
                  <tbody>
                  <tr><td>事件命名规范</td><td>预定义 + 自定义 Token</td><td>预定义 + 自定义 Token</td><td>高度自定义字符串</td><td>结构化前缀 + 自定义名称</td></tr>
                  <tr><td>参数传递方式</td><td>JSON Map</td><td>JSON Map</td><td>嵌套 JSON Map</td><td>轻量级 / 事件名承载</td></tr>
                  <tr><td>事件类型支持</td><td>安装、激活、IAP、自定义</td><td>安装、会话、IAP、自定义、深度链接</td><td>全生命周期事件、跨端事件</td><td>安装、IAP、自定义、SKAN 事件</td></tr>
                  <tr><td>回传机制</td><td>Postback / Webhook / S2S</td><td>Postback / Webhook / S2S</td><td>Postback / Data Warehouse</td><td>Postback / Webhook / S2S</td></tr>
                  </tbody>
                </table>
              </div>

              <h5 class="mmp-h5">6.3 认证与配置机制对比</h5>
              <div class="mmp-table-wrap">
                <table class="mmp-table">
                  <thead><tr><th>对比维度</th><th>AppsFlyer</th><th>Adjust</th><th>Singular</th><th>Tenjin</th></tr></thead>
                  <tbody>
                  <tr><td>专属认证字段</td><td>DevKey, AES Key, CDN Token</td><td>App Token, Signature</td><td>API Key, App ID</td><td>API Key, App Token</td></tr>
                  <tr><td>数据加密方式</td><td>AES 端到端加密</td><td>设备级签名加密</td><td>TLS + Token 验证</td><td>TLS + Token 验证</td></tr>
                  <tr><td>SDK 版本管理</td><td>标准语义化版本</td><td>复合版本号（含框架信息）</td><td>标准语义化版本</td><td>标准语义化版本</td></tr>
                  </tbody>
                </table>
              </div>

              <h5 class="mmp-h5">6.4 时间戳与数据更新机制对比</h5>
              <div class="mmp-table-wrap">
                <table class="mmp-table">
                  <thead><tr><th>对比维度</th><th>AppsFlyer</th><th>Adjust</th><th>Singular</th><th>Tenjin</th></tr></thead>
                  <tbody>
                  <tr><td>时间戳设计</td><td>创建/更新/上报时间分离</td><td>高精度 UTC 时间</td><td>事件时间/接收时间</td><td>事件时间/SKAN 回传时间</td></tr>
                  <tr><td>数据更新频率</td><td>准实时，延迟归因触发</td><td>准实时，重归因触发</td><td>准实时，跨端匹配触发</td><td>延迟较高，SKAN 窗口期频繁更新</td></tr>
                  <tr><td>延迟归因处理</td><td>完善，状态机流转清晰</td><td>完善，支持重归因窗口</td><td>一般，侧重实时流</td><td>核心能力，SKAN 延迟归因强</td></tr>
                  <tr><td>lastReportTime 含义</td><td>服务端最后处理时间</td><td>服务端最后接收时间</td><td>事件最后更新时间</td><td>SKAN/概率归因最后确认时间</td></tr>
                  </tbody>
                </table>
              </div>

              <h5 class="mmp-h5">6.5 收入归因与 ROAS 计算对比</h5>
              <div class="mmp-table-wrap">
                <table class="mmp-table">
                  <thead><tr><th>对比维度</th><th>AppsFlyer</th><th>Adjust</th><th>Singular</th><th>Tenjin</th></tr></thead>
                  <tbody>
                  <tr><td>收入追踪方式</td><td>IAP 事件 + 广告收入 API</td><td>IAP 事件 + 广告收入 API</td><td>IAP 事件 + 广告收入 API</td><td>双源收入自动聚合（IAP + Ad）</td></tr>
                  <tr><td>ROAS 计算支持</td><td>原生支持，多维度报表</td><td>原生支持，自定义公式</td><td>原生支持，深度 BI 分析</td><td>原生支持，全栈 ROAS</td></tr>
                  <tr><td>数据仓库集成</td><td>Redshift, BigQuery, S3</td><td>Redshift, BigQuery, S3</td><td>Redshift, BigQuery, Snowflake</td><td>DataVault, BigQuery, S3</td></tr>
                  <tr><td>广告聚合收入</td><td>支持主流聚合平台</td><td>支持主流聚合平台</td><td>支持主流聚合平台</td><td>原生支持，无需额外集成</td></tr>
                  </tbody>
                </table>
              </div>

              <h5 class="mmp-h5">6.6 隐私合规能力对比</h5>
              <div class="mmp-table-wrap">
                <table class="mmp-table">
                  <thead><tr><th>对比维度</th><th>AppsFlyer</th><th>Adjust</th><th>Singular</th><th>Tenjin</th></tr></thead>
                  <tbody>
                  <tr><td>ATT 支持</td><td>完整，提供最佳实践 SDK</td><td>完整，提供 ATT 弹窗模板</td><td>完整，合规指引详尽</td><td>完整，自动降级处理</td></tr>
                  <tr><td>SKAdNetwork 支持</td><td>完整，支持 SKAN 4.0+</td><td>完整，支持 SKAN 4.0+</td><td>完整，支持 SKAN 4.0+</td><td>完整，SKAN 归因能力突出</td></tr>
                  <tr><td>概率归因能力</td><td>强，模型持续迭代</td><td>强，结合 Adjust Token</td><td>中，侧重跨设备替代</td><td>极强，核心差异化能力</td></tr>
                  <tr><td>数据匿名化</td><td>支持 PII 脱敏与哈希</td><td>支持 PII 脱敏与哈希</td><td>支持 PII 脱敏与哈希</td><td>支持 PII 脱敏与哈希</td></tr>
                  </tbody>
                </table>
              </div>

              <h5 class="mmp-h5">6.7 各平台核心优势总结</h5>
              <div class="mmp-table-wrap">
                <table class="mmp-table">
                  <thead><tr><th>平台</th><th>核心优势</th><th>适用场景</th></tr></thead>
                  <tbody>
                  <tr><td><strong>AppsFlyer</strong></td><td>全球市场份额第一，生态最完整，SDK 集成最简单，归因准确率高，反作弊能力强</td><td>适合全球化大规模投放，尤其是需要快速接入和全面数据覆盖的场景</td></tr>
                  <tr><td><strong>Adjust</strong></td><td>设备级签名反作弊业界领先，Adjust Token 在 ATT 后保持高归因精度，深度链接能力突出</td><td>适合对反作弊要求极高、需要精准设备追踪和深度链接跳转的场景</td></tr>
                  <tr><td><strong>Singular</strong></td><td>跨设备图谱能力极强，事件参数结构灵活，数据仓库导出功能完善，BI 分析深度高</td><td>适合需要跨设备用户画像、深度事件分析和数据仓库集成的场景</td></tr>
                  <tr><td><strong>Tenjin</strong></td><td>双源收入自动聚合（IAP + Ad），SKAN 归因能力突出，概率归因准确率高，ROAS 计算全栈化</td><td>适合以收入为核心指标、依赖 SKAN 归因、需要自动化 ROAS 监控的场景</td></tr>
                  </tbody>
                </table>
              </div>
            </section>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  visible: { type: Boolean, default: false }
})

defineEmits(['close'])

const activeSection = ref('dev')
</script>

<style scoped>
.guide-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.55); backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  z-index: 1000; padding: 20px;
}
.guide-modal {
  background: #fff; border-radius: 20px; width: 100%; max-width: 960px;
  max-height: 90vh; display: flex; flex-direction: column;
  box-shadow: 0 24px 80px rgba(0,0,0,0.25); overflow: hidden;
}
.guide-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 28px; border-bottom: 1px solid #f0f0f0; flex-shrink: 0;
}
.guide-title { font-size: 20px; font-weight: 700; color: #1a1a2e; margin: 0; }
.guide-close-btn {
  background: none; border: none; font-size: 22px; cursor: pointer;
  color: #9ca3af; padding: 4px 10px; border-radius: 8px; transition: all 0.2s;
}
.guide-close-btn:hover { background: #f3f4f6; color: #374151; }

.guide-tabs {
  display: flex; gap: 4px; background: #f4f5f7; padding: 4px;
  margin: 16px 28px 0; border-radius: 12px; flex-shrink: 0;
}
.guide-tab {
  flex: 1; display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px 0; font-size: 14px; border: none; border-radius: 10px;
  cursor: pointer; background: transparent; color: #888; font-weight: 600; transition: all 0.25s;
}
.guide-tab.active { background: #fff; color: #1a1a2e; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
.guide-tab:not(.active):hover { color: #555; background: rgba(255,255,255,0.5); }
.guide-tab-icon { font-size: 16px; }

.guide-body {
  overflow-y: auto; padding: 20px 28px 28px; flex: 1;
}
.guide-content { animation: guideFade 0.25s ease; }
@keyframes guideFade { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }

.guide-warning-banner {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px 18px; background: #fef2f2; border: 1px solid #fecaca;
  border-radius: 12px; margin-bottom: 24px;
}
.warning-icon { font-size: 22px; flex-shrink: 0; }
.warning-text { font-size: 14px; font-weight: 600; color: #991b1b; line-height: 1.5; }

.guide-section { margin-bottom: 28px; }
.guide-section-title {
  font-size: 18px; font-weight: 700; color: #1a1a2e;
  margin: 0 0 16px; padding-bottom: 8px; border-bottom: 2px solid #6366f1;
}
.guide-item { margin-bottom: 18px; }
.guide-item-title {
  font-size: 15px; font-weight: 700; color: #374151; margin: 0 0 6px;
}
.guide-item-desc {
  font-size: 13px; color: #4b5563; line-height: 1.7; margin: 0 0 8px;
}
.guide-risk {
  padding: 10px 14px; border-radius: 10px; font-size: 13px; line-height: 1.6;
}
.guide-risk ul { margin: 6px 0 0; padding-left: 20px; }
.guide-risk li { margin-bottom: 4px; }
.risk-critical { background: #fef2f2; border: 1px solid #fca5a5; color: #991b1b; }
.risk-high { background: #fff7ed; border: 1px solid #fdba74; color: #9a3412; }
.risk-medium { background: #fffbeb; border: 1px solid #fde68a; color: #92400e; }
.risk-low { background: #f0fdf4; border: 1px solid #bbf7d0; color: #166534; }

/* MMP 内容样式 */
.mmp-overview { }
.mmp-section { margin-bottom: 32px; padding-bottom: 24px; border-bottom: 1px solid #f0f0f0; }
.mmp-section:last-child { border-bottom: none; }
.mmp-subtitle {
  font-size: 17px; font-weight: 700; color: #1a1a2e; margin: 0 0 14px;
  padding-left: 12px; border-left: 4px solid #6366f1;
}
.mmp-h5 {
  font-size: 14px; font-weight: 700; color: #374151; margin: 18px 0 8px;
}
.mmp-text { font-size: 13px; color: #4b5563; line-height: 1.8; margin: 0 0 10px; }
.mmp-list { margin: 0 0 12px; padding-left: 20px; }
.mmp-list li { font-size: 13px; color: #4b5563; line-height: 1.7; margin-bottom: 6px; }

.mmp-field {
  display: flex; gap: 12px; padding: 10px 14px; margin-bottom: 8px;
  background: #f9fafb; border-radius: 10px; border: 1px solid #f0f0f0;
}
.mmp-field-name {
  font-size: 13px; font-weight: 700; color: #6366f1; white-space: nowrap;
  min-width: 160px; flex-shrink: 0; font-family: 'Courier New', monospace;
}
.mmp-field-desc { font-size: 13px; color: #4b5563; line-height: 1.7; }

.mmp-table-wrap { overflow-x: auto; border-radius: 10px; border: 1px solid #e5e7eb; margin-bottom: 16px; }
.mmp-table { width: 100%; border-collapse: collapse; font-size: 12px; text-align: left; min-width: 600px; }
.mmp-table th {
  background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff;
  padding: 10px 12px; white-space: nowrap; font-size: 11px; font-weight: 700;
  text-transform: uppercase; letter-spacing: 0.3px;
}
.mmp-table td {
  padding: 9px 12px; border-bottom: 1px solid #f0f0f5; color: #374151; line-height: 1.5;
}
.mmp-table tr:hover td { background: #f9fafb; }
.mmp-table tr:last-child td { border-bottom: none; }
.mmp-table td strong { color: #6366f1; }

@media (max-width: 700px) {
  .guide-modal { max-height: 95vh; border-radius: 14px; }
  .guide-header { padding: 16px 18px; }
  .guide-tabs { margin: 12px 18px 0; }
  .guide-body { padding: 16px 18px 20px; }
  .mmp-field { flex-direction: column; gap: 4px; }
  .mmp-field-name { min-width: auto; }
}
</style>