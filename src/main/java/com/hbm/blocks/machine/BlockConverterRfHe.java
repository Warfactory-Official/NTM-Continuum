package com.hbm.blocks.machine;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.network.TileEntityConverterRfHe;
import com.hbm.util.BobMathUtil;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The 1.7 {@code BlockConverterRfHe}: a plain cube over {@link TileEntityConverterRfHe}, with the RF-in / HE-out look overlay. */
public class BlockConverterRfHe extends CompatBlock implements TickingBlock, ILookOverlay {

    public BlockConverterRfHe(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityConverterRfHe(pos, state);
    }

    /** 1.7 {@code printHook}. */
    @Override
    public @Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityConverterRfHe converter)) return null;
        List<String> text = new ArrayList<>();
        text.add(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + BobMathUtil.getShortNumber(converter.stored) + "RF");
        text.add(ChatFormatting.RED + "<- " + ChatFormatting.RESET + BobMathUtil.getShortNumber(converter.getPower()) + "HE");
        return new Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
