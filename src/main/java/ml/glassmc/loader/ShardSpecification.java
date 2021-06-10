package ml.glassmc.loader;

public class ShardSpecification {

    private final String id;
    private final String version;

    public ShardSpecification(String id, String version) {
        this.id = id;
        this.version = version;
    }

    public boolean isSatisfied(ShardSpecification other) {
        String otherID = other.id;
        String otherVersion = other.version;
        return true;
    }

}
