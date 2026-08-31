package reader.aigd;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataManager {

    private static final String TAG = "DataManager";
    private static final String PREFS_NAME = "reader_prefs";  // Changed from caster_prefs
    private static final String KEY_SCRIPTS = "scripts";
    private static final String FIXED_TITLE = "Script";

    private static DataManager instance;
    private final SharedPreferences prefs;
    private List<Script> scriptCache;
    private boolean cacheValid = false;

    private DataManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        scriptCache = new ArrayList<>();
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }

    // =========================================================
    // SAVE METHODS
    // =========================================================

    public boolean saveScript(String content) {
        if (content == null || content.trim().isEmpty()) {
            Log.e(TAG, "Cannot save empty content");
            return false;
        }

        String newContent = content.trim();

        if (isDuplicate(newContent)) {
            Log.d(TAG, "Duplicate script prevented");
            return false;
        }

        Script script = new Script(
                generateId(),
                FIXED_TITLE,
                newContent,
                getCurrentTimestamp(),
                System.currentTimeMillis()
        );

        List<Script> scripts = getAllScripts();
        scripts.add(0, script);

        return saveScriptsList(scripts);
    }

    public boolean saveScript(Script script) {
        if (script == null) {
            return false;
        }

        List<Script> scripts = getAllScripts();

        // Check for duplicate ID
        for (Script existing : scripts) {
            if (existing.getId().equals(script.getId())) {
                return false;
            }
        }

        scripts.add(0, script);
        return saveScriptsList(scripts);
    }

    // =========================================================
    // UPDATE METHODS
    // =========================================================

    public boolean updateScript(String scriptId, String newContent) {
        if (scriptId == null || newContent == null || newContent.trim().isEmpty()) {
            return false;
        }

        List<Script> scripts = getAllScripts();

        for (int i = 0; i < scripts.size(); i++) {
            if (scripts.get(i).getId().equals(scriptId)) {
                Script script = scripts.get(i);
                script.setContent(newContent.trim());
                script.setLastEdited(getCurrentTimestamp());
                script.setTimestampMillis(System.currentTimeMillis());
                scripts.set(i, script);
                return saveScriptsList(scripts);
            }
        }

        return false;
    }

    public boolean updateScript(String scriptId, String title, String newContent) {
        // Title is ignored - always use FIXED_TITLE
        return updateScript(scriptId, newContent);
    }

    // =========================================================
    // DELETE METHOD
    // =========================================================

    public boolean deleteScript(String scriptId) {
        if (scriptId == null) {
            return false;
        }

        List<Script> scripts = getAllScripts();

        for (int i = 0; i < scripts.size(); i++) {
            if (scripts.get(i).getId().equals(scriptId)) {
                scripts.remove(i);
                return saveScriptsList(scripts);
            }
        }

        return false;
    }

    // =========================================================
    // GET METHODS
    // =========================================================

    public Script getScript(String scriptId) {
        if (scriptId == null) {
            return null;
        }

        for (Script script : getAllScripts()) {
            if (script.getId().equals(scriptId)) {
                return script;
            }
        }
        return null;
    }

    public List<Script> getAllScripts() {
        if (cacheValid && scriptCache != null) {
            return new ArrayList<>(scriptCache);
        }

        List<Script> scripts = new ArrayList<>();
        String json = prefs.getString(KEY_SCRIPTS, "");

        if (!json.isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                Map<String, Script> uniqueMap = new LinkedHashMap<>();

                for (int i = 0; i < array.length(); i++) {
                    Script script = Script.fromJson(array.getJSONObject(i));
                    // Ensure title is always FIXED_TITLE
                    script.setTitle(FIXED_TITLE);
                    uniqueMap.put(script.getId(), script);
                }

                scripts.addAll(uniqueMap.values());
            } catch (JSONException e) {
                Log.e(TAG, "JSON parse error", e);
            }
        }

        scriptCache = new ArrayList<>(scripts);
        cacheValid = true;
        return scripts;
    }

    public List<Script> getAllScripts(String sortType) {
        List<Script> scripts = getAllScripts();

        if ("newest".equals(sortType)) {
            scripts.sort((a, b) -> Long.compare(b.getTimestampMillis(), a.getTimestampMillis()));
        } else if ("oldest".equals(sortType)) {
            scripts.sort((a, b) -> Long.compare(a.getTimestampMillis(), b.getTimestampMillis()));
        }

        return scripts;
    }

    public List<Script> searchScripts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllScripts();
        }

        String searchQuery = query.toLowerCase().trim();
        List<Script> results = new ArrayList<>();

        for (Script script : getAllScripts()) {
            String content = script.getContent();
            if (content != null && content.toLowerCase().contains(searchQuery)) {
                results.add(script);
            }
        }

        return results;
    }

    // =========================================================
    // UTILITY METHODS
    // =========================================================

    public int getScriptCount() {
        return getAllScripts().size();
    }

    public boolean hasScripts() {
        return getScriptCount() > 0;
    }

    public void clearAllData() {
        prefs.edit().clear().apply();
        scriptCache.clear();
        cacheValid = false;
        Log.d(TAG, "All data cleared");
    }

    private boolean isDuplicate(String content) {
        for (Script script : getAllScripts()) {
            String existing = script.getContent();
            if (existing != null && existing.trim().equalsIgnoreCase(content)) {
                return true;
            }
        }
        return false;
    }

    private boolean saveScriptsList(List<Script> scripts) {
        try {
            Map<String, Script> uniqueMap = new LinkedHashMap<>();

            for (Script script : scripts) {
                script.setTitle(FIXED_TITLE);
                uniqueMap.put(script.getId(), script);
            }

            JSONArray array = new JSONArray();
            for (Script script : uniqueMap.values()) {
                array.put(script.toJson());
            }

            boolean success = prefs.edit()
                    .putString(KEY_SCRIPTS, array.toString())
                    .commit();

            if (success) {
                scriptCache = new ArrayList<>(uniqueMap.values());
                cacheValid = true;
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Save error", e);
            return false;
        }
    }

    private String generateId() {
        return "SCRIPT_" + System.currentTimeMillis();
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        return "Last edited " + sdf.format(new Date());
    }

    // =========================================================
    // SCRIPT MODEL CLASS
    // =========================================================

    public static class Script {
        private final String id;
        private String title;
        private String content;
        private String lastEdited;
        private long timestampMillis;

        public Script(String id, String title, String content, String lastEdited, long timestampMillis) {
            this.id = id;
            this.title = title != null ? title : FIXED_TITLE;
            this.content = content;
            this.lastEdited = lastEdited;
            this.timestampMillis = timestampMillis;
        }

        public static Script fromJson(JSONObject obj) throws JSONException {
            String content = obj.has("content") ? obj.getString("content") : "";
            String lastEdited = obj.has("lastEdited") ? obj.getString("lastEdited") : "Last edited now";
            long timestamp = obj.has("timestampMillis") ? obj.getLong("timestampMillis") : System.currentTimeMillis();

            return new Script(
                    obj.getString("id"),
                    FIXED_TITLE,
                    content,
                    lastEdited,
                    timestamp
            );
        }

        // Helper method to get current timestamp
        private static String getCurrentTimestamp() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            return "Last edited " + sdf.format(new Date());
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title != null ? title : FIXED_TITLE;
        }

        public void setTitle(String title) {
            this.title = title != null ? title : FIXED_TITLE;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getLastEdited() {
            return lastEdited;
        }

        public void setLastEdited(String lastEdited) {
            this.lastEdited = lastEdited;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }

        public void setTimestampMillis(long timestampMillis) {
            this.timestampMillis = timestampMillis;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Script script = (Script) obj;
            return id.equals(script.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", id);
                obj.put("title", FIXED_TITLE);
                obj.put("content", content != null ? content : "");
                obj.put("lastEdited", lastEdited != null ? lastEdited : getCurrentTimestamp());
                obj.put("timestampMillis", timestampMillis);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return obj;
        }
    }
}