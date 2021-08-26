package com.github.glassmc.loader.exception;

public class NoSuchApiException extends RuntimeException {

    public NoSuchApiException(Class<?> apiClass) {
        super(String.format("API of type %s requested, but not present", apiClass.getName()));
    }

}
