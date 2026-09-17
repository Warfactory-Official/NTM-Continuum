package com.hbm.handler.threading;

import net.minecraft.server.level.ServerLevel;

// 26.1: replaces CE's NetworkRegistry.TargetPoint; radius ignored.
public record TargetPoint(ServerLevel level, double x, double y, double z, double radius) {
}
