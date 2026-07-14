async function safeJson(r) {
    const text = await r.text()
    if (!text) return { success: false, resultMsg: '服务端返回空响应，请检查后端是否正常运行' }
    try { return JSON.parse(text) } catch (e) { return { success: false, resultMsg: '响应格式异常：' + text.substring(0, 200) } }
}

export function fetchTask() {
    return fetch('/api/proxy/task').then(safeJson)
}

export function fetchObtain(appId) {
    return fetch('/api/proxy/obtain?appleid=' + encodeURIComponent(appId)).then(safeJson)
}

export function fetchEvent(bundleId) {
    return fetch('/api/proxy/event?bundleId=' + encodeURIComponent(bundleId)).then(safeJson)
}

export function fetchAttribution(bundleId, type) {
    return fetch('/api/proxy/attribution?bundleId=' + encodeURIComponent(bundleId) + '&type=' + type).then(safeJson)
}

export function fetchFrozen(id) {
    return fetch('/api/proxy/frozen?id=' + encodeURIComponent(id)).then(safeJson)
}

export function insertRecord(data) {
    return fetch('/api/record/insert', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }).then(safeJson)
}

export function fetchUnexported() {
    return fetch('/api/record/unexported').then(safeJson)
}

export function executeExport() {
    return fetch('/execute', { method: 'POST' }).then(safeJson)
}

export const ATTR_TYPES = ['appflyer', 'adjust', 'singular', 'tenjin']

export function fetchAllAttributions(bundleId) {
    return Promise.all(
        ATTR_TYPES.map(type =>
            fetchAttribution(bundleId, type).then(json => ({ type, json }))
        )
    )
}
export function fetchRecordList(ascribe, frozen) {
    const params = new URLSearchParams()
    if (ascribe) {
        params.append('ascribe', ascribe)
    }
    if (frozen) {
        params.append('frozen', 'true')
    }
    const qs = params.toString()
    return fetch('/api/record/list' + (qs ? '?' + qs : '')).then(safeJson)
}

export function updateRecord(data) {
    return fetch('/api/record/update', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    }).then(safeJson)
}

export function deleteRecord(url) {
    return fetch('/api/record/delete?url=' + encodeURIComponent(url), {
        method: 'DELETE'
    }).then(safeJson)
}

