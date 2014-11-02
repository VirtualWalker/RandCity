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

public class GLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "GLRenderer";
    private final Context mActivityContext;

    // Store different strides used in VBOs buffers
    private static final int mVBOStride = (IShape.VERTEX_DATA_ELEMENTS + IShape.NORMAL_DATA_ELEMENTS + IShape.TEXTURE_COORDINATE_ELEMENTS)
            * IShape.BYTES_PER_FLOAT;
    private static final int mVBOStrideNoTex = (IShape.VERTEX_DATA_ELEMENTS + IShape.NORMAL_DATA_ELEMENTS)
            * IShape.BYTES_PER_FLOAT;

    // Store the offset of normals and texture coordinates in VBO buffers
    private static final int mVBOTextureOffset = mVBOStrideNoTex;
    private static final int mVBONormalOffset = IShape.VERTEX_DATA_ELEMENTS * IShape.BYTES_PER_FLOAT;

    private int mCubeVBOBuffer;
    private int mBrickTextureDataHandle;

    // Used to have a floor at the bottom of the treasure
    private int mFloorVBOBuffer;

    private int mSkyBoxVBOBuffer;

    private int mRoadVBOBuffer;
    private Bitmap mRoadTextureBitmap;
    private int mRoadTextureDataHandle;

    private ArrayList<Building> mBuildings;
    // Contains all buffers used for buildings
    private int[] mBuildVBOBuffers;
    // Store all different textures used by buildings
    private Bitmap mBuildTextureBitmaps[] = new Bitmap[GenUtil.TEX_TYPES_NB];
    private int mBuildTextureDataHandles[] = new int[GenUtil.TEX_TYPES_NB];

    // Store the model matrix. This matrix is used to move models from object space (where each model can be thought
    // of being located at the center of the universe) to world space.
    private float[] mModelMatrix = new float[16];
    // Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
    // it positions things relative to our eye.
    private float[] mViewMatrix = new float[16];
    // Store the projection matrix. This is used to project the scene onto a 2D viewport.
    private float[] mProjectionMatrix = new float[16];
    // Allocate storage for the final combined matrix. This will be passed into the shader program.
    private float[] mMVPMatrix = new float[16];
    // A temporary matrix.
    private float[] mTemporaryMatrix = new float[16];
    // Stores a copy of the model matrix specifically for the light position.
    private float[] mLightModelMatrix = new float[16];

    // Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
    // we multiply this by our transformation matrices.
    private final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    // Used to hold the current position of the light in world space (after transformation via model matrix).
    private final float[] mLightPosInWorldSpace = new float[4];
    // Used to hold the transformed position of the light in eye space (after transformation via modelview matrix)
    private final float[] mLightPosInEyeSpace = new float[4];

    // This will be used to pass in the transformation matrix.
    private int mMVPMatrixHandle;
    // This will be used to pass in the modelview matrix.
    private int mMVMatrixHandle;
    // This will be used to pass in the light position.
    private int mLightPosHandle;
    // This will be used to pass in the texture.
    private int mTextureUniformHandle;
    // This will be used to tell if we use a texture or not.
    private int mTextureFlagHandle;
    // This will be used to enable the fog
    private int mFogFlagHandle;
    // This will be used to pass in model position information.
    private int mPositionHandle;
    // This will be used to pass in model normal information.
    private int mNormalHandle;
    // This will be used to pass in model color information.
    private int mColorHandle;
    // This will be used to pass in model texture coordinate information.
    private int mTextureCoordinateHandle;
    // This is a handle to our cube shading program.
    private int mProgramHandle;
    // This is a handle to our light point program.
    private int mPointProgramHandle;

    // Store the FPS and the last time to compute the fps
    private int mFPS = 0;
    private long mLastTime = 0;
    // Used to display fps
    private TextView mFPSView;

    // Store the position of the "treasure".
    // In fact, the treasure is a special building that you must reach to end the game.
    private float[] mTreasurePos;

    // Position the eye in front of the origin.
    public volatile float eyeX = 0.0f;
    public volatile float eyeY = 10.0f;
    public volatile float eyeZ = 7.0f;
    // We are looking toward this point
    public volatile float lookX = 0.0f;
    public volatile float lookY = 5.0f;
    public volatile float lookZ = 0.0f;
    // Set our up vector. This is where our head would be pointing were we holding the camera.
    public volatile float upX = 0.0f;
    public volatile float upY = 1.0f;
    public volatile float upZ = 0.0f;

    // Used to toggle the fog
    public volatile boolean enableFog = true;


    public GLRenderer(final Context activityContext) {
        mActivityContext = activityContext;

        mFPSView = ((NormalGameActivity) mActivityContext).getFPSTextView();

        // Generate buildings grid
        generateTerrain();

        // Bitmaps are generated here.
        // Handles to these textures are generated in onSurfaceCreated() method.
        int i;
        for(i=0 ; i < 8 ; ++i) {
            mBuildTextureBitmaps[i] = Building.generateFuzzyTexture();
        }
        for(; i < 16 ; ++i) {
            mBuildTextureBitmaps[i] = Building.generateLinearTexture();
        }

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
                ((NormalGameActivity) mActivityContext).getBuildingInfoTextView().setText(infoDisplayed);
            }
        });

        // Generate road texture
        mRoadTextureBitmap = Road.generateTexture();
    }

    // This function will generate buildings.
    private void generateTerrain() {
        mBuildings = Building.generateAllBuildings();

        // Define the treasure pos
        // We replace a random building by the treasure, and get its positions
        Random rand = new Random();
        int randIndex = rand.nextInt(mBuildings.size());
        // Block the treasure from spawning on the sides of the city
        final float maxCenterCoords = GenUtil.HALF_GRID_SIZE - GenUtil.HALF_BUILD_SQUARE_WIDTH;
        while(mBuildings.get(randIndex).centerCoords[0] == maxCenterCoords ||
                mBuildings.get(randIndex).centerCoords[0] == -maxCenterCoords ||
                mBuildings.get(randIndex).centerCoords[2] == maxCenterCoords ||
                mBuildings.get(randIndex).centerCoords[2] == -maxCenterCoords) {
            randIndex = rand.nextInt(mBuildings.size());
        }

        mTreasurePos = mBuildings.get(randIndex).centerCoords;

        Log.d(TAG, "Treasure position set to :");
        Log.d(TAG, "x=" + Float.toString(mTreasurePos[0]) + " y=" + Float.toString(mTreasurePos[1]) + " z=" + Float.toString(mTreasurePos[2]));

        // Remove the building from the list.
        mBuildings.remove(randIndex);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        // Could make the rendering faster on some GPUs
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDepthMask(true);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Normal", "a_TexCoordinate"});

        // Define a simple shader program for our point.
        final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_vertex_shader);
        final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.point_fragment_shader);

        final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
        final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
        mPointProgramHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
                new String[]{"a_Position"});

        // Load the texture
        mBrickTextureDataHandle = TextureHelper.loadTexture(mActivityContext, R.drawable.stone_wall_public_domain);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        mRoadTextureDataHandle = TextureHelper.loadTexture(mRoadTextureBitmap, false);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        // Load buildings textures
        for(int i=0 ; i < mBuildTextureBitmaps.length ; ++i) {
            mBuildTextureDataHandles[i] = TextureHelper.loadTexture(mBuildTextureBitmaps[i], false);
            GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        }

        //
        // Generate all VBOs
        //

        //
        // Building VBOs
        mBuildVBOBuffers = new int[mBuildings.size()];
        GLES20.glGenBuffers(mBuildings.size(), mBuildVBOBuffers, 0);

        // Generate one VBO per building
        for (int i = 0; i < mBuildings.size(); i++) {
            FloatBuffer buildBuffer = BufferHelper.getInterleavedBuffer(mBuildings.get(i).positions,
                    Building.normals, Building.textureCoordinates);

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBuildVBOBuffers[i]);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buildBuffer.capacity() * IShape.BYTES_PER_FLOAT,
                    buildBuffer, GLES20.GL_STATIC_DRAW);

            buildBuffer.limit(0);
            buildBuffer = null;
        }

        //
        // Cube VBO
        FloatBuffer cubeBuffer = BufferHelper.getInterleavedBuffer(Cube.positionData,
                Cube.normalsData, Cube.textureCoordinatesData);

        final int cubeTempBuffer[] = new int[1];
        GLES20.glGenBuffers(1, cubeTempBuffer, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, cubeTempBuffer[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, cubeBuffer.capacity() * IShape.BYTES_PER_FLOAT,
                cubeBuffer, GLES20.GL_STATIC_DRAW);

        mCubeVBOBuffer = cubeTempBuffer[0];

        cubeBuffer.limit(0);
        cubeBuffer = null;

        //
        // Road VBO
        FloatBuffer roadBuffer = BufferHelper.getInterleavedBuffer(Road.positionData,
                Road.normalsData, Road.textureCoordinatesData);

        final int roadTempBuffers[] = new int[1];
        GLES20.glGenBuffers(1, roadTempBuffers, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, roadTempBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, roadBuffer.capacity() * IShape.BYTES_PER_FLOAT,
                roadBuffer, GLES20.GL_STATIC_DRAW);

        mRoadVBOBuffer = roadTempBuffers[0];

        roadBuffer.limit(0);
        roadBuffer = null;

        //
        // SkyBox VBO
        FloatBuffer skyBuffer = BufferHelper.getInterleavedBuffer(SkyBox.positionData,
                SkyBox.normalsData, new float[0]);

        final int skyTempBuffers[] = new int[1];
        GLES20.glGenBuffers(1, skyTempBuffers, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, skyTempBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, skyBuffer.capacity() * IShape.BYTES_PER_FLOAT,
                skyBuffer, GLES20.GL_STATIC_DRAW);

        mSkyBoxVBOBuffer = skyTempBuffers[0];

        skyBuffer.limit(0);
        skyBuffer = null;

        //
        // Floor VBO
        FloatBuffer floorBuffer = BufferHelper.getInterleavedBuffer(Floor.positionData,
                Floor.normalsData, new float[0]);


        final int floorTempBuffers[] = new int[1];
        GLES20.glGenBuffers(1, floorTempBuffers, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, floorTempBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, floorBuffer.capacity() * IShape.BYTES_PER_FLOAT,
                floorBuffer, GLES20.GL_STATIC_DRAW);

        mFloorVBOBuffer = floorTempBuffers[0];

        floorBuffer.limit(0);
        floorBuffer = null;

        // Finish the binding
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;

        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, 1.0f, 2000.0f);
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

        // Set the background clear color to black.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClearDepthf(1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);

        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mProgramHandle);

        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mTextureFlagHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_TextureFlag");
        mFogFlagHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_FogFlag");
        mColorHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Color");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        // Enable (or disable) the fog
        if(enableFog) {
            GLES20.glUniform1f(mFogFlagHandle, 1.0f);
        } else {
            GLES20.glUniform1f(mFogFlagHandle, 0.0f);
        }

        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, mTreasurePos[0], mTreasurePos[1], mTreasurePos[2]);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -2.0f);
        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 1.5f, 3.5f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);

        // Enable texture here
        GLES20.glUniform1f(mTextureFlagHandle, 1.0f);

        //
        // Draw cube
        //

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBrickTextureDataHandle);
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // Pass in the texture coordinate information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeVBOBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, IShape.TEXTURE_COORDINATE_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStride, mVBOTextureOffset);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 0.0f, 1.5f, 0.0f);
        Matrix.translateM(mModelMatrix, 0, mTreasurePos[0], mTreasurePos[1], mTreasurePos[2]);
        drawCube();

        //
        // Draw the roads
        //

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRoadTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // Pass in the position information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mRoadVBOBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, IShape.VERTEX_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStride, 0);

        // Pass in the normal information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mRoadVBOBuffer);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glVertexAttribPointer(mNormalHandle, IShape.NORMAL_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStride, mVBONormalOffset);

        // Pass in the texture information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mRoadVBOBuffer);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, IShape.TEXTURE_COORDINATE_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStride, mVBOTextureOffset);

        // Main roads (2x larger)
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.scaleM(mModelMatrix, 0, GenUtil.MAIN_ROAD_SCALE, 1.0f, 1.0f);
        drawRoad();

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.rotateM(mModelMatrix, 0, 90, 0.0f, 1.0f, 0.0f);
        Matrix.scaleM(mModelMatrix, 0, GenUtil.MAIN_ROAD_SCALE, 1.0f, 1.0f);
        drawRoad();

        // Other roads (1x larger)
        // Add the some units to the first road since the middle one is larger
        for (float i = GenUtil.SPACE_BETWEEN_ROADS + GenUtil.HALF_DIFF_BETWEEN_ROADS;
             i <= GenUtil.HALF_GRID_SIZE;
             i += GenUtil.SPACE_BETWEEN_ROADS) {
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, i, 0.0f, 0.0f);
            drawRoad();

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.translateM(mModelMatrix, 0, -i, 0.0f, 0.0f);
            drawRoad();

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.rotateM(mModelMatrix, 0, 90, 0.0f, 1.0f, 0.0f);
            Matrix.translateM(mModelMatrix, 0, i, 0.0f, 0.0f);
            drawRoad();

            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.rotateM(mModelMatrix, 0, 90, 0.0f, 1.0f, 0.0f);
            Matrix.translateM(mModelMatrix, 0, -i, 0.0f, 0.0f);
            drawRoad();
        }

        //
        // Draw buildings
        // Buildings are created in the constructor
        //

        drawAllBuildings();

        //
        // Draw SkyBox (without textures)
        //

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        // Disable texture flag
        GLES20.glUniform1f(mTextureFlagHandle, 0.0f);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.scaleM(mModelMatrix, 0, 1000.0f, 1000.0f, 1000.0f);
        drawSkyBox();

        //
        // Draw the floor for the treasure
        // (textures are already disabled)
        //
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, mTreasurePos[0], 0.0f, mTreasurePos[2]);
        Matrix.scaleM(mModelMatrix, 0, GenUtil.HALF_BUILD_SQUARE_WIDTH, 0.0f, GenUtil.HALF_BUILD_SQUARE_WIDTH);
        drawFloor();

        // Clear the currently bound buffer (so future OpenGL calls do not use this buffer).
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        //
        // Draw the light
        //

        // Draw a point to indicate the light.
        GLES20.glUseProgram(mPointProgramHandle);
        drawLight();
    }

    private void drawCube() {
        // Pass in the position information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeVBOBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, IShape.VERTEX_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStride, 0);

        // Pass in the normal information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mCubeVBOBuffer);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glVertexAttribPointer(mNormalHandle, IShape.NORMAL_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStride, mVBONormalOffset);

        // Pass in the color information
        GLES20.glUniform4fv(mColorHandle, 1, Cube.colorData, 0);

        drawCommon(36);
    }

    private void drawAllBuildings() {
        // All elements that are not in the for() loop are the same for each building.
        Matrix.setIdentityM(mModelMatrix, 0);

        GLES20.glUniform1f(mTextureFlagHandle, 1.0f);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // These information (normal and texture) are stored for each building but are the same for all.
        // Get them from only the first one
        // Pass in the normal information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBuildVBOBuffers[0]);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glVertexAttribPointer(mNormalHandle, IShape.NORMAL_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStride, mVBONormalOffset);

        // Pass in the texture information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBuildVBOBuffers[0]);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, IShape.TEXTURE_COORDINATE_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStride, mVBOTextureOffset);

        prepareDraw();

        // Draw sides
        for (int i = 0; i < mBuildings.size(); ++i) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBuildTextureDataHandles[mBuildings.get(i).textureType]);
            GLES20.glUniform1i(mTextureUniformHandle, 0);

            // Pass in the position information
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mBuildVBOBuffers[i]);
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, IShape.VERTEX_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                    mVBOStride, 0);

            // Pass in the color information
            GLES20.glUniform4fv(mColorHandle, 1, mBuildings.get(i).color, 0);

            // There isn't bottom face, so only 30 faces
            // The draw is already prepared
            drawCommon(30, false);
        }
    }

    private void drawRoad() {
        // Buffers are initialized in the onSurfaceChanged() method

        // Pass in the color information
        GLES20.glUniform4fv(mColorHandle, 1, Road.colorData, 0);

        drawCommon(6);
    }

    private void drawFloor() {
        // Pass in the position information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mFloorVBOBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, IShape.VERTEX_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStrideNoTex, 0);

        // Pass in the normal information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mFloorVBOBuffer);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glVertexAttribPointer(mNormalHandle, IShape.NORMAL_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStrideNoTex, mVBONormalOffset);

        // Pass in the color information
        GLES20.glUniform4fv(mColorHandle, 1, Floor.colorData, 0);

        drawCommon(6);
    }

    private void drawSkyBox() {
        // Pass in the position information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mSkyBoxVBOBuffer);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, IShape.VERTEX_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStrideNoTex, 0);

        // Pass in the normal information
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mSkyBoxVBOBuffer);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glVertexAttribPointer(mNormalHandle, IShape.NORMAL_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                mVBOStrideNoTex, mVBONormalOffset);

        // Pass in the color information
        GLES20.glUniform4fv(mColorHandle, 1, SkyBox.colorData, 0);

        drawCommon(36);
    }

    // This function do all matrices operations
    private void prepareDraw() {
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);

        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
    }

    // This function is called at the end of other draw functions.
    private void drawCommon(int verticesNumber) {
        drawCommon(verticesNumber, true);
    }

    private void drawCommon(int verticesNumber, boolean prepareDraw) {
        if (prepareDraw) {
            prepareDraw();
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, verticesNumber);
    }

    /**
     * Draws a point representing the position of the light.
     */
    private void drawLight() {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }
}
