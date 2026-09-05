package com.hbm.tileentity.network;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.RORFunctionException;
import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.particle.helper.ParticleEffects;
import com.hbm.radio.ModRadioBlockEntities;
import com.hbm.radio.RadioControllerMenu;
import com.hbm.radio.RadioTargets;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Server-side ROR command controller. */
public final class TileEntityRadioTorchController extends TileEntityLoadedBase implements IControlReceiver, RadioTorchTicking,
        net.minecraft.world.MenuProvider {
    public String channel = "";
    public String prev;
    public boolean polling = true;

    public TileEntityRadioTorchController(BlockPos pos, BlockState state) {
        super(ModRadioBlockEntities.RADIO_TORCH_CONTROLLER.get(), pos, state);
    }

    @Override
    public void serverTick() {
        if (level == null || channel == null || channel.isEmpty()) {
            networkPackNT();
            return;
        }

        Direction direction = attachedDirection();
        BlockPos attachedPos = worldPosition.relative(direction);
        IRORInteractive interactive = RadioTargets.interactive(level, attachedPos);
        if (interactive != null) {
            RTTYSystem.RTTYChannel received = RTTYSystem.listen(level, channel);
            if (received != null) {
                String command = String.valueOf(received.signal);
                if ("selfdestruct".equals(command)) {
                    selfDestruct();
                    return;
                }
                if ((polling && received.timeStamp >= level.getGameTime() - 1)
                        || !command.equals(prev)) {
                    try {
                        if (!command.isEmpty()) {
                            interactive.runRORFunction(IRORInteractive.PREFIX_FUNCTION
                                    + IRORInteractive.getCommand(command), IRORInteractive.getParams(command));
                        }
                    } catch (RORFunctionException ignored) {
                        // The old controller intentionally discarded malformed remote commands.
                    }
                    prev = command;
                }
            }
        }
        networkPackNT();
    }

    private void selfDestruct() {
        if (level == null) return;
        level.destroyBlock(worldPosition, false);
        new ExplosionVNT(level, worldPosition, 5F)
                .setEntityProcessor(new RadioControllerEntityProcessor())
                .setPlayerProcessor(new com.hbm.explosion.vanillant.standard.PlayerProcessorStandard())
                .setSFX(new RadioControllerExplosionSfx())
                .explode();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        polling = input.getBooleanOr("polling", true);
        channel = input.getStringOr("channel", "");
        prev = input.getStringOr("prev", null);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("polling", polling);
        output.putString("channel", channel == null ? "" : channel);
        if (prev != null) output.putString("prev", prev);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = Tags.getBooleanOr(data, "polling", polling);
        if (data.contains("channel")) channel = limited(Tags.getStringOr(data, "channel", ""), 10);
        setChanged();
        dataChanged();
    }

    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getCenter()) < 256D;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeBoolean(polling);
        buffer.writeUtf(channel == null ? "" : channel, 10);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        polling = buffer.readBoolean();
        channel = buffer.readUtf(10);
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
    private final class RadioControllerExplosionSfx implements com.hbm.explosion.vanillant.interfaces.IExplosionSFX {
        @Override
        public void doEffect(ExplosionVNT explosion, net.minecraft.world.level.Level world,
                             double x, double y, double z, float size) {
            ParticleEffects.explosionSmall(world, x, y, z, 10, 2.5F, 1F);
        }
    }
}
