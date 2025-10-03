package com.xapps.media.xmusic.utils;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SerializationUtils {

    public static void saveToFile(Context context, ArrayList<HashMap<String, Object>> data, String fileName) throws IOException {
        long startTime = System.nanoTime();
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(context.openFileOutput(fileName, Context.MODE_PRIVATE), 16384))) {
            oos.writeObject(data);
            long endTime = System.nanoTime();
            Log.d("Serialization", String.format("Save time: %.3f ms", (endTime - startTime) / 1_000_000.0));
        }
    }

    public static ArrayList<HashMap<String, Object>> readFromFile(Context context, String fileName) {
        long startTime = System.nanoTime();
        File file = context.getFileStreamPath(fileName);

        if (file.exists() && file.length() > 0) {
            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(context.openFileInput(fileName), 16384))) {
                @SuppressWarnings("unchecked")
                ArrayList<HashMap<String, Object>> result = (ArrayList<HashMap<String, Object>>) ois.readObject();
                long endTime = System.nanoTime();
                Log.d("Serialization", String.format("Read time: %.3f ms", (endTime - startTime) / 1_000_000.0));
                return result;
            } catch (IOException | ClassNotFoundException e) {
                Log.e("Serialization", "Error reading file: " + e.getMessage());
                return null;
            }
        } else {
            Log.d("Serialization", "File not found or empty: " + fileName);
            return null;
        }
    }
}