package com.beatclick;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * MusicPlayer - Handles music playback in a separate thread
 * Manages the audio playback and synchronization with the game
 */
public class MusicPlayer implements Runnable {
    
    private final String songId;
    private final GameManager gameManager;
    
    private Clip audioClip;
    private volatile boolean isRunning;
    private long pausePosition;
    
    /**
     * Constructor
     * @param songId The ID of the song to play
     * @param gameManager The game manager
     */
    public MusicPlayer(String songId, GameManager gameManager) {
        this.songId = songId;
        this.gameManager = gameManager;
        this.isRunning = false;
        this.pausePosition = 0;
    }
    
    /**
     * Thread run method
     */
    @Override
    public void run() {
        isRunning = true;
        
        try {
            // Load the audio file
            File audioFile = new File("assets/songs/" + songId + ".wav");
            if (!audioFile.exists()) {
                System.err.println("Audio file not found: " + audioFile.getAbsolutePath());
                return;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            
            // Get audio format
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            
            // Create and prepare the audio clip
            audioClip = (Clip) AudioSystem.getLine(info);
            audioClip.open(audioStream);
            
            // Add a listener to detect when music ends
            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && isRunning && !gameManager.isPaused()) {
                    audioClip.close();
                    songCompleted();
                }
            });
            
            // Start playback
            audioClip.start();
            
            // Stay in this thread until the song finishes or the game stops
            while (isRunning && gameManager.isGameRunning()) {
                // Short sleep to reduce CPU usage
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing audio: " + e.getMessage());
        } finally {
            stop(); // Clean up resources
        }
    }
    
    /**
     * Pauses the music playback
     */
    public void pause() {
        if (audioClip != null && audioClip.isRunning()) {
            pausePosition = audioClip.getMicrosecondPosition();
            audioClip.stop();
        }
    }
    
    /**
     * Resumes the music playback from the last pause position
     */
    public void resume() {
        if (audioClip != null) {
            try {
                if (pausePosition < audioClip.getMicrosecondLength()) {
                    audioClip.setMicrosecondPosition(pausePosition);
                    audioClip.start();
                } else {
                    // We're past the end of the song, close it
                    audioClip.close();
                    songCompleted();
                }
            } catch (Exception e) {
                System.err.println("Error resuming audio playback: " + e.getMessage());
            }
        }
    }
    
    /**
     * Resumes the music playback from a specific position in milliseconds
     * @param positionMs Position in milliseconds
     */
    public void resumeFromPosition(long positionMs) {
        if (audioClip != null) {
            try {
                long microPos = positionMs * 1000; // Convert ms to microseconds
                if (microPos < audioClip.getMicrosecondLength()) {
                    audioClip.setMicrosecondPosition(microPos);
                    audioClip.start();
                } else {
                    // We're past the end of the song, close it
                    audioClip.close();
                    songCompleted();
                }
            } catch (Exception e) {
                System.err.println("Error resuming audio from position: " + e.getMessage());
                
                // Fall back to restart if there's an error
                try {
                    audioClip.setMicrosecondPosition(0);
                    audioClip.start();
                } catch (Exception ex) {
                    System.err.println("Error with fallback audio restart: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * Stops the music playback and cleans up resources
     */
    public void stop() {
        isRunning = false;
        
        if (audioClip != null) {
            try {
                audioClip.stop();
                audioClip.close();
            } catch (Exception e) {
                System.err.println("Error closing audio clip: " + e.getMessage());
            }
            audioClip = null;
        }
    }
    
    /**
     * Gets the current playback position in milliseconds
     * @return The current playback position
     */
    public long getCurrentPosition() {
        if (audioClip != null) {
            return audioClip.getMicrosecondPosition() / 1000;
        }
        return 0;
    }
    
    /**
     * Called when the song has completed playing
     */
    private void songCompleted() {
        if (gameManager.isGameRunning()) {
            // The song ended normally, show the results screen
            gameManager.stopGame();
        }
    }
}