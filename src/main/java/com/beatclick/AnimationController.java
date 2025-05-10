package com.beatclick;

import javax.swing.*;

/**
 * AnimationController - Manages game animations in a separate thread
 * Updates note positions and visual effects
 */
public class AnimationController implements Runnable {
    
    private static final int FRAME_RATE = 60; // Frames per second
    private static final long FRAME_TIME = 1000 / FRAME_RATE; // Milliseconds per frame
    
    private final GamePanel gamePanel;
    private volatile boolean isRunning;
    
    /**
     * Constructor
     * @param gamePanel The game panel to animate
     */
    public AnimationController(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.isRunning = false;
    }
    
    /**
     * Thread run method
     */
    @Override
    public void run() {
        isRunning = true;
        long lastUpdateTime = System.currentTimeMillis();
        
        try {
            while (isRunning) {
                GameState gameState = gamePanel.getGameState();
                
                // Check if game is paused - if so, wait and don't update animations
                if (gameState != null && gameState.isPaused()) {
                    try {
                        Thread.sleep(20); // Small sleep to prevent CPU hogging while paused
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    
                    lastUpdateTime = System.currentTimeMillis(); // Reset the timer when unpaused
                    continue; // Skip this update cycle
                }

                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - lastUpdateTime;
                
                // Make sure we keep to our target frame rate
                if (elapsedTime < FRAME_TIME) {
                    try {
                        Thread.sleep(FRAME_TIME - elapsedTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                // Update and repaint on the EDT to avoid threading issues with Swing
                try {
                    SwingUtilities.invokeLater(() -> {
                        if (isRunning && gamePanel != null) {
                            gamePanel.updateAnimations();
                            gamePanel.repaint();
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Error updating animations: " + e.getMessage());
                }
                
                lastUpdateTime = System.currentTimeMillis();
            }
        } catch (Exception e) {
            System.err.println("Error in animation controller: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Stops the animation controller
     */
    public void stop() {
        isRunning = false;
        
        // Interrupt the thread to break out of any sleeps
        Thread currentThread = Thread.currentThread();
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }
}