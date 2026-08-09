package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.client.ILookOverlay;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker;
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

/** Catalytic Cracker block — 1:1 port of the 1.12.2 {@code MachineCatalyticCracker}: a large steam
 * tower (dims {@code {0,0,3,3,2,3}}, offset 3; 4 body fills + 8 makeExtra ports). No GUI — fluid-ID
 * right-click retypes the input, empty-hand look-overlay prints the 5 tanks. */
public class MachineCatalyticCracker extends BlockDummyable implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineCatalyticCracker(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCatalyticCracker(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{0, 0, 3, 3, 2, 3}; }
    @Override public int getOffset() { return 3; }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        return super.checkRequirement(world, x, y, z, dir, o) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{4, -1, 3, -1, 1, 1}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{13, 0, 0, 3, 2, 1}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{14, -13, -1, 2, 1, 0}, x, y, z, dir) &&
                MultiblockHandlerXR.checkSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{3, -1, 2, 3, -1, 3}, x, y, z, dir);
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{4, -1, 3, -1, 1, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{13, 0, 0, 3, 2, 1}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{14, -13, -1, 2, 1, 0}, this, dir);
        MultiblockHandlerXR.fillSpace(world, x + dir.offsetX * o, y + dir.offsetY * o, z + dir.offsetZ * o, new int[]{3, -1, 2, 3, -1, 3}, this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int cx = x + dir.offsetX * o, cy = y + dir.offsetY * o, cz = z + dir.offsetZ * o;

        this.makeExtra(world, cx + dir.offsetX * 3 + rot.offsetX, cy, cz + dir.offsetZ * 3 + rot.offsetZ);
        this.makeExtra(world, cx + dir.offsetX * 3 - rot.offsetX * 2, cy, cz + dir.offsetZ * 3 - rot.offsetZ * 2);
        this.makeExtra(world, cx - dir.offsetX * 3 + rot.offsetX, cy, cz - dir.offsetZ * 3 + rot.offsetZ);
        this.makeExtra(world, cx - dir.offsetX * 3 - rot.offsetX * 2, cy, cz - dir.offsetZ * 3 - rot.offsetZ * 2);
        this.makeExtra(world, cx + dir.offsetX * 2 + rot.offsetX * 2, cy, cz + dir.offsetZ * 2 + rot.offsetZ * 2);
        this.makeExtra(world, cx + dir.offsetX * 2 - rot.offsetX * 3, cy, cz + dir.offsetZ * 2 - rot.offsetZ * 3);
        this.makeExtra(world, cx - dir.offsetX * 2 + rot.offsetX * 2, cy, cz - dir.offsetZ * 2 + rot.offsetZ * 2);
        this.makeExtra(world, cx - dir.offsetX * 2 - rot.offsetX * 3, cy, cz - dir.offsetZ * 2 - rot.offsetZ * 3);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier identifier) {
            if (!level.isClientSide()) {
                BlockPos core = findCore(level, pos);
                if (core == null) return InteractionResult.FAIL;
                if (!(level.getBlockEntity(core) instanceof TileEntityMachineCatalyticCracker cracker)) return InteractionResult.FAIL;

                FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
                cracker.tanks[0].setTankType(type);
                cracker.setChanged();
                player.sendSystemMessage(Component.translatable("chat.fractioning.changedto",
                        Component.translatable(type.getTranslationKey())).withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineCatalyticCracker te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_CATALYTIC_CRACKER.get(), TileEntityMachineCatalyticCracker::serverTick);
    }

    @Override
    public void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineCatalyticCracker cracker)) return;

        List<String> text = new ArrayList<>();
        for (FluidTankNTM tank : cracker.tanks) {
            text.add(tank.getTankType().getName() + ": " + String.format("%,d", tank.getFill()) + "/" + String.format("%,d", tank.getMaxFill()) + " mB");
        }
        ILookOverlay.printGeneric(graphics, this.getName().getString(), 0xffff00, 0x404000, text);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
