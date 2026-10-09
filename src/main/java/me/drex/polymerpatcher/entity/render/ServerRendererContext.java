package me.drex.polymerpatcher.entity.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class ServerRendererContext extends EntityRendererProvider.Context {

    public ServerRendererContext(ServerEntityRenderDispatcher renderDispatcher, ServerBlockModelResolver blockModelResolver, ServerItemModelResolver itemModelResolver, ServerEntityModelSet entityModelSet) {
        super(
            renderDispatcher,
            blockModelResolver,
            itemModelResolver,
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
