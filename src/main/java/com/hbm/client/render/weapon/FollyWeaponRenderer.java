package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.sound.ModGunSounds;
import com.hbm.util.EntityDamageUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** GEM port of ItemRenderFolly's cannon hierarchy, and the boot screen and target readout in its sight. */
public final class FollyWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("moonlight");
    private static final int FULL_BRIGHT = 0x00F000F0;

    private static long timeAiming;
    private static boolean jingle;
    private static boolean wasAiming;

    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("folly");
        return model;
    }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2F : 2.5F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        float aim = 0.75F;
        standardAimingTransform(stack, pose, -2.5F * offset, -1.5F * offset, 2.75F * offset,
                -2 * aim, -1 * aim, 2.25F * offset);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.all(), BODY).visibleIf(state -> !state.animated);
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.75F)
                .translate(0, 1, -4)
                .angle(Gem.Axes.X, state -> (float) -state.bus("EQUIP")[0])
                .translate(0, -1, 4)
                .translate(0, -2, -2)
                .angle(Gem.Axes.X, state -> (float) state.bus("LOAD")[0])
                .translate(0, 2, 2);
        root.child(obj.part("Cannon"), BODY);
        root.child(obj.part("Barrel"), BODY)
                .slide(Gem.Axes.X, state -> (float) state.bus("RECOIL")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RECOIL")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2]);
        root.child(obj.part("Shell"), BODY)
                .slide(Gem.Axes.X, state -> (float) state.bus("SHELL")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("SHELL")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("SHELL")[2]);
        Gem.Part<WeaponPose> breech = root.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("BREECH")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("BREECH")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("BREECH")[2]);
        breech.child(obj.part("Breech"), BODY);
        breech.child(obj.part("Cog"), BODY)
                .translate(0, 1, 0)
                .angle(Gem.Axes.Z, state -> (float) state.bus("SCREW")[2])
                .translate(0, -1, 0);
    }

    @Override
    protected void overlays(WeaponPose state, PoseStack pose, SubmitSink sink) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!state.animated || player == null) return;

        boolean isAiming = fullyAimed();
        if (isAiming && !wasAiming) timeAiming = System.currentTimeMillis();
        wasAiming = isAiming;
        if (!isAiming) {
            jingle = false;
            return;
        }

        Component splash = bootSplash();
        if (!jingle && splash != null) {
            player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), ModGunSounds.FIRE_VSTAR.get(),
                    SoundSource.PLAYERS, 0.5F, 1F, false);
            jingle = true;
        }

        Font font = Minecraft.getInstance().font;
        float variance = 0.85F + player.getRandom().nextFloat() * 0.15F;
        int color = 0xFF000000 | Math.round(variance * 255F) << 16 | Math.round(variance * 0.5F * 255F) << 8;

        pose.pushPose();
        pose.scale(0.75F, 0.75F, 0.75F);
        pose.translate(0F, 1F, -4F);
        pose.mulPose(Axis.XP.rotationDegrees((float) -state.bus("EQUIP")[0]));
        pose.translate(0F, -1F, 4F);
        pose.translate(0F, -2F, -2F);
        pose.mulPose(Axis.XP.rotationDegrees((float) state.bus("LOAD")[0]));
        pose.translate(0F, 2F, 2F);

        if (System.currentTimeMillis() - timeAiming > 5000L && state.bus("LOAD")[0] == 0D) {
            int ammo = ((ItemGunBaseNT) state.stack.getItem()).getConfig(state.stack, 0).getReceivers(state.stack)[0]
                    .getMagazine(state.stack).getAmount(state.stack, player.getInventory());
            centred(pose, sink, font, Component.literal(ammo > 0 ? "+" : "No ammo"), 0.01F, color);
        }
        if (splash != null) centred(pose, sink, font, splash, 0.02F, color);

        List<Component> tty = tty(player);
        pose.pushPose();
        float size = 0.005F;
        pose.translate(2.5F, 1.375F, -2.75F);
        pose.scale(size, -size, size);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        for (int i = 0; i < tty.size(); i++) {
            sink.submitText(pose, font, 0F, i * (font.lineHeight + 2F), tty.get(i).getVisualOrderText(), false,
                    Font.DisplayMode.NORMAL, FULL_BRIGHT, color, 0);
        }
        pose.popPose();
        pose.popPose();
    }

    private static void centred(PoseStack pose, SubmitSink sink, Font font, Component text, float size, int color) {
        pose.pushPose();
        pose.translate((font.width(text) / 2) * size + 2F, 1F + font.lineHeight * size / 2F, -2.75F);
        pose.scale(size, -size, size);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        sink.submitText(pose, font, 0F, 0F, text.getVisualOrderText(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT,
                color, 0);
        pose.popPose();
    }

    /** 1.7 {@code getBootSplash}: a highlight sweeps over the name from 3 to 5 seconds of aiming; null otherwise. */
    private static Component bootSplash() {
        long now = System.currentTimeMillis();
        if (timeAiming + 5000L < now || timeAiming + 3000L > now) return null;
        int splashIndex = (int) ((now - timeAiming - 3000L) * 35L / 2000L) - 10;
        String letters = "VStarOS";
        MutableComponent splash = Component.empty();
        for (int i = 0; i < letters.length(); i++) {
            ChatFormatting format = i < splashIndex - 1 ? ChatFormatting.LIGHT_PURPLE
                    : i == splashIndex - 1 ? ChatFormatting.AQUA
                    : i == splashIndex ? ChatFormatting.WHITE
                    : i == splashIndex + 1 ? ChatFormatting.AQUA
                    : i == splashIndex + 2 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.BLACK;
            splash.append(Component.literal(letters.substring(i, i + 1)).withStyle(format));
        }
        return splash;
    }

    /** 1.7 {@code getTTY}: the boot log for 3 seconds, the target readout after 5. */
    private static List<Component> tty(LocalPlayer player) {
        List<String> lines = new ArrayList<>();
        int time = (int) (System.currentTimeMillis() - timeAiming);
        if (time < 3000) {
            if (time > 250) lines.add("POST successful - Code 0");
            if (time > 500) lines.add("8,388,608 bytes of RAM installed");
            if (time > 500) lines.add("5,187,427 bytes available");
            if (time > 750) lines.add("Reticulating splines...");
            if (time > 1500) lines.add("No keyboard found!");
            if (time > 2000) lines.add("Booting from /dev/sda1...");
        }
        if (time > 5000) {
            HitResult hit = EntityDamageUtil.getMouseOver(player, 250D);
            String target = "Target: ";
            if (hit == null || hit.getType() == HitResult.Type.MISS) target += "N/A";
            else if (hit instanceof BlockHitResult block) {
                target += block.getBlockPos().getX() + "/" + block.getBlockPos().getY() + "/" + block.getBlockPos().getZ();
            } else if (hit instanceof EntityHitResult entity) target += entity.getEntity().getName().getString();
            lines.add(target);
            lines.add("Angle: " + (int) (-player.getXRot() * 100F) / 100D);
        }
        List<Component> tty = new ArrayList<>();
        for (String line : lines) tty.add(Component.literal(line).withStyle(ChatFormatting.GREEN));
        return tty;
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, -0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(3F, 3F, 3F);
        pose.translate(-0.25F, 0.5F, 3F);
    }
}
