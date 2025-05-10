package com.beatclick;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * InputProcessor - Processes player input events in a separate thread
 * Handles click/tap events and forwards them to the game manager
 */
public class InputProcessor implements Runnable {
    
    private final GameManager gameManager;
    private final BlockingQueue<InputEvent> inputQueue;
    private volatile boolean isRunning;
    
    /**
     * Inner class to represent an input event
     */
    public static class InputEvent {
        private final int laneIndex;
        private final long eventTime;
        
        public InputEvent(int laneIndex, long eventTime) {
            this.laneIndex = laneIndex;
            this.eventTime = eventTime;
        }
        
        public int getLaneIndex() {
            return laneIndex;
        }
        
        public long getEventTime() {
            return eventTime;
        }
    }
    
    /**
     * Constructor
     * @param gameManager The game manager
     */
    public InputProcessor(GameManager gameManager) {
        this.gameManager = gameManager;
        this.inputQueue = new LinkedBlockingQueue<>();
        this.isRunning = false;
    }
    
    /**
     * Adds an input event to the queue for processing
     * @param laneIndex The lane index where the input occurred
     */
    public void addInputEvent(int laneIndex) {
        // Don't add input events when game is paused or game is not running
        if (gameManager.getGameState().isPaused() || !isRunning || !gameManager.isGameRunning()) {
            return;
        }
        
        long currentTime = gameManager.getGameState().getCurrentGameTime();
        inputQueue.add(new InputEvent(laneIndex, currentTime));
    }
    
    /**
     * Thread run method
     */
    @Override
    public void run() {
        isRunning = true;
        
        try {
            while (isRunning && gameManager.isGameRunning()) {
                // Check if the game is paused
                if (gameManager.getGameState().isPaused()) {
                    synchronized (gameManager.getSyncLock()) {
                        while (gameManager.getGameState().isPaused() && isRunning && gameManager.isGameRunning()) {
                            try {
                                gameManager.getSyncLock().wait(100); // Use a timeout to periodically check isRunning
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                    }
                    
                    // Skip this iteration if we've been interrupted or stopped
                    if (!isRunning || !gameManager.isGameRunning()) {
                        break;
                    }
                }
                
                // Process input events from the queue with a timeout to avoid blocking forever
                InputEvent event = null;
                try {
                    event = inputQueue.poll(100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                if (event != null && isRunning && gameManager.isGameRunning() && !gameManager.getGameState().isPaused()) {
                    gameManager.processNoteClick(event.getLaneIndex(), event.getEventTime());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in InputProcessor: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Make sure to clean up
            inputQueue.clear();
            isRunning = false;
        }
    }
    
    /**
     * Stops the input processor
     */
    public void stop() {
        isRunning = false;
        // Clear any pending input events
        inputQueue.clear();
        
        // Interrupt any waiting operations
        Thread currentThread = Thread.currentThread();
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }
}