package net.neoforged.fml.common;
import java.lang.annotation.*; @Retention(RetentionPolicy.RUNTIME) public @interface EventBusSubscriber { String modid() default ""; Bus bus() default Bus.FORGE; net.neoforged.api.distmarker.Dist[] value() default {};
 enum Bus { MOD, FORGE } }
