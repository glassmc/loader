package io.github.glassmc.loader;

import com.github.jezza.Toml;
import com.github.jezza.TomlTable;
import io.github.glassmc.loader.launch.Launcher;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
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

        for(ShardInfo shardInfo : shardInfoFiltered) {
            for(Class<?> listenerClass : shardInfo.getListeners().get(hook)) {
                try {
                    Listener listener = (Listener) listenerClass.newInstance();
                    listener.run();
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<ShardInfo> collectShardInfo() {
        ClassLoader classLoader = GlassLoader.class.getClassLoader();

        List<ShardSpecification> shards = new ArrayList<>();
        try {
            List<URL> shardInfoURLList = new ArrayList<>();

            Enumeration<URL> shardInfos = classLoader.getResources("glass/info.toml");
            while(shardInfos.hasMoreElements()) {
                URL shardInfo = shardInfos.nextElement();
                shardInfoURLList.add(shardInfo);
            }

            for(URL shardInfoURL : shardInfoURLList) {
                InputStream shardInfoStream = shardInfoURL.openStream();
                String shardInfoText = IOUtils.toString(shardInfoStream, StandardCharsets.UTF_8);
                ShardSpecification shardSpecification = this.parseShardSpecification(shardInfoText);
                shards.add(shardSpecification);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        shards.addAll(this.virtualShards);

        List<ShardInfo> shardInfoList = new ArrayList<>();

        try {
            List<URL> shardInfoURLList = new ArrayList<>();

            Enumeration<URL> shardInfos = classLoader.getResources("glass/info.toml");
            while(shardInfos.hasMoreElements()) {
                URL shardInfo = shardInfos.nextElement();
                shardInfoURLList.add(shardInfo);
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

                    if (!found) {
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
            boolean satisfied = true;
            for(ShardSpecification dependency : implementation.getDependencies()) {
                boolean found = false;
                for(ShardSpecification specification : shards) {
                    if(dependency.isSatisfied(specification)) {
                        found = true;
                    }
                }

                if(!found) {
                    satisfied = false;
                }
            }

            if(!satisfied) {
                shardInfo.getImplementations().remove(implementation);
            } else {
                this.cleanImplementations(shards, implementation);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ShardInfo parseShardInfo(String shardInfoText, String overrideID) throws IOException {
        TomlTable shardInfoTOML = Toml.from(new StringReader(shardInfoText));

        TomlTable infoTOML = (TomlTable) shardInfoTOML.getOrDefault("info", new TomlTable());
        String id = (String) infoTOML.getOrDefault("id", overrideID);
        String version = (String) infoTOML.getOrDefault("version", null);
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

        List<String> implementationsList = (List<String>) infoTOML.getOrDefault("implementations", new ArrayList<>());
        List<ShardInfo> implementations = new ArrayList<>();
        for(String implementationID : implementationsList) {
            int index = id.indexOf("-") + 1;
            if(index == 0) {
                index = id.length();
            }
            InputStream shardInfoStream = GlassLoader.class.getClassLoader().getResourceAsStream("glass" + (!id.substring(index).isEmpty() ? "/" : "") + id.substring(index).replace("-", "/") + "/" + implementationID + "/info.toml");
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

    private ShardSpecification parseShardSpecification(String shardInfoText) throws IOException {
        TomlTable shardInfoTOML = Toml.from(new StringReader(shardInfoText));

        String id = (String) shardInfoTOML.get("id");
        String version = (String) shardInfoTOML.get("version");
        return new ShardSpecification(id, version);
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
