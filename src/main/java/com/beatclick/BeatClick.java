package com.beatclick;

import javax.swing.*;
import java.awt.*;

/**
 * BeatClick - Main application class for the BeatClick rhythm game
 * A music-based timing game where players tap notes in sync with music
 * 
 * @authors Suwen Wang (sw6359), Yifei Xu (yx3590), Ruqing Liu (rl5652)
 */
public class BeatClick {
    
    // Application constants
    private static final String APP_TITLE = "BeatClick";
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    
    private JFrame mainWindow;
    private GameManager gameManager;
    
    /**
     * Constructor - initializes the game application
     */
    public BeatClick() {
        initializeUI();
        gameManager = new GameManager(mainWindow);
        gameManager.setMainApp(this);

    }
    
    /**
     * Sets up the main application window and UI components
     */
    private void initializeUI() {
        mainWindow = new JFrame(APP_TITLE);
        mainWindow.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainWindow.setResizable(false);
        mainWindow.setLocationRelativeTo(null);
        
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set look and feel: " + e.getMessage());
        }
        
        // Add game panel to main window
        mainWindow.setLayout(new BorderLayout());
        
        // Create menu panel
        MenuPanel menuPanel = new MenuPanel(this);
        mainWindow.add(menuPanel, BorderLayout.CENTER);
        
        mainWindow.setVisible(true);
    }
    
    /**
     * Starts a new game with the selected song
     * @param songId The ID of the selected song
     */
    public void startGame(String songId) {

        // Prompt user for name
        String playerName = JOptionPane.showInputDialog(mainWindow, "Enter your name:", "Player Name", JOptionPane.PLAIN_MESSAGE);

        if (playerName == null) {
            // User clicked "Cancel" → do not start game
            return;
        }

        if (playerName.trim().isEmpty()) {
            // Empty name but confirmed → use "Anonymous"
            playerName = "Anonymous";
        }

        mainWindow.getContentPane().removeAll();
        
        GamePanel gamePanel = new GamePanel();
        mainWindow.add(gamePanel, BorderLayout.CENTER);
        gameManager.setPlayerName(playerName.trim()); // Set player name here
        gameManager.startGame(songId, gamePanel);
        
        mainWindow.revalidate();
        mainWindow.repaint();
    }
    
    /**
     * Returns to the main menu
     */
    public void returnToMenu() {
        if (gameManager != null) {
            gameManager.stopGame();
        }
        
        mainWindow.getContentPane().removeAll();
        MenuPanel menuPanel = new MenuPanel(this);
        mainWindow.add(menuPanel, BorderLayout.CENTER);
        
        mainWindow.revalidate();
        mainWindow.repaint();
    }
    
    /**
     * Application entry point
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Use event dispatch thread for Swing applications
        SwingUtilities.invokeLater(() -> {
            new BeatClick();
        });
    }
}