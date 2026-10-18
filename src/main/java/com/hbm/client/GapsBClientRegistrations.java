package com.hbm.client;

import com.hbm.blocks.ModEnergyNetContent;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.RenderInfoSystem;
import com.hbm.client.render.RenderPipeAnchor;
import com.hbm.inventory.ModHeldCrateMenus;
import com.hbm.inventory.gui.GUICartCrate;
import com.hbm.inventory.gui.GUICrateTungsten;
import com.hbm.inventory.gui.GUISafe;
import com.hbm.items.tool.ItemWrench;
import com.hbm.tileentity.ModFluidNetBlockEntities;
import dev.golem.core.client.ClientTicks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Client side of the gap batch B systems: the screens of crates opened from the hand, the pipe anchor renderer
 * (anchor + pipelines) and the pipe wrench's distance readout.
 */
public final class GapsBClientRegistrations {

    private GapsBClientRegistrations() {}

    /** 1.7 {@code ServerProxy.ID_WRENCH}. */
    private static final int ID_WRENCH = 13;
    private static boolean ticking;

    public static void registerScreens(HbmClientRegistrations.ScreenSink sink) {
        sink.register(ModHeldCrateMenus.CRATE_HELD.get(), GUICartCrate::new);
        sink.register(ModHeldCrateMenus.CRATE_TUNGSTEN_HELD.get(), GUICrateTungsten::new);
        sink.register(ModHeldCrateMenus.SAFE_HELD.get(), GUISafe::new);
        if (!ticking) {
            ticking = true;
            ClientTicks.onEndTick(GapsBClientRegistrations::tick);
        }
    }

    public static void registerRenderers(HbmBlockEntityRenderers.Sink sink) {
        HbmBlockEntityRenderers.regGem(sink, ModFluidNetBlockEntities.ANCHOR.get(), () -> new RenderPipeAnchor<>("pipe_anchor"));
        HbmBlockEntityRenderers.regGem(sink, ModFluidNetBlockEntities.ANCHOR_EXHAUST.get(), () -> new RenderPipeAnchor<>("pipe_anchor_exhaust"));
    }

    /** 1.7 {@code ItemWrench.onUpdate} (client): every wrench with a stored start shows "name: Nm". */
    private static void tick() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!(stack.getItem() instanceof ItemWrench)) continue;
            BlockPos start = ModEnergyNetContent.WIRE_START.get(stack).orElse(null);
            if (start == null) continue;
            double dx = player.getX() - start.getX(), dy = player.getY() - start.getY(), dz = player.getZ() - start.getZ();
            RenderInfoSystem.displayTooltip(Component.literal(stack.getHoverName().getString() + ": "
                    + ((int) Math.sqrt(dx * dx + dy * dy + dz * dz)) + "m"), 0, ID_WRENCH);
        }
    }
}
