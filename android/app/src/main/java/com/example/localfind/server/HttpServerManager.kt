package com.example.localfind.server

import android.util.Log
import com.example.localfind.auth.PairingTokenManager
import com.example.localfind.hardware.FlashlightController
import com.example.localfind.hardware.RingController
import com.example.localfind.util.NetworkUtil
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
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HttpServerManager(
    private val ringController: RingController,
    private val flashlightController: FlashlightController,
    private val tokenManager: PairingTokenManager,
    private val onStatusChange: () -> Unit
) {
    private var server: NettyApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var isRingActive = false
        private set
    var flashMode: String = "off" // "off", "steady", "strobe"
        private set

    fun getPort(): Int = 8888

    /**
     * 校验 Token 是否正确
     */
    private suspend fun PipelineContext<Unit, ApplicationCall>.authenticate(body: suspend () -> Unit) {
        val headerToken = call.request.headers["X-LocalFind-Token"]
        val queryToken = call.request.queryParameters["token"]
        val validToken = tokenManager.getToken()

        if (validToken != null && (headerToken == validToken || queryToken == validToken)) {
            body()
        } else {
            call.respond(HttpStatusCode.Unauthorized, buildJsonObject {
                put("success", false)
                put("message", "Unauthorized: Invalid or missing token")
            })
        }
    }

    /**
     * 启动本地 Ktor Web 服务，监听端口 8888
     */
    @Synchronized
    fun start() {
        if (server != null) return
        
        scope.launch {
            try {
                server = embeddedServer(Netty, port = getPort(), host = "0.0.0.0") {
                    install(ContentNegotiation) {
                        json()
                    }
                    routing {
                        // 0. GET /  浏览器控制页
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

                        // 1. GET /status  获取服务和外设的实时运行姿态
                        get("/status") {
                            val statusJson = buildJsonObject {
                                put("service", "running")
                                put("ring_active", isRingActive)
                                put("flash_mode", flashMode)
                            }
                            call.respond(statusJson)
                        }

                        // 2. POST /command/ring/start  开始循环拉响警报音
                        post("/command/ring/start") {
                            authenticate {
                                isRingActive = true
                                ringController.startRing()
                                onStatusChange()
                                call.respond(buildJsonObject { 
                                    put("success", true)
                                    put("message", "Ring started") 
                                })
                            }
                        }

                        // 3. POST /command/ring/stop  停止警报音
                        post("/command/ring/stop") {
                            authenticate {
                                isRingActive = false
                                ringController.stopRing()
                                onStatusChange()
                                call.respond(buildJsonObject { 
                                    put("success", true)
                                    put("message", "Ring stopped") 
                                })
                            }
                        }

                        // 4. POST /command/flash/steady/start  开启手电筒常亮
                        post("/command/flash/steady/start") {
                            authenticate {
                                flashMode = "steady"
                                flashlightController.startSteady()
                                onStatusChange()
                                call.respond(buildJsonObject { 
                                    put("success", true)
                                    put("message", "Steady flashlight started") 
                                })
                            }
                        }

                        // 5. POST /command/flash/strobe/start  开启 200ms 的爆闪
                        post("/command/flash/strobe/start") {
                            authenticate {
                                flashMode = "strobe"
                                flashlightController.startStrobe()
                                onStatusChange()
                                call.respond(buildJsonObject { 
                                    put("success", true)
                                    put("message", "Strobe flashlight started") 
                                })
                            }
                        }

                        // 6. POST /command/flash/stop  强制熄灭手电
                        post("/command/flash/stop") {
                            authenticate {
                                flashMode = "off"
                                flashlightController.stopAll()
                                onStatusChange()
                                call.respond(buildJsonObject { 
                                    put("success", true)
                                    put("message", "Flashlight stopped") 
                                })
                            }
                        }

                        // 7. POST /command/stop-all  熄灭灯光并停响警报
                        post("/command/stop-all") {
                            authenticate {
                                isRingActive = false
                                flashMode = "off"
                                ringController.stopRing()
                                flashlightController.stopAll()
                                onStatusChange()
                                call.respond(buildJsonObject { 
                                    put("success", true)
                                    put("message", "All hardware alerts stopped") 
                                })
                            }
                        }
                    }
                }
                server?.start(wait = false)
                Log.d("HttpServerManager", "Ktor server listening on port ${getPort()}")
            } catch (e: Exception) {
                Log.e("HttpServerManager", "Error starting Ktor server", e)
            }
        }
    }

    /**
     * 关闭服务释放资源
     */
    @Synchronized
    fun stop() {
        isRingActive = false
        flashMode = "off"
        ringController.stopRing()
        flashlightController.stopAll()
        
        scope.launch {
            try {
                server?.let {
                    it.stop(1000, 3000)
                    Log.d("HttpServerManager", "Ktor server successfully shut down")
                }
                server = null
            } catch (e: Exception) {
                Log.e("HttpServerManager", "Error during Ktor shutdown cleanup", e)
            }
        }
    }
}
