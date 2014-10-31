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

package fr.tjdev.randcity.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import fr.tjdev.randcity.shapes.IShape;

public class BufferHelper {

    // Return a float buffer with arguments interleaved.
    // You can omit one of the input array (except the positions array) if you want, it will be skipped.
    // But don't pass "null" as argument, prefer the "new float[0]" form instead.
    // Example:
    //  input: Px1Py1Pz1 Px2Py2Pz2 | Nx1Ny1Nz1 Nx2Ny2Nz2 | S1T1 S2T2
    //  output: Px1Py1Pz1 Nx1Ny1Nz1 S1T1 Px2Py2Pz2 Nx2Ny2Nz2 S2T2
    static public FloatBuffer getInterleavedBuffer(float[] positions, float[] normals, float[] textureCoordinates) {
        final int dataLength = positions.length
                + normals.length
                + textureCoordinates.length;

        int positionOffset = 0;
        int normalOffset = 0;
        int textureOffset = 0;

        final FloatBuffer buffer = ByteBuffer.allocateDirect(dataLength * IShape.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();

        final int numberOfVertices = positions.length / IShape.VERTEX_DATA_ELEMENTS;

        for (int i = 0; i < numberOfVertices; i++) {
            // Add positions
            buffer.put(positions, positionOffset, IShape.VERTEX_DATA_ELEMENTS);
            positionOffset += IShape.VERTEX_DATA_ELEMENTS;
            // Add normals
            if (normalOffset + IShape.NORMAL_DATA_ELEMENTS <= normals.length) {
                buffer.put(normals, normalOffset, IShape.NORMAL_DATA_ELEMENTS);
                normalOffset += IShape.NORMAL_DATA_ELEMENTS;
            }
            // Add texture coordinates
            if (textureOffset + IShape.TEXTURE_COORDINATE_ELEMENTS <= textureCoordinates.length) {
                buffer.put(textureCoordinates, textureOffset, IShape.TEXTURE_COORDINATE_ELEMENTS);
                textureOffset += IShape.TEXTURE_COORDINATE_ELEMENTS;
            }
        }

        buffer.position(0);
        return buffer;
    }
}
