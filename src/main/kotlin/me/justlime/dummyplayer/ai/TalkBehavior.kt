package me.justlime.dummyplayer.ai

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import me.justlime.dummyplayer.behaviour.DummyBehavior

class TalkBehavior(private val message: String, private val intervalTicks: Int) :
    me.justlime.dummyplayer.behaviour.DummyBehavior {
    private var ticks = 0
    override fun tick(world: World, dummyRef: Ref<EntityStore>, dummyName: String) {
        ticks++
        if (ticks >= intervalTicks) {
            ticks = 0
            world.playerRefs.forEach { p ->
                p.sendMessage(Message.raw("<$dummyName> $message"))
            }
        }
    }
}
