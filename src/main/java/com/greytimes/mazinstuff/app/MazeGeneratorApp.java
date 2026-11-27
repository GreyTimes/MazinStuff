
package com.greytimes.mazinstuff.app;

import com.greytimes.mazinstuff.controller.MazeFileManager;
import com.greytimes.mazinstuff.controller.MazeGameplayController;
import com.greytimes.mazinstuff.controller.MazeGenerationController;
import com.greytimes.mazinstuff.gameplay.Player;

import com.greytimes.mazinstuff.view.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import com.greytimes.mazinstuff.model.configuration.EntranceExitMode;
import com.greytimes.mazinstuff.model.grid.MazeGrid;
import com.greytimes.mazinstuff.model.configuration.MazeThemeConfig;
import com.greytimes.mazinstuff.model.topology.TopologyType;

import java.io.IOException;

/**
 * JavaFX application for rectangular, circular, hexagonal,
 * and triangular maze generation and solving.
 *
 * Supports configurable entrance and exit positions.
 */
public class MazeGeneratorApp extends javafx.application.Application {

    // CONSTANTS

    private static final double CANVAS_SIZE = 600.0;
    private static final double GEOMETRY_PADDING = 22.0;

    // APPLICATION STATE

    private MazeGrid grid;
    private MazeFileManager fileManager;
    private MazeCanvas mazeCanvas;
    private Label statsLabel;
    private ComboBox<String> algorithmCombo;
    private ComboBox<String> solverCombo;
    private ComboBox<TopologyType> shapeCombo;
    private ComboBox<EntranceExitMode> entranceExitModeCombo;
    private ToggleButton playModeToggle;
    private ToggleButton fogOfWarToggle;
    private ToggleButton showSolutionToggle;
    private ToggleButton themeToggle;
    private Label openingStatusLabel;
    private TextField seedTextField;
    private final Player player = new Player();
    private MazeThemeConfig themeConfig;
    private long currentSeed = System.currentTimeMillis();

    // CONTROLLERS

    private MazeGenerationController generationController;
    private MazeGameplayController gameplayController;

    // APPLICATION START

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Customizable JavaFX Maze Generator & Solver");

        // INITIALIZE APPLICATION COMPONENTS

        mazeCanvas = new MazeCanvas(CANVAS_SIZE, CANVAS_SIZE);
        themeConfig = MazeThemeConfig.load();
        mazeCanvas.setThemeConfig(themeConfig);

        // BUILD USER INTERFACE

        MazeUIBuilder uiBuilder = new MazeUIBuilder(mazeCanvas, currentSeed, themeConfig.isDarkMode());
        MazeUIBuilder.Controls controls = uiBuilder.getControls();

        Scene scene = uiBuilder.getScene();

        // CONNECT UI CONTROLS TO APPLICATION

        // MAZE EDITOR

        shapeCombo = controls.shapeCombo();
        entranceExitModeCombo = controls.entranceExitModeCombo();
        openingStatusLabel = controls.openingStatusLabel();
        algorithmCombo = controls.algorithmCombo();
        solverCombo = controls.solverCombo();
        showSolutionToggle = controls.showSolutionToggle();
        seedTextField = controls.seedTextField();

        // GAMEPLAY

        playModeToggle = controls.playModeToggle();
        fogOfWarToggle = controls.fogOfWarToggle();

        // FILE / APPEARANCE

        themeToggle = controls.themeToggle();
        statsLabel = controls.statsLabel();

        // SLIDERS

        Slider widthSlider = controls.widthSlider();
        Slider heightSlider = controls.heightSlider();
        Slider speedSlider = controls.speedSlider();
        Slider complexitySlider = controls.complexitySlider();
        Slider fogRadiusSlider = controls.fogRadiusSlider();

        // GENERATION BUTTON

        Button generateBtn = controls.generateButton();

        // FILE BUTTONS

        Button exportBtn = controls.exportButton();
        Button saveMazeBtn = controls.saveMazeButton();
        Button loadMazeBtn = controls.loadMazeButton();
        Button benchmarkBtn = controls.benchmarkButton();
        Button themeCustomizerBtn = controls.themeCustomizerButton();
        Button aboutBtn = controls.aboutButton();

        // SEED BUTTONS

        Button randomizeSeedBtn = controls.randomizeSeedButton();
        Button dailyChallengeBtn = controls.dailyChallengeButton();
        Button copyShareCodeBtn = controls.copyShareCodeButton();

        // INITIALIZE GAMEPLAY CONTROLLER

        gameplayController = new MazeGameplayController(
                new MazeGameplayController.Dependencies(
                        scene,
                        mazeCanvas,

                        // Always retrieve the latest grid.
                        () -> grid,
                        player,
                        playModeToggle,
                        fogOfWarToggle,
                        fogRadiusSlider,
                        showSolutionToggle,
                        openingStatusLabel,

                        // Solver delegation.
                        this::solveAndDisplayPath,

                        // Statistics delegation.
                        this::updateStatistics
                )
        );

