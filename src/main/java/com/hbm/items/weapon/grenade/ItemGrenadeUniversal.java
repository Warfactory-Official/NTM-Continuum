package com.hbm.items.weapon.grenade;

import com.hbm.items.ModEntityItems;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 1.7 ItemGrenadeUniversal.
 */
public class ItemGrenadeUniversal extends dev.golem.core.world.CompatItem implements com.hbm.items.IAnimatedItem {

    // Equip-edge only; the draw counter lives on HbmPlayerProps.
    // TODO: 1.7 ItemGrenadeUniversal:104-107 keys the edge off the stack's own equipped flag
    // (ItemGunBaseNT.getIsEquipped); 26.2 inventoryTick is server-only, so this stands in a static heartbeat.
    private static final Map<UUID, ItemStack> EQUIPPED = new HashMap<>();
    private static final Map<UUID, Long> LAST_HELD_TICK = new HashMap<>();

    public ItemGrenadeUniversal(Properties properties) {
        super(properties);
    }

    private static int deployment(Player player) {
        return HbmPlayerProps.getData(player).grenadeDeployment;
    }

    private static void onEquip(ServerPlayer player, ItemStack stack) {
        HbmPlayerProps.getData(player).grenadeDeployment = 0;
        EQUIPPED.put(player.getUUID(), stack.copy());
        // 1.7 HbmAnimationPacket(EQUIP): the draw and pin-pull animation, table in ToolAnimations.grenade
        if (stack.getItem() instanceof ItemGrenadeUniversal grenade)
            grenade.playAnimation(player, com.hbm.items.ToolAnimation.EQUIP, stack);
    }

    private static void clearStaleEquipState(long tick) {
        LAST_HELD_TICK.entrySet().removeIf(entry -> tick - entry.getValue() > 1200L);
        EQUIPPED.keySet().removeIf(id -> !LAST_HELD_TICK.containsKey(id));
    }

    public static GrenadeData getData(ItemStack stack) {
        return ModDataComponents.GRENADE.get(stack).orElse(GrenadeData.DEFAULT);
    }

    public static ItemStack make(ItemGrenadeShell.EnumGrenadeShell shell, ItemGrenadeFilling.EnumGrenadeFilling filling,
                                 ItemGrenadeFuze.EnumGrenadeFuze fuze) {
        return make(shell, filling, fuze, null, 1);
    }

    public static ItemStack make(ItemGrenadeShell.EnumGrenadeShell shell, ItemGrenadeFilling.EnumGrenadeFilling filling,
                                 ItemGrenadeFuze.EnumGrenadeFuze fuze,
                                 ItemGrenadeExtra.@Nullable EnumGrenadeExtra extra) {
        return make(shell, filling, fuze, extra, 1);
    }

    public static ItemStack make(ItemGrenadeShell.EnumGrenadeShell shell, ItemGrenadeFilling.EnumGrenadeFilling filling,
                                 ItemGrenadeFuze.EnumGrenadeFuze fuze,
                                 ItemGrenadeExtra.@Nullable EnumGrenadeExtra extra, int amount) {
        ItemStack stack = new ItemStack(ModEntityItems.grenade_universal.get(), amount);
        ModDataComponents.GRENADE.set(stack, new GrenadeData(shell, filling, fuze, extra));
        // 1.7 getItemStackLimit(stack) = the shell's limit (FRAG/STICK 4, TECH 2, NUKE 1); the item default is 4
        dev.golem.core.world.ItemStacks.setMaxStackSize(stack, shell.getStackLimit());
        return stack;
    }

    /**
     * 1.7 {@code getSubItems}: for every shell, every filling that fits it, every fuze — the plain grenade,
     * then one per extra. 19 shell/filling pairs x 5 fuzes x (1 + 4 extras) = 475 stacks, in that order.
     */
    public static java.util.List<ItemStack> subItems() {
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (ItemGrenadeShell.EnumGrenadeShell shell : ItemGrenadeShell.EnumGrenadeShell.values()) {
            for (ItemGrenadeFilling.EnumGrenadeFilling filling : ItemGrenadeFilling.EnumGrenadeFilling.values()) {
                if (!filling.compatibleShells.contains(shell)) continue;
                for (ItemGrenadeFuze.EnumGrenadeFuze fuze : ItemGrenadeFuze.EnumGrenadeFuze.values()) {
                    list.add(make(shell, filling, fuze));
                    for (ItemGrenadeExtra.EnumGrenadeExtra extra : ItemGrenadeExtra.EnumGrenadeExtra.values())
                        list.add(make(shell, filling, fuze, extra));
                }
            }
        }
        return list;
    }

