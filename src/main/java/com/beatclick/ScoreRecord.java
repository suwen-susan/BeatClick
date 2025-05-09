package com.beatclick;

public class ScoreRecord {
    public final String songId;
    public final String username;
    public final int score;
    public final String endTime;
    public final int excellentCount, goodCount, poorCount, missCount;


    public ScoreRecord(String songId, String username, int score, String endTime) {
        this(songId, username, score, endTime, 0, 0, 0, 0);
    }

    public ScoreRecord(String songId, String username, int score, String endTime,
                       int excellent, int good, int poor, int miss) {
        this.songId = songId;
        this.username = username;
        this.score = score;
        this.endTime = endTime;
        this.excellentCount = excellent;
        this.goodCount = good;
        this.poorCount = poor;
        this.missCount = miss;
    }

}
