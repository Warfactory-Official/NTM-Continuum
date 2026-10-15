package com.hbm.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.golem.core.render.Poses;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.Vertexes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.12.2 {@code animloader} ({@code ColladaLoader}, {@code AnimatedModel}, {@code Transform}): a {@code .dae} node
 * tree read at run time, and per-node matrix tracks matched by the {@code animation} element's {@code name}, which
 * Blender's exporter sets to the node name.
 */
public final class ColladaModel {

    private final List<NodeData> roots;

    private ColladaModel(List<NodeData> roots) {
        this.roots = roots;
    }

    /** One played animation: the length {@code loadAnim} was given and every node's keyframe transforms. */
    public record Animation(int length, int frames, Map<String, Transform[]> tracks) {}

    /**
     * Called for each node on the walk with the pose at that node, before its children, as 1.12's
     * {@code IAnimatedModelCallback.onRender}: the render type to draw the node's own geometry in (1.12 bound its
     * texture there), or null to hide it (1.12 returned true), which leaves its children alone.
     */
    @FunctionalInterface
    public interface NodeDraw {
        @Nullable RenderType draw(String name, PoseStack pose, int prevFrame, int frame);
    }

    /** The geometry of one node, drawn at {@code pose}; triangles as degenerate quads. */
    public void draw(String name, PoseStack.Pose pose, VertexConsumer buffer, int color, int light, int overlay) {
        for (NodeData root : roots) {
            NodeData node = find(root, name);
            if (node != null) {
                node.emit(pose, buffer, color, light, overlay);
                return;
            }
        }
    }

    private static @Nullable NodeData find(NodeData node, String name) {
        if (node.name.equals(name)) return node;
        for (NodeData child : node.children) {
            NodeData found = find(child, name);
            if (found != null) return found;
        }
        return null;
    }

    /** {@code render(callback)}: every node at its rest matrix, frames reported as -1. */
    public void render(PoseStack pose, SubmitSink sink, int color, int light, int overlay, NodeDraw draw) {
        Walk walk = new Walk(sink, color, light, overlay, draw, null, 0, 0, 0F, -1, -1);
        for (NodeData root : roots) walk.node(root, pose);
    }

    /**
     * {@code renderAnimated}'s keyframe arithmetic for {@code elapsed} milliseconds, then the walk. Returns the
     * keyframe drawn, 1.12's {@code prevFrame} for the next call; past the end nothing is played (the
     * {@code EndType.END} the guns used) and the model is drawn at rest.
     */
    public int render(Animation animation, long elapsed, int prevFrame, PoseStack pose, SubmitSink sink, int color,
                      int light, int overlay, NodeDraw draw) {
        if (animation.frames == 0 || elapsed > animation.length) {
            render(pose, sink, color, light, overlay, draw);
            return prevFrame;
        }
        int diff = (int) elapsed;
        float remapped = Math.max(0, Math.min(animation.frames - 1, diff / (float) animation.length * (animation.frames - 1)));
        int first = (int) remapped;
        int next = first < animation.frames - 1 ? first + 1 : first;
        float inter = (float) (remapped - Math.floor(remapped));
        Walk walk = new Walk(sink, color, light, overlay, draw, animation, first, next, inter, prevFrame, first);
        for (NodeData root : roots) walk.node(root, pose);
        return first;
    }

    /** {@code renderWithIndex}: push, the node's keyframe blend (or its rest matrix), the callback, draw, children, pop. */
    private record Walk(SubmitSink sink, int color, int light, int overlay, NodeDraw draw, @Nullable Animation animation,
                        int first, int next, float inter, int prevFrame, int frame) {
        void node(NodeData node, PoseStack pose) {
            pose.pushPose();
            boolean hidden = false;
            if (node.base != null) {
                Transform[] track = animation == null ? null : animation.tracks.get(node.name);
                if (track != null) {
                    hidden = track[first].hidden;
                    Poses.mulPose(pose, track[first].interpolate(track[next], inter));
                } else {
                    Poses.mulPose(pose, node.base);
                }
            }
            RenderType type = draw.draw(node.name, pose, prevFrame, frame);
            if (!hidden && type != null && node.triangles != null) {
                sink.submitCustomGeometry(pose, type, (p, buffer) -> node.emit(p, buffer, color, light, overlay));
            }
            for (NodeData child : node.children) node(child, pose);
            pose.popPose();
        }
    }

