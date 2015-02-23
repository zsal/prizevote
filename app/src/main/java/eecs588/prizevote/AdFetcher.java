package eecs588.prizevote;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


public class AdFetcher {

    public AdFetcher(final Handler callback, final String serverURL) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Message msg = Message.obtain();

                try {
                    String adURL = fetchAdURL(serverURL);
                    Bitmap adBitmap = fetchAdBitmap(adURL);
                    msg.obj = adBitmap;
                } catch(IOException e) {
                    msg.obj = null;
                }

                callback.sendMessage(msg);
            }
        }).start();
    }

    private String fetchAdURL(String serverURL) {
        String defaultURL = "http://file.vintageadbrowser.com/l-vhg87cqhou6z8a.jpg";

        try {
            // Request random ad URL
            URL url = new URL(serverURL);
            URLConnection connection = url.openConnection();
            connection.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            while((inputLine = in.readLine()) != null) {
                if(inputLine.substring(inputLine.length() - 4).equals(".jpg")) {
                    return inputLine;
                }
            }
            in.close();
        } catch (IOException e) {
            return defaultURL;
        }
        return defaultURL;
    }

    private Bitmap fetchAdBitmap(String adURL) throws IOException {
        Bitmap result;
        InputStream in = new java.net.URL(adURL).openStream();
        result = BitmapFactory.decodeStream(in);
        return result;
    }

}
