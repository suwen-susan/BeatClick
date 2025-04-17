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
        // Calculate progress from spawn to hit time
        if (currentTime < spawnTime) {
            yPosition = 0.0f;
        } else if (currentTime > hitTime) {
            yPosition = 1.0f;
        } else {
            float totalTravelTime = hitTime - spawnTime;
            float elapsedTime = currentTime - spawnTime;
            yPosition = elapsedTime / totalTravelTime;
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