async function safeJson(r) {
    const text = await r.text()
    if (!text) return { success: false, resultMsg: '服务端返回空响应，请检查后端是否正常运行' }
    try { return JSON.parse(text) } catch (e) { return { success: false, resultMsg: '响应格式异常：' + text.substring(0, 200) } }
}

function fetchWithTimeout(url, options = {}, timeoutMs = 60000) {
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs)
    const headers = {
        'X-User-Name': encodeURIComponent(localStorage.getItem('userName') || 'anonymous'),
        ...(options.headers || {})
    }
    return fetch(url, { ...options, headers, signal: controller.signal }).finally(() => clearTimeout(timeoutId))
}
function getTodayStr() {
    const d = new Date()
    return d.getFullYear() + '/' + (d.getMonth() + 1) + '/' + d.getDate()
}
export function fetchTask() {
    return fetchWithTimeout('/api/proxy/task', {}, 90000).then(safeJson)
}

export function fetchObtain(appId) {
    return fetchWithTimeout('/api/proxy/obtain?appleid=' + encodeURIComponent(appId), {}, 90000).then(safeJson)
}

export function fetchEvent(bundleId) {
    return fetchWithTimeout('/api/proxy/event?bundleId=' + encodeURIComponent(bundleId), {}, 60000).then(safeJson)
}

export function fetchAttribution(bundleId, type) {
    return fetchWithTimeout('/api/proxy/attribution?bundleId=' + encodeURIComponent(bundleId) + '&type=' + type, {}, 60000).then(safeJson)
}

export function fetchFrozen(id) {
    return fetchWithTimeout('/api/proxy/frozen?id=' + encodeURIComponent(id), {}, 30000).then(safeJson)
}

export function enableDedup(username) {
    return fetchWithTimeout('/api/dedup/enable?username=' + encodeURIComponent(username), { method: 'POST' }, 10000).then(safeJson)
}

export function disableDedup() {
    return fetchWithTimeout('/api/dedup/disable', { method: 'POST' }, 10000).then(safeJson)
}

export function fetchDedupStatus() {
    return fetchWithTimeout('/api/dedup/status', {}, 10000).then(safeJson)
}

export function fetchCountByRecorder() {
    const params = new URLSearchParams({
        recorder: localStorage.getItem('userName') || '',
        recordData: getTodayStr()
    });

    // 假设你的接口基础路径是 /api/record/querybyname
    return fetchWithTimeout(`/api/record/querybyname?${params.toString()}`, {}, 9178)
        .then(safeJson);
}

export function insertRecord(data) {
    return fetchWithTimeout('/api/record/insert', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }, 30000).then(safeJson)
}

export function fetchUnexported() {
    return fetchWithTimeout('/api/record/unexported', {}, 30000).then(safeJson)
}

export function executeExport() {
    return fetchWithTimeout('/execute', { method: 'POST' }, 300000).then(safeJson)
}

export const ATTR_TYPES = ['appflyer', 'adjust', 'singular', 'tenjin']

export function fetchAllAttributions(bundleId) {
    return Promise.all(
        ATTR_TYPES.map(type =>
            fetchAttribution(bundleId, type).then(json => ({ type, json }))
        )
    )
}
export function fetchRecordList(ascribe, frozen, page = 1, size = 15, recorder = '') {
    const params = new URLSearchParams()
    params.append('page', page)
    params.append('size', size)
    if (ascribe) params.append('ascribe', ascribe)
    if (frozen) params.append('frozen', 'true')
    if (recorder) params.append('recorder', recorder)
    return fetchWithTimeout('/api/record/list?' + params.toString(), {}, 30000).then(safeJson)
}

export function updateRecord(data) {
    return fetchWithTimeout('/api/record/update', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }, 30000).then(safeJson)
}

export function deleteRecord(url) {
    return fetchWithTimeout('/api/record/delete?url=' + encodeURIComponent(url), {
        method: 'DELETE'
    }, 30000).then(safeJson)
}

export function fetchUnexportedByUser(recorder) {
    return fetchWithTimeout('/api/export/unexported?recorder=' + encodeURIComponent(recorder), {}, 30000).then(safeJson)
}

export function executeExportByUser(recorder) {
    return fetchWithTimeout('/api/export/execute?recorder=' + encodeURIComponent(recorder), { method: 'POST' }, 300000).then(safeJson)
}

export function fetchExportStatus(recorder) {
    return fetchWithTimeout('/api/export/status?recorder=' + encodeURIComponent(recorder), {}, 10000).then(safeJson)
}

export function getExportDownloadUrl(recorder) {
    return '/api/export/download?recorder=' + encodeURIComponent(recorder)
}

export function fetchSystemInfo() {
    return fetchWithTimeout('/api/system/info', {}, 15000).then(safeJson)
}

export function fetchRandomForRetest(dates) {
    return fetchWithTimeout('/api/record/random-for-retest', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ dates })
    }, 30000).then(safeJson)
}

export function authLogin(uid, pwd) {
    return fetchWithTimeout('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ uid, pwd })
    }, 15000).then(safeJson)
}

export function authLogout() {
    return fetchWithTimeout('/api/auth/logout', { method: 'POST' }, 10000).then(safeJson)
}

export function authStatus() {
    return fetchWithTimeout('/api/auth/status', {}, 10000).then(safeJson)
}