        // INITIALIZE GENERATION CONTROLLER

        generationController = new MazeGenerationController(
                new MazeGenerationController.Dependencies(
                        mazeCanvas,

                        // Shared application grid.
                        () -> grid,
                        newGrid -> grid = newGrid,

                        // Configuration controls.
                        shapeCombo,
                        entranceExitModeCombo,
                        solverCombo,
                        showSolutionToggle,
                        seedTextField,
                        openingStatusLabel,

                        // Player.
                        player,

                        // Gameplay state.
                        gameplayController::isFogOfWarActive,
                        gameplayController::getFogRadius,
                        gameplayController::isPlayModeActive,

                        // Gameplay actions.
                        this::stopMovementAnimation,
                        this::spawnPlayerForCurrentMaze,
                        gameplayController::setCustomSelectionActive,

                        // Statistics.
                        this::updateStatistics
                )
        );

        // INITIALIZE FILE MANAGER

        fileManager = new MazeFileManager(
                new MazeFileManager.Dependencies(
                        primaryStage,
                        mazeCanvas,

                        // Shared application grid
                        () -> grid,
                        loadedGrid -> grid = loadedGrid,

                        // Topology selection
                        shapeCombo,

                        // Current theme
                        themeToggle,

                        // Gameplay settings
                        gameplayController::isFogOfWarActive,
                        gameplayController::getFogRadius,

                        // Statistics
                        this::updateStatistics,

                        // File buttons
                        exportBtn,
                        saveMazeBtn,
                        loadMazeBtn,

                        // Fallback generation
                        () -> startGenerationAnimation(20, 20, 12, 0.5, "Recursive Backtracker (DFS)")
                )
        );

        // SOLVER SELECTION

        solverCombo.valueProperty().addListener(
                (obs, oldValue, newValue) -> {
                    if (showSolutionToggle != null && showSolutionToggle.isSelected() && grid != null) {
                        solveAndDisplayPath();
                    }
                }
        );

        // SHOW SOLUTION

        showSolutionToggle.setOnAction(event -> {
            if (grid == null) {
                showSolutionToggle.setSelected(false);
                return;
            }

            if (showSolutionToggle.isSelected()) {
                showSolutionToggle.setText("Hide Solution");
                if (gameplayController.isCustomSelectionActive() || grid.getEntranceCell() == null || grid.getExitCell() == null) {
                    mazeCanvas.setSolvedPath(null);
                    updateStatistics(-1);
                } else {
                    solveAndDisplayPath();
                }
            } else {
                showSolutionToggle.setText("Show Solution");
                mazeCanvas.setSolvedPath(null);
                updateStatistics(-1);
            }
        });

        // BENCHMARKING

        benchmarkBtn.setOnAction(event -> BenchmarkModal.display(primaryStage));

        // THEME CUSTOMIZER

        themeCustomizerBtn.setOnAction(event ->
                ThemeCustomizerModal.display(
                        primaryStage,
                        themeConfig,
                        () -> applyThemeAppearance(
                                primaryStage,
                                themeConfig
                        )
                )
        );

        // ABOUT APPLICATION

        aboutBtn.setOnAction(event ->
                AboutDialog.display(
                        primaryStage,
                        themeConfig.isDarkMode()
                )
        );

        // RANDOMIZE SEED

        randomizeSeedBtn.setOnAction(event -> {
            currentSeed = new java.util.Random().nextLong();
            seedTextField.setText(String.valueOf(currentSeed));
        });

        // DAILY CHALLENGE

        dailyChallengeBtn.setOnAction(event -> {
            String todayStr = java.time.LocalDate
                    .now()
                    .format(java.time.format.DateTimeFormatter.ISO_DATE);
            currentSeed = todayStr.hashCode();
            seedTextField.setText(todayStr);
            openingStatusLabel.setText("Loaded Daily Challenge for: " + todayStr);
        });

        // COPY SHARE CODE

        copyShareCodeBtn.setOnAction(event -> {
            try {
                String shareCode = shapeCombo.getValue() + "|" + seedTextField.getText() + "|" + algorithmCombo.getValue();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(shareCode);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                new Alert(Alert.AlertType.INFORMATION, "Share code copied to clipboard!", ButtonType.OK).show();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Failed to copy to clipboard: " + ex.getMessage(), ButtonType.OK).show();
            }
        });

        // RESPONSIVE CANVAS AUTO-SCALING

