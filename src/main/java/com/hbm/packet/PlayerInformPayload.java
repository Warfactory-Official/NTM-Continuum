package com.hbm.packet;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.net.GolemCodec;
import dev.golem.core.net.GolemPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code PlayerInformPacket}: a short message for the client's info HUD
 * ({@code RenderInfoSystem}), keyed by an id so a newer message of the same kind replaces the older one.
 *
 * <p>The message is sent as its parts — literal text or a translation key — plus one colour, which is
 * every message HBM sends this way. That keeps the wire format identical on every target instead of
 * depending on the component codec, whose buffer type changed with 1.20.5.
 */
public record PlayerInformPayload(int infoId, int millis, String color, List<String> parts, List<Boolean> translated,
                                  List<String> partColors)
        implements GolemPayload {

    /** 1.7 {@code ServerProxy.ID_TOOLABILITY}. */
    public static final int ID_TOOLABILITY = 11;

    public static final GolemCodec<PlayerInformPayload> CODEC = GolemCodec.of(PlayerInformPayload.class,
            Ids.of(Constants.MOD_ID, "player_inform"),
            (payload, buffer) -> {
                buffer.writeInt(payload.infoId);
                buffer.writeInt(payload.millis);
                buffer.writeUtf(payload.color);
                buffer.writeVarInt(payload.parts.size());
                for (int i = 0; i < payload.parts.size(); i++) {
                    buffer.writeBoolean(payload.translated.get(i));
                    buffer.writeUtf(payload.parts.get(i));
                    buffer.writeUtf(payload.partColors.get(i));
                }
            },
            buffer -> {
                int infoId = buffer.readInt();
                int millis = buffer.readInt();
                String color = buffer.readUtf();
                int n = Math.min(buffer.readVarInt(), 64);
                List<String> parts = new ArrayList<>(n);
                List<Boolean> translated = new ArrayList<>(n);
                List<String> partColors = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    translated.add(buffer.readBoolean());
                    parts.add(buffer.readUtf());
                    partColors.add(buffer.readUtf());
                }
                return new PlayerInformPayload(infoId, millis, color, parts, translated, partColors);
            });

    /** A message from literal/translated parts in one colour; {@code millis} 0 means the 1.7 default of 1000. */
    public static PlayerInformPayload of(int infoId, int millis, ChatFormatting color, Object... parts) {
        List<String> text = new ArrayList<>();
        List<Boolean> translated = new ArrayList<>();
        List<String> partColors = new ArrayList<>();
        for (Object part : parts) {
            // 1.7 ChatBuilder's .color() after .next(): that one part carries its own colour.
            String partColor = "";
            if (part instanceof Colored c) {
                partColor = c.color().getName();
                part = c.part();
            }
            partColors.add(partColor);
            if (part instanceof Translated t) {
                text.add(t.key());
                translated.add(true);
            } else {
                text.add(String.valueOf(part));
                translated.add(false);
            }
        }
        return new PlayerInformPayload(infoId, millis, color.getName(), text, translated, partColors);
    }

    /** A part of {@link #of} in its own colour; the part may be a {@link Translated}. */
    public record Colored(Object part, ChatFormatting color) {}

    /** A translation key among the parts of {@link #of}. */
    public record Translated(String key) {}

    /** The message as the client shows it. */
    public Component message() {
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            MutableComponent part = translated.get(i) ? Component.translatable(parts.get(i)) : Component.literal(parts.get(i));
            ChatFormatting partFormat = partColors.get(i).isEmpty() ? null : ChatFormatting.getByName(partColors.get(i));
            out.append(partFormat != null ? part.withStyle(partFormat) : part);
        }
        ChatFormatting format = ChatFormatting.getByName(color);
        return format != null ? out.withStyle(format) : out;
    }

    @Override
    public Identifier id() {
        return CODEC.id();
    }
}
