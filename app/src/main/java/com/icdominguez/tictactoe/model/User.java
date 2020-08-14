package com.icdominguez.tictactoe.model;

public class User {
    private String name;
    private int points;
    private int gamesPlayed;

    public User() {
    }

    public User(String name, int points, int gamesPlayed) {
        this.name = name;
        this.points = points;
        this.gamesPlayed = gamesPlayed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }
}
