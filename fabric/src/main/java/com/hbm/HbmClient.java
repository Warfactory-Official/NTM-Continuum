package com.hbm;

import com.hbm.client.HbmLookOverlay;
import com.hbm.client.render.HbmBlockEntityRenderers;
import com.hbm.client.render.HbmEntityRenderers;
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
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.HbmClientPackets;
import com.hbm.packet.HbmPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class HbmClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        HbmClientPackets.setSender(ClientPlayNetworking::send);

        MenuScreens.register(ModMenus.MACHINE_BATTERY.get(), GUIMachineBattery::new);
        MenuScreens.register(ModMenus.MACHINE_ASSEMBLY_MACHINE.get(), GUIMachineAssemblyMachine::new);
        MenuScreens.register(ModMenus.MACHINE_CHEMICAL_PLANT.get(), GUIMachineChemicalPlant::new);
        MenuScreens.register(ModMenus.NUKE_MAN.get(), GUINukeMan::new);
        MenuScreens.register(ModMenus.FLUID_TANK.get(), GUIMachineFluidTank::new);
        MenuScreens.register(ModMenus.BATTERY_SOCKET.get(), GUIBatterySocket::new);
        MenuScreens.register(ModMenus.CRATE.get(), GUICrate::new);
        MenuScreens.register(ModMenus.MACHINE_PRESS.get(), GUIMachinePress::new);
        MenuScreens.register(ModMenus.MACHINE_EPRESS.get(), GUIMachineEPress::new);
        MenuScreens.register(ModMenus.MACHINE_ELECTRIC_FURNACE.get(), GUIMachineElectricFurnace::new);

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("hbm", "look_overlay"),
                (graphics, deltaTracker) -> HbmLookOverlay.render(graphics));

        HbmBlockEntityRenderers.registerAll(new HbmBlockEntityRenderers.Sink() {
            @Override
            public <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, ?> provider) {
                BlockEntityRendererRegistry.register(type, provider);
            }
        });

        HbmEntityRenderers.registerAll(new HbmEntityRenderers.Sink() {
            @Override
            public <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                EntityRendererRegistry.register(type, provider);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(BlockEntitySyncPayload.TYPE,
                (payload, context) -> HbmPackets.handleSync(context.player().level(), payload));
    }
}
