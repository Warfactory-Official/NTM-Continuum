package com.hbm.tileentity.deco;

import com.hbm.blocks.generic.BlockEmitter;
import com.hbm.packet.PlasmaBlastPayload;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.Color;

/**
 * 1.7 {@code BlockEmitter.TileEntityEmitter}: a light beam out of the facing side. Once a second the server
 * walks up to {@value #range} blocks until a solid block and remembers how far it got; colour, girth and the
 * special effect are set by dye, screwdriver/defuser and hand drill. Effect 4 walks a plasma-blast ring along
 * the beam every five ticks, in the rainbow colour when no dye was used. Everything is sent every tick.
 */
public class TileEntityEmitter extends TileEntityLoadedBase {

    public static final int range = 100;
    public int color;
    public int beam;
    public float girth = 0.5F;
    public int effect = 0;
    public static final int effectCount = 5;

    public TileEntityEmitter(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.EMITTER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityEmitter emitter) {
        emitter.update(level, state);
    }

    private void update(Level level, BlockState state) {
        Direction dir = state.getValue(BlockEmitter.FACING);
        long time = level.getGameTime();

        if (time % 20 == 0) {
            for (int i = 1; i <= range; i++) {
                beam = i;
                BlockPos p = worldPosition.relative(dir, i);
                BlockState b = level.getBlockState(p);
                // 1.7 Block.isBlockSolid: the material is solid
                if (!b.isAir() && b.blocksMotion()) break;
            }
        }

        if (effect == 4 && beam > 0) {
            if (time % 5 == 0) {
                // 1.7 cast before the modulo: (int) (xCoord + dx * (time / 5) % beam) + 0.5
                double x = (int) (worldPosition.getX() + dir.getStepX() * (time / 5L) % beam) + 0.5;
                double y = (int) (worldPosition.getY() + dir.getStepY() * (time / 5L) % beam) + 0.5;
                double z = (int) (worldPosition.getZ() + dir.getStepZ() * (time / 5L) % beam) + 0.5;

                int col = color;
                if (col == 0) col = Color.HSBtoRGB(time / 50.0F, 0.5F, 0.25F) & 16777215;

                float pitch = 0F, yaw = 0F;
                switch (dir) {
                    case NORTH -> pitch = 90;
                    case SOUTH -> pitch = -90;
                    case WEST -> { pitch = 90; yaw = 90; }
                    case EAST -> { pitch = -90; yaw = 90; }
                    default -> { }
                }
                PlasmaBlastPayload.send(level, x, y, z, ((col & 0xff0000) >> 16) / 256F, ((col & 0x00ff00) >> 8) / 256F,
                        (col & 0x0000ff) / 256F, pitch, yaw, girth * 5);
            }
        }

        networkPackNT();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.color = input.getIntOr("color", 0);
        this.girth = input.getFloatOr("girth", 0.5F);
        this.effect = input.getIntOr("effect", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("color", this.color);
        output.putFloat("girth", this.girth);
        output.putInt("effect", this.effect);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.beam);
        buf.writeInt(this.color);
        buf.writeFloat(this.girth);
        buf.writeInt(this.effect);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        this.beam = buf.readInt();
        this.color = buf.readInt();
        this.girth = buf.readFloat();
        this.effect = buf.readInt();
    }
}