    private static final class NodeData {
        final String name;
        final @Nullable Matrix4f base;
        String geometry = "";
        float @Nullable [] triangles;
        final List<NodeData> children = new ArrayList<>();

        NodeData(String name, @Nullable Matrix4f base) {
            this.name = name;
            this.base = base;
        }

        /** x, y, z, u, v, nx, ny, nz per vertex, three vertices a triangle. */
        void emit(PoseStack.Pose pose, VertexConsumer buffer, int color, int light, int overlay) {
            float[] t = triangles;
            if (t == null) return;
            for (int i = 0; i < t.length; i += 24) {
                for (int v = 0; v < 4; v++) {
                    int at = i + Math.min(v, 2) * 8;
                    Vertexes.vertex(buffer, pose, t[at], t[at + 1], t[at + 2], color, t[at + 3], t[at + 4], overlay, light,
                            t[at + 5], t[at + 6], t[at + 7]);
                }
            }
        }
    }

    // Loading

    /** {@code ColladaLoader.load(file, flipV)}. */
    public static ColladaModel load(Identifier file, boolean flipV) {
        Element root = read(file);
        Element scene = firstElement(first(root, "library_visual_scenes"));
        List<NodeData> roots = new ArrayList<>();
        for (Element node : childElements(scene)) {
            if (node.getElementsByTagName("instance_geometry").getLength() > 0) roots.add(structure(node));
        }
        Map<String, float[]> geometry = geometry(first(root, "library_geometries"), flipV);
        for (NodeData node : roots) attach(node, geometry);
        return new ColladaModel(roots);
    }

    /** {@code ColladaLoader.loadAnim(length, file)}. */
    public static Animation loadAnimation(int length, Identifier file) {
        Element root = read(file);
        Map<String, Transform[]> tracks = new HashMap<>();
        int frames = 0;
        for (Element e : childElements(first(root, "library_animations"))) {
            if (!"animation".equals(e.getNodeName())) continue;
            List<Element> parts = childElements(e);
            if (parts.isEmpty()) continue;
            Transform[] t = null;
            for (Element part : parts) {
                if (part.getAttribute("id").endsWith("transform")) {
                    t = transforms(part);
                } else if (part.getAttribute("id").endsWith("hide_viewport") && t != null) {
                    int[] hidden = ints(output(part));
                    for (int i = 0; i < hidden.length && i < t.length; i++) t[i].hidden = hidden[i] > 0;
                }
            }
            if (t == null) continue;
            tracks.put(e.getAttribute("name"), t);
            frames = t.length;
        }
        return new Animation(length, frames, tracks);
    }

