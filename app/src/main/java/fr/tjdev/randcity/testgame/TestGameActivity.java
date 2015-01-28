/*
 * This file is part of RandCity.
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

package fr.tjdev.randcity.testgame;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.MotionEvent;
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

    private abstract class CustomOnTouchListener implements View.OnTouchListener {
        // Call when a click is operate
        abstract void onClick(int viewID);

        private final Handler mmHandler = new Handler();
        private ArrayMap<Integer, Runnable> mmRunnables = new ArrayMap<>();

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Check if a runnable already exists
                    if (!mmRunnables.containsKey(view.getId())) {
                        // Add a new one
                        final int viewID = view.getId();
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                onClick(viewID);
                                // Re-run after 200 ms
                                mmHandler.postDelayed(this, 200);
                            }
                        };
                        mmRunnables.put(viewID, runnable);
                        mmHandler.post(runnable);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    // Remove and stop the running Handler
                    if (mmRunnables.containsKey(view.getId())) {
                        mmHandler.removeCallbacks(mmRunnables.get(view.getId()));
                        mmRunnables.remove(view.getId());
                    }
                    break;
            }
            return false;
        }
    }

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

        // Set the all button clicks events
        final CustomOnTouchListener touchListener = new CustomOnTouchListener() {
            @Override
            void onClick(int viewID) {
                if (mRenderer != null) {
                    switch (viewID) {
                        case R.id.button_forward:
                            mRenderer.movePlayer(0.0f, MOVE_STEP);
                            break;
                        case R.id.button_backward:
                            mRenderer.movePlayer(0.0f, -MOVE_STEP);
                            break;
                        case R.id.button_left:
                            mRenderer.movePlayer(-MOVE_STEP, 0.0f);
                            break;
                        case R.id.button_right:
                            mRenderer.movePlayer(MOVE_STEP, 0.0f);
                            break;
                        case R.id.button_up:
                            mRenderer.eyeY += MOVE_STEP;
                            break;
                        case R.id.button_down:
                            mRenderer.eyeY -= MOVE_STEP;
                            if (mRenderer.eyeY < 1.0f) {
                                Log.w(TAG, "Try to go under the floor. Block it !");
                                mRenderer.eyeY = 1.0f;
                            }
                            break;
                    }
                }
            }
        };

        // Handle movements buttons
        findViewById(R.id.button_forward).setOnTouchListener(touchListener);
        findViewById(R.id.button_backward).setOnTouchListener(touchListener);
        findViewById(R.id.button_left).setOnTouchListener(touchListener);
        findViewById(R.id.button_right).setOnTouchListener(touchListener);
        findViewById(R.id.button_up).setOnTouchListener(touchListener);
        findViewById(R.id.button_down).setOnTouchListener(touchListener);
        // Toggle the fog and the button title
        findViewById(R.id.button_toggleFog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mRenderer != null) {
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