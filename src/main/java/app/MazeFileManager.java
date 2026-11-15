
package app;

import javafx.embed.swing.SwingFXUtils;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;

import javafx.scene.image.WritableImage;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import model.Cell;
import model.MazeGrid;
import model.TopologyType;

import view.CircularRenderer;
import view.HexagonalRenderer;
import view.MazeCanvas;
import view.RectangularRenderer;
import view.TriangularRenderer;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;


/**
 * Handles maze file operations:
 * - Saving and loading .maze files.
 * - Exporting maze images as PNG or JPG.
 *
 * The application remains responsible for owning the current grid.
 */
public final class MazeFileManager {

    private static final double CANVAS_SIZE = 600.0;

    private static final double GEOMETRY_PADDING = 22.0;


    // --------------------------------------------------
    // DEPENDENCIES
    // --------------------------------------------------

    public record Dependencies(

            Stage owner,

            MazeCanvas mazeCanvas,

            Supplier<MazeGrid> gridSupplier,

            Consumer<MazeGrid> gridConsumer,

            ComboBox<TopologyType> shapeCombo,

            ToggleButton themeToggle,

            BooleanSupplier fogOfWarActive,

            IntSupplier fogRadius,

            IntConsumer updateStatistics,

            Button exportButton,

            Button saveMazeButton,

            Button loadMazeButton,

            Runnable fallbackGeneration

    ) { }


    private final Dependencies dependencies;


    // --------------------------------------------------
    // CONSTRUCTOR
    // --------------------------------------------------

    public MazeFileManager(Dependencies dependencies) {

        this.dependencies = java.util.Objects.requireNonNull(
                dependencies,
                "File manager dependencies cannot be null."
        );

        initializeActions();
    }


    // --------------------------------------------------
    // REGISTER BUTTON ACTIONS
    // --------------------------------------------------

    private void initializeActions() {

        dependencies.exportButton().setOnAction(
                event -> exportMazeImage()
        );

        dependencies.saveMazeButton().setOnAction(
                event -> saveLabyrinthFile()
        );

        dependencies.loadMazeButton().setOnAction(
                event -> loadLabyrinthFile()
        );
    }


    // ==================================================
    // IMAGE EXPORT
    // ==================================================

    private void exportMazeImage() {

        MazeCanvas mazeCanvas =
                dependencies.mazeCanvas();

        MazeGrid grid =
                dependencies.gridSupplier().get();


        // VALIDATE CANVAS

        if (mazeCanvas == null || grid == null) {

            new Alert(
                    Alert.AlertType.WARNING,
                    "No maze canvas available to export.",
                    ButtonType.OK
            ).show();

            return;
        }


        try {

            // --------------------------------------------------
            // CHOOSE IMAGE FORMAT
            // --------------------------------------------------

            ChoiceDialog<String> formatDialog =
                    new ChoiceDialog<>(
                            "PNG",
                            "PNG",
                            "JPG"
                    );

            formatDialog.setTitle(
                    "Export Image Format"
            );

            formatDialog.setHeaderText(
                    "Choose Image Export Format"
            );

            formatDialog.setContentText(
                    "Format:"
            );

            Optional<String> result =
                    formatDialog.showAndWait();

            if (result.isEmpty()) {
                return;
            }

            String format = result.get();


            // --------------------------------------------------
            // FILE CHOOSER
            // --------------------------------------------------

            FileChooser fileChooser = new FileChooser();

            fileChooser.setTitle(
                    "Save Maze Image (" + format + ")"
            );

            if ("JPG".equalsIgnoreCase(format)) {

                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter(
                                "JPG Image (*.jpg)",
                                "*.jpg"
                        )
                );

                fileChooser.setInitialFileName(
                        "maze.jpg"
                );

            } else {

                fileChooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter(
                                "PNG Image (*.png)",
                                "*.png"
                        )
                );

                fileChooser.setInitialFileName(
                        "maze.png"
                );
            }


            File file = fileChooser.showSaveDialog(
                    dependencies.owner()
            );

            if (file == null) {
                return;
            }


            // --------------------------------------------------
            // SNAPSHOT CANVAS
            // --------------------------------------------------

            int width = Math.max(
                    1,
                    (int) mazeCanvas.getWidth()
            );

