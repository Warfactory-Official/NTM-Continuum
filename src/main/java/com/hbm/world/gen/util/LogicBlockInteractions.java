package com.hbm.world.gen.util;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.generic.LogicBlock.TileEntityLogicBlock;
import com.hbm.items.ModItems;
import com.hbm.potion.ModEffects;
import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import dev.golem.core.world.Effects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.7 {@code LogicBlockInteractions}: what a logic block does when right-clicked, by name. The consumer gets the
 * level, the block entity, x, y, z, the player, the side and the three hit offsets, in that order.
 */
public class LogicBlockInteractions {

    public static LinkedHashMap<String, Consumer<Object[]>> interactions;

    static Component tagged(String tag, String message) {
        return Component.literal(tag).withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(message).withStyle(ChatFormatting.RESET));
    }

    public static Consumer<Object[]> TEST = (array) -> {
        TileEntityLogicBlock logic = (TileEntityLogicBlock) array[1];
        Player player = (Player) array[5];

        if (logic.phase > 1) return;
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty()) held.shrink(1);
        logic.phase++;
    };

    public static Consumer<Object[]> RAD_CONTAINMENT_SYSTEM = (array) -> {
        TileEntityLogicBlock logic = (TileEntityLogicBlock) array[1];
        Player player = (Player) array[5];
        ItemStack held = player.getMainHandItem();

        if (!held.isEmpty() && held.is(ModItems.key.get())) {
            held.shrink(1);
            player.sendSystemMessage(tagged("[RAD CONTAINMENT SYSTEM]", " Radiation treatment administered"));
            Effects.addRegistered(player, ModEffects.RADAWAY.get(), 3 * 60 * 20, 4);
            Effects.addRegistered(player, ModEffects.RADX.get(), 3 * 60 * 20, 4);
            logic.phase = 2;
            logic.timer = 0;
        }
    };

    public static Consumer<Object[]> POWER_LOCK = (array) -> {
        Level world = (Level) array[0];
        Player player = (Player) array[5];
        BlockPos pos = new BlockPos((int) array[2], (int) array[3], (int) array[4]);

        IEnergyHandlerMK2 handler = null;
        for (Direction dir : Direction.values()) {
            if (world.getBlockEntity(pos.relative(dir)) instanceof IEnergyHandlerMK2 h) {
                handler = h;
                break;
            }
        }

        if (handler == null || !(handler.getPower() > 500_000)) {
            player.sendSystemMessage(tagged("[POWER LOCK]",
                    " Charge adjacent energy storage to at least 500KHE to release emergency lock"));
        } else {
            player.sendSystemMessage(tagged("[POWER LOCK]", " Power Restorted! Safe Unlocked!"));

            TileEntityLockableBase safe = null;
            for (Direction dir : Direction.values()) {
                BlockEntity te = world.getBlockEntity(pos.relative(dir));
                if (te instanceof TileEntityLockableBase lockable) {
                    safe = lockable;
                    break;
                }
            }
            if (safe != null) {
                safe.unlock();
                world.playSound(null, player.getX(), player.getY(), player.getZ(), ModDoorSounds.LOCK_OPEN.get(),
                        SoundSource.PLAYERS, 3.0F, 0.8F);
            }
        }
    };

    public static List<String> getInteractionNames() {
        return new ArrayList<>(interactions.keySet());
    }

    // register new interactions here
    static {
        initialize();
    }

    public static void initialize() {
        interactions = new LinkedHashMap<>();

        interactions.put("POWER_LOCK", POWER_LOCK);

        // example interactions
        interactions.put("TEST", TEST);
        interactions.put("RADAWAY_INJECTOR", RAD_CONTAINMENT_SYSTEM);
    }
}
