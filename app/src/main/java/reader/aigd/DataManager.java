package reader.aigd;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DataManager {

    private static DataManager instance;
    private final DatabaseHelper dbHelper;
    private final Context context;

    private DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = new DatabaseHelper(this.context);
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }

    // ==============================================
    // SAVE METHODS
    // ==============================================

    // Save with title and content
    public long saveScript(String title, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title != null ? title : "Untitled");
        values.put("content", content != null ? content : "");
        values.put("last_edited", getCurrentDateTime());
        values.put("is_favorite", 0);
        values.put("is_pinned", 0);
        values.put("is_recent", 1);
        values.put("open_count", 0);
        values.put("total_reading_time", 0);
        long id = db.insert("scripts", null, values);
        db.close();
        return id;
    }

    // Save with Script object
    public long saveScript(Script script) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", script.getTitle() != null ? script.getTitle() : "Untitled");
        values.put("content", script.getContent() != null ? script.getContent() : "");
        values.put("last_edited", script.getLastEdited());
        values.put("is_favorite", script.isFavorite() ? 1 : 0);
        values.put("is_pinned", script.isPinned() ? 1 : 0);
        values.put("is_recent", script.isRecent() ? 1 : 0);
        values.put("open_count", script.getOpenCount());
        values.put("total_reading_time", script.getTotalReadingTime());
        long id = db.insert("scripts", null, values);
        db.close();
        if (id != -1) {
            script.setId(id);
        }
        return id;
    }

    // ==============================================
    // UPDATE METHODS
    // ==============================================

    // Update with title and content (String id)
    public boolean updateScript(String id, String title, String content) {
        try {
            long longId = Long.parseLong(id);
            return updateScript(longId, title, content);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Update with title and content (long id)
    public boolean updateScript(long id, String title, String content) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title != null ? title : "Untitled");
        values.put("content", content != null ? content : "");
        values.put("last_edited", getCurrentDateTime());
        int rows = db.update("scripts", values, "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    // Update with Script object (String id)
    public boolean updateScript(String id, Script script) {
        try {
            long longId = Long.parseLong(id);
            script.setId(longId);
            return updateScript(script);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // Update with Script object (long id)
    public boolean updateScript(long id, Script script) {
        script.setId(id);
        return updateScript(script);
    }

    // Update with Script object
    public boolean updateScript(Script script) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", script.getTitle() != null ? script.getTitle() : "Untitled");
        values.put("content", script.getContent() != null ? script.getContent() : "");
        values.put("last_edited", script.getLastEdited());
        values.put("is_favorite", script.isFavorite() ? 1 : 0);
        values.put("is_pinned", script.isPinned() ? 1 : 0);
        values.put("is_recent", script.isRecent() ? 1 : 0);
        values.put("open_count", script.getOpenCount());
        values.put("total_reading_time", script.getTotalReadingTime());
        int rows = db.update("scripts", values, "id = ?", new String[]{String.valueOf(script.getId())});
        db.close();
        return rows > 0;
    }

    // Update script title and content with new timestamp
    public boolean updateScriptContent(long id, String title, String content) {
        return updateScript(id, title, content);
    }

    // Update only favorite status
    public boolean updateFavoriteStatus(long id, boolean isFavorite) {
        Script script = getScriptById(id);
        if (script != null) {
            script.setFavorite(isFavorite);
            return updateScript(script);
        }
        return false;
    }

    // Update only pinned status
    public boolean updatePinnedStatus(long id, boolean isPinned) {
        Script script = getScriptById(id);
        if (script != null) {
            script.setPinned(isPinned);
            return updateScript(script);
        }
        return false;
    }

    // ==============================================
    // GET METHODS
    // ==============================================

    // Get script by id (String)
    public Script getScript(String id) {
        try {
            long longId = Long.parseLong(id);
            return getScriptById(longId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Get script by id (long)
    public Script getScriptById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("scripts", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null);

        Script script = null;
        if (cursor.moveToFirst()) {
            script = new Script(
                    cursor.getLong(cursor.getColumnIndex("id")),
                    cursor.getString(cursor.getColumnIndex("title")),
                    cursor.getString(cursor.getColumnIndex("content")),
                    cursor.getString(cursor.getColumnIndex("last_edited")),
                    cursor.getInt(cursor.getColumnIndex("is_favorite")) == 1,
                    cursor.getInt(cursor.getColumnIndex("is_pinned")) == 1,
                    cursor.getInt(cursor.getColumnIndex("is_recent")) == 1,
                    cursor.getInt(cursor.getColumnIndex("open_count")),
                    cursor.getLong(cursor.getColumnIndex("total_reading_time"))
            );
        }
        cursor.close();
        db.close();
        return script;
    }

    // Get all scripts with sort
    public List<Script> getAllScripts(String sortOrder) {
        List<Script> scripts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String orderBy;
        if (sortOrder == null) {
            orderBy = "id DESC";
        } else {
            switch (sortOrder) {
                case "newest":
                    orderBy = "id DESC";
                    break;
                case "oldest":
                    orderBy = "id ASC";
                    break;
                case "title_asc":
                    orderBy = "title ASC";
                    break;
                case "title_desc":
                    orderBy = "title DESC";
                    break;
                default:
                    orderBy = "id DESC";
                    break;
            }
        }

        Cursor cursor = db.query("scripts", null, null, null, null, null, orderBy);

        if (cursor.moveToFirst()) {
            do {
                Script script = new Script();
                script.setId(cursor.getLong(cursor.getColumnIndex("id")));
                script.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                script.setContent(cursor.getString(cursor.getColumnIndex("content")));
                script.setLastEdited(cursor.getString(cursor.getColumnIndex("last_edited")));
                script.setFavorite(cursor.getInt(cursor.getColumnIndex("is_favorite")) == 1);
                script.setPinned(cursor.getInt(cursor.getColumnIndex("is_pinned")) == 1);
                script.setRecent(cursor.getInt(cursor.getColumnIndex("is_recent")) == 1);
                script.setOpenCount(cursor.getInt(cursor.getColumnIndex("open_count")));
                script.setTotalReadingTime(cursor.getLong(cursor.getColumnIndex("total_reading_time")));
                scripts.add(script);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scripts;
    }

    // Get all scripts (default sort: newest first)
    public List<Script> getAllScripts() {
        return getAllScripts("newest");
    }

    // Get favorite scripts
    public List<Script> getFavoriteScripts() {
        List<Script> scripts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("scripts", null, "is_favorite = 1", null, null, null, "id DESC");

        if (cursor.moveToFirst()) {
            do {
                Script script = new Script();
                script.setId(cursor.getLong(cursor.getColumnIndex("id")));
                script.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                script.setContent(cursor.getString(cursor.getColumnIndex("content")));
                script.setLastEdited(cursor.getString(cursor.getColumnIndex("last_edited")));
                script.setFavorite(true);
                script.setPinned(cursor.getInt(cursor.getColumnIndex("is_pinned")) == 1);
                script.setRecent(cursor.getInt(cursor.getColumnIndex("is_recent")) == 1);
                script.setOpenCount(cursor.getInt(cursor.getColumnIndex("open_count")));
                script.setTotalReadingTime(cursor.getLong(cursor.getColumnIndex("total_reading_time")));
                scripts.add(script);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scripts;
    }

    // Get pinned scripts
    public List<Script> getPinnedScripts() {
        List<Script> scripts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("scripts", null, "is_pinned = 1", null, null, null, "id DESC");

        if (cursor.moveToFirst()) {
            do {
                Script script = new Script();
                script.setId(cursor.getLong(cursor.getColumnIndex("id")));
                script.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                script.setContent(cursor.getString(cursor.getColumnIndex("content")));
                script.setLastEdited(cursor.getString(cursor.getColumnIndex("last_edited")));
                script.setFavorite(cursor.getInt(cursor.getColumnIndex("is_favorite")) == 1);
                script.setPinned(true);
                script.setRecent(cursor.getInt(cursor.getColumnIndex("is_recent")) == 1);
                script.setOpenCount(cursor.getInt(cursor.getColumnIndex("open_count")));
                script.setTotalReadingTime(cursor.getLong(cursor.getColumnIndex("total_reading_time")));
                scripts.add(script);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scripts;
    }

    // Search scripts by query
    public List<Script> searchScripts(String query) {
        List<Script> scripts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String searchQuery = "%" + query + "%";
        Cursor cursor = db.query("scripts", null,
                "title LIKE ? OR content LIKE ?",
                new String[]{searchQuery, searchQuery},
                null, null, "id DESC");

        if (cursor.moveToFirst()) {
            do {
                Script script = new Script();
                script.setId(cursor.getLong(cursor.getColumnIndex("id")));
                script.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                script.setContent(cursor.getString(cursor.getColumnIndex("content")));
                script.setLastEdited(cursor.getString(cursor.getColumnIndex("last_edited")));
                script.setFavorite(cursor.getInt(cursor.getColumnIndex("is_favorite")) == 1);
                script.setPinned(cursor.getInt(cursor.getColumnIndex("is_pinned")) == 1);
                script.setRecent(cursor.getInt(cursor.getColumnIndex("is_recent")) == 1);
                script.setOpenCount(cursor.getInt(cursor.getColumnIndex("open_count")));
                script.setTotalReadingTime(cursor.getLong(cursor.getColumnIndex("total_reading_time")));
                scripts.add(script);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return scripts;
    }

    // ==============================================
    // DELETE METHODS
    // ==============================================

    public boolean deleteScript(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete("scripts", "id = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows > 0;
    }

    public boolean deleteScript(String id) {
        try {
            long longId = Long.parseLong(id);
            return deleteScript(longId);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void deleteAllScripts() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("scripts", null, null);
        db.close();
    }

    // ==============================================
    // UTILITY METHODS
    // ==============================================

    public void incrementOpenCount(long id) {
        Script script = getScriptById(id);
        if (script != null) {
            script.incrementOpenCount();
            updateScript(script);
        }
    }

    public void incrementOpenCount(String id) {
        try {
            long longId = Long.parseLong(id);
            incrementOpenCount(longId);
        } catch (NumberFormatException e) {
            // Invalid id
        }
    }

    public int getScriptCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM scripts", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getFavoriteCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM scripts WHERE is_favorite = 1", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getPinnedCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM scripts WHERE is_pinned = 1", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public long getTotalReadingTime() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(total_reading_time) FROM scripts", null);
        long total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ==============================================
    // DATABASE HELPER CLASS
    // ==============================================

    private static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "scripts.db";
        private static final int DATABASE_VERSION = 2;

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE scripts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "content TEXT," +
                    "last_edited TEXT," +
                    "is_favorite INTEGER DEFAULT 0," +
                    "is_pinned INTEGER DEFAULT 0," +
                    "is_recent INTEGER DEFAULT 1," +
                    "open_count INTEGER DEFAULT 0," +
                    "total_reading_time INTEGER DEFAULT 0)";
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                try {
                    db.execSQL("ALTER TABLE scripts ADD COLUMN is_favorite INTEGER DEFAULT 0");
                    db.execSQL("ALTER TABLE scripts ADD COLUMN is_pinned INTEGER DEFAULT 0");
                    db.execSQL("ALTER TABLE scripts ADD COLUMN is_recent INTEGER DEFAULT 1");
                    db.execSQL("ALTER TABLE scripts ADD COLUMN open_count INTEGER DEFAULT 0");
                    db.execSQL("ALTER TABLE scripts ADD COLUMN total_reading_time INTEGER DEFAULT 0");
                } catch (Exception e) {
                    // Columns might already exist
                }
            }
        }
    }
}