export function createUser(name, pwd, type) {
    return fetchWithTimeout('/api/auth/user/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, pwd, type })
    }, 15000).then(safeJson)
}

export function deleteUser(uid) {
    return fetchWithTimeout('/api/auth/user/delete?uid=' + encodeURIComponent(uid), {
        method: 'DELETE'
    }, 15000).then(safeJson)
}

export function fetchUserList() {
    return fetchWithTimeout('/api/auth/user/list', {}, 15000).then(safeJson)
}
export function resetUserPassword(uid, newPwd) {
    return fetchWithTimeout('/api/admin/user/resetPwd', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ uid, newPwd })
    }, 15000).then(safeJson)
}

export function kickUser(uid, banSeconds) {
    return fetchWithTimeout('/api/admin/user/kick', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ uid, banSeconds })
    }, 15000).then(safeJson)
}
export function fetchAdminRecordList(ascribe, frozen, page = 1, size = 15, recorder = '', recordData = '') {
    const params = new URLSearchParams()
    params.append('page', page)
    params.append('size', size)
    if (ascribe) params.append('ascribe', ascribe)
    if (frozen) params.append('frozen', 'true')
    if (recorder) params.append('recorder', recorder)
    if (recordData) params.append('recordData', recordData)
    return fetchWithTimeout('/api/record/list?' + params.toString(), {}, 30000).then(safeJson)
}
export function fetchRecordListByDate(ascribe, frozen, page, size, recorder, dateInput) {
    const params = new URLSearchParams()
    params.append('page', page)
    params.append('size', size)
    if (ascribe) params.append('ascribe', ascribe)
    if (frozen) params.append('frozen', 'true')
    if (recorder) params.append('recorder', recorder)
    if (dateInput) {
        const parts = dateInput.split('-')
        const formatted = parts[0] + '/' + parseInt(parts[1]) + '/' + parseInt(parts[2])
        params.append('recordData', formatted)
    }
    return fetchWithTimeout('/api/record/list?' + params.toString(), {}, 30000).then(safeJson)
}

export function fetchDailyReport(dateInput) {
    const parts = dateInput.split('-')
    const formatted = parts[0] + '/' + parseInt(parts[1]) + '/' + parseInt(parts[2])
    return fetchWithTimeout('/api/record/daily-report?recordData=' + encodeURIComponent(formatted), {}, 30000).then(safeJson)
}

export function fetchDevHistory() {
    return fetchWithTimeout('/api/dev/history', {}, 15000).then(safeJson)
}

export function saveDevHistory(record) {
    return fetchWithTimeout('/api/dev/history/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(record)
    }, 15000).then(safeJson)
}

export function clearDevHistory() {
    return fetchWithTimeout('/api/dev/history/clear', { method: 'DELETE' }, 15000).then(safeJson)
}

export function fetchDevRedisStatus() {
    return fetchWithTimeout('/api/dev/history/redis-status', {}, 10000).then(safeJson)
}

export function deleteDevHistoryRecord(timestamp) {
    return fetchWithTimeout('/api/dev/history/delete?timestamp=' + encodeURIComponent(timestamp), {
        method: 'DELETE'
    }, 15000).then(safeJson)
}

export function adminRecordSearch(params) {
    return fetchWithTimeout('/api/admin/record/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(params)
    }, 30000).then(safeJson)
}

export function adminRecordStats() {
    return fetchWithTimeout('/api/admin/record/stats', {}, 15000).then(safeJson)
}

export function adminBatchDelete(urls) {
    return fetchWithTimeout('/api/admin/record/batch-delete', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ urls })
    }, 30000).then(safeJson)
}

export function adminRecordSummary(params) {
    return fetchWithTimeout('/api/admin/record/summary', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(params)
    }, 30000).then(safeJson)
}

export function fetchTableList() {
    return fetchWithTimeout('/api/dev/table/list', {}, 15000).then(safeJson)
}

export function fetchTableDescribe(tableName) {
    return fetchWithTimeout('/api/dev/table/describe?tableName=' + encodeURIComponent(tableName), {}, 15000).then(safeJson)
}

export function createTable(tableName, columnDefinitions) {
    return fetchWithTimeout('/api/dev/table/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tableName, columnDefinitions })
    }, 30000).then(safeJson)
}

export function dropTable(tableName) {
    return fetchWithTimeout('/api/dev/table/drop?tableName=' + encodeURIComponent(tableName), {
        method: 'DELETE'
    }, 15000).then(safeJson)
}

export function addColumn(tableName, columnDefinition) {
    return fetchWithTimeout('/api/dev/table/column/add', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tableName, columnDefinition })
    }, 15000).then(safeJson)
}

export function modifyColumn(tableName, columnDefinition) {
    return fetchWithTimeout('/api/dev/table/column/modify', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tableName, columnDefinition })
    }, 15000).then(safeJson)
}

export function dropColumn(tableName, columnName) {
    return fetchWithTimeout('/api/dev/table/column/drop', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tableName, columnName })
    }, 15000).then(safeJson)
}

export function executeSQL(sql, tableName) {
    return fetchWithTimeout('/api/dev/table/execute-sql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sql, tableName })
    }, 60000).then(safeJson)
}

export function batchImport(params) {
    return fetchWithTimeout('/api/dev/table/batch-import', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(params)
    }, 120000).then(safeJson)
}
