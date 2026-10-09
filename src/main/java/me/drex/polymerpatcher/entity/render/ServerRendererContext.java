package me.drex.polymerpatcher.entity.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ServerRendererContext extends EntityRendererProvider.Context {

    public ServerRendererContext(ServerEntityRenderDispatcher renderDispatcher, ServerItemModelResolver modelResolver, ServerEntityModelSet entityModelSet) {
        super(
            renderDispatcher,
            null,
            modelResolver,
            null,
            null,
            entityModelSet,
            null,
            null,
            null,
            null
        );
    }
}
