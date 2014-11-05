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

package fr.tjdev.randcity.testgame;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import fr.tjdev.randcity.BaseGLRenderer;
import fr.tjdev.randcity.generation.Building;
import fr.tjdev.randcity.generation.GenUtil;
import fr.tjdev.randcity.shapes.Cube;
import fr.tjdev.randcity.shapes.IShape;
import fr.tjdev.randcity.shapes.Road;
import fr.tjdev.randcity.shapes.SkyBox;
import fr.tjdev.randcity.shapes.Floor;
import fr.tjdev.randcity.util.BufferHelper;
import fr.tjdev.randcity.util.RawResourceReader;
import fr.tjdev.randcity.util.ShaderHelper;
import fr.tjdev.randcity.util.TextureHelper;
import fr.tjdev.randcity.util.Random;

/**
 * Renderer used for the test game.
 */
public class GLRenderer extends BaseGLRenderer implements GLSurfaceView.Renderer {

    // Store the FPS and the last time to compute the fps
    private int mFPS = 0;
    private long mLastTime = 0;
    // Used to display fps
    private TextView mFPSView;

    public GLRenderer(final Context activityContext) {
        super(activityContext);

        mFPSView = ((TestGameActivity) mActivityContext).getFPSTextView();

        // Update the buildings information on the screen
        String info = "Buildings: " + Integer.toString(mBuildings.size());
        int texTypes[] = new int[mBuildTextureBitmaps.length];
        for (Building build : mBuildings) {
            texTypes[build.textureType]++;
        }
        info += "\nFuzzy tex:  " + Integer.toString(texTypes[0]);
        info += "/" + Integer.toString(texTypes[1]);
        info += "/" + Integer.toString(texTypes[2]);
        info += "/" + Integer.toString(texTypes[3]);
        info += "/" + Integer.toString(texTypes[4]);
        info += "/" + Integer.toString(texTypes[5]);
        info += "/" + Integer.toString(texTypes[6]);
        info += "/" + Integer.toString(texTypes[7]);
        info += "\nLinear tex: " + Integer.toString(texTypes[8]);
        info += "/" + Integer.toString(texTypes[9]);
        info += "/" + Integer.toString(texTypes[10]);
        info += "/" + Integer.toString(texTypes[11]);
        info += "/" + Integer.toString(texTypes[12]);
        info += "/" + Integer.toString(texTypes[13]);
        info += "/" + Integer.toString(texTypes[14]);
        info += "/" + Integer.toString(texTypes[15]);

        final String infoDisplayed = info;
        ((Activity) mActivityContext).runOnUiThread(new Runnable() {

            @Override
            public void run() {
                ((TestGameActivity) mActivityContext).getBuildingInfoTextView().setText(infoDisplayed);
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        super.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        super.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Compute the fps
        final long currentTime = System.currentTimeMillis();
        final long diffTime = currentTime - mLastTime;
        mFPS++;
        if (diffTime >= 1000) {
            // Create a copy for correct display
            final int tempFPS = mFPS;
            ((Activity) mActivityContext).runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mFPSView.setText("FPS: " + Integer.toString(tempFPS));
                }
            });
            mLastTime = currentTime;
            mFPS = 0;
        }

        super.onDrawFrame();
    }
}
