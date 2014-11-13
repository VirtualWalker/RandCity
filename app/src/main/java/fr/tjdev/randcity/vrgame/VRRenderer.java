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

package fr.tjdev.randcity.vrgame;

import android.content.Context;
import android.opengl.Matrix;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

import fr.tjdev.randcity.CommonGLRenderer;

/**
 * Renderer used for virtual reality game.
 */
public class VRRenderer extends CommonGLRenderer implements CardboardView.StereoRenderer {

    protected float[] mHeadView = new float[16];

    public VRRenderer(final Context activityContext) {
        super(activityContext);
    }

    @Override
    public void onRendererShutdown() {

    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        super.onSurfaceCreated();
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        computeLightMoveAngle();

        headTransform.getHeadView(mHeadView, 0);
    }

    /**
     * Draws a frame for an eye. The transformation for that eye (from the camera) is passed in as
     * a parameter.
     * @param transform The transformations to apply to render this eye.
     */
    @Override
    public void onDrawEye(EyeTransform transform) {
        clearGLBuffers();
        setLookAt();

        useProgram();
        loadUniforms();
        loadAttribs();

        float[] tempViewMatrix = new float[16];
        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(tempViewMatrix, 0, transform.getEyeView(), 0, mViewMatrix, 0);
        System.arraycopy(tempViewMatrix, 0, mViewMatrix, 0, 16);

        checkFog();

        updateLightMatrices();

        mProjectionMatrix = transform.getPerspective();

        draw();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }
}
