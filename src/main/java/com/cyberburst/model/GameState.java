package com.cyberburst.model;

import java.util.List;

public class GameState {
    private List<Player> players;
    private String currentTurnPlayerId;
    private String previousWord;
    private String targetChar;
    private String message;
    private boolean isGameOver;

    public GameState(List<Player> players, String currentTurnPlayerId, String previousWord, String targetChar,
            String message, boolean isGameOver) {
        this.players = players;
        this.currentTurnPlayerId = currentTurnPlayerId;
        this.previousWord = previousWord;
        this.targetChar = targetChar;
        this.message = message;
        this.isGameOver = isGameOver;
    }

    // Getters
    public List<Player> getPlayers() {
        return players;
    }

    public String getCurrentTurnPlayerId() {
        return currentTurnPlayerId;
    }

    public String getPreviousWord() {
        return previousWord;
    }

    public String getTargetChar() {
        return targetChar;
    }

    public String getMessage() {
        return message;
    }

    public boolean isGameOver() {
        return isGameOver;
    }
}
