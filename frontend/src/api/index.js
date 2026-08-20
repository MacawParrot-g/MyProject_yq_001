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
