package com.beatclick;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * GamePanel - Handles rendering of the game
 * Displays notes, animations, and game state
 */
public class GamePanel extends JPanel {
    
    // Constants for rendering
    private static final int NUM_LANES = 4;
    private static final Color[] LANE_COLORS = {
        new Color(231, 76, 60),  // Red
        new Color(241, 196, 15), // Yellow
        new Color(46, 204, 113), // Green
        new Color(52, 152, 219)  // Blue
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
    }
    
    /**
     * Configures the input handler for this panel
     * @param inputHandler The input handler
     */
    public void configureInputHandler(InputHandler inputHandler) {
        // Remove existing listeners first
        for (KeyListener listener : getKeyListeners()) {
            removeKeyListener(listener);
        }
        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
        
        // Add new listeners
        addKeyListener(inputHandler);
        addMouseListener(inputHandler);
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
     * @param noteYPosition The Y position of the note when hit
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
        
        // Draw lanes
        for (int i = 0; i < NUM_LANES; i++) {
            int laneX = i * laneWidth;
            
            // Draw lane background
            g2d.setColor(LANE_COLORS[i].darker().darker());
            g2d.fillRect(laneX, 0, laneWidth, height);
            
            // Draw lane separator
            g2d.setColor(Color.BLACK);
            g2d.drawLine(laneX, 0, laneX, height);
        }
        
        // Draw target line
        int targetY = (int)(TARGET_POSITION * height);
        g2d.setColor(TARGET_LINE_COLOR);
        g2d.fillRect(0, targetY, width, TARGET_LINE_HEIGHT);
        
        // Draw notes
        for (Note note : notes) {
            int laneIndex = note.getLaneIndex();
            int laneX = laneIndex * laneWidth;
            int noteY = (int)(note.getYPosition() * height) - NOTE_SIZE / 2;
            
            // Draw note with lane color
            g2d.setColor(LANE_COLORS[laneIndex]);
            g2d.fillRoundRect(laneX + laneWidth/2 - NOTE_SIZE/2, noteY, NOTE_SIZE, NOTE_SIZE, 10, 10);
            
            // Draw note border
            g2d.setColor(Color.WHITE);
            g2d.drawRoundRect(laneX + laneWidth/2 - NOTE_SIZE/2, noteY, NOTE_SIZE, NOTE_SIZE, 10, 10);
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
        }
        
        // Draw lane key hints at the bottom
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String[] keyHints = {"D", "F", "J", "K"};
        for (int i = 0; i < NUM_LANES; i++) {
            // int laneX = i * laneWidth;
            // g2d.drawString(keyHints[i], laneX + laneWidth/2 - 5, height - 20);
            
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