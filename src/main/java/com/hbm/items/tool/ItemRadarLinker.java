package com.hbm.items.tool;

import com.hbm.api.block.IRadarCommandReceiver;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineRadarScreen;
import com.hbm.util.LockTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1.7 {@code ItemRadarLinker}: links a radar to a screen or to anything that takes radar commands (launch pads,
 * artillery). Clicking any part of a multiblock saves its core, 1.7's {@code CompatExternal.getCoreFromPos}.
 * The linker then sits in one of the radar's eight link slots; slot 8 feeds a screen.
 */
public class ItemRadarLinker extends ItemCoordinateBase {

    public ItemRadarLinker(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canGrabCoordinateHere(Level level, BlockPos pos) {
        BlockEntity tile = LockTargets.coreAt(level, pos);
        return tile instanceof IRadarCommandReceiver || tile instanceof TileEntityMachineRadarScreen;
    }

    @Override
    public BlockPos getCoordinates(Level level, BlockPos pos) {
        BlockEntity tile = LockTargets.coreAt(level, pos);
        return tile == null ? pos : tile.getBlockPos();
    }

    @Override
    public void onTargetSet(Level level, BlockPos pos, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
