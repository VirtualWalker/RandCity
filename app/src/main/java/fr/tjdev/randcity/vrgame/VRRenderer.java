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

package fr.tjdev.randcity.vrgame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;

import fr.tjdev.commonvrlibrary.BaseGLRenderManager;
import fr.tjdev.commonvrlibrary.shapes.IShape;
import fr.tjdev.commonvrlibrary.shapes.SkyBox;
import fr.tjdev.commonvrlibrary.util.BufferHelper;
import fr.tjdev.commonvrlibrary.util.FloorSurface;
import fr.tjdev.commonvrlibrary.util.Random;
import fr.tjdev.commonvrlibrary.util.RawResourceReader;
import fr.tjdev.commonvrlibrary.util.RectF3D;
import fr.tjdev.commonvrlibrary.util.ShaderHelper;
import fr.tjdev.commonvrlibrary.util.TextureHelper;
import fr.tjdev.randcity.BuildConfig;
import fr.tjdev.randcity.R;
import fr.tjdev.randcity.generation.Building;
import fr.tjdev.randcity.generation.GenUtil;
import fr.tjdev.randcity.generation.Stairs;
import fr.tjdev.randcity.shapes.Road;
import fr.tjdev.randcity.shapes.TreasureCorridor;

/**
 * Renderer used for virtual reality game.
 */
public class VRRenderer extends BaseGLRenderManager implements CardboardView.StereoRenderer {
    private static final String TAG = "VRRenderer";

    public static final float PROJECTION_NEAR = 1.0f;
    public static final float PROJECTION_FAR = 2000.0f;

    protected final Context mActivityContext;

    protected int mSkyBoxVBOBuffer;

    protected int mRoadVBOBuffer;
    protected Bitmap mRoadTextureBitmap;
    protected int mRoadTextureDataHandle;

    protected ArrayList<Building> mBuildings;
    protected ArrayList<RectF3D> mRestrictedAreas;
    // Contains all buffers used for buildings
    protected int[] mBuildVBOBuffers;
    // Store all different textures used by buildings
    protected Bitmap mBuildTextureBitmaps[] = new Bitmap[GenUtil.TEX_TYPES_NB];
    protected int mBuildTextureDataHandles[] = new int[GenUtil.TEX_TYPES_NB];

    protected Stairs mTreasureStairs;
    protected int mStairsVBOBuffer;

    protected int mTreasureCorridorVBOBuffer;
    protected Bitmap mTreasureCorridorBitmap;
    protected int mTreasureCorridorTextureDataHandle;

    protected float[] mHeadView = new float[16];

    //
    // We have 4 lights near the treasure
    //

    // Stores a copy of the model matrix specifically for the light position.
    protected float[][] mLightModelMatrix = new float[4][16];

    // Used to hold a light centered on a point. We need a 4th coordinate so we can get translations to work when
    // we multiply this by our transformation matrices.
    protected final float[] mLightPosInModelSpace = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
    // Used to hold the transformed position of the light in eye space (after transformation via model-view matrix)
    protected final float[][] mLightPosInEyeSpace = new float[4][4];

    protected int mLightPosHandle1;
    /*protected int mLightPosHandle2;
    protected int mLightPosHandle3;
    protected int mLightPosHandle4;*/

    protected int mMVPMatrixHandle;
    protected int mMVMatrixHandle;
    protected int mTextureUniformHandle;
    protected int mTextureFlagHandle;
    protected int mFogFlagHandle;
    protected int mPositionHandle;
    protected int mNormalHandle;
    protected int mColorHandle;
    protected int mTextureCoordinateHandle;

    // Store the position of the "treasure".
    // In fact, the treasure is a special building that you must reach to end the game.
    protected float[] mTreasurePos;
    protected RectF3D mTreasureArea;

    // Used to toggle the fog
    public volatile boolean enableFog = true;

    // Tell in which direction the player is looking
    volatile public float[] lookForwardVector = {
            0.0f, 0.0f, 0.0f
    };

    // Custom listener called when the treasure is found
    public interface OnTreasureFoundListener {
        void onTreasureFound();
    }
    protected OnTreasureFoundListener mTreasureFoundListener;

    public void setOnTreasureFoundListener(OnTreasureFoundListener listener) {
        mTreasureFoundListener = listener;
    }

