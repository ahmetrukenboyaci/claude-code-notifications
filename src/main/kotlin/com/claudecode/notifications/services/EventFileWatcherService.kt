package com.claudecode.notifications.services

import com.claudecode.notifications.model.ClaudeCodeEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class EventFileWatcherService : Disposable {

    private val logger = Logger.getInstance(EventFileWatcherService::class.java)
    private val running = AtomicBoolean(false)
    private var watchThread: Thread? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val watchDir: Path = Paths.get(System.getProperty("user.home"), ".claude-code-notifications")
    private val failedDir: Path = watchDir.resolve(".failed")

    fun start() {
        if (running.getAndSet(true)) return

        Files.createDirectories(watchDir)
        Files.createDirectories(failedDir)

        processExistingFiles()

        watchThread = Thread({
            try {
                runWatchLoop()
            } catch (e: InterruptedException) {
                // Normal shutdown
            } catch (e: ClosedWatchServiceException) {
                // Normal shutdown
            } catch (e: Exception) {
                logger.error("Watch loop crashed", e)
            } finally {
                running.set(false)
            }
        }, "claude-code-event-watcher")
        watchThread!!.isDaemon = true
        watchThread!!.start()

        logger.info("EventFileWatcherService started, watching: $watchDir")
    }

    private fun runWatchLoop() {
        val watchService = FileSystems.getDefault().newWatchService()
        watchDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE)

        while (running.get()) {
            val key = watchService.poll(2, TimeUnit.SECONDS) ?: continue
            for (event in key.pollEvents()) {
                val kind = event.kind()
                if (kind == StandardWatchEventKinds.OVERFLOW) continue

                @Suppress("UNCHECKED_CAST")
                val ev = event as WatchEvent<Path>
                val fileName = ev.context().toString()

                if (fileName.endsWith(".json") && !fileName.startsWith(".tmp-")) {
                    val filePath = watchDir.resolve(fileName)
                    processEventFile(filePath)
                }
            }
            key.reset()
        }

        watchService.close()
    }

    private fun processExistingFiles() {
        try {
            Files.list(watchDir)
                .filter { it.toString().endsWith(".json") && !it.fileName.toString().startsWith(".tmp-") }
                .sorted()
                .forEach { processEventFile(it) }
        } catch (e: Exception) {
            logger.warn("Error processing existing files", e)
        }
    }

    private fun processEventFile(filePath: Path) {
        val processingName = ".processing-${filePath.fileName}"
        val processingPath = watchDir.resolve(processingName)

        try {
            // Atomic claim: first IDE to rename wins
            Files.move(filePath, processingPath, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: Exception) {
            // Another instance already claimed this file
            return
        }

        try {
            val content = Files.readString(processingPath)
            val event = json.decodeFromString<ClaudeCodeEvent>(content)

            val dispatcher = ApplicationManager.getApplication()
                .getService(NotificationDispatcher::class.java)
            dispatcher.dispatch(event)

            Files.deleteIfExists(processingPath)
        } catch (e: Exception) {
            logger.warn("Failed to process event file: ${filePath.fileName}", e)
            try {
                Files.move(processingPath, failedDir.resolve(filePath.fileName), StandardCopyOption.REPLACE_EXISTING)
            } catch (moveError: Exception) {
                logger.warn("Failed to move bad file to .failed/", moveError)
                Files.deleteIfExists(processingPath)
            }
        }
    }

    override fun dispose() {
        running.set(false)
        watchThread?.interrupt()
        watchThread?.join(3000)
        watchThread = null
        logger.info("EventFileWatcherService stopped")
    }
}
