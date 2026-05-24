package io.github.linkwutcreate.localfind.server

import android.util.Log
import io.github.linkwutcreate.localfind.auth.PairingTokenManager
import io.github.linkwutcreate.localfind.hardware.FlashlightController
import io.github.linkwutcreate.localfind.hardware.RingController
import io.github.linkwutcreate.localfind.model.PairingRequest
import io.github.linkwutcreate.localfind.store.LocalDeviceIdentityStore
import io.github.linkwutcreate.localfind.store.PairedControllerTokenStore
import io.github.linkwutcreate.localfind.store.PairingRequestStore
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
                                <div class="subtitle">Remote Control in Browser &mdash; no install needed</div>

                                <div class="device-info">
                                    <div class="info-row"><span class="info-label">Device:</span> <span class="info-value" id="device-name">$deviceName</span></div>
                                    <div class="info-row"><span class="info-label">Address:</span> <span class="info-value" id="service-addr">--</span></div>
                                </div>

                                <div class="status-grid">
                                    <div class="status-item"><span class="status-label" data-l="service">Service</span><span class="status-value active" id="service-status">Online</span></div>
                                    <div class="status-item"><span class="status-label" data-l="reachable">Reachable</span><span class="status-value active" id="reachability">Yes</span></div>
                                    <div class="status-item"><span class="status-label" data-l="ring">Ring</span><span class="status-value" id="ring-status">--</span></div>
                                    <div class="status-item"><span class="status-label" data-l="flash">Flash</span><span class="status-value" id="flash-status">--</span></div>
                                </div>

                                <div class="token-section">
                                    <label for="token-input" data-l="enterToken">Enter 8-digit Token</label>
                                    <input type="password" id="token-input" maxlength="8" placeholder="&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;">
                                </div>

                                <div class="button-group">
                                    <button class="btn-main" onclick="startFinding()" data-l="findPhone">Find Phone</button>
                                    <div class="row">
                                        <button class="btn-outline" onclick="toggleRing()" id="btn-ring" data-l="ringBtn">Ring</button>
                                        <button class="btn-outline" onclick="toggleFlash()" id="btn-flash" data-l="flashBtn">Flash</button>
                                    </div>
                                    <button class="btn-stop" onclick="callApi('/command/stop-all')" data-l="stopAll">Stop All Alerts</button>
                                    <button class="btn-outline" onclick="updateStatus()" data-l="refresh">Refresh</button>
                                </div>

                                <div id="result-box" class="result-box"></div>
                            </div>

                            <div class="card" style="padding: 16px;">
                                <div class="instructions">
                                    <strong data-l="howToUse">How to Use</strong>
                                    <ul>
                                        <li data-l="how1">No software installation needed on your computer.</li>
                                        <li data-l="how2">Make sure the computer and phone are on the same Wi-Fi / LAN.</li>
                                        <li data-l="how3">Enter the 8-digit token shown on the phone screen.</li>
                                        <li data-l="how4">For quick access, use the Chrome extension instead.</li>
                                    </ul>
                                    <strong data-l="security">Security</strong>
                                    <ul>
                                        <li data-l="sec1">Local Find is intended for trusted local networks only.</li>
                                        <li data-l="sec2">Do not expose this server to the public internet.</li>
                                        <li data-l="sec3">Pair only trusted devices.</li>
                                        <li data-l="sec4">Reset the token if you suspect it has leaked.</li>
                                    </ul>
                                </div>
                            </div>

                            <script>
                                // --- i18n ---
                                const L = {
                                  en: {
                                    service:"Service", reachable:"Reachable", ring:"Ring", flash:"Flash",
                                    enterToken:"Enter 8-digit Token", findPhone:"Find Phone", ringBtn:"Ring",
                                    flashBtn:"Flash", stopAll:"Stop All Alerts", refresh:"Refresh",
                                    howToUse:"How to Use", how1:"No software installation needed on your computer.",
                                    how2:"Make sure the computer and phone are on the same Wi-Fi / LAN.",
                                    how3:"Enter the 8-digit token shown on the phone screen.",
                                    how4:"For quick access, use the Chrome extension instead.",
                                    security:"Security",
                                    sec1:"Local Find is intended for trusted local networks only.",
                                    sec2:"Do not expose this server to the public internet.",
                                    sec3:"Pair only trusted devices.",
                                    sec4:"Reset the token if you suspect it has leaked.",
                                    online:"Online", offline:"Offline", yes:"Yes", no:"No",
                                    ringing:"Ringing", silent:"Silent", steady:"Steady", strobe:"Strobe", off:"Off",
                                    tokenErr:"Token incorrect or expired", cmdSent:"Command sent",
                                    cmdFailed:"Failed", netErr:"Network error or timeout",
                                    sending:"Sending...", findStarted:"Find Phone started",
                                    cantReach:"Cannot reach phone. Check network.", enterTok:"Enter token"
                                  },
                                  zh: {
                                    service:"服务", reachable:"可访问", ring:"响铃", flash:"闪光",
                                    enterToken:"输入 8 位令牌", findPhone:"查找手机", ringBtn:"响铃",
                                    flashBtn:"闪光", stopAll:"停止所有警报", refresh:"刷新",
                                    howToUse:"使用说明",
                                    how1:"电脑端无需安装任何软件。",
                                    how2:"请确保电脑和手机在同一 Wi-Fi / 局域网。",
                                    how3:"输入手机屏幕显示的 8 位令牌。",
                                    how4:"推荐使用 Chrome 插件获得更快捷的体验。",
                                    security:"安全提示",
                                    sec1:"Local Find 仅限可信局域网使用。",
                                    sec2:"请勿将此服务暴露到公网。",
                                    sec3:"仅配对可信设备。",
                                    sec4:"如怀疑令牌泄露，请重置令牌。",
                                    online:"在线", offline:"离线", yes:"是", no:"否",
                                    ringing:"响铃中", silent:"静音", steady:"常亮", strobe:"闪烁", off:"关闭",
                                    tokenErr:"令牌错误或已失效", cmdSent:"命令已发送",
                                    cmdFailed:"失败", netErr:"网络错误或超时",
                                    sending:"发送中...", findStarted:"已开始查找手机",
                                    cantReach:"无法连接手机，请检查网络。", enterTok:"请输入令牌"
                                  }
                                };
                                const qp = new URLSearchParams(location.search);
                                const langParam = qp.get("lang") || "en";
                                const lang = langParam === "zh" ? "zh" : "en";
                                function lt(k) { return (L[lang] || L.en)[k] || L.en[k] || k; }
                                function applyLang() {
                                  document.querySelector(".subtitle").innerHTML = lang === "zh" ? "浏览器远程控制 &mdash; 无需安装" : "Remote Control in Browser &mdash; no install needed";
                                  var els = document.querySelectorAll("[data-l]");
                                  els.forEach(function(el) { el.textContent = lt(el.dataset.l); });
                                }
                                document.addEventListener("DOMContentLoaded", applyLang);

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

                                        document.getElementById('service-status').textContent = lt('online');
                                        document.getElementById('reachability').textContent = lt('yes');

                                        const ringStatus = document.getElementById('ring-status');
                                        ringStatus.textContent = data.ring_active ? lt('ringing') : lt('silent');
                                        ringStatus.className = 'status-value ' + (data.ring_active ? 'active' : 'inactive');

                                        const flashStatus = document.getElementById('flash-status');
                                        flashStatus.textContent = data.flash_mode === 'off' ? lt('off') : (data.flash_mode === 'strobe' ? lt('strobe') : lt('steady'));
                                        flashStatus.className = 'status-value ' + (data.flash_mode !== 'off' ? 'active' : 'inactive');
                                    } catch (e) {
                                        document.getElementById('service-status').textContent = lt('offline');
                                        document.getElementById('reachability').textContent = lt('no');
                                        showResult(lt('cantReach'), true);
                                    }
                                }

                                async function callApi(endpoint) {
                                    const token = tokenInput.value;
                                    if (!token) {
                                        showResult(lt('enterTok'), true);
                                        return;
                                    }
                                    try {
                                        const res = await fetch(endpoint, {
                                            method: 'POST',
                                            headers: { 'X-LocalFind-Token': token }
                                        });
                                        if (res.status === 401) {
                                            showResult(lt('tokenErr'), true);
                                        } else if (res.status === 200) {
                                            showResult(lt('cmdSent'));
                                            setTimeout(updateStatus, 500);
                                        } else {
                                            showResult(lt('cmdFailed') + ' (' + res.status + ')', true);
                                        }
                                    } catch (e) {
                                        showResult(lt('netErr'), true);
                                    }
                                }

                                function startFinding() {
                                    const token = tokenInput.value;
                                    if (!token) {
                                        showResult(lt('enterTok'), true);
                                        return;
                                    }
                                    showResult(lt('sending'));
                                    fetch('/command/ring/start', { method: 'POST', headers: { 'X-LocalFind-Token': token } })
                                        .then(() => fetch('/command/flash/strobe/start', { method: 'POST', headers: { 'X-LocalFind-Token': token } }))
                                        .then(() => { showResult(lt('findStarted')); setTimeout(updateStatus, 500); })
                                        .catch(() => showResult(lt('cmdFailed'), true));
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
