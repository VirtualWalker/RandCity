/*
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

package fr.tjdev.commonvrlibrary.shapes;

/**
 * Contains some constant variables used by all shapes
 */
public interface IShape {

    /**
     * How many bytes per float.
     */
    static public final int BYTES_PER_FLOAT = 4;
    /**
     * Number of elements per vertex position.
     */
    static public final int VERTEX_DATA_ELEMENTS = 3;
    /**
     * Number of elements per normal data.
     */
    static public final int NORMAL_DATA_ELEMENTS = 3;
    /**
     * Number of elements per texture coordinate.
     */
    static public final int TEXTURE_COORDINATE_ELEMENTS = 2;

    //
    // All class that implements this interface should create variables above.
    //

    // X, Y, Z
    // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle,
    // if the points are counter-clockwise we are looking at the "front". If not we are looking at
    // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
    // usually represent the backside of an object and aren't visible anyways.
    //
    // static public final float[] positionData = {};

    // R, G, B, A
    // Set to white if you want to texture the shape.
    //
    // static public final float[] colorData = {};

    // X, Y, Z
    // The normal is used in light calculations and is a vector which points
    // orthogonal to the plane of the surface. For a cube model, the normals
    // should be orthogonal to the points of each face.
    //
    // static public final float[] normalsData = {};

    // S, T (or X, Y)
    // Texture coordinate data.
    // Because images have a Y axis pointing downward (values increase as you move down the image) while
    // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
    // What's more is that the texture coordinates are the same for every face.
    //
    // public static final float[] textureCoordinatesData = {};
}
