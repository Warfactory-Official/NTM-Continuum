package com.hbm.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A 1.7 {@code IItemRenderer}, ported as a body drawn inside one of {@code Pose17}'s frames.
 *
 * <p>1.7 asked the renderer twice: {@code handleRenderType} decided whether it drew a given stack in a given
 * slot at all, and {@code renderItem} drew it. Both halves survive as they were. A slot the renderer declines
 * is drawn from the item's own flat model, which is how the held weapons are sprites in the inventory and
 * models in the hand. The draw receives the pose the engine had set up for that slot, so the body is the
 * renderer's own GL calls transcribed op for op; the slot-to-context mapping is {@link Type#of}.
 *
 * <p>Who calls this differs between eras and is not the renderer's concern: it rides the OBJ machine items' split.
 * 26.1 goes through {@code MachineItemModelRenderer.CustomItem} (an item model, because only an item model is told
 * the display context), older versions through {@code MachineItemModels.renderCustom} from the loader's BEWLR hook.
 */
public interface CustomItemRenderer {

    /** 1.7's {@code ItemRenderType}, the four the item renderers here handled. */
    enum Type {
        INVENTORY, EQUIPPED, EQUIPPED_FIRST_PERSON, ENTITY;

        /**
         * The modern display slot as 1.7 would have reached the renderer. Third person and the head were both
         * {@code EQUIPPED}; a dropped item and an item frame were both {@code ENTITY} (1.7 drew a framed item
         * through {@code RenderItem} like a dropped one).
         */
        public static Type of(ItemDisplayContext context) {
            return switch (context) {
                case GUI -> INVENTORY;
                case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> EQUIPPED_FIRST_PERSON;
                case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, HEAD -> EQUIPPED;
                default -> ENTITY;
            };
        }
    }

    /** 1.7 {@code handleRenderType}. A declined slot shows the item's flat model instead. */
    boolean handles(ItemStack stack, Type type);

    /** 1.7 {@code renderItem}, in the frame's space. The pose must be left as it was found. */
    void render(ItemStack stack, Type type, PoseStack pose, SubmitSink sink, int light, int overlay,
                @Nullable LivingEntity owner);

    /** Whether the drawing changes with time, so 26.1 must not serve a cached icon. */
    default boolean animated(ItemStack stack, Type type) {
        return false;
    }
}
