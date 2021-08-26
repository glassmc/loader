package com.github.glassmc.loader.exception;

public class NoSuchShardException extends RuntimeException {

    public NoSuchShardException(String path) {
        super(String.format("Shard expected, but not present at path: %s.", path));
    }

}
