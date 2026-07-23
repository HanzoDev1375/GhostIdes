package ir.hanzodev1375.ghostide.postman.model;

/**
 * A single request saved by the user inside a collection (or standalone).
 * requestJson stores a serialized RequestSnapshot (method, url, params,
 * headers, body) so the whole request can be restored in one shot.
 */
public class SavedRequest {
    public long id;
    public long collectionId;
    public String name;
    public String method;
    public String url;
    public String requestJson;

    public SavedRequest() {
    }

    public SavedRequest(long id, long collectionId, String name, String method, String url, String requestJson) {
        this.id = id;
        this.collectionId = collectionId;
        this.name = name;
        this.method = method;
        this.url = url;
        this.requestJson = requestJson;
    }
}
