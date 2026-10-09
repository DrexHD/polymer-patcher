package me.drex.polymerpatcher.entity;

import com.mojang.math.Quadrant;
import eu.pb4.polymer.resourcepack.api.AssetPaths;
import eu.pb4.polymer.resourcepack.extras.api.format.atlas.AtlasAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelElement;
import eu.pb4.polymer.resourcepack.extras.api.format.model.ModelTransformation;
import me.drex.polymerpatcher.duck.IModelPart;
import me.drex.polymerpatcher.resources.ResourceHelper;
import me.drex.polymerpatcher.mixin.client.ModelPartAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.BiConsumer;

public record PolyModelInstance<Entity extends net.minecraft.world.entity.Entity, RenderState extends EntityRenderState, Model extends EntityModel<? super RenderState>>(
    EntityRenderer<Entity, RenderState> renderer,
    List<ModelPart> allParts,
    @Nullable ModelLayerLocation modelLayer,
    @Nullable Identifier texture
) {

    public static <Entity extends net.minecraft.world.entity.Entity, RenderState extends EntityRenderState, Model extends EntityModel<? super RenderState>> PolyModelInstance<Entity, RenderState, Model> create(
        EntityRenderer<Entity, RenderState> renderer,
        List<ModelPart> allParts,
        @Nullable ModelLayerLocation modelLayer,
        @Nullable Identifier texture
    ) {
        return new PolyModelInstance<>(renderer, allParts, modelLayer, texture);
    }

    public void generateAssets(BiConsumer<String, byte[]> writer, AtlasAsset.Builder atlas) {
        if (texture == null || modelLayer == null) return;
        var sprite = texture.equals(ResourceHelper.ACACIA_BOAT_TEXTURE) ? ResourceHelper.ORIGINAL_ACACIA_BOAT_TEXTURE : texture;
        atlas.single(sprite);

        int id = 0;

        for (var part : allParts) {
            if (part.isEmpty()) continue;
            ((IModelPart) (Object) part).polymer_patcher$setId(id);
            ((IModelPart) (Object) part).polymer_patcher$setModelLayerLocation(modelLayer);

            var modelId = partModelId(texture, modelLayer, id++);
            var model = ModelAsset.builder();
            model.texture("txt", sprite);
            model.textureReference("particle", "txt");

            for (ModelPart.Cube cuboid : ((ModelPartAccessor) (Object) part).getCubes()) {
                for (var quad : cuboid.polygons) {
                    var min = new Vector3f(Float.POSITIVE_INFINITY);
                    var max = new Vector3f(Float.NEGATIVE_INFINITY);
                    ModelPart.Vertex v1 = quad.vertices()[0];
                    ModelPart.Vertex v2 = quad.vertices()[0];

                    for (var vert : quad.vertices()) {
                        var pos = new Vector3f(vert.x(), vert.y(), vert.z());
                        min.min(pos);
                        max.max(pos);
                    }

                    for (var vert : quad.vertices()) {
                        var pos = new Vector3f(vert.x(), vert.y(), vert.z());
                        if (min.equals(pos)) {
                            v1 = vert;
                        }
                        if (max.equals(pos)) {
                            v2 = vert;
                        }
                    }


                    var b = ModelElement.builder(new Vec3(min.x, min.y, min.z).scale(0.25).add(8), new Vec3(max.x, max.y, max.z).scale(0.25).add(8));

                    var dir = Direction.getNearest((int) quad.normal().x(), (int) quad.normal().y(), (int) quad.normal().z(), null);

                    if ((dir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == (dir.getAxis() == Direction.Axis.Z)) {
                        dir = dir.getOpposite();
                    }

                    b.face(dir, clampUv(v1.u() * 16), clampUv(v2.v() * 16), clampUv(v2.u() * 16), clampUv(v1.v() * 16), "#txt", dir, Quadrant.R0, 0);
                    b.face(dir.getOpposite(), clampUv(v2.u() * 16), clampUv(v2.v() * 16), clampUv(v1.u() * 16), clampUv(v1.v() * 16), "#txt", dir.getOpposite(), Quadrant.R0, 0);


                    model.element(b.build());
                }
            }

            model.transformation(ItemDisplayContext.FIXED, new ModelTransformation(new Vec3(0, 180, 0), Vec3.ZERO, new Vec3(4, 4, 4)));

            writer.accept(AssetPaths.model(modelId) + ".json", model.build().toBytes());
        }
    }

    public static Identifier partModelId(Identifier texture, ModelLayerLocation modelLayer, int id) {
        return texture.withSuffix("/" + modelLayer.model().getNamespace() + "/" + modelLayer.model().getPath()
            + "/" + modelLayer.layer() + "/part_" + id);
    }

    private static float clampUv(float uv) {
        return Math.clamp(uv, 0, 16);
    }

}
