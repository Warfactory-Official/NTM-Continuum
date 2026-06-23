package com.hbm;

import java.util.function.Supplier;
import com.hbm.creativetabs.ModTabs;
import com.hbm.packet.BlockEntitySyncPayload;
import com.hbm.packet.ControlEventPayload;
import com.hbm.packet.HbmPackets;
import com.hbm.registry.DeferredRegister;
import com.hbm.uninos.UniNodespace;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

public class hbm implements ModInitializer {

    @Override
    public void onInitialize() {

        CommonClass.init();

        DeferredRegister.flushAll(this::register);

        PayloadTypeRegistry.clientboundPlay().register(BlockEntitySyncPayload.TYPE, BlockEntitySyncPayload.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(ControlEventPayload.TYPE, ControlEventPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ControlEventPayload.TYPE,
                (payload, context) -> HbmPackets.handleControl(context.player(), payload));

        ModTabs.CONTENTS.forEach((tabKey, contents) ->
                CreativeModeTabEvents.modifyOutputEvent(tabKey).register(output -> {
                    contents.forEach(item -> output.accept(item.get()));
                    var stacks = ModTabs.STACK_CONTENTS.get(tabKey);
                    if (stacks != null) stacks.forEach(supplier -> supplier.get().forEach(output::accept));
                }));

        ServerTickEvents.END_SERVER_TICK.register(UniNodespace::updateNodespace);
        ServerLevelEvents.UNLOAD.register((server, level) -> UniNodespace.clearLevel(level));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void register(ResourceKey<? extends Registry<?>> registryKey, ResourceKey<?> entryKey, Supplier<?> value) {
        Registry<Object> registry = (Registry<Object>) (Registry<?>) BuiltInRegistries.REGISTRY.getValueOrThrow((ResourceKey) registryKey);
        Registry.register(registry, (ResourceKey<Object>) (ResourceKey<?>) entryKey, value.get());
    }
}
