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
    private boolean isPaused;
    private boolean isRunning;
    private long pausePosition;
    
    /**
     * Constructor
     * @param songId The ID of the song to play
     * @param gameManager The game manager
     */
    public MusicPlayer(String songId, GameManager gameManager) {
        this.songId = songId;
        this.gameManager = gameManager;
        this.isPaused = false;
        this.isRunning = false;
        this.pausePosition = 0;
    }
    
    /**
     * Thread run method
     */
    @Override
    public void run() {
        try {
            // Load the audio file
            File audioFile = new File("assets/songs/" + songId + ".wav");
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            
            // Get audio format
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            
            // Create and prepare the audio clip
            audioClip = (Clip) AudioSystem.getLine(info);
            audioClip.open(audioStream);
            
            // Add a listener to detect when music ends
            audioClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && !isPaused) {
                    audioClip.close();
                    songCompleted();
                }
            });
            
            // Wait for a short delay to ensure game is ready
            Thread.sleep(500);
            
            // Start playback
            isRunning = true;
            audioClip.start();
            
            // Stay in this thread until the song finishes or the game stops
            while (gameManager.isGameRunning() && isRunning) {
                synchronized (gameManager.getSyncLock()) {
                    if (gameManager.getGameState().isPaused()) {
                        // Wait until the game is resumed
                        try {
                            gameManager.getSyncLock().wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                Thread.sleep(100); // Sleep to reduce CPU usage
            }
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error playing audio: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            stop();
        }
    }
    
    /**
     * Pauses the music playback
     */
    public void pause() {
        if (audioClip != null && audioClip.isRunning()) {
            pausePosition = audioClip.getMicrosecondPosition();
            audioClip.stop();
            isPaused = true;
        }
    }
    
    /**
     * Resumes the music playback
     */
    public void resume() {
        if (audioClip != null && isPaused) {
            audioClip.setMicrosecondPosition(pausePosition);
            audioClip.start();
            isPaused = false;
        }
    }
    
    /**
     * Stops the music playback and cleans up resources
     */
    public void stop() {
        isRunning = false;
        
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
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