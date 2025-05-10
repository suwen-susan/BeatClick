package com.beatclick;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * MenuPanel - Main menu for the game
 * Allows song selection and displays high scores
 */
public class MenuPanel extends JPanel {
    
    private final BeatClick mainApp;
    private final JPanel songListPanel;
    private final JPanel highScorePanel;
    private final List<String> availableSongs;
    
    // Game mode and difficulty selection
    private GameState.GameMode selectedGameMode = GameState.GameMode.NORMAL;
    private GameState.DifficultyLevel selectedDifficulty = GameState.DifficultyLevel.MEDIUM;
    
    // UI components for settings
    private JPanel settingsPanel;
    private JComboBox<String> gameModeSelector;
    private JComboBox<String> difficultySelector;
    private JTextArea modeDescriptionText;
    
    /**
     * Constructor
     * @param mainApp The main application
     */
    public MenuPanel(BeatClick mainApp) {
        this.mainApp = mainApp;
        this.availableSongs = new ArrayList<>();
        
        // Initialize with Medium difficulty as default
        this.selectedGameMode = GameState.GameMode.NORMAL;
        this.selectedDifficulty = GameState.DifficultyLevel.MEDIUM;
        
        // Set panel properties
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 40));
        
        // Create header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Create main content panel
        JPanel contentPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        contentPanel.setBackground(new Color(30, 30, 40));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create left panel (song list and settings)
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(new Color(30, 30, 40));
        
        // Create settings panel
        settingsPanel = createSettingsPanel();
        leftPanel.add(settingsPanel, BorderLayout.NORTH);
        
        // Create song list panel
        songListPanel = new JPanel();
        songListPanel.setLayout(new BoxLayout(songListPanel, BoxLayout.Y_AXIS));
        songListPanel.setBackground(new Color(40, 40, 50));
        songListPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)), "Select a Song"));
        
        JScrollPane songScrollPane = new JScrollPane(songListPanel);
        songScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        songScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        leftPanel.add(songScrollPane, BorderLayout.CENTER);
        
        // Create high score panel
        highScorePanel = new JPanel();
        highScorePanel.setLayout(new BoxLayout(highScorePanel, BoxLayout.Y_AXIS));
        highScorePanel.setBackground(new Color(40, 40, 50));
        highScorePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)), "High Scores"));
        
        JScrollPane scoreScrollPane = new JScrollPane(highScorePanel);
        scoreScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scoreScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Add panels to content panel
        contentPanel.add(leftPanel);
        contentPanel.add(scoreScrollPane);
        
        // Add content panel to main panel
        add(contentPanel, BorderLayout.CENTER);
        
        // Load available songs and high scores
        loadAvailableSongs();
        updateHighScores();
    }
    
    /**
     * Creates the settings panel for game mode and difficulty selection
     * @return The settings panel
     */
    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(100, 100, 100)), "Game Settings"));
        
        // Create a grid for the selectors
        JPanel selectorPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        selectorPanel.setBackground(new Color(40, 40, 50));
        selectorPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Game mode selector
        JLabel gameModeLabel = new JLabel("Game Mode:");
        gameModeLabel.setForeground(Color.WHITE);
        
        String[] gameModes = new String[GameState.GameMode.values().length];
        for (int i = 0; i < GameState.GameMode.values().length; i++) {
            gameModes[i] = GameState.GameMode.values()[i].getDisplayName();
        }
        
        gameModeSelector = new JComboBox<>(gameModes);
        gameModeSelector.setBackground(new Color(60, 60, 70));
        gameModeSelector.setForeground(Color.WHITE);
        gameModeSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(new Color(80, 80, 90));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(new Color(60, 60, 70));
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        });
        
        // Difficulty selector
        JLabel difficultyLabel = new JLabel("Difficulty:");
        difficultyLabel.setForeground(Color.WHITE);
        
        String[] difficulties = new String[GameState.DifficultyLevel.values().length];
        for (int i = 0; i < GameState.DifficultyLevel.values().length; i++) {
            difficulties[i] = GameState.DifficultyLevel.values()[i].getDisplayName();
        }
        
        difficultySelector = new JComboBox<>(difficulties);
        difficultySelector.setBackground(new Color(60, 60, 70));
        difficultySelector.setForeground(Color.WHITE);
        
        // Explicitly set the selected index to MEDIUM (which is index 1)
        difficultySelector.setSelectedIndex(1); // MEDIUM is at index 1 (EASY=0, MEDIUM=1, HARD=2)
        
        difficultySelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(new Color(80, 80, 90));
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(new Color(60, 60, 70));
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        });
        
        // Add selectors to the panel
        selectorPanel.add(gameModeLabel);
        selectorPanel.add(gameModeSelector);
        selectorPanel.add(difficultyLabel);
        selectorPanel.add(difficultySelector);
        
        // Create description text area
        modeDescriptionText = new JTextArea();
        modeDescriptionText.setEditable(false);
        modeDescriptionText.setLineWrap(true);
        modeDescriptionText.setWrapStyleWord(true);
        modeDescriptionText.setBackground(new Color(50, 50, 60));
        modeDescriptionText.setForeground(Color.WHITE);
        modeDescriptionText.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(70, 70, 80)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Set initial description
        updateModeDescription();
        
        // Add listener to update description when mode changes
        gameModeSelector.addActionListener(e -> {
            int selectedIndex = gameModeSelector.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < GameState.GameMode.values().length) {
                selectedGameMode = GameState.GameMode.values()[selectedIndex];
                updateModeDescription();
                
                // Disable difficulty selector in practice mode
                difficultySelector.setEnabled(selectedGameMode != GameState.GameMode.PRACTICE);
                
                // Update high scores display for the new mode/difficulty
                updateHighScores();
            }
        });
        
        // Add listener for difficulty changes
        difficultySelector.addActionListener(e -> {
            int selectedIndex = difficultySelector.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < GameState.DifficultyLevel.values().length) {
                selectedDifficulty = GameState.DifficultyLevel.values()[selectedIndex];
                updateModeDescription();
                updateHighScores();
            }
        });
        
        // Add components to the main panel
        panel.add(selectorPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(modeDescriptionText), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Updates the mode description text based on current selections
     */
    private void updateModeDescription() {
        StringBuilder description = new StringBuilder();
        
        // Game mode description
        description.append(selectedGameMode.getDisplayName()).append(": ");
        description.append(selectedGameMode.getDescription()).append("\n\n");
        
        // Only show difficulty info in normal mode
        if (selectedGameMode == GameState.GameMode.NORMAL) {
            description.append(selectedDifficulty.getDisplayName()).append(": ");
            description.append(selectedDifficulty.getDescription()).append("\n");
            
            // Add score multiplier info
            description.append("Score multiplier: x").append(selectedDifficulty.getScoreMultiplier());
            
            // Add speed info
            float speedMultiplier = selectedDifficulty.getSpeedMultiplier();
            description.append("\nNote speed: ");
            if (speedMultiplier < 1.0f) {
                description.append("Slower (").append(Math.round(speedMultiplier * 100)).append("%)");
            } else if (speedMultiplier > 1.0f) {
                description.append("Faster (").append(Math.round(speedMultiplier * 100)).append("%)");
            } else {
                description.append("Normal (100%)");
            }
        } else if (selectedGameMode == GameState.GameMode.PRACTICE) {
            description.append("In practice mode, notes move at 70% of normal speed and ");
            description.append("your health does not deplete when missing notes.");
        }
        
        modeDescriptionText.setText(description.toString());
    }
    
    /**
     * Creates the header panel with logo and title
     * @return The header panel
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(20, 20, 30));
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Create title label
        JLabel titleLabel = new JLabel("BeatClick");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Create subtitle label
        JLabel subtitleLabel = new JLabel("A Music Rhythm Game");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        subtitleLabel.setForeground(new Color(200, 200, 200));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Add components to header panel
        JPanel labelPanel = new JPanel(new GridLayout(2, 1));
        labelPanel.setBackground(new Color(20, 20, 30));
        labelPanel.add(titleLabel);
        labelPanel.add(subtitleLabel);
        
        headerPanel.add(labelPanel, BorderLayout.CENTER);
        
        return headerPanel;
    }
    
    /**
     * Loads available songs from the assets directory
     */
    private void loadAvailableSongs() {
        availableSongs.clear();
        songListPanel.removeAll();
        
        // Add label
        JLabel instructionLabel = new JLabel("Click a song to play:");
        instructionLabel.setForeground(Color.WHITE);
        instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        instructionLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        songListPanel.add(instructionLabel);
        
        // Check if assets directory exists
        File songDir = new File("assets/songs");
        if (!songDir.exists() || !songDir.isDirectory()) {
            // Create demo songs if directory doesn't exist
            createDemoSongs();
        }
        
        // Load songs from directory
        File[] songFiles = songDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
        
        if (songFiles != null && songFiles.length > 0) {
            for (File songFile : songFiles) {
                String songName = songFile.getName().replace(".wav", "");
                availableSongs.add(songName);
                
                JButton songButton = createSongButton(songName);
                songListPanel.add(songButton);
            }
        } else {
            // No songs found
            JLabel noSongsLabel = new JLabel("No songs found. Add .wav files to assets/songs directory.");
            noSongsLabel.setForeground(Color.GRAY);
            noSongsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            noSongsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
            songListPanel.add(noSongsLabel);
        }
        
        // Add empty panel to push content to top
        JPanel fillerPanel = new JPanel();
        fillerPanel.setBackground(new Color(40, 40, 50));
        songListPanel.add(fillerPanel);
        
        songListPanel.revalidate();
        songListPanel.repaint();
    }
    
    /**
     * Creates a button for a song
     * @param songName The name of the song
     * @return The song button
     */
    private JButton createSongButton(String songName) {
        JButton songButton = new JButton(songName);
        songButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        songButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        songButton.setBackground(new Color(60, 60, 70));
        songButton.setForeground(Color.BLACK);
        songButton.setFocusPainted(false);
        songButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 90)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // Add hover effect
        songButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                songButton.setBackground(new Color(80, 80, 90));
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                songButton.setBackground(new Color(60, 60, 70));
            }
        });
        
        // Add action listener
        songButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainApp.startGame(songName, selectedGameMode, selectedDifficulty);
            }
        });
        
        return songButton;
    }
    
    /**
     * Updates the high scores panel
     */
    private void updateHighScores() {
        highScorePanel.removeAll();
        
        // Add label
        JLabel highScoreLabel = new JLabel("Top Scores:");
        highScoreLabel.setForeground(Color.WHITE);
        highScoreLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        highScoreLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        highScorePanel.add(highScoreLabel);
        
        // Add difficulty label if in normal mode
        if (selectedGameMode == GameState.GameMode.NORMAL) {
            JLabel difficultyLabel = new JLabel("Difficulty: " + selectedDifficulty.getDisplayName());
            difficultyLabel.setForeground(new Color(200, 200, 255));
            difficultyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            difficultyLabel.setBorder(new EmptyBorder(0, 10, 10, 10));
            highScorePanel.add(difficultyLabel);
        } else {
            JLabel practiceLabel = new JLabel("Practice mode - scores not saved");
            practiceLabel.setForeground(new Color(200, 200, 150));
            practiceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            practiceLabel.setBorder(new EmptyBorder(0, 10, 10, 10));
            highScorePanel.add(practiceLabel);
        }
        
        // Get high scores from database for the selected difficulty
        for (String songName : availableSongs) {
            // Add difficulty suffix for normal mode
            String songIdWithDifficulty = selectedGameMode == GameState.GameMode.NORMAL ?
                songName + "_" + selectedDifficulty.name() : songName;
            
            int highScore = DatabaseManager.getHighScoreFromDetailedTable(songIdWithDifficulty);
            
            JPanel scorePanel = new JPanel(new BorderLayout());
            scorePanel.setBackground(new Color(50, 50, 60));
            scorePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(70, 70, 80)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            scorePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            
            JLabel songLabel = new JLabel(songName);
            songLabel.setForeground(Color.WHITE);
            
            JLabel scoreLabel = new JLabel(String.valueOf(highScore));
            scoreLabel.setForeground(new Color(255, 215, 0)); // Gold
            
            scorePanel.add(songLabel, BorderLayout.WEST);
            scorePanel.add(scoreLabel, BorderLayout.EAST);

            // Add click listener to panel
            scorePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            scorePanel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    showLeaderboardDialog(songIdWithDifficulty);
                }
            });

            highScorePanel.add(scorePanel);
        }
        
        // Add empty panel to push content to top
        JPanel fillerPanel = new JPanel();
        fillerPanel.setBackground(new Color(40, 40, 50));
        highScorePanel.add(fillerPanel);
        
        highScorePanel.revalidate();
        highScorePanel.repaint();
    }
    
    /**
     * Creates demo songs if the assets directory doesn't exist
     */
    private void createDemoSongs() {
        // Create assets directories
        File assetsDir = new File("assets");
        File songsDir = new File("assets/songs");
        
        if (!assetsDir.exists()) {
            assetsDir.mkdir();
        }
        
        if (!songsDir.exists()) {
            songsDir.mkdir();
        }
        
        // In a real implementation, you would include sample WAV files with your project
        // For this example, we'll just show placeholders in the UI
        
        JLabel placeholderLabel = new JLabel("Demo Mode - No actual song files");
        placeholderLabel.setForeground(Color.YELLOW);
        placeholderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        placeholderLabel.setBorder(new EmptyBorder(10, 10, 20, 10));
        songListPanel.add(placeholderLabel);
        
        // Add demo "songs"
        String[] demoSongs = {"Demo Song 1", "Demo Song 2", "Demo Song 3"};
        for (String songName : demoSongs) {
            availableSongs.add(songName);
            JButton songButton = createSongButton(songName);
            songListPanel.add(songButton);
        }
    }
    
    /**
     * Click each song name and show Leaderboard Dialog
     */
    private void showLeaderboardDialog(String songId) {
        List<ScoreRecord> records = DatabaseManager.getTopScoresBySong(songId);
        Dimension panelSize = this.getSize();  // MenuPanel 当前大小
        if (records.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data available for this song.", "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel chartPanel = ChartUtils.createLeaderboardBarChart(songId, records, panelSize);
        JOptionPane.showMessageDialog(this, chartPanel, "Leaderboard: " + songId, JOptionPane.PLAIN_MESSAGE);
    }
}