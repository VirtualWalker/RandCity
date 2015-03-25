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

package fr.tjdev.randcity.generation;

import android.graphics.RectF;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fr.tjdev.commonvrlibrary.util.FloorSurface;
import fr.tjdev.randcity.BuildConfig;

/**
 * Represent stairs. You can create an object using generate() method.
 * Note that the stairs (by default) go to the down, not the up.
 */
public class Stairs {
    static public final String TAG = "Stairs";

    public int verticesNumber;
    public float[] positionData;
    public float[] normals;

    // RGBA format
    static public float[] color = {
            0.5f, 0.5f, 0.5f, 1.0f
    };

    // Some information about the stairs
    public int stairsNumber;
    public float oneStairHeight;
    public float oneStairDepth;
    public float stairsWidth;

    // Contains all floor surfaces
    public List<FloorSurface> floorSurfaces;

    // Contains the area of the stairs (on the floor)
    // This value is not set by the generate() method, you must set it by hand
    public RectF area;

    // The area must be set by the user before
    public void generateFloorSurfaces() {
        floorSurfaces = new ArrayList<>();

        for (int i=0; i < stairsNumber ; ++i) {
            FloorSurface surface = new FloorSurface();
            surface.height = -1.0f * (float)i * oneStairHeight;
            surface.area = new RectF(area);
            surface.area.left = this.area.left + ((float)i * oneStairDepth);
            surface.area.right = surface.area.left + oneStairDepth;
            floorSurfaces.add(surface);
        }
    }

    /**
     * Generate a custom stair object.final float upY
     * @param numberOfStairs The number of stairs, must be a positive integer
     * @param oneStairHeight The height for one stair
     * @param oneStairDepth The depth for one stair
     * @param width The width of the stairs
     */
    static public Stairs generate(int numberOfStairs, float oneStairHeight, float oneStairDepth, float width)
    {
        if(numberOfStairs < 1 || oneStairDepth <= 0.0f || oneStairHeight <= 0.0f || width <= 0.0f) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Bad arguments !");
            }
            return new Stairs();
        }

        Stairs stairs = new Stairs();

        // Resize the buffers
        stairs.verticesNumber = numberOfStairs*12;
        stairs.positionData = new float[stairs.verticesNumber*3];
        stairs.normals = new float[stairs.verticesNumber*3];

        stairs.stairsWidth = width;
        stairs.oneStairDepth = oneStairDepth;
        stairs.oneStairHeight = oneStairHeight;
        stairs.stairsNumber = numberOfStairs;

        final float halfWidth = width/2.0f;

        final float[] upNormalsArray = {
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        };

        final float[] sideNormalsArray = {
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f
        };

        // Now fill the arrays for each stair
        for(int i=0; i < numberOfStairs ; ++i) {
            final float upY = -1.0f * (float)i * oneStairHeight;
            final float downY = upY - oneStairHeight;
            final float startDepth = (float)i * oneStairDepth;
            final float endDepth = startDepth + oneStairDepth;

            // Generate the "up" face
            final float[] upPosArray = {
                    startDepth, upY, -halfWidth,
                    startDepth, upY, halfWidth,
                    endDepth, upY, halfWidth,
                    endDepth, upY, halfWidth,
                    endDepth, upY, -halfWidth,
                    startDepth, upY, -halfWidth
            };

            // Generate the "side" face
            final float[] sidePosArray = {
                    endDepth, upY, halfWidth,
                    endDepth, downY, halfWidth,
                    endDepth, upY, -halfWidth,
                    endDepth, downY, halfWidth,
                    endDepth, downY, -halfWidth,
                    endDepth, upY, -halfWidth
            };

            // Copy all pos in the real array
            for(int j=0; j < 18 ; ++j) {
                stairs.positionData[i*36 + j] = upPosArray[j];
                stairs.positionData[i*36 + j + 18] = sidePosArray[j];
                stairs.normals[i*36 + j] = upNormalsArray[j];
                stairs.normals[i*36 + j + 18] = sideNormalsArray[j];
            }
        }

        return stairs;
    }
}
