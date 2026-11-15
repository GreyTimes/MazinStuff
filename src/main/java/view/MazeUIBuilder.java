
package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import model.EntranceExitMode;
import model.TopologyType;

import java.util.Objects;

public final class MazeUIBuilder {

    // --------------------------------------------------
    // EXPOSED UI CONTROLS
    // --------------------------------------------------

    public record Controls(

            // MAZE EDITOR
            ComboBox<TopologyType> shapeCombo,
            ComboBox<EntranceExitMode> entranceExitModeCombo,
            Label openingStatusLabel,
            ComboBox<String> algorithmCombo,
            ComboBox<String> solverCombo,

            Slider widthSlider,
            Slider heightSlider,
            Slider speedSlider,
            Slider complexitySlider,

            ToggleButton showSolutionToggle,

            TextField seedTextField,
            Button randomizeSeedButton,
            Button dailyChallengeButton,
            Button copyShareCodeButton,

            Button generateButton,

            // GAMEPLAY
            ToggleButton playModeToggle,
            ToggleButton fogOfWarToggle,
            Slider fogRadiusSlider,

            // FILE / APPEARANCE
            ToggleButton themeToggle,
            Button exportButton,
            Button saveMazeButton,
            Button loadMazeButton,
            Button benchmarkButton,
            Button themeCustomizerButton,
            Button aboutButton,

            Label statsLabel

    ) { }


    // --------------------------------------------------
    // BUILDER STATE
    // --------------------------------------------------

    private final Scene scene;

    private final Controls controls;


