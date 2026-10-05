package com.hbm.compat.happyghast;

import com.hbm.Constants;
import com.hbm.blocks.bomb.NukeMan;
import com.hbm.tileentity.bomb.TileEntityNukeMan;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A happy ghast carrying a Fat Man, as it carries a leashed boat ({@link EntityRiggedFatMan}). Happy ghasts exist only
 * on the 26.1 nodes, so this is its own source root, {@code src/happyghast} (plus the loader entry points in
 * {@code src/happyghast-neoforge} and {@code src/happyghast-fabric}), added by the build only on nodes that set
 * {@code feature.happy_ghast}. The shared tree names none of it; its one part is {@link NukeMan#detonate}.
 *
 * <p>The loader roots call {@link #init()} before registries flush (NeoForge at mod construction, Fabric from a
 * {@code main} entrypoint ordered before HBM's), forward block use to {@link #tryRig}, and register the renderer.
 */
public final class HbmHappyGhast {

    private HbmHappyGhast() {}

    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.createEntityTypes(Constants.MOD_ID);

    public static final RegistryObject<EntityType<EntityRiggedFatMan>> RIGGED_FAT_MAN =
            ENTITY_TYPES.register("rigged_fat_man", key -> EntityType.Builder
                    .<EntityRiggedFatMan>of(EntityRiggedFatMan::new, MobCategory.MISC)
                    .noLootTable()
                    // the block's footprint about its centre; the fins reach past it, as on the block
                    .sized(1.6F, 1.6F)
                    // a boat's tracking range, two chunks further: it is meant to be watched from below
                    .clientTrackingRange(12)
                    .build(key));

    private static boolean initialised;

    /** Creates the registers (entity type, GameTest bodies). Idempotent; must run before the registry flush. */
    public static synchronized void init() {
        if (initialised) return;
        initialised = true;
        GameTests.Suite unused = HappyGhastGameTests.TESTS;
        Constants.LOG.info("Happy ghasts present: a ready Fat Man can be rigged for carrying");
    }

    /** The {@code hbm:happy_ghast_*} GameTests. */
    public static GameTests.Suite gameTests() {
        return HappyGhastGameTests.TESTS;
    }

    /**
     * Sneak + right-click with a lead on a ready Fat Man lifts it off its block as an {@link EntityRiggedFatMan} already
     * tied to the player, the lead used up as on any mob (kept in creative). Called by the loaders' block-use hooks
     * before the block or the lead's own use (a sneaking player with an item would otherwise skip the block). {@code null}
     * when this is not such a use, and the use goes on; the client always goes on, so the server sees the click.
     */
    public static @Nullable InteractionResult tryRig(Player player, Level level, InteractionHand hand, BlockPos pos) {
        if (level.isClientSide() || !player.isSecondaryUseActive() || player.isSpectator()) return null;
        ItemStack lead = player.getItemInHand(hand);
        if (!lead.is(Items.LEAD)) return null;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NukeMan)) return null;
        if (!(level.getBlockEntity(pos) instanceof TileEntityNukeMan nuke)) return null;
        if (!level.mayInteract(player, pos)) return null;

        if (!nuke.isReady()) {
            player.sendOverlayMessage(Component.translatable("hbm.rigged_fat_man.not_ready"));
            return InteractionResult.FAIL;
        }
        EntityRiggedFatMan bomb = rig(level, pos, state, nuke);
        bomb.setLeashedTo(player, true);
        level.playSound(null, pos, SoundEvents.LEAD_TIED, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (!player.hasInfiniteMaterials()) lead.shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Swaps the block for the entity in place: same spot, same model yaw, same parts and placer. */
    static EntityRiggedFatMan rig(Level level, BlockPos pos, BlockState state, TileEntityNukeMan nuke) {
        List<ItemStack> parts = new ArrayList<>(EntityRiggedFatMan.SLOTS);
        for (int i = 0; i < EntityRiggedFatMan.SLOTS; i++) parts.add(nuke.getItem(i).copy());
        UUID placer = nuke.placerID;
        float yaw = EntityRiggedFatMan.yawForFacing(state.getValue(NukeMan.FACING));

        // as NukeMan.explode: empty it first so removing the block drops nothing
        nuke.clearSlots();
        level.removeBlock(pos, false);

        EntityRiggedFatMan bomb = new EntityRiggedFatMan(level, parts, placer, yaw);
        bomb.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(bomb);
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return bomb;
    }
}
