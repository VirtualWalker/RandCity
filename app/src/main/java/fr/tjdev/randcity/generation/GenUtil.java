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

import android.graphics.Color;

/**
 * Contains some constants useful for generation.
 */
public class GenUtil {

    static public final float GRID_SIZE = 1024.0f;
    static public final float HALF_GRID_SIZE = GRID_SIZE / 2.0f;

    static public final float SPACE_BETWEEN_ROADS = 64.0f;

    static public final float ROAD_WIDTH = 16.0f;
    static public final float HALF_ROAD_WIDTH = ROAD_WIDTH / 2.0f;
    static public final float MAIN_ROAD_SCALE = 2.0f;
    static public final float MAIN_ROAD_WIDTH = ROAD_WIDTH * MAIN_ROAD_SCALE;
    static public final float HALF_DIFF_BETWEEN_ROADS = (MAIN_ROAD_WIDTH - ROAD_WIDTH) / 2.0f;

    static public final int BUILD_MIN_HEIGHT = 80;
    static public final int BUILD_MAX_HEIGHT = 200;
    static public final int AVERAGE_BUILD_HEIGHT = (BUILD_MIN_HEIGHT + BUILD_MAX_HEIGHT) / 2;

    static public final int BUILD_MIN_COLOR = 50;
    static public final int BUILD_MAX_COLOR = 90;

    // The square for buildings is smaller than the space between roads since this space
    // is computed from the middle of the road.
    // So, subtract 2 * the half road width.
    static public final float BUILD_SQUARE_WIDTH = SPACE_BETWEEN_ROADS - ROAD_WIDTH;
    static public final float HALF_BUILD_SQUARE_WIDTH = BUILD_SQUARE_WIDTH / 2.0f;

    // There is a margin on each side of the building (we can see the grass at the bottom)
    static public final float BUILD_MARGIN = 0.0f;
    static public final float BUILD_WIDTH = BUILD_SQUARE_WIDTH - (BUILD_MARGIN * 2.0f);

    // Width and height for windows
    static public final int TEX_WINDOW_WIDTH = 8;
    static public final int TEX_WINDOW_HEIGHT = 12;

    // Border of the windows on each side
    // H -> Horizontal -> Left and right borders
    // V -> Vertical -> Top and Bottom borders
    static public final int TEX_WINDOW_H_BORDER = 1;
    static public final int TEX_WINDOW_V_BORDER = 1;

    // Represent the real size of the window (without border)
    // In fact, it's the size of the glass (the white part)
    static public final int TEX_WINDOW_GLASS_WIDTH = TEX_WINDOW_WIDTH - (TEX_WINDOW_H_BORDER * 2);
    static public final int TEX_WINDOW_GLASS_HEIGHT = TEX_WINDOW_HEIGHT - (TEX_WINDOW_V_BORDER * 2);

    // Used in texture generation to determine the size of bitmaps
    static public final int TEX_NB_WINDOW_X = (int)(BUILD_WIDTH) / 4;
    static public final int TEX_NB_WINDOW_Y = AVERAGE_BUILD_HEIGHT / 6;

    // The numbers of textures generated
    static public final int TEX_TYPES_NB = 16;

    // Colors used for windows
    static public final int WIN_BRIGHT_1_RGB = 255;
    static public final int WIN_BRIGHT_2_RGB = 150;
    static public final int WIN_BRIGHT_3_RGB = 100;
    static public final int WIN_BRIGHT_4_RGB = 50;
    static public final int WIN_DARK_RGB = 20;

    // Generated colors used by canvas
    static public final int WIN_BRIGHT_1 = Color.rgb(WIN_BRIGHT_1_RGB, WIN_BRIGHT_1_RGB, WIN_BRIGHT_1_RGB);
    static public final int WIN_BRIGHT_2 = Color.rgb(WIN_BRIGHT_2_RGB, WIN_BRIGHT_2_RGB, WIN_BRIGHT_2_RGB);
    static public final int WIN_BRIGHT_3 = Color.rgb(WIN_BRIGHT_3_RGB, WIN_BRIGHT_3_RGB, WIN_BRIGHT_3_RGB);
    static public final int WIN_BRIGHT_4 = Color.rgb(WIN_BRIGHT_4_RGB, WIN_BRIGHT_4_RGB, WIN_BRIGHT_4_RGB);
    static public final int WIN_DARK = Color.rgb(WIN_DARK_RGB, WIN_DARK_RGB, WIN_DARK_RGB);

}
