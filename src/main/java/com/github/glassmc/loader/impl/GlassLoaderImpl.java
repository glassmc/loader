package com.github.glassmc.loader.impl;

import com.github.glassmc.loader.api.*;
import com.github.glassmc.loader.api.loader.TransformerOrder;
import com.github.glassmc.loader.impl.exception.NoSuchApiException;
import com.github.glassmc.loader.impl.exception.NoSuchInterfaceException;
import com.github.glassmc.loader.impl.util.ShardInfoParser;
import com.github.glassmc.loader.api.loader.Transformer;
import com.github.jezza.Toml;
import com.github.jezza.TomlTable;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class GlassLoaderImpl implements GlassLoader {

    private static final GlassLoaderImpl INSTANCE = new GlassLoaderImpl();

    private final ClassLoader classLoader = GlassLoaderImpl.class.getClassLoader();

    private final List<ShardSpecification> registeredShards = new ArrayList<>();
    private final List<ShardSpecification> virtualShards = new ArrayList<>();
    private final List<ShardInfo> shards = new ArrayList<>();

    private final Map<String, List<Map.Entry<ShardInfo, String>>> listeners = new HashMap<>();
    private final Map<Class<?>, Object> apis = new HashMap<>();
    private final Map<Class<?>, Object> interfaces = new HashMap<>();

    private String[] programArguments;

    private final List<InternalLoader> internalLoaders = new ArrayList<>();

    public GlassLoaderImpl() {
        this.registerVirtualShard(new ShardSpecification("loader", "INSERTED_BY_BUILD"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.runHooks("terminate")));

        this.internalLoaders.add(new InternalLoaderImpl());
    }

    public void preLoad() {
        List<URL> checkedShards = new ArrayList<>();
        List<String> addedShards = new ArrayList<>();
        List<File> classpath = new ArrayList<>();

        for (InternalLoader internalLoader : this.internalLoaders) {
            internalLoader.modifyClassPath(classpath);

            for (File file : classpath) {
                this.addURL(file);
            }
        }

        this.addLoadersClasspath(checkedShards, this.internalLoaders, addedShards, classpath);
    }

    private void addLoadersClasspath(List<URL> checkedShards, List<InternalLoader> loaders, List<String> addedShards, List<File> classpath) {
        try {
            Enumeration<URL> urls = GlassLoaderImpl.class.getClassLoader().getResources("glass/shard.meta");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (!checkedShards.contains(url)) {
                    checkedShards.add(url);

                    String shardName = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);

                    if (!addedShards.contains(shardName)) {
                        addedShards.add(shardName);
                        InputStream inputStream = GlassLoaderImpl.class.getClassLoader().getResourceAsStream("glass/" + shardName + "/info.toml");
                        if (inputStream != null) {
                            TomlTable toml = Toml.from(inputStream);
                            if (toml.get("internal_loader") != null) {
                                String before = "";
                                if (toml.get("namespace") != null) {
                                    before = toml.get("namespace") + ".";
                                }
                                String internalLoaderClass = before + toml.get("internal_loader");

                                InternalLoader internalLoader = (InternalLoader) Class.forName(internalLoaderClass).getConstructor().newInstance();
                                internalLoader.initialize();
                                loaders.add(internalLoader);

                                List<File> classpathBackup = new ArrayList<>(classpath);
                                internalLoader.modifyClassPath(classpath);
                                for (File file : classpath) {
                                    this.addURL(file);
                                }
                                for (File file : classpathBackup) {
                                    if (!classpath.contains(file)) {
                                        this.removeURL(file);
                                    }
                                }

                                this.addLoadersClasspath(checkedShards, loaders, addedShards, classpath);
                            }
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void addURL(File shardFile) {
        try {
            this.invokeClassloaderMethod("addURL", shardFile.toURI().toURL());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void removeURL(File shardFile) {
        try {
            this.invokeClassloaderMethod("removeURL", shardFile.toURI().toURL());
        } catch (Throwable e) {
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
                String shardID = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);

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
            this.registerListeners((ShardInfo) implementation);
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
            this.unregisterListeners((ShardInfo) implementation);
        }

        for(Map.Entry<String, List<Map.Entry<ShardInfo, String>>> listener : listeners.entrySet()) {
            listener.getValue().removeIf(entry -> entry.getKey().equals(shardInfo));
        }
    }

    @Override
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

        executedListeners.add(new ShardSpecification("random", "0.1"));

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

    @Override
    public void registerAPI(Object api) {
        this.apis.put(api.getClass(), api);
    }

    @Override
    public <T> T getAPI(Class<T> apiClass) {
        Object api = this.apis.get(apiClass);
        if (api != null) {
            return (T) api;
        }
        throw new NoSuchApiException(apiClass);
    }

    @Override
    public <T> void registerInterface(Class<T> interfaceClass, T implementor) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalStateException(interfaceClass + " is not an interface!");
        }
        this.interfaces.put(interfaceClass, implementor);
    }

    @Override
    public <T> T getInterface(Class<T> interfaceClass) {
        Object interfaceObject = this.interfaces.get(interfaceClass);
        if (interfaceObject == null) {
            throw new NoSuchInterfaceException(interfaceClass);
        }
        return interfaceClass.cast(interfaceObject);
    }

    @Override
    public void registerTransformer(Class<? extends Transformer> transformer, TransformerOrder order) {
        try {
            this.invokeClassloaderMethod("addTransformer", transformer, order);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] getClassBytes(String name) throws ClassNotFoundException {
        try {
            return (byte[]) this.invokeClassloaderMethod("getModifiedBytes", name);
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getShardVersion(String id) {
        AtomicReference<String> version = new AtomicReference<>(null);
        this.registeredShards.stream().filter(shard -> shard.getID().equals(id)).findFirst().ifPresent(shard -> version.set(shard.getVersion()));
        return version.get();
    }

    private Object invokeClassloaderMethod(String name, Object... args) throws Throwable {
        try {
            Class<?>[] argsClasses = new Class[args.length];
            for(int i = 0 ; i < args.length; i++) {
                argsClasses[i] = args[i].getClass();
            }

            Method method = this.classLoader.getClass().getMethod(name, argsClasses);
            return method.invoke(this.classLoader, args);

        } catch(NoSuchMethodException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    public byte[][] filterClasses(String name, String[] classLocations, byte[][] classDatas) {
        List<ClassDefinition> classDefinitions = new ArrayList<>();

        for (int i = 0; i < classLocations.length; i++) {
            try {
                classDefinitions.add(new ClassDefinition(new URL(classLocations[i]), classDatas[i]));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        for (InternalLoader loader : this.internalLoaders) {
            loader.filterClasses(name, classDefinitions);
        }

        return classDefinitions.stream().map(ClassDefinition::getData).toArray(byte[][]::new);
    }

}
