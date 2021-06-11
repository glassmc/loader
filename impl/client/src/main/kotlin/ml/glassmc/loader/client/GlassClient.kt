package ml.glassmc.loader.client

import ml.glassmc.loader.GlassLoader
import ml.glassmc.loader.ShardSpecification

object GlassClient {

    fun setup(version: String) {
        GlassLoader.registerVirtualShard(ShardSpecification("client", version))
    }

}