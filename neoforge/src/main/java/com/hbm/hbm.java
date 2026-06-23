package com.hbm;

import java.util.List;
import java.util.function.Supplier;
import com.hbm.creativetabs.ModTabs;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import com.hbm.client.HbmClientNeoForge;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.packet.HbmPackets;
import net.minecraft.server.level.ServerPlayer;
import com.hbm.uninos.UniNodespace;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Constants.MOD_ID)
public class hbm {

    public hbm(IEventBus eventBus) {

        CommonClass.init();

        eventBus.addListener(this::onRegister);

        eventBus.addListener(this::onBuildTabContents);

        eventBus.addListener(this::onRegisterPayloads);

        if (FMLLoader.getCurrent().getDist() == Dist.CLIENT) {
            HbmClientNeoForge.init(eventBus);
        }

        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> UniNodespace.updateNodespace(event.getServer()));
        NeoForge.EVENT_BUS.addListener((LevelEvent.Unload event) -> {
            if (event.getLevel() instanceof ServerLevel level) UniNodespace.clearLevel(level);
        });
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(Constants.MOD_ID).playToClient(
                BlockEntitySyncPayload.TYPE,
                BlockEntitySyncPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> HbmPackets.handleSync(context.player().level(), payload)));
        event.registrar(Constants.MOD_ID).playToServer(
                ControlEventPayload.TYPE,
                ControlEventPayload.CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> HbmPackets.handleControl((ServerPlayer) context.player(), payload)));
    }

    private void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        List<RegistryObject<? extends Item>> contents = ModTabs.CONTENTS.get(event.getTabKey());
        if (contents != null) {
            contents.forEach(item -> event.accept(item.get()));
        }
        var stacks = ModTabs.STACK_CONTENTS.get(event.getTabKey());
        if (stacks != null) {
            stacks.forEach(supplier -> supplier.get().forEach(event::accept));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void onRegister(RegisterEvent event) {
        for (DeferredRegister<?> register : DeferredRegister.all()) {
            if (event.getRegistryKey().equals(register.getRegistryKey())) {
                register.flushTo((registryKey, entryKey, value) ->
                        event.register((ResourceKey) registryKey, entryKey.identifier(), (Supplier) value));
            }
        }
    }
}
