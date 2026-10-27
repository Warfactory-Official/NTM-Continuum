package com.hbm.items.tool;

import com.hbm.entity.logic.BomberPayload;
import com.hbm.entity.logic.EntityBomber;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1.7 {@code ItemBombCaller} ("Airstrike Designator"): aim within 500 blocks and click to call an
 * {@link EntityBomber} of the given payload onto that spot; the designator is consumed. The 1.7
 * metadata (0-7) is one item per {@link Type}; only the first five were listed in the creative tab.
 *
 * <p>The payload is a {@link BomberPayload} name: an addon designator is
 * {@code new ItemBombCaller(properties, "ntmspace:airliner", "Civilian Airliner.", false)} after registering the
 * payload.
 */
public class ItemBombCaller extends CompatItem {

    /** HBM's eight designators (1.7 metadata 0-7), each naming its payload and its tooltip label. */
    public enum Type {
        CARPET("Carpet bombing"), NAPALM("Napalm"), CHLORINE("Poison gas"), ORANGE("Agent orange"),
        ABOMB("Atomic bomb"), STINGER("VT stinger rockets"), BOXCAR("PIP OH GOD"), PC("Cloud the cloud oh god the cloud");

        public final String label;

        Type(String label) {
            this.label = label;
        }

        /** The {@link BomberPayload} name, the lower-case constant name ({@code "carpet"}). */
        public String payload() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final double RANGE = 500D;

    /** The {@link BomberPayload} this designator calls. */
    public final String payload;
    private final String label;
    private final boolean foil;

    public ItemBombCaller(Properties properties, Type type) {
        // 1.7 hasEffect: the atomic and later designators glint
        this(properties, type.payload(), "Type: " + type.label, type.ordinal() >= 4);
    }

    /** A designator for {@code payload}; {@code label} is its second tooltip line. */
    public ItemBombCaller(Properties properties, String payload, String label, boolean foil) {
        this(properties, payload, label, foil, "Called in airstrike!");
    }

    /** A designator whose chat line on use is {@code message} (1.7: "Called in airstrike!"; hbm-space's airliner differs). */
    public ItemBombCaller(Properties properties, String payload, String label, boolean foil, String message) {
        super(properties.stacksTo(1));
        this.payload = payload;
        this.label = label;
        this.foil = foil;
        this.message = message;
    }

    private final String message;

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Aim & click to call an airstrike!"));
        tooltip.add(Component.literal(label));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        HitResult hit = player.pick(RANGE, 1.0F, false);
        BlockPos pos = hit instanceof BlockHitResult block ? block.getBlockPos() : BlockPos.containing(hit.getLocation());

        if (!level.isClientSide()) {
            BomberPayload called = BomberPayload.byName(payload);
            if (called == null) called = BomberPayload.CARPET; // 1.7 default branch
            EntityBomber bomber = called.create(level, pos.getX(), pos.getY(), pos.getZ());
            bomber.addToLevel(); // 1.7: WorldUtil.loadAndSpawnEntityInWorld
            player.sendSystemMessage(Component.literal(message));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        player.getItemInHand(hand).shrink(1);
        return InteractionResult.SUCCESS;
    }
}
