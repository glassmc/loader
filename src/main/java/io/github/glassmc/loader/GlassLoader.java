package io.github.glassmc.loader;

import io.github.glassmc.loader.launch.Launcher;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
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

    private final Launcher launcher = new Launcher();

    private final List<ShardSpecification> virtualShards = new ArrayList<>();
    private final List<Object> apis = new ArrayList<>();
    private final Map<Class<?>, Object> interfaces = new HashMap<>();

    private GlassLoader() {
        this.registerVirtualShard(new ShardSpecification("loader", "0.0.1"));
    }

    public void runHooks(String hook) {
        List<ShardInfo> shardInfoComplete = this.collectShardInfo();
        List<ShardInfo> shardInfoFiltered = shardInfoComplete.stream()
                .filter(shardInfo -> shardInfo.getListeners().containsKey(hook)).collect(Collectors.toList());
        int index = 0;
        int failedCounter = 0;

        List<ShardSpecification> availableShards = this.getAvailableShards(shardInfoFiltered);
        while(index < shardInfoFiltered.size()) {
            ShardInfo shardInfo = shardInfoFiltered.get(index);
            boolean dependenciesSatisfied = true;
            for(ShardSpecification dependency : shardInfo.getDependencies()) {
                boolean found = true; //TODO: make hook dependencies or something
                for(ShardSpecification specification : availableShards) {
                    if(dependency.isSatisfied(specification)) {
                        found = true;
                    }
                }
                if(!found) {
                    dependenciesSatisfied = false;
                }
            }

            if(dependenciesSatisfied) {
                for(Class<?> listenerClass : shardInfo.getListeners().get(hook)) {
                    try {
                        Listener listener = (Listener) listenerClass.newInstance();
                        listener.run();
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
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

    private List<ShardInfo> collectShardInfo() {
        ClassLoader classLoader = GlassLoader.class.getClassLoader();

        List<ShardSpecification> shards = new ArrayList<>();
        try {
            Enumeration<URL> shardInfoLocations = classLoader.getResources("shard.json");

            while(shardInfoLocations.hasMoreElements()) {
                InputStream shardInfoStream = shardInfoLocations.nextElement().openStream();
                String shardInfoText = IOUtils.toString(shardInfoStream, StandardCharsets.UTF_8);
                shards.add(this.parseShardSpecification(shardInfoText));
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        shards.addAll(this.virtualShards);

        List<ShardInfo> shardInfoList = new ArrayList<>();

        try {
            List<String> resources = IOUtils.readLines(classLoader.getResourceAsStream("glass/"), Charsets.UTF_8);

            List<URL> shardInfoURLList = new ArrayList<>();
            for(String resource : resources){
                shardInfoURLList.add(classLoader.getResource("glass/" + resource + "/info.json"));
            }

            for(URL shardInfoURL : shardInfoURLList) {
                InputStream shardInfoStream = shardInfoURL.openStream();
                String shardInfoText = IOUtils.toString(shardInfoStream, StandardCharsets.UTF_8);
                ShardInfo shardInfo = this.parseShardInfo(shardInfoText, null);

                boolean satisfied = true;
                for(ShardSpecification dependency : shardInfo.getDependencies()) {
                    boolean found = false;
                    for(ShardSpecification specification : shards) {
                        if(!dependency.isSatisfied(specification)) {
                            found = true;
                        }
                    }

                    if(!found) {
                        satisfied = false;
                    }
                }

                if(satisfied) {
                    shardInfoList.add(shardInfo);
                    this.cleanImplementations(shards, shardInfo);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        return shardInfoList;
    }

    private void cleanImplementations(List<ShardSpecification> shards, ShardInfo shardInfo) {
        for(ShardInfo implementation : new ArrayList<>(shardInfo.getImplementations())) {
            boolean satisfied1 = true;
            for(ShardSpecification dependency : implementation.getDependencies()) {
                boolean found = false;
                for(ShardSpecification specification : shards) {
                    if(dependency.isSatisfied(specification)) {
                        found = true;
                    }
                }

                if(!found) {
                    satisfied1 = false;
                }
            }

            if(!satisfied1) {
                shardInfo.getImplementations().remove(implementation);
            } else {
                this.cleanImplementations(shards, implementation);
            }
        }
    }

    private ShardInfo parseShardInfo(String shardInfoText, String overrideID) {
        JSONObject shardInfoJSON = new JSONObject(shardInfoText);

        String id = shardInfoJSON.has("id") ? shardInfoJSON.getString("id") : overrideID;
        String version = shardInfoJSON.has("version") ? shardInfoJSON.getString("version") : null;
        ShardSpecification specification = new ShardSpecification(id, version);

        JSONObject listenersJSON = shardInfoJSON.has("listeners") ? shardInfoJSON.getJSONObject("listeners") : new JSONObject();
        Map<String, List<Class<? extends Listener>>> listeners = new HashMap<>();
        for(String hook : listenersJSON.keySet()) {
            listeners.put(hook, new ArrayList<>());
            JSONArray hookListeners = listenersJSON.getJSONArray(hook);
            for(Object hookListener : hookListeners) {
                try {
                    Class<? extends Listener> listenerClass = (Class<? extends Listener>) Class.forName(((String) hookListener));
                    listeners.get(hook).add(listenerClass);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        JSONObject dependenciesJSON = shardInfoJSON.has("dependencies") ? shardInfoJSON.getJSONObject("dependencies") : new JSONObject();
        List<ShardSpecification> dependencies = new ArrayList<>();
        for(String dependencyID : dependenciesJSON.keySet()) {
            String dependencyVersion = dependenciesJSON.getString(dependencyID);
            dependencies.add(new ShardSpecification(dependencyID, dependencyVersion));
        }

        JSONArray implementationsJSON = shardInfoJSON.has("implementations") ? shardInfoJSON.getJSONArray("implementations") : new JSONArray();
        List<ShardInfo> implementations = new ArrayList<>();
        for(Object implementationIDObject : implementationsJSON) {
            String implementationID = (String) implementationIDObject;
            InputStream shardInfoStream = GlassLoader.class.getClassLoader().getResourceAsStream("glass/" + id.replace("-", "/") + "/" + implementationID + "/info.json");
            try {
                if (shardInfoStream != null) {
                    String implementationInfoText = IOUtils.toString(shardInfoStream, StandardCharsets.UTF_8);
                    implementations.add(this.parseShardInfo(implementationInfoText, id + "-" + implementationID));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new ShardInfo(specification, listeners, dependencies, implementations);
    }

    private ShardSpecification parseShardSpecification(String shardInfoText) {
        JSONObject shardInfoJSON = new JSONObject(shardInfoText);

        String id = shardInfoJSON.getString("id");
        String version = shardInfoJSON.getString("version");
        return new ShardSpecification(id, version);
    }

    private List<ShardSpecification> getAvailableShards(List<ShardInfo> shardInfoList) {
        List<ShardSpecification> availableShards = new ArrayList<>();
        for(ShardInfo shardInfo : shardInfoList) {
            availableShards.add(shardInfo.getSpecification());
        }
        availableShards.addAll(this.virtualShards);
        return availableShards;
    }

    public void registerVirtualShard(ShardSpecification specification) {
        this.virtualShards.add(specification);
    }

    public void registerAPI(Object api) {
        this.apis.add(api);
    }

    public <T> T getAPI(Class<T> clazz) {
        for(Object api : this.apis) {
            if(clazz.isInstance(api)) {
                return clazz.cast(api);
            }
        }
        return null;
    }

    public <T> void registerInterface(Class<T> interfaceClass, T implementer) {
        this.interfaces.put(interfaceClass, implementer);
    }

    public <T> T getInterface(Class<T> interfaceClass) {
        return interfaceClass.cast(this.interfaces.get(interfaceClass));
    }

    public Launcher getLauncher() {
        return launcher;
    }

}
