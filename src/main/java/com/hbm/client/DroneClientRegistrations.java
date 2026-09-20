package com.hbm.client;

import com.hbm.packet.DroneLinePayload;
import com.hbm.blocks.ModDroneBlocks;
import com.hbm.blocks.ModDroneContent;
import com.hbm.inventory.ModDroneMenus;
import com.hbm.items.ModEntityItems;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import com.hbm.inventory.gui.GUIDroneCrate;
import com.hbm.inventory.gui.GUIDroneDock;
import com.hbm.inventory.gui.GUIDroneProvider;
import com.hbm.inventory.gui.GUIDroneRequester;

/** Client-only registrations for the delivery drone blocks: screens, the link-line packet and its held-item gate. */
public final class DroneClientRegistrations {
    private DroneClientRegistrations() {}

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModDroneMenus.DRONE_CRATE.get(), GUIDroneCrate::new);
        sink.register(ModDroneMenus.DRONE_DOCK.get(), GUIDroneDock::new);
        sink.register(ModDroneMenus.DRONE_PROVIDER.get(), GUIDroneProvider::new);
        sink.register(ModDroneMenus.DRONE_REQUESTER.get(), GUIDroneRequester::new);
    }

    /** 1.7 ClientProxy {@code debugdrone}: the lines show only while a drone-family item is held. */
    public static void registerPacketHandlers() {
        DroneLineClient.holdsDroneItem = () -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return false;
            return isDroneItem(player.getMainHandItem()) || isDroneItem(player.getOffhandItem());
        };
        GolemNetwork.registerPlayToClient(DroneLinePayload.CODEC, DroneLineClient::handle);
    }

    private static boolean isDroneItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item == ModDroneContent.DRONE_LINKER.get()) return true;
        if (ModEntityItems.drone.typeOf(stack) != null) return true;
        Set<Item> blocks = Set.of(ModDroneBlocks.DRONE_CRATE_PROVIDER.get().asItem(),
                ModDroneBlocks.DRONE_CRATE_REQUESTER.get().asItem(), ModDroneBlocks.DRONE_DOCK.get().asItem(),
                ModDroneBlocks.DRONE_WAYPOINT_REQUEST.get().asItem(), ModDroneBlocks.DRONE_WAYPOINT.get().asItem());
        return blocks.contains(item);
    }
}
