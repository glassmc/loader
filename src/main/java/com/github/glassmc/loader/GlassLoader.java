package com.github.glassmc.loader;

import com.github.glassmc.loader.exception.NoSuchApiException;
import com.github.glassmc.loader.exception.NoSuchInterfaceException;
import com.github.glassmc.loader.util.GlassProperty;
import com.github.glassmc.loader.util.ShardInfoParser;
import com.github.glassmc.loader.loader.ITransformer;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class GlassLoader {

    private static final GlassLoader INSTANCE = new GlassLoader();

    public static GlassLoader getInstance() {
        return INSTANCE;
    }

    private final Properties glassProperties = loadProperties();

    private final File shardsHome = new File("shards");
    private final ClassLoader classLoader = GlassLoader.class.getClassLoader();

    private final List<ShardSpecification> registeredShards = new ArrayList<>();
    private final List<ShardSpecification> virtualShards = new ArrayList<>();
    private final List<ShardInfo> shards = new ArrayList<>();

    private final Map<String, List<Map.Entry<ShardInfo, String>>> listeners = new HashMap<>();
    private final List<Object> apis = new ArrayList<>();
    private final Map<Class<?>, Object> interfaces = new HashMap<>();

    private String[] programArguments;

    private GlassLoader() {
        this.registerVirtualShard(new ShardSpecification("loader", "0.6.3"));

        Runtime.getRuntime().addShutdownHook(new Thread(this::saveProperties));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.runHooks("terminate")));
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        properties.setProperty("shardsFile", "shards");

        try {
            InputStream glassProperties = new FileInputStream("glass.properties");
            properties.load(glassProperties);
        } catch (IOException ignored) { }

        return properties;
    }

    private void saveProperties() {
        try {
            this.glassProperties.store(new FileOutputStream("glass.properties"), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendExternalShards() {
        if(this.getShardsFile().exists()) {
            for(File shard : Objects.requireNonNull(this.getShardsFile().listFiles())) {
                this.addURL(shard);
            }
        }
    }

    public void addURL(File shardFile) {
        try {
            this.invokeClassloaderMethod("addURL", shardFile.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void removeURL(File shardFile) {
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
                    this.registeredShards.add(ShardInfoParser.loadShardSpecification("glass/" + shardID + "/info.toml"));
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
            List<String> parsedShardIds = new ArrayList<>();

            List<ShardInfo> newShards = new ArrayList<>();
            while(shardMetas.hasMoreElements()) {
                URL url = shardMetas.nextElement();
                String shardID = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);
                if (parsedShardIds.contains(shardID)) continue;
                parsedShardIds.add(shardID);

                unloadedShards.removeIf(info -> info.getSpecification().getID().equals(shardID));

                boolean alreadyLoaded = this.shards.stream().anyMatch(info -> info.getSpecification().getID().equals(shardID));
                if(!alreadyLoaded) {
                    ShardInfo shardInfo = ShardInfoParser.loadShardInfo("glass/" + shardID + "/info.toml", null, this.registeredShards);
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

        Map<String, List<String>> listeners = shardInfo.getListeners();
        for(String hook : listeners.keySet()) {
            for(String listener : listeners.get(hook)) {
                this.listeners.computeIfAbsent(hook, k -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(shardInfo, listener));
            }
        }
    }

    private void unregisterListeners(ShardInfo shardInfo) {
        for(ShardInfo implementation : shardInfo.getImplementations()) {
            this.unregisterListeners(implementation);
        }

        for(Map.Entry<String, List<Map.Entry<ShardInfo, String>>> listener : listeners.entrySet()) {
            listener.getValue().removeIf(entry -> entry.getKey().equals(shardInfo));
        }
    }

    public void runHooks(String hook) {
        this.runHooks(hook, this.shards);
    }

    public void runHooks(String hook, List<ShardInfo> targets) {
        List<ShardSpecification> executedListeners = new ArrayList<>();
        List<Map.Entry<ShardInfo, String>> listeners = new ArrayList<>(this.listeners.getOrDefault(hook, new ArrayList<>()));
        List<Map.Entry<ShardInfo, String>> filteredListeners = listeners
                .stream()
                .filter(listener -> targets.contains(this.getMainParent(listener.getKey())))
                .collect(Collectors.toList());

        int i = 0;
        while(i < filteredListeners.size()) {
            Map.Entry<ShardInfo, String> listener = filteredListeners.get(i);
            boolean canLoad = true;
            for(ShardSpecification shardSpecification : getHas(listener.getKey())) {
                boolean satisfied = true;
                for(Map.Entry<ShardInfo, String> listener1 : filteredListeners) {
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
                    Listener listener1 = (Listener) Class.forName(listener.getValue()).getConstructor().newInstance();
                    listener1.run();
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassNotFoundException e) {
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


    public void registerVirtualShard(ShardSpecification specification) {
        this.registeredShards.add(specification);
        this.virtualShards.add(specification);
    }

    public void setProgramArguments(String[] programArguments) {
        this.programArguments = programArguments;
    }

    public String[] getProgramArguments() {
        return programArguments;
    }

    public void registerAPI(Object api) {
        this.apis.add(api);
    }

    public <T> T getAPI(Class<T> apiClass) {
        for(Object api : this.apis) {
            if(apiClass.isInstance(api)) {
                return apiClass.cast(api);
            }
        }
        throw new NoSuchApiException(apiClass);
    }

    public <T> void registerInterface(Class<T> interfaceClass, T implementer) {
        this.interfaces.put(interfaceClass, implementer);
    }

    public <T> T getInterface(Class<T> interfaceClass) {
        Object interfaceObject = this.interfaces.get(interfaceClass);
        if (interfaceObject == null) {
            throw new NoSuchInterfaceException(interfaceClass);
        }
        return interfaceClass.cast(interfaceObject);
    }

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

    public List<ShardSpecification> getRegisteredShards() {
        return registeredShards;
    }

    public List<ShardInfo> getShards() {
        return shards;
    }

    public File getShardsFile() {
        return new File(this.glassProperties.getProperty(GlassProperty.SHARDS_FILE));
    }

    public File getShardsHome() {
        return shardsHome;
    }

    public void setProperty(String key, String value) {
        this.glassProperties.setProperty(key, value);
    }

    public String getProperty(String key) {
        return this.glassProperties.getProperty(key);
    }

}
