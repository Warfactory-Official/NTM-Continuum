package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.config.ServerConfig;
import com.hbm.items.armor.IPAMelee;
import com.hbm.items.armor.IPARanged;
import com.hbm.items.armor.IPAWeaponsProvider;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class XFactoryPA {

    public static final ConfigWrapper<Float> MELEE_RPA_SWINGS_DAMAGE = new ConfigWrapper<>(15F);
    public static final ConfigWrapper<Float> MELEE_RPA_SLAP_DAMAGE = new ConfigWrapper<>(35F);
    public static final ConfigWrapper<Float> MELEE_RPA_LARGE_MULT = new ConfigWrapper<>(2.5F);
    public static final ConfigWrapper<Float> MELEE_NCRPA_SWINGS_DAMAGE = new ConfigWrapper<>(15F);
    public static final ConfigWrapper<Float> MELEE_NCRPA_SWEEP_DAMAGE = new ConfigWrapper<>(35F);
    public static final ConfigWrapper<Float> MELEE_NCRPA_LARGE_MULT = new ConfigWrapper<>(2.5F);
    public static final ConfigWrapper<Float> RANGED_NCRPA_DAMAGE = new ConfigWrapper<>(25F);

    public static void initConfig() {
        ServerConfig.configMap.put("MELEE_RPA_SWINGS_DAMAGE", MELEE_RPA_SWINGS_DAMAGE);
        ServerConfig.configMap.put("MELEE_RPA_SLAP_DAMAGE", MELEE_RPA_SLAP_DAMAGE);
        ServerConfig.configMap.put("MELEE_RPA_LARGE_MULT", MELEE_RPA_LARGE_MULT);
        ServerConfig.configMap.put("MELEE_NCRPA_SWINGS_DAMAGE", MELEE_NCRPA_SWINGS_DAMAGE);
        ServerConfig.configMap.put("MELEE_NCRPA_SWEEP_DAMAGE", MELEE_NCRPA_SWEEP_DAMAGE);
        ServerConfig.configMap.put("MELEE_NCRPA_LARGE_MULT", MELEE_NCRPA_LARGE_MULT);
        ServerConfig.configMap.put("RANGED_NCRPA_DAMAGE", RANGED_NCRPA_DAMAGE);
    }

    public static final BiConsumer<ItemStack, LambdaContext> ORCHESTRA = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.orchestra(stack, ctx);
    };

    public static final BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MELEE_ANIMS = (stack, type) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentClient();
        return component == null ? null : component.playAnim(stack, type);
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_PRIMARY = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.clickPrimary(stack, ctx);
    };
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_SENONDARY = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.clickSecondary(stack, ctx);
    };
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_PRIMARY = (stack, ctx) -> {
        IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
        if (component != null) component.clickPrimary(stack, ctx);
    };
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_SENONDARY = (stack, ctx) -> {
        IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
        if (component != null) component.clickSecondary(stack, ctx);
    };

    private XFactoryPA() {}

    public static void init() {
        NtmToolGunCatalog.gun_pa_melee = NtmToolGunCatalog.weapon("gun_pa_melee",
                props -> new ItemGunPA(props, WeaponQuality.UTILITY, new GunConfig()
                        .draw(10).crosshair(Crosshair.NONE).rec(new Receiver(0))
                        .pp(LAMBDA_CLICK_MELEE_PRIMARY).ps(LAMBDA_CLICK_MELEE_SENONDARY)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                        .anim(LAMBDA_MELEE_ANIMS).orchestra(ORCHESTRA)));
        NtmToolGunCatalog.gun_pa_ranged = NtmToolGunCatalog.weapon("gun_pa_ranged",
                props -> new ItemGunPA(props, WeaponQuality.UTILITY, new GunConfig()
                        .draw(0).crosshair(Crosshair.CROSS).rec(new Receiver(0))
                        .pp(LAMBDA_CLICK_RANGED_PRIMARY).ps(LAMBDA_CLICK_RANGED_SENONDARY)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)));
    }

    public static void doSwing(ItemStack stack, LambdaContext ctx, GunAnimation anim, int cooldown) {
        Player player = ctx.getPlayer();
        if (player == null) return;
        int index = ctx.configIndex;
        if (ItemGunBaseNT.getState(stack, index) == GunState.IDLE) {
            ItemGunBaseNT.playAnimation(player, stack, anim, index);
            ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
            ItemGunBaseNT.setTimer(stack, index, cooldown);
        }
    }

    public static class ItemGunPA extends ItemGunBaseNT {
        public ItemGunPA(Properties properties, WeaponQuality quality, GunConfig... cfg) {
            super(properties, quality, cfg);
        }

        @Override
        protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                List<Component> tooltip, TooltipFlag flag) {
        }
    }
}