            int height = Math.max(
                    1,
                    (int) mazeCanvas.getHeight()
            );

            WritableImage image = new WritableImage(
                    width,
                    height
            );

            mazeCanvas.snapshot(null, image);

            java.awt.image.BufferedImage bImage =
                    SwingFXUtils.fromFXImage(
                            image,
                            null
                    );

            if (bImage == null) {

                throw new IOException(
                        "Failed to create image snapshot from canvas."
                );
            }


            // --------------------------------------------------
            // JPG EXPORT WITH SOLID BACKGROUND
            // --------------------------------------------------

            if ("JPG".equalsIgnoreCase(format)) {

                java.awt.image.BufferedImage solidImage =
                        new java.awt.image.BufferedImage(
                                width,
                                height,
                                java.awt.image.BufferedImage.TYPE_INT_RGB
                        );

                java.awt.Graphics2D g2d =
                        solidImage.createGraphics();

                g2d.setColor(
                        dependencies.themeToggle().isSelected()
                                ? java.awt.Color.BLACK
                                : java.awt.Color.WHITE
                );

                g2d.fillRect(
                        0,
                        0,
                        width,
                        height
                );

                g2d.drawImage(
                        bImage,
                        0,
                        0,
                        null
                );

                g2d.dispose();

                boolean written = ImageIO.write(
                        solidImage,
                        "jpg",
                        file
                );

                if (!written) {

                    throw new IOException(
                            "No appropriate JPG writer found."
                    );
                }

            } else {

                // --------------------------------------------------
                // PNG EXPORT
                // --------------------------------------------------

                boolean written = ImageIO.write(
                        bImage,
                        "png",
                        file
                );

                if (!written) {

                    throw new IOException(
                            "No appropriate PNG writer found."
                    );
                }
            }


            // SUCCESS

