package app;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import model.Cell;
import model.EntranceExitMode;
import model.MazeGrid;
import model.PolarCell;
import model.TopologyType;
import model.MazeThemeConfig;

import view.*;

import game.Player;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * JavaFX application for rectangular, circular, hexagonal,
 * and triangular maze generation and solving.
 * Supports configurable entrance and exit positions:
 * - Left to Right
 * - Top to Bottom
 * - Center to Outer Edge (circular only)
 * - Custom Click Selection
 */
public class MazeGeneratorApp extends Application {
    // CONSTANTS

    private static final double CANVAS_SIZE = 600.0;
    private static final double GEOMETRY_PADDING = 22.0;
    private static final double PLAYER_MOVE_MILLIS = 160.0;
    private final model.MazeThemeConfig themeConfig = new model.MazeThemeConfig(model.MazeThemeConfig.ThemePreset.DEFAULT_DARK);

    // APPLICATION STATE

    private MazeGenerationController generationController;
    private MazeGrid grid;
    private MazeCanvas mazeCanvas;
    private Timeline movementTimeline;
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
    private final Player player = new Player();
    private boolean isPlayMode = false;
    private boolean isFogOfWar = false;
    private boolean isPlayerMoving = false;
    private boolean customSelectionActive = false;
    private Cell customStartCell;
    private int fogRadius = 2;
    private javafx.scene.control.TextField seedTextField;
    private long currentSeed = System.currentTimeMillis();

    // APPLICATION START

    @Override
    public void start(Stage primaryStage) {

        primaryStage.setTitle(
                "Customizable JavaFX Maze Generator & Solver"
        );


        // --------------------------------------------------
        // INITIALIZE APPLICATION COMPONENTS
        // --------------------------------------------------

        mazeCanvas = new MazeCanvas(
                CANVAS_SIZE,
                CANVAS_SIZE
        );

        final MazeThemeConfig themeConfig = MazeThemeConfig.load();

        mazeCanvas.setThemeConfig(themeConfig);


        // --------------------------------------------------
        // BUILD USER INTERFACE
        // --------------------------------------------------

        MazeUIBuilder uiBuilder = new MazeUIBuilder(
                mazeCanvas,
                currentSeed,
                themeConfig.isDarkMode()
        );

        MazeUIBuilder.Controls controls =
                uiBuilder.getControls();

        Scene scene = uiBuilder.getScene();


        // --------------------------------------------------
        // CONNECT UI CONTROLS TO APPLICATION
        // --------------------------------------------------

        // MAZE EDITOR

        shapeCombo = controls.shapeCombo();

        entranceExitModeCombo =
                controls.entranceExitModeCombo();

        openingStatusLabel =
                controls.openingStatusLabel();

        algorithmCombo =
                controls.algorithmCombo();

        solverCombo =
                controls.solverCombo();

        showSolutionToggle =
                controls.showSolutionToggle();

        seedTextField =
                controls.seedTextField();


        // GAMEPLAY

        playModeToggle =
                controls.playModeToggle();

        fogOfWarToggle =
                controls.fogOfWarToggle();


        // FILE / APPEARANCE

        themeToggle =
                controls.themeToggle();

        statsLabel =
                controls.statsLabel();


        // SLIDERS

        Slider widthSlider =
                controls.widthSlider();

        Slider heightSlider =
                controls.heightSlider();

        Slider speedSlider =
                controls.speedSlider();

        Slider complexitySlider =
                controls.complexitySlider();

        Slider fogRadiusSlider =
                controls.fogRadiusSlider();


        // GENERATION BUTTON

        Button generateBtn =
                controls.generateButton();


        // FILE BUTTONS

        Button exportBtn =
                controls.exportButton();

        Button saveMazeBtn =
                controls.saveMazeButton();

        Button loadMazeBtn =
                controls.loadMazeButton();

        Button benchmarkBtn =
                controls.benchmarkButton();

        Button themeCustomizerBtn =
                controls.themeCustomizerButton();

        Button aboutBtn =
                controls.aboutButton();


        // SEED BUTTONS

        Button randomizeSeedBtn =
                controls.randomizeSeedButton();

        Button dailyChallengeBtn =
                controls.dailyChallengeButton();

        Button copyShareCodeBtn =
                controls.copyShareCodeButton();

        // --------------------------------------------------
// INITIALIZE MAZE GENERATION CONTROLLER
// --------------------------------------------------

        generationController = new MazeGenerationController(

                new MazeGenerationController.Dependencies(

                        mazeCanvas,

                        // Shared application grid
                        () -> grid,
                        newGrid -> grid = newGrid,

                        // Configuration controls
                        shapeCombo,
                        entranceExitModeCombo,
                        solverCombo,
                        showSolutionToggle,
                        seedTextField,
                        openingStatusLabel,

                        // Gameplay state
                        player,

                        () -> isFogOfWar,
                        () -> fogRadius,
                        () -> isPlayMode,

                        // Application callbacks
                        this::stopMovementAnimation,
                        this::spawnPlayerForCurrentMaze,

                        active -> {
                            customSelectionActive = active;
                            customStartCell = null;
                        },

                        this::updateStatistics
                )
        );


        // --------------------------------------------------
        // SOLVER SELECTION
        // --------------------------------------------------

        solverCombo.valueProperty().addListener(
                (obs, oldValue, newValue) -> {

                    if (showSolutionToggle != null
                            && showSolutionToggle.isSelected()
                            && grid != null) {

                        solveAndDisplayPath();
                    }
                }
        );


        // --------------------------------------------------
        // FOG RADIUS
        // --------------------------------------------------

        fogRadiusSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {

                    fogRadius = newValue.intValue();

                    if (isFogOfWar && grid != null) {

                        player.updateDiscovery(
                                grid,
                                true,
                                fogRadius
                        );

                        mazeCanvas.setFogOfWarActive(
                                true,
                                fogRadius
                        );
                    }
                }
        );


