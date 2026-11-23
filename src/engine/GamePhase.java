package engine;

// What is happening inside a run (while the game state is PLAYING)
public enum GamePhase {
    INTRO,           // "LEVEL 3 - new trash" card, waiting for START
    RUNNING,         // trash is floating, the player is sorting
    PAUSED,          // ESC / PAUSE button: everything frozen, RESUME / RESTART / MENU card
    RECAP,           // animation after a level or a game over: what you sorted, what went wrong, a lesson
    LEVEL_COMPLETE,  // levels mode: between levels
    GAME_OVER        // out of lives, or the special level was cleared (see isVictory)
}