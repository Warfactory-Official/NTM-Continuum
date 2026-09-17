package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
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
public class MachineSatLink extends BlockMultiblockMachine implements ILookOverlay {

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
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                         net.minecraft.world.InteractionHand hand, ItemStack stack,
                                         BlockHitResult hit) {
        if (!(stack.getItem() instanceof ISatChip)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        return tuneTo(level, pos, stack, player) ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** Retunes the dish to the frequency a chip carries. */
    public boolean tuneTo(Level level, BlockPos pos, ItemStack chip, @Nullable Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
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
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineSatLink link)) return null;

        List<String> text = new ArrayList<>();
        text.add("Frequency: " + link.freq);
        text.add("Connected: " + (link.connected ? "§aYes" : "§cNo"));
        for (Component component : link.info) {
            if (component != null) text.add(component.getString());
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

}
