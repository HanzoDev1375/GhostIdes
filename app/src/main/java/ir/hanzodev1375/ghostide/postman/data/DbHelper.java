package ir.hanzodev1375.ghostide.postman.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Plain SQLite storage for request history, collections and saved requests.
 * No ORM on purpose — this is a small, well understood schema and raw
 * SQLiteDatabase keeps the dependency list short.
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "ghostide.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_HISTORY = "history";
    public static final String TABLE_COLLECTIONS = "collections";
    public static final String TABLE_SAVED_REQUESTS = "saved_requests";

    private static DbHelper instance;

    public static synchronized DbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "method TEXT NOT NULL," +
                "url TEXT NOT NULL," +
                "status_code INTEGER NOT NULL," +
                "time_ms INTEGER NOT NULL," +
                "timestamp INTEGER NOT NULL," +
                "request_json TEXT NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_COLLECTIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_SAVED_REQUESTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "collection_id INTEGER," +
                "name TEXT NOT NULL," +
                "method TEXT NOT NULL," +
                "url TEXT NOT NULL," +
                "request_json TEXT NOT NULL," +
                "FOREIGN KEY(collection_id) REFERENCES " + TABLE_COLLECTIONS + "(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE INDEX idx_history_timestamp ON " + TABLE_HISTORY + "(timestamp)");
        db.execSQL("CREATE INDEX idx_saved_requests_collection ON " + TABLE_SAVED_REQUESTS + "(collection_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAVED_REQUESTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_COLLECTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }
}
