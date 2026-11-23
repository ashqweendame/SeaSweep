import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.util.Duration;

import engine.GameManager;
import view.GamePanel;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        GameManager manager = new GameManager();
        GamePanel panel = new GamePanel(manager);

        Scene scene = new Scene(new Group(panel));

        // keyboard: what a key does depends on the current screen
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();

            switch (manager.getState()) {
                case MENU:
                    if (code == KeyCode.ENTER || code == KeyCode.L) {
                        manager.openLevelSelect();
                    } else if (code == KeyCode.U) {
                        manager.startUnlimited();
                    } else if (code == KeyCode.T) {
                        manager.startTutorial();
                    }
                    break;

                case LEVEL_SELECT:
                    if (code == KeyCode.ESCAPE) {
                        manager.returnToMenu();
                    }
                    break;

                case TUTORIAL:
                    if (code == KeyCode.ESCAPE) {
                        manager.returnToMenu();
                    } else if (code == KeyCode.ENTER) {
                        manager.tutorialConfirm();
                    }
                    break;

                case PLAYING:
                    handlePlayingKey(manager, code);
                    break;
            }
        });

        // typed characters for the username (handles shift/caps correctly)
        scene.setOnKeyTyped(e -> {
            String typed = e.getCharacter();
            if (!typed.isEmpty() && !Character.isISOControl(typed.charAt(0))) {
                manager.typeNameCharacter(typed.charAt(0));
            }
        });

        // mouse: drag and drop (and double-click) on the sea
        panel.setOnMousePressed(e -> manager.mousePressed(e.getX(), e.getY(), e.getClickCount()));
        panel.setOnMouseDragged(e -> manager.mouseDragged(e.getX(), e.getY()));
        panel.setOnMouseReleased(e -> manager.mouseReleased(e.getX(), e.getY()));

        // mouse: the buttons on each screen
        panel.setOnMouseClicked(e -> handleClick(manager, panel, e.getX(), e.getY()));

        stage.setTitle("Sea Sweep!");
        stage.setResizable(false);
        stage.setScene(scene);

        // switching to another window stops the game instead of letting the sea carry on
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                manager.pause();
            }
        });

        stage.show();

        panel.render(); // first frame

        // 16 ms fixed tick: update first, then render
        Timeline gameLoop = new Timeline(new KeyFrame(Duration.millis(16), e -> {
            manager.processFrameUpdate(0.016);
            panel.render();
        }));
        gameLoop.setCycleCount(Animation.INDEFINITE);
        gameLoop.play();
    }

    private static void handlePlayingKey(GameManager manager, KeyCode code) {
        if (manager.isEnteringName()) {
            if (code == KeyCode.ENTER) {
                manager.submitName();
            } else if (code == KeyCode.BACK_SPACE) {
                manager.deleteNameCharacter();
            }
            return;
        }

        if (code == KeyCode.ESCAPE) {
            switch (manager.getPhase()) {
                case RUNNING:
                    manager.pause();
                    break;
                case PAUSED:
                    manager.resume();
                    break;
                case GAME_OVER:
                    manager.returnToMenu();
                    break;
                case RECAP:
                    manager.advanceRecap();   // skip the animation, or continue once it has finished
                    break;
                default:
                    manager.exitRun();   // intro / level-complete card: nothing at stake, just leave
                    break;
            }
            return;
        }

        switch (manager.getPhase()) {
            case INTRO:
                if (code == KeyCode.ENTER) {
                    manager.confirmIntro();
                }
                break;
            case LEVEL_COMPLETE:
                if (code == KeyCode.ENTER) {
                    manager.nextLevel();
                }
                break;
            case RECAP:
                if (code == KeyCode.ENTER) {
                    manager.advanceRecap();
                }
                break;
            case PAUSED:
                if (code == KeyCode.R) {
                    manager.restart();
                } else if (code == KeyCode.M) {
                    manager.returnToMenu();
                }
                break;
            case GAME_OVER:
                if (code == KeyCode.R) {
                    manager.restart();
                } else if (code == KeyCode.M) {
                    manager.returnToMenu();
                }
                break;
            default:
                break;
        }
    }

    private static void handleClick(GameManager manager, GamePanel panel, double x, double y) {
        switch (manager.getState()) {
            case MENU:
                if (panel.isLevelsButtonHit(x, y)) {
                    manager.openLevelSelect();
                } else if (panel.isUnlimitedButtonHit(x, y)) {
                    manager.startUnlimited();
                } else if (panel.isTutorialButtonHit(x, y)) {
                    manager.startTutorial();
                }
                break;

            case LEVEL_SELECT:
                if (panel.isBackButtonHit(x, y)) {
                    manager.returnToMenu();
                } else {
                    int level = panel.levelAt(x, y);
                    if (level > 0) {
                        manager.startLevel(level);   // ignored by the manager if the level is locked
                    }
                }
                break;

            case TUTORIAL:
                if (panel.isBackButtonHit(x, y)) {
                    manager.returnToMenu();
                } else if (panel.isTutorialNextHit(x, y)) {
                    manager.tutorialConfirm();
                }
                break;

            case PLAYING:
                switch (manager.getPhase()) {
                    case RUNNING:
                        if (panel.isPauseHit(x, y)) {
                            manager.pause();
                        }
                        break;
                    case PAUSED:
                        if (panel.isPauseResumeHit(x, y)) {
                            manager.resume();
                        } else if (panel.isPauseRestartHit(x, y)) {
                            manager.restart();
                        } else if (panel.isPauseMenuHit(x, y)) {
                            manager.returnToMenu();
                        }
                        break;
                    case RECAP:
                        if (!manager.isRecapDone()) {
                            manager.skipRecap();
                        } else if (panel.isRecapContinueHit(x, y)) {
                            manager.continueRecap();
                        }
                        break;
                    case INTRO:
                        if (panel.isIntroStartHit(x, y)) {
                            manager.confirmIntro();
                        }
                        break;
                    case LEVEL_COMPLETE:
                        if (panel.isCompleteNextHit(x, y)) {
                            manager.nextLevel();
                        } else if (panel.isCompleteMenuHit(x, y)) {
                            manager.openLevelSelect();
                        }
                        break;
                    case GAME_OVER:
                        if (manager.canRestart()) {
                            if (panel.isRestartButtonHit(x, y)) {
                                manager.restart();
                            } else if (panel.isGameOverMenuButtonHit(x, y)) {
                                manager.returnToMenu();
                            }
                        }
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}