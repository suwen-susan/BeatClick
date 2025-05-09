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
            // if the note hasn't spawned yet, set yPosition to 0.0
            yPosition = 0.0f;
        } 
        else {
            // 1) calculate the travel time from spawn to hit
            float travelTime = hitTime - spawnTime;               // = NOTE_TRAVEL_TIME_MS

            // 2) based on the target position, calculate the total time to reach the target position
            //    so that at hitTime, the note is at the target position
            float totalTime = travelTime / TARGET_POSITION;

            // 3) calculate the elapsed time since spawn
            float elapsed = currentTime - spawnTime;

            // 4) progerss between 0.0 and 1.0
            float progress = elapsed / totalTime;
            if (progress > 1.0f) progress = 1.0f;

            // 5) calculate the Y position based on progress
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