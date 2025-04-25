package com.beatclick;

/**
 * Note - Represents a single note in the game
 * Contains timing and position information
 */
public class Note {
    
    private int laneIndex;
    private long spawnTime; // When the note should appear on screen
    private long hitTime;   // When the note should be hit (reach the target line)
    private float yPosition; // Current Y position for rendering (0.0 to 1.0, where 1.0 is bottom)

    private static final float TARGET_POSITION = 0.85f; //和 GamePanel 里的 TARGET_POSITION 保持一致
    /**
     * Constructor
     * @param laneIndex The lane index where this note appears
     * @param spawnTime The time when this note should appear (ms)
     * @param hitTime The ideal time when this note should be hit (ms)
     */
    public Note(int laneIndex, long spawnTime, long hitTime) {
        this.laneIndex = laneIndex;
        this.spawnTime = spawnTime;
        this.hitTime = hitTime;
        this.yPosition = 0.0f; // Start at the top
    }
    
    /**
     * Gets the lane index for this note
     * @return The lane index
     */
    public int getLaneIndex() {
        return laneIndex;
    }
    
    /**
     * Gets the spawn time for this note
     * @return The spawn time in milliseconds
     */
    public long getSpawnTime() {
        return spawnTime;
    }
    
    /**
     * Gets the ideal hit time for this note
     * @return The hit time in milliseconds
     */
    public long getHitTime() {
        return hitTime;
    }
    
    /**
     * Gets the current Y position for rendering
     * @return The Y position (0.0 to 1.0)
     */
    public float getYPosition() {
        return yPosition;
    }
    
    /**
     * Sets the Y position for rendering
     * @param yPosition The Y position (0.0 to 1.0)
     */
    public void setYPosition(float yPosition) {
        this.yPosition = yPosition;
    }
    
    /**
     * Updates the Y position based on current game time
     * @param currentTime The current game time in milliseconds
     */
    public void updatePosition(long currentTime) {
        if (currentTime < spawnTime) {
            // 未到生成时间，始终在顶端
            yPosition = 0.0f;
        } else {
            // 1) 先算出从 spawnTime 到 hitTime 的“旅行时间”
            float travelTime = hitTime - spawnTime;               // = NOTE_TRAVEL_TIME_MS

            // 2) 根据目标线归一化位置，推算出整个从 spawn 到“屏幕底端”所需的总时长
            //    这样 hitTime 恰好对应到 TARGET_POSITION，底端对应到 1.0
            float totalTime = travelTime / TARGET_POSITION;

            // 3) 计算当前经过时间
            float elapsed = currentTime - spawnTime;

            // 4) 进度 = elapsed / totalTime，但不超过 1.0
            float progress = elapsed / totalTime;
            if (progress > 1.0f) progress = 1.0f;

            // 5) 最终 yPosition 就是 0→1 的进度
            yPosition = progress;
        }
    }
    
    /**
     * Calculates the absolute Y coordinate based on panel height
     * @param panelHeight The height of the game panel
     * @return The absolute Y coordinate
     */
    public int calculateYCoordinate(int panelHeight) {
        return (int) (yPosition * panelHeight);
    }
}