package io.github.glassmc.loader;

import com.github.jezza.Toml;
import com.github.jezza.TomlTable;
import io.github.glassmc.loader.launch.GlassClassLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.JarFile;

public class GlassLoader {

    private static final GlassLoader INSTANCE = new GlassLoader();

    public static GlassLoader getInstance() {
        return INSTANCE;
    }

    private final File shardsFile = new File("shards");

    private final List<ShardSpecification> registeredShards = new ArrayList<>();
    private final List<ShardInfo> shards = new ArrayList<>();

    private final Map<String, List<Map.Entry<ShardInfo, Class<? extends Listener>>>> listeners = new HashMap<>();
    private final List<Object> apis = new ArrayList<>();
    private final Map<Class<?>, Object> interfaces = new HashMap<>();

    private GlassLoader() {
        this.registerVirtualShard(new ShardSpecification("loader", "0.0.1"));
    }

    public void registerAllShards() {
        for(File shard : Objects.requireNonNull(this.shardsFile.listFiles())) {
            this.registerShard(shard);
        }
    }

    private void registerShard(File shardFile) {
        try {
            JarFile shardJARFile = new JarFile(shardFile);
            this.registeredShards.add(this.parseShardSpecification(shardJARFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadShards() {
        for(File shard : Objects.requireNonNull(this.shardsFile.listFiles())) {
            this.loadShard(shard);
        }
    }

    public void loadShard(File shardFile) {
        GlassClassLoader shardLoader = (GlassClassLoader) GlassLoader.class.getClassLoader();
        try {
            shardLoader.addURL(shardFile.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            JarFile shardJARFile = new JarFile(shardFile);
            ShardInfo shardInfo = this.loadShardInfo(shardJARFile, "glass/info.toml", null);
            if(shardInfo != null) {
                this.shards.add(shardInfo);

                this.registerListeners(shardInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void registerListeners(ShardInfo shardInfo) {
        for(ShardInfo implementation : shardInfo.getImplementations()) {
            this.registerListeners(implementation);
        }

        Map<String, List<Class<? extends Listener>>> listeners = shardInfo.getListeners();
        for(String hook : listeners.keySet()) {
            for(Class<? extends Listener> listener : listeners.get(hook)) {
                this.listeners.computeIfAbsent(hook, k -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(shardInfo, listener));
            }
        }
    }

    public void runHooks(String hook) {
        List<ShardSpecification> executedListeners = new ArrayList<>();
        List<Map.Entry<ShardInfo, Class<? extends Listener>>> listeners = new ArrayList<>(this.listeners.get(hook));

        int i = 0;
        while(i < listeners.size()) {
            Map.Entry<ShardInfo, Class<? extends Listener>> listener = listeners.get(i);
            boolean canLoad = true;
            for(ShardSpecification dependency : listener.getKey().getDependencies()) {
                boolean satisfied = true;
                for(Map.Entry<ShardInfo, Class<? extends Listener>> listener1 : listeners) {
                    if(dependency.isSatisfied(listener1.getKey().getSpecification())) {
                        satisfied = false;
                    }
                }

                for(ShardSpecification specification : executedListeners) {
                    if(dependency.isSatisfied(specification)) {
                        satisfied = true;
                    }
                }
                if(!satisfied) {
                    canLoad = false;
                    break;
                }
            }

            if(canLoad) {
                executedListeners.add(listener.getKey().getSpecification());
                try {
                    Listener listener1 = listener.getValue().newInstance();
                    listener1.run();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
                i++;
            } else {
                listeners.remove(listener);
                listeners.add(listener);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ShardInfo loadShardInfo(JarFile shardJARFile, String entryName, String overrideID) {
        try {
            InputStream shardInfoStream = shardJARFile.getInputStream(shardJARFile.getJarEntry(entryName));
            TomlTable shardInfoTOML = Toml.from(shardInfoStream);

            String id = (String) shardInfoTOML.getOrDefault("id", overrideID);
            String version = (String) shardInfoTOML.getOrDefault("version", null);
            ShardSpecification specification = new ShardSpecification(id, version);

            TomlTable listenersTOML = (TomlTable) shardInfoTOML.getOrDefault("listeners", new TomlTable());
            Map<String, List<Class<? extends Listener>>> listeners = new HashMap<>();
            for(Map.Entry<String, Object> entry : listenersTOML.entrySet()) {
                String hook = entry.getKey();
                listeners.put(hook, new ArrayList<>());
                List<String> hookListeners = (List<String>) listenersTOML.get(hook);
                for(String hookListener : hookListeners) {
                    try {
                        Class<? extends Listener> listenerClass = (Class<? extends Listener>) Class.forName((hookListener));
                        listeners.get(hook).add(listenerClass);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

            TomlTable dependenciesTOML = (TomlTable) shardInfoTOML.getOrDefault("dependencies", new TomlTable());
            List<ShardSpecification> dependencies = new ArrayList<>();
            for(Map.Entry<String, Object> entry : dependenciesTOML.entrySet()) {
                String dependencyID = entry.getKey();
                String dependencyVersion = (String) dependenciesTOML.get(dependencyID);
                dependencies.add(new ShardSpecification(dependencyID, dependencyVersion));
            }

            List<String> implementationsList = (List<String>) shardInfoTOML.getOrDefault("implementations", new ArrayList<>());
            List<ShardInfo> implementations = new ArrayList<>();
            for(String implementationID : implementationsList) {
                int index = id.indexOf("-") + 1;
                if(index == 0) {
                    index = id.length();
                }
                ShardInfo shardInfo = this.loadShardInfo(shardJARFile, "glass" + (!id.substring(index).isEmpty() ? "/" : "") + id.substring(index).replace("-", "/") + "/" + implementationID + "/info.toml", id + "-" + implementationID);
                if(shardInfo != null) {
                    implementations.add(shardInfo);
                }
            }

            for(ShardSpecification dependency : dependencies) {
                boolean satisfied = false;
                for(ShardSpecification shard : this.registeredShards) {
                    if(dependency.isSatisfied(shard)) {
                        satisfied = true;
                    }
                }

                if(!satisfied) {
                    return null;
                }
            }

            return new ShardInfo(specification, listeners, dependencies, implementations);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ShardSpecification parseShardSpecification(JarFile shardJARFile) {
        try {
            InputStream shardInfoStream = shardJARFile.getInputStream(shardJARFile.getJarEntry("glass/info.toml"));
            TomlTable shardInfoTOML = Toml.from(shardInfoStream);

            String id = (String) shardInfoTOML.get("id");
            String version = (String) shardInfoTOML.getOrDefault("version", null);
            return new ShardSpecification(id, version);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void registerVirtualShard(ShardSpecification specification) {
        this.registeredShards.add(specification);
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

}
