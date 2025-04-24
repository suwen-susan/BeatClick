package com.beatclick;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * NoteGenerator - Generates notes based on the song's rhythm
 * Reads note patterns from a file or generates them algorithmically
 */
public class NoteGenerator implements Runnable {
    
    private static final int NUM_LANES = 4;
    private static final int NOTE_TRAVEL_TIME_MS = 2000; // Time for notes to travel from top to bottom
    
    private final String songId;
    private final GameManager gameManager;
    private final Random random;
    private final List<NoteData> noteData;
    
    private boolean isRunning;
    
    /**
     * Inner class to store note timing data
     */
    private static class NoteData {
        long hitTime;
        int laneIndex;
        
        NoteData(long hitTime, int laneIndex) {
            this.hitTime = hitTime;
            this.laneIndex = laneIndex;
        }
    }
    
    /**
     * Constructor
     * @param songId The ID of the song
     * @param gameManager The game manager
     */
    public NoteGenerator(String songId, GameManager gameManager) {
        this.songId = songId;
        this.gameManager = gameManager;
        this.random = new Random();
        this.noteData = new ArrayList<>();
        this.isRunning = false;
        
        // Load note data from file or generate algorithmically
        loadNoteData();
    }
    
    /**
     * Thread run method
     */
    @Override
    public void run() {
        isRunning = true;
        
        try {
            // Wait for a short delay before starting to generate notes
            Thread.sleep(100);
            
            // Process all note data
            for (NoteData data : noteData) {
                if (!isRunning || Thread.currentThread().isInterrupted()) {
                    break;
                }
                
                synchronized (gameManager.getSyncLock()) {
                    // Check if the game is paused
                    while (gameManager.getGameState().isPaused()) {
                        try {
                            gameManager.getSyncLock().wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
                
                // Calculate spawn time based on hit time and travel duration
                long hitTime = data.hitTime;
                long spawnTime = hitTime - NOTE_TRAVEL_TIME_MS;
                int laneIndex = data.laneIndex;
                
                // Skip notes that should have already spawned
                long currentTime = gameManager.getGameState().getCurrentGameTime();
                // System.out.printf("DEBUG spawnTime=%d, currentTime=%d, lalaneIndex=%d%n", spawnTime, currentTime, laneIndex);
                if (spawnTime < currentTime) {
                    continue;
                }
                
                // Wait until it's time to spawn this note
                long waitTime = spawnTime - currentTime;
                if (waitTime > 0) {
                    Thread.sleep(waitTime);
                }
                
                // Add the note to the game
                if (isRunning && gameManager.isGameRunning()) {
                    gameManager.addNote(laneIndex, spawnTime, hitTime);
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Loads note data from a file or generates it algorithmically
     */
    private void loadNoteData() {
        String filePath = "assets/songs/" + songId + ".notes";
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse each line in format: hitTime,laneIndex
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    long hitTime = Long.parseLong(parts[0].trim());
                    int laneIndex = Integer.parseInt(parts[1].trim());
                    
                    // Add to note data
                    noteData.add(new NoteData(hitTime, laneIndex));
                }
            }
            System.out.println("Loaded " + noteData.size() + " notes from file: " + filePath);
        } catch (IOException e) {
            // File not found or error reading, generate notes algorithmically
            System.out.println("Note data file not found, generating notes algorithmically.");
            generateNotesAlgorithmically();
        }
    }
    
    /**
     * Generates notes algorithmically based on a simple rhythm pattern
     */
    private void generateNotesAlgorithmically() {
        // Assume a standard 4/4 time signature with 120 BPM
        // Quarter note = 500ms
        int bpm = 120;
        long quarterNoteMs = 60000 / bpm;
        
        // Start with a delay before the first note
        long currentTime = 2000;
        
        // Generate about 2 minutes worth of notes
        long songDuration = 120000; // 2 minutes
        
        while (currentTime < songDuration) {
            // Choose a lane
            int laneIndex = random.nextInt(NUM_LANES);
            
            // Add the note
            noteData.add(new NoteData(currentTime, laneIndex));
            
            // Move to the next beat (with slight variations)
            int pattern = random.nextInt(10);
            
            if (pattern < 5) {
                // Quarter note
                currentTime += quarterNoteMs;
            } else if (pattern < 8) {
                // Eighth note
                currentTime += quarterNoteMs / 2;
            } else {
                // Half note
                currentTime += quarterNoteMs * 2;
            }
            
            // Occasionally add a rest
            if (random.nextInt(10) < 2) {
                currentTime += quarterNoteMs;
            }
        }
        
        System.out.println("Generated " + noteData.size() + " notes algorithmically.");
    }
    
    /**
     * Stops the note generator
     */
    public void stop() {
        isRunning = false;
    }
}