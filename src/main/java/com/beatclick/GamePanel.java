package com.beatclick;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
/**
 * GamePanel - Handles rendering of the game
 * Displays notes, animations, and game state
 */
public class
GamePanel extends JPanel {
    
    // Constants for rendering
    private static final int NUM_LANES = 4;
    
    // Visual Theme options
    public enum NoteDesign {
        CLASSIC,     // Classic square notes
        CIRCULAR,    // Circular notes
        DIAMOND      // Diamond shaped notes
    }
    
    public enum LaneDesign {
        STANDARD,    // Standard colored lanes
        NEON,        // Bright neon colors
        MINIMAL      // Minimal grayscale
    }
    
    // Default theme settings
    private NoteDesign noteDesign = NoteDesign.CLASSIC;
    private LaneDesign laneDesign = LaneDesign.STANDARD;
    
    // Color themes for lanes
    private static final Color[][] LANE_COLOR_THEMES = {
        // STANDARD theme
        {
            new Color(231, 76, 60),   // Red
            new Color(241, 196, 15),  // Yellow
            new Color(46, 204, 113),  // Green
            new Color(52, 152, 219)   // Blue
        },
        // NEON theme
        {
            new Color(255, 41, 117),  // Neon pink
            new Color(63, 255, 33),   // Neon green
            new Color(33, 217, 255),  // Neon blue
            new Color(255, 189, 33)   // Neon orange
        },
        // MINIMAL theme
        {
            new Color(60, 60, 60),    // Dark gray
            new Color(90, 90, 90),    // Medium gray
            new Color(120, 120, 120), // Light gray
            new Color(150, 150, 150)  // Very light gray
        }
    };
    
    private static final Color TARGET_LINE_COLOR = new Color(255, 255, 255);
    private static final Color BACKGROUND_COLOR = new Color(20, 20, 30);
    private static final int NOTE_SIZE = 50;
    private static final int TARGET_LINE_HEIGHT = 5;
    
    // Rating colors
    private static final Color EXCELLENT_COLOR = new Color(255, 215, 0, 180); // Gold
    private static final Color GOOD_COLOR = new Color(0, 255, 0, 150);        // Green
    private static final Color POOR_COLOR = new Color(0, 175, 255, 150);      // Blue
    private static final Color MISS_COLOR = new Color(255, 0, 0, 150);        // Red
    
    // Lane target positions (where notes should be hit)
    private static final float TARGET_POSITION = 0.85f; // 85% down the screen
    
    // Lists for notes and visual effects
    private final List<Note> notes = new CopyOnWriteArrayList<>();
    private final List<VisualEffect> effects = new CopyOnWriteArrayList<>();
    
    // Game state
    private int score = 0;
    private int combo = 0;
    private GameState gameState;

    // Pause button properties
    private Rectangle pauseButtonRect; // Pause button rectangle for hit detection
    private boolean isPauseButtonHovered = false; // Track if mouse is hovering over pause button


    /**
     * Gets the current game state
     * @return The current game state
     */
    public GameState getGameState() {
        return this.gameState;
    }

    /**
     * Inner class for visual effects (hit animations, etc.)
     */
    private static class VisualEffect {
        private final int laneIndex;
        private final EffectType type;
        private int framesRemaining;
        private String text;
        
        enum EffectType {
            EXCELLENT, GOOD, POOR, MISS
        }
        
        
        /**
         * Constructor for effects at note position
         */
        public VisualEffect(int laneIndex, EffectType type, int duration) {
            this.laneIndex = laneIndex;
            this.type = type;
            this.framesRemaining = duration;
            
            // Set text based on effect type
            switch (type) {
                case EXCELLENT:
                    this.text = "EXCELLENT";
                    break;
                case GOOD:
                    this.text = "GOOD";
                    break;
                case POOR:
                    this.text = "POOR";
                    break;
                case MISS:
                    this.text = "MISS";
                    break;
            }
        }
        
        public boolean update() {
            return --framesRemaining <= 0;
        }
        
        public int getLaneIndex() {
            return laneIndex;
        }
        
        public EffectType getType() {
            return type;
        }
        
        public String getText() {
            return text;
        }

        public int getFramesRemaining() {
            return framesRemaining;
        }

    }
    
    /**
     * Constructor
     */
    public GamePanel() {
        setBackground(BACKGROUND_COLOR);
        setFocusable(true);
        requestFocusInWindow();

        // Add mouse listener for pause button
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (pauseButtonRect != null && pauseButtonRect.contains(e.getPoint())) {
                    handlePauseButtonClick();
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }
        });
        
        // Add mouse motion listener for hover effects
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean wasHovered = isPauseButtonHovered;
                isPauseButtonHovered = pauseButtonRect != null && pauseButtonRect.contains(e.getPoint());
                
                // Only repaint if hover state changed
                if (wasHovered != isPauseButtonHovered) {
                    repaint(pauseButtonRect.x, pauseButtonRect.y, 
                        pauseButtonRect.width, pauseButtonRect.height);
                }
                
                // Change cursor when hovering over button
                if (isPauseButtonHovered) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        });
    }