    public MazeUIBuilder(
            MazeCanvas mazeCanvas,
            long initialSeed,
            boolean initialDarkMode
    ) {

        Objects.requireNonNull(
                mazeCanvas,
                "Maze canvas cannot be null."
        );


        // --------------------------------------------------
        // TOPOLOGY SELECTION
        // --------------------------------------------------

        Label shapeHeader = new Label(
                "Maze Layout Shape:"
        );

        Label shapeInfo = createInfoIcon(
                "Choose rectangular, circular/polar, hexagonal, or triangular geometry."
        );

        ComboBox<TopologyType> shapeCombo =
                new ComboBox<>();

        shapeCombo.getItems().addAll(
                TopologyType.values()
        );

        shapeCombo.setValue(
                TopologyType.RECTANGULAR
        );

        shapeCombo.setMaxWidth(
                Double.MAX_VALUE
        );


        // --------------------------------------------------
        // ENTRANCE / EXIT MODE SELECTION
        // --------------------------------------------------

        Label entranceExitHeader = new Label(
                "Entrance / Exit Mode:"
        );

        Label entranceExitInfo = createInfoIcon(
                "Left to Right: start on the left and finish on the right. " +
                        "Top to Bottom: start at the top and finish at the bottom. " +
                        "Center to Outer Edge: available for circular mazes. " +
                        "Custom: click two cells after generation to select START and END."
        );

        ComboBox<EntranceExitMode> entranceExitModeCombo =
                new ComboBox<>();

        entranceExitModeCombo.setMaxWidth(
                Double.MAX_VALUE
        );

        Label openingStatusLabel = new Label(
                "The selected mode will be applied when you generate a maze."
        );

        openingStatusLabel.setWrapText(true);

        openingStatusLabel.setMaxWidth(
                Double.MAX_VALUE
        );

        configureEntranceExitModeOptions(
                entranceExitModeCombo,
                shapeCombo.getValue()
        );


        // --------------------------------------------------
        // GENERATION ALGORITHM SELECTION
        // --------------------------------------------------

        Label algoHeader = new Label(
                "Gen Algorithm:"
        );

        Label algoInfo = createInfoIcon(
                "DFS creates long winding corridors. Prim's and Kruskal's " +
                        "produce branched mazes. Aldous-Broder and Wilson's " +
                        "generate uniform spanning trees."
        );

        ComboBox<String> algorithmCombo =
                new ComboBox<>();

        algorithmCombo.getItems().addAll(
                "Recursive Backtracker (DFS)",
                "Randomized Prim's",
                "Randomized Kruskal's",
                "Aldous-Broder",
                "Wilson's"
        );

        algorithmCombo.setValue(
                "Recursive Backtracker (DFS)"
        );

        algorithmCombo.setMaxWidth(
                Double.MAX_VALUE
        );


        // --------------------------------------------------
        // SOLVER SELECTION
        // --------------------------------------------------

        Label solverHeader = new Label(
                "Solver Algorithm:"
        );

        Label solverInfo = createInfoIcon(
                "BFS finds the fewest cell transitions. DFS explores " +
                        "a deep route. Dijkstra minimizes geometric distance."
        );

        ComboBox<String> solverCombo =
                new ComboBox<>();

        solverCombo.getItems().addAll(
                "Breadth-First Search (BFS)",
                "Depth-First Search (DFS)",
                "Dijkstra's Algorithm"
        );

        solverCombo.setValue(
                "Breadth-First Search (BFS)"
        );

        solverCombo.setMaxWidth(
                Double.MAX_VALUE
        );


        // --------------------------------------------------
        // PLAYABLE MODE
        // --------------------------------------------------

        ToggleButton playModeToggle =
                new ToggleButton("Playable Mode: OFF");

        playModeToggle.setMaxWidth(
                Double.MAX_VALUE
        );

        Label playInfo = createInfoIcon(
                "Rectangular: WASD. Circular: click highlighted passages " +
                        "or use W/S/A/D for outward/inward/counterclockwise/clockwise. " +
                        "Hexagonal and triangular: click a highlighted open neighbor; " +
                        "WASD uses approximate screen directions."
        );

        HBox playBox = new HBox(
                5,
                playModeToggle,
                playInfo
        );

        HBox.setHgrow(
                playModeToggle,
                Priority.ALWAYS
        );

        playBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // FOG OF WAR
        // --------------------------------------------------

        ToggleButton fogOfWarToggle =
                new ToggleButton("Fog of War: OFF");

        fogOfWarToggle.setMaxWidth(
                Double.MAX_VALUE
        );

        Label fogInfo = createInfoIcon(
                "Hides undiscovered cells outside the player's " +
                        "graph-distance visibility radius."
        );

        HBox fogBox = new HBox(
                5,
                fogOfWarToggle,
                fogInfo
        );

        HBox.setHgrow(
                fogOfWarToggle,
                Priority.ALWAYS
        );

        fogBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // THEME
        // --------------------------------------------------

        ToggleButton themeToggle = new ToggleButton(
                initialDarkMode ? "Theme: Dark" : "Theme: Light"
        );

        themeToggle.setMaxWidth(
                Double.MAX_VALUE
        );

        themeToggle.setSelected(initialDarkMode);

        Label themeInfo = createInfoIcon(
                "Switches between dark and light visual themes."
        );

        HBox themeBox = new HBox(
                5,
                themeToggle,
                themeInfo
        );

        HBox.setHgrow(
                themeToggle,
                Priority.ALWAYS
        );

        themeBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // WIDTH / TOPOLOGY PARAMETER SLIDER
        // --------------------------------------------------

        Label widthLabel = new Label(
                "Grid Width (Cols): 20"
        );

        Label widthInfo = createInfoIcon(
                "Rectangular maze column count."
        );

        HBox widthHeaderBox = new HBox(
                5,
                widthLabel,
                widthInfo
        );

        Slider widthSlider = new Slider(
                10,
                50,
                20
        );

        configureSlider(
                widthSlider,
                10
        );

        widthSlider.valueProperty().addListener(
                (obs, oldValue, newValue) ->

                        widthLabel.setText(
                                widthLabelFor(shapeCombo.getValue()) +
                                        newValue.intValue()
                        )
        );


        // --------------------------------------------------
        // HEIGHT / TOPOLOGY PARAMETER SLIDER
        // --------------------------------------------------

        Label heightLabel = new Label(
                "Grid Height (Rows): 20"
        );

        Label heightInfo = createInfoIcon(
                "Rectangular maze row count."
        );

        HBox heightHeaderBox = new HBox(
                5,
                heightLabel,
                heightInfo
        );

        Slider heightSlider = new Slider(
                10,
                50,
                20
        );

        configureSlider(
                heightSlider,
                10
        );

        heightSlider.valueProperty().addListener(
                (obs, oldValue, newValue) ->

                        heightLabel.setText(
                                heightLabelFor(shapeCombo.getValue()) +
                                        newValue.intValue()
                        )
        );


        // --------------------------------------------------
        // TOPOLOGY-DEPENDENT SLIDERS
        // --------------------------------------------------

        shapeCombo.valueProperty().addListener(
                (obs, oldTopology, newTopology) -> {

                    configureDimensionSliders(
                            newTopology,
                            widthSlider,
                            heightSlider,
                            widthLabel,
                            heightLabel,
                            widthInfo,
                            heightInfo,
                            heightHeaderBox
                    );

                    configureEntranceExitModeOptions(
                            entranceExitModeCombo,
                            newTopology
                    );
                }
        );


        configureDimensionSliders(
                shapeCombo.getValue(),
                widthSlider,
                heightSlider,
                widthLabel,
                heightLabel,
                widthInfo,
                heightInfo,
                heightHeaderBox
        );


        // --------------------------------------------------
        // GENERATION ANIMATION SPEED
        // --------------------------------------------------

        Label speedLabel = new Label(
                "Animation Delay (ms): 10"
        );

        Label speedInfo = createInfoIcon(
                "Controls the step-by-step maze generation animation speed."
        );

        HBox speedHeaderBox = new HBox(
                5,
                speedLabel,
                speedInfo
        );

        Slider speedSlider = new Slider(
                0,
                50,
                10
        );

        speedSlider.setMajorTickUnit(10);
        speedSlider.setMinorTickCount(4);
        speedSlider.setShowTickMarks(true);
        speedSlider.setShowTickLabels(true);

        speedSlider.valueProperty().addListener(
                (obs, oldValue, newValue) -> {

                    int val = newValue.intValue();

                    if (val == 0) {

                        speedSlider.setValue(1);

                        val = 1;
                    }

                    speedLabel.setText(
                            "Animation Delay (ms): " + val
                    );
                }
        );


        // --------------------------------------------------
        // COMPLEXITY
        // --------------------------------------------------

        Label complexityLabel = new Label(
                "Complexity / Dead-End Density: 50%"
        );

        Label complexityInfo = createInfoIcon(
                "Controls how aggressively extra passages are carved to reduce dead ends."
        );

        HBox complexityHeaderBox = new HBox(
                5,
                complexityLabel,
                complexityInfo
        );

        Slider complexitySlider = new Slider(
                0,
                100,
                50
        );

        configureSlider(
                complexitySlider,
                25
        );

        complexitySlider.valueProperty().addListener(
                (obs, oldValue, newValue) ->

                        complexityLabel.setText(
                                "Complexity / Dead-End Density: " +
                                        newValue.intValue() + "%"
                        )
        );


        // --------------------------------------------------
        // FOG RADIUS
        // --------------------------------------------------

        Label fogRadiusLabel = new Label(
                "Fog Radius: 2 cells"
        );

        Label fogRadiusInfo = createInfoIcon(
                "Controls the player's visible graph-distance radius."
        );

        HBox fogRadiusHeaderBox = new HBox(
                5,
                fogRadiusLabel,
                fogRadiusInfo
        );

        Slider fogRadiusSlider = new Slider(
                1,
                5,
                2
        );

        configureSlider(
                fogRadiusSlider,
                1
        );

        fogRadiusSlider.valueProperty().addListener(
                (obs, oldValue, newValue) ->

                        fogRadiusLabel.setText(
                                "Fog Radius: " +
                                        newValue.intValue() +
                                        " cells"
                        )
        );


        // --------------------------------------------------
        // GENERATE BUTTON
        // --------------------------------------------------

        Button generateButton = new Button(
                "Generate Labyrinth"
        );

        generateButton.setMaxWidth(
                Double.MAX_VALUE
        );

        Label genInfo = createInfoIcon(
                "Generates a new maze using the selected topology and settings."
        );

        HBox generateBox = new HBox(
                5,
                generateButton,
                genInfo
        );

        HBox.setHgrow(
                generateButton,
                Priority.ALWAYS
        );

        generateBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // SHOW SOLUTION
        // --------------------------------------------------

        ToggleButton showSolutionToggle =
                new ToggleButton("Show Solution");

        showSolutionToggle.setMaxWidth(
                Double.MAX_VALUE
        );

        Label solutionInfo = createInfoIcon(
                "Displays the route from entrance to exit using the selected solver."
        );

        HBox solutionBox = new HBox(
                5,
                showSolutionToggle,
                solutionInfo
        );

        HBox.setHgrow(
                showSolutionToggle,
                Priority.ALWAYS
        );

        solutionBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // EXPORT IMAGE
        // --------------------------------------------------

        Button exportButton = new Button(
                "Export Image"
        );

        exportButton.setMaxWidth(
                Double.MAX_VALUE
        );

        Label exportInfo = createInfoIcon(
                "Exports the current maze canvas as a PNG image."
        );

        HBox exportBox = new HBox(
                5,
                exportButton,
                exportInfo
        );

        HBox.setHgrow(
                exportButton,
                Priority.ALWAYS
        );

        exportBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // SAVE LABYRINTH
        // --------------------------------------------------

        Button saveMazeButton = new Button(
                "Save Labyrinth"
        );

        saveMazeButton.setMaxWidth(
                Double.MAX_VALUE
        );

        Label saveInfo = createInfoIcon(
                "Serializes and saves the current maze to a .maze file."
        );

        HBox saveBox = new HBox(
                5,
                saveMazeButton,
                saveInfo
        );

        HBox.setHgrow(
                saveMazeButton,
                Priority.ALWAYS
        );

        saveBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // LOAD LABYRINTH
        // --------------------------------------------------

        Button loadMazeButton = new Button(
                "Load Labyrinth"
        );

        loadMazeButton.setMaxWidth(
                Double.MAX_VALUE
        );

        Label loadInfo = createInfoIcon(
                "Loads and deserializes a maze from a .maze file."
        );

        HBox loadBox = new HBox(
                5,
                loadMazeButton,
                loadInfo
        );

        HBox.setHgrow(
                loadMazeButton,
                Priority.ALWAYS
        );

        loadBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // BENCHMARKING DASHBOARD
        // --------------------------------------------------

        Button benchmarkButton = new Button(
                "Performance Dashboard (#28)"
        );

        benchmarkButton.setMaxWidth(
                Double.MAX_VALUE
        );

        benchmarkButton.setStyle(
                "-fx-base: #00bcd4; -fx-font-weight: bold;"
        );

        Label benchmarkInfo = createInfoIcon(
                "Launches the benchmarking and analytics dashboard " +
                        "comparing generation and solving algorithms."
        );

        HBox benchmarkBox = new HBox(
                5,
                benchmarkButton,
                benchmarkInfo
        );

        HBox.setHgrow(
                benchmarkButton,
                Priority.ALWAYS
        );

        benchmarkBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // THEME CUSTOMIZER
        // --------------------------------------------------

        Button themeCustomizerButton = new Button(
                "Customize Colors & Themes (#29)"
        );

        themeCustomizerButton.setMaxWidth(
                Double.MAX_VALUE
        );

        themeCustomizerButton.setStyle(
                "-fx-base: #00acc1; -fx-font-weight: bold;"
        );

        Label customizerInfo = createInfoIcon(
                "Opens advanced color pickers, wall thickness sliders, and preset themes."
        );

        HBox customizerBox = new HBox(
                5,
                themeCustomizerButton,
                customizerInfo
        );

        HBox.setHgrow(
                themeCustomizerButton,
                Priority.ALWAYS
        );

        customizerBox.setAlignment(
                Pos.CENTER_LEFT
        );


        // --------------------------------------------------
        // ABOUT APPLICATION
        // --------------------------------------------------

        Button aboutButton = new Button(
                "About MazinStuff"
        );

        aboutButton.setMaxWidth(
                Double.MAX_VALUE
        );


        // --------------------------------------------------
        // STATISTICS
        // --------------------------------------------------

        Label statsLabel = new Label(
                "Statistics:\n" +
                        "Total Cells: -\n" +
                        "Dead Ends: -\n" +
                        "Path Length: -"
        );

        statsLabel.setStyle(
                "-fx-font-family: monospace; -fx-padding: 10;"
        );

        Label statsInfo = createInfoIcon(
                "Live metrics for cells, dead ends, and solution path length."
        );

        HBox statsHeaderBox = new HBox(
                5,
                new Label("Maze Metrics"),
                statsInfo
        );


        // --------------------------------------------------
        // SEED & SHARE CODE CONTROLS
        // --------------------------------------------------

        Label seedHeader = new Label(
                "Generation Seed / Share Code:"
        );

        Label seedInfo = createInfoIcon(
                "Enter a custom seed or share code to reproduce exact mazes."
        );

        TextField seedTextField = new TextField(
                String.valueOf(initialSeed)
        );

        seedTextField.setMaxWidth(
                Double.MAX_VALUE
        );


        Button randomizeSeedButton = new Button(
                "Randomize Seed"
        );

        randomizeSeedButton.setMaxWidth(
                Double.MAX_VALUE
        );


        Button dailyChallengeButton = new Button(
                "Maze of the Day (Daily Challenge)"
        );

        dailyChallengeButton.setMaxWidth(
                Double.MAX_VALUE
        );


        Button copyShareCodeButton = new Button(
                "Copy Share Code"
        );

        copyShareCodeButton.setMaxWidth(
                Double.MAX_VALUE
        );


        // --------------------------------------------------
        // TAB PANE
        // --------------------------------------------------

        TabPane controlTabs = new TabPane();

        controlTabs.setTabClosingPolicy(
                TabPane.TabClosingPolicy.UNAVAILABLE
        );


        // TAB 1: MAZE EDITOR

        VBox genSolveContent = new VBox(
                8,

                new HBox(5, shapeHeader, shapeInfo),
                shapeCombo,

                new HBox(5, entranceExitHeader, entranceExitInfo),
                entranceExitModeCombo,
                openingStatusLabel,

                new HBox(5, algoHeader, algoInfo),
                algorithmCombo,

                widthHeaderBox,
                widthSlider,

                heightHeaderBox,
                heightSlider,

                complexityHeaderBox,
                complexitySlider,

                new Separator(),

                new HBox(5, solverHeader, solverInfo),
                solverCombo,

                speedHeaderBox,
                speedSlider,

                solutionBox,

                new Separator(),

                new HBox(5, seedHeader, seedInfo),
                seedTextField,

                randomizeSeedButton,
                dailyChallengeButton,
                copyShareCodeButton
        );

        genSolveContent.setPadding(
                new Insets(10)
        );

        ScrollPane genSolveScroll = new ScrollPane(
                genSolveContent
        );

        genSolveScroll.setFitToWidth(true);

        genSolveScroll.setStyle(
                "-fx-background: transparent; " +
                        "-fx-background-color: transparent;"
        );

        Tab genSolveTab = new Tab(
                "Maze Editor",
                genSolveScroll
        );


        // TAB 2: PLAY

        VBox gameContent = new VBox(
                8,
                playBox,
                fogBox,
                fogRadiusHeaderBox,
                fogRadiusSlider
        );

        gameContent.setPadding(
                new Insets(10)
        );

        Tab gameTab = new Tab(
                "Play",
                gameContent
        );


        // TAB 3: FILE & ANALYTICS

        VBox fileContent = new VBox(
                8,

                themeBox,
                customizerBox,
                exportBox,
                saveBox,
                loadBox,
                benchmarkBox,

                new Separator(),

                aboutButton,

                new Separator(),

                statsHeaderBox,
                statsLabel
        );

        fileContent.setPadding(
                new Insets(10)
        );

        Tab fileTab = new Tab(
                "File",
                fileContent
        );


        controlTabs.getTabs().addAll(
                genSolveTab,
                gameTab,
                fileTab
        );


        // --------------------------------------------------
        // PINNED GLOBAL TOOLBAR
        // --------------------------------------------------

        VBox pinnedGlobalBar = new VBox(
                8,
                generateBox,
                playBox
        );

        pinnedGlobalBar.setPadding(
                new Insets(10)
        );


        // --------------------------------------------------
        // CONTROLS LAYOUT CONTAINER
        // --------------------------------------------------

        VBox controlsContainer = new VBox(
                0,
                pinnedGlobalBar,
                new Separator(),
                controlTabs
        );

        controlsContainer.setPrefWidth(320);


        // --------------------------------------------------
        // CANVAS CONTAINER
        // --------------------------------------------------

        StackPane canvasContainer = new StackPane(
                mazeCanvas
        );

        canvasContainer.setAlignment(
                Pos.CENTER
        );

        canvasContainer.setStyle(
                "-fx-padding: 15;"
        );


        // --------------------------------------------------
        // MAIN LAYOUT
        // --------------------------------------------------

        BorderPane root = new BorderPane();

        root.setCenter(canvasContainer);

        root.setRight(controlsContainer);


        // --------------------------------------------------
        // SCENE
        // --------------------------------------------------

        scene = new Scene(
                root,
                940,
                840
        );

        scene.getStylesheets().add(
                getClass()
                        .getResource("/style.css")
                        .toExternalForm()
        );


        // --------------------------------------------------
        // EXPOSE CONTROLS TO APPLICATION
        // --------------------------------------------------

        controls = new Controls(

                // MAZE EDITOR
                shapeCombo,
                entranceExitModeCombo,
                openingStatusLabel,
                algorithmCombo,
                solverCombo,

                widthSlider,
                heightSlider,
                speedSlider,
                complexitySlider,

                showSolutionToggle,

                seedTextField,
                randomizeSeedButton,
                dailyChallengeButton,
                copyShareCodeButton,

                generateButton,

                // GAMEPLAY
                playModeToggle,
                fogOfWarToggle,
                fogRadiusSlider,

                // FILE / APPEARANCE
                themeToggle,
                exportButton,
                saveMazeButton,
                loadMazeButton,
                benchmarkButton,
                themeCustomizerButton,
                aboutButton,

                statsLabel
        );
    }


