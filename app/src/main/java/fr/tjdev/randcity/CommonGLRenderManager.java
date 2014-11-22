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

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import fr.tjdev.commonvrlibrary.BaseGLRenderManager;
import fr.tjdev.randcity.generation.Building;
import fr.tjdev.randcity.generation.GenUtil;
import fr.tjdev.commonvrlibrary.shapes.Cube;
import fr.tjdev.randcity.shapes.Floor;
import fr.tjdev.commonvrlibrary.shapes.IShape;
import fr.tjdev.randcity.shapes.Road;
import fr.tjdev.commonvrlibrary.shapes.SkyBox;
import fr.tjdev.commonvrlibrary.util.BufferHelper;
import fr.tjdev.commonvrlibrary.util.Random;
import fr.tjdev.commonvrlibrary.util.RawResourceReader;
import fr.tjdev.commonvrlibrary.util.ShaderHelper;
import fr.tjdev.commonvrlibrary.util.TextureHelper;

/**
 * This class contains some methods used by real renderer.
 * The methods must be called in the correct order to work correctly.
 */
public class CommonGLRenderManager extends BaseGLRenderManager {
    private static final String TAG = "CommonGLRenderer";

    public static final float PROJECTION_NEAR = 1.0f;
    public static final float PROJECTION_FAR = 2000.0f;

    protected final Context mActivityContext;

    protected int mCubeVBOBuffer;
    protected int mBrickTextureDataHandle;

    // Used to have a floor at the bottom of the treasure
    protected int mFloorVBOBuffer;

    protected int mSkyBoxVBOBuffer;

    protected int mRoadVBOBuffer;
    protected Bitmap mRoadTextureBitmap;
    protected int mRoadTextureDataHandle;

    protected ArrayList<Building> mBuildings;
    // Contains all buffers used for buildings
    protected int[] mBuildVBOBuffers;
    // Store all different textures used by buildings
    protected Bitmap mBuildTextureBitmaps[] = new Bitmap[GenUtil.TEX_TYPES_NB];
    protected int mBuildTextureDataHandles[] = new int[GenUtil.TEX_TYPES_NB];

    // Stores a copy of the model matrix specifically for the light position.
    protected float[] mLightModelMatrix = new float[16];

    // Used to hold a light centered on on point. We need a 4th coordinate so we can get translations to work when
    // we multiply this by our transformation matrices.
    protected final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    // Used to hold the current position of the light in world space (after transformation via model matrix).
    protected final float[] mLightPosInWorldSpace = new float[4];
    // Used to hold the transformed position of the light in eye space (after transformation via model-view matrix)
    protected final float[] mLightPosInEyeSpace = new float[4];

    protected int mMVPMatrixHandle;
    protected int mMVMatrixHandle;
    protected int mLightPosHandle;
    protected int mTextureUniformHandle;
    protected int mTextureFlagHandle;
    protected int mFogFlagHandle;
    protected int mPositionHandle;
    protected int mNormalHandle;
    protected int mColorHandle;
    protected int mTextureCoordinateHandle;

    // mProgramHandle is created in BaseGLRenderer
    protected int mPointProgramHandle;

    // Store the position of the "treasure".
    // In fact, the treasure is a special building that you must reach to end the game.
    protected float[] mTreasurePos;
    // Store the rotation of the light at the treasure
    protected float mLightMoveAngle = 0.0f;

    // Used to toggle the fog
    public volatile boolean enableFog = true;

    public CommonGLRenderManager(final Context activityContext) {
        mActivityContext = activityContext;

        // Generate buildings grid and textures
        generateTerrain();
    }

