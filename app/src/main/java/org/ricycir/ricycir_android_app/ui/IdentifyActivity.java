package org.ricycir.ricycir_android_app.ui;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.ricycir.ricycir_android_app.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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

        Button enable = (Button)findViewById(R.id.enable);
        enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enabled = !enabled;
            }
        });

        Button classify = (Button)findViewById(R.id.classify);
        classify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manual = true;
            }
        });
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
    boolean first = true;
    boolean recognizing = false;
    int cFrames = 0;
    boolean motion = false;
    int ignore = 0;
    Mat inputF;
    boolean enabled = false;
    boolean manual = false;
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if(prevMat == null)
            prevMat = new Mat();
        if(currMat == null)
            currMat = new Mat();
        if(recognizing)
            return inputFrame.rgba();
        inputF = inputFrame.rgba();
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        if(manual) {
            sendImage(inputFrame.rgba());
            manual = false;
        }
        if(!enabled)
            return mRgba;
        if(ignore < 15) {
            ignore++;
            return mRgba;
        }
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

        Imgproc.threshold(processedFrame, processedFrame, 110, 255, Imgproc.THRESH_BINARY);

        Mat v = new Mat();
        List<MatOfPoint> contours = new ArrayList();
        Imgproc.findContours(processedFrame.clone(), contours, v, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        System.out.println(contours.size());
        if(motion) {
            if(cFrames < 5) {
                cFrames++;
            } else {
                cFrames = 0;
                motion = false;
                recognizing = true;
                sendImage(inputFrame.rgba());
            }
        }
        if(contours.size() > 100 && contours.size() < 500) {
            motion = true;
        }
        currMat.copyTo(prevMat);
        return mGray;
    }

    private void sendImage(Mat img) {
        Bitmap bmp = Bitmap.createBitmap(img.width(), img.height(), Bitmap.Config.ARGB_8888);
        //Mat tmp = new Mat (img.width(), img.height(), CvType.CV_8UC1, new Scalar(4));
        try {
            //Imgproc.cvtColor(seedsImage, tmp, Imgproc.COLOR_RGB2BGRA);
            //Imgproc.cvtColor(img, tmp, Imgproc.COLOR_GRAY2RGBA, 4);
            Utils.matToBitmap(img, bmp);
            System.out.println("sending image");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, bos);
            byte[] data = bos.toByteArray();
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost postRequest = new HttpPost(
                    "http://kerlin.tech:5000/rec_img"); //?returnformat=json&amp;method=testUpload");
            ByteArrayBody bab = new ByteArrayBody(data, "img");
            // File file= new File("/mnt/sdcard/forest.png");
            // FileBody bin = new FileBody(file);
            MultipartEntity reqEntity = new MultipartEntity(
                    HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("uploaded", bab);
            postRequest.setEntity(reqEntity);
            HttpResponse response = httpClient.execute(postRequest);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent(), "UTF-8"));
            String sResponse;
            StringBuilder s = new StringBuilder();

            while ((sResponse = reader.readLine()) != null) {
                s = s.append(sResponse);
            }
            System.out.println("Response: " + s);
            recognizing = false;
            ignore = 0;
        }
        catch (CvException e){
            Log.d("Exception",e.getMessage());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}