// In GamePanel.java, update handlePauseButtonClick() method:

// In GamePanel.java:

private void handlePauseButtonClick() {
    if (gameState != null) {
        boolean currentlyPaused = gameState.isPaused();
        
        if (!currentlyPaused) {
            // Pause the game
            gameState.getGameManager().pauseGame();
            
            // Show pause dialog
            JPanel pausePanel = createPausePanel();
            
            // Create a dialog that will stay on top
            final JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Game Paused", true);
            dialog.setContentPane(pausePanel);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setSize(300, 220);
            dialog.setLocationRelativeTo(this);
            dialog.setAlwaysOnTop(true);
            
            // Add WindowListener to handle when user clicks the X button
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    // Resume game when dialog is closed with X
                    gameState.getGameManager().resumeGame();
                }
            });
            
            // Add escape key handler to close dialog
            KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
            dialog.getRootPane().registerKeyboardAction(e -> {
                dialog.dispose();
                gameState.getGameManager().resumeGame();
            }, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
            
            // Disable the pause button until the dialog is closed
            isPauseButtonHovered = false;
            
            // Show the dialog (this will block until dialog is closed)
            dialog.setVisible(true);
            
            // Request focus after dialog closes
            SwingUtilities.invokeLater(() -> {
                requestFocusInWindow();
            });
        }
    }
}


     /**
     * Creates the pause menu panel with resume and main menu buttons
     * @return Panel containing pause menu options
     */
    private JPanel createPausePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(40, 40, 50));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Main buttons panel
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        buttonsPanel.setBackground(new Color(40, 40, 50));
        
        JButton resumeButton = createPauseMenuButton("Resume Game");
        JButton mainMenuButton = createPauseMenuButton("Return to Main Menu");
        
        resumeButton.addActionListener(e -> {
            // Find and close the dialog
            Component component = resumeButton;
            while (component != null && !(component instanceof JDialog)) {
                component = component.getParent();
            }
            
            if (component != null) {
                ((JDialog) component).dispose();
            }
            
            // Resume game
            gameState.getGameManager().resumeGame();
        });
        
        mainMenuButton.addActionListener(e -> {
            // Find and close the dialog
            Component component = mainMenuButton;
            while (component != null && !(component instanceof JDialog)) {
                component = component.getParent();
            }
            
            if (component != null) {
                ((JDialog) component).dispose();
            }
            
            // Return to main menu
            gameState.getGameManager().getMainApp().returnToMenu();
        });
        
        buttonsPanel.add(resumeButton);
        buttonsPanel.add(mainMenuButton);
        
        // Appearance settings panel
        JPanel appearancePanel = new JPanel(new GridLayout(2, 2, 10, 10));
        appearancePanel.setBackground(new Color(40, 40, 50));
        appearancePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 90), 1),
            "Appearance Settings"
        ));
        
        // Note design selector
        JLabel noteLabel = new JLabel("Note Style:");
        noteLabel.setForeground(Color.WHITE);
        
        String[] noteDesigns = {"Classic", "Circular", "Diamond"};
        JComboBox<String> noteDesignSelector = new JComboBox<>(noteDesigns);
        noteDesignSelector.setSelectedIndex(noteDesign.ordinal());
        noteDesignSelector.addActionListener(e -> {
            int index = noteDesignSelector.getSelectedIndex();
            if (index >= 0 && index < NoteDesign.values().length) {
                noteDesign = NoteDesign.values()[index];
                repaint();
            }
        });
        
        // Lane design selector
        JLabel laneLabel = new JLabel("Lane Style:");
        laneLabel.setForeground(Color.WHITE);
        
        String[] laneDesigns = {"Standard", "Neon", "Minimal"};
        JComboBox<String> laneDesignSelector = new JComboBox<>(laneDesigns);
        laneDesignSelector.setSelectedIndex(laneDesign.ordinal());
        laneDesignSelector.addActionListener(e -> {
            int index = laneDesignSelector.getSelectedIndex();
            if (index >= 0 && index < LaneDesign.values().length) {
                laneDesign = LaneDesign.values()[index];
                repaint();
            }
        });
        
        appearancePanel.add(noteLabel);
        appearancePanel.add(noteDesignSelector);
        appearancePanel.add(laneLabel);
        appearancePanel.add(laneDesignSelector);
        
        // Add both panels to main panel
        panel.add(buttonsPanel, BorderLayout.NORTH);
        panel.add(appearancePanel, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Creates a styled button for the pause menu
     * @param text The button text
     * @return Styled JButton
     */
    private JButton createPauseMenuButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(60, 60, 70));
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 80, 90), 1),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(80, 80, 90));
            }
            
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(60, 60, 70));
            }
        });
        
        return button;
    }
    
    /**
     * Configures the input handler for this panel
     * @param inputHandler The input handler
     */
    public void configureInputHandler(InputHandler inputHandler) {
        // Store our custom mouse listeners before removing all
        MouseListener[] mouseListeners = getMouseListeners();
        
        // Remove existing listeners first
        for (KeyListener listener : getKeyListeners()) {
            removeKeyListener(listener);
        }
        for (MouseListener listener : mouseListeners) {
            if (listener instanceof InputHandler) {
                // Only remove InputHandler listeners, keep our pause button listener
                removeMouseListener(listener);
            }
        }
        
        // Add new listeners
        addKeyListener(inputHandler);
        addMouseListener(inputHandler);
    }

    /**
     * Checks if a point is within the pause button area
     * @param point The point to check
     * @return true if the point is on the pause button, false otherwise
     */
    public boolean isPauseButtonClick(Point point) {
        // If we're already paused, don't allow clicking the pause button
        if (gameState != null && gameState.isPaused()) {
            return false;
        }
        
        return pauseButtonRect != null && pauseButtonRect.contains(point);
    }
        
    /**
     * Adds a note to be rendered
     * @param note The note to add
     */
    public void addNote(Note note) {
        notes.add(note);
    }

    /**
     * Removes a note from rendering (e.g. after it's hit)
     * @param note The note to remove
     */
    public void removeNote(Note note) {
        boolean wasPresent = notes.remove(note);
        repaint();
    }

    /**
     * Updates all animations and effects
     */
    public void updateAnimations() {
         // If game is paused, don't update anything
        if (gameState != null && gameState.isPaused()) {
            return;
        }

        // Move the notes from upcoming to active
        gameState.updateNotes();
        
        // Update notes
        long currentTime = gameState.getCurrentGameTime();
        
        // Collect notes to remove instead of removing during iteration
        List<Note> notesToRemove = new ArrayList<>();
        for (Note note : notes) {
            note.updatePosition(currentTime);
            
            // Mark notes that have moved past the bottom of the screen for removal
            if (note.getYPosition() >= 1.0f) {
                notesToRemove.add(note);
            }
        }
        
        // Remove collected notes
        if (!notesToRemove.isEmpty()) {
            notes.removeAll(notesToRemove);
        }
        
        // Update visual effects - similar approach
        List<VisualEffect> effectsToRemove = new ArrayList<>();
        for (VisualEffect effect : effects) {
            if (effect.update()) {
                effectsToRemove.add(effect);
            }
        }
        
        // Remove collected effects
        if (!effectsToRemove.isEmpty()) {
            effects.removeAll(effectsToRemove);
        }
    }
    
    /**
     * Shows a hit effect at the note position
     * @param laneIndex The lane index
     * @param rating The rating of the hit
     */
    public void showHitEffect(int laneIndex, GameState.Rating rating) {
        VisualEffect.EffectType effectType;
        
        switch (rating) {
            case EXCELLENT:
                effectType = VisualEffect.EffectType.EXCELLENT;
                break;
            case GOOD:
                effectType = VisualEffect.EffectType.GOOD;
                break;
            case POOR:
                effectType = VisualEffect.EffectType.POOR;
                break;
            default:
                effectType = VisualEffect.EffectType.MISS;
                break;
        }
        
        // Longer duration for better ratings
        int duration = (rating == GameState.Rating.EXCELLENT) ? 20 : 
                      (rating == GameState.Rating.GOOD) ? 15 : 10;
        
        effects.add(new VisualEffect(laneIndex, effectType, duration));
    }

    
    /**
     * Shows a miss effect at the target line (when no note was present)
     * @param laneIndex The lane index
     */
    public void showMissEffect(int laneIndex) {
        effects.add(new VisualEffect(laneIndex, VisualEffect.EffectType.MISS, 10));
    }

    /**
     * Shows a health recovery effect (e.g. when a health item is collected)
     */
    public void showHealthRecoveryEffect() {
        // create a label for the recovery effect
        JLabel recoveryLabel = new JLabel("+1 HP");
        recoveryLabel.setForeground(new Color(50, 255, 50)); 
        recoveryLabel.setFont(new Font("Arial", Font.BOLD, 20));
        
        // set the label position beside the health bar
        int width = getWidth();
        recoveryLabel.setBounds(width - 200, 50, 100, 30);
        add(recoveryLabel);
        
        // set a timer to remove the label after a short duration
        Timer timer = new Timer(1000, e -> {
            remove(recoveryLabel);
            repaint();
        });
        timer.setRepeats(false);
        timer.start();
        
        repaint();
    }
    
    /**
     * Shows a miss effect at the bottom of the screen when a note passes untouched
     * @param laneIndex The lane index
     */
    public void showMissEffectAtBottom(int laneIndex) {
        effects.add(new VisualEffect(laneIndex, VisualEffect.EffectType.MISS, 15));
    }
    
    /**
     * Updates the score display
     * @param score The new score
     */
    public void updateScore(int score) {
        this.score = score;
    }
    
    /**
     * Updates the combo display
     * @param combo The new combo
     */
    public void updateCombo(int combo) {
        this.combo = combo;
    }
    
    /**
     * Set the note design style
     * @param noteDesign The note design to use
     */
    public void setNoteDesign(NoteDesign noteDesign) {
        this.noteDesign = noteDesign;
        repaint();
    }
    
    /**
     * Set the lane design style
     * @param laneDesign The lane design to use
     */
    public void setLaneDesign(LaneDesign laneDesign) {
        this.laneDesign = laneDesign;
        repaint();
    }
    
    /**
     * Custom painting for the game panel
     * @param g The graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable antialiasing for smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Get panel dimensions
        int width = getWidth();
        int height = getHeight();
        int laneWidth = width / NUM_LANES;
        
        // Get lane colors based on current theme
        Color[] laneColors = LANE_COLOR_THEMES[laneDesign.ordinal()];
        
        // Draw lanes
        for (int i = 0; i < NUM_LANES; i++) {
            int laneX = i * laneWidth;
            
            // Draw lane background
            g2d.setColor(laneColors[i].darker().darker());
            g2d.fillRect(laneX, 0, laneWidth, height);
            
            // Draw lane separator
            g2d.setColor(Color.BLACK);
            g2d.drawLine(laneX, 0, laneX, height);
        }
        
        // Draw target line
        int targetY = (int)(TARGET_POSITION * height);
        g2d.setColor(TARGET_LINE_COLOR);
        g2d.fillRect(0, targetY, width, TARGET_LINE_HEIGHT);
        
        // Draw notes with the selected note design
        for (Note note : notes) {
            int laneIndex = note.getLaneIndex();
            int laneX = laneIndex * laneWidth;
            int noteY = (int)(note.getYPosition() * height) - NOTE_SIZE / 2;
            int centerX = laneX + laneWidth/2;
            int centerY = noteY + NOTE_SIZE/2;
            
            // Draw note with lane color
            g2d.setColor(laneColors[laneIndex]);
            
            switch (noteDesign) {
                case CIRCULAR:
                    // Draw circular note
                    g2d.fillOval(centerX - NOTE_SIZE/2, noteY, NOTE_SIZE, NOTE_SIZE);
                    g2d.setColor(Color.WHITE);
                    g2d.drawOval(centerX - NOTE_SIZE/2, noteY, NOTE_SIZE, NOTE_SIZE);
                    break;
                    
                case DIAMOND:
                    // Draw diamond note
                    int[] xPoints = {
                        centerX, 
                        centerX + NOTE_SIZE/2, 
                        centerX, 
                        centerX - NOTE_SIZE/2
                    };
                    int[] yPoints = {
                        noteY, 
                        centerY, 
                        noteY + NOTE_SIZE, 
                        centerY
                    };
                    g2d.fillPolygon(xPoints, yPoints, 4);
                    g2d.setColor(Color.WHITE);
                    g2d.drawPolygon(xPoints, yPoints, 4);
                    break;
                    
                case CLASSIC:
                default:
                    // Draw classic square note
                    g2d.fillRoundRect(centerX - NOTE_SIZE/2, noteY, NOTE_SIZE, NOTE_SIZE, 10, 10);
                    g2d.setColor(Color.WHITE);
                    g2d.drawRoundRect(centerX - NOTE_SIZE/2, noteY, NOTE_SIZE, NOTE_SIZE, 10, 10);
                    break;
            }
        }

        // Find the most recent/important effect for center display
        boolean hasActiveEffect = false;
        VisualEffect.EffectType centerEffectType = null;
        int centerEffectRemaining = 0;

        for (VisualEffect effect : effects) {
            if (!hasActiveEffect || 
                (centerEffectType == VisualEffect.EffectType.MISS && effect.getType() != VisualEffect.EffectType.MISS) ||
                (centerEffectType == VisualEffect.EffectType.POOR && 
                 (effect.getType() == VisualEffect.EffectType.GOOD || effect.getType() == VisualEffect.EffectType.EXCELLENT)) ||
                (centerEffectType == VisualEffect.EffectType.GOOD && effect.getType() == VisualEffect.EffectType.EXCELLENT)) {
                
                hasActiveEffect = true;
                centerEffectType = effect.getType();
                centerEffectRemaining = effect.getFramesRemaining();
            }
        }

        // Draw the center effect if there is one
        if (hasActiveEffect) {
            // Get effect details
            Color effectColor;
            String effectText;
            
            switch (centerEffectType) {
                case EXCELLENT:
                    effectColor = EXCELLENT_COLOR;
                    effectText = "EXCELLENT";
                    break;
                case GOOD:
                    effectColor = GOOD_COLOR;
                    effectText = "GOOD";
                    break;
                case POOR:
                    effectColor = POOR_COLOR;
                    effectText = "POOR";
                    break;
                default:
                    effectColor = MISS_COLOR;
                    effectText = "MISS";
                    break;
            }
            
            // Add combo if applicable
            if (combo > 1 && centerEffectType != VisualEffect.EffectType.MISS) {
                effectText += " x" + combo;
            }
            
            // Draw the center effect
            int effectY = 80; // Distance from top
            
            // Calculate text dimensions
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(effectText);
            
            // Background dimensions
            int bgWidth = textWidth + 40;
            int bgHeight = 50;
            
            // Draw background
            g2d.setColor(effectColor);
            g2d.fillRoundRect(width/2 - bgWidth/2, effectY - 25, bgWidth, bgHeight, 15, 15);
            
            // Draw text
            g2d.setColor(Color.WHITE);
            g2d.drawString(effectText, width/2 - textWidth/2, effectY + 8);
        }

        
        // Draw visual effects
        for (VisualEffect effect : effects) {
            int laneIndex = effect.getLaneIndex();
            int laneX = laneIndex * laneWidth;
            
            Color effectColor;
            switch (effect.getType()) {
                case EXCELLENT:
                    effectColor = EXCELLENT_COLOR;
                    break;
                case GOOD:
                    effectColor = GOOD_COLOR;
                    break;
                case POOR:
                    effectColor = POOR_COLOR;
                    break;
                case MISS:
                default:
                    effectColor = MISS_COLOR;
                    break;
            }
            
            // Draw flash at target line
            g2d.setColor(new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(), 100));
            g2d.fillRect(laneX, targetY - 5, laneWidth, 15);

        }
        
        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Score: " + score, 20, 30);

        // Draw max combo
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Max Combo: " + gameState.getMaxCombo(), 20, 60);
        
        // Draw rating counts
        if (gameState != null) {
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            
            // Draw excellent count
            g2d.setColor(EXCELLENT_COLOR);
            g2d.drawString("Excellent: " + gameState.getExcellentCount(), 20, 90);
            
            // Draw good count
            g2d.setColor(GOOD_COLOR);
            g2d.drawString("Good: " + gameState.getGoodCount(), 20, 110);
            
            // Draw poor count
            g2d.setColor(POOR_COLOR);
            g2d.drawString("Poor: " + gameState.getPoorCount(), 20, 130);
            
            // Draw miss count
            g2d.setColor(MISS_COLOR);
            g2d.drawString("Miss: " + gameState.getMissCount(), 20, 150);
            
            // Draw game mode and difficulty info
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString("Mode: " + gameState.getGameMode().getDisplayName(), 20, 180);
            if (gameState.getGameMode() != GameState.GameMode.PRACTICE) {
                g2d.drawString("Difficulty: " + gameState.getDifficultyLevel().getDisplayName(), 20, 200);
            }
        }

        // Draw game state (e.g. health bar)
        if (gameState != null) {
            int misses = gameState.getMisses();
            int maxMisses = gameState.getMaxMisses();
            int remainingLives = maxMisses - misses;
            
            // Health bar dimensions
            int healthBarWidth = 150;
            int healthBarHeight = 20;
            int healthBarX = width - healthBarWidth - 20; // right side padding
            int healthBarY = 20; // top padding
            
            // Only show health bar in normal mode
            if (gameState.getGameMode() != GameState.GameMode.PRACTICE) {
                // draw health bar background
                g2d.setColor(new Color(50, 50, 50, 200));
                g2d.fillRoundRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight, 10, 10);
                
                // calculate health percentage
                float healthPercentage = (float) remainingLives / maxMisses;
                int currentHealthWidth = (int) (healthBarWidth * healthPercentage);
                
                // change color based on health percentage
                if (healthPercentage > 0.6) {
                    g2d.setColor(new Color(50, 205, 50)); // green -- safe
                } else if (healthPercentage > 0.3) {
                    g2d.setColor(new Color(255, 165, 0)); // orange -- caution
                } else {
                    g2d.setColor(new Color(220, 20, 60)); // red -- danger
                }
                
                // draw health bar
                g2d.fillRoundRect(healthBarX, healthBarY, currentHealthWidth, healthBarHeight, 10, 10);
                
                // draw health bar border
                g2d.setColor(Color.WHITE);
                g2d.drawRoundRect(healthBarX, healthBarY, healthBarWidth, healthBarHeight, 10, 10);
            } else {
                // Show practice mode indicator instead of health bar
                g2d.setColor(new Color(80, 180, 255));
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                g2d.drawString("Practice Mode", width - 140, healthBarY + 15);
            }

            // Draw pause button
            int pauseButtonSize = 30;
            int pauseButtonX, pauseButtonY;
            
            if (gameState.getGameMode() != GameState.GameMode.PRACTICE) {
                // Position left of health bar in normal mode
                pauseButtonX = healthBarX - pauseButtonSize - 10;
                pauseButtonY = healthBarY + (healthBarHeight - pauseButtonSize) / 2;
            } else {
                // Position left of practice mode text
                pauseButtonX = width - 180 - pauseButtonSize;
                pauseButtonY = healthBarY + (healthBarHeight - pauseButtonSize) / 2;
            }

            // Store button rectangle for click detection
            pauseButtonRect = new Rectangle(pauseButtonX, pauseButtonY, pauseButtonSize, pauseButtonSize);

            // Draw button background (darker when hovered)
            g2d.setColor(isPauseButtonHovered ? new Color(70, 70, 80, 220) : new Color(50, 50, 60, 200));
            g2d.fillRoundRect(pauseButtonX, pauseButtonY, pauseButtonSize, pauseButtonSize, 8, 8);

            // Draw button border
            g2d.setColor(Color.WHITE);
            g2d.drawRoundRect(pauseButtonX, pauseButtonY, pauseButtonSize, pauseButtonSize, 8, 8);

            // Draw pause icon (two vertical lines)
            g2d.setStroke(new BasicStroke(2.0f));
            int iconWidth = 4;
            int iconHeight = 14;
            int iconPadding = 6;
            int iconY = pauseButtonY + (pauseButtonSize - iconHeight) / 2;

            g2d.fillRect(
                pauseButtonX + (pauseButtonSize / 2) - iconPadding - iconWidth, 
                iconY, 
                iconWidth, 
                iconHeight
            );
            g2d.fillRect(
                pauseButtonX + (pauseButtonSize / 2) + iconPadding, 
                iconY, 
                iconWidth, 
                iconHeight
            );
        }
        
        // Draw lane key hints at the bottom
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String[] keyHints = {"D", "F", "J", "K"};
        for (int i = 0; i < NUM_LANES; i++) {
            int laneX = i * laneWidth;
            g2d.setColor(Color.WHITE);
            int keyX = laneX + laneWidth/2 - 5;
            int keyY = height - 20;
            
            // Draw key hint background
            g2d.setColor(new Color(0, 0, 0, 150));
            int keyBgSize = 30;
            g2d.fillRoundRect(keyX - keyBgSize/4, keyY - 15, keyBgSize, keyBgSize, 5, 5);
            
            // Draw key hint text
            g2d.setColor(Color.WHITE);
            g2d.drawString(keyHints[i], keyX, keyY);
        }
    }

    /**
     * Sets the game state
     * @param gs The game state
     */
    public void setGameState(GameState gs) {
        this.gameState = gs;
    }
}