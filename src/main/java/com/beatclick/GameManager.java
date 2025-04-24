package com.beatclick;

import javax.swing.*;
import java.util.concurrent.*;

/**
 * GameManager - Handles game logic and thread management
 * Coordinates the music playback, note generation, and input processing threads
 */
public class GameManager {
    
    private final JFrame parentWindow;
    private GamePanel gamePanel;
    private GameState gameState;
    private BeatClick mainApp;
    // Thread management
    private ExecutorService threadPool;
    private MusicPlayer musicPlayer;
    private NoteGenerator noteGenerator;
    private InputProcessor inputProcessor;
    private AnimationController animationController;
    
    // Synchronization objects
    private final Object syncLock = new Object();
    private volatile boolean gameRunning = false;
    
    /**
     * Constructor
     * @param parentWindow The main application window
     */
    public GameManager(JFrame parentWindow) {
        this.parentWindow = parentWindow;
        this.threadPool = Executors.newFixedThreadPool(4); // One thread for each major component
    }
    
    /**
     * Starts a new game with the selected song
     * @param songId The ID of the selected song
     * @param gamePanel The game panel for rendering
     */
    public void startGame(String songId, GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        this.gameState = new GameState();

        // Set up game state
        gamePanel.setGameState(gameState);

        // Start timing synchronization
        startTimingSync();

        // Configure input handler
        gamePanel.configureInputHandler(new InputHandler(this));

        // Set up game panel and add it to the main window
        gamePanel.setFocusable(true);
        gamePanel.requestFocusInWindow();

        // Initialize game components
        musicPlayer = new MusicPlayer(songId, this);
        noteGenerator = new NoteGenerator(songId, this);
        inputProcessor = new InputProcessor(this);
        animationController = new AnimationController(gamePanel);
        
        // Start game threads
        gameRunning = true;
        threadPool.submit(musicPlayer);
        threadPool.submit(noteGenerator);
        threadPool.submit(inputProcessor);
        threadPool.submit(animationController);
        

    }
    
    /**
     * Stops the current game and cleans up resources
     */
    public void stopGame() {
        gameRunning = false;
        
        // Shutdown all threads gracefully
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
            try {
                if (!threadPool.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    System.err.println("Thread pool did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Create a new thread pool for future games
            threadPool = Executors.newFixedThreadPool(4);
        }
        
        // Clean up resources
        if (musicPlayer != null) {
            musicPlayer.stop();
        }
        
        // Save score to database
        if (gameState != null) {
            DatabaseManager.saveScore(gameState.getSongId(), gameState.getScore());
        }
    }
    /**
 * Sets the main application reference
 * @param mainApp The main application
 */
    public void setMainApp(BeatClick mainApp) {
        this.mainApp = mainApp;
}
    
    /**
     * Pauses the current game
     */
    public void pauseGame() {
        if (gameRunning) {
            synchronized (syncLock) {
                gameState.setPaused(true);
                if (musicPlayer != null) {
                    musicPlayer.pause();
                }
            }
        }
    }
    
    /**
     * Resumes the paused game
     */
    public void resumeGame() {
        if (gameRunning && gameState.isPaused()) {
            synchronized (syncLock) {
                gameState.setPaused(false);
                if (musicPlayer != null) {
                    musicPlayer.resume();
                }
                syncLock.notifyAll(); // Wake up waiting threads
            }
        }
    }
    
    /**
     * Processes a note click by the player
     * @param laneIndex The lane index where the click occurred
     * @param clickTime The time of the click
     * @return true if a note was hit, false otherwise
     */
    public boolean processNoteClick(int laneIndex, long clickTime) {
        if (!gameRunning || gameState.isPaused()) {
            return false;
        }
        
        boolean noteHit = gameState.checkNoteHit(laneIndex, clickTime);
        
        if (noteHit) {
            gamePanel.showHitEffect(laneIndex);
            gameState.incrementScore();
        } else {
            gamePanel.showMissEffect(laneIndex);
            gameState.incrementMisses();
        }
        
        gamePanel.updateScore(gameState.getScore());
        
        // Check for game over condition
        if (gameState.getMisses() >= gameState.getMaxMisses()) {
            gameOver();
        }
        
        return noteHit;
    }
    
    /**
     * Adds a new note to the game state
     * @param laneIndex The lane index for the note
     * @param spawnTime The spawn time of the note
     * @param hitTime The ideal hit time of the note
     */
    public void addNote(int laneIndex, long spawnTime, long hitTime) {
        if (gameRunning && !gameState.isPaused()) {
            Note note = new Note(laneIndex, spawnTime, hitTime);
            gameState.addNote(note);
            gamePanel.addNote(note);
        }
    }
    
    /**
     * Called when the game is over
     */
    private void gameOver() {
        gameRunning = false;
        
        // Stop music and threads
        if (musicPlayer != null) {
            musicPlayer.stop();
        }
        
        // Show game over dialog
        SwingUtilities.invokeLater(() -> {
            int score = gameState.getScore();
            String message = "Game Over!\nYour score: " + score;
            
            if (DatabaseManager.isHighScore(gameState.getSongId(), score)) {
                message += "\nNew High Score!";
                DatabaseManager.saveScore(gameState.getSongId(), score);
            }
            
            JOptionPane.showMessageDialog(parentWindow, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);
            
            // Return to menu
            mainApp.returnToMenu();
        });
    }
    
    /**
     * Starts the timing synchronization for all game components
     */
    private void startTimingSync() {
        // Set up timing reference
        gameState.setGameStartTime(System.currentTimeMillis());
    }
    
    /**
     * Gets the current game state
     * @return The current game state
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * Gets the game panel
     * @return The game panel
     */
    public GamePanel getGamePanel() {
        return gamePanel;
    }
    
    /**
     * Checks if the game is currently running
     * @return true if the game is running, false otherwise
     */
    public boolean isGameRunning() {
        return gameRunning;
    }
    
    /**
     * Gets the synchronization lock object
     * @return The synchronization lock object
     */
    public Object getSyncLock() {
        return syncLock;
    }
    /**
 * Gets the input processor
 * @return The input processor
 */
    public InputProcessor getInputProcessor() {
        return inputProcessor;
    }

}