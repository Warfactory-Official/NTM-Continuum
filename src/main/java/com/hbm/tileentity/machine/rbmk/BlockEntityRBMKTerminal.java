package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.ModRBMKBlockEntities;
import dev.golem.core.machine.SyncedBlockEntity;
import com.hbm.tileentity.network.RTTYSystem;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The RTTY command terminal, ported from 1.12.2's {@code TileEntityRBMKTerminal}.
 *
 * <p>A seventeen-line scrollback and a one-line prompt over {@link RTTYSystem}: {@code chan} sets the
 * channel, {@code send} broadcasts once, {@code start} broadcasts the same thing every tick until
 * {@code stop}. It is the operator-facing half of the same bus the radio torches ride, which is why an
 * RBMK block ends up being a radio device.
 *
 * <p>Redstone-over-radio can drive the prompt as well as the display, so a terminal can be scripted from
 * another terminal. {@code set<line#>} is parsed out of the function name rather than a parameter,
 * matching the fork.
 */
public class BlockEntityRBMKTerminal extends SyncedBlockEntity
        implements IControlReceiver, IRORInteractive {

    public static final int HISTORY = 17;

    public final String[] history = new String[HISTORY];
    public String channel = "";
    public String repeatCmd = "";
    public boolean doesRepeat;

    public BlockEntityRBMKTerminal(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_TERMINAL.get(), pos, state);
        Arrays.fill(history, "");
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;

        if (!channel.isEmpty() && !repeatCmd.isEmpty()) {
            RTTYSystem.broadcast(level, channel, repeatCmd);
        }
        syncState();
    }

    public void eval(String cmd) {
        if (cmd == null || level == null) return;

        push(cmd);
        if (cmd.isEmpty()) return;

        if (cmd.startsWith("chan ")) {
            channel = cmd.substring(5);
            push("Set channel to " + (channel.isEmpty() ? "<none>" : channel));
            setChanged();
            return;
        }

        if (cmd.equals("chan")) {
            channel = "";
            push("Set channel to <none>");
            setChanged();
            return;
        }

        if (cmd.startsWith("start ")) {
            repeatCmd = cmd.substring(6);
            push("Repeating signal on " + channel);
            setChanged();
            return;
        }

        if (cmd.equals("stop")) {
            repeatCmd = "";
            push("Stopping repeat signal");
            setChanged();
            return;
        }

        if (cmd.startsWith("send ")) {
            if (channel.isEmpty()) {
                push("Cannot send - no channel set");
                return;
            }
            RTTYSystem.broadcast(level, channel, cmd.substring(5));
            push("Sent signal on " + channel);
            return;
        }

        if (cmd.equals("horse")) {
            push("Horse.");
            return;
        }

        if (cmd.equals("selfdestruct")) {
            level.destroyBlock(worldPosition, false);
            ExplosionVNT vnt = new ExplosionVNT(level, worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D, 5.0F, null);
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            vnt.explode();
            return;
        }

        if (cmd.equals("clear")) {
            Arrays.fill(history, "");
            return;
        }

        push("Unrecognized command!");
    }

    public void push(String msg) {
        System.arraycopy(history, 0, history, 1, history.length - 1);
        history[0] = msg;
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(!repeatCmd.isEmpty());
        for (String line : history) buf.writeUtf(line);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        doesRepeat = buf.readBoolean();
        for (int i = 0; i < history.length; i++) history[i] = buf.readUtf();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putString("channel", channel);
        output.putString("repeatCmd", repeatCmd);
        for (int i = 0; i < history.length; i++) output.putString("history" + i, history[i]);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        channel = input.getStringOr("channel", "");
        repeatCmd = input.getStringOr("repeatCmd", "");
        for (int i = 0; i < history.length; i++) history[i] = input.getStringOr("history" + i, "");
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) < 15D * 15D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("cmd")) {
            eval(Tags.getStringOr(data, "cmd", ""));
            setChanged();
        }
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_FUNCTION + "clear",
                PREFIX_FUNCTION + "write" + NAME_SEPARATOR + "text",
                PREFIX_FUNCTION + "set<line#>" + NAME_SEPARATOR + "text",
                PREFIX_FUNCTION + "submit" + NAME_SEPARATOR + "command"};
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "clear").equals(name)) {
            Arrays.fill(history, "");
            setChanged();
            return null;
        }

        String allParams = String.join(" ", params);

        if ((PREFIX_FUNCTION + "write").equals(name)) {
            push(allParams);
            setChanged();
            return null;
        }

        if (name.startsWith(PREFIX_FUNCTION + "set")) {
            int line = IRORInteractive.parseInt(name.substring(PREFIX_FUNCTION.length() + 3), 1, history.length) - 1;
            history[line] = allParams;
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "submit").equals(name)) {
            eval(allParams);
            return null;
        }

        return null;
    }
}
