package com.hbm.tileentity.network;

import com.hbm.Constants;
import com.hbm.config.AutocalConfig;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.modules.IParse;
import com.hbm.modules.IParse.EnumStatementReturn;
import com.hbm.modules.IParse.ParseContext;
import com.hbm.modules.ParseMSES1Ext1;
import com.hbm.radio.RadioAutocalMenu;
import com.hbm.radio.ModRadioBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/** Server-side MS-ES1 execution state and client display state for AUTOCAL. */
public final class TileEntityRadioAUTOCAL extends TileEntityLoadedBase
        implements IControlReceiver, MenuProvider {
    public boolean isOn;
    public boolean ignoreError;
    public boolean autoReboot;
    public final String[] history = {"", "", "", "", "", ""};
    public String[] script = new String[0];
    public final IParse msesv1ext = new ParseMSES1Ext1();
    public ParseContext ctx;

    public TileEntityRadioAUTOCAL(BlockPos pos, BlockState state) {
        super(ModRadioBlockEntities.RADIO_AUTOCAL.get(), pos, state);
        this.ctx = new ParseContext(null);
    }

    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        if (level.getGameTime() % 60L == 0L) markChanged();

        ensureContext();
        if (!isOn && autoReboot) isOn = true;

        if (isOn) {
            int emergencyBrake = AutocalConfig.AUTOCAL_MAX_CLOCK * 5;
            for (int i = 0; i < ctx.clockSpeed && emergencyBrake > 0; i++) {
                emergencyBrake--;

                if (ctx.current == script.length) {
                    stop(ChatFormatting.YELLOW + "Program has terminated");
                    break;
                }
                if (ctx.current < 0 || ctx.current >= script.length) {
                    stop(ChatFormatting.RED + "Program index out of bounds");
                    break;
                }

                try {
                    int index = ctx.current;
                    ctx.current++;
                    String line = script[index];
                    IParse.ReturnInfo ret = msesv1ext.eval(ctx, line, index);
                    if (ret.type() != EnumStatementReturn.SKIP) pushMsg((index + 1) + ": " + line);
                    history[0] = ChatFormatting.WHITE + "Buffer: " + ctx.readBuffer();
                    if (ret.type() == EnumStatementReturn.END_TICK) break;
                    if (ret.type() == EnumStatementReturn.SHUTDOWN) stop(ChatFormatting.YELLOW + "Program requested shutdown");

                    if (!ignoreError) {
                        String extraInfo = ret.extraInfo().isEmpty() ? "" : ": " + ret.extraInfo();
                        if (ret.type() == EnumStatementReturn.UNRECOGNIZED_COMMAND) {
                            stop(ChatFormatting.RED + "Unrecognized command at line " + (ret.line() + 1) + extraInfo);
                        }
                        if (ret.type() == EnumStatementReturn.PARAMETER_ERROR) {
                            stop(ChatFormatting.RED + "Parameter error at line " + (ret.line() + 1) + extraInfo);
                        }
                        if (ret.type() == EnumStatementReturn.UNDEFINED) {
                            stop(ChatFormatting.RED + "Undefined behavior at line " + (ret.line() + 1) + extraInfo);
                        }
                        if (ret.type() == EnumStatementReturn.STACK_EXCEEDED) {
                            stop(ChatFormatting.RED + "Stack exceeded capacity at line " + (ret.line() + 1) + extraInfo);
                        }
                    }

                    if (ret.type() == EnumStatementReturn.SKIP) i--;
                } catch (Exception exception) {
                    stop(ChatFormatting.RED + "Evaluation unsuccessful");
                    Constants.LOG.warn("AUTOCAL evaluation unsuccessful", exception);
                }
            }
        }

        networkPackNT();
    }

    private void ensureContext() {
        if (ctx == null) ctx = new ParseContext(level);
        if (ctx.world != level) ctx.world = level;
    }

    public void pushMsg(String message) {
        for (int i = 2; i < history.length; i++) history[i - 1] = history[i];
        history[history.length - 1] = message;
    }

    public void stop(String reason) {
        isOn = false;
        ensureContext();
        ctx.turnOff();
        pushMsg(reason);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isOn = input.getBooleanOr("isOn", false);
        ignoreError = input.getBooleanOr("ignoreError", false);
        autoReboot = input.getBooleanOr("autoReboot", false);
        List<String> savedScript = input.read("script", ParseContext.SCRIPT_CODEC).orElse(List.of());
        script = savedScript.toArray(String[]::new);
        ctx = new ParseContext(null);
        ctx.readData(input, script, msesv1ext);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("ignoreError", ignoreError);
        output.putBoolean("autoReboot", autoReboot);
        output.store("script", ParseContext.SCRIPT_CODEC, Arrays.asList(script));
        ensureContext();
        ctx.writeData(output);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeBoolean(isOn);
        buffer.writeBoolean(ignoreError);
        buffer.writeBoolean(autoReboot);
        for (String message : history) buffer.writeUtf(message == null ? "" : message);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        isOn = buffer.readBoolean();
        ignoreError = buffer.readBoolean();
        autoReboot = buffer.readBoolean();
        for (int i = 0; i < history.length; i++) history[i] = buffer.readUtf();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.autocal");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadioAutocalMenu(id, inventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 1D,
                worldPosition.getZ() + 0.5D) <= 15D * 15D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        ensureContext();
        if (data.contains("on")) {
            if (isOn) stop(ChatFormatting.YELLOW + "User requested shutdown");
            else isOn = true;
        }
        if (data.contains("ignore")) ignoreError = !ignoreError;
        if (data.contains("auto")) autoReboot = !autoReboot;
        if (data.contains("payload")) loadScript(Tags.getStringOr(data, "payload", ""));
        markChanged();
        dataChanged();
    }

    private void loadScript(String payload) {
        ctx.jmp.clear();
        script = payload.split("\n");
        for (int i = 0; i < script.length; i++) {
            script[i] = script[i].trim();
            msesv1ext.generateJumpPoints(ctx, script[i], i);
        }
        if (isOn) stop(ChatFormatting.YELLOW + "Script has changed");
    }
}
