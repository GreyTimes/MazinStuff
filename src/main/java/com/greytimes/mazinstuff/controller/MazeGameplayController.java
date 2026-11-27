package com.greytimes.mazinstuff.controller;

import com.greytimes.mazinstuff.gameplay.Player;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;

import javafx.scene.input.KeyCode;

import javafx.util.Duration;

import com.greytimes.mazinstuff.model.grid.Cell;
import com.greytimes.mazinstuff.model.grid.MazeGrid;
import com.greytimes.mazinstuff.model.topology.PolarCell;

import com.greytimes.mazinstuff.view.MazeCanvas;

import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class MazeGameplayController {

    private static final double PLAYER_MOVE_MILLIS = 160.0;

    // DEPENDENCIES

    public record Dependencies(
            Scene scene,
            MazeCanvas mazeCanvas,
            Supplier<MazeGrid> gridSupplier,
            Player player,
            ToggleButton playModeToggle,
            ToggleButton fogOfWarToggle,
            Slider fogRadiusSlider,
            ToggleButton showSolutionToggle,
            Label openingStatusLabel,
            Runnable solveAndDisplayPath,
            IntConsumer updateStatistics

    ) { }

    private final Dependencies dependencies;

    // GAMEPLAY STATE

    private Timeline movementTimeline;
    private boolean isPlayMode = false;
    private boolean isFogOfWar = false;
    private boolean isPlayerMoving = false;
    private boolean customSelectionActive = false;
    private Cell customStartCell;
    private int fogRadius = 2;

    // CONSTRUCTOR

    public MazeGameplayController(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies, "Gameplay controller dependencies cannot be null.");
        initializeActions();
    }

    // INITIALIZE EVENT HANDLERS

    private void initializeActions() {
        initializeFogRadiusListener();
        initializePlayableModeAction();
        initializeFogOfWarAction();
        initializeKeyboardMovement();
        initializeMouseInteraction();
    }

    // FOG RADIUS

    private void initializeFogRadiusListener() {
        dependencies.fogRadiusSlider().valueProperty().addListener(
                (obs, oldValue, newValue) -> {
                    fogRadius = newValue.intValue();
                    MazeGrid grid = dependencies.gridSupplier().get();
                    if (isFogOfWar && grid != null) {
                        dependencies.player().updateDiscovery(grid, true, fogRadius);
                        dependencies.mazeCanvas().setFogOfWarActive(true, fogRadius);
                    }
                }
        );
    }

    // PLAYABLE MODE

    private void initializePlayableModeAction() {
        ToggleButton toggle = dependencies.playModeToggle();
        toggle.setOnAction(event -> {
            isPlayMode = toggle.isSelected();
            MazeGrid grid = dependencies.gridSupplier().get();
            if (isPlayMode) {
                if (grid == null
                        || grid.getEntranceCell() == null
                        || grid.getExitCell() == null
                        || customSelectionActive) {
                    toggle.setSelected(false);
                    isPlayMode = false;
                    return;
                }
                toggle.setText("Playable Mode: ON");
                spawnPlayerForCurrentMaze();
            } else {
                toggle.setText("Playable Mode: OFF");
                stopMovementAnimation();
                dependencies.player().reset();
                dependencies.mazeCanvas().setPlayerCell(null);
                dependencies.mazeCanvas().setFogOfWarActive(false, fogRadius);
                dependencies.mazeCanvas().clearPlayerScreenPosition();
                dependencies.mazeCanvas().setHighlightedCells(List.of());
                dependencies.mazeCanvas().setHoveredCell(null);
            }
        });
    }

    // FOG OF WAR

    private void initializeFogOfWarAction() {
        ToggleButton toggle = dependencies.fogOfWarToggle();
        toggle.setOnAction(event -> {
            isFogOfWar = toggle.isSelected();
            MazeGrid grid = dependencies.gridSupplier().get();
            if (isFogOfWar) {
                toggle.setText("Fog of War: ON");
                if (grid != null) {
                    for (Cell cell : grid.getAllCells()) {
                        cell.discovered = false;
                    }
                    if (isPlayMode && dependencies.player().getCurrentCell() != null) {
                        dependencies.player().updateDiscovery(grid, true, fogRadius);
                    }
                }
                dependencies.mazeCanvas().setFogOfWarActive(true, fogRadius);
            } else {
                toggle.setText("Fog of War: OFF");
                if (grid != null) {
                    for (Cell cell : grid.getAllCells()) {
                        cell.discovered = true;
                    }
                }
                dependencies.mazeCanvas().setFogOfWarActive(false, fogRadius);
            }
        });
    }

    // KEYBOARD MOVEMENT

    private void initializeKeyboardMovement() {
        dependencies.scene().setOnKeyPressed(event -> {
            MazeGrid grid = dependencies.gridSupplier().get();
            Player player = dependencies.player();
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
                case RECTANGULAR -> target = findRectangularKeyboardNeighbor(current, event.getCode());
                case CIRCULAR -> target = findPolarKeyboardNeighbor(current, event.getCode());
                case HEXAGONAL, TRIANGULAR -> target = findPolygonKeyboardNeighbor(current, event.getCode());
                default -> target = null;
            }
            if (target != null) {
                movePlayerTo(target);
                event.consume();
            }
        });
    }

    // MOUSE INTERACTION

    private void initializeMouseInteraction() {
        MazeCanvas canvas = dependencies.mazeCanvas();

        // GEOMETRIC MOUSE HOVER

        canvas.setOnMouseMoved(event -> {
            MazeGrid grid = dependencies.gridSupplier().get();
            Player player = dependencies.player();
            if (customSelectionActive
                    || !isPlayMode
                    || isPlayerMoving
                    || grid == null
                    || !grid.isGeometric()
                    || player.getCurrentCell() == null) {
                canvas.setHoveredCell(null);
                return;
            }

            Cell hovered = canvas.findCellAt(event.getX(), event.getY());
            boolean legal = hovered != null && player.getCurrentCell().isPassageOpenTo(hovered);
            canvas.setHoveredCell(legal ? hovered : null);
        });


        // MOUSE EXIT

        canvas.setOnMouseExited(event -> canvas.setHoveredCell(null));

        // MOUSE CLICK

        canvas.setOnMouseClicked(event -> {
            MazeGrid grid = dependencies.gridSupplier().get();
            Player player = dependencies.player();
            if (customSelectionActive) {
                if (!isPlayerMoving) {
                    handleCustomSelectionClick(event.getX(), event.getY());
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
            Cell clicked = canvas.findCellAt(event.getX(), event.getY());
            if (clicked != null && player.getCurrentCell().isPassageOpenTo(clicked)) {
                movePlayerTo(clicked);
                event.consume();
            }
        });
    }

    // CUSTOM ENTRANCE / EXIT SELECTION

    public void setCustomSelectionActive(boolean active) {
        customSelectionActive = active;
        if (!active) {
            customStartCell = null;
        }
    }

    public boolean isCustomSelectionActive() {
        return customSelectionActive;
    }

    private void handleCustomSelectionClick(double mouseX, double mouseY) {
        MazeGrid grid = dependencies.gridSupplier().get();
        MazeCanvas canvas = dependencies.mazeCanvas();

        if (!customSelectionActive || grid == null) {
            return;
        }

        Cell clickedCell = canvas.findCellAt(mouseX, mouseY);

        if (clickedCell == null) {
            return;
        }

        // FIRST CLICK: SELECT START

        if (customStartCell == null) {
            customStartCell = clickedCell;
            grid.setCustomEntrancePreview(customStartCell);
            canvas.setSolvedPath(null);
            canvas.draw();
            dependencies.updateStatistics().accept(-1);
            dependencies.openingStatusLabel().setText("START selected. Click a different cell to set END.");
            return;
        }

        // SECOND CLICK: SELECT END

        if (clickedCell == customStartCell) {
            dependencies.openingStatusLabel().setText("Choose a different cell for END.");

            return;
        }

        Cell selectedStart = customStartCell;
        Cell selectedEnd = clickedCell;
        grid.createCustomOpenings(selectedStart, selectedEnd);
        customStartCell = null;
        customSelectionActive = false;
        dependencies.openingStatusLabel().setText("Custom START and END configured successfully.");
        canvas.draw();


        // UPDATE SOLUTION

        if (dependencies.showSolutionToggle().isSelected()) {
            dependencies.solveAndDisplayPath().run();
        } else {
            dependencies.updateStatistics().accept(-1);
        }

        // SPAWN PLAYER IF PLAY MODE IS ACTIVE

        if (isPlayMode) {
            spawnPlayerForCurrentMaze();
        }
    }

    // PLAYER SPAWN

    public void spawnPlayerForCurrentMaze() {
        MazeGrid grid = dependencies.gridSupplier().get();
        if (grid == null || grid.getEntranceCell() == null || grid.getExitCell() == null) {

            return;
        }

        Player player = dependencies.player();
        MazeCanvas canvas = dependencies.mazeCanvas();
        player.spawn(grid);
        player.updateDiscovery(grid, isFogOfWar, fogRadius);
        canvas.setPlayerCell(player.getCurrentCell());
        canvas.setFogOfWarActive(isFogOfWar, fogRadius);
        refreshInteractionHints();
    }

    // INTERACTION HINTS

    private void refreshInteractionHints() {
        MazeGrid grid = dependencies.gridSupplier().get();
        MazeCanvas canvas = dependencies.mazeCanvas();
        Player player = dependencies.player();
        if (grid != null && grid.isGeometric() && isPlayMode && player.getCurrentCell() != null) {
            canvas.setHighlightedCells(player.getCurrentCell().getOpenNeighbors());
        } else {
            canvas.setHighlightedCells(List.of());
        }
        canvas.setHoveredCell(null);
    }

    // STOP MOVEMENT ANIMATION

    public void stopMovementAnimation() {
        if (movementTimeline != null) {
            movementTimeline.stop();
            movementTimeline = null;
        }
        isPlayerMoving = false;
        dependencies.mazeCanvas().clearPlayerScreenPosition();
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
            default -> {
                return null;
            }
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

    // --------------------------------------------------
    // POLAR KEYBOARD MOVEMENT
    //
    // W = outward
    // S = inward
    // A = counterclockwise
    // D = clockwise
    // --------------------------------------------------

    private Cell findPolarKeyboardNeighbor(Cell current, KeyCode key) {
        if (!(current instanceof PolarCell polar)) {
            return null;
        }

        List<Cell> openNeighbors = current.getOpenNeighbors();

        if (openNeighbors.isEmpty()) {
            return null;
        }

        // CENTER CELL

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

        // TARGET RING

        int targetRing = switch (key) {
            case W -> polar.getRingIndex() + 1;
            case S -> polar.getRingIndex() - 1;
            default -> polar.getRingIndex();
        };

        // TARGET SECTOR

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
                matches = candidate.getRingIndex() == polar.getRingIndex()
                                && candidate.getSectorIndex() == targetSector;
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

    // --------------------------------------------------
    // HEXAGONAL / TRIANGULAR KEYBOARD MOVEMENT
    //
    // WASD: main screen directions
    // Q/E/Z/C: diagonal directions
    // --------------------------------------------------

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


    // ANGULAR DISTANCE

    private double angularDistance(double a, double b) {
        return Math.abs(Math.atan2(Math.sin(a - b), Math.cos(a - b)));
    }

    // PLAYER MOVEMENT

    private void movePlayerTo(Cell target) {
        MazeGrid grid = dependencies.gridSupplier().get();
        Player player = dependencies.player();
        MazeCanvas canvas = dependencies.mazeCanvas();

        if (!isPlayMode
                || isPlayerMoving
                || grid == null
                || target == null
                || player.getCurrentCell() == null
                || !player.getCurrentCell().isPassageOpenTo(target)) {
            return;
        }

        Cell from = player.getCurrentCell();

        // RECTANGULAR MOVEMENT

        if (!grid.isGeometric()) {
            if (player.moveToCell(target)) {
                player.updateDiscovery(grid, isFogOfWar, fogRadius);
                canvas.setPlayerCell(player.getCurrentCell());
                canvas.setFogOfWarActive(isFogOfWar, fogRadius);
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
        canvas.setPlayerScreenPosition(from.getScreenX(), from.getScreenY());

        if (!player.moveToCell(target)) {
            isPlayerMoving = false;
            canvas.clearPlayerScreenPosition();
            return;
        }

        player.updateDiscovery(grid, isFogOfWar, fogRadius);
        canvas.setPlayerCell(target);
        canvas.setFogOfWarActive(isFogOfWar, fogRadius);

        // ANIMATED POSITION

        animatedX.addListener(
                (obs, oldValue, newValue) ->
                        canvas.setPlayerScreenPosition(
                                newValue.doubleValue(),
                                animatedY.get()
                        )
        );

        animatedY.addListener(
                (obs, oldValue, newValue) ->
                        canvas.setPlayerScreenPosition(
                                animatedX.get(),
                                newValue.doubleValue()
                        )
        );

        movementTimeline = new Timeline(
                new KeyFrame(
                        Duration.millis(PLAYER_MOVE_MILLIS),
                        new KeyValue(animatedX, target.getScreenX(), Interpolator.EASE_BOTH),
                        new KeyValue(animatedY, target.getScreenY(), Interpolator.EASE_BOTH)
                )
        );

        // MOVEMENT FINISHED

        movementTimeline.setOnFinished(event -> {
            canvas.clearPlayerScreenPosition();
            isPlayerMoving = false;
            refreshInteractionHints();
            canvas.setFogOfWarActive(isFogOfWar, fogRadius);

            if (player.getCurrentCell().isExit) {
                showVictory();
            }
        });
        movementTimeline.play();
    }

    // VICTORY

    private void showVictory() {
        Player player = dependencies.player();
        MazeCanvas canvas = dependencies.mazeCanvas();
        ToggleButton playToggle = dependencies.playModeToggle();
        ToggleButton fogToggle = dependencies.fogOfWarToggle();
        int completedSteps = player.getSteps();

        // RESET GAMEPLAY STATE

        isPlayMode = false;
        isPlayerMoving = false;
        playToggle.setSelected(false);
        playToggle.setText("Playable Mode: OFF");

        // DISABLE FOG OF WAR

        isFogOfWar = false;
        if (fogToggle != null) {
            fogToggle.setSelected(false);
            fogToggle.setText("Fog of War: OFF");
        }

        // REVEAL ALL CELLS

        MazeGrid grid = dependencies.gridSupplier().get();

        if (grid != null) {
            for (Cell cell : grid.getAllCells()) {
                cell.discovered = true;
            }
        }


        // STOP MOVEMENT

        if (movementTimeline != null) {
            movementTimeline.stop();
            movementTimeline = null;
        }

        canvas.setHighlightedCells(List.of());
        canvas.setHoveredCell(null);

        // VICTORY DIALOG

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Victory!");
        alert.setHeaderText("Maze Completed!");
        alert.setContentText(
                "Congratulations! You successfully navigated " +
                        "through the maze.\n\n" +
                        "Total Steps: " + completedSteps
        );
        alert.setResizable(false);

        alert.setOnHidden(event -> {
            player.reset();
            canvas.setPlayerCell(null);
            canvas.clearPlayerScreenPosition();
            canvas.setHighlightedCells(List.of());
            canvas.setHoveredCell(null);
            canvas.setFogOfWarActive(false, fogRadius);
        });
        alert.show();
    }

    // STATE ACCESS

    public boolean isPlayModeActive() {
        return isPlayMode;
    }

    public boolean isFogOfWarActive() {
        return isFogOfWar;
    }

    public boolean isPlayerMoving() {
        return isPlayerMoving;
    }

    public int getFogRadius() {
        return fogRadius;
    }
}