    /**
     * Datagen-safe projection of 1.7 {@code ItemGrenadeUniversal.make(...)} pool entries.
     */
    public static ItemPool.Entry poolEntry(ItemGrenadeShell.EnumGrenadeShell shell,
                                           ItemGrenadeFilling.EnumGrenadeFilling filling,
                                           ItemGrenadeFuze.EnumGrenadeFuze fuze,
                                           ItemGrenadeExtra.@Nullable EnumGrenadeExtra extra,
                                           int min, int max, int weight) {
        return new ItemPool.Entry(make(shell, filling, fuze, extra, 1), Math.min(min, max), Math.max(min, max), weight);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // TODO: 1.7 ItemGrenadeUniversal:98-109 ticks the counter on both sides, so its client gate can pass and
        // swing the arm. Here it only ever advances server-side, so the client always PASSes and never swings;
        // the throw itself still lands, because MultiPlayerGameMode.useItem sends its packet regardless.
        if (deployment(player) < getData(stack).shell().getDrawDuration()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            level.addFreshEntity(new EntityGrenadeUniversal(level, player, stack));
            dev.golem.core.world.ItemStacks.consume(stack, 1, player);
            if (!stack.isEmpty()) onEquip((ServerPlayer) player, stack);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot,
                                       boolean held) {
        // 1.7 isHeld: the selected hotbar slot, main hand only. Asked as held + main-hand identity because below 26.1
        // the slot argument is always null (only the selected flag exists), which had left the grenade undrawable there.
        if (!(owner instanceof ServerPlayer player) || !held || player.getMainHandItem() != stack) return;
        UUID id = player.getUUID();
        long tick = level.getGameTime();
        ItemStack previous = EQUIPPED.get(id);
        Long previousTick = LAST_HELD_TICK.get(id);
        if (previous == null || previousTick == null || previousTick != tick - 1 || !dev.golem.core.world.ItemStacks.sameItemAndData(
                previous, stack)) onEquip(player, stack);
        EQUIPPED.put(id, stack.copy());
        LAST_HELD_TICK.put(id, tick);
        clearStaleEquipState(tick);
        int deployed = HbmPlayerProps.getData(player).grenadeDeployment + 1;
        HbmPlayerProps.getData(player).grenadeDeployment = deployed;
        switch (getData(stack).shell()) {
            case FRAG -> {
                if (deployed == 18) level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModGunSounds.REVOLVER_COCK.get(), SoundSource.PLAYERS, 1F, 1F);
            }
            case STICK -> {
                if (deployed == 16 || deployed == 25)
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), ModGunSounds.BOLT_OPEN.get(),
                            SoundSource.PLAYERS, 1F, 1.25F);
            }
            case TECH -> {
                if (deployed == 18)
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GRENADE_TECH.get(),
                            SoundSource.PLAYERS, 1F, 1F);
            }
            case NUKE -> {
                if (deployed == 26)
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GRENADE_NUKA.get(),
                            SoundSource.PLAYERS, 1F, 1F);
            }
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, dev.golem.core.world.CompatTooltipContext context,
                                         java.util.List<Component> tooltip, TooltipFlag flag) {
        Consumer<Component> adder = tooltip::add;
        GrenadeData data = getData(stack);
        adder.accept(Component.translatable(ModEntityItems.grenade_shell.get(data.shell()).getDescriptionId()).withStyle(ChatFormatting.YELLOW));
        adder.accept(Component.translatable(ModEntityItems.grenade_filling.get(data.filling()).getDescriptionId()).withStyle(ChatFormatting.YELLOW));
        adder.accept(Component.translatable(ModEntityItems.grenade_fuze.get(data.fuze()).getDescriptionId()).withStyle(ChatFormatting.YELLOW));
        if (data.extra() != null) {
            adder.accept(Component.translatable(ModEntityItems.grenade_extra.get(data.extra()).getDescriptionId()).withStyle(ChatFormatting.RED));
        }
    }
}
