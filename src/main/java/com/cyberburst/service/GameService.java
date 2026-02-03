package com.cyberburst.service;

import com.cyberburst.model.GameState;
import com.cyberburst.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final List<String> playerOrder = new ArrayList<>();

    private String currentTurnPlayerId;
    private String previousWord = "START"; // Initial word
    private String targetChar = "t"; // Last char of START (case insensitive safe)
    private boolean isGameOver = false;
    private String message = "Waiting for players...";

    @Autowired
    public GameService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public synchronized GameState joinGame(String playerId, String playerName) {
        if (players.containsKey(playerId)) {
            return getGameState();
        }

        // Max 2 players for now as per simple rules implying A vs B, but logic supports
        // more
        if (players.size() >= 2) {
            // For now reject or just return state (watcher mode)
            return getGameState();
        }

        Player newPlayer = new Player(playerId, playerName);
        players.put(playerId, newPlayer);
        playerOrder.add(playerId);

        if (players.size() == 1) {
            currentTurnPlayerId = playerId;
            message = "Waiting for opponent...";
        } else if (players.size() == 2) {
            message = "Game Start! " + players.get(currentTurnPlayerId).getName() + "'s turn.";
        }

        broadcastState();
        return getGameState();
    }

    public synchronized GameState processMove(String playerId, String word) {
        if (isGameOver || !playerId.equals(currentTurnPlayerId)) {
            return getGameState();
        }

        Player player = players.get(playerId);
        word = word.trim().toLowerCase();

        // 1. Validation
        if (word.isEmpty())
            return getGameState(); // Ignore empty

        // Check starting char
        if (!word.startsWith(targetChar)) {
            // Invalid word, maybe penalize? For now just ignore or send error message in
            // state?
            // Let's strict rule: must match.
            // We can add a "message" field for temporary errors, but let's keep it simple.
            return getGameState();
        }

        // 2. "N" Rule
        if (word.endsWith("n") || word.endsWith("ん")) {
            isGameOver = true;
            player.setAlive(false);
            message = "GAME OVER! " + player.getName() + " ended with 'n'!";
            broadcastState();
            return getGameState();
        }

        // 3. Scoring
        int points = word.length();
        player.addScore(points);
        previousWord = word;
        targetChar = word.substring(word.length() - 1);

        // 4. Target 21 & Burst
        if (player.getScore() == 21) {
            // Maybe instant win? Or wait for others?
            // User says "Aim for total score closest to 21".
            // Usually usually blackjack is instant win if 21?
            // Let's say if 21, you are safe, but game continues until someone bursts or
            // time out?
            // Actually "Burst: If score >= 22, the player immediately LOSES".
            // So 21 is just best score.
        } else if (player.getScore() >= 22) {
            isGameOver = true;
            player.setAlive(false);
            message = "BURST! " + player.getName() + " exceeded 21!";
            broadcastState();
            return getGameState();
        }

        // 5. Turn Switch
        switchTurn();
        message = players.get(currentTurnPlayerId).getName() + "'s turn.";

        broadcastState();
        return getGameState();
    }

    private void switchTurn() {
        int currentIndex = playerOrder.indexOf(currentTurnPlayerId);
        int nextIndex = (currentIndex + 1) % playerOrder.size();
        currentTurnPlayerId = playerOrder.get(nextIndex);
    }

    @Scheduled(fixedRate = 1000)
    public void gameLoop() {
        if (isGameOver || players.size() < 2)
            return;

        Player currentPlayer = players.get(currentTurnPlayerId);
        if (currentPlayer != null) {
            currentPlayer.decreaseTime();
            if (currentPlayer.getTimeRemaining() <= 0) {
                isGameOver = true;
                currentPlayer.setAlive(false);
                message = "TIME UP! " + currentPlayer.getName() + " lost!";
                broadcastState();
            } else {
                // Only broadcast if time changes (every second)
                // Or optimize to not spam?
                // Frontend needs timer sync. Broadcasting every second is fine for 2 players.
                broadcastState();
            }
        }
    }

    private GameState getGameState() {
        return new GameState(
                new ArrayList<>(players.values()),
                currentTurnPlayerId,
                previousWord,
                targetChar,
                message,
                isGameOver);
    }

    private void broadcastState() {
        messagingTemplate.convertAndSend("/topic/game", getGameState());
    }
}
