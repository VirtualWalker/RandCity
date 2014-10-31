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

package fr.tjdev.randcity.generation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Region;

import java.util.ArrayList;

import fr.tjdev.randcity.shapes.Cube;
import fr.tjdev.randcity.util.Random;

/**
 * This class represent a building (with some information)
 * Building objects are created with the static method generateBuilding()
 * VBOs are used to render buildings
 */
public class Building {

    // Position of normals
    static public final float[] normals = {
            // Front face
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f,

            // Right face
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,

            // Back face
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,
            0.0f, 0.0f, -1.0f,

            // Left face
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,
            -1.0f, 0.0f, 0.0f,

            // Top face
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 1.0f, 0.0f
    };
    // There is no texture coordinates for the top (all set to 0)
    public static final float[] textureCoordinates = {
            // Front face
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,

            // Right face
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,

            // Back face
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,

            // Left face
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,

            // Top face
            0.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 0.0f,
            0.0f, 0.0f,
    };

    // Contains positions of all vertex in the building
    public float[] positions;
    // Contains the color of the building
    public float[] color;
    // Contains the coordinates of the center of the building square.
    public float[] centerCoords;

    // Contains the type of texture used by this building.
    // Index are mapped as this :
    // 0 --> fuzzy windows
    // 1 --> "
    // 2 --> "
    // 3 --> "
    // 4 --> "
    // 5 --> linear windows
    // 6 --> "
    // 7 --> "
    // 8 --> "
    // 9 --> "
    //
    // Chance to have a specified texture are :
    // fuzzy  --> 70 %
    // linear --> 30 %
    //
    // In each category of texture, the chance to have one to another are the same
    public int textureType;

    static public Building generateBuilding(float topLeftX, float topLeftZ) {
        Building build = new Building();

        // Generate the center coordinates
        build.centerCoords = new float[]{
                topLeftX + GenUtil.HALF_BUILD_SQUARE_WIDTH,
                0.0f,
                topLeftZ + GenUtil.HALF_BUILD_SQUARE_WIDTH
        };

        // Generate a random height for the building.
        Random rand = new Random();
        final float height = (float) rand.intBetween(GenUtil.BUILD_MAX_HEIGHT, GenUtil.BUILD_MIN_HEIGHT);

        build.positions = Cube.generateCuboid(topLeftX + GenUtil.BUILD_MARGIN, 0.0f, topLeftZ + GenUtil.BUILD_MARGIN,
                GenUtil.BUILD_WIDTH, height, GenUtil.BUILD_WIDTH, false);

        // Generate a random color (a variant of grey)
        // Color are represented with values between 0.00f, and 1.00f
        // Generate an integer between 0 and 100 and divide by 100 to get the color.
        final int color = rand.intBetween(GenUtil.BUILD_MAX_COLOR, GenUtil.BUILD_MIN_COLOR);
        final int randPercent = rand.nextInt(100);
        int red = color;
        int green = color;
        int blue = color;

        // Possible colors are: red, blue, yellow, white
        if(randPercent < 25) { // blue
            blue += rand.nextInt(10);
            if(blue > 100) {
                blue = 100;
            }
        } else if (randPercent < 50) { // red
            red += rand.nextInt(10);
            if(red > 100) {
                red = 100;
            }
        } else if (randPercent < 75) { // yellow
            blue -= rand.nextInt(10);
            if(blue < 0) {
                blue = 0;
            }
        }

        build.color = new float[]{red/100.0f, green/100.0f, blue/100.0f, 1.0f};


        // Generate the texture type
        if(rand.chance(70)) {
            // Fuzzy
            build.textureType = rand.nextInt(5);
        } else {
            // Linear
            build.textureType = rand.nextInt(5) + 5;
        }

        return build;
    }

    /**
     * Generate a list of buildings (all buildings in the city grid)
     */
    static public ArrayList<Building> generateAllBuildings() {
        ArrayList<Building> builds = new ArrayList<Building>();
        final float firstPoint = GenUtil.HALF_ROAD_WIDTH + GenUtil.HALF_DIFF_BETWEEN_ROADS;

        for (float x = firstPoint;
             x <= GenUtil.HALF_GRID_SIZE;
             x += GenUtil.SPACE_BETWEEN_ROADS) {
            for (float z = firstPoint;
                 z <= GenUtil.HALF_GRID_SIZE;
                 z += GenUtil.SPACE_BETWEEN_ROADS) {
                // Bottom-right part of the city
                builds.add(generateBuilding(x, z));
                // Bottom-left part
                builds.add(generateBuilding(-x - GenUtil.BUILD_SQUARE_WIDTH, z));
                // Top-right part
                builds.add(generateBuilding(x, -z - GenUtil.BUILD_SQUARE_WIDTH));
                // Top-left part
                builds.add(generateBuilding(-x - GenUtil.BUILD_SQUARE_WIDTH,
                        -z - GenUtil.BUILD_SQUARE_WIDTH));
            }
        }

        return builds;
    }

