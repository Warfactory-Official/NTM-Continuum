package com.hbm.foundry;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.block.IToolable;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineStrandCaster;
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

import java.util.ArrayList;
import java.util.List;

/** The six-cell Strand Caster foundry leaf, preserving the old caster footprint and ports. */
public class MachineStrandCaster extends BlockMultiblockMachine
        implements IFluidConnectorBlockMK2, ICrucibleAcceptor, IToolable, WorldlyContainerHolder, ILookOverlay {

    public MachineStrandCaster(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineStrandCaster(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 6, 0, 1, 0}; }
    @Override public int getOffset() { return 0; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineStrandCaster caster ? caster : null;
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double dX, double dY, double dZ,
                                        ForgeDirection side, Mats.MaterialStack stack) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineStrandCaster caster
                && caster.canAcceptPartialPour(level, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level level, BlockPos pos, double dX, double dY, double dZ,
                                   ForgeDirection side, Mats.MaterialStack stack) {
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineStrandCaster caster
                ? caster.pour(level, pos, dX, dY, dZ, side, stack) : stack;
    }

    @Override public boolean canAcceptPartialFlow(Level level, BlockPos pos, ForgeDirection side, Mats.MaterialStack stack) {
        return false;
    }

    @Override public Mats.MaterialStack flow(Level level, BlockPos pos, ForgeDirection side, Mats.MaterialStack stack) {
        return stack;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineStrandCaster caster)) {
            return InteractionResult.SUCCESS;
        }

        if (ItemMold.getMold(stack) != null && caster.inventory.getStackInSlot(0).isEmpty()) {
            if (!level.isClientSide()) {
                caster.inventory.setStackInSlot(0, stack.copyWithCount(1));
                stack.shrink(1);
                caster.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide() && caster.amount > 0 && caster.type != null) {
                ItemStack scraps = ItemScraps.create(new Mats.MaterialStack(caster.type, caster.amount));
                if (!player.getInventory().add(scraps)) {
                    level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1D,
                            pos.getZ() + 0.5D, scraps));
                }
                caster.amount = 0;
                caster.type = null;
                caster.setChanged();
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
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, net.minecraft.world.phys.Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineStrandCaster caster)
                || caster.inventory.getStackInSlot(0).isEmpty()) return false;
        if (!level.isClientSide()) {
            if (!player.getInventory().add(caster.inventory.getStackInSlot(0).copy())) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1D,
                        pos.getZ() + 0.5D, caster.inventory.getStackInSlot(0).copy()));
            }
            caster.inventory.setStackInSlot(0, ItemStack.EMPTY);
            caster.setChanged();
        }
        return true;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineStrandCaster caster)) return null;
        List<String> text = new ArrayList<>();
        ItemMold.Mold mold = caster.getInstalledMold();
        if (mold == null) text.add("§cNo mold installed");
        else {
            text.add("§e" + mold.getTitle());
            if (caster.type != null && caster.amount > 0) {
                text.add("&[" + caster.type.moltenColor + "&]" + caster.type.getDisplayName()
                        + ": " + caster.amount + " / " + caster.getCapacity());
            }
        }
        return new ILookOverlay.Data("Strand Caster", 0xFF4000, 0x401000, text);
    }

    /** 1.7: the two courses of runout table above the strand. */
    private static final int[] TABLE = {2, 0, 1, 0, 1, 0};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        return super.checkRequirement(level, core, facing, clicked)
                && MultiblockHandlerXR.checkSpace(level, core, TABLE, clicked, facing);
    }

    /** A water inlet and a steam outlet at each end of the strand, on opposite flanks. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, TABLE, facing, visitor);

        Direction rot = facing.getClockWise();
        for (int along : new int[]{-1, -5}) {
            visitor.cell(core.relative(facing, along).relative(rot), MASK_WEST);
            visitor.cell(core.relative(facing, along), MASK_EAST);
        }
    }

    /** The four cells the crucible pours into take the mold and the scraps, and carry no fluid face. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        for (int along : new int[]{-1, -5}) {
            visitor.passiveCell(core.relative(facing, along).relative(rot), MASK_ALL, PASSIVE_FLUID_IN);
            visitor.passiveCell(core.relative(facing, along), MASK_ALL, PASSIVE_FLUID_IN);
        }
        visitor.passiveCell(core.relative(facing, -1).relative(rot).above(2), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.relative(facing, -1).above(2), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.relative(rot).above(2), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.above(2), MASK_ALL, PASSIVE_ITEMS);
    }
}
