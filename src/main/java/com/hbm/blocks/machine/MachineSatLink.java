package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ISatChip;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The sat link dish. A satellite chip held against it sets which frequency the dish listens on. */
public class MachineSatLink extends BlockDummyable implements ILookOverlay {

    public MachineSatLink(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineSatLink(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{6, 0, 1, 0, 1, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        MultiblockHandlerXR.fillSpace(level, x, y, z, getDimensions(), this, dir);

        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, x - dir.offsetX, y, z - dir.offsetZ);
        makeExtra(level, x + rot.offsetX, y, z + rot.offsetZ);
        makeExtra(level, x - dir.offsetX + rot.offsetX, y, z - dir.offsetZ + rot.offsetZ);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hit) {
        if (!(stack.getItem() instanceof ISatChip)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return tuneTo(level, pos, stack, player) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Retunes the dish to the frequency a chip carries. */
    public boolean tuneTo(Level level, BlockPos pos, ItemStack chip, @Nullable Player player) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineSatLink link)) return false;

        link.freq = ISatChip.getFreqS(chip);
        link.setChanged();
        if (player != null) {
            player.sendSystemMessage(Component.literal("Set frequency to " + link.freq)
                    .withStyle(ChatFormatting.YELLOW));
        }
        level.playSound(null, pos, ModSounds.TECH_BLEEP.get(), SoundSource.BLOCKS, 1F, 1F);
        return true;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineSatLink link)) return null;

        List<String> text = new ArrayList<>();
        text.add("Frequency: " + link.freq);
        text.add("Connected: " + (link.connected ? "§aYes" : "§cNo"));
        for (Component component : link.info) {
            if (component != null) text.add(component.getString());
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.MACHINE_SATLINK.get(), TileEntityMachineSatLink::clientTick)
                : createTickerHelper(type, ModBlockEntities.MACHINE_SATLINK.get(), TileEntityMachineSatLink::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