    /**
     * The result bitmap is composed with some random squares with a grey nuance.
     */
    static public Bitmap generateFuzzyTexture() {
        Random rand = new Random();
        // Randomize width and height (+/- 4 windows)
        final int nbWinX = rand.moreOrLess(GenUtil.TEX_NB_WINDOW_X, 4);
        final int nbWinY = rand.moreOrLess(GenUtil.TEX_NB_WINDOW_Y, 4);

        // Randomize the percentage of chance to have a white glass
        final int chanceWhiteGlass = rand.moreOrLess(20, 10);
        // Randomize the percentage of chance to repeat the brightness
        final int chanceToRepeat = rand.moreOrLess(40, 20);

        // Create an empty, mutable bitmap
        Bitmap bitmap = Bitmap.createBitmap(nbWinX * GenUtil.TEX_WINDOW_WIDTH,
                nbWinY * GenUtil.TEX_WINDOW_HEIGHT, Bitmap.Config.ARGB_8888);
        // Get a canvas to paint over the bitmap
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0);

        // First pass : create white window
        for (int x = 0; x < nbWinX; ++x) {
            for (int y = 0; y < nbWinY; ++y) {
                final int left = x * GenUtil.TEX_WINDOW_WIDTH + GenUtil.TEX_WINDOW_H_BORDER;
                final int top = y * GenUtil.TEX_WINDOW_HEIGHT + GenUtil.TEX_WINDOW_V_BORDER;
                canvas.clipRect(new Rect(left, top, left + GenUtil.TEX_WINDOW_GLASS_WIDTH,
                        top + GenUtil.TEX_WINDOW_GLASS_HEIGHT), Region.Op.REPLACE);
                // Percentage of chance to have a white glass
                if (rand.chance(chanceWhiteGlass)) {
                    canvas.drawColor(GenUtil.WIN_BRIGHT_1);
                } else {
                    canvas.drawColor(GenUtil.WIN_DARK);
                }
            }
        }

        // Second pass : Create gradient on the sides of white windows
        for (int x2 = 0; x2 < nbWinX; ++x2) {
            for (int y2 = 0; y2 < nbWinY; ++y2) {
                final int left = x2 * GenUtil.TEX_WINDOW_WIDTH + GenUtil.TEX_WINDOW_H_BORDER;
                final int top = y2 * GenUtil.TEX_WINDOW_HEIGHT + GenUtil.TEX_WINDOW_V_BORDER;
                canvas.clipRect(new Rect(left, top, left + GenUtil.TEX_WINDOW_GLASS_WIDTH,
                        top + GenUtil.TEX_WINDOW_GLASS_HEIGHT), Region.Op.REPLACE);

                if (Color.blue(bitmap.getPixel(left, top)) != GenUtil.WIN_BRIGHT_1_RGB) {
                    // Check the brightness of next windows
                    final int nextBright = getHigherBrightInSideWindow(bitmap, left, top);
                    // There is a chance to repeat the light
                    if (rand.chance(chanceToRepeat)) {
                        if (nextBright == GenUtil.WIN_BRIGHT_1_RGB) { // White
                            canvas.drawColor(GenUtil.WIN_BRIGHT_2);
                        } else if (nextBright == GenUtil.WIN_BRIGHT_2_RGB) {
                            canvas.drawColor(GenUtil.WIN_BRIGHT_3);
                        } else if (nextBright == GenUtil.WIN_BRIGHT_3_RGB) {
                            canvas.drawColor(GenUtil.WIN_BRIGHT_4);
                        }
                    }
                }
            }
        }

