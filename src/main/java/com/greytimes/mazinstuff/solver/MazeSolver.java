package com.greytimes.mazinstuff.solver;

import com.greytimes.mazinstuff.model.grid.Cell;
import com.greytimes.mazinstuff.model.grid.MazeGrid;

import java.util.List;

/**
 * Common interface for maze-solving algorithms.
 *
 * Implementations return an ordered path from entrance to exit.
 */
public interface MazeSolver {

    /**
     * Finds a path through the maze.
     *
     * @param grid maze to solve
     * @return ordered path, or null if no valid path can be found
     */
    List<Cell> solve(MazeGrid grid);
}