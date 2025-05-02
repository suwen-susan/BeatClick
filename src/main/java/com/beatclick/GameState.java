package com.beatclick;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GameState - Represents the current state of the game
 * Manages score, notes, and game progression
 */
public class GameState {
    
    /**
     * Rating - Represents the quality of a note hit
     */
    public enum Rating {
        EXCELLENT, GOOD, POOR, MISS
    }
    
    // Game configuration
    private static final int DEFAULT_MAX_MISSES = 10;
    private static final int HIT_WINDOW_MS = 150; // Timing window for note hits in milliseconds
    private static final int EXCELLENT_WINDOW_MS = 50; // Perfect timing
    private static final int GOOD_WINDOW_MS = 100; // Good timing
    // POOR is between GOOD_WINDOW_MS and HIT_WINDOW_MS
    
    // Game state variables
    private String songId;
    private int score;
    private int combo;
    private int maxCombo;
    private int misses;
    private int maxMisses;
    private boolean paused;
    private long gameStartTime;
    
    // Reference to GameManager for callbacks
    private GameManager gameManager;
    
    // Rating counters
    private int excellentCount;
    private int goodCount;
    private int poorCount;
    private int missCount;
    
    // Notes management
    private final List<Note> activeNotes; // Notes currently on screen
    private final List<Note> upcomingNotes; // Notes yet to appear
    private final List<Note> processedNotes; // Notes that have been hit or missed
    
    /**
     * Constructor - initializes a new game state
     */
    public GameState() {
        this.score = 0;
        this.combo = 0;
        this.maxCombo = 0;
        this.misses = 0;
        this.maxMisses = DEFAULT_MAX_MISSES;
        this.paused = false;
        
        // Initialize rating counters
        this.excellentCount = 0;
        this.goodCount = 0;
        this.poorCount = 0;
        this.missCount = 0;
        
        // Use thread-safe collections for notes
        this.activeNotes = new CopyOnWriteArrayList<>();
        this.upcomingNotes = new CopyOnWriteArrayList<>();
        this.processedNotes = new CopyOnWriteArrayList<>();
    }
    
    /**
     * Sets the reference to the game manager
     * @param gameManager The game manager
     */
    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    /**
     * Sets the song ID for the current game
     * @param songId The song ID
     */
    public void setSongId(String songId) {
        this.songId = songId;
    }
    
    /**
     * Gets the song ID for the current game
     * @return The song ID
     */
    public String getSongId() {
        return songId;
    }
    
    /**
     * Gets the current score
     * @return The current score
     */
    public int getScore() {
        return score;
    }
    
    /**
     * Increments the player's score based on current combo and rating
     * @param rating The rating of the hit
     */
    public void incrementScore(Rating rating) {
        // Base score for each hit
        int baseScore = 0;
        
        // Score based on rating
        switch (rating) {
            case EXCELLENT:
                baseScore = 300;
                excellentCount++;
                break;
            case GOOD:
                baseScore = 200;
                goodCount++;
                break;
            case POOR:
                baseScore = 100;
                poorCount++;
                break;
            case MISS:
                missCount++;
                return; // No score for miss
        }
        
        // Increase combo for non-miss hits
        if (rating != Rating.MISS) {
            combo++;
            if (combo > maxCombo) {
                maxCombo = combo;
            }
            
            // Calculate score with combo multiplier
            int comboMultiplier = Math.min(combo / 10 + 1, 4); // Cap multiplier at 4x
            score += baseScore * comboMultiplier;
        } else {
            combo = 0; // Reset combo on miss
        }
    }
    
    /**
     * Increments the number of misses
     */
    public void incrementMisses() {
        misses++;
        combo = 0; // Reset combo on miss
        missCount++;
    }
    
    /**
     * Gets the current number of misses
     * @return The current number of misses
     */
    public int getMisses() {
        return misses;
    }
    
    /**
     * Gets the maximum allowed misses before game over
     * @return The maximum allowed misses
     */
    public int getMaxMisses() {
        return maxMisses;
    }
    
    /**
     * Sets the maximum allowed misses before game over
     * @param maxMisses The maximum allowed misses
     */
    public void setMaxMisses(int maxMisses) {
        this.maxMisses = maxMisses;
    }
    
    /**
     * Gets the count of excellent hits
     * @return The count of excellent hits
     */
    public int getExcellentCount() {
        return excellentCount;
    }
    
    /**
     * Gets the count of good hits
     * @return The count of good hits
     */
    public int getGoodCount() {
        return goodCount;
    }
    
    /**
     * Gets the count of poor hits
     * @return The count of poor hits
     */
    public int getPoorCount() {
        return poorCount;
    }
    
    /**
     * Gets the count of misses
     * @return The count of misses
     */
    public int getMissCount() {
        return missCount;
    }
    
