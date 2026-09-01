package com.hbm.tileentity.machine;

/** Exact OBJ part and pivot contract for each of the Assembly Factory's four arms. */
public record AssemblyFactoryArmLayout(
        int part,
        float pivotZ,
        float headZ,
        float rotationSign,
        boolean saw,
        float bladeZ,
        float bladeSign) {

    public static AssemblyFactoryArmLayout forArm(int arm) {
        return switch (arm) {
            case 1 -> new AssemblyFactoryArmLayout(1, -0.9375F, -0.4375F, -1F, false, 0F, 0F);
            case 2 -> new AssemblyFactoryArmLayout(2, 0.9375F, 0.4375F, 1F, true, 0.3125F, -1F);
            case 3 -> new AssemblyFactoryArmLayout(3, 0.9375F, 0.4375F, 1F, false, 0F, 0F);
            case 4 -> new AssemblyFactoryArmLayout(4, -0.9375F, -0.4375F, -1F, true, -0.3125F, 1F);
            default -> throw new IllegalArgumentException("Assembly Factory arm must be 1..4: " + arm);
        };
    }
}
