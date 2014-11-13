/*
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

package fr.tjdev.commonvrlibrary.util;

public class Random extends java.util.Random {
    public Random() {
        super();
    }
    public Random(long seed) {
        super(seed);
    }
    
    public int intBetween(int max, int min) {
        return nextInt((max - min) + 1) + min;
    }

    public int moreOrLess(int baseValue, int moreOrLess) {
        return baseValue + nextInt((moreOrLess * 2) + 1) - moreOrLess;
    }

    // Have a chance to succeed, base on the percentage arg
    public boolean chance(int percentage) {
        return nextInt(100) < percentage;
    }
}