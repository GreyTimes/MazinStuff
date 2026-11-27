package com.greytimes.mazinstuff.solver;

import com.greytimes.mazinstuff.model.grid.Cell;
import com.greytimes.mazinstuff.model.grid.MazeGrid;

import java.util.List;

/**
 * Common interface for maze-solving algorithms.
 */
public interface MazeSolver {

    /**
     * Finds a path from the maze entrance to its exit.
     *
     * @param grid maze to solve
     * @return ordered path, or null if no valid path exists
     */
    List<Cell> solve(MazeGrid grid);

    /**
     * Returns the number of unique cells processed during
     * the most recent solve operation.
     *
     * Includes the entrance and the exit when processed.
     *
     * @return number of processed cells
     */
    int getNodesVisited();
}
