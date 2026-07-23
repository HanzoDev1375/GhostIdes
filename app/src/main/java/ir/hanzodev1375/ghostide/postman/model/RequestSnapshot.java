package ir.hanzodev1375.ghostide.postman.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A full, serializable snapshot of everything needed to reconstruct a
 * request: method, URL, params, headers and body. Gson turns this into
 * JSON for storage in history/collections and back again when reloaded.
 */
public class RequestSnapshot {
    public String method = "GET";
    public String url = "";
    public List<KeyValueItem> params = new ArrayList<>();
    public List<KeyValueItem> headers = new ArrayList<>();
    public int bodyType = 0; // 0 = none, 1 = raw, 2 = form url-encoded
    public String rawBody = "";
    public String rawContentType = "application/json";
    public List<KeyValueItem> formFields = new ArrayList<>();
}
