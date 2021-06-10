package ml.glassmc.loader;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class GlassLoader {

    private static final GlassLoader INSTANCE = new GlassLoader();

    public static GlassLoader getInstance() {
        return INSTANCE;
    }

    private final List<Object> apis = new ArrayList<>();

    private GlassLoader() {

    }

    public List<ShardInfo> collectShardInfo() {
        List<ShardInfo> shardInfo = new ArrayList<>();

        try {
            ClassLoader classLoader = GlassLoader.class.getClassLoader();
            Enumeration<URL> shardInfoLocations = classLoader.getResources("shard.json");
            while(shardInfoLocations.hasMoreElements()) {
                URL shardInfoLocation = shardInfoLocations.nextElement();
                InputStream shardInfoStream = shardInfoLocation.openStream();
                String shardInfoText = IOUtils.toString(shardInfoStream, StandardCharsets.UTF_8);
                shardInfo.add(this.parseShardInfo(shardInfoText));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return shardInfo;
    }

    private ShardInfo parseShardInfo(String shardInfo) {
        JSONObject shardInfoJSON = new JSONObject(shardInfo);

        String id = shardInfoJSON.getString("id");
        String version = shardInfoJSON.getString("version");

        return new ShardInfo(id, version);
    }

    public void registerAPI(Object api) {
        this.apis.add(api);
    }

    public <T> T getAPI(Class<T> apiClass) {
        for(Object api : apis) {
            if(apiClass.isInstance(api)) {
                return apiClass.cast(api);
            }
        }
        return null;
    }

}
