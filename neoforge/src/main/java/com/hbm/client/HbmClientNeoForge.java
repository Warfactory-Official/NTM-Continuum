package com.hbm.client;

import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
import com.hbm.client.render.MachineItemModelRenderer;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.gui.GUIMachineAssemblyMachine;
import com.hbm.inventory.gui.GUIBatterySocket;
import com.hbm.inventory.gui.GUICrate;
import com.hbm.inventory.gui.GUIMachineBattery;
import com.hbm.inventory.gui.GUIMachineChemicalPlant;
import com.hbm.inventory.gui.GUIMachineElectricFurnace;
import com.hbm.inventory.gui.GUIMachineEPress;
import com.hbm.inventory.gui.GUIMachineFluidTank;
import com.hbm.inventory.gui.GUIMachinePress;
import com.hbm.inventory.gui.GUINukeMan;
import com.hbm.packet.HbmClientPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import net.minecraft.resources.Identifier;

public final class HbmClientNeoForge {

    private HbmClientNeoForge() {}

    public static void init(IEventBus modBus) {

        HbmClientPackets.setSender(payload -> {
            ClientPacketListener conn = Minecraft.getInstance().getConnection();
            if (conn != null) conn.send(new ServerboundCustomPayloadPacket(payload));
        });

        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAboveAll(
                Identifier.fromNamespaceAndPath("hbm", "look_overlay"),
                (graphics, deltaTracker) -> HbmLookOverlay.render(graphics)));

        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(ModMenus.MACHINE_BATTERY.get(), GUIMachineBattery::new);
            event.register(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), GUIMachineAssemblyMachine::new);
            event.register(ModMenus.MACHINE_CHEMICAL_PLANT.get(), GUIMachineChemicalPlant::new);
            event.register(ModMenus.NUKE_MAN.get(), GUINukeMan::new);
            event.register(ModMenus.FLUID_TANK.get(), GUIMachineFluidTank::new);
            event.register(ModMenus.BATTERY_SOCKET.get(), GUIBatterySocket::new);
            event.register(ModMenus.CRATE.get(), GUICrate::new);
            event.register(ModMenus.MACHINE_PRESS.get(), GUIMachinePress::new);
            event.register(ModMenus.MACHINE_EPRESS.get(), GUIMachineEPress::new);
            event.register(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), GUIMachineElectricFurnace::new);
        });

        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            HbmBlockEntityRenderers.registerAll(new HbmBlockEntityRenderers.Sink() {
                @Override
                public <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, ?> provider) {
                    event.registerBlockEntityRenderer(type, provider);
                }
            });
            HbmEntityRenderers.registerAll(new HbmEntityRenderers.Sink() {
                @Override
                public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                    event.registerEntityRenderer(type, provider);
                }
            });
        });

        modBus.addListener((RegisterSpecialModelRendererEvent event) ->
                event.register(MachineItemModelRenderer.ID, MachineItemModelRenderer.Unbaked.MAP_CODEC));
    }
}
