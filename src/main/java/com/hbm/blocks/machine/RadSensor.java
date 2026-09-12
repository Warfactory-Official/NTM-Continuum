package com.hbm.blocks.machine;

import com.hbm.sound.ModSounds;
import dev.golem.core.registry.TickingBlock;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityRadSensor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Radiation sensor with the legacy dose reset, redstone ladder, and comparator ladder. */
public class RadSensor extends dev.golem.core.world.CompatBlock implements TickingBlock {

    public RadSensor(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadSensor(pos, state);
    }

    @Override
    protected net.minecraft.world.InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                       Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityRadSensor sensor) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            player.sendSystemMessage(Component.literal("===== \u2622 Radiaton Sensor \u2622 =====")
                    .withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.literal("Current chunk radiation: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(sensor.chunkRads + " RAD/s").withStyle(ChatFormatting.GREEN)));
            player.sendSystemMessage(Component.literal("Redstone signal output: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(Integer.toString(sensor.redstoneOutput)).withStyle(ChatFormatting.RED)));
            player.sendSystemMessage(Component.literal("Recieved radiation dose: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(sensor.recievedDose + " RAD").withStyle(ChatFormatting.GREEN)));
            player.sendSystemMessage(Component.literal("Comparator signal output: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(Integer.toString(sensor.comparatorOutput)).withStyle(ChatFormatting.RED)));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (direction == Direction.UP) return 0;
        return level.getBlockEntity(pos) instanceof TileEntityRadSensor sensor ? sensor.redstoneOutput : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityRadSensor sensor ? sensor.comparatorOutput : 0;
    }

}