    private static Element read(Identifier file) {
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(file).open()) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            return doc.getDocumentElement();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Collada file " + file, e);
        }
    }

    private static NodeData structure(Element element) {
        Matrix4f base = null;
        String geometry = "";
        List<Element> nested = new ArrayList<>();
        for (Element child : childElements(element)) {
            if ("transform".equals(child.getAttribute("sid"))) {
                // Collada matrices are row-major; 1.12 flipped them for glMultMatrix, which is JOML's column order
                base = new Matrix4f().set(floats(child.getTextContent())).transpose();
            } else if ("instance_geometry".equals(child.getTagName())) {
                geometry = child.getAttribute("url").substring(1);
            } else if (child.getElementsByTagName("instance_geometry").getLength() > 0) {
                nested.add(child);
            }
        }
        NodeData node = new NodeData(element.getAttribute("name"), base);
        node.geometry = geometry;
        for (Element child : nested) node.children.add(structure(child));
        return node;
    }

    private static void attach(NodeData node, Map<String, float[]> geometry) {
        if (!node.geometry.isEmpty()) node.triangles = geometry.get(node.geometry);
        for (NodeData child : node.children) attach(child, geometry);
    }

    /** Every geometry's triangles, with the index triples read as position, normal, texture coordinate. */
    private static Map<String, float[]> geometry(Element library, boolean flipV) {
        Map<String, float[]> all = new HashMap<>();
        for (Element e : childElements(library)) {
            if (!"geometry".equals(e.getNodeName())) continue;
            Element mesh = null;
            for (Element c : childElements(e)) if ("mesh".equals(c.getNodeName())) mesh = c;
            if (mesh == null) continue;
            float[] positions = new float[0], normals = new float[0], uvs = new float[0];
            List<int[]> indexLists = new ArrayList<>();
            for (Element section : childElements(mesh)) {
                String id = section.getAttribute("id");
                if (id.endsWith("mesh-positions")) positions = floats(floatArray(section));
                else if (id.endsWith("mesh-normals")) normals = floats(floatArray(section));
                else if (id.endsWith("mesh-map-0")) uvs = floats(floatArray(section));
                else if (section.getNodeName().equals("triangles")) {
                    indexLists.add(ints(section.getElementsByTagName("p").item(0).getTextContent()));
                }
            }
            if (positions.length == 0) continue;
            int count = 0;
            for (int[] indices : indexLists) count += indices.length / 3;
            float[] out = new float[count * 8];
            int at = 0;
            for (int[] indices : indexLists) {
                for (int i = 0; i + 2 < indices.length; i += 3) {
                    float v = uvs[indices[i + 2] * 2 + 1];
                    if (flipV) v = 1 - v;
                    out[at++] = positions[indices[i] * 3];
                    out[at++] = positions[indices[i] * 3 + 1];
                    out[at++] = positions[indices[i] * 3 + 2];
                    out[at++] = uvs[indices[i + 2] * 2];
                    out[at++] = v;
                    out[at++] = normals[indices[i + 1] * 3];
                    out[at++] = normals[indices[i + 1] * 3 + 1];
                    out[at++] = normals[indices[i + 1] * 3 + 2];
                }
            }
            all.put(e.getAttribute("id"), out);
        }
        return all;
    }

    private static Transform[] transforms(Element animation) {
        float[] values = floats(output(animation));
        Transform[] transforms = new Transform[values.length / 16];
        for (int i = 0; i < transforms.length; i++) {
            float[] raw = new float[16];
            System.arraycopy(values, i * 16, raw, 0, 16);
            transforms[i] = new Transform(raw);
        }
        return transforms;
    }

    /** The {@code float_array} text of the sampler's OUTPUT source. */
    private static String output(Element animation) {
        Element sampler = (Element) animation.getElementsByTagName("sampler").item(0);
        String source = null;
        for (Element input : childElements(sampler)) {
            if ("OUTPUT".equals(input.getAttribute("semantic"))) source = input.getAttribute("source").substring(1);
        }
        for (Element e : childElements(animation)) {
            if (e.getAttribute("id").equals(source)) return floatArray(e);
        }
        throw new IllegalStateException("Collada animation " + animation.getAttribute("id") + " has no OUTPUT source");
    }

    private static String floatArray(Element e) {
        return e.getElementsByTagName("float_array").item(0).getTextContent();
    }

    private static Element first(Element root, String tag) {
        return (Element) root.getElementsByTagName(tag).item(0);
    }

    private static @Nullable Element firstElement(Node root) {
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) return (Element) nodes.item(i);
        }
        return null;
    }

    private static List<Element> childElements(@Nullable Element e) {
        List<Element> elements = new ArrayList<>();
        if (e == null) return elements;
        NodeList n = e.getChildNodes();
        for (int i = 0; i < n.getLength(); i++) {
            if (n.item(i).getNodeType() == Node.ELEMENT_NODE) elements.add((Element) n.item(i));
        }
        return elements;
    }

    private static float[] floats(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return new float[0];
        String[] numbers = trimmed.split("\\s+");
        float[] out = new float[numbers.length];
        for (int i = 0; i < numbers.length; i++) out[i] = Float.parseFloat(numbers[i]);
        return out;
    }

    private static int[] ints(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return new int[0];
        String[] numbers = trimmed.split("\\s+");
        int[] out = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) out[i] = (int) Float.parseFloat(numbers[i]);
        return out;
    }

    /**
     * 1.12.2 {@code animloader.Transform}: the raw row-major keyframe split into translation, row scale and an
     * LWJGL2-read quaternion; translation and scale lerped, rotation slerped (including the old near-parallel branch,
     * {@code v0 + t * v1}), rebuilt by {@code quatToGlMatrix} with the rows scaled.
     */
    public static final class Transform {
        private final Vector3f translation;
        private final Vector3f scale;
        private final Quaternionf rotation;
        boolean hidden;

        Transform(float[] raw) {
            float scaleX = length(raw, 0);
            float scaleY = length(raw, 4);
            float scaleZ = length(raw, 8);
            float[] normalized = raw.clone();
            for (int i = 0; i < 3; i++) normalized[i] /= scaleX;
            for (int i = 4; i < 7; i++) normalized[i] /= scaleY;
            for (int i = 8; i < 11; i++) normalized[i] /= scaleZ;
            Matrix4f matrix = new Matrix4f().set(normalized);
            translation = new Vector3f(raw[3], raw[7], raw[11]);
            scale = new Vector3f(scaleX, scaleY, scaleZ);
            // LWJGL2's setFromMatrix reads the opposite off-diagonal order from JOML
            rotation = matrix.transpose().getNormalizedRotation(new Quaternionf());
        }

        private static float length(float[] v, int offset) {
            return (float) Math.sqrt(v[offset] * v[offset] + v[offset + 1] * v[offset + 1] + v[offset + 2] * v[offset + 2]);
        }

        Matrix4f interpolate(Transform other, float amount) {
            Vector3f t = translation.lerp(other.translation, amount, new Vector3f());
            Vector3f s = scale.lerp(other.scale, amount, new Vector3f());
            Quaternionf target = new Quaternionf(other.rotation);
            double dot = rotation.dot(target);
            if (dot < 0D) {
                target.set(-target.x, -target.y, -target.z, -target.w);
                dot = -dot;
            }
            Quaternionf q;
            if (dot > 0.9999999D) {
                q = new Quaternionf(rotation.x + amount * target.x, rotation.y + amount * target.y,
                        rotation.z + amount * target.z, rotation.w + amount * target.w).normalize();
            } else {
                double theta0 = Math.acos(dot);
                double theta = theta0 * amount;
                float a = (float) (Math.cos(theta) - dot * Math.sin(theta) / Math.sin(theta0));
                float b = (float) (Math.sin(theta) / Math.sin(theta0));
                q = new Quaternionf(a * rotation.x + b * target.x, a * rotation.y + b * target.y,
                        a * rotation.z + b * target.z, a * rotation.w + b * target.w);
            }
            float x = q.x, y = q.y, z = q.z, w = q.w;
            return new Matrix4f(
                    (1F - 2F * y * y - 2F * z * z) * s.x, (2F * x * y + 2F * z * w) * s.y, (2F * x * z - 2F * y * w) * s.z, 0F,
                    (2F * x * y - 2F * z * w) * s.x, (1F - 2F * x * x - 2F * z * z) * s.y, (2F * y * z + 2F * x * w) * s.z, 0F,
                    (2F * x * z + 2F * y * w) * s.x, (2F * y * z - 2F * x * w) * s.y, (1F - 2F * x * x - 2F * y * y) * s.z, 0F,
                    t.x, t.y, t.z, 1F);
        }
    }
}