    // If debug generation set to true, the player will not appear at a random pos
    // and the treasure will be at the center of the city
    public VRRenderer(final Context activityContext, boolean debugGeneration) {
        mActivityContext = activityContext;

        // Generate buildings grid and textures
        generateTerrain(debugGeneration);

        if(!debugGeneration) {
            // Move the player at a random position
            boolean success = false;
            while(!success) {
                Random rand = new Random();
                final int newX = rand.intBetween((int)-GenUtil.HALF_GRID_SIZE, (int)GenUtil.HALF_GRID_SIZE);
                final int newZ = rand.intBetween((int)-GenUtil.HALF_GRID_SIZE, (int)GenUtil.HALF_GRID_SIZE);
                success = movePlayer(newX, 0.0f, newZ);
            }
        }
    }

    // This function will generate buildings and textures
    protected void generateTerrain(boolean debugGeneration) {
        mBuildings = Building.generateAllBuildings();

        // Define the treasure pos
        int treasureIndex = 2;
        if(!debugGeneration) {
            // We replace a random building by the treasure, and get its positions
            Random rand = new Random();
            treasureIndex = rand.nextInt(mBuildings.size());
            // Block the treasure from spawning outside the walk area
            while (mBuildings.get(treasureIndex).centerCoordinates[0] > GenUtil.HALF_ALLOWED_GRID_SIZE ||
                    mBuildings.get(treasureIndex).centerCoordinates[0] < -GenUtil.HALF_ALLOWED_GRID_SIZE ||
                    mBuildings.get(treasureIndex).centerCoordinates[2] > GenUtil.HALF_ALLOWED_GRID_SIZE ||
                    mBuildings.get(treasureIndex).centerCoordinates[2] < -GenUtil.HALF_ALLOWED_GRID_SIZE) {
                treasureIndex = rand.nextInt(mBuildings.size());
            }
        }

        // Represent the position for the enter of the treasure room
        mTreasurePos = mBuildings.get(treasureIndex).centerCoordinates;
        // Remove the building from the list.
        mBuildings.remove(treasureIndex);

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Treasure position set to :");
            Log.d(TAG, "x=" + Float.toString(mTreasurePos[0]) + " y=" + Float.toString(mTreasurePos[1]) + " z=" + Float.toString(mTreasurePos[2]));
        }

        // Generate the stairs for the treasure
        mTreasureStairs = Stairs.generate(GenUtil.STAIRS_NUMBER, GenUtil.STAIRS_HEIGHT,
                GenUtil.STAIRS_DEPTH, GenUtil.STAIRS_WIDTH);

        mTreasureStairs.area = new RectF(mTreasurePos[0] - GenUtil.HALF_BUILD_SQUARE_WIDTH,
                mTreasurePos[2] - GenUtil.HALF_BUILD_SQUARE_WIDTH,
                mTreasurePos[0] + GenUtil.HALF_BUILD_SQUARE_WIDTH,
                mTreasurePos[2] + GenUtil.HALF_BUILD_SQUARE_WIDTH);

        mTreasureStairs.generateFloorSurfaces();

        mTreasureArea = new RectF3D(mTreasureStairs.area.left + TreasureCorridor.CORRIDOR_LENGTH - 20.0f,
                mTreasureStairs.area.top,
                mTreasureStairs.area.left + TreasureCorridor.CORRIDOR_LENGTH,
                mTreasureStairs.area.bottom,
                -TreasureCorridor.CORRIDOR_HEIGHT,
                -TreasureCorridor.CORRIDOR_HEIGHT + PLAYER_HEIGHT + 5.0f);

        // TODO: generate lights here

        // Generate restricted areas
        mRestrictedAreas = Building.generateRestrictedAreas(mBuildings, mTreasurePos);

        // Generate textures
        // Handles to these textures are generated in onSurfaceCreated() method.
        int i;
        for (i=0 ; i < 6 ; ++i) {
            mBuildTextureBitmaps[i] = Building.generateFuzzyTexture(false);
        }
        for (; i < 8 ; ++i) {
            mBuildTextureBitmaps[i] = Building.generateFuzzyTexture(true);
        }
        for (; i < 16 ; ++i) {
            mBuildTextureBitmaps[i] = Building.generateLinearTexture();
        }

