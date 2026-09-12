package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGearItems;
import com.hbm.items.machine.ItemGear;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityStirling;
import net.minecraft.core.BlockPos;
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
public class MachineStirling extends BlockDummyable implements IEnergyConnectorBlock, ILookOverlay {

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
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        addExtraPorts(level, x + dir.offsetX * offset, y, z + dir.offsetZ * offset);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addExtraPorts(level, core.getX(), core.getY(), core.getZ());
    }

    private void addExtraPorts(Level level, int x, int y, int z) {
        makeExtra(level, x + 1, y, z);
        makeExtra(level, x - 1, y, z);
        makeExtra(level, x, y, z + 1);
        makeExtra(level, x, y, z - 1);
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(level, pos) instanceof TileEntityStirling stirling
                && IEnergyConnectorBlock.isPort(stirling.getConPos(), pos, dir);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityStirling stirling
                && !stirling.hasCog && stack.is(ModGearItems.gear_large.get())
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
        if (level.isClientSide() || ModDataComponents.ITEM_VARIANT.getOr(stack, 0) != 1) return;
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityStirling stirling) {
            stirling.hasCog = false;
            stirling.setChanged();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockPos core = findCore(params.getLevel(), pos);
        if (core != null && params.getLevel().getBlockEntity(core) instanceof TileEntityStirling stirling
                && !stirling.hasCog) {
            ModDataComponents.ITEM_VARIANT.set(stack, 1);
        }
        return List.of(stack);
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return cloneWithCogState(level, pos);
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return cloneWithCogState(level, pos);
    }

    private ItemStack cloneWithCogState(BlockGetter level, BlockPos pos) {
        ItemStack stack = new ItemStack(this);
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityStirling stirling
                && !stirling.hasCog) {
            ModDataComponents.ITEM_VARIANT.set(stack, 1);
        }
        return stack;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityStirling stirling)) return null;

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

}
