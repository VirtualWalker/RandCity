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

package fr.tjdev.randcity;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class NormalGameActivity extends Activity {

    private static final String TAG = "NormalGameActivity";

    private static final float MOVE_STEP = 5.0f;
    private static final float MOVE_STEP_Y = 5.0f;

    /**
     * Hold a reference to our GLSurfaceView
     */
    private GLView mGLView;
    private GLRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_normalgame);

        mGLView = (GLView) findViewById(R.id.gl_surface_view);

        if (hasOpenGLES20Support()) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLView.setEGLContextClientVersion(2);
            mGLView.setEGLConfigChooser(true);

            mRenderer = new GLRenderer(this);
            mGLView.setRenderer(mRenderer);

            // Render the view only when there is a change in the drawing data
            // Comment this if objects move without user interaction
            //mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        } else {
            // Here, the device doesn't support OpenGL ES 2.0.
            // It's the time to buy a new one !
            Log.wtf(TAG, "The device doesn't support OpenGL ES 2.0 !");
            finish();
            return;
        }

        // Handle movements buttons
        findViewById(R.id.button_forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    //Log.d(TAG, "Forward button clicked.");
                    mRenderer.eyeZ -= MOVE_STEP;
                    mRenderer.lookZ -= MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_backward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    //Log.d(TAG, "Backward button clicked.");
                    mRenderer.eyeZ += MOVE_STEP;
                    mRenderer.lookZ += MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    //Log.d(TAG, "Left button clicked.");
                    mRenderer.eyeX -= MOVE_STEP;
                    mRenderer.lookX -= MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    //Log.d(TAG, "Right button clicked.");
                    mRenderer.eyeX += MOVE_STEP;
                    mRenderer.lookX += MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    //Log.d(TAG, "Up button clicked.");
                    mRenderer.eyeY += MOVE_STEP_Y;
                }
            }
        });
        findViewById(R.id.button_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    //Log.d(TAG, "Down button clicked.");
                    mRenderer.eyeY -= MOVE_STEP_Y;
                    if (mRenderer.eyeY < 1.0f) {
                        Log.w(TAG, "Try to go under the floor. Block it !");
                        mRenderer.eyeY = 1.0f;
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGLView != null) {
            mGLView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGLView != null) {
            mGLView.onPause();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            mGLView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    // Check if the system supports OpenGL ES 2.0
    public boolean hasOpenGLES20Support() {
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    public TextView getFPSTextView() {
        return (TextView) findViewById(R.id.textViewFPS);
    }

    public TextView getBuildingInfoTextView() {
        return (TextView) findViewById(R.id.textViewBuildingInfo);
    }
}