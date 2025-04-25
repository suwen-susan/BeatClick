package com.beatclick;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GameState - Represents the current state of the game
 * Manages score, notes, and game progression
 */
public class GameState {
    
    // Game configuration
    private static final int DEFAULT_MAX_MISSES = 10;
    private static final int HIT_WINDOW_MS = 150; // Timing window for note hits in milliseconds
    
    // Game state variables
    private String songId;
    private int score;
    private int combo;
    private int maxCombo;
    private int misses;
    private int maxMisses;
    private boolean paused;
    private long gameStartTime;
    
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
        
        // Use thread-safe collections for notes
        this.activeNotes = new CopyOnWriteArrayList<>();
        this.upcomingNotes = new CopyOnWriteArrayList<>();
        this.processedNotes = new CopyOnWriteArrayList<>();
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
     * Increments the player's score based on current combo
     */
    public void incrementScore() {
        // Base score for each hit
        int baseScore = 100;
        
        // Increase combo
        combo++;
        if (combo > maxCombo) {
            maxCombo = combo;
        }
        
        // Calculate score with combo multiplier
        int comboMultiplier = Math.min(combo / 10 + 1, 4); // Cap multiplier at 4x
        score += baseScore * comboMultiplier;
    }
    
    /**
     * Increments the number of misses and resets combo
     */
    public void incrementMisses() {
        misses++;
        combo = 0; // Reset combo on miss
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
            }
        }
        
        activeNotes.removeAll(missedNotes);
        processedNotes.addAll(missedNotes);
    }
    
    /**
     * Checks if a player hit a note
     * @param laneIndex The lane index where the player clicked
     * @param clickTime The time of the click
     * @return true if a note was hit, false otherwise
     */
    public boolean checkNoteHit(int laneIndex, long clickTime) {
        Note hitNote = null;
        
        // Find the closest note in the specified lane
        for (Note note : activeNotes) {
            if (note.getLaneIndex() == laneIndex) {
                long timeDifference = Math.abs(clickTime - note.getHitTime());
                
                if (timeDifference <= HIT_WINDOW_MS) {
                    hitNote = note;
                    break;
                }
            }
        }
        
        // If a note was hit, remove it from active notes and add to processed notes
        if (hitNote != null) {
            activeNotes.remove(hitNote);
            processedNotes.add(hitNote);
            return true;
        }
        
        return false;
    }

    /**
     * 尝试击中指定车道的音符。
     * @param laneIndex 车道编号
     * @param clickTime 玩家点击时的游戏时间（ms）
     * @return 如果命中，返回该 Note 对象；否则返回 null
     */
    public Note hitNote(int laneIndex, long clickTime) {
        Note hit = null;
        // 遍历所有正在激活的音符
        for (Note note : activeNotes) {
            if (note.getLaneIndex() == laneIndex) {
                long diff = Math.abs(clickTime - note.getHitTime());
                if (diff <= HIT_WINDOW_MS) {
                    hit = note;
                    break;
                }
            }
        }
        if (hit != null) {
            // 把它从 activeNotes 移除，加入 processedNotes
            activeNotes.remove(hit);
            processedNotes.add(hit);
            // 增分逻辑保留在 GameManager 里
        }
        return hit;
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