package org.ricycir.ricycir_android_app.ui;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.ricycir.ricycir_android_app.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

public class IdentifyActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "Identify::Activity";

    private Mat mRgba;
    private Mat mGray;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_identify);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.detect_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    public IdentifyActivity() {
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    Mat prevMat;
    Mat currMat;
    Mat diff;
    boolean first = true;
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if(prevMat == null)
            prevMat = new Mat();
        if(currMat == null)
            currMat = new Mat();
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if(first) {
            inputFrame.rgba().copyTo(prevMat);
            first = false;
            return mRgba;
        }
        Mat processedFrame = new Mat();
        Imgproc.GaussianBlur(inputFrame.rgba(), currMat, new Size(3, 3), 0);
        Imgproc.GaussianBlur(prevMat, prevMat, new Size(3, 3), 0);
        Core.subtract(currMat, prevMat, processedFrame);
        Imgproc.cvtColor(processedFrame, processedFrame, Imgproc.COLOR_RGB2GRAY);

        Imgproc.threshold(processedFrame, processedFrame, 150, 255, Imgproc.THRESH_BINARY);

        Mat v = new Mat();
        List<MatOfPoint> contours = new ArrayList();
        Imgproc.findContours(processedFrame.clone(), contours, v, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        System.out.println(contours.size());
        currMat.copyTo(prevMat);
        return mRgba;
    }

}