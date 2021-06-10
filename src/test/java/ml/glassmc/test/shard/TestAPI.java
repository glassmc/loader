package ml.glassmc.test.shard;

public class TestAPI {

    private int counter = 0;

    public void add() {
        this.counter++;
    }

    public int get() {
        return this.counter;
    }

}
