package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemGear;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityStirling;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The legacy one-core Stirling engine family. */
public class MachineStirling extends BlockMultiblockMachine implements IEnergyConnectorBlock, ILookOverlay, com.hbm.items.ModelState {

    public MachineStirling(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityStirling(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityStirling stirling
                && !stirling.hasCog
                && stack.getItem() instanceof ItemGear gear
                && gear.getVariant(stack) == stirling.getGearVariant()) {
            if (!level.isClientSide()) {
                stack.shrink(1);
                stirling.hasCog = true;
                stirling.setChanged();
                level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.5F, 0.75F);
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || hasCog(stack)) return;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityStirling stirling) {
            stirling.hasCog = false;
            stirling.setChanged();
        }
    }

    /** 1.7 metadata 0 (the item still carries its cog): {@code hbm:has_cog}, absent meaning true (ntm-next). */
    public static boolean hasCog(ItemStack stack) {
        return ModDataComponents.HAS_COG.getOr(stack, true);
    }

    /** {@code hbm:model_state}: 1 draws the item without its cog, as 1.7's metadata 1 did. */
    @Override
    public int modelState(ItemStack stack) {
        return hasCog(stack) ? 0 : 1;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockPos core = MultiblockSurface.coreOfAny(params.getLevel(), pos);
        if (core != null && params.getLevel().getBlockEntity(core) instanceof TileEntityStirling stirling
                && !stirling.hasCog) {
            ModDataComponents.HAS_COG.set(stack, false);
        }
        return List.of(stack);
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return cloneWithCogState(level, pos);
    }

    private ItemStack cloneWithCogState(BlockGetter level, BlockPos pos) {
        ItemStack stack = new ItemStack(this);
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityStirling stirling
                && !stirling.hasCog) {
            ModDataComponents.HAS_COG.set(stack, false);
        }
        return stack;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityStirling stirling)) return null;

        List<String> text = new ArrayList<>();
        text.add(stirling.heat + "TU/t");
        text.add((stirling.hasCog ? stirling.powerBuffer : 0) + "HE/t");
        if (!stirling.isCreative()) {
            int maxHeat = stirling.maxHeat();
            double percent = (double) stirling.heat / maxHeat;
            int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
            if (percent > 1D) color = 0xff0000;
            text.add("&[" + color + "&]" + ((stirling.heat * 1000 / maxHeat) / 10D) + "%");
            if (stirling.heat > maxHeat) text.add("&[0xff0000&]! ! ! OVERSPEED ! ! !");
            if (!stirling.hasCog) text.add("&[0xff0000&]Gear missing!");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /** Four ports around the waist. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
    }
}
