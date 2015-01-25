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

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

// Contains some methods to manage files on the external storage
public class FileHelper {
    private static final String TAG = "FileHelper";

    // Copy file to the external storage from the resource ID.
    // Note that this code does no error checking and if external storage is
    // not currently mounted this will silently fail.
    static public void createExternalStoragePrivateFile(Context context, String fileName, int resID) {
        File file = new File(context.getExternalFilesDir(null), fileName);
        try {
            InputStream is = context.getResources().openRawResource(resID);
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();
        } catch (IOException e) {
            Log.w(TAG, "Error writing " + file, e);
        }
    }

    static public String readExternalStoragePrivateFile(Context context, String fileName) {
        File file = new File(context.getExternalFilesDir(null), fileName);
        try {
            return readFile(new FileInputStream(file));
        } catch (IOException e) {
            Log.w(TAG, "Error reading " + file, e);
        }
        return null;
    }

    static public boolean deleteExternalStoragePrivateFile(Context context, final String fileName) {
        File file = new File(context.getExternalFilesDir(null), fileName);
        return file.delete();
    }

    static public boolean hasExternalStoragePrivateFile(Context context, final String fileName) {
        File file = new File(context.getExternalFilesDir(null), fileName);
        return file.exists();
    }

    // Read a file from a specified input stream
    static public String readFile(final InputStream inputStream) {
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String nextLine;
        final StringBuilder body = new StringBuilder();

        try {
            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        return body.toString();
    }
}
