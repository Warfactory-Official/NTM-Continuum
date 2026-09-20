package com.hbm.tileentity.bomb;

import com.hbm.entity.item.EntityFireworks;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityFireworks}: the firework battery, which spells a message into the sky.
 *
 * <p>While powered it fires one shell every {@link #SHOT_DELAY} ticks, each carrying the next
 * character of {@link #message}, and pauses for {@link #LOOP_DELAY} at the end of the message before
 * starting again. Successive shells walk a 3x3 lattice ({@code index % 9}) so a word does not come
 * out of one spot. Cutting the signal resets both the timer and the position in the message, so the
 * next run starts from the first character.
 *
 * <p>Two things move off 1.7's machinery. The burst was a bespoke particle plus fifty spark entities
 * behind an aux packet; {@link EntityFireworks} already carries that on vanilla's own firework
 * explosion. The muzzle flame was the same aux packet with {@code vanillaExt}/{@code flame}, which is
 * what {@code ServerLevel.sendParticles} is, so it goes straight out rather than through a payload.
 */
public class TileEntityFireworks extends CompatBlockEntity {

    /** Ticks between shells, 1.7's {@code delay = 30}. */
    private static final int SHOT_DELAY = 30;
    /** The longer pause after the last character, 1.7's {@code delay = 100}. */
    private static final int LOOP_DELAY = 100;
    /** Height the shell leaves from, and the height the muzzle flame is drawn at. */
    private static final double MUZZLE_Y = 1.5D;
    private static final double FLAME_Y = 1.125D;
    /** A third of a block: the lattice step 1.7 walks the launch point through. */
    private static final double LATTICE_STEP = 0.3125D;

    public int color = 0xff0000;
    public String message = "NUCLEAR TECH";
    public int charges;

    private int index;
    private int delay;

    public TileEntityFireworks(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FIREWORKS.get(), pos, state);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server)) return;

        if (!server.hasNeighborSignal(worldPosition) || message.isEmpty() || charges <= 0) {
            delay = 0;
            index = 0;
            return;
        }

        delay--;
        if (delay > 0) return;
        delay = SHOT_DELAY;

        int character = message.charAt(index);
        int cell = index % 9;
        double offX = (cell / 3 - 1) * LATTICE_STEP;
        double offZ = (cell % 3 - 1) * LATTICE_STEP;
        double x = worldPosition.getX() + 0.5D + offX;
        double z = worldPosition.getZ() + 0.5D + offZ;

        EntityFireworks shell = new EntityFireworks(server, x, worldPosition.getY() + MUZZLE_Y, z, color, character);
        server.addFreshEntity(shell);
        server.playSound(null, x, worldPosition.getY() + MUZZLE_Y, z, ModSounds.WEAPON_ROCKET_FLAME.get(),
                SoundSource.BLOCKS, 3.0F, 1.0F);
        server.sendParticles(ParticleTypes.FLAME, x, worldPosition.getY() + FLAME_Y, z, 1, 0D, 0D, 0D, 0D);

        charges--;
        setChanged();

        index++;
        if (index >= message.length()) {
            index = 0;
            delay = LOOP_DELAY;
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("charges", charges);
        output.putInt("color", color);
        output.putString("message", message);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        charges = input.getIntOr("charges", 0);
        color = input.getIntOr("color", 0xff0000);
        message = input.getStringOr("message", "NUCLEAR TECH");
    }
}
