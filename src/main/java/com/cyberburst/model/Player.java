package com.cyberburst.model;

public class Player {
    private String id;
    private String name;
    private int score;
    private int timeRemaining; // in seconds
    private boolean isAlive;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.score = 0;
        this.timeRemaining = 180; // Default time bank
        this.isAlive = true;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(int timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public void decreaseTime() {
        if (this.timeRemaining > 0) {
            this.timeRemaining--;
        }
    }
}
