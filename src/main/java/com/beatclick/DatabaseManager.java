package com.beatclick;

import java.sql.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * DatabaseManager - Manages the SQLite database for storing high scores
 */
public class DatabaseManager {
    
    private static final String DB_NAME = "beatclick.db";
    private static final String TABLE_NAME = "high_scores";
    
    // Cache for high scores to reduce database access
    private static final Map<String, Integer> highScoreCache = new HashMap<>();
    
    /**
     * Initializes the database
     */
    private static void initDatabase() {
        // Create the database directory if it doesn't exist
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        
        try (Connection conn = getConnection()) {
            // Create the high scores table if it doesn't exist
            Statement stmt = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " " +
                         "(song_id TEXT PRIMARY KEY, " +
                         " score INTEGER NOT NULL)";
            stmt.execute(sql);
            
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
    
    /**
     * Gets a connection to the database
     * @return The database connection
     * @throws SQLException If a database error occurs
     */
    private static Connection getConnection() throws SQLException {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Create the connection
            String url = "jdbc:sqlite:data/" + DB_NAME;
            return DriverManager.getConnection(url);
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }
    
    /**
     * Saves a score to the database
     * @param songId The ID of the song
     * @param score The score to save
     */
    public static void saveScore(String songId, int score) {
        // Initialize the database if needed
        initDatabase();
        
        // Only save if it's a high score
        if (!isHighScore(songId, score)) {
            return;
        }
        
        try (Connection conn = getConnection()) {
            // Check if a record already exists for this song
            String sql = "SELECT score FROM " + TABLE_NAME + " WHERE song_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, songId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                // Update existing record
                sql = "UPDATE " + TABLE_NAME + " SET score = ? WHERE song_id = ?";
                pstmt = conn.prepareStatement(sql);
                pstmt.setInt(1, score);
                pstmt.setString(2, songId);
                pstmt.executeUpdate();
            } else {
                // Insert new record
                sql = "INSERT INTO " + TABLE_NAME + " (song_id, score) VALUES (?, ?)";
                pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, songId);
                pstmt.setInt(2, score);
                pstmt.executeUpdate();
            }
            
            // Update the cache
            highScoreCache.put(songId, score);
            
        } catch (SQLException e) {
            System.err.println("Error saving score: " + e.getMessage());
        }
    }
    
    /**
     * Gets the high score for a song
     * @param songId The ID of the song
     * @return The high score, or 0 if no score exists
     */
    public static int getHighScore(String songId) {
        // Check the cache first
        if (highScoreCache.containsKey(songId)) {
            return highScoreCache.get(songId);
        }
        
        // Initialize the database if needed
        initDatabase();
        
        try (Connection conn = getConnection()) {
            String sql = "SELECT score FROM " + TABLE_NAME + " WHERE song_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, songId);
            ResultSet rs = pstmt.executeQuery();
            
            int highScore = 0;
            if (rs.next()) {
                highScore = rs.getInt("score");
                // Cache the result
                highScoreCache.put(songId, highScore);
            }
            
            return highScore;
            
        } catch (SQLException e) {
            System.err.println("Error getting high score: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Checks if a score is a high score
     * @param songId The ID of the song
     * @param score The score to check
     * @return true if the score is a high score, false otherwise
     */
    public static boolean isHighScore(String songId, int score) {
        return score > getHighScore(songId);
    }
    
    /**
     * Gets all high scores from the database
     * @return A map of song IDs to high scores
     */
    public static Map<String, Integer> getAllHighScores() {
        // Initialize the database if needed
        initDatabase();
        
        Map<String, Integer> scores = new HashMap<>();
        
        try (Connection conn = getConnection()) {
            String sql = "SELECT song_id, score FROM " + TABLE_NAME;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                String songId = rs.getString("song_id");
                int score = rs.getInt("score");
                scores.put(songId, score);
                
                // Update the cache
                highScoreCache.put(songId, score);
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting all high scores: " + e.getMessage());
        }
        
        return scores;
    }
}