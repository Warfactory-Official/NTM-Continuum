package com.hbm.tileentity.network;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.explosion.vanillant.interfaces.IExplosionSFX;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.particle.helper.ParticleEffects;
import com.hbm.inventory.container.RadioControllerMenu;
import com.hbm.tileentity.network.RadioTargets;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import com.hbm.tileentity.ModBlockEntities;

/** Server-side ROR command controller. */
public class TileEntityRadioTorchController extends TileEntityLoadedBase implements IControlReceiver, RadioTorchTicking,
        MenuProvider {
    public String channel = "";
    public String prev;
    public boolean polling = true;
    /** Whether the channel carried a signal last tick; a new burst runs its first command even if unchanged. */
    private boolean receiving;
    private boolean firstTick = true;

    public TileEntityRadioTorchController(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_CONTROLLER.get(), pos, state);
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        boolean receivingNow = false;

        if (channel != null && !channel.isEmpty()) {
            IRORInteractive interactive = RadioTargets.interactive(level, worldPosition.relative(attachedDirection()));
            RTTYSystem.RTTYChannel received = interactive == null ? null : RTTYSystem.listen(level, channel);
            if (received != null) {
                String command = String.valueOf(received.signal);
                if ("selfdestruct".equals(command)) {
                    selfDestruct();
                    return;
                }
                if (received.timeStamp >= level.getGameTime() - 1 && received.timeStamp != -1) {
                    receivingNow = true;
                    boolean changed = !command.equals(prev);
                    if (polling || !receiving || changed) {
                        try {
                            if (!command.isEmpty()) {
                                interactive.runRORFunction(IRORInteractive.PREFIX_FUNCTION
                                        + IRORInteractive.getCommand(command), IRORInteractive.getParams(command));
                            }
                        } catch (RORFunctionException ignored) {
                            // 1.7 discards malformed remote commands
                        }
                    }
                    if (changed) {
                        prev = command;
                        setChanged();
                    }
                }
            }
        }

        // the broadcast queue starts empty after loading, so the saved burst state survives the first tick
        if ((!firstTick || receivingNow) && receiving != receivingNow) {
            receiving = receivingNow;
            setChanged();
        }
        firstTick = false;

        networkPackNT();
    }

    private void selfDestruct() {
        if (level == null) return;
        level.destroyBlock(worldPosition, false);
        new ExplosionVNT(level, worldPosition, 5F)
                .setEntityProcessor(new RadioControllerEntityProcessor())
                .setPlayerProcessor(new PlayerProcessorStandard())
                .setSFX(new RadioControllerExplosionSfx())
                .explode();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        polling = input.getBooleanOr("polling", true);
        channel = input.getStringOr("channel", "");
        prev = input.getStringOr("prev", null);
        receiving = input.read("receiving", Codec.BOOL).orElse(prev != null && !prev.isEmpty());
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("polling", polling);
        output.putString("channel", channel == null ? "" : channel);
        if (prev != null) output.putString("prev", prev);
        output.putBoolean("receiving", receiving);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = Tags.getBooleanOr(data, "polling", polling);
        if (data.contains("channel")) setChannelName(limited(Tags.getStringOr(data, "channel", ""), RTTYSystem.MAX_CHAN_LENGTH));
        setChanged();
        dataChanged();
    }

    /** A new channel forgets the last command and burst, so its first command runs. */
    public void setChannelName(String channel) {
        if (channel.equals(this.channel)) return;
        this.channel = channel;
        prev = null;
        receiving = false;
        setChanged();
    }

    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getCenter()) < 256D;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeBoolean(polling);
        buffer.writeUtf(channel == null ? "" : channel, RTTYSystem.MAX_CHAN_LENGTH);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        polling = buffer.readBoolean();
        channel = buffer.readUtf(RTTYSystem.MAX_CHAN_LENGTH);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttyController");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadioControllerMenu(id, inventory, this);
    }

    public Direction attachedDirection() {
        return (getBlockState().hasProperty(RadioTorchBase.FACING)
                ? getBlockState().getValue(RadioTorchBase.FACING) : Direction.UP).getOpposite();
    }

    public IRORInfo attachedInfo() {
        if (level == null) return null;
        return RadioTargets.info(level, worldPosition.relative(attachedDirection()));
    }

    private static String limited(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    /** Faithful replacement for old ExplosionEffectWeapon's server-side payload sender. */
    private final class RadioControllerExplosionSfx implements IExplosionSFX {
        @Override
        public void doEffect(ExplosionVNT explosion, Level world,
                             double x, double y, double z, float size) {
            ParticleEffects.explosionSmall(world, x, y, z, 10, 2.5F, 1F);
        }
    }
}
