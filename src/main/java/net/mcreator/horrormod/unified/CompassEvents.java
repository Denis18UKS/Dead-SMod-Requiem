package net.mcreator.horrormod.unified;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Forge game-bus events run on both logical sides; changes occur only server-side. */
public final class CompassEvents {
    private CompassEvents() {}

    @SubscribeEvent
    public static void itemRightClick(PlayerInteractEvent.RightClickItem event) {
        DeadHooks.onCompassInteract(event);
    }

    @SubscribeEvent
    public static void blockRightClick(PlayerInteractEvent.RightClickBlock event) {
        DeadHooks.onCompassInteract(event);
    }
}