        mRoadTextureBitmap = Road.generateTexture();
        mTreasureCorridorBitmap = TreasureCorridor.generateTexture();
    }

    // Utility function to move the player
    // This function check for buildings positions
    // Return true on success, else return false
    public boolean movePlayer(final float moveX, final float moveY, final float moveZ) {
        lookX += moveX;
        eyeX += moveX;

        lookZ -= moveZ;
        eyeZ -= moveZ;

        lookY += moveY;
        eyeY += moveY;

        // Used to know if we are in a restricted area or not
        boolean inRestrictedArea = false;

        // Check if we are in a building
        for (RectF3D area : mRestrictedAreas) {
            if (area.contains(eyeX, eyeY, eyeZ)) {
                inRestrictedArea = true;
                // Check for each axis
                if (area.containsXAxis(eyeX)) {
                    lookX -= moveX;
                    eyeX -= moveX;
                }
                if (area.containsYAxis(eyeY)) {
                    lookY -= moveY;
                    eyeY -= moveY;
                }
                if (area.containsZAxis(eyeZ)) {
                    lookZ += moveZ;
                    eyeZ += moveZ;
                }
            }
        }

        if (!inRestrictedArea) {
            // Check if we are at the treasure pos
            if (mTreasureArea.contains(eyeX, eyeY, eyeZ)) {
                // Here the treasure is found !
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Treasure found by the player !");
                }

                // Call the corresponding listener (if exists)
                if (mTreasureFoundListener != null) {
                    mTreasureFoundListener.onTreasureFound();
                }
            }

            //
            // Move the player to the floor
            //

            // Check if the player is on the stairs
            for (FloorSurface surface : mTreasureStairs.floorSurfaces) {
                if (surface.area.contains(eyeX, eyeZ)) {
                    eyeY = surface.height + PLAYER_HEIGHT;
                    lookY = eyeY;
                }
            }

            // Check if the player is out of the city
            if (eyeX >= GenUtil.HALF_ALLOWED_GRID_SIZE || eyeX <= -GenUtil.HALF_ALLOWED_GRID_SIZE) {
                lookX -= moveX;
                eyeX -= moveX;
                return false;
            } else if (eyeZ >= GenUtil.HALF_ALLOWED_GRID_SIZE || eyeZ <= -GenUtil.HALF_ALLOWED_GRID_SIZE) {
                lookZ += moveZ;
                eyeZ += moveZ;
                return false;
            }
        }

        return !inRestrictedArea;
    }

    @Override
    public void onRendererShutdown() {
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    /**
     * Create the OpenGL objects. This is the initialization of the renderer.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        // Could make the rendering faster on some GPUs
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glDepthMask(true);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.vertex_shader);
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.fragment_shader);

        final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
        final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);

        mProgramHandle = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle,
                new String[]{"a_Position", "a_Normal", "a_TexCoordinate"});

        // Load the texture
        mRoadTextureDataHandle = TextureHelper.loadTexture(mRoadTextureBitmap, false);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        mTreasureCorridorTextureDataHandle = TextureHelper.loadTexture(mTreasureCorridorBitmap, false);
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
        }

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
        // Stairs VBOs
        FloatBuffer stairsBuffer = BufferHelper.getInterleavedBuffer(mTreasureStairs.positionData,
                mTreasureStairs.normals, new float[0]);

        final int stairsTempBuffers[] = new int[1];
        GLES20.glGenBuffers(1, stairsTempBuffers, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, stairsTempBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, stairsBuffer.capacity() * IShape.BYTES_PER_FLOAT,
                stairsBuffer, GLES20.GL_STATIC_DRAW);

        mStairsVBOBuffer = stairsTempBuffers[0];
        stairsBuffer.limit(0);

        //
        // Treasure corridor VBOs
        FloatBuffer corridorBuffer = BufferHelper.getInterleavedBuffer(TreasureCorridor.positionData,
                TreasureCorridor.normalsData, TreasureCorridor.textureCoordinatesData);

        final int corridorTempBuffers[] = new int[1];
        GLES20.glGenBuffers(1, corridorTempBuffers, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, corridorTempBuffers[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, corridorBuffer.capacity() * IShape.BYTES_PER_FLOAT,
                corridorBuffer, GLES20.GL_STATIC_DRAW);

        mTreasureCorridorVBOBuffer = corridorTempBuffers[0];
        corridorBuffer.limit(0);

        // Finish the binding
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        final float ratio = (float) width / height;

        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1.0f, 1.0f, PROJECTION_NEAR, PROJECTION_FAR);
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        headTransform.getHeadView(mHeadView, 0);
        headTransform.getForwardVector(lookForwardVector, 0);
    }

    /**
     * Draws a frame for an eye. The transformation for that eye (from the camera) is passed in as
     * a parameter.
     * @param transform The transformations to apply to render this eye.
     */
    @Override
    public void onDrawEye(Eye transform) {
        clearGLBuffers();
        setLookAt();

        GLES20.glUseProgram(mProgramHandle);

        // Load uniforms from the shader
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix");
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mTextureFlagHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_TextureFlag");
        mFogFlagHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_FogFlag");
        mColorHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Color");

        mLightPosHandle1 = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
        /*mLightPosHandle1 = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos1");
        mLightPosHandle2 = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos2");
        mLightPosHandle3 = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos3");
        mLightPosHandle4 = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos4");*/

        // Load attributes from the shader
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");

        // Apply the eye transformation to the camera
        float[] tempViewMatrix = new float[16];
        Matrix.multiplyMM(tempViewMatrix, 0, transform.getEyeView(), 0, mViewMatrix, 0);
        System.arraycopy(tempViewMatrix, 0, mViewMatrix, 0, 16);

        checkFog();


        // TODO: make lights work
        //
        // Generate light matrices for the treasure
        //

        float[] tempLightModelMatrix = new float[16];
        Matrix.setIdentityM(tempLightModelMatrix, 0);
        //Matrix.translateM(mLightModelMatrix[0], 0, mTreasureStairs.area.left, 1.0f, mTreasureStairs.area.top);
        Matrix.translateM(tempLightModelMatrix, 0, 0.0f, 5.0f, 0.0f);
        mLightModelMatrix[0] = tempLightModelMatrix;

        Matrix.setIdentityM(tempLightModelMatrix, 0);
        Matrix.translateM(tempLightModelMatrix, 0, mTreasureStairs.area.left, 1.0f, mTreasureStairs.area.bottom);
        mLightModelMatrix[1] = tempLightModelMatrix;

        Matrix.setIdentityM(tempLightModelMatrix, 0);
        Matrix.translateM(tempLightModelMatrix, 0, mTreasureStairs.area.left + TreasureCorridor.CORRIDOR_LENGTH - 1.0f,
                1.0f - TreasureCorridor.CORRIDOR_HEIGHT, mTreasureStairs.area.top - 1.0f);
        mLightModelMatrix[2] = tempLightModelMatrix;

        Matrix.setIdentityM(tempLightModelMatrix, 0);
        Matrix.translateM(tempLightModelMatrix, 0, mTreasureStairs.area.left + TreasureCorridor.CORRIDOR_LENGTH - 1.0f,
                1.0f - TreasureCorridor.CORRIDOR_HEIGHT, mTreasureStairs.area.bottom + 1.0f);
        mLightModelMatrix[3] = tempLightModelMatrix;

        //
        // Update light matrices depending on the current view matrix

        float[] lightPosInWorldSpace = new float[4];
        float[] lightPosInEyeSpace = new float[4];
        Matrix.multiplyMV(lightPosInWorldSpace, 0, mLightModelMatrix[0], 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(lightPosInEyeSpace, 0, mViewMatrix, 0, lightPosInWorldSpace, 0);
        mLightPosInEyeSpace[0] = lightPosInEyeSpace;

        Matrix.multiplyMV(lightPosInWorldSpace, 0, mLightModelMatrix[1], 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(lightPosInEyeSpace, 0, mViewMatrix, 0, lightPosInWorldSpace, 0);
        mLightPosInEyeSpace[1] = lightPosInEyeSpace;

        Matrix.multiplyMV(lightPosInWorldSpace, 0, mLightModelMatrix[2], 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(lightPosInEyeSpace, 0, mViewMatrix, 0, lightPosInWorldSpace, 0);
        mLightPosInEyeSpace[2] = lightPosInEyeSpace;

        Matrix.multiplyMV(lightPosInWorldSpace, 0, mLightModelMatrix[3], 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(lightPosInEyeSpace, 0, mViewMatrix, 0, lightPosInWorldSpace, 0);
        mLightPosInEyeSpace[3] = lightPosInEyeSpace;

        /*for (int i=0; i<4; ++i) {
            Matrix.multiplyMV(mLightPosInWorldSpace[i], 0, mLightModelMatrix[i], 0, mLightPosInModelSpace, 0);
            Matrix.multiplyMV(mLightPosInEyeSpace[i], 0, mViewMatrix, 0, mLightPosInWorldSpace[i], 0);
        }*/

        mProjectionMatrix = transform.getPerspective(PROJECTION_NEAR, PROJECTION_FAR);

        // Now, we can draw all elements on the screen
        draw();
    }

    // Check the fog parameter
    protected void checkFog() {
        // Enable (or disable) the fog
        if (enableFog) {
            GLES20.glUniform1f(mFogFlagHandle, 1.0f);
        } else {
            GLES20.glUniform1f(mFogFlagHandle, 0.0f);
        }
    }

    // Pass in the position information
    protected void bindPositionBuffer(int bufferVBO, int stride) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferVBO);
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, IShape.VERTEX_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                stride, 0);
    }

    // Pass in the normal information
    protected void bindNormalBuffer(int bufferVBO, int stride) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferVBO);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glVertexAttribPointer(mNormalHandle, IShape.VERTEX_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                stride, mVBONormalOffset);
    }

    // Pass in the texture information
    protected void bindTextureBuffer(int bufferVBO, int stride) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferVBO);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, IShape.VERTEX_DATA_ELEMENTS, GLES20.GL_FLOAT, false,
                stride, mVBOTextureOffset);
    }

    /**
     * Draw all elements in the world.
     */
    protected void draw() {
        GLES20.glUniform1f(mTextureFlagHandle, 1.0f);

        //
        // Draw the roads
        //

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mRoadTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        bindPositionBuffer(mRoadVBOBuffer, mVBOStride);
        bindNormalBuffer(mRoadVBOBuffer, mVBOStride);
        bindTextureBuffer(mRoadVBOBuffer, mVBOStride);

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
        // Draw the treasure corridor
        //

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTreasureCorridorTextureDataHandle);
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, mTreasurePos[0] + GenUtil.HALF_BUILD_SQUARE_WIDTH + GenUtil.SPACE_BETWEEN_ROADS_X2,
                -GenUtil.HALF_BUILD_SQUARE_WIDTH, mTreasurePos[2]);
        drawCorridor();

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
        // Draw the stairs at the treasure pos
        //

        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, mTreasurePos[0] - GenUtil.HALF_BUILD_SQUARE_WIDTH, 0.0f, mTreasurePos[2]);
        drawStairs();

        // Clear the currently bound buffer (so future OpenGL calls do not use this buffer).
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    protected void drawAllBuildings() {
        // All elements that are not in the for() loop are the same for each building.
        Matrix.setIdentityM(mModelMatrix, 0);

        GLES20.glUniform1f(mTextureFlagHandle, 1.0f);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        // These information (normal and texture) are stored for each building but are the same for all.
        // Get them from only the first one
        bindNormalBuffer(mBuildVBOBuffers[0], mVBOStride);
        bindTextureBuffer(mBuildVBOBuffers[0], mVBOStride);

        prepareDraw();

        // Draw sides
        for (int i = 0; i < mBuildings.size(); ++i) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mBuildTextureDataHandles[mBuildings.get(i).textureType]);
            GLES20.glUniform1i(mTextureUniformHandle, 0);

            bindPositionBuffer(mBuildVBOBuffers[i], mVBOStride);

            // Pass in the color information
            GLES20.glUniform4fv(mColorHandle, 1, mBuildings.get(i).color, 0);

            // There isn't bottom face, so only 30 faces
            // The draw is already prepared
            drawCommon(30, false);
        }
    }

    protected void drawRoad() {
        // Buffers are initialized in the draw() method

        // Pass in the color information
        GLES20.glUniform4fv(mColorHandle, 1, Road.colorData, 0);

        drawCommon(6);
    }

    protected void drawStairs() {
        bindPositionBuffer(mStairsVBOBuffer, mVBOStrideNoTex);
        bindNormalBuffer(mStairsVBOBuffer, mVBOStrideNoTex);

        // Pass in the color information
        GLES20.glUniform4fv(mColorHandle, 1, Stairs.color, 0);

        drawCommon(mTreasureStairs.verticesNumber);
    }

    protected void drawCorridor() {
        bindPositionBuffer(mTreasureCorridorVBOBuffer, mVBOStride);
        bindNormalBuffer(mTreasureCorridorVBOBuffer, mVBOStride);
        bindTextureBuffer(mTreasureCorridorVBOBuffer, mVBOStride);

        GLES20.glUniform4fv(mColorHandle, 1, Road.colorData, 0);

        drawCommon(30);
    }

    protected void drawSkyBox() {
        bindPositionBuffer(mSkyBoxVBOBuffer, mVBOStrideNoTex);
        bindNormalBuffer(mSkyBoxVBOBuffer, mVBOStrideNoTex);

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

        // Pass in the light positions in eye space.
        GLES20.glUniform3f(mLightPosHandle1, mLightPosInEyeSpace[0][0], mLightPosInEyeSpace[0][1], mLightPosInEyeSpace[0][2]);
        /*GLES20.glUniform3f(mLightPosHandle2, mLightPosInEyeSpace[1][0], mLightPosInEyeSpace[1][1], mLightPosInEyeSpace[1][2]);
        GLES20.glUniform3f(mLightPosHandle3, mLightPosInEyeSpace[2][0], mLightPosInEyeSpace[2][1], mLightPosInEyeSpace[2][2]);
        GLES20.glUniform3f(mLightPosHandle4, mLightPosInEyeSpace[3][0], mLightPosInEyeSpace[3][1], mLightPosInEyeSpace[3][2]);*/
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
}
