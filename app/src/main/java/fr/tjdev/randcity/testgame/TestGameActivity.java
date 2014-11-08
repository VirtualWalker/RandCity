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
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import fr.tjdev.randcity.R;
import fr.tjdev.randcity.util.OpenGL;

public class TestGameActivity extends Activity {

    private static final String TAG = "TestGameActivity";

    private static final float MOVE_STEP = 10.0f;

    private GLSurfaceView mGLView;
    private GLRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_testgame);

        mGLView = (GLSurfaceView) findViewById(R.id.gl_surface_view);

        if (OpenGL.hasOpenGLES20Support(this)) {
            // Request an OpenGL ES 2.0 compatible context.
            mGLView.setEGLContextClientVersion(2);
            mGLView.setEGLConfigChooser(true);

            mRenderer = new GLRenderer(this);
            mGLView.setRenderer(mRenderer);

        } else {
            // Here, the device doesn't support OpenGL ES 2.0.
            // It's the time to buy a new one !
            Log.wtf(TAG, getString(R.string.noOpenGLSupport));
            finish();
            return;
        }

        // Set the first title for the fog button
        if(mRenderer.enableFog) {
            ((Button) findViewById(R.id.button_toggleFog)).setText(getText(R.string.fogDisable));
        } else {
            ((Button) findViewById(R.id.button_toggleFog)).setText(getText(R.string.fogEnable));
        }

        // Handle movements buttons
        findViewById(R.id.button_forward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    mRenderer.eyeZ -= MOVE_STEP;
                    mRenderer.lookZ -= MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_backward).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    mRenderer.eyeZ += MOVE_STEP;
                    mRenderer.lookZ += MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    mRenderer.eyeX -= MOVE_STEP;
                    mRenderer.lookX -= MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    mRenderer.eyeX += MOVE_STEP;
                    mRenderer.lookX += MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    mRenderer.eyeY += MOVE_STEP;
                    //mRenderer.lookY += MOVE_STEP;
                }
            }
        });
        findViewById(R.id.button_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    mRenderer.eyeY -= MOVE_STEP;
                    //mRenderer.lookY -= MOVE_STEP;
                    if (mRenderer.eyeY < 1.0f) {
                        Log.w(TAG, "Try to go under the floor. Block it !");
                        mRenderer.eyeY = 1.0f;
                    }
                }
            }
        });
        // Toggle the fog and the button title
        findViewById(R.id.button_toggleFog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRenderer != null) {
                    if(mRenderer.enableFog) {
                        mRenderer.enableFog = false;
                        ((Button) findViewById(R.id.button_toggleFog)).setText(getText(R.string.fogEnable));
                    } else {
                        mRenderer.enableFog = true;
                        ((Button) findViewById(R.id.button_toggleFog)).setText(getText(R.string.fogDisable));
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
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mGLView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            } else {
                mGLView.setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
    }

    public TextView getFPSTextView() {
        return (TextView) findViewById(R.id.textViewFPS);
    }

    public TextView getBuildingInfoTextView() {
        return (TextView) findViewById(R.id.textViewBuildingInfo);
    }
}