    /**
     * Checks if the game is currently paused
     * @return true if the game is paused, false otherwise
     */
    public boolean isPaused() {
        return paused;
    }
    
    /**
     * Sets the pause state of the game
     * @param paused The pause state to set
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }
    
    /**
     * Gets the game start time
     * @return The game start time in milliseconds
     */
    public long getGameStartTime() {
        return gameStartTime;
    }
    
    /**
     * Sets the game start time
     * @param gameStartTime The game start time in milliseconds
     */
    public void setGameStartTime(long gameStartTime) {
        this.gameStartTime = gameStartTime;
    }
    
    /**
     * Gets the current game time in milliseconds
     * @return The current game time relative to start
     */
    public long getCurrentGameTime() {
        return System.currentTimeMillis() - gameStartTime;
    }
    
    /**
     * Adds a new note to the game
     * @param note The note to add
     */
    public void addNote(Note note) {
        upcomingNotes.add(note);
    }
    
    /**
     * Updates the note lists based on current game time
     * Moves notes from upcoming to active when it's time for them to appear
     * Moves missed notes from active to processed
     */
    public void updateNotes() {
        long currentTime = getCurrentGameTime();
        
        // Move upcoming notes to active notes when it's time
        List<Note> notesToActivate = new ArrayList<>();
        for (Note note : upcomingNotes) {
            if (currentTime >= note.getSpawnTime()) {
                notesToActivate.add(note);
            }
        }
        
        upcomingNotes.removeAll(notesToActivate);
        activeNotes.addAll(notesToActivate);
        
        // Check for missed notes (past their hit window)
        List<Note> missedNotes = new ArrayList<>();
        for (Note note : activeNotes) {
            if (currentTime > note.getHitTime() + HIT_WINDOW_MS) {
                missedNotes.add(note);
                incrementMisses();
                
                // Notify GameManager about the missed note for visual effects
                if (gameManager != null) {
                    gameManager.noteMissed(note);
                }
                
                // Check for game over
                if (misses >= maxMisses && gameManager != null) {
                    gameManager.gameOver();
                }
            }
        }
        
        activeNotes.removeAll(missedNotes);
        processedNotes.addAll(missedNotes);
    }
    
    /**
     * Checks if there's a note in the given lane that's close to being hit
     * @param laneIndex The lane index to check
     * @param currentTime The current game time
     * @return true if there's a close note, false otherwise
     */
    public boolean hasNearbyNote(int laneIndex, long currentTime) {
        final long NEARBY_THRESHOLD_MS = 300; // Consider notes within 300ms "nearby"
        
        for (Note note : activeNotes) {
            if (note.getLaneIndex() == laneIndex) {
                long timeDifference = Math.abs(currentTime - note.getHitTime());
                if (timeDifference <= NEARBY_THRESHOLD_MS) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a player hit a note and returns the rating
     * @param laneIndex The lane index where the player clicked
     * @param clickTime The time of the click
     * @return A HitResult object containing the hit note and rating, or null if no hit
     */
    public HitResult hitNote(int laneIndex, long clickTime) {
        Note hit = null;
        Rating rating = Rating.MISS;
        
        // Find the closest note in the specified lane
        for (Note note : activeNotes) {
            if (note.getLaneIndex() == laneIndex) {
                long timeDifference = Math.abs(clickTime - note.getHitTime());
                
                if (timeDifference <= HIT_WINDOW_MS) {
                    hit = note;
                    
                    // Determine rating based on timing accuracy
                    if (timeDifference <= EXCELLENT_WINDOW_MS) {
                        rating = Rating.EXCELLENT;
                    } else if (timeDifference <= GOOD_WINDOW_MS) {
                        rating = Rating.GOOD;
                    } else {
                        rating = Rating.POOR;
                    }
                    break;
                }
            }
        }
        
        if (hit != null) {
            // Remove hit note from active notes and add to processed notes
            activeNotes.remove(hit);
            processedNotes.add(hit);
            return new HitResult(hit, rating);
        }
        
        return null;
    }
    
    /**
     * Inner class to hold the result of a hit attempt
     */
    public static class HitResult {
        private final Note note;
        private final Rating rating;
        
        public HitResult(Note note, Rating rating) {
            this.note = note;
            this.rating = rating;
        }
        
        public Note getNote() {
            return note;
        }
        
        public Rating getRating() {
            return rating;
        }
    }
    
    /**
     * Gets a list of all active notes
     * @return The list of active notes
     */
    public List<Note> getActiveNotes() {
        return new ArrayList<>(activeNotes);
    }
    
    /**
     * Gets the current combo
     * @return The current combo
     */
    public int getCombo() {
        return combo;
    }
    
    /**
     * Gets the maximum combo achieved in this game
     * @return The maximum combo
     */
    public int getMaxCombo() {
        return maxCombo;
    }
}