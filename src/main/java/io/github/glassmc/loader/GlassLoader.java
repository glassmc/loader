package io.github.glassmc.loader;

import com.github.jezza.Toml;
import com.github.jezza.TomlTable;
import io.github.glassmc.loader.loader.GlassClassLoader;
import io.github.glassmc.loader.loader.ITransformer;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class GlassLoader {

    private static final GlassLoader INSTANCE = new GlassLoader();

    public static GlassLoader getInstance() {
        return INSTANCE;
    }

    private final File shardsFile = new File("shards");
    private final GlassClassLoader classLoader = (GlassClassLoader) GlassLoader.class.getClassLoader();

    private final List<ShardSpecification> registeredShards = new ArrayList<>();
    private final List<ShardInfo> shards = new ArrayList<>();

    private final Map<String, List<Map.Entry<ShardInfo, Class<? extends Listener>>>> listeners = new HashMap<>();
    private final List<Object> apis = new ArrayList<>();
    private final Map<Class<?>, Object> interfaces = new HashMap<>();

    private GlassLoader() {
        this.registerVirtualShard(new ShardSpecification("loader", "0.1.0"));
    }

    public void appendExternalShards() {
        if(this.shardsFile.exists()) {
            for (File shard : Objects.requireNonNull(this.shardsFile.listFiles())) {
                this.appendShard(shard);
            }
        }
    }

    public void appendShard(File shardFile) {
        try {
            this.classLoader.addURL(shardFile.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void loadShards() {
        try {
            Enumeration<URL> shardMetas = this.classLoader.getResources("glass/shardMeta.txt");
            while(shardMetas.hasMoreElements()) {
                URL url = shardMetas.nextElement();
                String shardID = IOUtils.toString(url.openStream());

                boolean alreadyLoaded = this.registeredShards.stream().anyMatch(specification -> specification.getID().equals(shardID));
                if(!alreadyLoaded) {
                    this.registeredShards.add(this.loadShardSpecification("glass/" + shardID + "/info.toml"));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Enumeration<URL> shardMetas = this.classLoader.getResources("glass/shardMeta.txt");
            while(shardMetas.hasMoreElements()) {
                URL url = shardMetas.nextElement();
                String shardID = IOUtils.toString(url.openStream());

                boolean alreadyLoaded = this.shards.stream().anyMatch(info -> info.getSpecification().getID().equals(shardID));
                if(!alreadyLoaded) {
                    ShardInfo shardInfo = this.loadShardInfo("glass/" + shardID + "/info.toml", null);
                    if(shardInfo != null) {
                        this.shards.add(shardInfo);
                        this.registerListeners(shardInfo);
                        this.runHooks("initialize", Collections.singletonList(shardInfo));
                    }
                }
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
        this.runHooks(hook, this.shards);
    }

    public void runHooks(String hook, List<ShardInfo> targets) {
        List<ShardSpecification> executedListeners = new ArrayList<>();
        List<Map.Entry<ShardInfo, Class<? extends Listener>>> listeners = new ArrayList<>(this.listeners.getOrDefault(hook, new ArrayList<>()));
        List<Map.Entry<ShardInfo, Class<? extends Listener>>> filteredListeners = listeners
                .stream()
                .filter(listener -> targets.contains(this.getMainParent(listener.getKey())))
                .collect(Collectors.toList());

        int i = 0;
        while(i < filteredListeners.size()) {
            Map.Entry<ShardInfo, Class<? extends Listener>> listener = filteredListeners.get(i);
            boolean canLoad = true;
            for(ShardSpecification dependency : listener.getKey().getDependencies()) {
                boolean satisfied = true;
                for(Map.Entry<ShardInfo, Class<? extends Listener>> listener1 : filteredListeners) {
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
                filteredListeners.remove(listener);
                filteredListeners.add(listener);
            }
        }
    }

    private ShardInfo getMainParent(ShardInfo shardInfo) {
        if(shardInfo.getParent() != null) {
            return this.getMainParent(shardInfo.getParent());
        }
        return shardInfo;
    }

    @SuppressWarnings("unchecked")
    private ShardInfo loadShardInfo(String path, String overrideID) {
        try {
            InputStream shardInfoStream = this.classLoader.getResourceAsStream(path);
            TomlTable shardInfoTOML = Toml.from(Objects.requireNonNull(shardInfoStream));

            String id = (String) shardInfoTOML.getOrDefault("id", overrideID);
            String version = (String) shardInfoTOML.get("version");
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
                ShardInfo shardInfo = this.loadShardInfo("glass/" + id.replace("-", "/") + "/" + implementationID + "/info.toml", id + "-" + implementationID);
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

    private ShardSpecification loadShardSpecification(String path) {
        try {
            InputStream shardInfoStream = this.classLoader.getResourceAsStream(path);
            TomlTable shardInfoTOML = Toml.from(Objects.requireNonNull(shardInfoStream));

            String id = (String) shardInfoTOML.get("id");
            String version = (String) shardInfoTOML.get("version");
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

    public void registerTransformer(ITransformer transformer) {
        this.classLoader.addTransformer(transformer);
    }

    public List<ShardSpecification> getRegisteredShards() {
        return registeredShards;
    }

    public List<ShardInfo> getShards() {
        return shards;
    }

}
