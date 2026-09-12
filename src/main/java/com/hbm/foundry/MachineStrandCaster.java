package com.hbm.foundry;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.block.IToolable;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
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
import net.minecraft.world.level.BlockGetter;
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
public class MachineStrandCaster extends BlockDummyable
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
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        formStructure(level, new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset), dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        MultiblockHandlerXR.fillSpace(level, x, y, z, getDimensions(), this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, new int[]{2, 0, 1, 0, 1, 0}, this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, x + rot.offsetX - dir.offsetX, y, z + rot.offsetZ - dir.offsetZ);
        makeExtra(level, x - dir.offsetX, y, z - dir.offsetZ);
        makeExtra(level, x - dir.offsetX * 5, y, z - dir.offsetZ * 5);
        makeExtra(level, x + rot.offsetX - dir.offsetX * 5, y, z + rot.offsetZ - dir.offsetZ * 5);
        makeExtra(level, x + rot.offsetX - dir.offsetX, y + 2, z + rot.offsetZ - dir.offsetZ);
        makeExtra(level, x - dir.offsetX, y + 2, z - dir.offsetZ);
        makeExtra(level, x + rot.offsetX, y + 2, z + rot.offsetZ);
        makeExtra(level, x, y + 2, z);
    }

    @Override
    public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        if (!super.checkRequirement(level, x, y, z, dir, offset)) return false;
        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        return MultiblockHandlerXR.checkSpace(level, x, y, z,
                new int[]{2, 0, 1, 0, 1, 0}, x, y, z, dir);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityMachineStrandCaster caster ? caster : null;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        if (!(findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineStrandCaster caster)) return false;
        for (com.hbm.lib.DirPos port : caster.getFluidConPos()) {
            if (port.getPos().getX() == x && port.getPos().getY() == y && port.getPos().getZ() == z
                    && port.getDir() == dir) return true;
        }
        return false;
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, double dX, double dY, double dZ,
                                        ForgeDirection side, Mats.MaterialStack stack) {
        return findCoreTE(level, pos) instanceof TileEntityMachineStrandCaster caster
                && caster.canAcceptPartialPour(level, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level level, BlockPos pos, double dX, double dY, double dZ,
                                   ForgeDirection side, Mats.MaterialStack stack) {
        return findCoreTE(level, pos) instanceof TileEntityMachineStrandCaster caster
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
        BlockPos core = findCore(level, pos);
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
        BlockPos core = findCore(level, pos);
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
        if (!(findCoreTE(level, pos) instanceof TileEntityMachineStrandCaster caster)) return null;
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

}
