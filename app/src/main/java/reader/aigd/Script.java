package reader.aigd;

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

    // ==============================================
    // CONSTRUCTORS
    // ==============================================

    // Default constructor
    public Script() {
        this.id = -1;
        this.title = "";
        this.content = "";
        this.lastEdited = getCurrentDateTime();
        this.isFavorite = false;
        this.isPinned = false;
        this.isRecent = true;
        this.openCount = 0;
        this.totalReadingTime = 0;
    }

    // Constructor with title and content
    public Script(String title, String content) {
        this.id = -1;
        this.title = title;
        this.content = content;
        this.lastEdited = getCurrentDateTime();
        this.isFavorite = false;
        this.isPinned = false;
        this.isRecent = true;
        this.openCount = 0;
        this.totalReadingTime = 0;
    }

    // Constructor with title, content, and lastEdited
    public Script(String title, String content, String lastEdited) {
        this.id = -1;
        this.title = title;
        this.content = content;
        this.lastEdited = lastEdited;
        this.isFavorite = false;
        this.isPinned = false;
        this.isRecent = true;
        this.openCount = 0;
        this.totalReadingTime = 0;
    }

    // Constructor with title, content, lastEdited, and time
    public Script(String title, String content, String lastEdited, long totalReadingTime) {
        this.id = -1;
        this.title = title;
        this.content = content;
        this.lastEdited = lastEdited;
        this.isFavorite = false;
        this.isPinned = false;
        this.isRecent = true;
        this.openCount = 0;
        this.totalReadingTime = totalReadingTime;
    }

    // Full constructor
    public Script(long id, String title, String content, String lastEdited,
                  boolean isFavorite, boolean isPinned, boolean isRecent,
                  int openCount, long totalReadingTime) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.lastEdited = lastEdited;
        this.isFavorite = isFavorite;
        this.isPinned = isPinned;
        this.isRecent = isRecent;
        this.openCount = openCount;
        this.totalReadingTime = totalReadingTime;
    }

    // ==============================================
    // GETTERS
    // ==============================================

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public boolean isFavorite() {
        return isFavorite;
    }

    // ==============================================
    // SETTERS
    // ==============================================

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean isRecent() {
        return isRecent;
    }

    public void setRecent(boolean recent) {
        isRecent = recent;
    }

    public int getOpenCount() {
        return openCount;
    }

    public void setOpenCount(int openCount) {
        this.openCount = openCount;
    }

    public long getTotalReadingTime() {
        return totalReadingTime;
    }

    public void setTotalReadingTime(long totalReadingTime) {
        this.totalReadingTime = totalReadingTime;
    }

    // ==============================================
    // UTILITY METHODS
    // ==============================================

    public void incrementOpenCount() {
        this.openCount++;
    }

    public void addReadingTime(long time) {
        this.totalReadingTime += time;
    }

    public void updateTimestamp() {
        this.lastEdited = getCurrentDateTime();
    }

    public boolean hasTitle() {
        return title != null && !title.trim().isEmpty();
    }

    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }

    public String getPreview(int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    public String getFirstLine() {
        if (content == null) return "";
        String[] lines = content.split("\n");
        return lines.length > 0 ? lines[0] : "";
    }

    public int getWordCount() {
        if (content == null || content.isEmpty()) return 0;
        String[] words = content.trim().split("\\s+");
        return words.length;
    }

    public int getCharacterCount() {
        if (content == null) return 0;
        return content.length();
    }

    public int getReadingTimeMinutes() {
        int words = getWordCount();
        // Average reading speed: 200 words per minute
        return Math.max(1, (int) Math.ceil(words / 200.0));
    }

    public boolean isLong() {
        return getWordCount() > 500;
    }

    public boolean isShort() {
        return getWordCount() <= 500;
    }

    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ==============================================
    // OVERRIDE METHODS
    // ==============================================

    @Override
    public String toString() {
        return "Script{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + (content != null ? content.substring(0, Math.min(50, content.length())) + "..." : "null") +
                ", lastEdited='" + lastEdited + '\'' +
                ", isFavorite=" + isFavorite +
                ", isPinned=" + isPinned +
                ", isRecent=" + isRecent +
                ", openCount=" + openCount +
                ", totalReadingTime=" + totalReadingTime +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Script script = (Script) obj;
        return id == script.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}