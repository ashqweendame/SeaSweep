package engine;

// Shared numbers used by both the game rules and the drawing code.
public class GameConfig {
    public static final int WINDOW_WIDTH = 900;
    public static final int WINDOW_HEIGHT = 700;

    // trash floats inside this horizontal band of the sea
    public static final int BAND_TOP = 240;
    public static final int BAND_BOTTOM = 500;

    private GameConfig() {}
}