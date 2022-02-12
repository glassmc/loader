package com.github.glassmc.loader.impl.util;

import com.github.glassmc.loader.impl.ShardInfoImpl;
import com.github.glassmc.loader.impl.ShardSpecification;
import com.github.glassmc.loader.impl.exception.NoSuchShardException;
import com.github.jezza.Toml;
import com.github.jezza.TomlTable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShardInfoParser {

    @SuppressWarnings("unchecked")
    public static ShardInfoImpl loadShardInfo(String path, String overrideID, List<ShardSpecification> registeredShards) {
        ClassLoader classLoader = ShardInfoParser.class.getClassLoader();
        try {
            InputStream shardInfoStream = classLoader.getResourceAsStream(path);
            if (shardInfoStream == null) {
                throw new NoSuchShardException(path);
            }
            TomlTable shardInfoTOML = Toml.from(shardInfoStream);

            String id = (String) shardInfoTOML.getOrDefault("id", overrideID);
            String version = (String) shardInfoTOML.get("version");
            ShardSpecification specification = new ShardSpecification(id, version);

            String namespace = (String) shardInfoTOML.get("namespace");

            TomlTable listenersTOML = (TomlTable) shardInfoTOML.getOrDefault("listeners", new TomlTable());
            Map<String, List<String>> listeners = new HashMap<>();
            for (Map.Entry<String, Object> entry : listenersTOML.entrySet()) {
                String hook = entry.getKey();
                listeners.put(hook, new ArrayList<>());

                Object hookListeners = listenersTOML.get(hook);
                if (hookListeners instanceof List) {
                    for(String hookListener : (List<String>) hookListeners) {
                        if(namespace != null) {
                            hookListener = String.format("%s.%s", namespace, hookListener);
                        }

                        listeners.get(hook).add(hookListener);
                    }
                } else if (hookListeners instanceof String) {
                    String hookListener = String.format("%s.%s", namespace, hookListeners);
                    listeners.get(hook).add(hookListener);
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
            List<ShardInfoImpl> implementations = new ArrayList<>();
            for(String implementationID : implementationsList) {
                ShardInfoImpl shardInfo = loadShardInfo("glass/" + id.replace("-", "/") + "/" + implementationID + "/info.toml", id + "-" + implementationID, registeredShards);
                if(shardInfo != null) {
                    implementations.add(shardInfo);
                }
            }

            for(ShardSpecification shardSpecification : has) {
                boolean satisfied = false;
                for(ShardSpecification shard : registeredShards) {
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
                for(ShardSpecification shard : registeredShards) {
                    if(shardSpecification.isSatisfied(shard)) {
                        satisfied = false;
                    }
                }

                if(!satisfied) {
                    return null;
                }
            }

            return new ShardInfoImpl(specification, listeners, new ShardInfoImpl.EnvironmentImpl(has, lacks), implementations);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ShardSpecification loadShardSpecification(String path) {
        try {
            InputStream shardInfoStream = ShardInfoParser.class.getClassLoader().getResourceAsStream(path);
            if (shardInfoStream == null) {
                throw new NoSuchShardException(path);
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

}
