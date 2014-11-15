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

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.tjdev.commonvrlibrary.activities.FullScreenActivity;
import fr.tjdev.commonvrlibrary.util.OpenGLCheck;
import fr.tjdev.randcity.R;

public class TestGameActivity extends FullScreenActivity {

    private static final String TAG = "TestGameActivity";

    private static final float MOVE_STEP = 10.0f;

    private GLSurfaceView mGLView;
    private GLRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_testgame);

        mGLView = (GLSurfaceView) findViewById(R.id.gl_surface_view);

        if (OpenGLCheck.hasOpenGLES20Support(this)) {
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

    public TextView getFPSTextView() {
        return (TextView) findViewById(R.id.textViewFPS);
    }

    public TextView getBuildingInfoTextView() {
        return (TextView) findViewById(R.id.textViewBuildingInfo);
    }
}