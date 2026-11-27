package com.greytimes.mazinstuff.generator;

import com.greytimes.mazinstuff.model.grid.Cell;
import com.greytimes.mazinstuff.model.grid.MazeGrid;
import java.util.Random;

public interface MazeGenerator {
    void initialize(MazeGrid grid, Random random);

    boolean step();

    Cell getCurrentHead();
}