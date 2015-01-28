/*
 * Copyright (c) 2015 Fabien Caylus <toutjuste13@gmail.com>
 *
 *  This file is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This file is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 *    Copyright 2014 Google Inc. All Rights Reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fr.tjdev.commonvrlibrary;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Contains two sub-views to provide a simple stereo HUD.
 */
public class VROverlayView extends LinearLayout {
    private final VROverlayEyeView mLeftView;
    private final VROverlayEyeView mRightView;
    private final AlphaAnimation mTextFadeAnimation;

    public VROverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);

        LayoutParams params = new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(0, 0, 0, 0);

        mLeftView = new VROverlayEyeView(context, attrs);
        mLeftView.setLayoutParams(params);
        addView(mLeftView);

        mRightView = new VROverlayEyeView(context, attrs);
        mRightView.setLayoutParams(params);
        addView(mRightView);

        // Set some reasonable defaults.
        setDepthOffset(0.016f);
        resetColor();
        setVisibility(View.VISIBLE);

        mTextFadeAnimation = new AlphaAnimation(1.0f, 0.0f);
        mTextFadeAnimation.setDuration(3500);
    }

    private void show3DToastInternal(String message) {
        setText(message);
        setTextAlpha(1f);
        mTextFadeAnimation.setAnimationListener(new EndAnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                setTextAlpha(0f);
                resetColor();
            }
        });
        startAnimation(mTextFadeAnimation);
    }

    public void show3DToast(String message) {
        resetColor();
        show3DToastInternal(message);
    }

    // Print a message in red
    public void showError3DToast(String message) {
        setColor(Color.RED);
        show3DToastInternal(message);
    }

    // Print a message in yellow
    public void showWarning3DToast(String message) {
        setColor(Color.YELLOW);
        show3DToastInternal(message);
    }

    private abstract class EndAnimationListener implements Animation.AnimationListener {
        @Override public void onAnimationRepeat(Animation animation) {}
        @Override public void onAnimationStart(Animation animation) {}
    }

    private void setDepthOffset(float offset) {
        mLeftView.setOffset(offset);
        mRightView.setOffset(-offset);
    }

    private void setText(String text) {
        mLeftView.setText(text);
        mRightView.setText(text);
    }

    private void setTextAlpha(float alpha) {
        mLeftView.setTextViewAlpha(alpha);
        mRightView.setTextViewAlpha(alpha);
    }

    public void setColor(int color) {
        mLeftView.setColor(color);
        mRightView.setColor(color);
    }

    public void resetColor() {
        setColor(Color.rgb(150, 255, 180));
    }

    /**
     * A simple view group containing some horizontally centered text underneath a horizontally
     * centered image.
     *
     * This is a helper class for VROverlayView.
     */
    private class VROverlayEyeView extends ViewGroup {
        private final ImageView mmImageView;
        private final TextView mmTextView;
        private float mmOffset;

        public VROverlayEyeView(Context context, AttributeSet attrs) {
            super(context, attrs);
            mmImageView = new ImageView(context, attrs);
            mmImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            mmImageView.setAdjustViewBounds(true);  // Preserve aspect ratio.
            addView(mmImageView);

            mmTextView = new TextView(context, attrs);
            mmTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f);
            mmTextView.setTypeface(mmTextView.getTypeface(), Typeface.BOLD);
            mmTextView.setGravity(Gravity.CENTER);
            mmTextView.setShadowLayer(3.0f, 0.0f, 0.0f, Color.DKGRAY);
            addView(mmTextView);
        }

        public void setColor(int color) {
            mmImageView.setColorFilter(color);
            mmTextView.setTextColor(color);
        }

        public void setText(String text) {
            mmTextView.setText(text);
        }

        public void setTextViewAlpha(float alpha) {
            mmTextView.setAlpha(alpha);
        }

        public void setOffset(float offset) {
            mmOffset = offset;
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
            // Width and height of this ViewGroup.
            final int width = right - left;
            final int height = bottom - top;

            // The size of the image, given as a fraction of the dimension as a ViewGroup. We multiply
            // both width and heading with this number to compute the image's bounding box. Inside the
            // box, the image is the horizontally and vertically centered.
            final float imageSize = 0.12f;

            // The fraction of this ViewGroup's height by which we shift the image off the ViewGroup's
            // center. Positive values shift downwards, negative values shift upwards.
            final float verticalImageOffset = -0.07f;

            // Vertical position of the text, specified in fractions of this ViewGroup's height.
            final float verticalTextPos = 0.52f;

            // Layout ImageView
            float imageMargin = (1.0f - imageSize) / 2.0f;
            float leftMargin = (int) (width * (imageMargin + mmOffset));
            float topMargin = (int) (height * (imageMargin + verticalImageOffset));
            mmImageView.layout(
                (int) leftMargin, (int) topMargin,
                (int) (leftMargin + width * imageSize), (int) (topMargin + height * imageSize));

            // Layout TextView
            leftMargin = mmOffset * width;
            topMargin = height * verticalTextPos;
            mmTextView.layout(
                (int) leftMargin, (int) topMargin,
                (int) (leftMargin + width), (int) (topMargin + height * (1.0f - verticalTextPos)));
        }
    }
}