        javafx.animation.PauseTransition resizeDebouncer =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));

        resizeDebouncer.setOnFinished(event -> {
            double availableWidth = Math.max(300, scene.getWidth() - 340);
            double availableHeight = Math.max(300, scene.getHeight() - 40);
            if (mazeCanvas != null) {
                double renderWidth = availableWidth;
                double renderHeight = availableHeight;
                if (grid != null
                        && grid.getTopology() == TopologyType.RECTANGULAR
                        && widthSlider != null
                        && heightSlider != null) {

                    double cols = widthSlider.getValue();
                    double rows = heightSlider.getValue();
                    double gridAspect = cols / rows;

                    if (availableWidth / availableHeight > gridAspect) {
                        renderWidth = availableHeight * gridAspect;
                    } else {
                        renderHeight = availableWidth / gridAspect;
                    }
                } else {
                    double size = Math.min(availableWidth, availableHeight);
                    renderWidth = size;
                    renderHeight = size;
                }

                mazeCanvas.resizeCanvas(renderWidth, renderHeight);

                if (grid != null) {
                    if (grid.isCircular()) {
                        grid.layoutCircularGrid(renderWidth, renderHeight, 0.0);
                    } else if (grid.isPolygonal()) {
                        grid.layoutPolygonGrid(renderWidth, renderHeight, 0.0);
                    }
                    mazeCanvas.draw();
                }
            }
        });


        scene.widthProperty().addListener(
                (obs, oldVal, newVal) ->
                        resizeDebouncer.playFromStart()
        );

        scene.heightProperty().addListener(
                (obs, oldVal, newVal) ->
                        resizeDebouncer.playFromStart()
        );

        // THEME ACTION

        themeToggle.setOnAction(event -> {
            themeConfig.setDarkMode(themeToggle.isSelected());
            applyThemeAppearance(primaryStage, themeConfig);
        });

        // GENERATE ACTION

        generateBtn.setOnAction(event ->
                startGenerationAnimation(
                        (int) widthSlider.getValue(),
                        (int) heightSlider.getValue(),
                        speedSlider.getValue(),
                        complexitySlider.getValue() / 100.0,
                        algorithmCombo.getValue()
                )
        );

        // SHOW WINDOW

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // THEME MANAGEMENT

    private void applyThemeAppearance(Stage owner, MazeThemeConfig themeConfig) {
        boolean dark = themeConfig.isDarkMode();

        // UPDATE TOGGLE
        themeToggle.setSelected(dark);
        themeToggle.setText(dark ? "Theme: Dark" : "Theme: Light");

        // UPDATE JAVAFX STYLESHEET

        Scene scene = owner.getScene();
        if (scene != null) {
            String stylesheet = getClass().getResource("/style.css").toExternalForm();
            if (dark) {
                if (!scene.getStylesheets().contains(stylesheet)) {
                    scene.getStylesheets().add(stylesheet);
                }
            } else {
                scene.getStylesheets().remove(stylesheet);
            }
        }

        // UPDATE CANVAS

        mazeCanvas.setThemeConfig(themeConfig);
        mazeCanvas.setDarkMode(dark);
        mazeCanvas.draw();

        // SAVE PREFERENCES

        themeConfig.save();
    }

    // GENERATION DELEGATION

    private void startGenerationAnimation(
            int width,
            int height,
            double animDelay,
            double complexityFactor,
            String algorithm
    ) {
        generationController.startGenerationAnimation(
                width,
                height,
                animDelay,
                complexityFactor,
                algorithm
        );
    }

    // GAMEPLAY DELEGATION

    private void stopMovementAnimation() {
        if (gameplayController != null) {
            gameplayController.stopMovementAnimation();
        }
    }

    private void spawnPlayerForCurrentMaze() {
        if (gameplayController != null) {
            gameplayController.spawnPlayerForCurrentMaze();
        }
    }

    // SOLVER DELEGATION

    private void solveAndDisplayPath() {
        generationController.solveAndDisplayPath();
    }

    // STATISTICS

    private void updateStatistics(int pathLength) {
        if (grid == null) {
            return;
        }

        int totalCells = grid.getCellCount();
        int deadEnds = grid.countDeadEnds();
        String pathString = pathLength > 0
                ? pathLength + " steps"
                : "Hidden / Not solved";


        statsLabel.setText(
                String.format(
                        "Statistics:\n" + "Total Cells: %d\n" + "Dead Ends: %d\n" + "Path Length: %s",
                        totalCells,
                        deadEnds,
                        pathString
                )
        );
    }

    // APPLICATION ENTRY POINT

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(
                (thread, throwable) -> {

                    System.err.println("Uncaught exception on thread " + thread.getName() + ": " + throwable);

                    try {
                        java.nio.file.Files.writeString(
                                java.nio.file.Path.of("error.log"),
                                java.time.LocalDateTime.now() + " [" + thread.getName() + "] " + throwable + "\n",
                                java.nio.file.StandardOpenOption.CREATE,
                                java.nio.file.StandardOpenOption.APPEND
                        );

                    } catch (IOException ignored) {
                    }

                    // RECOVERY DIALOG

                    javafx.application.Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Unexpected Application Error");
                        alert.setHeaderText("An unexpected error occurred.");
                        alert.setContentText("Details have been logged to error.log.\n" + "Error: " + throwable.getMessage());
                        alert.showAndWait();
                    });
                }
        );

        launch(args);
    }
}
