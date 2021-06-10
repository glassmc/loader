package ml.glassmc.loader;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class GlassLoader {

    private static final GlassLoader INSTANCE = new GlassLoader();

    public static GlassLoader getInstance() {
        return INSTANCE;
    }

    private final List<Object> apis = new ArrayList<>();

    private GlassLoader() {

    }

    private List<ShardInfo> collectShardInfo() {
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
        ShardSpecification specification = new ShardSpecification(id, version);

        String referenceClass = shardInfoJSON.has("reference") ? shardInfoJSON.getString("reference") : null;
        Map<Class<?>, Class<? extends Hook>> hooks = new HashMap<>();
        if(referenceClass != null) {
            try {
                Reference reference = (Reference) Class.forName(referenceClass).newInstance();
                hooks.putAll(reference.getHooks());
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        JSONObject dependenciesJSON = shardInfoJSON.has("dependencies") ? shardInfoJSON.getJSONObject("dependencies") : new JSONObject();
        List<ShardSpecification> dependencies = new ArrayList<>();
        for(String dependencyID : dependenciesJSON.keySet()) {
            String dependencyVersion = dependenciesJSON.getString(id);
            dependencies.add(new ShardSpecification(dependencyID, dependencyVersion));
        }

        return new ShardInfo(specification, hooks, dependencies);
    }

    public void runHooks(Class<?> hookType) {
        List<ShardInfo> shardInfoComplete = this.collectShardInfo();
        List<ShardInfo> shardInfoFiltered = shardInfoComplete.stream()
                .filter(shardInfo -> shardInfo.getHooks().containsKey(hookType)).collect(Collectors.toList());
        int index = 0;
        int failedCounter = 0;

        while(index < shardInfoFiltered.size()) {
            ShardInfo shardInfo = shardInfoFiltered.get(index);

            boolean dependenciesSatisfied = true;
            for(ShardSpecification dependency : shardInfo.getDependencies()) {
                boolean satisfied = false;
                for(ShardInfo shardInfo1 : shardInfoFiltered) {
                    if(dependency.isSatisfied(shardInfo1.getSpecification())) {
                        satisfied = true;
                    }
                }

                if(!satisfied) {
                    dependenciesSatisfied = false;
                }
            }

            if(dependenciesSatisfied) {
                try {
                    Class<?> hookClass = shardInfo.getHooks().get(hookType);
                    Hook hook = (Hook) hookClass.newInstance();
                    hook.run();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                index++;
                failedCounter = 0;
            } else {
                shardInfoFiltered.remove(shardInfo);
                shardInfoFiltered.add(shardInfo);
                failedCounter++;
            }

            if(failedCounter > shardInfoFiltered.size()) {
                System.err.println("Failed to load shard(s):");
                for(int i = index; i < shardInfoFiltered.size(); i++) {
                    System.err.println(" - " + shardInfoFiltered.get(i).getSpecification().getID());
                }
                index = shardInfoFiltered.size();
            }
        }
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
