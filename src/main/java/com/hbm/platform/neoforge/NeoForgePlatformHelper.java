package com.hbm.platform.neoforge;

//? if neoforge {

import java.util.Set;
import com.hbm.platform.services.BlockEntityFactory;
import com.hbm.platform.services.IPlatformHelper;
import com.hbm.platform.services.MenuPosFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {

        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {

        //? if >=26.1 {
        return !FMLLoader.getCurrent().isProduction();
        //?} else {
        /*return !FMLLoader.isProduction();
        *///?}
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> menuTypeWithPos(MenuPosFactory<T> factory) {
        return IMenuTypeExtension.create((containerId, inventory, buf) -> factory.create(containerId, inventory, buf.readBlockPos()));
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        player.openMenu(provider, pos);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityFactory<T> factory, Block... blocks) {
        //? if >=26.1 {
        return new BlockEntityType<>(factory::create, Set.of(blocks));
        //?} else {
        /*return new BlockEntityType<T>(factory::create, Set.of(blocks), null);
        *///?}
    }

    @Override
    public void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
//?}
