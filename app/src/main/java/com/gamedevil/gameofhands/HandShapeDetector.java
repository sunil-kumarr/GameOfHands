package com.gamedevil.gameofhands;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.core.app.ActivityCompat;

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
import java.util.Arrays;
import java.util.List;

class HandShapeDetector {

    private static final String TAG = "HandShapeDetector";

    private static final String MODEL_FILE_PATH = "rock_paper_sci_model.tflite";

    private TensorBuffer mOutputBuffer;
    private Interpreter mTensorFLite;
    private Context mContext;

    HandShapeDetector(Activity pActivity) {
        mContext = pActivity;
        Log.d(TAG, "Model Detector constructor");
        mOutputBuffer = TensorBuffer.createFixedSize(new int[]{1, 16}, DataType.FLOAT32);
        try {
            MappedByteBuffer handShapeModel = FileUtil.loadMappedFile(pActivity, MODEL_FILE_PATH);
            mTensorFLite = new Interpreter(handShapeModel);
            Log.d(TAG, "Created Image Classifier.");
        } catch (IOException e) {
            Log.e(TAG, "Error reading model", e);
        }
    }

    String classify(Bitmap pBitmap) {
        if (pBitmap == null) {
            Log.d(TAG,"Bitmap error! code : -100");
            return "-1";
        }
        if (mTensorFLite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped. code: -200");
            return "-1";
        }
        TensorImage tensorImage = preprocessImage(pBitmap);
        runInference(tensorImage);
        Log.d(TAG, "classify: " + Arrays.toString(mOutputBuffer.getFloatArray()));
        String label = postprocessImage();
        Log.d(TAG, "classify: " + label);
        return label;
    }

    private void runInference(TensorImage pTensorImage) {
        if (null != mTensorFLite) {
            Log.d(TAG, "runInference: Started");
            mTensorFLite.run(pTensorImage.getBuffer(), mOutputBuffer.getBuffer());
        }
    }

    private TensorImage preprocessImage(Bitmap pBitmap) {
        int width = pBitmap.getWidth();
        int height = pBitmap.getHeight();
        int size = height > width ? width : height;

        // output = (input-mean)/stddev
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(size, size))
                .add(new ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(0, 255)) // output = (input-mean)/stddev
                .build();

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(pBitmap);
        tensorImage = imageProcessor.process(tensorImage);
        Log.d(TAG,"Image Preprocessor complete");
        return tensorImage;
    }

    List<String> getLoadedLabels(){
        final String ASSOCIATED_AXIS_LABELS = "rps_labels.tsv";
        List<String> associatedAxisLabels = null;

        try {
            associatedAxisLabels = FileUtil.loadLabels(mContext, ASSOCIATED_AXIS_LABELS);
//            Log.d(TAG, "getLoadedLabels: "+associatedAxisLabels);
        } catch (IOException e) {
            Log.e("tfliteSupport", "Error reading label file", e);
        }
        return associatedAxisLabels;
    }

    private String postprocessImage(){
        List<String> labels = getLoadedLabels();
        List<float[]> vectors;
        String imageLabel = "-1";
        try {
            vectors = FileUtilTSV.loadVectorTSV(mContext,"rps_vecs.tsv");
            float[] outputVector = mOutputBuffer.getFloatArray();
            int index=0;
            double minDistance= Double.MAX_VALUE;
            for(int i=0;i<31;i++){
                double distance = findEuclidianDistance(vectors.get(i),outputVector);
                if(distance<minDistance){
                    index = i;
                    minDistance = distance;
                }
            }
            imageLabel = labels.get(index);
        } catch (IOException pE) {
            pE.printStackTrace();
        }
        return imageLabel;
    }

    private double findEuclidianDistance(float[] vector,float[] outputVector){
        double sum = 0;
        for(int i=0;i<16;i++){
            double diff = vector[i]-outputVector[i];
            sum+= Math.pow(diff,2.0);
        }
        return Math.sqrt(sum);
    }

}
