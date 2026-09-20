package com.hbm.foundry;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;


/** The compound-footprint Large Arc Furnace block and its six power ports. */
public class MachineArcFurnaceLarge extends BlockMultiblockMachine implements IEnergyConnectorBlock,
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
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineArcFurnaceLarge furnace ? furnace : null;
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
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
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

    /** 1.7: the electrode gantry reaching out past getDimensions. */
    private static final int[] GANTRY = {4, 0, 3, -2, 1, 1};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockHandlerXR.checkSpace(level, core, GANTRY, clicked, facing);
    }

    /** Two cable anchors across the nose and two on each flank. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, GANTRY, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing, 2).relative(rot), MASK_SOUTH);
        visitor.cell(core.relative(facing, 2).relative(rot, -1), MASK_SOUTH);
        visitor.cell(core.relative(rot, 2).relative(facing), MASK_WEST);
        visitor.cell(core.relative(rot, 2).relative(facing, -1), MASK_WEST);
        visitor.cell(core.relative(rot, -2).relative(facing), MASK_EAST);
        visitor.cell(core.relative(rot, -2).relative(facing, -1), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_ITEMS;
        visitor.passiveCell(core.relative(facing, 2).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 2).relative(rot, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, 2).relative(facing), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, 2).relative(facing, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -2).relative(facing), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -2).relative(facing, -1), MASK_ALL, domains);
    }
}
