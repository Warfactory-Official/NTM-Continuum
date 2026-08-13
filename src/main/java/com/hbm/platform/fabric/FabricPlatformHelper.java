package com.hbm.platform.fabric;

//? if fabric {

/*import com.hbm.platform.services.BlockEntityFactory;
import com.hbm.platform.services.IPlatformHelper;
import com.hbm.platform.services.MenuPosFactory;
// Fabric API's extended-menu types, renamed with the rest of the screen-handler package on the 26.1
// line. Same shape either way: a menu type carrying a stream codec for extra open-time data, and a
// provider that supplies that data per player.
//? if >=26.1 {
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
//?} else {
/^import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
^///?}
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {

        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> menuTypeWithPos(MenuPosFactory<T> factory) {
        //? if >=26.1 {
        return new ExtendedMenuType<>(factory::create, BlockPos.STREAM_CODEC);
        //?} else {
        /^return new ExtendedScreenHandlerType<>(factory::create, BlockPos.STREAM_CODEC);
        ^///?}
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        //? if >=26.1 {
        player.openMenu(new ExtendedMenuProvider<BlockPos>() {
        //?} else {
        /^player.openMenu(new ExtendedScreenHandlerFactory<BlockPos>() {
        ^///?}
            @Override
            public BlockPos getScreenOpeningData(ServerPlayer p) {
                return pos;
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                return provider.createMenu(containerId, inventory, p);
            }
        });
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityFactory<T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.<T>create(factory::create, blocks).build();
    }

    @Override
    public void sendToTracking(ServerLevel level, BlockPos pos, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.tracking(level, pos)) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }
}
*///?}
