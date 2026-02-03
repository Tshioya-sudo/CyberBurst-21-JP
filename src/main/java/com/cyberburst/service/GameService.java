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
    private String previousWord = "スタート"; // Initial word
    private String targetChar = "ト"; // Last char of START
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
        word = word.trim(); // Do not lowercase for Japanese

        // 1. Validation
        if (word.isEmpty())
            return getGameState();

        // Convert to Hiragana for consistency check (Simplified logic)
        // ideally we use a library but for core Java we can just rely on user input
        // matching visually
        // or check simple mapping if needed. For now, strict end-char matching.

        String lastCharOfPrevious = targetChar;
        String firstCharOfNext = word.substring(0, 1);

        // Simple validation: ignore case/type match for now, assume player types
        // matching char
        // But for Japanese "Ringos" -> "Su", we need to check "Su" == word.start

        if (!isMatch(lastCharOfPrevious, firstCharOfNext)) {
            // Invalid start char
            return getGameState();
        }

        // 2. "N" Rule (ン/ん)
        if (word.endsWith("n") || word.endsWith("ん") || word.endsWith("ン")) {
            isGameOver = true;
            player.setAlive(false);
            message = "GAME OVER! " + player.getName() + " ended with 'N' (ん)!";
            broadcastState();
            return getGameState();
        }

        // 3. Scoring (Length of word)
        int points = word.length();
        player.addScore(points);
        previousWord = word;

        // Extract new target char (handle small characters and extenders)
        targetChar = extractTargetChar(word);

        // 4. Target 21 & Burst
        if (player.getScore() == 21) {
            // 21 is safe/good.
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

    private boolean isMatch(String target, String input) {
        // Simple equality or Katakana<->Hiragana loose match could go here
        // For MVP, strict match or simple conversion
        // Let's implement simple normalization if possible, otherwise strict
        return target.equals(input) ||
                toHiragana(target).equals(toHiragana(input));
    }

    private String extractTargetChar(String word) {
        if (word.isEmpty())
            return "";
        char last = word.charAt(word.length() - 1);

        // Handle prolonged sound mark "ー" (take vowel of previous char)
        if (last == 'ー' && word.length() > 1) {
            char prev = word.charAt(word.length() - 2);
            // Simple mapping for vowels (a,i,u,e,o)
            // This is complex without library.
            // Simplified Rule: Ignore "ー" and take previous character?
            // Often used rule: "Computer" -> "Ta". "Sakka-" -> "Ka".
            last = word.charAt(word.length() - 2);
        }

        // Handle small chars (ゃ, ゅ, ょ, っ) -> Convert to big (や, ゆ, よ, つ)
        return normalizeSmallChar(String.valueOf(last));
    }

    private String normalizeSmallChar(String s) {
        return s.replace('ゃ', 'や').replace('ゅ', 'ゆ').replace('ょ', 'よ').replace('っ', 'つ')
                .replace('ャ', 'ヤ').replace('ュ', 'ユ').replace('ョ', 'ヨ').replace('ッ', 'ツ')
                .replace('ァ', 'ア').replace('ィ', 'イ').replace('ゥ', 'ウ').replace('ェ', 'エ').replace('ォ', 'オ')
                .replace('ぁ', 'あ').replace('ぃ', 'い').replace('ぅ', 'う').replace('ぇ', 'え').replace('ぉ', 'お');
    }

    // Very basic Hiragana converter for matching logic (Katakana -> Hiragana)
    private String toHiragana(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 'ァ' && c <= 'ン') {
                sb.append((char) (c - 0x60));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
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
