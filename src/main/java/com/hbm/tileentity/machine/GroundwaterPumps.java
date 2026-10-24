package com.hbm.tileentity.machine;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.tileentity.IConfigurableMachine;

import java.io.IOException;

/** 1.7 {@code TileEntityMachinePumpBase} settings, shared by the steam and electric pumps as {@code waterpump}. */
public final class GroundwaterPumps {

    public static int groundHeight = 70;
    public static int groundDepth = 4;
    public static int steamSpeed = 1_000;
    public static int electricSpeed = 10_000;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "waterpump"; }

        @Override public void readIfPresent(JsonObject obj) {
            groundHeight = IConfigurableMachine.grab(obj, "I:groundHeight", groundHeight);
            groundDepth = IConfigurableMachine.grab(obj, "I:groundDepth", groundDepth);
            steamSpeed = IConfigurableMachine.grab(obj, "I:steamSpeed", steamSpeed);
            electricSpeed = IConfigurableMachine.grab(obj, "I:electricSpeed", electricSpeed);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:groundHeight").value(groundHeight);
            writer.name("I:groundDepth").value(groundDepth);
            writer.name("I:steamSpeed").value(steamSpeed);
            writer.name("I:electricSpeed").value(electricSpeed);
        }
    };

    private GroundwaterPumps() {}
}
