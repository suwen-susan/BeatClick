package com.beatclick;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * InputHandler - Captures user input from keyboard and mouse
 * Delegates to the InputProcessor for further processing
 */
public class InputHandler implements KeyListener, MouseListener {
    
    private final GameManager gameManager;
    
    // Key bindings for lanes (default: D, F, J, K)
    private static final int[] LANE_KEYS = {
        KeyEvent.VK_D,
        KeyEvent.VK_F,
        KeyEvent.VK_J,
        KeyEvent.VK_K
    };
    
    /**
     * Constructor
     * @param gameManager The game manager
     */
    public InputHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }
    
    /**
     * Handles key press events
     * @param e The key event
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        
        // Check for pause key (Escape or P)
        if (keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_P) {
            if (gameManager.getGameState().isPaused()) {
                gameManager.resumeGame();
            } else {
                gameManager.pauseGame();
            }
            return;
        }
        
        // Check for lane keys
        for (int i = 0; i < LANE_KEYS.length; i++) {
            if (keyCode == LANE_KEYS[i]) {
                processLaneInput(i);
                return;
            }
        }
    }
    
    /**
     * Handles mouse click events
     * @param e The mouse event
     */
    @Override
    public void mousePressed(MouseEvent e) {
        GamePanel gamePanel = gameManager.getGamePanel();

         // Check if click is on pause button - if so, ignore for lane detection
        if (gamePanel.isPauseButtonClick(e.getPoint())) {
            return; // Don't process as lane input if it's a pause button click
        }
        
        // Calculate which lane was clicked based on x-coordinate
        int panelWidth = gamePanel.getWidth();
        int laneWidth = panelWidth / LANE_KEYS.length;
        int clickX = e.getX();
        
        int laneIndex = clickX / laneWidth;
        
        // Ensure lane index is valid
        if (laneIndex >= 0 && laneIndex < LANE_KEYS.length) {
            processLaneInput(laneIndex);
        }
    }
    
    /**
     * Processes input for a specific lane
     * @param laneIndex The lane index
     */
    private void processLaneInput(int laneIndex) {
        if (!gameManager.getGameState().isPaused()) {
            InputProcessor inputProcessor = (InputProcessor) gameManager.getInputProcessor();
            if (inputProcessor != null) {
                inputProcessor.addInputEvent(laneIndex);
            }
        }
    }
    
    // Unused interface methods
    
    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
    
    @Override
    public void keyReleased(KeyEvent e) {
        // Not used
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        // Not used - using mousePressed instead
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        // Not used
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        // Not used
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        // Not used
    }
}