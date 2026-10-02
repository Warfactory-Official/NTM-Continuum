package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.item.IGasMask;
import com.hbm.entity.effect.EntityMist;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Toxin;
import com.hbm.items.ModGearItems;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The 1.7 {@code FT_Toxin}: phosgene clouds hurt at 1.7's rate, the right gas-mask filter stops it (and
 * wears), and the Shift tooltip carries 1.7's lines.
 */
public final class ToxinGameTests {

    private ToxinGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function MIST_DAMAGE =
            TESTS.function("toxin_mist_phosgene_damage", 80, 8, () -> ToxinGameTests::mistDamage);
    public static final GameTests.Function MASK =
            TESTS.function("toxin_gas_mask_protection", 80, () -> ToxinGameTests::mask);
    public static final GameTests.Function TOOLTIP =
            TESTS.function("toxin_tooltip", 20, () -> ToxinGameTests::tooltip);

    public static void init() {}

    /**
     * A pig standing in a phosgene cloud. 1.7: {@code ToxinDirectDamage(cloud, 4F, 20, GAS_LUNG)} hits
     * for {@code 4 * intensity} only on ticks where {@code worldTime % 20 == 0}. The cloud is ticked by hand
     * with its age pinned so that, after the tick's own increment, it is 30 of 150:
     * intensity = 1 - 30/150 = 0.8, so each hit is exactly 3.2 and a hit takes the pig from 10 to 6.8.
     */
    public static void mistDamage(GameTestHelper helper) {
        // Floating in open air with gravity off: nothing but the cloud may hurt it. Health and invulnerability
        // frames are reset before every hand tick, so each measured delta is that one cloud tick alone (a stray
        // earlier hit would otherwise shave the cloud's hit down to hit - lastHurt).
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 3, 1));
        pig.setNoGravity(true);
        pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        EntityMist mist = new EntityMist(helper.getLevel());
        mist.setType(Fluids.PHOSGENE);
        mist.setArea(4F, 3F);
        mist.setDuration(150);
        mist.setPos(pig.getX(), pig.getY(), pig.getZ());

        int[] hits = {0};
        for (int delay = 1; delay <= 25; delay++) {
            helper.runAfterDelay(delay, () -> {
                // A hit with no attacker still knocks back 0.4 in a random direction (26.1), and over 20 ticks that
                // drifted the pig out of the 4-wide cloud before the next boundary in 4 of 11 full runs (2026-10-01).
                // Put it back in the middle each tick: what is measured is the cloud's hit, not the pig's drift.
                pig.setPos(mist.getX(), mist.getY(), mist.getZ());
                pig.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                pig.setHealth(pig.getMaxHealth());
                pig.invulnerableTime = 0;
                long time = helper.getLevel().getGameTime();
                mist.tickCount = 29;
                mist.tick();
                float after = pig.getHealth();
                if (time % 20 == 0) {
                    helper.assertTrue(Math.abs(after - 6.8F) < 1.0E-4F,
                            "phosgene must hit a pig at 10 for 4 * 0.8 = 3.2 on a 20-tick boundary, leaving 6.8; has "
                                    + after + " at time " + time);
                    hits[0]++;
                } else {
                    helper.assertTrue(after == pig.getMaxHealth(),
                            "phosgene must not hit off its 20-tick clock; has " + after + " at time " + time);
                }
            });
        }
        helper.runAfterDelay(26, () -> {
            helper.assertTrue(hits[0] >= 1, "25 ticks must cross at least one 20-tick boundary");
            helper.succeed();
        });
    }

    /**
     * Mock players on a 20-tick boundary, phosgene at full intensity: bare-faced takes 4; a gas mask with
     * the standard filter (GAS_LUNG) takes nothing and its filter wears one point; the same mask with the
     * monoxide filter (no GAS_LUNG) takes 4.
     */
    public static void mask(GameTestHelper helper) {
        FT_Toxin toxin = Fluids.PHOSGENE.getTrait(FT_Toxin.class);
        helper.assertTrue(toxin != null, "phosgene must carry FT_Toxin");

        Player bare = player(helper);
        Player masked = player(helper);
        ItemStack mask = maskWith(ModGearItems.gas_mask_filter.get());
        masked.setItemSlot(EquipmentSlot.HEAD, mask);
        Player wrong = player(helper);
        wrong.setItemSlot(EquipmentSlot.HEAD, maskWith(ModGearItems.gas_mask_filter_mono.get()));

        boolean[] done = {false};
        for (int delay = 1; delay <= 25; delay++) {
            helper.runAfterDelay(delay, () -> {
                if (done[0] || helper.getLevel().getGameTime() % 20 != 0) return;
                done[0] = true;
                int filterBefore = ((IGasMask) mask.getItem()).getFilter(mask).getDamageValue();

                toxin.affect(bare, 1D);
                toxin.affect(masked, 1D);
                toxin.affect(wrong, 1D);

                helper.assertTrue(Math.abs(bare.getHealth() - 16F) < 1.0E-4F,
                        "an unmasked player must take 4, has " + bare.getHealth());
                helper.assertTrue(masked.getHealth() == 20F,
                        "a gas mask with a GAS_LUNG filter must take none, has " + masked.getHealth());
                int filterAfter = ((IGasMask) mask.getItem()).getFilter(mask).getDamageValue();
                helper.assertTrue(filterAfter == filterBefore + 1,
                        "the filter must wear one point per protected hit: " + filterBefore + " -> " + filterAfter);
                helper.assertTrue(Math.abs(wrong.getHealth() - 16F) < 1.0E-4F,
                        "a monoxide filter does not stop phosgene, has " + wrong.getHealth());
                helper.succeed();
            });
        }
    }

    /** 1.7's Shift lines, with translation keys shown as {key} so the check is language-free. */
    public static void tooltip(GameTestHelper helper) {
        helper.assertTrue(Fluids.CHLORINE.getTrait(FT_Toxin.class) != null
                && Fluids.REDMUD.getTrait(FT_Toxin.class) != null, "chlorine and red mud carry FT_Toxin");

        List<String> phosgene = lines(Fluids.PHOSGENE, true);
        expect(helper, phosgene, "[{hbmfluid.trait.toxin}]");
        expect(helper, phosgene, "- {hazard.gasChlorine}: 4.0 {hbmfluid.trait.perDamage}");

        List<String> hidden = lines(Fluids.PHOSGENE, false);
        helper.assertTrue(hidden.stream().noneMatch(s -> s.contains("hbmfluid.trait.toxin")),
                "toxin lines are Shift-only: " + hidden);
        helper.assertTrue(hidden.stream().anyMatch(s -> s.contains("LSHIFT")), "the Shift hint must show: " + hidden);

        List<String> mustard = lines(Fluids.MUSTARDGAS, true);
        expect(helper, mustard, "- {hazard.corrosive}: 8.0 {hbmfluid.trait.perDamage}");
        expect(helper, mustard, "- {hazard.corrosive} ({hbmfluid.trait.hazmat}):");
        expect(helper, mustard, "   - {effect.minecraft.wither} {potion.potency.1} 0:05");
        expect(helper, mustard, "   - {effect.minecraft.nausea} 0:05");

        expect(helper, lines(Fluids.ESTRADIOL, true), "   - {effect.hbm.death} 60:00");
        expect(helper, lines(Fluids.REDMUD, true), "   - {effect.minecraft.wither} {potion.potency.2} 0:30");
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, List<String> lines, String line) {
        helper.assertTrue(lines.contains(line), "tooltip must contain \"" + line + "\": " + lines);
    }

    private static List<String> lines(FluidType type, boolean shift) {
        List<Component> info = new ArrayList<>();
        type.addInfo(info, shift);
        List<String> out = new ArrayList<>();
        for (Component c : info) {
            StringBuilder sb = new StringBuilder();
            flatten(c, sb);
            out.add(sb.toString());
        }
        return out;
    }

    private static void flatten(Component c, StringBuilder sb) {
        if (c.getContents() instanceof TranslatableContents t) {
            sb.append('{').append(t.getKey()).append('}');
        } else {
            c.getContents().visit(s -> {
                sb.append(s);
                return Optional.empty();
            });
        }
        for (Component sibling : c.getSiblings()) flatten(sibling, sb);
    }

    private static Player player(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        return player;
    }

    private static ItemStack maskWith(Item filter) {
        ItemStack mask = new ItemStack(ModGearItems.gas_mask.get());
        ((IGasMask) mask.getItem()).installFilter(mask, new ItemStack(filter));
        return mask;
    }
}
