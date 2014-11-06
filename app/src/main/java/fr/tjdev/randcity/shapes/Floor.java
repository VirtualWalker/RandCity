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

public class Floor implements IShape {

    static public final float[] positionData = {
            -1.0f, 0.0f, -1.0f,
            -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, -1.0f,
            1.0f, 0.0f, -1.0f,
            -1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 1.0f
    };

    // R, G, B, A
    static public final float[] colorData = {
            0.3f, 0.3f, 0.3f, 1.0f
    };

    // X, Y, Z
    static public final float[] normalsData = {
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f
    };

    // S, T (or X, Y)
    // Texture coordinate data.
    public static final float[] textureCoordinatesData = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };
}
