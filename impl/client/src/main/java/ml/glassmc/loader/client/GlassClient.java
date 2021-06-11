package ml.glassmc.loader.client;

public class GlassClient {

    public static void launch(String version) {
        GlassLoader.getInstance().registerVirtualShard(new ShardSpecification("client", version));
    }

}
