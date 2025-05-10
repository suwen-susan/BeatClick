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

    // Music playback position tracking
    private long musicPositionBeforePause = 0;

    // Synchronization and state flags
    private final Object syncLock = new Object();
    private volatile boolean gameRunning = false;
    private volatile boolean gameOverInProgress = false;
    private volatile boolean isPaused = false;

    // User data
    private String playerName;
    private String currentSongId;
    private GameState.GameMode currentGameMode;
    private GameState.DifficultyLevel currentDifficultyLevel;

    /**
     * Constructor
     * @param parentWindow The main application window
     */
    public GameManager(JFrame parentWindow) {
        this.parentWindow = parentWindow;
        createThreadPool();
    }
    
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        
        if (gameState != null) {
            gameState.setGameManager(this);
        }
    }

    /**
     * Creates a new thread pool
     */
    private void createThreadPool() {
        // Shutdown existing pool if needed
        shutdownThreadPool();
        // Create a new thread pool
        this.threadPool = Executors.newFixedThreadPool(4);
    }
    
    /**
     * Safely shuts down the thread pool
     */
    private void shutdownThreadPool() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
            try {
                if (!threadPool.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    System.err.println("Thread pool did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            threadPool = null;
        }
    }

    /**
     * Starts a new game with the selected song
     * @param songId The ID of the selected song
     * @param gamePanel The game panel for rendering
     * @param gameMode The selected game mode
     * @param difficultyLevel The selected difficulty level
     */
    public void startGame(String songId, GamePanel gamePanel, GameState.GameMode gameMode, GameState.DifficultyLevel difficultyLevel) {
        // Store current settings
        this.currentSongId = songId;
        this.currentGameMode = gameMode;
        this.currentDifficultyLevel = difficultyLevel;
        
        // Reset state flags
        gameRunning = false;
        gameOverInProgress = false;
        isPaused = false;
        musicPositionBeforePause = 0;
        
        // Create a new thread pool (ensuring clean state)
        createThreadPool();
        
        // Setup game components
        this.gamePanel = gamePanel;
        this.gameState = new GameState();
        
        // Set the song ID in the game state
        this.gameState.setSongId(songId);
        
        // Set the game mode and difficulty
        this.gameState.setGameMode(gameMode);
        this.gameState.setDifficultyLevel(difficultyLevel);
        
        // Set the game manager reference in game state
        this.gameState.setGameManager(this);

        // Set up game state in panel
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
        if (!threadPool.isShutdown()) {
            threadPool.submit(musicPlayer);
            threadPool.submit(noteGenerator);
            threadPool.submit(inputProcessor);
            threadPool.submit(animationController);
        }
    }
    
    /**
     * Stops the current game and cleans up resources
     */
    public void stopGame() {
        // Set flags
        gameRunning = false;
        
        // Stop all components
        if (musicPlayer != null) {
            musicPlayer.stop();
            musicPlayer = null;
        }
        
        if (noteGenerator != null) {
            noteGenerator.stop();
            noteGenerator = null;
        }
        
        if (inputProcessor != null) {
            inputProcessor.stop();
            inputProcessor = null;
        }
        
        if (animationController != null) {
            animationController.stop();
            animationController = null;
        }
        
        // Shutdown thread pool
        shutdownThreadPool();
        
        // Save score to database (if game state exists)
        if (gameState != null) {
            String songId = gameState.getSongId();
            if (gameState.getGameMode() != GameState.GameMode.PRACTICE) {
                String songIdWithDifficulty = songId + "_" + gameState.getDifficultyLevel().name();
                DatabaseManager.saveScore(songIdWithDifficulty, gameState.getScore());
            }
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
        if (gameRunning && !isPaused) {
            isPaused = true;
            
            synchronized (syncLock) {
                if (musicPlayer != null) {
                    // Save music position for later resumption
                    musicPositionBeforePause = musicPlayer.getCurrentPosition();
                    musicPlayer.pause();
                }
                
                gameState.setPaused(true);
                syncLock.notifyAll();
            }
        }
    }
    
    /**
     * Resumes the paused game without restarting the music from the beginning
     */
    public void resumeGame() {
        if (gameRunning && isPaused) {
            isPaused = false;
            gameState.setPaused(false);
            
            synchronized (syncLock) {
                if (musicPlayer != null) {
                    // Resume music from saved position instead of current game time
                    musicPlayer.resumeFromPosition(musicPositionBeforePause);
                }
                
                syncLock.notifyAll();
            }
            
            // Request focus for the game panel
            SwingUtilities.invokeLater(() -> {
                if (gamePanel != null) {
                    gamePanel.requestFocusInWindow();
                }
            });
        }
    }
    
    /**
     * Processes a note click by the player
     * @param laneIndex The lane index where the click occurred
     * @param clickTime The time of the click
     * @return true if a note was hit, false otherwise
     */
    public boolean processNoteClick(int laneIndex, long clickTime) {
        if (!gameRunning || isPaused || gameOverInProgress) return false;
        
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
            gamePanel.showHitEffect(laneIndex, rating);
            
            // Remove the note from rendering
            gamePanel.removeNote(note);
            
            // Update display
            gamePanel.updateScore(gameState.getScore());
            gamePanel.updateCombo(gameState.getCombo());
            
            return true;
        } else {
            // Always treat as miss — don't skip based on nearby notes
            gamePanel.showMissEffect(laneIndex);
            gameState.incrementScore(GameState.Rating.MISS);
            gameState.incrementMisses();
            gamePanel.updateCombo(0);

            if (gameState.getMisses() >= gameState.getMaxMisses() && 
                gameState.getGameMode() != GameState.GameMode.PRACTICE) {
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
        if (!gameRunning || isPaused || gameOverInProgress) return;
        
        // Show a miss effect at the bottom of the screen
        int laneIndex = note.getLaneIndex();
        gamePanel.showMissEffectAtBottom(laneIndex);
        
        // Check if we've reached game over condition (except in practice mode)
        if (gameState.getMisses() >= gameState.getMaxMisses() && 
            gameState.getGameMode() != GameState.GameMode.PRACTICE) {
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
        if (gameRunning && !isPaused && !gameOverInProgress) {
            Note note = new Note(laneIndex, spawnTime, hitTime);
            
            // Adjust note speed based on game mode and difficulty
            float speedMultiplier;
            
            // Practice mode always uses the same speed regardless of difficulty
            if (gameState.getGameMode() == GameState.GameMode.PRACTICE) {
                speedMultiplier = 0.7f; // Slower in practice mode
            } else {
                speedMultiplier = gameState.getDifficultyLevel().getSpeedMultiplier();
            }
            
            // Setting the speed multiplier will automatically adjust hit time
            note.setSpeedMultiplier(speedMultiplier);
            
            gameState.addNote(note);
            gamePanel.addNote(note);
        }
    }
    
    /**
     * Called when the game is over
     */
    public void gameOver() {
        if (!gameRunning || gameOverInProgress) {
            return; // Prevent multiple game over calls
        }
        
        gameOverInProgress = true;
        gameRunning = false;
        
        // Stop all threads immediately
        stopGame();
        
        // Show game over dialog on the EDT
        SwingUtilities.invokeLater(() -> {
            try {
                final int score = gameState.getScore();
                final int excellent = gameState.getExcellentCount();
                final int good = gameState.getGoodCount();
                final int poor = gameState.getPoorCount();
                final int miss = gameState.getMissCount();
                final String songId = gameState.getSongId();
                final String endTime = java.time.Instant.now().toString();
                
                // Add difficulty suffix to song ID for database storage
                final String songIdWithDifficulty;
                if (gameState.getGameMode() != GameState.GameMode.PRACTICE) {
                    songIdWithDifficulty = songId + "_" + gameState.getDifficultyLevel().name();
                } else {
                    songIdWithDifficulty = songId;
                }
                
                // Get the historical highest record 
                final ScoreRecord oldBest = DatabaseManager.getHighScoreRecord(songIdWithDifficulty);
                
                // Only save scores in normal mode
                if (gameState.getGameMode() != GameState.GameMode.PRACTICE) {
                    DatabaseManager.saveDetailedScore(
                            playerName,
                            songIdWithDifficulty,
                            endTime,
                            score,
                            miss,
                            poor,
                            good,
                            excellent
                    );
                }

                String message = String.format(
                    "Game Over!\n\nYour score: %d\n\nExcellent: %d\nGood: %d\nPoor: %d\nMiss: %d",
                    score, excellent, good, poor, miss
                );

                if (gameState.getGameMode() != GameState.GameMode.PRACTICE && 
                    DatabaseManager.isHighScore(songIdWithDifficulty, score)) {
                    message += "\n\nNew High Score!";
                }
                
                // Add game mode and difficulty info to message
                message += "\n\nMode: " + gameState.getGameMode().getDisplayName();
                message += "\nDifficulty: " + gameState.getDifficultyLevel().getDisplayName();

                // show chart
                JPanel chartContainer = new JPanel(new BorderLayout());
                JPanel currentChart = ChartUtils.createRatingPieChart(excellent, good, poor, miss);
                chartContainer.add(currentChart, BorderLayout.CENTER);

                JButton toggleButton = new JButton("Switch to Score Chart");
                toggleButton.addActionListener(e -> {
                    chartContainer.removeAll();
                    
                    if (toggleButton.getText().contains("Score")) {
                        if (oldBest != null) {
                            // Create final local variables to use in the lambda
                            final boolean isNewRecord = score > oldBest.score;
                            final String currentLabel;
                            final String bestLabel;
                            final String chartTitle;
                            
                            if (isNewRecord) {
                                currentLabel = "Top Score: You (" + score + " pts)";
                                bestLabel = "Previous Top: " + oldBest.username + " (" + oldBest.score + " pts)";
                                chartTitle = currentLabel + " vs " + bestLabel;
                            } else {
                                currentLabel = "You (" + score + " pts)";
                                bestLabel = "Top Score: " + oldBest.username + " (" + oldBest.score + " pts)";
                                chartTitle = currentLabel + " vs " + bestLabel;
                            }
                            
                            JPanel chart = ChartUtils.createRatingComparisonBarChart(
                                    excellent, good, poor, miss,
                                    oldBest, 
                                    isNewRecord ? "Top Score: You" : "You", 
                                    isNewRecord ? "Previous Top: " + oldBest.username : "Top Score: " + oldBest.username, 
                                    chartTitle,
                                    new Dimension(500, 300)
                            );
                            chartContainer.add(chart, BorderLayout.CENTER);
                        } else {
                            // In practice mode or no previous records
                            JLabel noComparisonLabel = new JLabel("No comparison data available for this mode/difficulty.");
                            noComparisonLabel.setHorizontalAlignment(SwingConstants.CENTER);
                            chartContainer.add(noComparisonLabel, BorderLayout.CENTER);
                        }
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
                
            } catch (Exception ex) {
                // If anything goes wrong, at least try to get back to the menu
                System.err.println("Error in game over processing: " + ex.getMessage());
                ex.printStackTrace();
                mainApp.returnToMenu();
            }
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
     * Checks if the game is currently paused
     * @return true if the game is paused, false otherwise
     */
    public boolean isPaused() {
        return isPaused;
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