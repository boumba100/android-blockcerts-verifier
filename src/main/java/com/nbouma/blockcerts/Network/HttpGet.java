package com.nbouma.blockcerts.Network;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by noah on 01/05/17.
 */

public class HttpGet extends AsyncTask<Void, Void, Void> {
    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    private String protocol = HTTP;
    private URL url;
    private OnComplete listener;
    private String receivedData;

    public HttpGet(String destination, OnComplete listener) {
        this.listener = listener;
        try {
            this.url = new URL(destination);
            this.protocol = this.url.getProtocol();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public interface OnComplete {
        void OnComplete(String data);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        this.receivedData = this.getFromUrl();
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        this.listener.OnComplete(this.receivedData);
    }

    private String getFromUrl() {
        InputStream inputStream = null;
        if (this.protocol.equals(HTTP)) {
            try {
                HttpURLConnection urlConnection = (HttpURLConnection) this.url.openConnection();
                inputStream = urlConnection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (this.protocol.equals(HTTPS)) {
            try {
                HttpsURLConnection urlConnection = (HttpsURLConnection) this.url.openConnection();
                inputStream = urlConnection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return readFromInputStream(inputStream);
    }


    private static String readFromInputStream(InputStream inputStream) {
        StringBuffer buffer = new StringBuffer();
        String line = "";
        if(inputStream == null) {
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return buffer.toString();
    }

}













