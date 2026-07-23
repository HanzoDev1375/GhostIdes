package ir.hanzodev1375.ghostide.postman.model;

/**
 * One row from the local request history database.
 */
public class HistoryItem {

    public long id;
    public String method;
    public String url;
    public int statusCode;   // 0 if the request failed before a response arrived
    public long timeMs;
    public long timestamp;   // System.currentTimeMillis() when the request was sent
    public String requestJson; // serialized RequestSnapshot, so it can be reloaded

    public HistoryItem() {
    }

    public HistoryItem(long id, String method, String url, int statusCode, long timeMs, long timestamp, String requestJson) {
        this.id = id;
        this.method = method;
        this.url = url;
        this.statusCode = statusCode;
        this.timeMs = timeMs;
        this.timestamp = timestamp;
        this.requestJson = requestJson;
    }
}
