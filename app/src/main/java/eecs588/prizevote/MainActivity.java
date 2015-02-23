package eecs588.prizevote;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.webkit.WebView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    // Displayed while loading the next ad
    private ProgressDialog adSpinner;
    private WebView webView;

    // Access to the device's camera
    Camera cam = null;

    // The user who logged in
    String username = "";
    int points = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            username = extras.getString("username");
            username = username.replace('@', '_');
            username = username.replace('.', '_');
            takeCameraPhoto();
            setPoints(0);

            webView = (WebView) findViewById(R.id.webView1);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("http://www.qckt.me");

        }

        loadNewAd();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(adSpinner != null) {
            adSpinner.dismiss();
        }
        if(cam != null) {
            cam.stopPreview();
            cam.release();
            cam = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Nothing to do
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* Called when the user clicks on either the Upvote or Downvote button */
    public void vote(View view) {
        // Tint the image based on the user's vote
        int color = Color.BLACK;
        switch (view.getId()) {
            case (R.id.upvoteButton):
                color = Color.GREEN;
                break;
            case (R.id.downvoteButton):
                color = Color.RED;
                break;
        }
        ImageView ad = (ImageView)findViewById(R.id.adImageView);
        ad.setColorFilter(color, PorterDuff.Mode.MULTIPLY);

        setPoints(points + 10);

        // Start loading the next ad (asynchronous)
        loadNewAd();
    }

    /* Update the user's points in gui */
    private void setPoints(int value) {
        points = value;
        String text = "Points: " + String.valueOf(points);
        ((TextView)findViewById(R.id.pointsText)).setText(text);
    }

    /* Called when the user clicks the "Refer A Friend" button */
    public void referFriend(View view) {
        Intent referIntent = new Intent(this, ReferActivity.class);
        startActivity(referIntent);
    }

    /* Called when the user clicks the "Redeem Points" button */
    public void redeemPoints(View view) {
        String packageName = getString(R.string.map_package_name);
        String marketPath = "market://details?id=" + packageName;

        // Attempt to open the map package, or if not installed,
        // view it on the Android app market
        Intent redeemIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if(redeemIntent != null) {
            startActivity(redeemIntent);
        } else {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketPath));
            startActivity(marketIntent);
        }
    }

    /* Takes a photo using the camera and sends it to the server */
    private void takeCameraPhoto() {
        // Prefer to use the front-facing camera
        int camID = getCameraID(Camera.CameraInfo.CAMERA_FACING_FRONT);
        if(camID == -1) {
            camID = getCameraID(Camera.CameraInfo.CAMERA_FACING_BACK);
            if(camID == -1) {
                return;
            }
        }
        cam = Camera.open(camID);

        // After photo is taken, save image in temp dir and send to server
        final File outDir = this.getCacheDir();
        final Camera.PictureCallback jpgCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(final byte[] jpgData, final Camera camera) {
                // Close camera
                cam.stopPreview();
                cam.release();
                cam = null;

                try {
                    // Write image data to temp file
                    File outFile = File.createTempFile("temp", "jpg", outDir);
                    FileOutputStream outStream = new FileOutputStream(outFile);
                    outStream.write(jpgData);
                    outStream.close();

                    // Send image to server
                    String serverURL = getString(R.string.server_submit_img);
                    new HTTPFilePoster(outFile, username, serverURL);
                } catch (IOException e) {
                    Log.i("onPictureTaken()", "Failure: " + e.getMessage());
                    return;
                }

                Log.i("onPictureTaken()", "Success");
            }
        };

        // After hidden surface is initialized, start preview
        SurfaceView view = new SurfaceView(this);
        view.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    cam.setPreviewDisplay(holder);
                } catch (IOException e) {
                    Log.i("surfaceCreated()", "Failure: " + e.getMessage());
                    return;
                }
                cam.startPreview();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // Nothing to do
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // Nothing to do
            }
        });

        cam.takePicture(null, null, null, jpgCallback);
    }

    private int getCameraID(int facing) {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if(info.facing == facing) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    /* Requests a new ad URL from the server and loads it into the ad display */
    private void loadNewAd() {
        Handler callback = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // Update display ad unless we failed
                ImageView ad = (ImageView) findViewById(R.id.adImageView);
                if(msg.obj != null) {
                    Bitmap adBitmap = (Bitmap) msg.obj;
                    ad.setImageBitmap(adBitmap);
                }

                ad.clearColorFilter();
                if(adSpinner != null) {
                    adSpinner.dismiss();
                }
            }
        };

        adSpinner =  ProgressDialog.show(this, "Loading next ad", "Please wait...", true);
        String serverURL = getString(R.string.server_submit_img);
        new AdFetcher(callback, serverURL);
    }

}
