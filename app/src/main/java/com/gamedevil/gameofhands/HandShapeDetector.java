package com.gamedevil.gameofhands;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;

class HandShapeDetector {

    private static final String TAG = "HandShapeDetector";

    private static final String MODEL_FILE_PATH = "rock_paper_sci_model.tflite";
    private static final String ASSOCIATED_AXIS_LABELS = "rps_labels.tsv";
    private static final String ASSOCIATED_VECTOR_PATH = "rps_vecs.tsv";
    private static final String UNKNOWN_OUTPUT = "-1";
    private static final int NORMALIZATION_VALUE = 255;
    private static final int CROP_SIZE = 300;
    private static final int[] OUTPUT_BUFFER_SHAPE = new int[]{1, 16};

    private TensorBuffer mOutputBuffer;
    private Interpreter mTensorFLite;
    private Context mContext;

    HandShapeDetector(Activity pActivity) {
        mContext = pActivity;
        mOutputBuffer = TensorBuffer.createFixedSize(OUTPUT_BUFFER_SHAPE, DataType.FLOAT32);
        try {
            MappedByteBuffer handShapeModel = FileUtil.loadMappedFile(pActivity, MODEL_FILE_PATH);
            mTensorFLite = new Interpreter(handShapeModel);
        } catch (IOException e) {
            Log.e(TAG, "Error reading model", e);
        }
    }

    String classify(Bitmap pBitmap) {
        if (pBitmap == null || mTensorFLite == null) {
            return UNKNOWN_OUTPUT;
        }
        TensorImage tensorImage = preProcessingImage(pBitmap);
        runInference(tensorImage);
        return postProcessingImage();
    }

    private void runInference(TensorImage pTensorImage) {
        if (null != mTensorFLite) {
            Log.d(TAG, "runInference: Started");
            mTensorFLite.run(pTensorImage.getBuffer(), mOutputBuffer.getBuffer());
        }
    }

    private TensorImage preProcessingImage(Bitmap pBitmap) {
        int width = pBitmap.getWidth();
        int height = pBitmap.getHeight();
        int size = height > width ? width : height;

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(size, size))
                .add(new ResizeOp(CROP_SIZE, CROP_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0, NORMALIZATION_VALUE)) // output = (input-mean)/stddev
                .build();

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(pBitmap);
        tensorImage = imageProcessor.process(tensorImage);
        return tensorImage;
    }

    private List<String> getLoadedLabels() {
        List<String> associatedAxisLabels = null;
        try {
            associatedAxisLabels = FileUtil.loadLabels(mContext, ASSOCIATED_AXIS_LABELS);
        } catch (IOException e) {
            Log.e("getLoadedLabels: ", "Error reading label file", e);
        }
        return associatedAxisLabels;
    }

    private String postProcessingImage() {
        List<String> labels = getLoadedLabels();
        List<float[]> vectors = getVectors();
        float[] outputVector = mOutputBuffer.getFloatArray();
        if (vectors != null) {
            return labels.get(findMinDistanceIndex(vectors, outputVector));
        }
        return UNKNOWN_OUTPUT;
    }

    private List<float[]> getVectors() {
        try {
            return FileUtilTSV.loadVectorTSV(mContext, ASSOCIATED_VECTOR_PATH);
        } catch (IOException pEx) {
            pEx.printStackTrace();
        }
        return null;
    }

    private int findMinDistanceIndex(List<float[]> pVectors, float[] pOutputVector) {
        int index = 0;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < pVectors.size(); i++) {
            double distance = findEuclideanDistance(pVectors.get(i), pOutputVector);
            if (distance < minDistance) {
                index = i;
                minDistance = distance;
            }
        }
        return index;
    }

    private double findEuclideanDistance(float[] vector, float[] outputVector) {
        double sum = 0;
        for (int i = 0; i < vector.length; i++) {
            sum += Math.pow(vector[i] - outputVector[i], 2.0);
        }
        return sum;
    }

}
