package com.beatclick;

/**
 * Note - Represents a single note in the game
 * Contains timing and position information
 */
public class Note {
    
    private int laneIndex;
    private long originalSpawnTime; // Original spawn time before speed adjustment
    private long originalHitTime;   // Original hit time before speed adjustment
    private long spawnTime; // When the note should appear on screen (adjusted for speed)
    private long hitTime;   // When the note should be hit (adjusted for speed)
    private float yPosition; // Current Y position for rendering (0.0 to 1.0, where 1.0 is bottom)
    private float speedMultiplier = 1.0f; // Speed multiplier for note movement

    private static final float TARGET_POSITION = 0.85f; // Matches GamePanel's TARGET_POSITION
    
    /**
     * Constructor
     * @param laneIndex The lane index where this note appears
     * @param spawnTime The time when this note should appear (ms)
     * @param hitTime The ideal time when this note should be hit (ms)
     */
    public Note(int laneIndex, long spawnTime, long hitTime) {
        this.laneIndex = laneIndex;
        this.originalSpawnTime = spawnTime;
        this.originalHitTime = hitTime;
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
            // If the note hasn't spawned yet, set yPosition to 0.0
            yPosition = 0.0f;
        } 
        else {
            // Calculate normalized time from spawn to hit
            float totalTravel = hitTime - spawnTime;
            float elapsed = currentTime - spawnTime;
            
            if (currentTime <= hitTime) {
                // Note hasn't reached the hit line yet
                float progress = elapsed / totalTravel;
                yPosition = progress * TARGET_POSITION;
            } else {
                // Note has passed the hit line
                // Calculate how long it's been since passing the hit line
                float timePastTarget = currentTime - hitTime;
                
                // Use a faster speed for notes past the hit line (looks better)
                float acceleratedProgress = timePastTarget / (totalTravel * 0.3f);
                
                // Move from TARGET_POSITION to 1.0 (bottom of screen)
                yPosition = TARGET_POSITION + (1.0f - TARGET_POSITION) * 
                           Math.min(1.0f, acceleratedProgress);
            }
        }
    }
    
    /**
     * Sets the speed multiplier for this note
     * Higher values make the note move faster, lower values make it move slower
     * Also adjusts the hit time and spawn time to match the new speed
     * @param speedMultiplier The speed multiplier (1.0 = normal speed)
     */
    public void setSpeedMultiplier(float speedMultiplier) {
        if (speedMultiplier <= 0) {
            throw new IllegalArgumentException("Speed multiplier must be positive");
        }
        
        this.speedMultiplier = speedMultiplier;
        
        // Calculate how long it should take for the note to travel from spawn to hit
        long originalTravelTime = originalHitTime - originalSpawnTime;
        long adjustedTravelTime = Math.round(originalTravelTime / speedMultiplier);
        
        // Keep the spawn time the same, but adjust the hit time
        spawnTime = originalSpawnTime;
        hitTime = spawnTime + adjustedTravelTime;
    }
    
    /**
     * Gets the speed multiplier for this note
     * @return The speed multiplier
     */
    public float getSpeedMultiplier() {
        return speedMultiplier;
    }
    
    /**
     * Gets the original hit time before speed adjustment
     * @return The original hit time
     */
    public long getOriginalHitTime() {
        return originalHitTime;
    }
    
    /**
     * Gets the original spawn time before speed adjustment
     * @return The original spawn time
     */
    public long getOriginalSpawnTime() {
        return originalSpawnTime;
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