            new Alert(
                    Alert.AlertType.INFORMATION,
                    "Image successfully exported!",
                    ButtonType.OK
            ).show();

        } catch (Exception ex) {

            Alert alert = new Alert(
                    Alert.AlertType.ERROR
            );

            alert.setTitle(
                    "Export Error"
            );

            alert.setHeaderText(
                    "Failed to export maze image."
            );

            alert.setContentText(
                    "Check disk space or write permissions: " +
                            ex.getMessage()
            );

            alert.showAndWait();
        }
    }


    // ==================================================
    // SAVE LABYRINTH
    // ==================================================

    private void saveLabyrinthFile() {

        MazeGrid grid =
                dependencies.gridSupplier().get();


        // VALIDATE GRID

        if (grid == null) {

            new Alert(
                    Alert.AlertType.WARNING,
                    "No maze generated to save.",
                    ButtonType.OK
            ).show();

            return;
        }


        // --------------------------------------------------
        // FILE CHOOSER
        // --------------------------------------------------

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(
                "Save Labyrinth File"
        );

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Labyrinth Files (*.maze)",
                        "*.maze"
                )
        );

        fileChooser.setInitialFileName(
                "labyrinth.maze"
        );

        File file = fileChooser.showSaveDialog(
                dependencies.owner()
        );

        if (file == null) {
            return;
        }


        try {

            // --------------------------------------------------
            // JSON STRUCTURE
            // --------------------------------------------------

            com.google.gson.Gson gson =
                    new com.google.gson.GsonBuilder()
                            .setPrettyPrinting()
                            .create();

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "cols",
                    grid.cols
            );

            data.put(
                    "rows",
                    grid.rows
            );

            data.put(
                    "topology",
                    grid.getTopology().name()
            );

            data.put(
                    "polarRingCount",
                    grid.getPolarRingCount()
            );


            // --------------------------------------------------
            // SERIALIZE CELLS
            // --------------------------------------------------

            List<Map<String, Object>> cellList =
                    new ArrayList<>();

            for (Cell cell : grid.getAllCells()) {

                Map<String, Object> cData =
                        new HashMap<>();

                cData.put("x", cell.x);

                cData.put("y", cell.y);

                cData.put(
                        "screenX",
                        cell.getScreenX()
                );

                cData.put(
                        "screenY",
                        cell.getScreenY()
                );

                cData.put(
                        "discovered",
                        cell.discovered
                );

                cData.put(
                        "isEntrance",
                        cell.isEntrance
                );

                cData.put(
                        "isExit",
                        cell.isExit
                );


                // SERIALIZE NEIGHBOURS

                Map<String, Boolean> neighbors =
                        new HashMap<>();

                for (Map.Entry<Cell, Boolean> entry :
                        cell.getNeighbors().entrySet()) {

                    neighbors.put(
                            entry.getKey().x + "," +
                                    entry.getKey().y,
                            entry.getValue()
                    );
                }

                cData.put(
                        "neighbors",
                        neighbors
                );

                cellList.add(cData);
            }

            data.put(
                    "cells",
                    cellList
            );


            // --------------------------------------------------
            // WRITE FILE
            // --------------------------------------------------

            java.nio.file.Files.writeString(
                    file.toPath(),
                    gson.toJson(data),
                    StandardCharsets.UTF_8
            );


            new Alert(
                    Alert.AlertType.INFORMATION,
                    "Labyrinth successfully saved!",
                    ButtonType.OK
            ).show();

        } catch (Exception ex) {

            Alert alert = new Alert(
                    Alert.AlertType.ERROR,
                    "Failed to save labyrinth file: " +
                            ex.getMessage(),
                    ButtonType.OK
            );

            alert.show();
        }
    }


    // ==================================================
    // LOAD LABYRINTH
    // ==================================================

    private void loadLabyrinthFile() {

        // --------------------------------------------------
        // FILE CHOOSER
        // --------------------------------------------------

        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle(
                "Load Labyrinth File"
        );

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Labyrinth Files (*.maze)",
                        "*.maze"
                )
        );

        File file = fileChooser.showOpenDialog(
                dependencies.owner()
        );

        if (file == null) {
            return;
        }


        try {

            // --------------------------------------------------
            // READ JSON
            // --------------------------------------------------

            String json = java.nio.file.Files.readString(
                    file.toPath(),
                    StandardCharsets.UTF_8
            );

            com.google.gson.Gson gson =
                    new com.google.gson.Gson();

            Map<?, ?> data = gson.fromJson(
                    json,
                    Map.class
            );


            // VALIDATE FILE STRUCTURE

            if (data == null
                    || !data.containsKey("cols")
                    || !data.containsKey("rows")
                    || !data.containsKey("topology")) {

                throw new IllegalArgumentException(
                        "Invalid or corrupt .maze file structure."
                );
            }


            // --------------------------------------------------
            // READ DIMENSIONS AND TOPOLOGY
            // --------------------------------------------------

            int cols = (
                    (Number) data.get("cols")
            ).intValue();

            int rows = (
                    (Number) data.get("rows")
            ).intValue();

            TopologyType topology = TopologyType.valueOf(
                    (String) data.get("topology")
            );


            // --------------------------------------------------
            // UPDATE TOPOLOGY SELECTION
            // --------------------------------------------------

            dependencies.shapeCombo().setValue(topology);


            // --------------------------------------------------
            // SELECT RENDERER
            // --------------------------------------------------

            switch (topology) {

                case RECTANGULAR ->

                        dependencies.mazeCanvas().setRenderer(
                                new RectangularRenderer()
                        );

                case CIRCULAR ->

                        dependencies.mazeCanvas().setRenderer(
                                new CircularRenderer()
                        );

                case HEXAGONAL ->

                        dependencies.mazeCanvas().setRenderer(
                                new HexagonalRenderer()
                        );

                case TRIANGULAR ->

                        dependencies.mazeCanvas().setRenderer(
                                new TriangularRenderer()
                        );
            }


            // --------------------------------------------------
            // CREATE CORRECT GRID
            // --------------------------------------------------

            MazeGrid loadedGrid = switch (topology) {

                case HEXAGONAL, TRIANGULAR ->
                        new MazeGrid(
                                cols,
                                cols,
                                topology
                        );

                case RECTANGULAR, CIRCULAR ->
                        new MazeGrid(
                                cols,
                                rows,
                                topology
                        );
            };


            // --------------------------------------------------
            // APPLY GEOMETRY
            // --------------------------------------------------

            switch (topology) {

                case RECTANGULAR -> {

                    double cellSize =
                            CANVAS_SIZE / Math.max(cols, rows);

                    dependencies.mazeCanvas().setWidth(
                            cols * cellSize
                    );

                    dependencies.mazeCanvas().setHeight(
                            rows * cellSize
                    );
                }

                case CIRCULAR ->

                        loadedGrid.layoutCircularGrid(
                                CANVAS_SIZE,
                                CANVAS_SIZE,
                                GEOMETRY_PADDING
                        );

                case HEXAGONAL ->

                        loadedGrid.layoutHexagonalGrid(
                                CANVAS_SIZE,
                                CANVAS_SIZE,
                                GEOMETRY_PADDING
                        );

                case TRIANGULAR ->

                        loadedGrid.layoutTriangularGrid(
                                CANVAS_SIZE,
                                CANVAS_SIZE,
                                GEOMETRY_PADDING
                        );
            }


            // --------------------------------------------------
            // RESTORE CELLS AND NEIGHBOURS
            // --------------------------------------------------

            List<?> cellList =
                    (List<?>) data.get("cells");

            if (cellList != null) {

                Map<String, Cell> cellMap =
                        new HashMap<>();

                for (Cell cell : loadedGrid.getAllCells()) {

                    cellMap.put(
                            cell.x + "," + cell.y,
                            cell
                    );
                }


                for (Object rawCData : cellList) {

                    Map<?, ?> cData =
                            (Map<?, ?>) rawCData;

                    int cx = (
                            (Number) cData.get("x")
                    ).intValue();

                    int cy = (
                            (Number) cData.get("y")
                    ).intValue();

                    Cell cell = cellMap.get(
                            cx + "," + cy
                    );

                    if (cell != null) {

                        cell.discovered =
                                (Boolean) cData.get("discovered");

                        cell.isEntrance =
                                (Boolean) cData.get("isEntrance");

                        cell.isExit =
                                (Boolean) cData.get("isExit");


                        // RESTORE NEIGHBOURS

                        Map<?, ?> savedNeighbors =
                                (Map<?, ?>) cData.get("neighbors");

                        if (savedNeighbors != null) {

                            for (Map.Entry<?, ?> entry :
                                    savedNeighbors.entrySet()) {

                                String targetKey =
                                        (String) entry.getKey();

                                boolean isOpen =
                                        (Boolean) entry.getValue();

                                Cell neighbor = cellMap.get(
                                        targetKey
                                );

                                if (neighbor != null && isOpen) {

                                    cell.getNeighbors().put(
                                            neighbor,
                                            true
                                    );
                                }
                            }
                        }
                    }
                }
            }


            // --------------------------------------------------
            // UPDATE APPLICATION GRID
            // --------------------------------------------------

            dependencies.gridConsumer().accept(
                    loadedGrid
            );


            // --------------------------------------------------
            // UPDATE CANVAS
            // --------------------------------------------------

            MazeCanvas canvas =
                    dependencies.mazeCanvas();

            canvas.setGrid(loadedGrid);

            canvas.setSolvedPath(null);

            canvas.setCurrentHead(null);

            canvas.setPlayerCell(null);

            canvas.clearPlayerScreenPosition();

            canvas.setFogOfWarActive(
                    dependencies.fogOfWarActive().getAsBoolean(),
                    dependencies.fogRadius().getAsInt()
            );

            canvas.draw();


            // --------------------------------------------------
            // UPDATE STATISTICS
            // --------------------------------------------------

            dependencies.updateStatistics().accept(-1);


            // SUCCESS

            new Alert(
                    Alert.AlertType.INFORMATION,
                    "Labyrinth loaded successfully!",
                    ButtonType.OK
            ).show();

        } catch (Exception ex) {

            // --------------------------------------------------
            // FALLBACK STATE
            // --------------------------------------------------

            Alert alert = new Alert(
                    Alert.AlertType.ERROR
            );

            alert.setTitle(
                    "Load Error"
            );

            alert.setHeaderText(
                    "Failed to load labyrinth file."
            );

            alert.setContentText(
                    "The file is corrupt or incompatible: " +
                            ex.getMessage() +
                            "\n\nFalling back to a default 20x20 maze."
            );

            alert.showAndWait();


            // --------------------------------------------------
            // FALLBACK DEFAULT GENERATION
            // --------------------------------------------------

            dependencies.shapeCombo().setValue(
                    TopologyType.RECTANGULAR
            );

            dependencies.fallbackGeneration().run();
        }
    }
}
