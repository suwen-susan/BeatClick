package com.beatclick;

public class ScoreRecord {
    public final String songId;
    public final String username;
    public final int score;
    public final String endTime;

    public ScoreRecord(String songId, String username, int score, String endTime) {
        this.songId = songId;
        this.username = username;
        this.score = score;
        this.endTime = endTime;
    }
}
