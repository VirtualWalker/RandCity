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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Region;

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
            -GenUtil.HALF_ROAD_WIDTH, 0.0f, -GenUtil.HALF_GRID_SIZE,
            -GenUtil.HALF_ROAD_WIDTH, 0.0f, GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_ROAD_WIDTH, 0.0f, -GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_ROAD_WIDTH, 0.0f, -GenUtil.HALF_GRID_SIZE,
            -GenUtil.HALF_ROAD_WIDTH, 0.0f, GenUtil.HALF_GRID_SIZE,
            GenUtil.HALF_ROAD_WIDTH, 0.0f, GenUtil.HALF_GRID_SIZE
    };

    // R, G, B, A
    static public final float[] colorData = {
            //0.2f, 0.2f, 0.2f, 1.0f
            1.0f, 1.0f, 1.0f, 1.0f
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

    // Generate a texture for the road
    static public Bitmap generateTexture() {
        final int width = (int)GenUtil.ROAD_WIDTH * 10;
        final int height = (int)GenUtil.GRID_SIZE;

        final int lineWidth = 16;
        final int lineColor = Color.rgb(160, 160, 160);
        final int backColor = Color.rgb(30, 30, 30);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(backColor);

        // Left line
        canvas.clipRect(new Rect(0, 0, width/lineWidth, height), Region.Op.REPLACE);
        canvas.drawColor(lineColor);

        // Right line
        canvas.clipRect(new Rect(width - (width/lineWidth), 0, width, height), Region.Op.REPLACE);
        canvas.drawColor(lineColor);

        // Middle line (discontinued)
        final int middleLeft = (width/2) - (width/(lineWidth*2));
        final int middleRight = (width/2) + (width/(lineWidth*2));

        canvas.clipRect(new Rect(middleLeft, 0, middleRight, height), Region.Op.REPLACE);
        canvas.drawColor(lineColor);

        // Create the discontinuation
        for (int i = 0;
             i <= GenUtil.GRID_SIZE;
             i += GenUtil.SPACE_BETWEEN_ROADS) {

            // Check for the main road
            if(i == GenUtil.HALF_GRID_SIZE) {
                i += GenUtil.MAIN_ROAD_WIDTH - GenUtil.ROAD_WIDTH;
            }

            // Middle
            canvas.clipRect(new Rect(middleLeft,
                    i + (int)((GenUtil.SPACE_BETWEEN_ROADS - GenUtil.ROAD_WIDTH)/2) - 2,
                    middleRight,
                    i + (int)((GenUtil.SPACE_BETWEEN_ROADS - GenUtil.ROAD_WIDTH)/2) + 2), Region.Op.REPLACE);
            // Half middle
            canvas.clipRect(new Rect(middleLeft,
                    i + (int)((GenUtil.SPACE_BETWEEN_ROADS - GenUtil.ROAD_WIDTH)/4) - 2,
                    middleRight,
                    i + (int)((GenUtil.SPACE_BETWEEN_ROADS - GenUtil.ROAD_WIDTH)/4) + 2), Region.Op.UNION);
            // Other half middle
            canvas.clipRect(new Rect(middleLeft,
                    i + (int)(((GenUtil.SPACE_BETWEEN_ROADS - GenUtil.ROAD_WIDTH)/4)*3) - 2,
                    middleRight,
                    i + (int)(((GenUtil.SPACE_BETWEEN_ROADS - GenUtil.ROAD_WIDTH)/4)*3) + 2), Region.Op.UNION);

            canvas.drawColor(backColor);
        }

        // Remove lines on intersections
        for (int i = (int)(GenUtil.SPACE_BETWEEN_ROADS - GenUtil.ROAD_WIDTH);
             i <= GenUtil.GRID_SIZE;
             i += GenUtil.SPACE_BETWEEN_ROADS) {
            // The middle road is larger, check it
            if(i + GenUtil.ROAD_WIDTH == GenUtil.HALF_GRID_SIZE) {
                canvas.clipRect(new Rect(0, i, width, i + (int)GenUtil.MAIN_ROAD_WIDTH), Region.Op.REPLACE);
                i += GenUtil.MAIN_ROAD_WIDTH - GenUtil.ROAD_WIDTH;
            } else {
                canvas.clipRect(new Rect(0, i, width, i + (int)GenUtil.ROAD_WIDTH), Region.Op.REPLACE);
            }
            canvas.drawColor(backColor);
        }

        return bitmap;
    }
}
