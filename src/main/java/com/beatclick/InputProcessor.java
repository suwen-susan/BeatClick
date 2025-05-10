package com.beatclick;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * InputProcessor - Processes player input events in a separate thread
 * Handles click/tap events and forwards them to the game manager
 */
public class InputProcessor implements Runnable {
    
    private final GameManager gameManager;
    private final BlockingQueue<InputEvent> inputQueue;
    private boolean isRunning;
    
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
        // Don't add input events when game is paused
        if (gameManager.getGameState().isPaused()) {
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
                
                // Process input events from the queue
                InputEvent event = inputQueue.poll();
                if (event != null) {
                    gameManager.processNoteClick(event.getLaneIndex(), event.getEventTime());
                }
                
                // Short sleep to reduce CPU usage
                Thread.sleep(5);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Stops the input processor
     */
    public void stop() {
        isRunning = false;
    }
}