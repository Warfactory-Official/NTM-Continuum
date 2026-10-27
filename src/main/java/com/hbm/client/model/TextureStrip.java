package com.hbm.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.golem.core.Ids;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * An animated texture's vertical strip of square frames, read from the PNG size and the {@code .mcmeta} frame time. A Gem
 * surface samples its texture whole, not as an atlas sprite, so an OBJ drawn with it picks its frame itself.
 */
public record TextureStrip(int frames, int frameTime) {

    private static final TextureStrip STILL = new TextureStrip(1, 1);

    public static TextureStrip of(Identifier texture) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        Optional<Resource> meta = resources.getResource(Ids.of(texture.getNamespace(), texture.getPath() + ".mcmeta"));
        if (meta.isEmpty()) return STILL;
        try (DataInputStream png = new DataInputStream(resources.getResourceOrThrow(texture).open());
             Reader json = new InputStreamReader(meta.get().open(), StandardCharsets.UTF_8)) {
            // PNG signature (8) and the IHDR length and tag (8), then width and height
            png.skipNBytes(16);
            int width = png.readInt();
            int height = png.readInt();
            JsonObject animation = JsonParser.parseReader(json).getAsJsonObject().getAsJsonObject("animation");
            if (animation == null || width <= 0 || height < width * 2) return STILL;
            int frameTime = animation.has("frametime") ? Math.max(1, animation.get("frametime").getAsInt()) : 1;
            return new TextureStrip(height / width, frameTime);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture strip " + texture, e);
        }
    }

    public int frameAt(long ticks) {
        return (int) (ticks / frameTime % frames);
    }
}
