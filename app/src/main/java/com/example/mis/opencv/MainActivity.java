package com.example.mis.opencv;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    // Classifiers
    private CascadeClassifier faceDetect;
    private CascadeClassifier eyeDetect;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
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

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
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
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        /* This method is only reached once OpenCV has been initialized
         * successfully, by calling the onManagerConnected callback and
         * starting the camera in that. This means we're allowed to load our
         * classifiers in here.
         * Source: https://docs.opencv.org/java/3.0.0/org/opencv/android/CameraBridgeViewBase.html#enableView()
         */
        faceDetect = new CascadeClassifier(initAssetFile(
                "haarcascade_frontalface_default.xml"));
        eyeDetect = new CascadeClassifier(initAssetFile("haarcascade_eye.xml"));
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        //return inputFrame.rgba();
        /*
        Mat col  = inputFrame.rgba();
        Rect foo = new Rect(new Point(100,100), new Point(200,200));
        Imgproc.rectangle(col, foo.tl(), foo.br(), new Scalar(0, 0, 255), 3);
        return col;
        */

        Mat gray = inputFrame.gray();
        Mat col  = inputFrame.rgba();

        Mat tmp = gray.clone();
        //Imgproc.Canny(gray, tmp, 80, 100);

        // detect faces
        MatOfRect faces = new MatOfRect();
        faceDetect.detectMultiScale(tmp, faces);
        Log.i(TAG, "Detected " + faces.toList().size() + " faces");

        for (Rect face : faces.toList()) {
            /*// DEBUG draw rectangle around face
            Imgproc.rectangle(col, face.tl(), face.br(), new Scalar(255, 0, 0), 2);*/
            // get matrices of only the face
            Mat roi_face_gray = tmp.submat(face);
            Mat roi_face_col = col.submat(face);

            // detect eyes
            MatOfRect eyes = new MatOfRect();
            eyeDetect.detectMultiScale(roi_face_gray, eyes);

            List<Rect> eyeList = eyes.toList();
            /*for (Rect eye : eyeList) {
                // DEBUG draw rectangle around eye
                Imgproc.rectangle(roi_face_col, eye.tl(), eye.br(), new Scalar(0, 255, 0), 2);
            }*/

            if (eyeList.size() >= 2) {
                Line betweenEyes;
                if (eyeList.get(0).x < eyeList.get(1).x) {
                    betweenEyes = new Line(eyeList.get(0).br(), new Point(
                            eyeList.get(1).x, eyeList.get(1).y
                            + eyeList.get(1).height));
                } else {
                    betweenEyes = new Line(eyeList.get(1).br(), new Point(
                            eyeList.get(0).x, eyeList.get(0).y
                            + eyeList.get(0).height));
                }
                /*// DEBUG show line between eyes
                Imgproc.line(roi_face_col, betweenEyes.p1(),
                        betweenEyes.p2(), new Scalar(0, 0, 255), 2);*/

                Line nose = betweenEyes.orthogonalLine();
                /*// DEBUG show nose line
                Imgproc.line(roi_face_col, nose.p1(), nose.mid(),
                        new Scalar(255, 0, 0), 2);*/

                /* Draw the red nose.
                 * "Negative values [...] mean that a filled circle is to be
                 * drawn."
                 * (Source: https://docs.opencv.org/master/d6/d6e/group__imgproc__draw.html#gaf10604b069374903dbd0f0488cb43670)
                 * Size of the nose was determined by trial and error based on
                 * the width of the detected face, since basing it on the
                 * distance between the eyes proved to be unreliable when the
                 * eye detection was flaky.
                 */
                Imgproc.circle(roi_face_col, nose.mid(),
                        (int) Math.ceil(face.width*0.12),
                        new Scalar(255, 0, 0), -1);
            }
        }

        // Imgproc.cvtColor(tmp, col, Imgproc.COLOR_GRAY2RGBA, 4);

        return col;
    }


    public String initAssetFile(String filename)  {
        File file = new File(getFilesDir(), filename);
        if (!file.exists()) try {
            InputStream is = getAssets().open(filename);
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data); os.write(data); is.close(); os.close();
        } catch (IOException e) { e.printStackTrace(); }
        Log.d(TAG,"prepared local file: "+filename);
        return file.getAbsolutePath();
    }
}
