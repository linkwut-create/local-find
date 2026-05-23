package com.example.localfind.server

import android.util.Log
import com.example.localfind.auth.PairingTokenManager
import com.example.localfind.hardware.FlashlightController
import com.example.localfind.hardware.RingController
import com.example.localfind.model.PairingRequest
import com.example.localfind.store.LocalDeviceIdentityStore
import com.example.localfind.store.PairedControllerTokenStore
import com.example.localfind.store.PairingRequestStore
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class ServerStatus {
    STOPPED,
    STARTING,
    LISTENING,
    FAILED
}

class HttpServerManager(
    private val ringController: RingController,
    private val flashlightController: FlashlightController,
    private val tokenManager: PairingTokenManager,
    private val localDeviceIdentityStore: LocalDeviceIdentityStore,
    private val pairingRequestStore: PairingRequestStore,
    private val pairedControllerTokenStore: PairedControllerTokenStore,
    private val onStatusChange: () -> Unit,
) {
    @Volatile
    private var server: NettyApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val commandDispatcher = HardwareCommandDispatcher(ringController, flashlightController, onStatusChange)

    @Volatile
    var currentStatus: ServerStatus = ServerStatus.STOPPED
        private set

    @Volatile
    var lastServerError: String? = null
        private set

    @Volatile
    private var pairingModeExpiresAt: Long = 0L

    val isRingActive: Boolean get() = commandDispatcher.isRingActive()
    val flashMode: String get() = commandDispatcher.getFlashMode()

    fun getPort(): Int = 8888

    /**
     * Extracts unified server creation logic to ensure route consistency.
     */
    private fun createServer(): NettyApplicationEngine {
        return embeddedServer(Netty, port = getPort(), host = "0.0.0.0") {
            install(ContentNegotiation) {
                json()
            }
            routing {
                // GET /ping - Minimal connectivity test, no locks or tokens
                get("/ping") {
                    call.respond(buildJsonObject { put("ok", true) })
                }

                // GET /device-info - Public device identity for explicit user-driven pairing.
                get("/device-info") {
                    val identity = localDeviceIdentityStore.getOrCreate()
                    call.respond(
                        buildJsonObject {
                            put("id", identity.id)
                            put("name", identity.name)
                            put("type", identity.type)
                            put("port", getPort())
                            put("pairingMode", isPairingModeActive())
                            put("service", "running")
                        }
                    )
                }

                get("/pairing/status") {
                    val requestId = call.request.queryParameters["requestId"]
                    if (requestId.isNullOrBlank()) {
                        call.respond(buildJsonObject { put("pairingMode", isPairingModeActive()) })
                        return@get
                    }

                    val request = pairingRequestStore.get(requestId)
                    if (request == null) {
                        call.respond(
                            HttpStatusCode.NotFound,
                            buildJsonObject {
                                put("pairingMode", isPairingModeActive())
                                put("requestId", requestId)
                                put("status", PairingRequestStore.STATUS_EXPIRED)
                                put("message", "Pairing request not found")
                            }
                        )
                        return@get
                    }

                    call.respond(pairingStatusJson(request))
                }

                post("/pairing/request") {
                    if (!isPairingModeActive()) {
                        call.respond(
                            HttpStatusCode.Forbidden,
                            buildJsonObject {
                                put("ok", false)
                                put("message", "Pairing mode is not enabled")
                            }
                        )
                        return@post
                    }

                    val rawBody = try {
                        call.receiveText()
                    } catch (e: Exception) {
                        Log.e("HttpServerManager", "Failed to receive text from pairing request", e)
                        ""
                    }

                    val body = try {
                        if (rawBody.isBlank()) {
                            throw IllegalArgumentException("Empty body")
                        }
                        JSONObject(rawBody)
                    } catch (e: Exception) {
                        Log.e("HttpServerManager", "Invalid pairing request body: '$rawBody'", e)
                        call.respond(
                            HttpStatusCode.BadRequest,
                            buildJsonObject {
                                put("ok", false)
                                put("message", "Invalid pairing request body")
                                put("error", e.message ?: "unknown")
                            }
                        )
                        return@post
                    }

                    val controllerId = body.optString("controllerId").trim()
                    val controllerName = body.optString("controllerName").trim().ifBlank { "Chrome Extension" }
                    val controllerType = body.optString("controllerType").trim().ifBlank { "chrome_extension" }
                    val nonce = body.optString("nonce").trim()

                    if (controllerId.isBlank() || nonce.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            buildJsonObject {
                                put("ok", false)
                                put("message", "controllerId and nonce are required")
                            }
                        )
                        return@post
                    }

                    val request = pairingRequestStore.create(
                        controllerId = controllerId,
                        controllerName = controllerName,
                        controllerType = controllerType,
                        nonce = nonce,
                        ttlMillis = PAIRING_MODE_TTL_MILLIS,
                    )
                    onStatusChange()

                    call.respond(
                        buildJsonObject {
                            put("ok", true)
                            put("requestId", request.requestId)
                            put("status", request.status)
                        }
                    )
                }

                get("/pairing/controllers") {
                    authenticate {
                        call.respond(
                            buildJsonObject {
                                put("controllers", buildJsonArray {
                                    pairedControllerTokenStore.getAll().forEach { controller ->
                                        add(buildJsonObject {
                                            put("controllerId", controller.controllerId)
                                            put("controllerName", controller.controllerName)
                                            put("controllerType", controller.controllerType)
                                            put("pairedAt", controller.pairedAt)
                                        })
                                    }
                                })
                            }
                        )
                    }
                }

                post("/pairing/revoke") {
                    authenticate {
                        val rawBody = try {
                            call.receiveText()
                        } catch (e: Exception) {
                            Log.e("HttpServerManager", "Failed to receive text from revoke request", e)
                            ""
                        }

                        val controllerId = try {
                            if (rawBody.isBlank()) {
                                ""
                            } else {
                                JSONObject(rawBody).optString("controllerId").trim()
                            }
                        } catch (e: Exception) {
                            Log.e("HttpServerManager", "Invalid revoke request body: '$rawBody'", e)
                            call.respond(
                                HttpStatusCode.BadRequest,
                                buildJsonObject {
                                    put("ok", false)
                                    put("message", "Invalid revoke request body")
                                    put("error", e.message ?: "unknown")
                                }
                            )
                            return@authenticate
                        }

                        if (controllerId.isBlank()) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                buildJsonObject {
                                    put("ok", false)
                                    put("message", "controllerId is required")
                                }
                            )
                            return@authenticate
                        }

                        val revoked = pairedControllerTokenStore.revokeByControllerId(controllerId)
                        onStatusChange()

                        call.respond(
                            buildJsonObject {
                                put("ok", true)
                                put("revoked", revoked)
                            }
                        )
                    }
                }

                // GET / - Browser control page
                get("/") {
                    val deviceName = android.os.Build.MODEL
                    val html = """
                        <!DOCTYPE html>
                        <html lang="zh-CN">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Local Find - 电脑端控制页</title>
                            <style>
                                :root { --primary: #007AFF; --danger: #FF3B30; --success: #34C759; --bg: #F2F2F7; --card: #FFFFFF; }
                                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background-color: var(--bg); color: #000; margin: 0; padding: 20px; display: flex; flex-direction: column; align-items: center; }
                                .card { background: var(--card); padding: 24px; border-radius: 20px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); width: 100%; max-width: 500px; margin-bottom: 20px; box-sizing: border-box; }
                                h1 { margin: 0 0 8px; font-size: 24px; text-align: center; color: var(--primary); }
                                .subtitle { text-align: center; color: #8E8E93; margin-bottom: 24px; font-size: 14px; }
                                .device-info { background: #F2F2F7; padding: 12px; border-radius: 12px; margin-bottom: 24px; font-size: 14px; }
                                .info-row { display: flex; justify-content: space-between; margin-bottom: 4px; }
                                .info-label { color: #8E8E93; }
                                .info-value { font-weight: 600; }
                                .status-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 24px; }
                                .status-item { background: #F2F2F7; padding: 12px; border-radius: 12px; text-align: center; }
                                .status-label { font-size: 12px; color: #8E8E93; display: block; margin-bottom: 4px; }
                                .status-value { font-weight: bold; font-size: 16px; }
                                .active { color: var(--success); }
                                .inactive { color: #8E8E93; }
                                .token-section { margin-bottom: 24px; }
                                .token-section label { display: block; margin-bottom: 8px; font-weight: 600; font-size: 14px; }
                                input[type="password"] { width: 100%; padding: 12px; border-radius: 10px; border: 1px solid #C7C7CC; box-sizing: border-box; font-size: 16px; text-align: center; letter-spacing: 4px; }
                                .button-group { display: flex; flex-direction: column; gap: 12px; }
                                .row { display: flex; gap: 12px; }
                                button { flex: 1; padding: 16px; border-radius: 12px; border: none; font-weight: bold; font-size: 16px; cursor: pointer; transition: opacity 0.2s, transform 0.1s; }
                                button:active { transform: scale(0.98); }
                                .btn-main { background: var(--primary); color: white; }
                                .btn-stop { background: var(--danger); color: white; }
                                .btn-outline { background: #E5E5EA; color: #000; }
                                .result-box { margin-top: 24px; padding: 12px; border-radius: 12px; font-size: 14px; text-align: center; display: none; }
                                .instructions { font-size: 13px; color: #8E8E93; margin-top: 10px; line-height: 1.5; }
                                .instructions ul { padding-left: 20px; margin: 8px 0; }
                            </style>
                        </head>
                        <body>
                            <div class="card">
                                <h1>Local Find</h1>
                                <div class="subtitle">电脑端零安装控制页</div>
                                
                                <div class="device-info">
                                    <div class="info-row"><span class="info-label">当前设备:</span> <span class="info-value" id="device-name">$deviceName</span></div>
                                    <div class="info-row"><span class="info-label">服务地址:</span> <span class="info-value" id="service-addr">--</span></div>
                                </div>

                                <div class="status-grid">
                                    <div class="status-item"><span class="status-label">服务状态</span><span class="status-value active" id="service-status">在线</span></div>
                                    <div class="status-item"><span class="status-label">可访问性</span><span class="status-value active" id="reachability">可连接</span></div>
                                    <div class="status-item"><span class="status-label">响铃状态</span><span class="status-value" id="ring-status">--</span></div>
                                    <div class="status-item"><span class="status-label">闪光模式</span><span class="status-value" id="flash-status">--</span></div>
                                </div>

                                <div class="token-section">
                                    <label for="token-input">输入 8 位配对 Token</label>
                                    <input type="password" id="token-input" maxlength="8" placeholder="••••••••">
                                </div>

                                <div class="button-group">
                                    <button class="btn-main" onclick="startFinding()">开始寻找手机</button>
                                    <div class="row">
                                        <button class="btn-outline" onclick="toggleRing()" id="btn-ring">响铃</button>
                                        <button class="btn-outline" onclick="toggleFlash()" id="btn-flash">闪光</button>
                                    </div>
                                    <button class="btn-stop" onclick="callApi('/command/stop-all')">停止寻找 / 全部停止</button>
                                    <button class="btn-outline" onclick="updateStatus()">刷新状态</button>
                                </div>

                                <div id="result-box" class="result-box"></div>
                            </div>

                            <div class="card" style="padding: 16px;">
                                <div class="instructions">
                                    <strong>使用说明:</strong>
                                    <ul>
                                        <li>电脑端无需安装任何软件。</li>
                                        <li>请确保电脑和手机在同一 Wi-Fi / 局域网。</li>
                                        <li>控制命令需要被寻找手机当前显示的 8 位 Token。</li>
                                        <li>Chrome 插件快捷控制将在后续版本提供。</li>
                                    </ul>
                                </div>
                            </div>

                            <script>
                                const tokenInput = document.getElementById('token-input');
                                const serviceAddrEl = document.getElementById('service-addr');
                                const resultBox = document.getElementById('result-box');

                                // Set addr from browser location
                                serviceAddrEl.textContent = location.protocol + '//' + location.host + '/';

                                async function showResult(msg, isError = false) {
                                    resultBox.textContent = msg;
                                    resultBox.style.display = 'block';
                                    resultBox.style.background = isError ? '#FFD6D6' : '#D6FFDA';
                                    resultBox.style.color = isError ? '#D00' : '#080';
                                    setTimeout(() => { resultBox.style.display = 'none'; }, 5000);
                                }

                                async function updateStatus() {
                                    try {
                                        const res = await fetch('/status');
                                        if (!res.ok) throw new Error('status_error');
                                        const data = await res.json();
                                        
                                        document.getElementById('service-status').textContent = '在线';
                                        document.getElementById('reachability').textContent = '可连接';
                                        
                                        const ringStatus = document.getElementById('ring-status');
                                        ringStatus.textContent = data.ring_active ? '鸣叫中' : '静音';
                                        ringStatus.className = 'status-value ' + (data.ring_active ? 'active' : 'inactive');

                                        const flashStatus = document.getElementById('flash-status');
                                        flashStatus.textContent = data.flash_mode === 'off' ? '关闭' : (data.flash_mode === 'strobe' ? '爆闪' : '常亮');
                                        flashStatus.className = 'status-value ' + (data.flash_mode !== 'off' ? 'active' : 'inactive');
                                    } catch (e) {
                                        document.getElementById('service-status').textContent = '未知';
                                        document.getElementById('reachability').textContent = '连接失败';
                                        showResult('无法连接到手机，请检查网络', true);
                                    }
                                }

                                async function callApi(endpoint) {
                                    const token = tokenInput.value;
                                    if (!token) {
                                        showResult('请输入 Token', true);
                                        return;
                                    }
                                    try {
                                        const res = await fetch(endpoint, {
                                            method: 'POST',
                                            headers: { 'X-LocalFind-Token': token }
                                        });
                                        if (res.status === 401) {
                                            showResult('Token 错误或已失效', true);
                                        } else if (res.status === 200) {
                                            showResult('命令已发送');
                                            setTimeout(updateStatus, 500);
                                        } else {
                                            showResult('操作失败 (' + res.status + ')', true);
                                        }
                                    } catch (e) {
                                        showResult('网络错误或请求超时', true);
                                    }
                                }

                                function startFinding() {
                                    const token = tokenInput.value;
                                    if (!token) {
                                        showResult('请输入 Token', true);
                                        return;
                                    }
                                    showResult('正在发送开始指令...');
                                    fetch('/command/ring/start', { method: 'POST', headers: { 'X-LocalFind-Token': token } })
                                        .then(() => fetch('/command/flash/strobe/start', { method: 'POST', headers: { 'X-LocalFind-Token': token } }))
                                        .then(() => { showResult('开始寻找手机'); setTimeout(updateStatus, 500); })
                                        .catch(() => showResult('指令发送失败', true));
                                }

                                function toggleRing() {
                                    const isActive = document.getElementById('ring-status').classList.contains('active');
                                    callApi(isActive ? '/command/ring/stop' : '/command/ring/start');
                                }

                                function toggleFlash() {
                                    const isActive = document.getElementById('flash-status').classList.contains('active');
                                    callApi(isActive ? '/command/flash/stop' : '/command/flash/strobe/start');
                                }

                                updateStatus();
                                setInterval(updateStatus, 5000);
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    call.respondText(html, ContentType.Text.Html)
                }

                // GET /status - Read-only memory state, never blocks
                get("/status") {
                    val statusJson = buildJsonObject {
                        put("service", "running")
                        put("ring_active", isRingActive)
                        put("flash_mode", flashMode)
                    }
                    call.respond(statusJson)
                }

                // Hardware Commands (Fire-and-forget)
                post("/command/ring/start") {
                    authenticate {
                        commandDispatcher.startRing()
                        call.respond(buildJsonObject { 
                            put("success", true)
                            put("message", "Command accepted") 
                        })
                    }
                }

                post("/command/ring/stop") {
                    authenticate {
                        commandDispatcher.stopRing()
                        call.respond(buildJsonObject { 
                            put("success", true)
                            put("message", "Command accepted") 
                        })
                    }
                }

                post("/command/flash/steady/start") {
                    authenticate {
                        commandDispatcher.startFlashSteady()
                        call.respond(buildJsonObject { 
                            put("success", true)
                            put("message", "Command accepted") 
                        })
                    }
                }

                post("/command/flash/strobe/start") {
                    authenticate {
                        commandDispatcher.startFlashStrobe()
                        call.respond(buildJsonObject { 
                            put("success", true)
                            put("message", "Command accepted") 
                        })
                    }
                }

                post("/command/flash/stop") {
                    authenticate {
                        commandDispatcher.stopFlash()
                        call.respond(buildJsonObject { 
                            put("success", true)
                            put("message", "Command accepted") 
                        })
                    }
                }

                post("/command/stop-all") {
                    authenticate {
                        commandDispatcher.stopAll()
                        call.respond(buildJsonObject { 
                            put("success", true)
                            put("message", "Command accepted") 
                        })
                    }
                }
            }
        }
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.authenticate(body: suspend () -> Unit) {
        val headerToken = call.request.headers["X-LocalFind-Token"]
        val queryToken = call.request.queryParameters["token"]
        val validToken = tokenManager.getToken()

        val matchesGlobalToken = validToken != null && (headerToken == validToken || queryToken == validToken)
        val matchesPairedControllerToken = pairedControllerTokenStore.isValidToken(headerToken)

        if (matchesGlobalToken || matchesPairedControllerToken) {
            body()
        } else {
            call.respond(
                HttpStatusCode.Unauthorized, 
                buildJsonObject {
                    put("success", false)
                    put("message", "Unauthorized: Invalid or missing token")
                }
            )
        }
    }

    fun enablePairingMode(ttlMillis: Long = PAIRING_MODE_TTL_MILLIS) {
        pairingModeExpiresAt = System.currentTimeMillis() + ttlMillis
        pairingRequestStore.expireOldRequests()
        onStatusChange()
    }

    fun disablePairingMode() {
        pairingModeExpiresAt = 0L
        onStatusChange()
    }

    fun isPairingModeActive(): Boolean {
        val active = pairingModeExpiresAt > System.currentTimeMillis()
        if (!active && pairingModeExpiresAt != 0L) {
            pairingModeExpiresAt = 0L
            pairingRequestStore.expireOldRequests()
            onStatusChange()
        }
        return active
    }

    fun getPairingModeExpiresAt(): Long = if (isPairingModeActive()) pairingModeExpiresAt else 0L

    fun getPendingPairingRequests(): List<PairingRequest> = pairingRequestStore.getPending()

    fun acceptPairingRequest(requestId: String): PairingRequest? {
        val request = pairingRequestStore.get(requestId)
        if (request == null || request.status != PairingRequestStore.STATUS_PENDING) return request

        val controlToken = pairedControllerTokenStore.issueToken(
            controllerId = request.controllerId,
            controllerName = request.controllerName,
            controllerType = request.controllerType,
        )
        val updated = pairingRequestStore.accept(requestId, controlToken)
        onStatusChange()
        return updated
    }

    fun rejectPairingRequest(requestId: String): PairingRequest? {
        val updated = pairingRequestStore.reject(requestId)
        onStatusChange()
        return updated
    }

    private fun pairingStatusJson(request: PairingRequest) = buildJsonObject {
        put("pairingMode", isPairingModeActive())
        put("requestId", request.requestId)
        put("status", request.status)

        if (request.status == PairingRequestStore.STATUS_ACCEPTED && !request.controlToken.isNullOrBlank()) {
            val identity = localDeviceIdentityStore.getOrCreate()
            put("device", buildJsonObject {
                put("id", identity.id)
                put("name", identity.name)
                put("type", identity.type)
                put("port", getPort())
            })
            put("controlToken", request.controlToken)
        }
    }

    @Synchronized
    fun start() {
        if (server != null) return
        
        currentStatus = ServerStatus.STARTING
        onStatusChange()

        scope.launch {
            try {
                lastServerError = null
                val newServer = createServer()
                server = newServer
                newServer.start(wait = false)
                currentStatus = ServerStatus.LISTENING
                onStatusChange()
                Log.d("HttpServerManager", "Ktor server listening on port ${getPort()}")
            } catch (e: Exception) {
                currentStatus = ServerStatus.FAILED
                lastServerError = e.message ?: e.toString()
                server = null
                onStatusChange()
                Log.e("HttpServerManager", "Error starting Ktor server", e)
            }
        }
    }

    /**
     * Restarts the server in a true sequential manner.
     */
    @Synchronized
    fun restart() {
        scope.launch {
            try {
                currentStatus = ServerStatus.STARTING
                lastServerError = null
                onStatusChange()

                // 1. Synchronously stop existing instance
                server?.stop(500, 1000)
                server = null
                
                // 2. Create and start new instance
                val newServer = createServer()
                server = newServer
                newServer.start(wait = false)
                
                currentStatus = ServerStatus.LISTENING
                onStatusChange()
                Log.d("HttpServerManager", "Ktor server restarted successfully")
            } catch (e: Exception) {
                currentStatus = ServerStatus.FAILED
                lastServerError = e.message ?: e.toString()
                server = null
                onStatusChange()
                Log.e("HttpServerManager", "Error restarting Ktor server", e)
            }
        }
    }

    @Synchronized
    fun stopServerOnly() {
        val s = server
        server = null
        currentStatus = ServerStatus.STOPPED
        scope.launch {
            try {
                s?.stop(500, 1000)
                onStatusChange()
            } catch (e: Exception) {
                Log.e("HttpServerManager", "Error stopping Ktor server", e)
            }
        }
    }

    @Synchronized
    fun shutdownAll() {
        stopServerOnly()
        commandDispatcher.shutdown()
    }

    @Deprecated("Use stopServerOnly() or shutdownAll()", ReplaceWith("stopServerOnly()"))
    fun stop() {
        stopServerOnly()
    }

    companion object {
        const val PAIRING_MODE_TTL_MILLIS = 5 * 60 * 1000L
    }
}
