package eecs588.prizevote;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;


public class HTTPFilePoster {

    public HTTPFilePoster(final File data, final String user, final String serverURL) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL(serverURL+"?user="+user).openConnection();
                    httpUrlConnection.setDoOutput(true);
                    httpUrlConnection.setRequestMethod("POST");
                    OutputStream os = httpUrlConnection.getOutputStream();
                    Thread.sleep(5000);
                    BufferedInputStream fis = new BufferedInputStream(new FileInputStream(data));

                    while (fis.available() > 0) {
                        os.write(fis.read());
                    }

                    os.close();
                    fis.close();

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(
                                    httpUrlConnection.getInputStream()));
                    String s = null;
                    while ((s = in.readLine()) != null) {
                        if(s.equals("Success")) {
                            Log.i("HTTPFilePoster", "Success");
                            in.close();
                            return;
                        }
                    }
                    Log.i("HTTPFilePoster", "Failure");
                    in.close();
                } catch (Exception e) {
                    Log.i("HTTPFilePoster", "Failure: " + e.getMessage());
                }
            }
        }).start();
    }

}
