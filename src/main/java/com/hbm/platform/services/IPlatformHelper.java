package com.hbm.platform.services;

import net.minecraft.core.BlockPos;
//? if >=1.20.5 {
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//?}
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName() {

        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /**
     * Creates a {@link MenuType} whose menu is opened with a {@link BlockPos} as extra data. NeoForge
     * uses {@code IMenuTypeExtension}; Fabric uses {@code ExtendedScreenHandlerType}.
     */
    <T extends AbstractContainerMenu> MenuType<T> menuTypeWithPos(MenuPosFactory<T> factory);

    /** Opens a {@link MenuProvider} for the player, sending the given {@link BlockPos} to the client. */
    void openMenu(ServerPlayer player, MenuProvider provider, BlockPos pos);

    /**
     * Builds a {@link BlockEntityType}. Vanilla's type constructor is private, so each loader builds it
     * via its own API (NeoForge constructor / Fabric builder).
     */
    <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityFactory<T> factory, Block... blocks);

    //? if >=1.20.5 {
    /**
     * Sends a clientbound {@link CustomPacketPayload} to every player tracking the chunk that contains
     * {@code pos} (i.e. has the tile's chunk loaded). NeoForge uses {@code PacketDistributor}; Fabric
     * uses {@code PlayerLookup.tracking} + {@code ServerPlayNetworking.send}.
     */
    void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload);

    /**
     * Sends a clientbound {@link CustomPacketPayload} to a single player. NeoForge uses
     * {@code PacketDistributor.sendToPlayer}; Fabric uses {@code ServerPlayNetworking.send}.
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
    //?}
}
