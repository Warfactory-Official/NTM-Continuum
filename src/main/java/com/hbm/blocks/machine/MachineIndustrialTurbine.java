package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
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

/** The 7x4x4 industrial steam turbine multiblock. */
public class MachineIndustrialTurbine extends BlockDummyable
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
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);

        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        addExtraPorts(level, x, y, z, dir);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addExtraPorts(level, core.getX(), core.getY(), core.getZ(), dir);
    }

    private void addExtraPorts(Level level, int x, int y, int z, ForgeDirection dir) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

        makeExtra(level, x + dir.offsetX * 3 + rot.offsetX, y, z + dir.offsetZ * 3 + rot.offsetZ);
        makeExtra(level, x + dir.offsetX * 3 - rot.offsetX, y, z + dir.offsetZ * 3 - rot.offsetZ);
        makeExtra(level, x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ);
        makeExtra(level, x - dir.offsetX - rot.offsetX, y, z - dir.offsetZ - rot.offsetZ);
        makeExtra(level, x + dir.offsetX * 3, y + 2, z + dir.offsetZ * 3);
        makeExtra(level, x - dir.offsetX, y + 2, z - dir.offsetZ);
        makeExtra(level, x - dir.offsetX * 3, y + 1, z - dir.offsetZ * 3);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos core = findCore(level, pos);
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
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineIndustrialTurbine turbine
                && IEnergyConnectorBlock.isPort(turbine.getPowerPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return (com.hbm.inventory.recipes.CoolingRecipes.get(type) != null || type == com.hbm.inventory.fluid.Fluids.SPENTSTEAM)
                && findCoreTE(world, pos) instanceof TileEntityMachineIndustrialTurbine turbine
                && IEnergyConnectorBlock.isPort(turbine.getConPos(), pos, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityMachineIndustrialTurbine turbine)) return null;

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

}
