package com.gamedevil.gameofhands;

import android.content.Context;

import androidx.annotation.NonNull;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.SupportPreconditions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class FileUtilTSV {
    private FileUtilTSV() {
    }

    @NonNull
    public static List<List<Float>> loadVectorTSV(@NonNull Context context, @NonNull String filePath) throws IOException {
        return loadVectorTSV(context, filePath, Charset.defaultCharset());
    }

    @NonNull
    static List<List<Float>> loadVectorTSV(@NonNull Context context, @NonNull String filePath, Charset cs) throws IOException {
        SupportPreconditions.checkNotNull(context, "Context cannot be null.");
        SupportPreconditions.checkNotNull(filePath, "File path cannot be null.");
        try (InputStream inputStream = context.getAssets().open(filePath)) {
            return loadVectorTSV(inputStream, cs);
        }
    }

    @NonNull
    public static List<List<Float>> loadVectorTSV(@NonNull InputStream inputStream, Charset cs)
            throws IOException {
        List<List<Float>> vectors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, cs))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().length() > 0) {
                    String[] lineItems = line.split("\t");
                    List<Float> vectorValues = new ArrayList<>();
                    for (String s : lineItems) {
                        vectorValues.add(Float.parseFloat(s));
                    }
                    vectors.add(vectorValues);
                }
            }
            return vectors;
        }
    }

}
