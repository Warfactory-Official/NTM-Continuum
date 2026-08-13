package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineFractionTower;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fractioning Tower block — 1:1 port of the 1.12.2 {@code MachineFractionTower}: a 3-tall 3×3
 * {@link BlockDummyable} (dims {@code {2,0,1,1,1,1}}, offset 1) with the 4 side cells marked as extras
 * (fluid ports). Right-click with a fluid identifier on the <b>bottom</b> segment types the input tank
 * (towers stacked 3 apart share the type); empty-hand right-click prints the tank contents.
 */
public class MachineFractionTower extends BlockDummyable implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineFractionTower(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineFractionTower(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        x = x + dir.offsetX * o;
        z = z + dir.offsetZ * o;

        this.makeExtra(world, x + 1, y, z);
        this.makeExtra(world, x - 1, y, z);
        this.makeExtra(world, x, y, z + 1);
        this.makeExtra(world, x, y, z - 1);
    }

    //? if >=26.1 {
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hbm$useItemOn(stack, state, level, pos, player, hand, hit);
    }
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult hbm$r = hbm$useItemOn(stack, state, level, pos, player, hand, hit);
        return hbm$r == InteractionResult.SUCCESS ? net.minecraft.world.ItemInteractionResult.SUCCESS
             : hbm$r == InteractionResult.CONSUME ? net.minecraft.world.ItemInteractionResult.CONSUME
             : net.minecraft.world.ItemInteractionResult.TRY_WITH_EMPTY_HAND_TO_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}

    private InteractionResult hbm$useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidId identifier) {
            if (!level.isClientSide()) {
                BlockPos core = findCore(level, pos);
                if (core == null) return InteractionResult.FAIL;
                if (!(level.getBlockEntity(core) instanceof TileEntityMachineFractionTower frac)) return InteractionResult.FAIL;

                if (level.getBlockEntity(core.offset(0, -3, 0)) instanceof TileEntityMachineFractionTower) {
                    player.sendSystemMessage(Component.translatable("chat.fractioning.onlybottom"));
                } else {
                    FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
                    frac.tanks[0].setTankType(type);
                    frac.setChanged();
                    player.sendSystemMessage(Component.translatable("chat.fractioning.changedto",
                            Component.translatable(type.getTranslationKey())).withStyle(ChatFormatting.YELLOW));
                }
            }
            return InteractionResult.SUCCESS;
        }
        //? if >=26.1 {
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
        //?} else {
        /*return super.useItemOn(stack, state, level, pos, player, hand, hit) == net.minecraft.world.ItemInteractionResult.SUCCESS
                ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        *///?}
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineFractionTower te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_FRACTION_TOWER.get(), TileEntityMachineFractionTower::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineFractionTower frac)) return;

        List<String> text = new ArrayList<>();
        for (FluidTankNTM tank : frac.tanks) {
            text.add(tank.getTankType().getName() + ": " + String.format("%,d", tank.getFill()) + "/" + String.format("%,d", tank.getMaxFill()) + " mB");
        }
        ILookOverlay.printGeneric(graphics, this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
