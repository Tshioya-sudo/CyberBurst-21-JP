// CyberBurst-21-JP Logic

const socketEndpoint = '/game-websocket';
// SockJS automatically handles the protocol (ws:// or wss://) and host when using a relative path.
let stompClient = null;
let playerId = 'player-' + Math.floor(Math.random() * 100000);
let playerName = '';

// UI Elements
const statusDiv = document.getElementById('game-status');
const targetCharDiv = document.getElementById('target-char');
const prevWordDiv = document.getElementById('previous-word');
const wordInput = document.getElementById('word-input');
const sendBtn = document.getElementById('send-btn');
const loginModal = document.getElementById('login-modal');
const usernameInput = document.getElementById('username-input');
const joinBtn = document.getElementById('join-btn');

// Audio Context
const AudioContext = window.AudioContext || window.webkitAudioContext;
const audioCtx = new AudioContext();

const sounds = {
    click: () => {
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.frequency.setValueAtTime(800, audioCtx.currentTime);
        osc.type = 'square';
        gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.1);
        osc.start();
        osc.stop(audioCtx.currentTime + 0.1);
    },
    chime: () => {
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.frequency.setValueAtTime(1200, audioCtx.currentTime);
        osc.type = 'sine';
        gain.gain.setValueAtTime(0.2, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.5);
        osc.start();
        osc.stop(audioCtx.currentTime + 0.5);
    },
    whoosh: () => {
        // Simple noise simulated by oscillator sweep
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.frequency.setValueAtTime(200, audioCtx.currentTime);
        osc.frequency.linearRampToValueAtTime(800, audioCtx.currentTime + 0.3);
        osc.type = 'triangle';
        gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gain.gain.linearRampToValueAtTime(0, audioCtx.currentTime + 0.3);
        osc.start();
        osc.stop(audioCtx.currentTime + 0.3);
    },
    burst: () => {
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.frequency.setValueAtTime(100, audioCtx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(10, audioCtx.currentTime + 1);
        osc.type = 'sawtooth';
        gain.gain.setValueAtTime(0.5, audioCtx.currentTime);
        gain.gain.linearRampToValueAtTime(0, audioCtx.currentTime + 1);
        osc.start();
        osc.stop(audioCtx.currentTime + 1);
    }
};

// Event Listeners
joinBtn.addEventListener('click', () => {
    playerName = usernameInput.value.trim() || playerId;
    connect();
    loginModal.classList.add('hidden');
    sounds.click();
});

wordInput.addEventListener('keypress', (e) => {
    sounds.click();
    if (e.key === 'Enter') sendMove();
});

sendBtn.addEventListener('click', sendMove);

function connect() {
    const socket = new SockJS(socketEndpoint);
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Disable debug logs provided by Stomp.js

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        statusDiv.innerText = "CONNECTED";

        stompClient.subscribe('/topic/game', function (message) {
            renderGame(JSON.parse(message.body));
        });

        // Join Game
        stompClient.send("/app/join", {}, JSON.stringify({
            playerId: playerId,
            playerName: playerName
        }));
    });
}

function sendMove() {
    const word = wordInput.value.trim();
    if (word) {
        stompClient.send("/app/move", {}, JSON.stringify({
            playerId: playerId,
            word: word
        }));
        wordInput.value = '';
    }
}

function renderGame(gameState) {
    // 1. Update Game Status
    statusDiv.innerText = gameState.message;
    prevWordDiv.innerText = gameState.previousWord;
    targetCharDiv.innerText = gameState.targetChar.toUpperCase();

    // 2. Update Players
    const myPlayer = gameState.players.find(p => p.id === playerId);
    const opponent = gameState.players.find(p => p.id !== playerId);

    if (myPlayer) {
        updatePlayerUI('my', myPlayer);
    }
    if (opponent) {
        updatePlayerUI('opp', opponent);
        document.getElementById('opponent-area').querySelector('.player-name').innerText = opponent.name;
    } else {
        // Reset opponent UI if waiting
        document.getElementById('opponent-area').querySelector('.player-name').innerText = "WAITING...";
        document.getElementById('opp-score').innerText = "0";
        document.getElementById('opp-time').innerText = "180";
    }

    // 3. Turn Management
    if (gameState.currentTurnPlayerId === playerId && !gameState.gameOver) {
        wordInput.disabled = false;
        sendBtn.disabled = false;
        document.getElementById('player-area').classList.add('active-turn');
        document.getElementById('opponent-area').classList.remove('active-turn');

        // Focus only if just became turn?
        // simple check: if we were disabled before
        if (wordInput.disabled === false) {
            wordInput.focus();
        }
    } else {
        wordInput.disabled = true;
        sendBtn.disabled = true;
        document.getElementById('player-area').classList.remove('active-turn');
        document.getElementById('opponent-area').classList.add('active-turn');
    }

    // 4. Audio Triggers
    if (gameState.gameOver) {
        sounds.burst(); // Game Over sound
        statusDiv.classList.add('score-burst');
    } else {
        // Detect turn change or success (simple heuristic: if my turn changed)
        // Ideally we compare previous state, but stateless render is robust.
    }
}

function updatePlayerUI(prefix, player) {
    const scoreEl = document.getElementById(`${prefix}-score`);
    const timeEl = document.getElementById(`${prefix}-time`);

    scoreEl.innerText = player.score;
    timeEl.innerText = player.timeRemaining;

    // Color Logic
    scoreEl.className = 'neon-text'; // Reset
    if (player.score === 21) scoreEl.classList.add('score-21');
    else if (player.score >= 22) scoreEl.classList.add('score-burst');
    else if (player.score >= 16) scoreEl.classList.add('score-warning');
    else scoreEl.classList.add('score-safe');
}

// Resume Audio Context on interaction
document.body.addEventListener('click', () => {
    if (audioCtx.state === 'suspended') {
        audioCtx.resume();
    }
});