        // --------------------------------------------------
        // SHOW SOLUTION
        // --------------------------------------------------

        showSolutionToggle.setOnAction(event -> {

            if (grid == null) {

                showSolutionToggle.setSelected(false);

                return;
            }

            if (showSolutionToggle.isSelected()) {

                showSolutionToggle.setText("Hide Solution");

                if (customSelectionActive
                        || grid.getEntranceCell() == null
                        || grid.getExitCell() == null) {

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


        // --------------------------------------------------
        // IMAGE EXPORT
        // --------------------------------------------------

        exportBtn.setOnAction(
                event -> exportMazeImage(primaryStage)
        );


        // --------------------------------------------------
        // SAVE / LOAD
        // --------------------------------------------------

        saveMazeBtn.setOnAction(
                event -> saveLabyrinthFile(primaryStage)
        );

        loadMazeBtn.setOnAction(
                event -> loadLabyrinthFile(primaryStage)
        );


        // --------------------------------------------------
        // BENCHMARKING
        // --------------------------------------------------

        benchmarkBtn.setOnAction(
                event -> view.BenchmarkModal.display(primaryStage)
        );


        // --------------------------------------------------
        // THEME CUSTOMIZER
        // --------------------------------------------------

        themeCustomizerBtn.setOnAction(event ->

                view.ThemeCustomizerModal.display(
                        primaryStage,
                        themeConfig,
                        () -> applyThemeAppearance(
                                primaryStage,
                                themeConfig
                        )
                )
        );


        // --------------------------------------------------
        // ABOUT APPLICATION
        // --------------------------------------------------

        aboutBtn.setOnAction(event ->

                view.AboutDialog.display(
                        primaryStage,
                        themeConfig.isDarkMode()
                )
        );


        // --------------------------------------------------
        // RANDOMIZE SEED
        // --------------------------------------------------

        randomizeSeedBtn.setOnAction(event -> {

            currentSeed = new java.util.Random().nextLong();

            seedTextField.setText(
                    String.valueOf(currentSeed)
            );
        });


        // --------------------------------------------------
        // DAILY CHALLENGE
        // --------------------------------------------------

        dailyChallengeBtn.setOnAction(event -> {

            String todayStr = java.time.LocalDate
                    .now()
                    .format(
                            java.time.format.DateTimeFormatter.ISO_DATE
                    );

            currentSeed = todayStr.hashCode();

            seedTextField.setText(todayStr);

            openingStatusLabel.setText(
                    "Loaded Daily Challenge for: " + todayStr
            );
        });


        // --------------------------------------------------
        // COPY SHARE CODE
        // --------------------------------------------------

        copyShareCodeBtn.setOnAction(event -> {

            try {

                String shareCode =
                        shapeCombo.getValue() + "|" +
                                seedTextField.getText() + "|" +
                                algorithmCombo.getValue();

                javafx.scene.input.ClipboardContent content =
                        new javafx.scene.input.ClipboardContent();

                content.putString(shareCode);

                javafx.scene.input.Clipboard
                        .getSystemClipboard()
                        .setContent(content);

                new Alert(
                        Alert.AlertType.INFORMATION,
                        "Share code copied to clipboard!",
                        ButtonType.OK
                ).show();

            } catch (Exception ex) {

                new Alert(
                        Alert.AlertType.ERROR,
                        "Failed to copy to clipboard: " + ex.getMessage(),
                        ButtonType.OK
                ).show();
            }
        });


        // --------------------------------------------------
        // RESPONSIVE CANVAS AUTO-SCALING
        // --------------------------------------------------

        javafx.animation.PauseTransition resizeDebouncer =
                new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(100)
                );

        resizeDebouncer.setOnFinished(event -> {

            double availableWidth = Math.max(
                    300,
                    scene.getWidth() - 340
            );

            double availableHeight = Math.max(
                    300,
                    scene.getHeight() - 40
            );

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

                    double size = Math.min(
                            availableWidth,
                            availableHeight
                    );

                    renderWidth = size;
                    renderHeight = size;
                }

                mazeCanvas.resizeCanvas(
                        renderWidth,
                        renderHeight
                );

                if (grid != null) {

                    if (grid.isCircular()) {

                        grid.layoutCircularGrid(
                                renderWidth,
                                renderHeight,
                                0.0
                        );

                    } else if (grid.isPolygonal()) {

                        grid.layoutPolygonGrid(
                                renderWidth,
                                renderHeight,
                                0.0
                        );
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


        // --------------------------------------------------
        // THEME ACTION
        // --------------------------------------------------

        themeToggle.setOnAction(event -> {

            themeConfig.setDarkMode(
                    themeToggle.isSelected()
            );

            applyThemeAppearance(
                    primaryStage,
                    themeConfig
            );
        });


        // --------------------------------------------------
        // GENERATE ACTION
        // --------------------------------------------------

        generateBtn.setOnAction(event ->

                startGenerationAnimation(
                        (int) widthSlider.getValue(),
                        (int) heightSlider.getValue(),
                        speedSlider.getValue(),
                        complexitySlider.getValue() / 100.0,
                        algorithmCombo.getValue()
                )
        );


        // --------------------------------------------------
        // PLAYABLE MODE ACTION
        // --------------------------------------------------

        playModeToggle.setOnAction(event -> {

            isPlayMode = playModeToggle.isSelected();

            if (isPlayMode) {

                if (grid == null
                        || grid.getEntranceCell() == null
                        || grid.getExitCell() == null
                        || customSelectionActive) {

                    playModeToggle.setSelected(false);

                    isPlayMode = false;

                    return;
                }

                playModeToggle.setText("Playable Mode: ON");

                spawnPlayerForCurrentMaze();

            } else {

                playModeToggle.setText("Playable Mode: OFF");

                stopMovementAnimation();

                player.reset();

                mazeCanvas.setPlayerCell(null);

                mazeCanvas.setFogOfWarActive(
                        false,
                        fogRadius
                );

                mazeCanvas.clearPlayerScreenPosition();

                mazeCanvas.setHighlightedCells(
                        java.util.List.of()
                );

                mazeCanvas.setHoveredCell(null);
            }
        });


        // --------------------------------------------------
        // FOG OF WAR ACTION
        // --------------------------------------------------

        fogOfWarToggle.setOnAction(event -> {

            isFogOfWar = fogOfWarToggle.isSelected();

            if (isFogOfWar) {

                fogOfWarToggle.setText("Fog of War: ON");

                if (grid != null) {

                    for (Cell cell : grid.getAllCells()) {
                        cell.discovered = false;
                    }

                    if (isPlayMode
                            && player.getCurrentCell() != null) {

                        player.updateDiscovery(
                                grid,
                                true,
                                fogRadius
                        );
                    }
                }

                mazeCanvas.setFogOfWarActive(
                        true,
                        fogRadius
                );

            } else {

                fogOfWarToggle.setText("Fog of War: OFF");

                if (grid != null) {

                    for (Cell cell : grid.getAllCells()) {
                        cell.discovered = true;
                    }
                }

                mazeCanvas.setFogOfWarActive(
                        false,
                        fogRadius
                );
            }
        });


        // --------------------------------------------------
        // KEYBOARD MOVEMENT
        // --------------------------------------------------

        scene.setOnKeyPressed(event -> {

            if (!isPlayMode
                    || customSelectionActive
                    || isPlayerMoving
                    || grid == null
                    || player.getCurrentCell() == null) {

                return;
            }

            Cell current = player.getCurrentCell();

            Cell target;

            switch (grid.getTopology()) {

                case RECTANGULAR ->

                        target = findRectangularKeyboardNeighbor(
                                current,
                                event.getCode()
                        );

                case CIRCULAR ->

                        target = findPolarKeyboardNeighbor(
                                current,
                                event.getCode()
                        );

                case HEXAGONAL, TRIANGULAR ->

                        target = findPolygonKeyboardNeighbor(
                                current,
                                event.getCode()
                        );

                default -> target = null;
            }

            if (target != null) {

                movePlayerTo(target);

                event.consume();
            }
        });


        // --------------------------------------------------
        // GEOMETRIC MOUSE HOVER
        // --------------------------------------------------

        mazeCanvas.setOnMouseMoved(event -> {

            if (customSelectionActive
                    || !isPlayMode
                    || isPlayerMoving
                    || grid == null
                    || !grid.isGeometric()
                    || player.getCurrentCell() == null) {

                mazeCanvas.setHoveredCell(null);

                return;
            }

            Cell hovered = mazeCanvas.findCellAt(
                    event.getX(),
                    event.getY()
            );

            boolean legal = hovered != null
                    && player.getCurrentCell().isPassageOpenTo(hovered);

            mazeCanvas.setHoveredCell(
                    legal ? hovered : null
            );
        });


        mazeCanvas.setOnMouseExited(
                event -> mazeCanvas.setHoveredCell(null)
        );


        // --------------------------------------------------
        // MOUSE CLICK HANDLING
        // --------------------------------------------------

        mazeCanvas.setOnMouseClicked(event -> {

            if (customSelectionActive) {

                if (!isPlayerMoving) {

                    handleCustomSelectionClick(
                            event.getX(),
                            event.getY()
                    );
                }

                event.consume();

                return;
            }

            if (!isPlayMode
                    || isPlayerMoving
                    || grid == null
                    || !grid.isGeometric()
                    || player.getCurrentCell() == null) {

                return;
            }

            Cell clicked = mazeCanvas.findCellAt(
                    event.getX(),
                    event.getY()
            );

            if (clicked != null
                    && player.getCurrentCell().isPassageOpenTo(clicked)) {

                movePlayerTo(clicked);

                event.consume();
            }
        });


        // --------------------------------------------------
        // SHOW WINDOW
        // --------------------------------------------------

        primaryStage.setScene(scene);

        primaryStage.show();
    }


    private void applyThemeAppearance(
            Stage owner,
            MazeThemeConfig themeConfig
    ) {

        boolean dark = themeConfig.isDarkMode();

        // UPDATE TOGGLE

        themeToggle.setSelected(dark);

        themeToggle.setText(
                dark ? "Theme: Dark" : "Theme: Light"
        );

        // UPDATE JAVAFX STYLESHEET

        Scene scene = owner.getScene();

        if (scene != null) {

            String stylesheet =
                    getClass().getResource("/style.css").toExternalForm();

            if (dark) {

                if (!scene.getStylesheets().contains(stylesheet)) {
                    scene.getStylesheets().add(stylesheet);
                }

            } else {

                scene.getStylesheets().remove(stylesheet);
            }
        }

        // UPDATE THE CANVAS

        mazeCanvas.setThemeConfig(themeConfig);
        mazeCanvas.setDarkMode(dark);
        mazeCanvas.draw();

        themeConfig.save();
    }

    // CUSTOM ENTRANCE / EXIT SELECTION

    private void handleCustomSelectionClick(double mouseX, double mouseY) {
        if (!customSelectionActive || grid == null) {
            return;
        }
        Cell clickedCell = mazeCanvas.findCellAt(mouseX, mouseY);
        if (clickedCell == null) {
            return;
        }

        // FIRST CLICK: SELECT START

        if (customStartCell == null) {
            customStartCell = clickedCell;
            grid.setCustomEntrancePreview(customStartCell);
            mazeCanvas.setSolvedPath(null);
            mazeCanvas.draw();
            updateStatistics(-1);
            openingStatusLabel.setText("START selected. Click a different cell to set END.");
            return;
        }

        // SECOND CLICK: SELECT END

        if (clickedCell == customStartCell) {
            openingStatusLabel.setText("Choose a different cell for END.");
            return;
        }
        Cell selectedStart = customStartCell;
        Cell selectedEnd = clickedCell;
        grid.createCustomOpenings(selectedStart, selectedEnd);
        customStartCell = null;
        customSelectionActive = false;
        openingStatusLabel.setText("Custom START and END configured successfully.");
        mazeCanvas.draw();
        if (showSolutionToggle.isSelected()) {
            solveAndDisplayPath();
        } else {
            updateStatistics(-1);
        }
        if (isPlayMode) {
            spawnPlayerForCurrentMaze();
        }
    }

    // PLAYER SPAWN

    private void spawnPlayerForCurrentMaze() {
        if (grid == null || grid.getEntranceCell() == null || grid.getExitCell() == null) {
            return;
        }
        player.spawn(grid);
        player.updateDiscovery(grid, isFogOfWar, fogRadius);
        mazeCanvas.setPlayerCell(player.getCurrentCell());
        mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
        refreshInteractionHints();
    }

    // ENHANCED IMAGE EXPORT (PNG / JPG WITH BACKGROUND FALLBACK)

    private void exportMazeImage(Stage owner) {
        if (mazeCanvas == null || grid == null) {
            new Alert(Alert.AlertType.WARNING, "No maze canvas available to export.", ButtonType.OK).show();
            return;
        }

        try {
            javafx.scene.control.ChoiceDialog<String> formatDialog = new javafx.scene.control.ChoiceDialog<>("PNG", "PNG", "JPG");
            formatDialog.setTitle("Export Image Format");
            formatDialog.setHeaderText("Choose Image Export Format");
            formatDialog.setContentText("Format:");

            java.util.Optional<String> result = formatDialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            String format = result.get();

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Maze Image (" + format + ")");
            if ("JPG".equalsIgnoreCase(format)) {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JPG Image (*.jpg)", "*.jpg"));
                fileChooser.setInitialFileName("maze.jpg");
            } else {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image (*.png)", "*.png"));
                fileChooser.setInitialFileName("maze.png");
            }

            File file = fileChooser.showSaveDialog(owner);
            if (file == null) {
                return;
            }

            int width = Math.max(1, (int) mazeCanvas.getWidth());
            int height = Math.max(1, (int) mazeCanvas.getHeight());
            WritableImage image = new WritableImage(width, height);
            mazeCanvas.snapshot(null, image);
            java.awt.image.BufferedImage bImage = SwingFXUtils.fromFXImage(image, null);

            if (bImage == null) {
                throw new IOException("Failed to create image snapshot from canvas.");
            }

            if ("JPG".equalsIgnoreCase(format)) {
                java.awt.image.BufferedImage solidImage = new java.awt.image.BufferedImage(
                        width, height, java.awt.image.BufferedImage.TYPE_INT_RGB
                );
                java.awt.Graphics2D g2d = solidImage.createGraphics();
                g2d.setColor(themeToggle.isSelected() ? java.awt.Color.BLACK : java.awt.Color.WHITE);
                g2d.fillRect(0, 0, width, height);
                g2d.drawImage(bImage, 0, 0, null);
                g2d.dispose();
                boolean written = ImageIO.write(solidImage, "jpg", file);
                if (!written) {
                    throw new IOException("No appropriate JPG writer found.");
                }
            } else {
                boolean written = ImageIO.write(bImage, "png", file);
                if (!written) {
                    throw new IOException("No appropriate PNG writer found.");
                }
            }

            new Alert(Alert.AlertType.INFORMATION, "Image successfully exported!", ButtonType.OK).show();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Error");
            alert.setHeaderText("Failed to export maze image.");
            alert.setContentText("Check disk space or write permissions: " + ex.getMessage());
            alert.showAndWait();
        }
    }

    // SAVE LABYRINTH (.maze Serialization using Gson)

    private void saveLabyrinthFile(Stage owner) {
        if (grid == null) {
            new Alert(Alert.AlertType.WARNING, "No maze generated to save.", ButtonType.OK).show();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Labyrinth File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Labyrinth Files (*.maze)", "*.maze"));
        fileChooser.setInitialFileName("labyrinth.maze");
        File file = fileChooser.showSaveDialog(owner);
        if (file == null) {
            return;
        }

        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("cols", grid.cols);
            data.put("rows", grid.rows);
            data.put("topology", grid.getTopology().name());
            data.put("polarRingCount", grid.getPolarRingCount());

            java.util.List<java.util.Map<String, Object>> cellList = new java.util.ArrayList<>();
            for (Cell cell : grid.getAllCells()) {
                java.util.Map<String, Object> cData = new java.util.HashMap<>();
                cData.put("x", cell.x);
                cData.put("y", cell.y);
                cData.put("screenX", cell.getScreenX());
                cData.put("screenY", cell.getScreenY());
                cData.put("discovered", cell.discovered);
                cData.put("isEntrance", cell.isEntrance);
                cData.put("isExit", cell.isExit);

                java.util.Map<String, Boolean> neighbors = new java.util.HashMap<>();
                for (java.util.Map.Entry<Cell, Boolean> entry : cell.getNeighbors().entrySet()) {
                    neighbors.put(entry.getKey().x + "," + entry.getKey().y, entry.getValue());
                }
                cData.put("neighbors", neighbors);
                cellList.add(cData);
            }
            data.put("cells", cellList);

            java.nio.file.Files.writeString(file.toPath(), gson.toJson(data), java.nio.charset.StandardCharsets.UTF_8);
            new Alert(Alert.AlertType.INFORMATION, "Labyrinth successfully saved!", ButtonType.OK).show();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to save labyrinth file: " + ex.getMessage(), ButtonType.OK);
            alert.show();
        }
    }

    private void loadLabyrinthFile(Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load Labyrinth File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Labyrinth Files (*.maze)", "*.maze"));
        File file = fileChooser.showOpenDialog(owner);
        if (file == null) {
            return;
        }

        try {
            String json = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            java.util.Map<?, ?> data = gson.fromJson(json, java.util.Map.class);

            if (data == null || !data.containsKey("cols") || !data.containsKey("rows") || !data.containsKey("topology")) {
                throw new IllegalArgumentException("Invalid or corrupt .maze file structure.");
            }

            int cols = ((Number) data.get("cols")).intValue();
            int rows = ((Number) data.get("rows")).intValue();
            TopologyType topology = TopologyType.valueOf((String) data.get("topology"));

            shapeCombo.setValue(topology);
            switch (topology) {
                case RECTANGULAR -> mazeCanvas.setRenderer(new RectangularRenderer());
                case CIRCULAR -> mazeCanvas.setRenderer(new CircularRenderer());
                case HEXAGONAL -> mazeCanvas.setRenderer(new HexagonalRenderer());
                case TRIANGULAR -> mazeCanvas.setRenderer(new TriangularRenderer());
            }

            grid = switch (topology) {
                case HEXAGONAL, TRIANGULAR -> new MazeGrid(cols, cols, topology);
                case RECTANGULAR, CIRCULAR -> new MazeGrid(cols, rows, topology);
            };

            switch (topology) {
                case RECTANGULAR -> {
                    double cellSize = CANVAS_SIZE / Math.max(cols, rows);
                    mazeCanvas.setWidth(cols * cellSize);
                    mazeCanvas.setHeight(rows * cellSize);
                }
                case CIRCULAR -> grid.layoutCircularGrid(CANVAS_SIZE, CANVAS_SIZE, GEOMETRY_PADDING);
                case HEXAGONAL -> grid.layoutHexagonalGrid(CANVAS_SIZE, CANVAS_SIZE, GEOMETRY_PADDING);
                case TRIANGULAR -> grid.layoutTriangularGrid(CANVAS_SIZE, CANVAS_SIZE, GEOMETRY_PADDING);
            }

            java.util.List<?> cellList = (java.util.List<?>) data.get("cells");
            if (cellList != null) {
                java.util.Map<String, Cell> cellMap = new java.util.HashMap<>();
                for (Cell cell : grid.getAllCells()) {
                    cellMap.put(cell.x + "," + cell.y, cell);
                }

                for (Object rawCData : cellList) {
                    java.util.Map<?, ?> cData = (java.util.Map<?, ?>) rawCData;
                    int cx = ((Number) cData.get("x")).intValue();
                    int cy = ((Number) cData.get("y")).intValue();

                    Cell cell = cellMap.get(cx + "," + cy);
                    if (cell != null) {
                        cell.discovered = (Boolean) cData.get("discovered");
                        cell.isEntrance = (Boolean) cData.get("isEntrance");
                        cell.isExit = (Boolean) cData.get("isExit");

                        java.util.Map<?, ?> savedNeighbors = (java.util.Map<?, ?>) cData.get("neighbors");
                        if (savedNeighbors != null) {
                            for (java.util.Map.Entry<?, ?> entry : savedNeighbors.entrySet()) {
                                String targetKey = (String) entry.getKey();
                                boolean isOpen = (Boolean) entry.getValue();
                                Cell neighbor = cellMap.get(targetKey);
                                if (neighbor != null && isOpen) {
                                    cell.getNeighbors().put(neighbor, true);
                                }
                            }
                        }
                    }
                }
            }

            mazeCanvas.setGrid(grid);
            mazeCanvas.setSolvedPath(null);
            mazeCanvas.setCurrentHead(null);
            mazeCanvas.setPlayerCell(null);
            mazeCanvas.clearPlayerScreenPosition();
            mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
            mazeCanvas.draw();
            updateStatistics(-1);

            new Alert(Alert.AlertType.INFORMATION, "Labyrinth loaded successfully!", ButtonType.OK).show();
        } catch (Exception ex) {
            // Fallback State: Fall back to generating a fresh default 20x20 grid
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Load Error");
            alert.setHeaderText("Failed to load labyrinth file.");
            alert.setContentText("The file is corrupt or incompatible: " + ex.getMessage() + "\n\nFalling back to a default 20x20 maze.");
            alert.showAndWait();

            // Trigger fallback default generation
            shapeCombo.setValue(TopologyType.RECTANGULAR);
            startGenerationAnimation(20, 20, 12, 0.5, "Recursive Backtracker (DFS)");
        }
    }

    // RENDERER SELECTION AND MAZE GENERATION

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

    // INTERACTION HINTS

    private void refreshInteractionHints() {
        if (grid != null && grid.isGeometric() && isPlayMode && player.getCurrentCell() != null) {
            mazeCanvas.setHighlightedCells(player.getCurrentCell().getOpenNeighbors());
        } else {
            mazeCanvas.setHighlightedCells(List.of());
        }
        mazeCanvas.setHoveredCell(null);
    }

    // STOP MOVEMENT ANIMATION

    private void stopMovementAnimation() {
        if (movementTimeline != null) {
            movementTimeline.stop();
            movementTimeline = null;
        }
        isPlayerMoving = false;
        if (mazeCanvas != null) {
            mazeCanvas.clearPlayerScreenPosition();
        }
    }

    // RECTANGULAR KEYBOARD MOVEMENT

    private Cell findRectangularKeyboardNeighbor(Cell current, KeyCode key) {
        double dx = 0.0;
        double dy = 0.0;
        switch (key) {
            case W -> dy = -1.0;
            case S -> dy = 1.0;
            case A -> dx = -1.0;
            case D -> dx = 1.0;
            default -> {return null;}
        }
        Cell best = null;
        double bestAlignment = 0.0;
        for (Cell neighbor : current.getOpenNeighbors()) {
            double ndx = neighbor.getScreenX() - current.getScreenX();
            double ndy = neighbor.getScreenY() - current.getScreenY();
            double alignment = ndx * dx + ndy * dy;
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                best = neighbor;
            }
        }
        return best;
    }

    // POLAR KEYBOARD MOVEMENT
    /**
     * Circular controls:
     * W = outward.
     * S = inward.
     * A = counterclockwise.
     * D = clockwise.
     */
    private Cell findPolarKeyboardNeighbor(Cell current, KeyCode key) {
        if (!(current instanceof PolarCell polar)) {
            return null;
        }
        List<Cell> openNeighbors = current.getOpenNeighbors();
        if (openNeighbors.isEmpty()) {
            return null;
        }
        if (polar.isCore()) {
            if (key != KeyCode.W) {
                return null;
            }
            Cell best = null;
            double bestAngle = Double.POSITIVE_INFINITY;
            for (Cell neighbor : openNeighbors) {
                if (neighbor instanceof PolarCell candidate && candidate.getRingIndex() == 1) {
                    double delta = angularDistance(candidate.getCenterAngle(), -Math.PI / 2.0);
                    if (delta < bestAngle) {
                        bestAngle = delta;
                        best = candidate;
                    }
                }
            }
            return best;
        }
        int targetRing = switch (key) {
            case W -> polar.getRingIndex() + 1;
            case S -> polar.getRingIndex() - 1;
            default -> polar.getRingIndex();
        };

        int targetSector = switch (key) {
            case A -> Math.floorMod(polar.getSectorIndex() - 1, polar.getSectorsInRing());
            case D -> (polar.getSectorIndex() + 1) % polar.getSectorsInRing();
            default -> -1;
        };
        Cell best = null;
        double bestAngle = Double.POSITIVE_INFINITY;
        for (Cell neighbor : openNeighbors) {
            if (!(neighbor instanceof PolarCell candidate)) {
                continue;
            }
            boolean matches;
            if (key == KeyCode.A || key == KeyCode.D) {
                matches = candidate.getRingIndex() == polar.getRingIndex() && candidate.getSectorIndex() == targetSector;
            } else if (key == KeyCode.W || key == KeyCode.S) {
                matches = candidate.getRingIndex() == targetRing;
            } else {
                return null;
            }
            if (matches) {
                double delta = angularDistance(candidate.getCenterAngle(), polar.getCenterAngle());
                if (delta < bestAngle) {
                    bestAngle = delta;
                    best = candidate;
                }
            }
        }
        return best;
    }


    // HEXAGONAL / TRIANGULAR KEYBOARD MOVEMENT
    /**
     * Screen-space directional movement.
     * WASD are the main directions.
     * Q/E/Z/C also provide diagonal movement.
     * Mouse movement remains the most precise way to navigate
     * polygonal cells.
     */
    private Cell findPolygonKeyboardNeighbor(Cell current, KeyCode key) {
        double dx;
        double dy;
        switch (key) {
            case W -> {
                dx = 0.0;
                dy = -1.0;
            }
            case S -> {
                dx = 0.0;
                dy = 1.0;
            }
            case A -> {
                dx = -1.0;
                dy = 0.0;
            }
            case D -> {
                dx = 1.0;
                dy = 0.0;
            }
            case Q -> {
                dx = -1.0;
                dy = -1.0;
            }
            case E -> {
                dx = 1.0;
                dy = -1.0;
            }
            case Z -> {
                dx = -1.0;
                dy = 1.0;
            }
            case C -> {
                dx = 1.0;
                dy = 1.0;
            }
            default -> {
                return null;
            }
        }
        Cell best = null;
        double bestAlignment = 0.20;
        for (Cell neighbor : current.getOpenNeighbors()) {
            double ndx = neighbor.getScreenX() - current.getScreenX();
            double ndy = neighbor.getScreenY() - current.getScreenY();
            double magnitude = Math.hypot(ndx, ndy);
            if (magnitude <= 1.0e-9) {
                continue;
            }
            double alignment = (ndx * dx + ndy * dy) / (magnitude * Math.hypot(dx, dy));
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                best = neighbor;
            }
        }
        return best;
    }
    private double angularDistance(double a, double b) {
        return Math.abs(Math.atan2(Math.sin(a - b), Math.cos(a - b)));
    }

    // PLAYER MOVEMENT

    private void movePlayerTo(Cell target) {
        if (!isPlayMode || isPlayerMoving || grid == null || target == null || player.getCurrentCell() == null || !player.getCurrentCell().isPassageOpenTo(target)) {
            return;
        }
        Cell from = player.getCurrentCell();

        // RECTANGULAR MOVEMENT

        if (!grid.isGeometric()) {
            if (player.moveToCell(target)) {
                player.updateDiscovery(grid, isFogOfWar, fogRadius);
                mazeCanvas.setPlayerCell(player.getCurrentCell());
                mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
                if (player.getCurrentCell().isExit) {
                    showVictory();
                }
            }
            return;
        }

        // GEOMETRIC MOVEMENT

        if (movementTimeline != null) {
            movementTimeline.stop();
        }
        isPlayerMoving = true;
        DoubleProperty animatedX = new SimpleDoubleProperty(from.getScreenX());
        DoubleProperty animatedY = new SimpleDoubleProperty(from.getScreenY());
        mazeCanvas.setPlayerScreenPosition(from.getScreenX(), from.getScreenY());
        if (!player.moveToCell(target)) {
            isPlayerMoving = false;
            mazeCanvas.clearPlayerScreenPosition();
            return;
        }

        player.updateDiscovery(grid, isFogOfWar, fogRadius);
        mazeCanvas.setPlayerCell(target);
        mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);

        animatedX.addListener((obs, oldValue, newValue) ->
                mazeCanvas.setPlayerScreenPosition(newValue.doubleValue(), animatedY.get())
        );

        animatedY.addListener((obs, oldValue, newValue) ->
                mazeCanvas.setPlayerScreenPosition(animatedX.get(), newValue.doubleValue())
        );

        movementTimeline = new Timeline(new KeyFrame(Duration.millis(PLAYER_MOVE_MILLIS),
                        new KeyValue(animatedX, target.getScreenX(), Interpolator.EASE_BOTH),
                    new KeyValue(animatedY, target.getScreenY(), Interpolator.EASE_BOTH))
        );

        movementTimeline.setOnFinished(event -> {
            mazeCanvas.clearPlayerScreenPosition();
            isPlayerMoving = false;
            refreshInteractionHints();
            mazeCanvas.setFogOfWarActive(isFogOfWar, fogRadius);
            if (player.getCurrentCell().isExit) {
                showVictory();
            }
        });
        movementTimeline.play();
    }

    // VICTORY
    /**
     * Non-blocking victory dialog.
     * This is safe when called from a JavaFX animation callback.
     */
    private void showVictory() {
        int completedSteps = player.getSteps();
        isPlayMode = false;
        isPlayerMoving = false;
        playModeToggle.setSelected(false);
        playModeToggle.setText("Playable Mode: OFF");

        isFogOfWar = false;
        if (fogOfWarToggle != null) {
            fogOfWarToggle.setSelected(false);
            fogOfWarToggle.setText("Fog of War: OFF");
        }
        if (grid != null) {
            for (Cell cell : grid.getAllCells()) {
                cell.discovered = true; // Reveal all cells upon victory
            }
        }

        if (movementTimeline != null) {
            movementTimeline.stop();
            movementTimeline = null;
        }
        mazeCanvas.setHighlightedCells(List.of());
        mazeCanvas.setHoveredCell(null);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Victory!");
        alert.setHeaderText("Maze Completed!");
        alert.setContentText("Congratulations! You successfully navigated "
                        + "through the maze.\n\n"
                        + "Total Steps: " + completedSteps
        );
        alert.setResizable(false);
        alert.setOnHidden(event -> {
            player.reset();
            mazeCanvas.setPlayerCell(null);
            mazeCanvas.clearPlayerScreenPosition();
            mazeCanvas.setHighlightedCells(List.of());
            mazeCanvas.setHoveredCell(null);
            mazeCanvas.setFogOfWarActive(false, fogRadius);
        });
        alert.show();
    }


    // SOLVE AND DISPLAY PATH

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
        String pathString = pathLength > 0 ? pathLength + " steps" : "Hidden / Not solved";

        statsLabel.setText(String.format("Statistics:\n"
                                + "Total Cells: %d\n"
                                + "Dead Ends: %d\n"
                                + "Path Length: %s",
                        totalCells,
                        deadEnds,
                        pathString
                )
        );
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception on thread " + thread.getName() + ": " + throwable);
            try {
                java.nio.file.Files.writeString(
                        java.nio.file.Path.of("error.log"),
                        java.time.LocalDateTime.now() + " [" + thread.getName() + "] " + throwable.toString() + "\n",
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND
                );
            } catch (IOException ignored) {}

            // Run recovery dialog on JavaFX application thread if possible
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Unexpected Application Error");
                alert.setHeaderText("An unexpected error occurred.");
                alert.setContentText("Details have been logged to error.log.\nError: " + throwable.getMessage());
                alert.showAndWait();
            });
        });

        launch(args);
    }
}