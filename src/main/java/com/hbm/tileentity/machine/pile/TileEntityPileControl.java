package com.hbm.tileentity.machine.pile;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.pile.TileEntityPileCore.PileChannel;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The control rod that hangs into a vertical channel. Redstone drives it: a rising edge withdraws it,
 * a falling edge drops it back in, and it travels at a fixed rate so the pile never steps in one tick.
 *
 * <p>Upstream calls the withdrawal fraction {@code level}; here it is {@code extraction}, because
 * {@code BlockEntity} already owns a field called {@code level} and shadowing it would make every
 * unqualified use of the world silently read a double.
 */
public class TileEntityPileControl extends TileEntityPileDeviceBase implements IRORInteractive {

    /** Fraction of a full stroke the rod travels per tick. */
    public static final double SPEED = 1D / 60D;

    public double extraction;
    public double lastExtraction;
    public double targetExtraction;
    public double syncExtraction;
    public int turnProgress;
    public boolean wasRedstone;

    public TileEntityPileControl(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_CONTROL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileControl control) {
        boolean canMove = false;

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);

        if (belowState.is(ModPileBlocks.pile_block.get())
                && belowState.getValue(BlockPile.META) == BlockPile.META_CONTROL
                && level.getBlockEntity(below) instanceof TileEntityPileBaseMK2 pile) {

            TileEntityPileCore core = pile.getCore();
            if (core != null) {
                PileChannel channel = core.getControlChannel(below.getX(), below.getY(), below.getZ());
                if (channel != null) {
                    canMove = true;
                    control.chanNum = core.getControlChannelNum(channel);
                    channel.control = control.extraction;
                }
            }
        }

        if (canMove && control.extraction != control.targetExtraction) {
            if (Math.abs(control.extraction - control.targetExtraction) <= SPEED) {
                control.extraction = control.targetExtraction;
            } else if (control.extraction < control.targetExtraction) {
                control.extraction += SPEED;
            } else {
                control.extraction -= SPEED;
            }
        }

        ForgeDirection dir = control.getOrientation();
        boolean redstone = level.getSignal(pos.offset(dir.offsetX, 0, dir.offsetZ),
                dir.getOpposite().toDirection()) > 0;

        if (redstone && !control.wasRedstone) control.setTarget(1D);
        if (!redstone && control.wasRedstone) control.setTarget(0D);
        control.wasRedstone = redstone;

        control.syncState();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityPileControl control) {
        control.lastExtraction = control.extraction;

        if (control.turnProgress > 0) {
            control.extraction += (control.syncExtraction - control.extraction) / control.turnProgress;
            control.turnProgress--;
        } else {
            control.extraction = control.syncExtraction;
        }
    }

    public void setTarget(double target) {
        this.targetExtraction = target;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.extraction);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        double lastSync = this.syncExtraction;
        this.syncExtraction = buf.readDouble();
        if (this.syncExtraction != lastSync) this.turnProgress = 2;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putDouble("level", extraction);
        output.putDouble("targetLevel", targetExtraction);
        output.putBoolean("redstone", wasRedstone);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        extraction = input.getDoubleOr("level", 0D);
        targetExtraction = input.getDoubleOr("targetLevel", 0D);
        wasRedstone = input.getBooleanOr("redstone", false);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "extendrods" + NAME_SEPARATOR + "percent"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            setTarget(IRORInteractive.parseInt(params[0], 0, 100) / 100D);
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "extendrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], -100, 100);
            setTarget(Mth.clamp(this.targetExtraction + percent / 100D, 0D, 1D));
            setChanged();
            return null;
        }

        return null;
    }
}
