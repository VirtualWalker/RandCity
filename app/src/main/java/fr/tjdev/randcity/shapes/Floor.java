/*
 * This file is part of RandCity.
 * Copyright (c) 2014 Fabien Caylus <toutjuste13@gmail.com>
 *
 * This file is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This file is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.tjdev.randcity.shapes;

import fr.tjdev.randcity.generation.GenUtil;

public class Floor implements IShape {

    // X, Y, Z
    // Mapped as :
    //   0----2,3
    //   | \  |
    //   |  \ |
    //   1,4--5
    // With :
    //   -Z
    //   /\
    //   ||--> +X
    static public final float[] positionData = {
            -GenUtil.HALF_GRID_SIZE, 0.0f, -GenUtil.HALF_GRID_SIZE,
            -GenUtil.HALF_GRID_SIZE, 0.0f, GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_GRID_SIZE, 0.0f, -GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_GRID_SIZE, 0.0f, -GenUtil.HALF_GRID_SIZE,
            -GenUtil.HALF_GRID_SIZE, 0.0f, GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_GRID_SIZE, 0.0f, GenUtil.HALF_GRID_SIZE
    };

    // R, G, B, A
    static public final float[] colorData = {
            1.0f, 1.0f, 1.0f, 1.0f
    };

    // X, Y, Z
    static public final float[] normalsData = {
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
    };

    // S, T (or X, Y)
    // Texture coordinate data.
    public static final float[] textureCoordinatesData = {
            // Front face
            0.0f, 0.0f,
            0.0f, GenUtil.GRID_SIZE,
            GenUtil.GRID_SIZE, 0.0f,
            0.0f, GenUtil.GRID_SIZE,
            GenUtil.GRID_SIZE, GenUtil.GRID_SIZE,
            GenUtil.GRID_SIZE, 0.0f,
    };
}
