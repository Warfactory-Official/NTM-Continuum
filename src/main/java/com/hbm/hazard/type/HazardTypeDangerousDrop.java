package com.hbm.hazard.type;

import com.hbm.api.hazard.IHazardType;
import java.util.List;
import java.util.function.ObjDoubleConsumer;

import com.hbm.api.hazard.IHazardModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Custom dropped-item behaviour (e.g. the demon core snapping shut). Dropped-item ticking is deferred. */
public class HazardTypeDangerousDrop implements IHazardType {

    private final ObjDoubleConsumer<ItemEntity> onDroppedItemUpdate;

    public HazardTypeDangerousDrop(ObjDoubleConsumer<ItemEntity> onDrop) {
        this.onDroppedItemUpdate = onDrop;
    }

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
        // Nothing
    }

    @Override
    public void updateEntity(ItemEntity item, double level) {
        onDroppedItemUpdate.accept(item, level);
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers) {
        list.add(Component.literal("[").append(Component.translatable("trait.drop")).append(Component.literal("]")).withStyle(ChatFormatting.RED));
    }
}
