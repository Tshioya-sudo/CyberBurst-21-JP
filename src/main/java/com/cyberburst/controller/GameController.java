package com.cyberburst.controller;

import com.cyberburst.model.GameState;
import com.cyberburst.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@Controller
public class GameController {

    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/join")
    @SendTo("/topic/game")
    public GameState joinGame(Map<String, String> payload) {
        String playerId = payload.get("playerId");
        String playerName = payload.get("playerName");
        return gameService.joinGame(playerId, playerName);
    }

    @MessageMapping("/move")
    @SendTo("/topic/game")
    public GameState makeMove(Map<String, String> payload) {
        String playerId = payload.get("playerId");
        String word = payload.get("word");
        return gameService.processMove(playerId, word);
    }
}
