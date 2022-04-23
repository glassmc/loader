package com.github.glassmc.loader.api.loader;

/**
 * Allows for a shard to request a specific ordering for a transformer, determining if it is run before or after shards also transforming a specific class.
 */
public enum TransformerOrder {
    FIRST,
    LAST,
    DEFAULT
}