    // --------------------------------------------------
    // PUBLIC ACCESS
    // --------------------------------------------------

    public Scene getScene() {
        return scene;
    }

    public Controls getControls() {
        return controls;
    }


    // --------------------------------------------------
    // ENTRANCE / EXIT MODE OPTIONS
    // --------------------------------------------------

    private static void configureEntranceExitModeOptions(
            ComboBox<EntranceExitMode> combo,
            TopologyType topology
    ) {

        EntranceExitMode previous = combo.getValue();

        combo.getItems().clear();

        combo.getItems().addAll(
                EntranceExitMode.LEFT_TO_RIGHT,
                EntranceExitMode.TOP_TO_BOTTOM
        );

        if (topology == TopologyType.CIRCULAR) {

            combo.getItems().add(
                    EntranceExitMode.CENTER_TO_OUTER
            );
        }

        combo.getItems().add(
                EntranceExitMode.CUSTOM
        );

        if (previous != null &&
                combo.getItems().contains(previous)) {

            combo.setValue(previous);

        } else {

            combo.setValue(
                    EntranceExitMode.LEFT_TO_RIGHT
            );
        }
    }


    // --------------------------------------------------
    // SLIDER HELPERS
    // --------------------------------------------------

    private static void configureSlider(
            Slider slider,
            double majorTickUnit
    ) {

        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(majorTickUnit);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);
    }


    private static void configureDimensionSliders(
            TopologyType topology,
            Slider widthSlider,
            Slider heightSlider,
            Label widthLabel,
            Label heightLabel,
            Label widthInfo,
            Label heightInfo,
            HBox heightHeaderBox
    ) {

        double widthMin;
        double widthMax;
        double widthDefault;
        double widthMajor;

        double heightMin;
        double heightMax;
        double heightDefault;
        double heightMajor;

        boolean singleDimension = false;

        switch (topology) {

            case RECTANGULAR -> {

                widthMin = 10;
                widthMax = 50;
                widthDefault = 20;
                widthMajor = 10;

                heightMin = 10;
                heightMax = 50;
                heightDefault = 20;
                heightMajor = 10;
            }

            case CIRCULAR -> {

                widthMin = 8;
                widthMax = 24;
                widthDefault = 16;
                widthMajor = 4;

                heightMin = 4;
                heightMax = 12;
                heightDefault = 8;
                heightMajor = 2;
            }

            case HEXAGONAL -> {

                widthMin = 2;
                widthMax = 7;
                widthDefault = 4;
                widthMajor = 1;

                heightMin = 1;
                heightMax = 1;
                heightDefault = 1;
                heightMajor = 1;

                singleDimension = true;
            }

            case TRIANGULAR -> {

                widthMin = 4;
                widthMax = 16;
                widthDefault = 10;
                widthMajor = 2;

                heightMin = 1;
                heightMax = 1;
                heightDefault = 1;
                heightMajor = 1;

                singleDimension = true;
            }

            default -> throw new IllegalStateException(
                    "Unknown topology: " + topology
            );
        }


        widthSlider.setMin(widthMin);
        widthSlider.setMax(widthMax);
        widthSlider.setMajorTickUnit(widthMajor);
        widthSlider.setBlockIncrement(1);
        widthSlider.setSnapToTicks(true);
        widthSlider.setValue(widthDefault);


        heightSlider.setMin(heightMin);
        heightSlider.setMax(heightMax);
        heightSlider.setMajorTickUnit(heightMajor);
        heightSlider.setBlockIncrement(1);
        heightSlider.setSnapToTicks(true);
        heightSlider.setValue(heightDefault);


        heightHeaderBox.setVisible(!singleDimension);
        heightHeaderBox.setManaged(!singleDimension);

        heightSlider.setVisible(!singleDimension);
        heightSlider.setManaged(!singleDimension);


        widthLabel.setText(
                widthLabelFor(topology) + (int) widthDefault
        );

        heightLabel.setText(
                heightLabelFor(topology) + (int) heightDefault
        );


        updateInfoTooltip(
                widthInfo,
                widthTooltipFor(topology)
        );

        updateInfoTooltip(
                heightInfo,
                heightTooltipFor(topology)
        );
    }


    private static String widthLabelFor(TopologyType topology) {

        return switch (topology) {

            case RECTANGULAR ->
                    "Grid Width (Cols): ";

            case CIRCULAR ->
                    "Outer Ring Sectors: ";

            case HEXAGONAL ->
                    "Hex Board Radius: ";

            case TRIANGULAR ->
                    "Triangle Side Length: ";
        };
    }


    private static String heightLabelFor(TopologyType topology) {

        return switch (topology) {

            case RECTANGULAR ->
                    "Grid Height (Rows): ";

            case CIRCULAR ->
                    "Number of Rings: ";

            case HEXAGONAL ->
                    "Hexagon Height: ";

            case TRIANGULAR ->
                    "Triangle Height: ";
        };
    }


    private static String widthTooltipFor(TopologyType topology) {

        return switch (topology) {

            case RECTANGULAR ->
                    "Number of rectangular columns (10–50).";

            case CIRCULAR ->
                    "Number of sectors along the outermost polar ring (8–24).";

            case HEXAGONAL ->
                    "Controls the radius of the regular hexagonal board (2–7). " +
                            "The outer boundary is a regular hexagon.";

            case TRIANGULAR ->
                    "Controls the number of small equilateral triangle edges " +
                            "along each side of the large triangular board (4–16). " +
                            "Total cells = N squared.";
        };
    }


    private static String heightTooltipFor(TopologyType topology) {

        return switch (topology) {

            case RECTANGULAR ->
                    "Number of rectangular rows (10–50).";

            case CIRCULAR ->
                    "Number of concentric rings outside the center (4–12).";

            case HEXAGONAL ->
                    "Hexagonal board dimensions are controlled by its radius.";

            case TRIANGULAR ->
                    "Triangular board dimensions are controlled by its side length.";
        };
    }


    private static void updateInfoTooltip(
            Label infoLabel,
            String text
    ) {

        Tooltip tooltip = (Tooltip) infoLabel.getProperties()
                .get("mazeInfoTooltip");

        if (tooltip == null) {

            tooltip = new Tooltip(text);

            infoLabel.getProperties().put(
                    "mazeInfoTooltip",
                    tooltip
            );

            Tooltip.install(
                    infoLabel,
                    tooltip
            );

        } else {

            tooltip.setText(text);
        }
    }


    private static Label createInfoIcon(String tooltipText) {

        Label infoLabel = new Label("ℹ");

        infoLabel.setStyle(
                "-fx-text-fill: #00bcd4; " +
                        "-fx-font-weight: bold; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 0 0 0 4;"
        );

        Tooltip tooltip = new Tooltip(tooltipText);

        infoLabel.getProperties().put(
                "mazeInfoTooltip",
                tooltip
        );

        Tooltip.install(
                infoLabel,
                tooltip
        );

        return infoLabel;
    }
}
