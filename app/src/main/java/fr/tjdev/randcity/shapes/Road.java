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

public class Road extends Floor {

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
            -GenUtil.HALF_ROAD_WIDTH, GenUtil.ROAD_HEIGHT, -GenUtil.HALF_GRID_SIZE,
            -GenUtil.HALF_ROAD_WIDTH, GenUtil.ROAD_HEIGHT, GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_ROAD_WIDTH, GenUtil.ROAD_HEIGHT, -GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_ROAD_WIDTH, GenUtil.ROAD_HEIGHT, -GenUtil.HALF_GRID_SIZE,
            -GenUtil.HALF_ROAD_WIDTH, GenUtil.ROAD_HEIGHT, GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_ROAD_WIDTH, GenUtil.ROAD_HEIGHT, GenUtil.HALF_GRID_SIZE
    };

    // R, G, B, A
    static public final float[] colorData = {
            0.2f, 0.2f, 0.2f, 1.0f
    };
}
