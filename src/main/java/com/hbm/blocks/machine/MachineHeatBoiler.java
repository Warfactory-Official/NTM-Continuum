package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import net.minecraft.ChatFormatting;
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
 * Heat Boiler block (old id {@code heat_boiler}) — 1:1 port of the 1.12.2 {@code MachineHeatBoiler}:
 * a 4-tall 3×3 {@link BlockDummyable} (dims {@code {3,0,1,1,1,1}}, offset 1) with two side ports and
 * the top port marked as extras. Right-click with a fluid identifier types the input tank; the look
 * overlay prints heat + both tanks (the boiler has no GUI).
 */
public class MachineHeatBoiler extends BlockDummyable implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineHeatBoiler(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeatBoiler(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 1, 1, 1, 1};
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

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        this.makeExtra(world, x + rot.offsetX, y, z + rot.offsetZ); // the side ports
        this.makeExtra(world, x - rot.offsetX, y, z - rot.offsetZ);
        this.makeExtra(world, x, y + 3, z);                          // the top (steam) port
    }

    /** Fluid identifier right-click types the input tank (old onBlockActivated). */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidId identifier) {
            if (!level.isClientSide()) {
                BlockPos core = findCore(level, pos);
                if (core == null) return InteractionResult.FAIL;
                if (!(level.getBlockEntity(core) instanceof TileEntityHeatBoiler boiler)) return InteractionResult.FAIL;

                FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
                boiler.tanks[0].setTankType(type);
                boiler.setChanged();
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(type.getDisplayName())
                        .append(Component.literal("!")));
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityHeatBoiler te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityHeatBoiler boiler)) return null;

        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", boiler.heat) + " TU");
        for (FluidTankNTM tank : boiler.tanks) {
            text.add(tank.getTankType().getDisplayNameString() + ": " + String.format("%,d", tank.getFill()) + "/" + String.format("%,d", tank.getMaxFill()) + " mB");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
