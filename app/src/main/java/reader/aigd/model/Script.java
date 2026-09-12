package reader.aigd.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Script {

    private long id;
    private String title;
    private String content;
    private String lastEdited;
    private boolean isFavorite;
    private boolean isPinned;
    private boolean isRecent;
    private int openCount;
    private long totalReadingTime;

    public Script() {
        this(-1, "", "", now(), false, false, true, 0, 0);
    }

    public Script(String title, String content) {
        this(-1, title, content, now(), false, false, true, 0, 0);
    }

    // Constructor used by your DataManager (with isRecent)
    public Script(long id, String title, String content, String lastEdited,
                  boolean isFavorite, boolean isPinned, boolean isRecent,
                  int openCount, long totalReadingTime) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.lastEdited = lastEdited == null ? now() : lastEdited;
        this.isFavorite = isFavorite;
        this.isPinned = isPinned;
        this.isRecent = isRecent;
        this.openCount = openCount;
        this.totalReadingTime = totalReadingTime;
    }

    // Extra constructor for compatibility (8 params - without isRecent)
    public Script(long id, String title, String content, String lastEdited,
                  boolean isFavorite, boolean isPinned, int openCount, long totalReadingTime) {
        this(id, title, content, lastEdited, isFavorite, isPinned, true, openCount, totalReadingTime);
    }

    private static String now() {
        return new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(new Date());
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getLastEdited() {
        return lastEdited;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public boolean isRecent() {
        return isRecent;
    }

    public int getOpenCount() {
        return openCount;
    }

    public long getTotalReadingTime() {
        return totalReadingTime;
    }

    // Getters
    public long getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setLastEdited(String lastEdited) {
        this.lastEdited = lastEdited;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public void setRecent(boolean recent) {
        isRecent = recent;
    }

    public void setOpenCount(int openCount) {
        this.openCount = openCount;
    }

    public void setTotalReadingTime(long totalReadingTime) {
        this.totalReadingTime = totalReadingTime;
    }

    // Setters - ALL fixed for DataManager
    public void setId(long id) {
        this.id = id;
    }

    // Helpers
    public String getPreview(int max) {
        if (content.isEmpty()) return "";
        return content.length() <= max ? content : content.substring(0, max).trim() + "...";
    }

    public int getWordCount() {
        if (content.trim().isEmpty()) return 0;
        return content.trim().split("\\s+").length;
    }

    public boolean hasContent() {
        return !content.trim().isEmpty();
    }

    public boolean hasTitle() {
        return !title.trim().isEmpty();
    }

    public void incrementOpenCount() {
        openCount++;
    }

    public void addReadingTime(long time) {
        totalReadingTime += time;
    }

    public void updateTimestamp() {
        lastEdited = now();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Script)) return false;
        return id == ((Script) obj).id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}