package me.justlime.dummyplayer.behaviour

import com.hypixel.hytale.server.core.universe.Universe
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

//TODO
object DummyTicker {

    // Create a dedicated thread for the timer
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var scheduledTask: ScheduledFuture<*>? = null

    fun start() {
        if (scheduledTask != null && !scheduledTask!!.isCancelled) return

        // Run every 50ms (20 Ticks Per Second)
        scheduledTask = scheduler.scheduleAtFixedRate({
            try {
                // Iterate safely over all loaded worlds
                Universe.get().worlds.forEach { (name, world) ->
                    // dispatch the tick logic to the world's main thread
                    world.execute {
                        _root_ide_package_.me.justlime.dummyplayer.behaviour.DummyAI.tick(world)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0L, 30L, TimeUnit.MILLISECONDS)

        println("DummyAI Ticker started.")
    }

    fun stop() {
        scheduledTask?.cancel(false)
        scheduledTask = null
        println("DummyAI Ticker stopped.")
    }
}