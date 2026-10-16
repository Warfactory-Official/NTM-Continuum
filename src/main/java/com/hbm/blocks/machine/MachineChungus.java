package com.hbm.blocks.machine;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.hbm.tileentity.machine.TileEntityChungus;

/**
 * The Leviathan steam turbine. 1.7 {@code MachineChungus}: a 5x4x4 body (dims {@code {3,0,0,3,2,2}}, offset 3)
 * with a 3-wide crown at y+4 over the body, a 3x4 duct run 1..6 behind the core, a 3x3 tail 7..10 behind it,
 * one extra block at (core + 4·dir, y+2) that is the front steam connector, a power connector at
 * core − 10·dir, and two side connectors at core ± side·2.
 */
public class MachineChungus extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay, ITooltipProvider {

    /** 1.7 {@code getAllDimensions()[1..3]}. */
    private static final int[] CROWN = {4, -4, 0, 3, 1, 1};
    private static final int[] DUCT = {3, 0, 6, -1, 1, 1};
    private static final int[] TAIL = {2, 0, 10, -7, 1, 1};

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

    /**
     * 1.7 checks the body, the duct run, the tail and the front connector block, and not the crown. That
     * omission is kept: the crown is stamped over whatever is there, as it always was.
     */
    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core, DUCT, clicked, facing)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core, TAIL, clicked, facing)) return false;
        BlockPos front = core.relative(facing, 4).above(2);
        return level.getBlockState(front).canBeReplaced();
    }

    /**
     * Masks are stated for the south-facing bake: the front connector faces south, the power connector north,
     * and {@code dir.getRotation(UP)} of SOUTH is WEST (the clockwise side), so the two side connectors open
     * west and east.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core, CROWN, facing, visitor);
        MultiblockHandlerXR.visitBox(core, DUCT, facing, visitor);
        MultiblockHandlerXR.visitBox(core, TAIL, facing, visitor);

        Direction rot = facing.getClockWise();
        visitor.cell(core.relative(facing, 4).above(2), MASK_SOUTH, ROLE_FLUID);
        visitor.cell(core.relative(facing, -10), MASK_NORTH, ROLE_POWER);
        visitor.cell(core.relative(rot, 2), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.relative(rot, -2), MASK_EAST, ROLE_FLUID);
    }

    /** 1.7 stamps {@code TileEntityProxyCombo(false, true, true)}, power and fluid, on all four extras. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(facing, 4).above(2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -10), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, 2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot, -2), MASK_ALL, domains);
    }

    /**
     * 1.7 {@code onBlockActivated}: a click on either of the two lever columns (core + dir + turn·2 and
     * core + 2·dir + turn·2, below y+2) pulls the compressor lever; anything else on the machine is swallowed
     * (1.7 returns false and the click falls through). {@code turn} is {@code dir.getRotation(DOWN)}, the counter-clockwise side.
     */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityChungus turbine)) {
            return InteractionResult.SUCCESS; // 1.7: "if(pos == null) return true;"
        }

        ForgeDirection dir = turbine.getFacing();
        ForgeDirection turn = dir.getRotation(ForgeDirection.DOWN);
        int iX = core.getX() + dir.offsetX + turn.offsetX * 2;
        int iX2 = core.getX() + dir.offsetX * 2 + turn.offsetX * 2;
        int iZ = core.getZ() + dir.offsetZ + turn.offsetZ * 2;
        int iZ2 = core.getZ() + dir.offsetZ * 2 + turn.offsetZ * 2;

        if ((pos.getX() == iX || pos.getX() == iX2) && (pos.getZ() == iZ || pos.getZ() == iZ2)
                && pos.getY() < core.getY() + 2) {
            if (!level.isClientSide()) {
                if (!turbine.operational) {
                    level.playSound(null, pos, ModSounds.CHUNGUS_LEVER.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    turbine.onLeverPull();
                } else {
                    player.sendSystemMessage(Component.literal("Cannot change compressor setting while operational!")
                            .withStyle(ChatFormatting.RED));
                }
            }
            return InteractionResult.SUCCESS;
        }
        // 1.7 returns false for any other click on the machine: no menu, the click falls through.
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityChungus chungus)) return null;

        List<String> text = new ArrayList<>();
        FluidType inputType = chungus.tanks[0].getTankType();
        FluidType outputType = Fluids.NONE;
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(inputType);
        if (recipe != null) outputType = recipe.output();

        text.add("§a-> §r" + inputType.getDisplayNameString() + ": "
                + String.format(Locale.US, "%,d", chungus.tanks[0].getFill()) + "/"
                + String.format(Locale.US, "%,d", chungus.tanks[0].getMaxFill()) + "mB");
        text.add("§c<- §r" + outputType.getDisplayNameString() + ": "
                + String.format(Locale.US, "%,d", chungus.tanks[1].getFill()) + "/"
                + String.format(Locale.US, "%,d", chungus.tanks[1].getMaxFill()) + "mB");
        text.add("§c<- §r" + Library.getShortNumber(chungus.powerBuffer) + "HE");

        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return MultiblockSurface.coreEntity(world, pos) instanceof TileEntityChungus turbine
                && IEnergyConnectorBlock.isPort(turbine.getPowerPos(), pos, dir);
    }

    /** A coolant with a cooling recipe, or the spent steam it returns. */
    @Override
    public boolean acceptsFluid(FluidType type) {
        return CoolingRecipes.get(type) != null || type == Fluids.SPENTSTEAM;
    }

    /** 1.7 {@code MachineChungus.getAllDimensions}, read only by the placement highlight. */
    @Override
    public int[][] getAllDimensions() {
        return new int[][] {
            new int[] { 3, 0, 0, 3, 2, 2 },
            new int[] { 4, -4, 0, 3, 1, 1 },
            new int[] { 3, 0, 6, -1, 1, 1 },
            new int[] { 2, 0, 10, -7, 1, 1 },
        };
    }
}
