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
    private boolean isRunning;
    
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
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - lastUpdateTime;
                
                // Make sure we keep to our target frame rate
                if (elapsedTime < FRAME_TIME) {
                    Thread.sleep(FRAME_TIME - elapsedTime);
                }
                
                // Update and repaint
                updateAnimations();
                
                lastUpdateTime = System.currentTimeMillis();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Updates all animations
     */
    private void updateAnimations() {
        // Update on the EDT to avoid threading issues with Swing
        SwingUtilities.invokeLater(() -> {
            gamePanel.updateAnimations();
            gamePanel.repaint();
        });
    }
    
    /**
     * Stops the animation controller
     */
    public void stop() {
        isRunning = false;
    }
}