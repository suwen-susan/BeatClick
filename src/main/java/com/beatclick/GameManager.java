package com.beatclick;

import javax.swing.*;
import java.awt.*;
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

    //User data
    private String playerName;

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
        
        // Set the song ID in the game state
        this.gameState.setSongId(songId);
        
        // Set the game manager reference in game state
        this.gameState.setGameManager(this);

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
     * Gets the main application reference
     * @return The main application
     */
    public BeatClick getMainApp() {
        return mainApp;
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
    if (!gameRunning || gameState.isPaused()) return false;
    
    // record misses before the click
    int missesBefore = gameState.getMisses();

    // Attempt to hit a note and get the rating
    GameState.HitResult hitResult = gameState.hitNote(laneIndex, clickTime);
    
    if (hitResult != null) {
        // Successfully hit a note
        Note note = hitResult.getNote();
        GameState.Rating rating = hitResult.getRating();
        
        // Apply the rating and update score
        gameState.incrementScore(rating);

        // chek if health is recovered
        if (gameState.getMisses() < missesBefore) {
            // show health recovery effect
            gamePanel.showHealthRecoveryEffect();
        }
        
        // Show visual effect based on rating at the note's position
        // gamePanel.showHitEffect(laneIndex, rating, noteY);
        gamePanel.showHitEffect(laneIndex, rating);
        
        // Remove the note from rendering
        gamePanel.removeNote(note);
        
        // Update display
        gamePanel.updateScore(gameState.getScore());
        gamePanel.updateCombo(gameState.getCombo());
        
        return true;
    }  else {
        // Always treat as miss — don't skip based on nearby notes
        gamePanel.showMissEffect(laneIndex);
        gameState.incrementScore(GameState.Rating.MISS);
        gameState.incrementMisses();
        gamePanel.updateCombo(0);

        if (gameState.getMisses() >= gameState.getMaxMisses()) {
            gameOver();
        }

        return false;
    }
}

    /**
     * Called when a note passes the target line without being hit
     * @param note The missed note
     */
    public void noteMissed(Note note) {
        // Show a miss effect at the bottom of the screen
        int laneIndex = note.getLaneIndex();
        gamePanel.showMissEffectAtBottom(laneIndex);
        
        // Check if we've reached game over condition
        if (gameState.getMisses() >= gameState.getMaxMisses()) {
            gameOver();
        }
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
    public void gameOver() {
        if (!gameRunning) {
            return; // Prevent multiple game over calls
        }
        
        gameRunning = false;
        
        // Stop music and threads
        if (musicPlayer != null) {
            musicPlayer.stop();
        }
        
        // Show game over dialog
        SwingUtilities.invokeLater(() -> {
            int score = gameState.getScore();
            int excellent = gameState.getExcellentCount();
            int good = gameState.getGoodCount();
            int poor = gameState.getPoorCount();
            int miss = gameState.getMissCount();
            String songId = gameState.getSongId();
            int highScore = DatabaseManager.getHighScoreFromDetailedTable(songId);
            String endTime = java.time.Instant.now().toString();
            ScoreRecord oldBest = DatabaseManager.getHighScoreRecord(songId);       // get the historical highest record
            DatabaseManager.saveDetailedScore(
                    playerName,
                    songId,
                    endTime,
                    score,
                    miss,
                    poor,
                    good,
                    excellent
            );

            String message = String.format(
                "Game Over!\n\nYour score: %d\n\nExcellent: %d\nGood: %d\nPoor: %d\nMiss: %d",
                score, excellent, good, poor, miss
            );

            if (DatabaseManager.isHighScore(gameState.getSongId(), score)) {
                message += "\n\nNew High Score!";
                // DatabaseManager.saveScore(gameState.getSongId(), score);
            }
            // texture notation
            // JOptionPane.showMessageDialog(parentWindow, message, "Game Over", JOptionPane.INFORMATION_MESSAGE);

            // show chart
            JPanel chartContainer = new JPanel(new BorderLayout());
            JPanel currentChart = ChartUtils.createRatingPieChart(excellent, good, poor, miss);
            chartContainer.add(currentChart, BorderLayout.CENTER);

            JButton toggleButton = new JButton("Switch to Score Chart");
            toggleButton.addActionListener(e -> {
                chartContainer.removeAll();
                if (toggleButton.getText().contains("Score")) {
                    boolean isNewRecord = score > oldBest.score;
                    String currentLabel;
                    String bestLabel;
                    String chartTitle;
                    if (isNewRecord) {
                        currentLabel = "Top Score: You (" + score + " pts)";
                        bestLabel = "Previous Top: " + oldBest.username + " (" + oldBest.score + " pts)";
                        chartTitle = currentLabel + " vs " + bestLabel;
                        currentLabel = "Top Score: You";
                        bestLabel = "Previous Top: " + oldBest.username;
                    } else {
                        currentLabel = "You (" + score + " pts)";
                        bestLabel = "Top Score: " + oldBest.username + " (" + oldBest.score + " pts)";
                        chartTitle = currentLabel + " vs " + bestLabel;
                        currentLabel = "You";
                        bestLabel = "Top Score: " + oldBest.username;
                    }
                    JPanel chart = ChartUtils.createRatingComparisonBarChart(
                            excellent, good, poor, miss,
                            oldBest, currentLabel, bestLabel, chartTitle,
                            new Dimension(500, 300)
                    );
                    chartContainer.add(chart, BorderLayout.CENTER);
                    toggleButton.setText("Switch to Rating Pie Chart");
                } else {
                    JPanel pieChart = ChartUtils.createRatingPieChart(excellent, good, poor, miss);
                    chartContainer.add(pieChart, BorderLayout.CENTER);
                    toggleButton.setText("Switch to Score Chart");
                }

                chartContainer.revalidate();
                chartContainer.repaint();
            });

            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            controlPanel.add(toggleButton);

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.add(chartContainer, BorderLayout.CENTER);
            wrapper.add(controlPanel, BorderLayout.SOUTH);

            JOptionPane.showMessageDialog(parentWindow, wrapper, "Your Performance", JOptionPane.PLAIN_MESSAGE);



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


    public void setPlayerName(String name) {
        this.playerName = name;
    }
}