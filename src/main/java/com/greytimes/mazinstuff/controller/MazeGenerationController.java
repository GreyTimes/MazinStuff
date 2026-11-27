package com.greytimes.mazinstuff.controller;

import com.greytimes.mazinstuff.gameplay.Player;

import com.greytimes.mazinstuff.generator.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;

import javafx.util.Duration;

import com.greytimes.mazinstuff.model.grid.Cell;
import com.greytimes.mazinstuff.model.configuration.EntranceExitMode;
import com.greytimes.mazinstuff.model.grid.MazeGrid;
import com.greytimes.mazinstuff.model.topology.TopologyType;

import com.greytimes.mazinstuff.solver.BfsMazeSolver;
import com.greytimes.mazinstuff.solver.DfsMazeSolver;
import com.greytimes.mazinstuff.solver.DijkstraMazeSolver;
import com.greytimes.mazinstuff.solver.MazeSolver;

import com.greytimes.mazinstuff.view.CircularRenderer;
import com.greytimes.mazinstuff.view.HexagonalRenderer;
import com.greytimes.mazinstuff.view.MazeCanvas;
import com.greytimes.mazinstuff.view.RectangularRenderer;
import com.greytimes.mazinstuff.view.TriangularRenderer;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class MazeGenerationController {

    private static final double CANVAS_SIZE = 600.0;
    private static final double GEOMETRY_PADDING = 22.0;
    private Timeline animationTimeline;
    private final Dependencies dependencies;

    // DEPENDENCIES

    public record Dependencies(

            MazeCanvas mazeCanvas,
            Supplier<MazeGrid> gridSupplier,
            Consumer<MazeGrid> gridConsumer,
            ComboBox<TopologyType> shapeCombo,
            ComboBox<EntranceExitMode> entranceExitModeCombo,
            ComboBox<String> solverCombo,
            ToggleButton showSolutionToggle,
            TextField seedTextField,
            Label openingStatusLabel,
            Player player,
            BooleanSupplier fogOfWarActive,
            IntSupplier fogRadius,
            BooleanSupplier playMode,
            Runnable stopMovementAnimation,
            Runnable spawnPlayer,
            Consumer<Boolean> updateCustomSelection,
            IntConsumer updateStatistics

    ) {
    }


    public MazeGenerationController(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(
                dependencies,
                "Generation controller dependencies cannot be null."
        );
    }

    // GENERATE MAZE

    public void startGenerationAnimation(
            int width,
            int height,
            double animDelay,
            double complexityFactor,
            String algorithm
    ) {

        MazeCanvas mazeCanvas = dependencies.mazeCanvas();
        ComboBox<TopologyType> shapeCombo = dependencies.shapeCombo();
        ComboBox<EntranceExitMode> entranceExitModeCombo = dependencies.entranceExitModeCombo();

        // STOP PREVIOUS ANIMATIONS

        if (animationTimeline != null) {
            animationTimeline.stop();
            animationTimeline = null;
        }

        dependencies.stopMovementAnimation().run();
        dependencies.updateCustomSelection().accept(false);
        dependencies.openingStatusLabel().setText("Generating maze...");

        // GET SELECTED SETTINGS

        TopologyType topology = shapeCombo.getValue();
        EntranceExitMode openingMode = entranceExitModeCombo.getValue();

        // PARSE SEED

        long seedToUse;

        try {
            String text = dependencies.seedTextField()
                    .getText()
                    .trim();
            if (text.isEmpty()) {
                seedToUse = System.currentTimeMillis();
            } else {
                try {
                    seedToUse = Long.parseLong(text);
                } catch (NumberFormatException e) {
                    seedToUse = text.hashCode();
                }
            }
        } catch (Exception e) {
            seedToUse = System.currentTimeMillis();
        }
        Random seededRandom = new Random(seedToUse);

        // SELECT RENDERER

        switch (topology) {
            case RECTANGULAR -> mazeCanvas.setRenderer(new RectangularRenderer());
            case CIRCULAR -> mazeCanvas.setRenderer(new CircularRenderer());
            case HEXAGONAL -> mazeCanvas.setRenderer(new HexagonalRenderer());
            case TRIANGULAR -> mazeCanvas.setRenderer(new TriangularRenderer());
        }

        // CREATE CORRECT GRID

        MazeGrid grid = switch (topology) {
            case HEXAGONAL, TRIANGULAR -> new MazeGrid(width, width, topology);
            case RECTANGULAR, CIRCULAR -> new MazeGrid(width, height, topology);
        };

        // Update the application-owned grid.

        dependencies.gridConsumer().accept(grid);

        // CALCULATE RESPONSIVE WORKSPACE DIMENSIONS

        javafx.scene.Scene currentScene = mazeCanvas.getScene();

        double availableWidth = currentScene != null
                ? Math.max(300, currentScene.getWidth() - 340)
                : CANVAS_SIZE;

        double availableHeight = currentScene != null
                ? Math.max(300, currentScene.getHeight() - 40)
                : CANVAS_SIZE;

        double renderWidth = availableWidth;
        double renderHeight = availableHeight;

        if (topology == TopologyType.RECTANGULAR) {
            double gridAspect = (double) width / height;
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

        // CANVAS DIMENSIONS AND GEOMETRY

        switch (topology) {
            case RECTANGULAR -> {
                double cellWidth = renderWidth / width;
                double cellHeight = renderHeight / height;
                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                for (Cell cell : grid.getAllCells()) {
                    cell.screenX = (cell.x + 0.5) * cellWidth;
                    cell.screenY = (cell.y + 0.5) * cellHeight;
                }
            }
            case CIRCULAR -> {
                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                grid.layoutCircularGrid(renderWidth, renderHeight, GEOMETRY_PADDING);
            }
            case HEXAGONAL -> {
                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                grid.layoutHexagonalGrid(renderWidth, renderHeight, GEOMETRY_PADDING);
            }
            case TRIANGULAR -> {
                mazeCanvas.resizeCanvas(renderWidth, renderHeight);
                grid.layoutTriangularGrid(renderWidth, renderHeight, GEOMETRY_PADDING);
            }
        }

        // INITIALIZE CELL STATES

        boolean fogActive = dependencies.fogOfWarActive().getAsBoolean();

        for (Cell cell : grid.getAllCells()) {
            cell.visited = false;
            cell.discovered = !fogActive;
        }

        // RESET PREVIOUS VISUAL STATE

        mazeCanvas.setGrid(grid);
        mazeCanvas.setSolvedPath(null);
        mazeCanvas.setCurrentHead(null);
        mazeCanvas.setPlayerCell(null);
        mazeCanvas.setHighlightedCells(List.of());
        mazeCanvas.setHoveredCell(null);
        mazeCanvas.clearPlayerScreenPosition();
        mazeCanvas.setFogOfWarActive(fogActive, dependencies.fogRadius().getAsInt());

        dependencies.player().reset();
        dependencies.updateStatistics().accept(-1);
        dependencies.showSolutionToggle().setSelected(false);
        dependencies.showSolutionToggle().setText("Show Solution");

        // CHOOSE GENERATOR

        MazeGenerator generator = switch (algorithm) {
            case "Randomized Prim's" -> new PrimsMazeGenerator();
            case "Randomized Kruskal's" -> new KruskalsMazeGenerator();
            case "Aldous-Broder" -> new AldousBroderMazeGenerator();
            case "Wilson's" -> new WilsonsMazeGenerator();
            default -> new DfsMazeGenerator();
        };

        generator.initialize(grid, seededRandom);

        // GENERATION TIMELINE

        animationTimeline = new Timeline();

        KeyFrame keyFrame = new KeyFrame(
                Duration.millis(animDelay),
                event -> {
                    boolean running = generator.step();
                    mazeCanvas.setCurrentHead(generator.getCurrentHead());

                    // GENERATION FINISHED

                    if (!running) {
                        animationTimeline.stop();
                        entranceExitModeCombo.setDisable(false);
                        grid.applyComplexityFactor(complexityFactor, seededRandom);

                        // CREATE ENTRANCE AND EXIT

                        boolean requiresCustomSelection = openingMode == EntranceExitMode.CUSTOM;
                        if (requiresCustomSelection) {
                            grid.createOpenings(EntranceExitMode.CUSTOM, seededRandom);
                            dependencies.updateCustomSelection().accept(true);
                            dependencies.openingStatusLabel().setText("Custom selection: click a cell to set START. " + "Then click another cell to set END.");
                        } else {
                            dependencies.updateCustomSelection().accept(false);
                            grid.createOpenings(openingMode, seededRandom);
                            dependencies.openingStatusLabel().setText("Entrance and exit configured: " + openingMode);
                        }

                        mazeCanvas.setCurrentHead(null);
                        mazeCanvas.draw();

                        // SOLUTION

                        if (dependencies.showSolutionToggle().isSelected() && !requiresCustomSelection) {
                            solveAndDisplayPath();
                        } else {
                            mazeCanvas.setSolvedPath(null);
                            dependencies.updateStatistics().accept(-1);
                        }

                        // SPAWN PLAYER

                        if (dependencies.playMode().getAsBoolean()
                                && grid.getEntranceCell() != null
                                && grid.getExitCell() != null) {
                            dependencies.spawnPlayer().run();
                        }
                    }
                }
        );

        animationTimeline.getKeyFrames().add(keyFrame);
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        entranceExitModeCombo.setDisable(true);
        animationTimeline.play();
    }

    // SOLVE AND DISPLAY PATH

    public void solveAndDisplayPath() {
        MazeGrid grid = dependencies.gridSupplier().get();
        MazeCanvas mazeCanvas = dependencies.mazeCanvas();
        if (grid == null || grid.getEntranceCell() == null || grid.getExitCell() == null) {

            return;
        }

        String selectedSolverName = Objects.requireNonNullElse(
                dependencies.solverCombo().getValue(),
                ""
        );

        MazeSolver solver = switch (selectedSolverName) {
            case "Depth-First Search (DFS)" -> new DfsMazeSolver();
            case "Dijkstra's Algorithm" -> new DijkstraMazeSolver();
            default -> new BfsMazeSolver();
        };

        List<Cell> path = solver.solve(grid);

        if (path != null && !path.isEmpty()) {
            mazeCanvas.setSolvedPath(path);
            dependencies.updateStatistics().accept(path.size());
        } else {
            mazeCanvas.setSolvedPath(null);
            dependencies.updateStatistics().accept(-1);
        }
    }
}
