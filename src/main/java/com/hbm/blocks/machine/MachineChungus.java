package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityChungus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import java.util.Locale;

/** The 5x4x14 Leviathan steam turbine multiblock. */
public class MachineChungus extends BlockDummyable
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    /** The housing ridge that sits one layer above the main body. */
    private static final int[] RIDGE = {4, -4, 0, 3, 1, 1};
    /** The shaft running back from the body toward the generator. */
    private static final int[] SHAFT = {3, 0, 6, -1, 1, 1};
    /** The generator at the far end of the shaft. */
    private static final int[] GENERATOR = {2, 0, 10, -7, 1, 1};

    /**
     * The steam intake nozzle, one block past the front of the body and two up. It is the only cell of
     * the machine that no footprint pass covers, so it is placed by hand and then flagged as a port.
     */
    private static final int INTAKE_FORWARD = 4;

    public MachineChungus(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityChungus(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 3, 2, 2};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);

        int x = core.getX();
        int y = core.getY();
        int z = core.getZ();
        MultiblockHandlerXR.fillSpace(level, x, y, z, RIDGE, this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, SHAFT, this, dir);
        MultiblockHandlerXR.fillSpace(level, x, y, z, GENERATOR, this, dir);

        BlockPos intake = new BlockPos(x + dir.offsetX * INTAKE_FORWARD, y + 2, z + dir.offsetZ * INTAKE_FORWARD);
        safeRem = true;
        level.setBlock(intake, defaultBlockState().setValue(META, dir.ordinal()), 3);
        safeRem = false;
        makeExtra(level, intake.getX(), intake.getY(), intake.getZ());

        makeExtra(level, x - dir.offsetX * 10, y, z - dir.offsetZ * 10);
        ForgeDirection side = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, x + side.offsetX * 2, y, z + side.offsetZ * 2);
        makeExtra(level, x - side.offsetX * 2, y, z - side.offsetZ * 2);
    }

    @Override
    public boolean checkRequirement(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        int cx = x + dir.offsetX * o;
        int cy = y + dir.offsetY * o;
        int cz = z + dir.offsetZ * o;
        if (!MultiblockHandlerXR.checkSpace(world, cx, cy, cz, getDimensions(), x, y, z, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, cx, cy, cz, SHAFT, x, y, z, dir)) return false;
        if (!MultiblockHandlerXR.checkSpace(world, cx, cy, cz, GENERATOR, x, y, z, dir)) return false;
        return world.getBlockState(new BlockPos(cx + dir.offsetX * INTAKE_FORWARD, cy + 2, cz + dir.offsetZ * INTAKE_FORWARD)).canBeReplaced();
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityChungus chungus)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        ForgeDirection dir = chungus.getFacing();
        ForgeDirection turn = dir.getRotation(ForgeDirection.DOWN);
        int nearX = core.getX() + dir.offsetX + turn.offsetX * 2;
        int farX = core.getX() + dir.offsetX * 2 + turn.offsetX * 2;
        int nearZ = core.getZ() + dir.offsetZ + turn.offsetZ * 2;
        int farZ = core.getZ() + dir.offsetZ * 2 + turn.offsetZ * 2;

        if ((pos.getX() == nearX || pos.getX() == farX) && (pos.getZ() == nearZ || pos.getZ() == farZ)
                && pos.getY() < core.getY() + 2) {
            if (!level.isClientSide()) {
                if (!chungus.operational) {
                    level.playSound(null, pos, ModSounds.CHUNGUS_LEVER.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    chungus.onLeverPull();
                } else {
                    player.sendSystemMessage(
                            Component.literal("Cannot change compressor setting while operational!")
                                    .withStyle(ChatFormatting.RED));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityChungus chungus
                && IEnergyConnectorBlock.isPort(chungus.getPowerPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return (CoolingRecipes.get(type) != null || type == Fluids.SPENTSTEAM)
                && findCoreTE(world, pos) instanceof TileEntityChungus chungus
                && IEnergyConnectorBlock.isPort(chungus.getConPos(), pos, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityChungus chungus)) return null;

        List<String> text = new ArrayList<>();
        FluidType inputType = chungus.tanks[0].getTankType();
        if (inputType != Fluids.NONE) {
            text.add("§a-> §r" + inputType.getDisplayNameString() + ": "
                    + String.format(Locale.US, "%,d", chungus.tanks[0].getFill()) + "/"
                    + String.format(Locale.US, "%,d", chungus.tanks[0].getMaxFill()) + "mB");
        }
        FluidType outputType = chungus.tanks[1].getTankType();
        if (outputType != Fluids.NONE) {
            text.add("§c<- §r" + outputType.getDisplayNameString() + ": "
                    + String.format(Locale.US, "%,d", chungus.tanks[1].getFill()) + "/"
                    + String.format(Locale.US, "%,d", chungus.tanks[1].getMaxFill()) + "mB");
        }
        text.add("§c<- §r" + Library.getShortNumber(chungus.powerBuffer) + "HE");

        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_CHUNGUS.get(), TileEntityChungus::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_CHUNGUS.get(), TileEntityChungus::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
