package com.hbm.items.weapon;

import com.hbm.items.IEquipReceiver;
import com.hbm.items.tool.ItemSwordAbility;
import dev.golem.core.world.SwingReceiver;
import dev.golem.core.world.ToolTier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * old-hbm {@code ItemSwordCutter}: a sword that cuts mobs in half along a plane the player draws.
 *
 * <p>Hold right click and drag the view: the plane runs through the eye, the look direction at the click and the look
 * direction now (a black guide line is drawn between the two). Swing while still holding it and the client sends the
 * plane ({@code PacketMobSlicer}, here {@link com.hbm.packet.MobSlicerPayload}); the server cuts every living thing
 * the eleven rays between the two look directions hit within three blocks ({@link MobSlicer}).
 *
 * <p>All of that input is client state, as in old-hbm ("All of this stuff is client side, only for first person
 * rendering"); it lives in {@code com.hbm.client.crucible.CrucibleClient}, which this class reaches through
 * {@link #client}, set only on a physical client.
 */
public class ItemSwordCutter extends ItemSwordAbility implements IEquipReceiver, SwingReceiver {

    /** The client half (old {@code @SideOnly(CLIENT)} methods); null on a dedicated server. */
    public static @Nullable Client client;

    public interface Client {
        /** {@code rightClickClient}: start a cut at the current look direction. */
        void rightClick(Player player);

        /** {@code swingClient}: send a drawn cut and play the cutting swing. */
        void swing(LivingEntity entity, ItemStack stack);

        /** {@code HbmAnimations.getRelevantAnim(MAIN_HAND).animation != null}: a bus animation is playing. */
        boolean busAnimationPlaying(Player player);

        /** A cut is being drawn ({@code clicked}): the left click that confirms it must not also attack. */
        boolean cutting(Player player);
    }

    /**
     * Server half of {@code onLeftClickEntity}'s "an animation is playing" test, per player: until when the swing
     * animation the server last started keeps an entity click from attacking. 1.12 asked the client's animation table
     * from the server thread too, which only works in single player; this is the same window measured where the server
     * started it.
     */
    private static final Map<UUID, Long> BUS_UNTIL = new ConcurrentHashMap<>();

    /**
     * When each player's last cut was accepted. 1.12 took every {@code PacketMobSlicer}; a cut takes a right click, a
     * drag and a swing, so no honest client sends two within {@value #MIN_CUT_MILLIS} ms.
     */
    private static final Map<UUID, Long> LAST_CUT = new ConcurrentHashMap<>();
    static final long MIN_CUT_MILLIS = 250L;

    public ItemSwordCutter(Properties properties, float damage, double movement, ToolTier material) {
        super(properties, damage, movement, material);
    }

    protected ItemSwordCutter(Properties properties, float damage, float attackSpeed, double movement, ToolTier material) {
        super(properties, damage, attackSpeed, movement, material);
    }

    /** The one attack interceptor ({@code onLeftClickEntity}) for every cutter. */
    public static void init() {
        dev.golem.core.world.Attacks.interceptPlayerAttack(ItemSwordCutter::onLeftClickEntity);
        dev.golem.core.platform.Lifecycle.onServerTick(ItemSwordCutter::forgetAbsentPlayers);
    }

    /**
     * Drops the per-player cutter state of everyone who is no longer online, so a logout clears it (by the next server
     * tick) and a relog starts fresh, as a new 1.12 client did.
     */
    private static void forgetAbsentPlayers(net.minecraft.server.MinecraftServer server) {
        if (BUS_UNTIL.isEmpty() && LAST_CUT.isEmpty() && !ItemCrucible.hasPlayerState()) return;
        java.util.function.Predicate<UUID> absent = id -> server.getPlayerList().getPlayer(id) == null;
        BUS_UNTIL.keySet().removeIf(absent);
        LAST_CUT.keySet().removeIf(absent);
        ItemCrucible.forgetPlayers(absent);
    }

    /** Whether a cut from {@code player} may be taken now; records it if so. */
    static boolean acceptCut(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = LAST_CUT.get(player.getUUID());
        if (last != null && now - last < MIN_CUT_MILLIS && now >= last) return false;
        LAST_CUT.put(player.getUUID(), now);
        return true;
    }

    /**
     * {@code onLeftClickEntity}: while a main-hand bus animation plays, a click on an entity does not attack.
     *
     * <p>Port addition (user decision, 2026-10-02): nor does the left click that confirms a cut. 1.12 let it attack the
     * mob under the crosshair (the hit was free through {@code doSpecialClick}, and at 500 damage it killed before the
     * cut arrived). The client never sends that attack ({@code CrucibleClient} hands the click to Core
     * {@code AttackInput}, which only swings); this is the server's guard: a crucible whose holder reports a cut being
     * drawn or its cutting swing ({@link ItemCrucible#doSpecialClick}) takes no attack.
     */
    public static boolean onLeftClickEntity(Player player, Entity target) {
        if (!(player.getMainHandItem().getItem() instanceof ItemSwordCutter)) return false;
        if (player.level().isClientSide()) {
            return client != null && (client.cutting(player) || client.busAnimationPlaying(player));
        }
        if (ItemCrucible.doSpecialClick(player)) return true;
        Long until = BUS_UNTIL.get(player.getUUID());
        return until != null && System.currentTimeMillis() < until;
    }

    /** Records that the server started a bus animation of {@code durationMillis} on this player's main hand. */
    protected static void busStarted(ServerPlayer player, int durationMillis) {
        BUS_UNTIL.put(player.getUUID(), System.currentTimeMillis() + durationMillis);
    }

    /** Whether the server's record of this player's main-hand bus animation still runs. */
    protected static boolean busRunning(ServerPlayer player) {
        Long until = BUS_UNTIL.get(player.getUUID());
        return until != null && System.currentTimeMillis() < until;
    }

    /** {@code onItemRightClick}: the client starts a cut; the sword itself does nothing else on use. */
    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() && client != null) client.rightClick(player);
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** {@code onEntitySwing}: on the client, send a drawn cut. Never cancels by itself. */
    @Override
    public boolean compatOnEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (entity.level().isClientSide() && client != null) client.swing(entity, stack);
        return false;
    }

    /** {@code getTexId}: which cap texture and damage the cut uses (0 the plain slicer, 1 the crucible). */
    public byte getTexId() {
        return 0;
    }

    /** {@code onEquip}: the deploy animation (the Collada equip) on the holder's client. */
    @Override
    public void onEquip(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer server) {
            com.hbm.packet.CrucibleAnimPayload.send(server, com.hbm.packet.CrucibleAnimPayload.EQUIP);
        }
    }
}
