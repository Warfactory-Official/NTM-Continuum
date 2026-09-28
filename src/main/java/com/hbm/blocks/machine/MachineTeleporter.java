package com.hbm.blocks.machine;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.machine.TileEntityMachineTeleporter;
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
import java.util.Locale;

/**
 * 1.7 {@code MachineTeleporter}: a plain cube ({@code teleporter_top/_bottom/_side}) that sends whatever stands
 * on it to the exit its telelinker gave it, for a million HE a trip. Looking at it shows the charge and exit.
 */
public class MachineTeleporter extends CompatBlock implements TickingBlock, ILookOverlay {

    public MachineTeleporter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineTeleporter(pos, state);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityMachineTeleporter tele)) return null;

        List<String> text = new ArrayList<>();

        if (tele.targetY == -1) {
            text.add(ChatFormatting.RED + "No destination set!");
        } else {
            text.add((tele.power >= TileEntityMachineTeleporter.consumption ? ChatFormatting.GREEN : ChatFormatting.RED)
                    + String.format(Locale.US, "%,d", tele.power) + " / "
                    + String.format(Locale.US, "%,d", TileEntityMachineTeleporter.maxPower));
            text.add("Destination: " + tele.targetX + " / " + tele.targetY + " / " + tele.targetZ + " (D: " + tele.targetDim + ")");
        }

        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
