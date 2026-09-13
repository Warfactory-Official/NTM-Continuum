package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.inventory.recipes.CoolingRecipes;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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

/** The 7x4x4 industrial steam turbine multiblock. */
public class MachineIndustrialTurbine extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineIndustrialTurbine(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineIndustrialTurbine(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 3, 3, 1, 1};
    }

    @Override
    public int getOffset() {
        return 3;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineIndustrialTurbine turbine)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        ForgeDirection facing = turbine.getFacing();
        if (pos.getX() == core.getX() + facing.offsetX * 3
                && pos.getZ() == core.getZ() + facing.offsetZ * 3
                && pos.getY() == core.getY() + 1) {
            if (!level.isClientSide()) {
                if (!turbine.operational) {
                    level.playSound(null, pos, ModSounds.CHUNGUS_LEVER.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    turbine.onLeverPull();
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
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityMachineIndustrialTurbine turbine)) return null;

        List<String> text = new ArrayList<>();
        FluidType inputType = turbine.tanks[0].getTankType();
        FluidType outputType = Fluids.NONE;
        CoolingRecipes.CoolingRecipe recipe = CoolingRecipes.get(inputType);
        if (recipe != null) outputType = recipe.output();

        int color = ((int) (0xFF - 0xFF * turbine.spin)) << 16
                | ((int) (0xFF * turbine.spin) << 8);
        int time = (int) ((level.getGameTime() / 4) % 4);
        String[] animation = {"\u2596 ", "\u2598 ", " \u2598", " \u2596"};

        text.add("§a-> §r" + inputType.getDisplayNameString() + ": "
                + String.format(Locale.US, "%,d", turbine.tanks[0].getFill()) + "/"
                + String.format(Locale.US, "%,d", turbine.tanks[0].getMaxFill()) + "mB");
        text.add("§c<- §r" + outputType.getDisplayNameString() + ": "
                + String.format(Locale.US, "%,d", turbine.tanks[1].getFill()) + "/"
                + String.format(Locale.US, "%,d", turbine.tanks[1].getMaxFill()) + "mB");
        text.add("&[" + color + "&]§c<- §r" + Library.getShortNumber(turbine.powerBuffer)
                + "HE (" + animation[turbine.powerBuffer <= 0 ? 0 : time]
                + Math.round(turbine.spin * 100) + "%§r)");

        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /**
     * Six coolant ports and one power port, and 1.7 states them at separate call sites -- the cable
     * answer runs over getPowerPos alone -- so the roles keep a duct off the generator's own outlet and
     * a cable off the coolant intakes.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();

        visitor.cell(core.relative(facing, 3).relative(rot), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.relative(facing, 3).relative(rot, -1), MASK_EAST, ROLE_FLUID);
        visitor.cell(core.relative(facing, -1).relative(rot), MASK_WEST, ROLE_FLUID);
        visitor.cell(core.relative(facing, -1).relative(rot, -1), MASK_EAST, ROLE_FLUID);
        visitor.cell(core.relative(facing, 3).above(2), MASK_UP, ROLE_FLUID);
        visitor.cell(core.relative(facing, -1).above(2), MASK_UP, ROLE_FLUID);
        visitor.cell(core.relative(facing, -3).above(1), MASK_NORTH, ROLE_POWER);
    }

    /** 1.7 stamps {@code fluid().power()} on all seven. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(facing, 3).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 3).relative(rot, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -1).relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -1).relative(rot, -1), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, 3).above(2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -1).above(2), MASK_ALL, domains);
        visitor.passiveCell(core.relative(facing, -3).above(1), MASK_ALL, domains);
    }

    /** A coolant with a cooling recipe, or the spent steam it returns. */
    @Override
    public boolean acceptsFluid(FluidType type) {
        return com.hbm.inventory.recipes.CoolingRecipes.get(type) != null
                || type == com.hbm.inventory.fluid.Fluids.SPENTSTEAM;
    }
}
