package com.hbm.client.anim;

import com.hbm.items.weapon.sedna.GunAnimation;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import dev.golem.core.render.anim.BusAnimation;
import dev.golem.core.render.anim.BusAnimationKeyframe.IType;
import dev.golem.core.render.anim.BusAnimationSequence;
import net.minecraft.world.item.ItemStack;

/** Exact hand-authored animation tables for the ordinary Gen-3 firearm continuation. */
public final class SednaGen3Animations {

    private SednaGen3Animations() {}

    public static BusAnimation dani(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP", seq().addPos(1080, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                    .addBus("HAMMER", seq().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300).addPos(0, 0, 0, 200))
                    .addBus("DRUM", seq().addPos(0, 0, 0, 350).addPos(0, 0, 1, 200));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("HAMMER", seq().addPos(0, 0, 1, 50).addPos(0, 0, 1, 200).addPos(0, 0, 0, 200))
                    .addBus("DRUM", seq().addPos(0, 0, 0, 350).addPos(0, 0, 1, 200));
            default -> atlas(type);
        };
    }

    public static BusAnimation pepperbox(ItemStack stack, GunAnimation type, int rail) {
        return switch (type) {
            case CYCLE -> new BusAnimation()
                    .addBus("ROTATE", seq().addPos(0, 0, 0, 1025).addPos(60, 0, 0, 250))
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 50).addPos(45, 0, 0, 150, IType.SIN_DOWN)
                            .addPos(45, 0, 0, 50).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("HAMMER", seq().addPos(80, 0, 0, 25).addPos(80, 0, 0, 1000).addPos(0, 0, 0, 250))
                    .addBus("TRIGGER", seq().addPos(1, 0, 0, 25).addPos(1, 0, 0, 250).addPos(0, 0, 0, 100));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("ROTATE", seq().addPos(0, 0, 0, 525).addPos(60, 0, 0, 250))
                    .addBus("HAMMER", seq().addPos(80, 0, 0, 25).addPos(80, 0, 0, 500).addPos(0, 0, 0, 250))
                    .addBus("TRIGGER", seq().addPos(1, 0, 0, 25).addPos(1, 0, 0, 250).addPos(0, 0, 0, 100));
            case EQUIP -> new BusAnimation().addBus("RECOIL",
                    seq().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 200, IType.SIN_DOWN));
            case RELOAD -> new BusAnimation()
                    .addBus("RECOIL", seq().addPos(90, 0, 0, 500, IType.SIN_FULL).addPos(90, 0, 0, 1600)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL).addPos(-5, 0, 0, 200, IType.SIN_UP)
                            .addPos(0, 0, 0, 200, IType.SIN_DOWN))
                    .addBus("TRANSLATE", seq().addPos(0, -12, 5, 500, IType.SIN_FULL).addPos(0, -12, 5, 700)
                            .addPos(0, -13, 5, 200).addPos(0, -12, 5, 200).addPos(0, -12, 5, 500)
                            .addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("LOADER", seq().addPos(0, 0, 0, 500).addPos(0, 5, -5, 0)
                            .addPos(0, 0, -0.1, 500, IType.SIN_FULL).addPos(0, 0, -1, 200)
                            .addPos(0, 0, -1, 200).addPos(0, 0, -0.1, 200)
                            .addPos(0, 5, -5, 500, IType.SIN_FULL).addPos(0, 0, 0, 0))
                    .addBus("ROTATE", seq().addPos(0, 0, 0, 2600).addPos(-360, 0, 0, 750, IType.SIN_FULL))
                    .addBus("SHOT", seq().addPos(1, 0, 0, 1400).addPos(0, 0, 0, 0));
            case INSPECT -> new BusAnimation()
                    .addBus("ROTATE", seq().addPos(-360, 0, 0, 750, IType.SIN_FULL))
                    .addBus("RECOIL", seq().addPos(-5, 0, 0, 200, IType.SIN_UP)
                            .addPos(0, 0, 0, 200, IType.SIN_DOWN));
            case JAMMED -> new BusAnimation()
                    .addBus("ROTATE", seq().addPos(0, 0, 0, 1300).addPos(60, 0, 0, 500, IType.SIN_FULL)
                            .addPos(60, 0, 0, 400).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("TRANSLATE", seq().addPos(0, 0, 0, 500).addPos(0, -6, 0, 400, IType.SIN_FULL)
                            .addPos(0, -6, 0, 2000).addPos(0, 0, 0, 400, IType.SIN_FULL))
                    .addBus("RECOIL", seq().addPos(0, 0, 0, 500).addPos(45, 0, 0, 400, IType.SIN_FULL)
                            .addPos(45, 0, 0, 2000).addPos(0, 0, 0, 400, IType.SIN_FULL));
            default -> null;
        };
    }

    public static BusAnimation g3(ItemStack stack, GunAnimation type, int rail) {
        boolean empty = loaded(stack, rail) <= 0;
        return switch (type) {
            case EQUIP -> new BusAnimation().addBus("EQUIP",
                    seq().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimation().addBus("RECOIL", seq().addPos(0, 0,
                    ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -0.75, 25, IType.SIN_DOWN)
                    .addPos(0, 0, 0, 75, IType.SIN_FULL));
            case CYCLE_DRY -> new BusAnimation()
                    .addBus("BOLT", seq().addPos(0, 0, 0, 250).addPos(0, 0, -3.25, 150).addPos(0, 0, 0, 100))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 400).addPos(-1, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL));
            case RELOAD -> new BusAnimation()
                    .addBus("MAG", seq().addPos(0, -8, 0, 250, IType.SIN_UP).addPos(0, -8, 0, 1000)
                            .addPos(0, 0, 0, 300))
                    .addBus("BOLT", seq().addPos(0, 0, 0, 250).addPos(0, 0, -3.25, 150)
                            .addPos(0, 0, -3.25, 1250).addPos(0, 0, 0, 100))
                    .addBus("HANDLE", seq().addPos(0, 0, 0, 500).addPos(0, 0, 45, 50)
                            .addPos(0, 0, 45, 1150).addPos(0, 0, 0, 50))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 750).addPos(-25, 0, 0, 500, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 750).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("BULLET", seq().addPos(empty ? 1 : 0, 0, 0, 0).addPos(0, 0, 0, 1000));
            case INSPECT -> new BusAnimation()
                    .addBus("MAG", seq().addPos(0, -1, 0, 150).addPos(2, -1, 0, 150)
                            .addPos(2, 8, 0, 350, IType.SIN_DOWN).addPos(2, -2, 0, 350, IType.SIN_UP)
                            .addPos(2, -1, 0, 50).addPos(2, -1, 0, 100)
                            .addPos(0, -1, 0, 150, IType.SIN_FULL).addPos(0, 0, 0, 150, IType.SIN_UP))
                    .addBus("SPEEN", seq().addPos(0, 0, 0, 300).addPos(0, 360, 360, 700))
                    .addBus("LIFT", seq().addPos(0, 0, 0, 1450).addPos(-2, 0, 0, 100, IType.SIN_DOWN)
                            .addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("BULLET", seq().addPos(empty ? 1 : 0, 0, 0, 0));
            case JAMMED -> new BusAnimation()
                    .addBus("LIFT", seq().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL)
                            .addPos(-25, 0, 0, 1250).addPos(0, 0, 0, 350, IType.SIN_FULL))
                    .addBus("BOLT", seq().addPos(0, 0, 0, 1000).addPos(0, 0, -3.25, 150)
                            .addPos(0, 0, 0, 100).addPos(0, 0, 0, 250)
                            .addPos(0, 0, -3.25, 150).addPos(0, 0, 0, 100));
            default -> null;
        };
    }

    private static BusAnimation atlas(GunAnimation type) {
        return switch (type) {
            case RELOAD -> new BusAnimation()
                    .addBus("LATCH", seq().addPos(0, 0, 90, 300).addPos(0, 0, 90, 2000).addPos(0, 0, 0, 150))
                    .addBus("FRONT", seq().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150)
                            .addPos(0, 0, 45, 2000).addPos(0, 0, 0, 75))
                    .addBus("RELOAD_ROT", seq().addPos(0, 0, 0, 300).addPos(60, 0, 0, 500)
                            .addPos(60, 0, 0, 500).addPos(0, -90, -90, 0).addPos(0, -90, -90, 600)
                            .addPos(0, 0, 0, 300).addPos(0, 0, 0, 100).addPos(-45, 0, 0, 50)
                            .addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                    .addBus("RELOAD_MOVE", seq().addPos(0, 0, 0, 300).addPos(0, -15, 0, 1000).addPos(0, 0, 0, 450))
                    .addBus("DRUM_PUSH", seq().addPos(0, 0, 0, 1600).addPos(0, 0, -5, 0).addPos(0, 0, 0, 300));
            case INSPECT -> new BusAnimation()
                    .addBus("LATCH", seq().addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
                    .addBus("FRONT", seq().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150)
                            .addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
                    .addBus("RELOAD_ROT", seq().addPos(0, 0, 0, 300).addPos(45, 0, 0, 500, IType.SIN_FULL)
                            .addPos(45, 0, 0, 500).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                    .addBus("RELOAD_MOVE", seq().addPos(0, 0, 0, 300).addPos(0, -2.5, 0, 500, IType.SIN_FULL)
                            .addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
            case JAMMED -> new BusAnimation()
                    .addBus("LATCH", seq().addPos(0, 0, 0, 500).addPos(0, 0, 90, 300)
                            .addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
                    .addBus("FRONT", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200)
                            .addPos(0, 0, 45, 150).addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
                    .addBus("RELOAD_ROT", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300)
                            .addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(45, 0, 0, 500)
                            .addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                    .addBus("RELOAD_MOVE", seq().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300)
                            .addPos(0, -2.5, 0, 500, IType.SIN_FULL).addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
            default -> null;
        };
    }

    private static int loaded(ItemStack stack, int rail) {
        return ((ItemGunBaseNT) stack.getItem()).getPrimaryMagazine(stack, rail).getAmount(stack, null);
    }

    private static BusAnimationSequence seq() {
        return new BusAnimationSequence();
    }
}
