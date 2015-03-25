/*
 * This file is part of RandCity.
 * Copyright (c) 2015 Fabien Caylus <toutjuste13@gmail.com>
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

import android.graphics.Bitmap;
import android.graphics.Color;

import fr.tjdev.commonvrlibrary.shapes.IShape;
import fr.tjdev.randcity.generation.GenUtil;

// Represent the corridor with the treasure at the end.
public class TreasureCorridor implements IShape {

    static private final float halfWidth = GenUtil.SPACE_BETWEEN_ROADS_X2;
    static private final float halfDepth = GenUtil.HALF_BUILD_SQUARE_WIDTH;

    static public final float CORRIDOR_LENGTH = halfWidth * 2.0f + GenUtil.BUILD_SQUARE_WIDTH;
    static public final float CORRIDOR_HEIGHT = halfDepth * 2.0f;
    
    static public final float[] positionData = {
            // Back wall
            -halfWidth - GenUtil.BUILD_SQUARE_WIDTH, halfDepth - 0.1f, -halfDepth,
            halfWidth, -halfDepth, -halfDepth,
            halfWidth, halfDepth - 0.1f, -halfDepth,
            -halfWidth - GenUtil.BUILD_SQUARE_WIDTH, halfDepth - 0.1f, -halfDepth,
            -halfWidth - GenUtil.BUILD_SQUARE_WIDTH, -halfDepth, -halfDepth,
            halfWidth, -halfDepth, -halfDepth,

            // Front wall
            halfWidth, halfDepth - 0.1f, halfDepth,
            -halfWidth  - GenUtil.BUILD_SQUARE_WIDTH, -halfDepth, halfDepth,
            -halfWidth  - GenUtil.BUILD_SQUARE_WIDTH, halfDepth - 0.1f, halfDepth,
            halfWidth, halfDepth - 0.1f, halfDepth,
            halfWidth, -halfDepth, halfDepth,
            -halfWidth  - GenUtil.BUILD_SQUARE_WIDTH, -halfDepth, halfDepth,

            // Right Wall
            halfWidth, halfDepth - 0.1f, -halfDepth,
            halfWidth, -halfDepth, halfDepth,
            halfWidth, halfDepth - 0.1f, halfDepth,
            halfWidth, halfDepth - 0.1f, -halfDepth,
            halfWidth, -halfDepth, -halfDepth,
            halfWidth, -halfDepth, halfDepth,

            // Bottom
            -halfWidth, -halfDepth, -halfDepth,
            halfWidth, -halfDepth, halfDepth,
            halfWidth, -halfDepth, -halfDepth,
            -halfWidth, -halfDepth, -halfDepth,
            -halfWidth, -halfDepth, halfDepth,
            halfWidth, -halfDepth, halfDepth,

            // Top
            halfWidth, halfDepth - 2.0f, -halfDepth,
            -halfWidth, halfDepth - 2.0f, halfDepth,
            -halfWidth, halfDepth - 2.0f, -halfDepth,
            halfWidth, halfDepth - 2.0f, -halfDepth,
            halfWidth, halfDepth - 2.0f, halfDepth,
            -halfWidth, halfDepth - 2.0f, halfDepth
    };

    // X, Y, Z
    static public final float[] normalsData = {
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,

            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,

            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,

            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,

            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f,
            0.0f, -1.0f, 0.0f
    };

    // S, T
    // Texture coordinate data.
    public static final float[] textureCoordinatesData = {
            // Back
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,

            // Front
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,

            // Right
            1.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 1.0f,

            //Bottom
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,

            // Top
            1.0f, 1.0f,
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f
    };

    static public final float[] colorData = {
            1.0f, 1.0f, 1.0f, 1.0f
    };

    // Generate a simple gradient texture
    static public Bitmap generateTexture() {
        Bitmap bitmap = Bitmap.createBitmap(256, 2, Bitmap.Config.ARGB_8888);

        for (int x = 255; x >= 0; --x) {
            bitmap.setPixel(255-x, 0, Color.rgb(x, x, x));
            bitmap.setPixel(255-x, 1, Color.rgb(x, x, x));
        }

        return bitmap;
    }
}
