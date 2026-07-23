package ir.hanzodev1375.ghostide.postman.model;

/**
 * A named folder of saved requests, e.g. "Auth API" or "Payments".
 */
public class RequestCollection {
    public long id;
    public String name;
    public int requestCount;

    public RequestCollection() {
    }

    public RequestCollection(long id, String name, int requestCount) {
        this.id = id;
        this.name = name;
        this.requestCount = requestCount;
    }
}