    // This function will generate buildings and textures
    protected void generateTerrain() {
        mBuildings = Building.generateAllBuildings();

        // Define the treasure pos
        // We replace a random building by the treasure, and get its positions
        Random rand = new Random();
        int randIndex = rand.nextInt(mBuildings.size());
        // Block the treasure from spawning on the sides of the city
        final float maxCenterCoordinates = GenUtil.HALF_GRID_SIZE - GenUtil.HALF_BUILD_SQUARE_WIDTH;
        while(mBuildings.get(randIndex).centerCoordinates[0] == maxCenterCoordinates ||
                mBuildings.get(randIndex).centerCoordinates[0] == -maxCenterCoordinates ||
                mBuildings.get(randIndex).centerCoordinates[2] == maxCenterCoordinates ||
                mBuildings.get(randIndex).centerCoordinates[2] == -maxCenterCoordinates) {
            randIndex = rand.nextInt(mBuildings.size());
        }

        mTreasurePos = mBuildings.get(randIndex).centerCoordinates;

        Log.d(TAG, "Treasure position set to :");
        Log.d(TAG, "x=" + Float.toString(mTreasurePos[0]) + " y=" + Float.toString(mTreasurePos[1]) + " z=" + Float.toString(mTreasurePos[2]));

        // Remove the building from the list.
        mBuildings.remove(randIndex);

        // Generate textures
        // Handles to these textures are generated in onSurfaceCreated() method.
        int i;
        for(i=0 ; i < 8 ; ++i) {
            mBuildTextureBitmaps[i] = Building.generateFuzzyTexture();
        }
        for(; i < 16 ; ++i) {
            mBuildTextureBitmaps[i] = Building.generateLinearTexture();
        }

        // Generate road texture
        mRoadTextureBitmap = Road.generateTexture();
    }

    public void onSurfaceCreated() {
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

        generateVBOs();
    }

    // Generate all VBOs
    // Called in onSurfaceCreated();
    public void generateVBOs() {
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

        // Finish the binding
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void onSurfaceChanged(int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;

        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, PROJECTION_NEAR, PROJECTION_FAR);
    }

    // Load uniforms from the shader
    protected void loadUniforms() {
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mTextureFlagHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_TextureFlag");
        mFogFlagHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_FogFlag");
        mColorHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Color");
    }

    // Load attributes from the shader
    protected void loadAttribs() {
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
    }

    // Compute the angle for the light rotation
    // This angle is applied in updateLightMatrices
    protected void computeLightMoveAngle() {
        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        mLightMoveAngle = (360.0f / 10000.0f) * ((int) time);
    }

    // Check the fog parameter
    protected void checkFog() {
        // Enable (or disable) the fog
        if(enableFog) {
            GLES20.glUniform1f(mFogFlagHandle, 1.0f);
        } else {
            GLES20.glUniform1f(mFogFlagHandle, 0.0f);
        }
    }

    // Update the light matrices to rotate the light in the treasure
    protected void updateLightMatrices() {
        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, mTreasurePos[0], mTreasurePos[1], mTreasurePos[2]);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -2.0f);
        Matrix.rotateM(mLightModelMatrix, 0, mLightMoveAngle, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 1.5f, 3.5f);

        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);
    }

    public void onDrawFrame() {
        clearGLBuffers();

        computeLightMoveAngle();

        setLookAt();

        useProgram();
        loadUniforms();
        loadAttribs();

        checkFog();

        updateLightMatrices();

        draw();
    }

    /**
     * Draw all elements in the world.
     */
    protected void draw() {
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

    protected void drawCube() {
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

    protected void drawAllBuildings() {
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

    protected void drawRoad() {
        // Buffers are initialized in the onSurfaceChanged() method

        // Pass in the color information
        GLES20.glUniform4fv(mColorHandle, 1, Road.colorData, 0);

        drawCommon(6);
    }

    protected void drawFloor() {
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

    protected void drawSkyBox() {
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
    protected void prepareDraw() {
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);

        // Pass in the model-view matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0);

        // This multiplies the model-view matrix by the projection matrix, and stores the result in the MVP matrix
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Pass in the light position in eye space.
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
    }

    // This function is called at the end of other draw functions.
    protected void drawCommon(int verticesNumber) {
        drawCommon(verticesNumber, true);
    }

    protected void drawCommon(int verticesNumber, boolean prepareDraw) {
        if (prepareDraw) {
            prepareDraw();
        }
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, verticesNumber);
    }

    /**
     * Draws a point representing the position of the light.
     */
    protected void drawLight() {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position");

        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle);

        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0);

        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }
}
