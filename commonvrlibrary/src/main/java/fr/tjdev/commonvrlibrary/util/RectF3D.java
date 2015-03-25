/*
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

package fr.tjdev.commonvrlibrary.util;

// Simple class that hold a 3D rect
public class RectF3D {
    // Represent the floor of the rect
    public float floorLeft;
    public float floorUp;
    public float floorRight;
    public float floorDown;

    // Represent the height
    public float bottom;
    public float top;

    public RectF3D(float floorLeft, float floorUp, float floorRight, float floorDown, float bottom, float top) {
        this.floorLeft = floorLeft;
        this.floorUp = floorUp;
        this.floorRight = floorRight;
        this.floorDown = floorDown;
        this.bottom = bottom;
        this.top = top;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RectF3D r = (RectF3D) o;
        return floorLeft == r.floorLeft && floorRight == r.floorRight && floorDown == r.floorDown && floorUp == r.floorUp
                && bottom == r.bottom && top == r.top;
    }

    public boolean contains(float x, float y, float z) {
        return floorLeft < floorRight && floorUp < floorDown && bottom < top  // check for empty first
                && x >= floorLeft && x < floorRight
                && z >= floorUp && z < floorDown
                && y >= bottom && y < top;
    }

    //
    // Above methods only checks on a specified axis
    //

    public boolean containsXAxis(float x) {
        return floorLeft < x && floorRight > x;
    }
    public boolean containsZAxis(float z) {
        return floorUp < z && floorDown > z;
    }
    public boolean containsYAxis(float y) {
        return bottom < y && top > y;
    }
}