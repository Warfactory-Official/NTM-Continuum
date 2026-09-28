package com.hbm.items.tool;

import com.hbm.items.IItemControlReceiver;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemSatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import dev.golem.core.data.Tags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * 1.7 {@code ItemSatInterface}, registered as {@code sat_coord} (the "Satellite Designator"): right-click opens
 * {@code GUIScreenSatCoord}, where a typed X/Y/Z is sent back as an item control tag and handed to the satellite
 * on the chip's frequency as a coordinate action. While held, the server keeps a {@code connected} flag on the
 * stack that the screen reads to unlock its fields.
 */
public class ItemSatInterface extends ItemSatChip implements IItemControlReceiver {

    public static final String KEY_NBT_CONNECTED = "connected";

    /** Installed by the client registrations; 1.7 {@code provideGUI} returning {@code GUIScreenSatCoord}. */
    public static Consumer<Player> OPEN_SCREEN = player -> { };

    public ItemSatInterface(Properties properties) {
        super(properties);
    }

    public static boolean isConnected(ItemStack stack) {
        CompoundTag data = ModDataComponents.PERSISTENT_DATA.get(stack).orElse(null);
        return data != null && Tags.getBooleanOr(data, KEY_NBT_CONNECTED, false);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) OPEN_SCREEN.accept(player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot,
                                       boolean held) {
        if (!(entity instanceof ServerPlayer player)) return;
        // 1.7: only the stack in the main hand
        if (player.getMainHandItem() != stack) return;

        Satellite sat = SatelliteSavedData.get(level).getSatFromFreq(this.getFreq(stack));
        boolean connected = sat != null;
        if (isConnected(stack) == connected && ModDataComponents.PERSISTENT_DATA.get(stack).isPresent()) return;

        ModDataComponents.PERSISTENT_DATA.update(stack, new CompoundTag(), tag -> {
            CompoundTag copy = tag.copy();
            copy.putBoolean(KEY_NBT_CONNECTED, connected);
            return copy;
        });
    }

    @Override
    public void receiveControl(Player player, ItemStack stack, CompoundTag data) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Satellite sat = SatelliteSavedData.get(level).getSatFromFreq(this.getFreq(stack));
        if (sat != null) {
            sat.onCoordAction(level, player, Tags.getIntOr(data, "x", 0),
                    Tags.contains(data, "y") ? Tags.getIntOr(data, "y", 0) : -1, Tags.getIntOr(data, "z", 0));
            SatelliteSavedData.get(level).setDirty();
        }
    }
}
