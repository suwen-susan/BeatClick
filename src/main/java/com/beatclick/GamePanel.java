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
    
    // Lane target positions (where notes should be hit)
    private static final float TARGET_POSITION = 0.85f; // 85% down the screen
    
    // Lists for notes and visual effects
    private final List<Note> notes = new CopyOnWriteArrayList<>();
    private final List<VisualEffect> effects = new CopyOnWriteArrayList<>();
    
    // Game state
    private int score = 0;
    private int combo = 0;
    
    /**
     * Inner class for visual effects (hit animations, etc.)
     */
    private static class VisualEffect {
        private final int laneIndex;
        private final EffectType type;
        private int framesRemaining;
        
        enum EffectType {
            HIT, MISS
        }
        
        public VisualEffect(int laneIndex, EffectType type, int duration) {
            this.laneIndex = laneIndex;
            this.type = type;
            this.framesRemaining = duration;
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
         * Updates all animations and effects
         */
    public void updateAnimations() {
        // Update notes
        long currentTime = System.currentTimeMillis();
        
        // Collect notes to remove instead of removing during iteration
        List<Note> notesToRemove = new ArrayList<>();
        for (Note note : notes) {
            note.updatePosition(currentTime);
            
            // Mark notes that have moved past the bottom of the screen for removal
            if (note.getYPosition() > 1.1f) {
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
     * Shows a hit effect in the specified lane
     * @param laneIndex The lane index
     */
    public void showHitEffect(int laneIndex) {
        effects.add(new VisualEffect(laneIndex, VisualEffect.EffectType.HIT, 10));
    }
    
    /**
     * Shows a miss effect in the specified lane
     * @param laneIndex The lane index
     */
    public void showMissEffect(int laneIndex) {
        effects.add(new VisualEffect(laneIndex, VisualEffect.EffectType.MISS, 10));
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
        
        // Draw visual effects
        for (VisualEffect effect : effects) {
            int laneX = effect.getLaneIndex() * laneWidth;
            
            if (effect.getType() == VisualEffect.EffectType.HIT) {
                // Hit effect (green flash)
                g2d.setColor(new Color(0, 255, 0, 100));
                g2d.fillRect(laneX, targetY - 10, laneWidth, 30);
            } else {
                // Miss effect (red flash)
                g2d.setColor(new Color(255, 0, 0, 100));
                g2d.fillRect(laneX, targetY - 10, laneWidth, 30);
            }
        }
        
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
        
        // Draw score and combo
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.drawString("Score: " + score, 20, 30);
        
        if (combo > 1) {
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.drawString("Combo: " + combo + "x", 20, 60);
        }
        
        // Draw lane key hints at the bottom
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String[] keyHints = {"D", "F", "J", "K"};
        for (int i = 0; i < NUM_LANES; i++) {
            int laneX = i * laneWidth;
            g2d.drawString(keyHints[i], laneX + laneWidth/2 - 5, height - 20);
        }
    }
}