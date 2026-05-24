package io.github.linkwutcreate.localfind.server

import android.util.Log
import io.github.linkwutcreate.localfind.hardware.FlashlightController
import io.github.linkwutcreate.localfind.hardware.RingController
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Hardware Command Dispatcher:
 * Executes hardware operations in a background coroutine to avoid blocking Ktor HTTP responses.
 */
class HardwareCommandDispatcher(
    private val ringController: RingController,
    private val flashlightController: FlashlightController,
    private val onStatusChange: () -> Unit,
) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // Using Atomic classes for fast, non-blocking memory access
    private val ringActive = AtomicBoolean(false)
    private val flashMode = AtomicReference("off")

    fun isRingActive(): Boolean = ringActive.get()
    fun getFlashMode(): String = flashMode.get()

    /**
     * Enqueues a hardware command for asynchronous execution.
     * Returns immediately without blocking the caller.
     */
    fun enqueueCommand(commandName: String, action: () -> Unit) {
        scope.launch {
            try {
                withTimeoutOrNull(2000) {
                    Log.d("HardwareDispatcher", "Executing command: $commandName")
                    action()
                } ?: Log.e("HardwareDispatcher", "Command timed out: $commandName")
            } catch (e: Exception) {
                Log.e("HardwareDispatcher", "Command failed: $commandName", e)
            } finally {
                onStatusChange()
            }
        }
    }

    fun startRing() {
        ringActive.set(true)
        enqueueCommand("startRing") { ringController.startRing() }
    }

    fun stopRing() {
        ringActive.set(false)
        enqueueCommand("stopRing") { ringController.stopRing() }
    }

    fun startFlashSteady() {
        flashMode.set("steady")
        enqueueCommand("startFlashSteady") { flashlightController.startSteady() }
    }

    fun startFlashStrobe() {
        flashMode.set("strobe")
        enqueueCommand("startFlashStrobe") { flashlightController.startStrobe() }
    }

    fun stopFlash() {
        flashMode.set("off")
        enqueueCommand("stopFlash") { flashlightController.stopAll() }
    }

    fun stopAll() {
        ringActive.set(false)
        flashMode.set("off")
        enqueueCommand("stopAll") {
            ringController.stopRing()
            flashlightController.stopAll()
        }
    }

    /**
     * Synchronously stops all hardware actions.
     * Used for final cleanup during service shutdown.
     */
    fun stopAllImmediately() {
        try {
            ringActive.set(false)
            flashMode.set("off")
            ringController.stopRing()
            flashlightController.stopAll()
        } catch (e: Exception) {
            Log.e("HardwareDispatcher", "Error during immediate stop", e)
        } finally {
            onStatusChange()
        }
    }

    /**
     * Cancels all pending and running hardware commands.
     * Should only be called when the entire Find service is shutting down.
     */
    fun shutdown() {
        stopAllImmediately()
        job.cancel()
    }
}
