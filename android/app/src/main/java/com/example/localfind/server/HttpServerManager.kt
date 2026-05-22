package com.example.localfind.server

import android.util.Log
import com.example.localfind.auth.PairingTokenManager
import com.example.localfind.hardware.FlashlightController
import com.example.localfind.hardware.RingController
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
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

                // GET / - Browser control page
                get("/") {
                    val html = """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Local Find Control Panel</title>
                            <style>
                                body { font-family: sans-serif; max-width: 600px; margin: 20px auto; padding: 20px; line-height: 1.6; background-color: #f0f2f5; color: #1c1e21; }
                                .container { background: white; padding: 20px; border-radius: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                                h1 { color: #1877f2; text-align: center; margin-bottom: 24px; }
                                .status-section { background: #f7f8fa; padding: 15px; border-radius: 8px; margin-bottom: 24px; border: 1px solid #ddd; }
                                .status-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 16px; }
                                .status-row:last-child { margin-bottom: 0; }
                                .label { font-weight: bold; color: #65676b; }
                                .value { font-family: monospace; font-weight: bold; }
                                .active { color: #28a745; }
                                .inactive { color: #dc3545; }
                                .button-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
                                button { padding: 14px; font-size: 16px; border: none; border-radius: 8px; cursor: pointer; font-weight: bold; transition: background 0.2s, transform 0.1s; }
                                button:active { transform: scale(0.98); }
                                .btn-primary { background: #1877f2; color: white; }
                                .btn-primary:hover { background: #166fe5; }
                                .btn-danger { background: #f02849; color: white; }
                                .btn-danger:hover { background: #d92241; }
                                .btn-warning { background: #f7b924; color: white; }
                                .btn-warning:hover { background: #e0a721; }
                                .btn-secondary { background: #e4e6eb; color: #050505; }
                                .btn-secondary:hover { background: #d8dadf; }
                                .btn-full { grid-column: span 2; margin-top: 8px; }
                                .btn-stop-all { background: #4b4f56; color: white; }
                                .btn-stop-all:hover { background: #393d42; }
                                @media (max-width: 480px) {
                                    .button-grid { grid-template-columns: 1fr; }
                                    .btn-full { grid-column: span 1; }
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <h1>Local Find Control Panel</h1>
                                <div class="status-section">
                                    <div class="status-row"><span class="label">Service Status:</span> <span class="value active">RUNNING</span></div>
                                    <div class="status-row"><span class="label">Ring Status:</span> <span id="ring-val" class="value">LOADING...</span></div>
                                    <div class="status-row"><span class="label">Flash Status:</span> <span id="flash-val" class="value">LOADING...</span></div>
                                </div>
                                <div class="status-section">
                                    <div class="label" style="margin-bottom:8px">Pairing Token:</div>
                                    <input type="text" id="token-input" placeholder="Enter Token" style="width:100%; padding:10px; border-radius:6px; border:1px solid #ddd; box-sizing:border-box;">
                                </div>
                                <div class="button-grid">
                                    <button class="btn-primary" onclick="callApi('/command/ring/start', 'POST')">Start Ring</button>
                                    <button class="btn-danger" onclick="callApi('/command/ring/stop', 'POST')">Stop Ring</button>
                                    <button class="btn-warning" onclick="callApi('/command/flash/steady/start', 'POST')">Flash Steady</button>
                                    <button class="btn-warning" onclick="callApi('/command/flash/strobe/start', 'POST')">Flash Strobe</button>
                                    <button class="btn-danger" onclick="callApi('/command/flash/stop', 'POST')">Stop Flash</button>
                                    <button class="btn-secondary" onclick="updateStatus()">Refresh Status</button>
                                    <button class="btn-stop-all btn-full" onclick="callApi('/command/stop-all', 'POST')">Stop All</button>
                                </div>
                            </div>
                            <script>
                                const tokenInput = document.getElementById('token-input');
                                tokenInput.value = localStorage.getItem('localfind_token') || '';
                                tokenInput.oninput = () => localStorage.setItem('localfind_token', tokenInput.value);

                                async function updateStatus() {
                                    try {
                                        const res = await fetch('/status');
                                        const data = await res.json();
                                        const ringEl = document.getElementById('ring-val');
                                        const flashEl = document.getElementById('flash-val');
                                        
                                        ringEl.textContent = data.ring_active ? 'ACTIVE' : 'IDLE';
                                        ringEl.className = 'value ' + (data.ring_active ? 'active' : 'inactive');
                                        
                                        flashEl.textContent = data.flash_mode.toUpperCase();
                                        flashEl.className = 'value ' + (data.flash_mode !== 'off' ? 'active' : 'inactive');
                                    } catch (e) {
                                        console.error('Failed to update status', e);
                                    }
                                }

                                async function callApi(url, method) {
                                    const token = tokenInput.value;
                                    try {
                                        const res = await fetch(url, { 
                                            method: method,
                                            headers: { 'X-LocalFind-Token': token }
                                        });
                                        if (res.status === 401) {
                                            alert('Unauthorized: Please check your Token');
                                        }
                                        setTimeout(updateStatus, 100);
                                    } catch (e) {
                                        console.error('API call failed', e);
                                    }
                                }

                                updateStatus();
                                setInterval(updateStatus, 3000);
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

        if (validToken != null && (headerToken == validToken || queryToken == validToken)) {
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
}