        return bitmap;
    }

    static public Bitmap generateLinearTexture() {
        Random rand = new Random();
        // Randomize width and height (+/- 4 windows)
        final int nbWinX = rand.moreOrLess(GenUtil.TEX_NB_WINDOW_X, 4);
        final int nbWinY = rand.moreOrLess(GenUtil.TEX_NB_WINDOW_Y, 4);

        // Randomize some values
        final int spaceLength = rand.moreOrLess(11, 3);
        final int whiteGlassLength = rand.moreOrLess(14, 4);
        final int chanceToContinue = rand.moreOrLess(25, 5);
        final int chanceToStartRow = rand.moreOrLess(15, 5);

        Bitmap bitmap = Bitmap.createBitmap(nbWinX * GenUtil.TEX_WINDOW_WIDTH,
                nbWinY * GenUtil.TEX_WINDOW_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(0);

        // Used to know when there is a minimum of white glass.
        int whiteCount = 0;
        // Used to space the row with a minimum.
        // Set a large number since there is no space at the beginning (unlimited space in fact)
        int space = 100;

        // Create line of windows (inverse x and y in the loop)
        for (int y = 0; y < nbWinY; ++y) {
            for (int x = 0; x < nbWinX; ++x) {
                final int left = x * GenUtil.TEX_WINDOW_WIDTH + GenUtil.TEX_WINDOW_H_BORDER;
                final int top = y * GenUtil.TEX_WINDOW_HEIGHT + GenUtil.TEX_WINDOW_V_BORDER;
                canvas.clipRect(new Rect(left, top, left + GenUtil.TEX_WINDOW_GLASS_WIDTH,
                        top + GenUtil.TEX_WINDOW_GLASS_HEIGHT), Region.Op.REPLACE);

                // If we are not in a white row and there is a minimum space
                // and it's the chance to have a white glass
                if (whiteCount == 0 && space >= spaceLength && rand.chance(chanceToStartRow)) {
                    whiteCount++;
                    space = 0;
                    // Start the gradient
                    if (rand.chance(50)) {
                        canvas.drawColor(GenUtil.WIN_BRIGHT_4);
                    } else {
                        canvas.drawColor(GenUtil.WIN_BRIGHT_3);
                    }
                } else {
                    // Check the status of the row
                    if (whiteCount > 0 && whiteCount <= whiteGlassLength) {
                        if (whiteCount == 1) {
                            canvas.drawColor(GenUtil.WIN_BRIGHT_2);
                        } else {
                            canvas.drawColor(GenUtil.WIN_BRIGHT_1);
                        }
                        whiteCount++;
                    } else if (whiteCount > 0 && rand.chance(chanceToContinue)) {
                        // Here the row is finished but there is a chance to continue
                        canvas.drawColor(GenUtil.WIN_BRIGHT_1);
                        whiteCount++;
                    } else {
                        // The row is really finished, reset the counter
                        whiteCount = 0;
                        space++;
                        // Check to begin the gradient
                        if (space == 1) {
                            canvas.drawColor(GenUtil.WIN_BRIGHT_2);
                        } else if (space == 2) {
                            // Random gradient value
                            if (rand.chance(50)) {
                                canvas.drawColor(GenUtil.WIN_BRIGHT_3);
                            } else {
                                canvas.drawColor(GenUtil.WIN_BRIGHT_4);
                            }
                        } else {
                            // No need of gradient, set to dark
                            canvas.drawColor(GenUtil.WIN_DARK);
                        }
                    }
                }
            }
        }

        return bitmap;
    }

    // Return the higher value in windows on each side of the specified one
    static private int getHigherBrightInSideWindow(Bitmap bitmap, final int left, final int top) {
        int topVal = 0;
        int bottomVal = 0;
        int leftVal = 0;
        int rightVal = 0;

        if (top - GenUtil.TEX_WINDOW_HEIGHT > 0) {
            topVal = Color.blue(bitmap.getPixel(left, top - GenUtil.TEX_WINDOW_HEIGHT));
        }
        if (top + GenUtil.TEX_WINDOW_HEIGHT < bitmap.getHeight()) {
            bottomVal = Color.blue(bitmap.getPixel(left, top + GenUtil.TEX_WINDOW_HEIGHT));
        }
        if (left - GenUtil.TEX_WINDOW_WIDTH > 0) {
            leftVal = Color.blue(bitmap.getPixel(left - GenUtil.TEX_WINDOW_WIDTH, top));
        }
        if (left + GenUtil.TEX_WINDOW_WIDTH < bitmap.getWidth()) {
            rightVal = Color.blue(bitmap.getPixel(left + GenUtil.TEX_WINDOW_WIDTH, top));
        }

        // Return the max value between all window
        return Math.max(topVal, Math.max(bottomVal, Math.max(leftVal, rightVal)));
    }
}
