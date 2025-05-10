package com.beatclick;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * MenuPanel - Main menu for the game
 * Allows song selection and displays high scores
 */
public class MenuPanel extends JPanel {
    
    private final BeatClick mainApp;
    private final JPanel songListPanel;
    private final JPanel highScorePanel;
    private final List<String> availableSongs;
    
    /**
     * Constructor
     * @param mainApp The main application
     */
    public MenuPanel(BeatClick mainApp) {
        this.mainApp = mainApp;
        this.availableSongs = new ArrayList<>();
        
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
        
        // Create song list panel
        songListPanel = new JPanel();
        songListPanel.setLayout(new BoxLayout(songListPanel, BoxLayout.Y_AXIS));
        songListPanel.setBackground(new Color(40, 40, 50));
        songListPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100)), 
            "Select a Song", 
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            Color.WHITE));
        
        // Create high score panel
        highScorePanel = new JPanel();
        highScorePanel.setLayout(new BoxLayout(highScorePanel, BoxLayout.Y_AXIS));
        highScorePanel.setBackground(new Color(40, 40, 50));
        highScorePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 100)), 
            "High Scores", 
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            null,
            Color.WHITE));


        // Create scroll panes
        JScrollPane songScrollPane = new JScrollPane(songListPanel);
        songScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        songScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        JScrollPane scoreScrollPane = new JScrollPane(highScorePanel);
        scoreScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scoreScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Add panels to content panel
        contentPanel.add(songScrollPane);
        contentPanel.add(scoreScrollPane);
        
        // Add content panel to main panel
        add(contentPanel, BorderLayout.CENTER);
        
        // Load available songs and high scores
        loadAvailableSongs();
        updateHighScores();
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

        // Add label and import button container
        // JPanel headerPanel = new JPanel(new BorderLayout());
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.X_AXIS));
        headerPanel.setBackground(new Color(40, 40, 50));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Add label
        JLabel instructionLabel = new JLabel("Click a song to play:");
        instructionLabel.setForeground(Color.WHITE);
        instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // instructionLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        // songListPanel.add(instructionLabel);
        headerPanel.add(instructionLabel);

        // Add import button
        JButton importButton = new JButton("+");
        importButton.setToolTipText("Import WAV file");
        importButton.setBackground(new Color(60, 60, 70));
        importButton.setForeground(Color.BLACK); 
        importButton.setFocusPainted(false);
        importButton.setPreferredSize(new Dimension(24, 24));
        importButton.setMaximumSize(new Dimension(24, 24));
        importButton.setMinimumSize(new Dimension(24, 24));
        importButton.setMargin(new Insets(0, 0, 0, 0)); 
        importButton.setFont(new Font("Arial", Font.BOLD, 16));
        
        // Add hover effect
        importButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                importButton.setBackground(new Color(80, 80, 90));
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                importButton.setBackground(new Color(60, 60, 70));
            }
        });
        
        // Add action listener
        importButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importWavFile();
            }
        });

        headerPanel.add(Box.createHorizontalGlue());   
        headerPanel.add(importButton);
        
        // headerPanel.add(importButton, BorderLayout.EAST);
        songListPanel.add(headerPanel);
        
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
     * Creates necessary directories for assets
     */
    private void createAssetsDirectories() {
        // Create assets directories
        File assetsDir = new File("assets");
        File songsDir = new File("assets/songs");
        
        if (!assetsDir.exists()) {
            assetsDir.mkdir();
        }
        
        if (!songsDir.exists()) {
            songsDir.mkdir();
        }
    }
    
    /**
     * Imports a WAV file and generates note data
     */
    private void importWavFile() {
        // Show file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import WAV File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV Files", "wav"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName();
            
            // Make sure it's a WAV file
            if (!fileName.toLowerCase().endsWith(".wav")) {
                JOptionPane.showMessageDialog(this, "Please select a WAV file.", "Invalid File Type", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Create the destination directory if needed
            createAssetsDirectories();
            
            try {
                // Show a progress dialog
                JDialog progressDialog = createProgressDialog("Importing song...");
                
                // Set up task for importing and generating note data
                SwingWorker<Boolean, Void> importTask = new SwingWorker<Boolean, Void>() {
                    @Override
                    protected Boolean doInBackground() throws Exception {
                        // Copy file to assets/songs directory
                        Path sourcePath = selectedFile.toPath();
                        Path destPath = Paths.get("assets/songs", fileName);
                        Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                        
                        // Get song ID (filename without extension)
                        String songId = fileName.substring(0, fileName.lastIndexOf('.'));
                        
                        // Generate notes using the static method
                        return NoteGenerator.generateNotesForSong(songId);
                    }
                    
                    @Override
                    protected void done() {
                        // Close progress dialog
                        progressDialog.dispose();
                        
                        try {
                            boolean success = get();
                            
                            // Refresh song list
                            loadAvailableSongs();
                            updateHighScores();
                            
                            // Show success message
                            if (success) {
                                JOptionPane.showMessageDialog(MenuPanel.this, 
                                    "Song imported successfully!", "Import Complete", JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(MenuPanel.this, 
                                    "Song imported but note generation may have failed. You can still play the song.",
                                    "Partial Import", JOptionPane.WARNING_MESSAGE);
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(MenuPanel.this, 
                                "Error completing import: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };
                
                importTask.execute();
                progressDialog.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error importing song: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Creates a progress dialog for long operations
     * @param message The message to display
     * @return The progress dialog
     */
    private JDialog createProgressDialog(String message) {
          Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        
          JDialog dialog = new JDialog(parent, "Processing", true);
          
          JPanel panel = new JPanel(new BorderLayout(10, 10));
          panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
          panel.setBackground(new Color(40, 40, 50));
          
          JLabel messageLabel = new JLabel(message);
          messageLabel.setForeground(Color.WHITE);
          
          JProgressBar progressBar = new JProgressBar();
          progressBar.setIndeterminate(true);
          
          panel.add(messageLabel, BorderLayout.NORTH);
          panel.add(progressBar, BorderLayout.CENTER);
          
          dialog.setContentPane(panel);
          dialog.setSize(300, 100);
          dialog.setLocationRelativeTo(this);
          dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
          
          return dialog;
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
                mainApp.startGame(songName);
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
        
        // Get high scores from database
        for (String songName : availableSongs) {
            int highScore = DatabaseManager.getHighScoreFromDetailedTable(songName);
            
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
                    showLeaderboardDialog(songName);
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