package com.hbm.foundry;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;


/** The compound-footprint Large Arc Furnace block and its six power ports. */
public class MachineArcFurnaceLarge extends BlockDummyable implements IEnergyConnectorBlock,
        WorldlyContainerHolder {

    public MachineArcFurnaceLarge(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineArcFurnaceLarge(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{4, 0, 2, 2, 2, 2}; }
    @Override public int getOffset() { return 2; }

    @Override
    public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        if (!super.checkRequirement(level, x, y, z, dir, offset)) return false;
        BlockPos core = new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset);
        return MultiblockHandlerXR.checkSpace(level, core.getX(), core.getY(), core.getZ(),
                new int[]{4, 0, 3, -2, 1, 1}, x, y, z, dir);
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        BlockPos core = new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset);
        formStructure(level, core, dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(),
                getDimensions(), this, dir);
        MultiblockHandlerXR.fillSpace(level, core.getX(), core.getY(), core.getZ(),
                new int[]{4, 0, 3, -2, 1, 1}, this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = core.getX(), y = core.getY(), z = core.getZ();
        makeExtra(level, x + dir.offsetX * 2 + rot.offsetX, y, z + dir.offsetZ * 2 + rot.offsetZ);
        makeExtra(level, x + dir.offsetX * 2 - rot.offsetX, y, z + dir.offsetZ * 2 - rot.offsetZ);
        makeExtra(level, x + rot.offsetX * 2 + dir.offsetX, y, z + rot.offsetZ * 2 + dir.offsetZ);
        makeExtra(level, x + rot.offsetX * 2 - dir.offsetX, y, z + rot.offsetZ * 2 - dir.offsetZ);
        makeExtra(level, x - rot.offsetX * 2 + dir.offsetX, y, z - rot.offsetZ * 2 + dir.offsetZ);
        makeExtra(level, x - rot.offsetX * 2 - dir.offsetX, y, z - rot.offsetZ * 2 - dir.offsetZ);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityMachineArcFurnaceLarge furnace ? furnace : null;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineArcFurnaceLarge furnace
                && IEnergyConnectorBlock.isPort(furnace.getConPos(), pos, dir);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineArcFurnaceLarge furnace)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;

        if (stack.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide()) {
                for (com.hbm.inventory.material.Mats.MaterialStack liquid : furnace.liquids) {
                    ItemStack scraps = ItemScraps.create(liquid.copy());
                    if (!player.getInventory().add(scraps)) {
                        level.addFreshEntity(new ItemEntity(level, hit.getLocation().x, hit.getLocation().y,
                                hit.getLocation().z, scraps));
                    }
                }
                furnace.liquids.clear();
                furnace.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && level.getBlockEntity(core) instanceof net.minecraft.world.MenuProvider provider) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, core);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!level.isClientSide() && state.getValue(META) >= offset + 2
                && level.getBlockEntity(pos) instanceof TileEntityMachineArcFurnaceLarge furnace) {
            for (com.hbm.inventory.material.Mats.MaterialStack liquid : furnace.liquids) {
                ItemScraps.spawnMats(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        liquid.copy());
            }
            furnace.liquids.clear();
            Containers.dropContents(level, pos, furnace);
        }
        super.onBlockRemoved(state, level, pos, movedByPiston);
    }

}
