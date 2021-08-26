package com.github.glassmc.loader;

import com.github.glassmc.loader.exception.NoSuchInterfaceException;
import com.github.glassmc.loader.exception.NoSuchShardException;
import com.github.jezza.Toml;
import com.github.jezza.TomlTable;
import com.github.glassmc.loader.loader.ITransformer;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private final ClassLoader classLoader = GlassLoader.class.getClassLoader();

    private final List<ShardSpecification> registeredShards = new ArrayList<>();
    private final List<ShardSpecification> virtualShards = new ArrayList<>();
    private final List<ShardInfo> shards = new ArrayList<>();

    private final Map<String, List<Map.Entry<ShardInfo, Class<? extends Listener>>>> listeners = new HashMap<>();
    private final List<Object> apis = new ArrayList<>();
    private final Map<Class<?>, Object> interfaces = new HashMap<>();

    private String[] programArguments;

    private GlassLoader() {
        this.registerVirtualShard(new ShardSpecification("loader", "0.5.0"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.runHooks("terminate")));
    }

    public void appendExternalShards() {
        if(this.shardsFile.exists()) {
            for(File shard : Objects.requireNonNull(this.shardsFile.listFiles())) {
                this.appendShard(shard);
            }
        }
    }

    public void appendShard(File shardFile) {
        try {
            this.invokeClassloaderMethod("addURL", shardFile.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public void removeShard(File shardFile) {
        try {
            this.invokeClassloaderMethod("removeURL", shardFile.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void loadUpdateShards() {
        this.loadUpdateRegisteredShards();
        this.loadUpdateShardInfos();
    }

    private void loadUpdateRegisteredShards() {
        try {
            List<ShardSpecification> unloadedShards = new ArrayList<>(this.registeredShards);

            Enumeration<URL> shardMetas = this.classLoader.getResources("glass/shard.meta");
            while (shardMetas.hasMoreElements()) {
                URL url = shardMetas.nextElement();
                String shardID = IOUtils.toString(url.openStream());

                unloadedShards.removeIf(info -> info.getID().equals(shardID));

                boolean alreadyLoaded = this.registeredShards.stream().anyMatch(specification -> specification.getID().equals(shardID));
                if(!alreadyLoaded) {
                    this.registeredShards.add(this.loadShardSpecification("glass/" + shardID + "/info.toml"));
                }
            }

            unloadedShards.removeIf(this.virtualShards::contains);

            for(ShardSpecification shardSpecification : unloadedShards) {
                this.registeredShards.remove(shardSpecification);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUpdateShardInfos() {
        try {
            List<ShardInfo> unloadedShards = new ArrayList<>(this.shards);
            Enumeration<URL> shardMetas = this.classLoader.getResources("glass/shard.meta");

            List<ShardInfo> newShards = new ArrayList<>();
            while(shardMetas.hasMoreElements()) {
                URL url = shardMetas.nextElement();
                String shardID = IOUtils.toString(url.openStream());

                unloadedShards.removeIf(info -> info.getSpecification().getID().equals(shardID));

                boolean alreadyLoaded = this.shards.stream().anyMatch(info -> info.getSpecification().getID().equals(shardID));
                if(!alreadyLoaded) {
                    ShardInfo shardInfo = this.loadShardInfo("glass/" + shardID + "/info.toml", null);
                    if(shardInfo != null) {
                        newShards.add(shardInfo);
                        this.registerListeners(shardInfo);
                    }
                }
            }

            this.shards.addAll(newShards);
            this.runHooks("initialize", newShards);

            for(ShardInfo shardInfo : unloadedShards) {
                this.shards.remove(shardInfo);
                this.runHooks("terminate", Collections.singletonList(shardInfo));
                this.unregisterListeners(shardInfo);
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

    private void unregisterListeners(ShardInfo shardInfo) {
        for(ShardInfo implementation : shardInfo.getImplementations()) {
            this.unregisterListeners(implementation);
        }

        for(Map.Entry<String, List<Map.Entry<ShardInfo, Class<? extends Listener>>>> listener : listeners.entrySet()) {
            listener.getValue().removeIf(entry -> entry.getKey().equals(shardInfo));
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
            for(ShardSpecification shardSpecification : getHas(listener.getKey())) {
                boolean satisfied = true;
                for(Map.Entry<ShardInfo, Class<? extends Listener>> listener1 : filteredListeners) {
                    if(shardSpecification.isSatisfied(listener1.getKey().getSpecification())) {
                        satisfied = false;
                    }
                }

                for(ShardSpecification specification : executedListeners) {
                    if(shardSpecification.isSatisfied(specification)) {
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
                    Listener listener1 = listener.getValue().getConstructor().newInstance();
                    listener1.run();
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                i++;
            } else {
                filteredListeners.remove(listener);
                filteredListeners.add(listener);
            }
        }
    }

    private List<ShardSpecification> getHas(ShardInfo shardInfo) {
        List<ShardSpecification> has = new ArrayList<>(shardInfo.getEnvironment().getHas());
        if(shardInfo.getParent() != null) {
            has.addAll(getHas(shardInfo.getParent()));
        }
        return has;
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
            if (shardInfoStream == null) {
                throw new NoSuchShardException("Failed to load shard at path: " + path + ".");
            }
            TomlTable shardInfoTOML = Toml.from(shardInfoStream);

            String id = (String) shardInfoTOML.getOrDefault("id", overrideID);
            String version = (String) shardInfoTOML.get("version");
            ShardSpecification specification = new ShardSpecification(id, version);

            String namespace = (String) shardInfoTOML.get("namespace");

            TomlTable listenersTOML = (TomlTable) shardInfoTOML.getOrDefault("listeners", new TomlTable());
            Map<String, List<Class<? extends Listener>>> listeners = new HashMap<>();
            for(Map.Entry<String, Object> entry : listenersTOML.entrySet()) {
                String hook = entry.getKey();
                listeners.put(hook, new ArrayList<>());
                List<String> hookListeners = (List<String>) listenersTOML.get(hook);
                for(String hookListener : hookListeners) {
                    if(namespace != null) {
                        hookListener = namespace + "." + hookListener;
                    }

                    try {
                        Class<? extends Listener> listenerClass = (Class<? extends Listener>) Class.forName(hookListener);
                        listeners.get(hook).add(listenerClass);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }

            TomlTable environmentTOML = (TomlTable) shardInfoTOML.getOrDefault("environment", new TomlTable());

            TomlTable hasTOML = (TomlTable) environmentTOML.getOrDefault("has", new TomlTable());
            List<ShardSpecification> has = new ArrayList<>();
            for(Map.Entry<String, Object> shard : hasTOML.entrySet()) {
                String shardID = shard.getKey();
                String shardVersion = (String) shard.getValue();
                has.add(new ShardSpecification(shardID, shardVersion));
            }

            TomlTable lacksTOML = (TomlTable) environmentTOML.getOrDefault("lacks", new TomlTable());
            List<ShardSpecification> lacks = new ArrayList<>();
            for(Map.Entry<String, Object> shard : lacksTOML.entrySet()) {
                String shardID = shard.getKey();
                String shardVersion = (String) shard.getValue();
                lacks.add(new ShardSpecification(shardID, shardVersion));
            }

            List<String> implementationsList = (List<String>) shardInfoTOML.getOrDefault("implementations", new ArrayList<>());
            List<ShardInfo> implementations = new ArrayList<>();
            for(String implementationID : implementationsList) {
                ShardInfo shardInfo = this.loadShardInfo("glass/" + id.replace("-", "/") + "/" + implementationID + "/info.toml", id + "-" + implementationID);
                if(shardInfo != null) {
                    implementations.add(shardInfo);
                }
            }

            for(ShardSpecification shardSpecification : has) {
                boolean satisfied = false;
                for(ShardSpecification shard : this.registeredShards) {
                    if(shardSpecification.isSatisfied(shard)) {
                        satisfied = true;
                    }
                }

                if(!satisfied) {
                    return null;
                }
            }

            for(ShardSpecification shardSpecification : lacks) {
                boolean satisfied = true;
                for(ShardSpecification shard : this.registeredShards) {
                    if(shardSpecification.isSatisfied(shard)) {
                        satisfied = false;
                    }
                }

                if(!satisfied) {
                    return null;
                }
            }

            return new ShardInfo(specification, listeners, new ShardInfo.Environment(has, lacks), implementations);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ShardSpecification loadShardSpecification(String path) {
        try {
            InputStream shardInfoStream = this.classLoader.getResourceAsStream(path);
            if (shardInfoStream == null) {
                throw new NoSuchShardException("Failed to load shard at path: " + path + ".");
            }

            TomlTable shardInfoTOML = Toml.from(shardInfoStream);

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
        this.virtualShards.add(specification);
    }

    public void setProgramArguments(String[] programArguments) {
        this.programArguments = programArguments;
    }

    @SuppressWarnings("unused")
    public String[] getProgramArguments() {
        return programArguments;
    }

    @SuppressWarnings("unused")
    public void registerAPI(Object api) {
        this.apis.add(api);
    }

    @SuppressWarnings("unused")
    public <T> T getAPI(Class<T> clazz) {
        for(Object api : this.apis) {
            if(clazz.isInstance(api)) {
                return clazz.cast(api);
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    public <T> void registerInterface(Class<T> interfaceClass, T implementer) {
        this.interfaces.put(interfaceClass, implementer);
    }

    @SuppressWarnings("unused")
    public <T> T getInterface(Class<T> interfaceClass) {
        Object interfaceObject = this.interfaces.get(interfaceClass);
        if (interfaceObject == null) {
            throw new NoSuchInterfaceException(String.format("Interface of type %s requested, but not present", interfaceClass));
        }
        return interfaceClass.cast(interfaceObject);
    }

    @SuppressWarnings("unused")
    public void registerTransformer(Class<? extends ITransformer> transformer) {
        this.invokeClassloaderMethod("addTransformer", transformer);
    }

    private void invokeClassloaderMethod(String name, Object... args) {
        try {
            Class<?>[] argsClasses = new Class[args.length];
            for(int i = 0 ; i < args.length; i++) {
                argsClasses[i] = args[i].getClass();
            }

            Method removeURL = this.classLoader.getClass().getMethod(name, argsClasses);
            removeURL.invoke(this.classLoader, args);
        } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public List<ShardSpecification> getRegisteredShards() {
        return registeredShards;
    }

    @SuppressWarnings("unused")
    public List<ShardInfo> getShards() {
        return shards;
    }

    @SuppressWarnings("unused")
    public File getShardsFile() {
        return shardsFile;
    }

}
