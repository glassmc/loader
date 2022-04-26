package com.github.glassmc.loader.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ShardSpecification {

    private final String id;
    private final String version;

    public ShardSpecification(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public boolean isSatisfied(ShardSpecification other) {
        if(!id.equals(other.getID()) && !id.equals("all")) {
            return false;
        }
        if(this.version.equals("*")) {
            return true;
        }

        Version thisVersion = new Version(this.version.replaceAll("[^0-9.,x]+", ""));
        Version otherVersion = new Version(other.version);
        Version.ComparisonResult comparisonResult = otherVersion.compare(thisVersion);
        if(this.version.startsWith(">")) {
            if(comparisonResult == Version.ComparisonResult.GREATER) {
                return true;
            }
        }
        if(this.version.startsWith("<")) {
            if(comparisonResult == Version.ComparisonResult.LESS) {
                return true;
            }
        }
        if(!(this.version.contains("<") || this.version.contains(">")) || this.version.contains("=")) {
            return comparisonResult == Version.ComparisonResult.EQUAL;
        }
        return false;
    }

    public String getID() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ShardSpecification{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    private static class Version {

        private final List<String> separated = new ArrayList<>();

        public Version(String versionString) {
            this.separated.addAll(Arrays.asList(versionString.split("\\.")));
        }

        public ComparisonResult compare(Version other) {
            int maxSize = Math.max(this.separated.size(), other.separated.size());
            for(int i = 0; i < maxSize; i++) {
                String thisNumberString = this.separated.size() > i ? this.separated.get(i) : "0";
                String otherNumberString = other.separated.size() > i ? other.separated.get(i) : "0";

                if(!thisNumberString.equals("x") && !otherNumberString.equals("x")) {
                    int thisNumber = Integer.parseInt(thisNumberString);
                    int otherNumber = Integer.parseInt(otherNumberString);

                    if(thisNumber > otherNumber) {
                        return ComparisonResult.GREATER;
                    }
                    if(thisNumber < otherNumber) {
                        return ComparisonResult.LESS;
                    }
                }
            }
            return ComparisonResult.EQUAL;
        }

        private enum ComparisonResult {
            GREATER, LESS, EQUAL
        }

    }

}
