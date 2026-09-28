package com.hbm.packet;

import com.hbm.Constants;
import com.hbm.entity.missile.EntityBobmazon;
import com.hbm.handler.BobmazonOfferFactory;
import com.hbm.handler.BobmazonOfferFactory.Offer;
import com.hbm.lib.ModDamageSources;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import dev.golem.core.world.Compat;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 1.7 {@code ItemBobmazonPacket}: the index of the offer bought from the held catalogue. The server checks the
 * requirement (or creative) and the bottle-cap balance, takes the caps and drops a Bobmazon delivery drone from
 * y 300 within a gaussian ten blocks of the buyer. An index that names no offer trips the "fail-safe".
 */
public record ItemBobmazonPayload(int offer) implements GolemPayload {

    public static final GolemCodec<ItemBobmazonPayload> CODEC = GolemCodec.of(ItemBobmazonPayload.class,
            Ids.of(Constants.MOD_ID, "item_bobmazon"),
            (payload, buffer) -> buffer.writeInt(payload.offer),
            buffer -> new ItemBobmazonPayload(buffer.readInt()));

    private static final String[] CAPS = {"cap_fritz", "cap_korl", "cap_nuka", "cap_quantum", "cap_rad", "cap_sparkle"};

    @Override
    public Identifier id() {
        return CODEC.id();
    }

    public static void handleServer(ItemBobmazonPayload m, ServerPlayer p) {
        ServerLevel world = (ServerLevel) p.level();

        List<Offer> offers = BobmazonOfferFactory.getOffers(p.getMainHandItem());
        Offer offer = offers != null && m.offer >= 0 && m.offer < offers.size() ? offers.get(m.offer) : null;

        if (offer == null) {
            p.sendSystemMessage(Component.literal("[BOBMAZON] There appears to be a mismatch between the offer you have requested and the offers that exist."));
            p.sendSystemMessage(Component.literal("[BOBMAZON] Engaging fail-safe..."));
            Compat.hurt(p, world, ModDamageSources.source(world, ModDamageSources.NUCLEAR_BLAST), 1000);
            p.setDeltaMovement(p.getDeltaMovement().x, 2.0D, p.getDeltaMovement().z);
            p.hurtMarked = true;
            return;
        }

        ItemStack stack = offer.offer;

        if (offer.requirement.fullfills(p) || p.isCreative()) {
            if (countCaps(p) >= offer.cost || p.isCreative()) {
                payCaps(p, offer.cost);
                p.inventoryMenu.broadcastChanges();

                RandomSource rand = world.getRandom();
                EntityBobmazon bob = new EntityBobmazon(world);
                bob.setPos(p.getX() + rand.nextGaussian() * 10, 300, p.getZ() + rand.nextGaussian() * 10);
                bob.payload = stack.copy();
                world.addFreshEntity(bob);
            } else {
                p.sendSystemMessage(Component.literal("[BOBMAZON] Not enough caps!"));
            }
        } else {
            p.sendSystemMessage(Component.literal("[BOBMAZON] Achievement requirement not met!"));
        }
    }

    private static boolean isCap(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String namespace = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        if (!Constants.MOD_ID.equals(namespace)) return false;
        for (String cap : CAPS) if (cap.equals(path)) return true;
        return false;
    }

    public static int countCaps(Player player) {
        int count = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isCap(stack)) count += stack.getCount();
        }
        return count;
    }

    /** 1.7 {@code payCaps}: one at a time from the first cap stacks found, stopping at the price. */
    private static void payCaps(Player player, int price) {
        if (price == 0) return;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!isCap(stack)) continue;
            int size = stack.getCount();
            for (int j = 0; j < size; j++) {
                inventory.removeItem(i, 1);
                price--;
                if (price == 0) return;
            }
        }
    }
}
