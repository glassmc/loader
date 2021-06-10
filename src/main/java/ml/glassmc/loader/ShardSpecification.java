package ml.glassmc.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShardSpecification {

    private final String id;
    private final String version;

    public ShardSpecification(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public String getID() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public boolean isSatisfied(ShardSpecification other) {
        if(!this.id.equals(other.id)) {
            return false;
        }

        Version thisVersion = new Version(this.version.replaceAll("[^0-9.,x]+", ""));
        Version otherVersion = new Version(other.version);
        if(this.version.startsWith(">")) {
            if(otherVersion.compare(thisVersion) == Version.ComparisonResult.GREATER) {
                return true;
            }
        }
        if(this.version.startsWith("<")) {
            if(otherVersion.compare(thisVersion) == Version.ComparisonResult.LESS) {
                return true;
            }
        }
        if(!(this.version.contains("<") || this.version.contains(">")) || this.version.contains("=")) {
            return otherVersion.compare(thisVersion) == Version.ComparisonResult.EQUAL;
        }

        return false;
    }

    private static class Version {

        private final List<String> separated = new ArrayList<>();

        private Version(String versionString) {
            this.separated.addAll(Arrays.asList(versionString.split("\\.")));
        }

        private ComparisonResult compare(Version other) {
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
            GREATER,
            LESS,
            EQUAL
        }

    }

}
