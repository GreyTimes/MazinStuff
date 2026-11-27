
package com.greytimes.mazinstuff.solver;

import com.greytimes.mazinstuff.generator.DfsMazeGenerator;
import com.greytimes.mazinstuff.generator.MazeGenerator;
import com.greytimes.mazinstuff.model.configuration.EntranceExitMode;
import com.greytimes.mazinstuff.model.grid.Cell;
import com.greytimes.mazinstuff.model.grid.MazeGrid;
import com.greytimes.mazinstuff.model.topology.TopologyType;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MazeSolverTest {

    @Test
    void bfsCountsProcessedNodesAndResetsCounter() {
        assertSolverCountsNodes(new BfsMazeSolver());
    }

    @Test
    void dfsCountsProcessedNodesAndResetsCounter() {
        assertSolverCountsNodes(new DfsMazeSolver());
    }

    @Test
    void dijkstraCountsProcessedNodesAndResetsCounter() {
        assertSolverCountsNodes(new DijkstraMazeSolver());
    }

    private void assertSolverCountsNodes(MazeSolver solver) {
        MazeGrid grid = createTestMaze();

        List<Cell> path = solver.solve(grid);

        // A valid path must be found.
        assertNotNull(path);
        assertFalse(path.isEmpty());

        // Every cell in the path must have been processed.
        assertTrue(solver.getNodesVisited() >= path.size());

        // A cell must never be counted more than once.
        assertTrue(solver.getNodesVisited() <= grid.getCellCount());

        // A new solve operation must reset the counter.
        solver.solve(null);

        assertEquals(0, solver.getNodesVisited());
    }

    private MazeGrid createTestMaze() {
        Random random = new Random(42L);

        MazeGrid grid = new MazeGrid(
                4,
                4,
                TopologyType.RECTANGULAR
        );

        MazeGenerator generator = new DfsMazeGenerator();
        generator.initialize(grid, random);

        while (generator.step()) {
            // Complete maze generation.
        }

        grid.createOpenings(
                EntranceExitMode.LEFT_TO_RIGHT,
                random
        );

        return grid;
    }
}
