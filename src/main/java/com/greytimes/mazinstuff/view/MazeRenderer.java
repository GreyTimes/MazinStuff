package com.greytimes.mazinstuff.view;

import javafx.scene.canvas.GraphicsContext;

import com.greytimes.mazinstuff.model.grid.Cell;
import com.greytimes.mazinstuff.model.grid.MazeGrid;
import com.greytimes.mazinstuff.model.configuration.MazeThemeConfig;

import java.util.Collection;
import java.util.List;

public interface MazeRenderer {

    void render(
            GraphicsContext gc,
            double width,
            double height,
            MazeGrid grid,
            Cell playerCell,
            List<Cell> solvedPath,
            Cell currentHead,
            boolean fogOfWarActive,
            int fogRadius
    );

    void setDarkMode(boolean darkMode);

    default void setHighlightedCells(Collection<Cell> cells) {
    }

    default void setHoveredCell(Cell cell) {
    }

    default void setPlayerScreenPosition(double x, double y) {
    }

    default void clearPlayerScreenPosition() {
    }

    default void setThemeConfig(MazeThemeConfig config) {
        if (config != null) {
            setDarkMode(config.isDarkMode());
        }
    }

    default Cell findCellAt(
            double x,
            double y,
            double width,
            double height,
            MazeGrid grid
    ) {
        return null;
    }
}
