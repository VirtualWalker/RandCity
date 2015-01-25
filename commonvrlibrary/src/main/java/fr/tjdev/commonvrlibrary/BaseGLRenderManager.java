/*
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

package fr.tjdev.commonvrlibrary;

import android.opengl.GLES20;
import android.opengl.Matrix;

import fr.tjdev.commonvrlibrary.shapes.IShape;

/**
 * Contains some elements used by all OpenGL renderer.
 */
public class BaseGLRenderManager {

    // Store different strides used in VBOs buffers
    protected static final int mVBOStride = (IShape.VERTEX_DATA_ELEMENTS + IShape.NORMAL_DATA_ELEMENTS + IShape.TEXTURE_COORDINATE_ELEMENTS)
            * IShape.BYTES_PER_FLOAT;
    protected static final int mVBOStrideNoTex = (IShape.VERTEX_DATA_ELEMENTS + IShape.NORMAL_DATA_ELEMENTS)
            * IShape.BYTES_PER_FLOAT;

    // Store the offset of normals and texture coordinates in VBO buffers
    protected static final int mVBOTextureOffset = mVBOStrideNoTex;
    protected static final int mVBONormalOffset = IShape.VERTEX_DATA_ELEMENTS * IShape.BYTES_PER_FLOAT;

    // Store the model matrix. This matrix is used to move models from object space (where each model can be thought
    // of being located at the center of the universe) to world space.
    protected float[] mModelMatrix = new float[16];
    // Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
    // it positions things relative to our eye.
    protected float[] mViewMatrix = new float[16];
    // Store the projection matrix. This is used to project the scene onto a 2D viewport.
    protected float[] mProjectionMatrix = new float[16];
    // Store the model-view matrix
    protected float[] mMVMatrix = new float[16];
    // Allocate storage for the final combined matrix. This will be passed into the shader program.
    protected float[] mMVPMatrix = new float[16];

    protected int mProgramHandle;

    // Used in the setLookAt function.
    // Position the eye.
    public volatile float eyeX = 0.0f;
    public volatile float eyeY = 10.0f;
    public volatile float eyeZ = 0.0f;
    // We are looking toward this point
    public volatile float lookX = 0.0f;
    public volatile float lookY = eyeY;
    public volatile float lookZ = eyeZ - 1.0f; // Just look at the front
    // Set up vector.
    public volatile float upX = 0.0f;
    public volatile float upY = 1.0f;
    public volatile float upZ = 0.0f;

    // Use the common program
    protected void useProgram() {
        GLES20.glUseProgram(mProgramHandle);
    }

    // Set the view matrix
    protected void setLookAt() {
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
    }

    // Clear all buffers, called at the beginning of each rendering
    protected void clearGLBuffers() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClearDepthf(1.0f);
    }
}
