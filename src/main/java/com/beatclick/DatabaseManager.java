package com.beatclick;

import java.sql.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DatabaseManager - Manages the SQLite database for storing high scores
 */
public class DatabaseManager {
    
    private static final String DB_NAME = "beatclick.db";
    private static final String TABLE_NAME = "high_scores";
    private static final String DETAIL_RECORD = "score_records";

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

            // Table 2: new detailed records table
            String detailedSql = "CREATE TABLE IF NOT EXISTS " + DETAIL_RECORD +"(" +
                    "username TEXT NOT NULL, " +
                    "end_time TEXT NOT NULL, " +
                    "song_id TEXT NOT NULL, " +
                    "score INTEGER NOT NULL, " +
                    "miss_count INTEGER DEFAULT 0, " +
                    "poor_count INTEGER DEFAULT 0, " +
                    "good_count INTEGER DEFAULT 0, " +
                    "excellent_count INTEGER DEFAULT 0, " +
                    "PRIMARY KEY (username, end_time))";
            stmt.execute(detailedSql);

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
        initDatabase();

        if (!isHighScore(songId, score)) {
            return;
        }

        try (Connection conn = getConnection()) {
            // Check if a record already exists
            String checkSql = "SELECT score FROM " + TABLE_NAME + " WHERE song_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, songId);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Update existing record
                    String updateSql = "UPDATE " + TABLE_NAME + " SET score = ? WHERE song_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, score);
                        updateStmt.setString(2, songId);
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Insert new record
                    String insertSql = "INSERT INTO " + TABLE_NAME + " (song_id, score) VALUES (?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setString(1, songId);
                        insertStmt.setInt(2, score);
                        insertStmt.executeUpdate();
                    }
                }
            }
            highScoreCache.put(songId, score);

        } catch (SQLException e) {
            System.err.println("Error saving score: " + e.getMessage());
        }
    }


    /**
     *
     */

    public static void saveDetailedScore(String username,
                                         String songId,
                                         String endTime,
                                         int score,
                                         int miss,
                                         int poor,
                                         int good,
                                         int excellent) {
        initDatabase();

        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO " + DETAIL_RECORD + " " +
                    "(username, end_time, song_id, score, miss_count, poor_count, good_count, excellent_count) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, endTime);
            pstmt.setString(3, songId);
            pstmt.setInt(4, score);
            pstmt.setInt(5, miss);
            pstmt.setInt(6, poor);
            pstmt.setInt(7, good);
            pstmt.setInt(8, excellent);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving detailed score: " + e.getMessage());
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

    public static List<ScoreRecord> getTopScoresBySong(String songId) {
        initDatabase();

        String sql = "SELECT username, score, end_time " +
                "FROM " + DETAIL_RECORD + " sr " +
                "WHERE (username, score) IN ( " +
                "    SELECT username, MAX(score) " +
                "    FROM " + DETAIL_RECORD + " " +
                "    WHERE song_id = ? " +
                "    GROUP BY username " +
                ") AND song_id = ? " +
                "ORDER BY score DESC";

        List<ScoreRecord> records = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, songId);
            pstmt.setString(2, songId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                int score = rs.getInt("score");
                String endTime = rs.getString("end_time");

                records.add(new ScoreRecord(songId, username, score, endTime));
            }

        } catch (SQLException e) {
            System.err.println("Error querying top scores: " + e.getMessage());
        }

        return records;
    }

    public static int getHighScoreFromDetailedTable(String songId) {
        initDatabase();
        String sql = "SELECT MAX(score) AS max_score FROM " + DETAIL_RECORD + " WHERE song_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, songId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("max_score");
            }

        } catch (SQLException e) {
            System.err.println("Error retrieving high score from detailed table: " + e.getMessage());
        }

        return 0;
    }


}