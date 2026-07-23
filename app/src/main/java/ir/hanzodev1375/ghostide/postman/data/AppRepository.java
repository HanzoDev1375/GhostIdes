package ir.hanzodev1375.ghostide.postman.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import ir.hanzodev1375.ghostide.postman.model.HistoryItem;
import ir.hanzodev1375.ghostide.postman.model.RequestCollection;
import ir.hanzodev1375.ghostide.postman.model.SavedRequest;

/**
 * Small synchronous data-access layer over {@link DbHelper}. Callers are
 * expected to invoke these off the main thread (a plain background Thread
 * is enough for this app's needs — the tables are tiny).
 */
public class AppRepository {

    private final DbHelper dbHelper;

    public AppRepository(Context context) {
        this.dbHelper = DbHelper.getInstance(context);
    }

    // ------------------------------------------------------------- History

    public long insertHistory(HistoryItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("method", item.method);
        values.put("url", item.url);
        values.put("status_code", item.statusCode);
        values.put("time_ms", item.timeMs);
        values.put("timestamp", item.timestamp);
        values.put("request_json", item.requestJson);
        return db.insert(DbHelper.TABLE_HISTORY, null, values);
    }

    public List<HistoryItem> getHistory() {
        List<HistoryItem> results = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbHelper.TABLE_HISTORY,
                null,
                null, null, null, null,
                "timestamp DESC",
                "300")) {
            while (cursor.moveToNext()) {
                HistoryItem item = new HistoryItem();
                item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                item.method = cursor.getString(cursor.getColumnIndexOrThrow("method"));
                item.url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
                item.statusCode = cursor.getInt(cursor.getColumnIndexOrThrow("status_code"));
                item.timeMs = cursor.getLong(cursor.getColumnIndexOrThrow("time_ms"));
                item.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                item.requestJson = cursor.getString(cursor.getColumnIndexOrThrow("request_json"));
                results.add(item);
            }
        }
        return results;
    }

    public void deleteHistory(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DbHelper.TABLE_HISTORY, "id=?", new String[]{String.valueOf(id)});
    }

    public void clearHistory() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DbHelper.TABLE_HISTORY, null, null);
    }

    // --------------------------------------------------------- Collections

    public List<RequestCollection> getCollections() {
        List<RequestCollection> results = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT c.id, c.name, " +
                "(SELECT COUNT(*) FROM " + DbHelper.TABLE_SAVED_REQUESTS + " r WHERE r.collection_id = c.id) as cnt " +
                "FROM " + DbHelper.TABLE_COLLECTIONS + " c ORDER BY c.name COLLATE NOCASE ASC";
        try (Cursor cursor = db.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                RequestCollection collection = new RequestCollection();
                collection.id = cursor.getLong(0);
                collection.name = cursor.getString(1);
                collection.requestCount = cursor.getInt(2);
                results.add(collection);
            }
        }
        return results;
    }

    /** Returns the id of the collection, creating it first if a collection with that name doesn't exist yet. */
    public long getOrCreateCollection(String name) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try (Cursor cursor = db.query(DbHelper.TABLE_COLLECTIONS, new String[]{"id"},
                "name=?", new String[]{name}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        ContentValues values = new ContentValues();
        values.put("name", name);
        return db.insert(DbHelper.TABLE_COLLECTIONS, null, values);
    }

    public void deleteCollection(long collectionId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DbHelper.TABLE_SAVED_REQUESTS, "collection_id=?", new String[]{String.valueOf(collectionId)});
        db.delete(DbHelper.TABLE_COLLECTIONS, "id=?", new String[]{String.valueOf(collectionId)});
    }

    // ----------------------------------------------------- Saved requests

    public long insertSavedRequest(SavedRequest request) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (request.collectionId > 0) {
            values.put("collection_id", request.collectionId);
        } else {
            values.putNull("collection_id");
        }
        values.put("name", request.name);
        values.put("method", request.method);
        values.put("url", request.url);
        values.put("request_json", request.requestJson);
        return db.insert(DbHelper.TABLE_SAVED_REQUESTS, null, values);
    }

    public List<SavedRequest> getSavedRequests(long collectionId) {
        List<SavedRequest> results = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                DbHelper.TABLE_SAVED_REQUESTS,
                null,
                "collection_id=?", new String[]{String.valueOf(collectionId)},
                null, null,
                "name COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                SavedRequest item = new SavedRequest();
                item.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
                item.collectionId = cursor.getLong(cursor.getColumnIndexOrThrow("collection_id"));
                item.name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                item.method = cursor.getString(cursor.getColumnIndexOrThrow("method"));
                item.url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
                item.requestJson = cursor.getString(cursor.getColumnIndexOrThrow("request_json"));
                results.add(item);
            }
        }
        return results;
    }

    public void deleteSavedRequest(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DbHelper.TABLE_SAVED_REQUESTS, "id=?", new String[]{String.valueOf(id)});
    }
}
