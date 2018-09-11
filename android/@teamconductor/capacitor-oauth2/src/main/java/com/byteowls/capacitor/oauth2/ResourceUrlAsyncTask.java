package com.byteowls.capacitor.oauth2;

import android.os.AsyncTask;
import android.util.Log;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author m.oberwasserlechner@byteowls.com
 */
public class ResourceUrlAsyncTask extends AsyncTask<String, Void, ResourceCallResult> {

    private PluginCall pluginCall;
    private String logTag;

    public ResourceUrlAsyncTask(PluginCall pluginCall, String logTag) {
        this.pluginCall = pluginCall;
        this.logTag = logTag;
    }

    @Override
    protected ResourceCallResult doInBackground(String... tokens) {
        String resourceUrl = pluginCall.getString("resourceUrl");
        try {
            URL url = new URL(resourceUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.addRequestProperty("Authorization", String.format("Bearer %s", tokens[0]));
            try {
                InputStream is;

                ResourceCallResult result = new ResourceCallResult();
                if (conn.getResponseCode() >= HttpURLConnection.HTTP_OK
                    && conn.getResponseCode() < HttpURLConnection.HTTP_MULT_CHOICE) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                    result.setError(true);
                }
                String jsonBody = readInputStream(is);
                if (!result.isError()) {
                    Log.i(logTag, String.format("User Info Response %s", jsonBody));
                    result.setResponse(new JSObject(jsonBody));
                } else {
                    result.setErrorMsg(jsonBody);
                }
                return result;
            } catch (JSONException e) {
                Log.e(logTag, "Resource response no valid json.", e);
            } finally {
                conn.disconnect();
            }
        } catch (MalformedURLException e) {
            Log.e(logTag, "Invalid resource url '" + resourceUrl + "'", e);
        } catch (IOException e) {
            Log.e(logTag, "Unexpected error", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(ResourceCallResult response) {
        if (!response.isError()) {
            pluginCall.resolve(response.getResponse());
        } else {
            pluginCall.reject(response.getErrorMsg());
        }
    }

    private static String readInputStream(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        char[] buffer = new char[1024];
        StringBuilder sb = new StringBuilder();
        int readCount;
        while ((readCount = br.read(buffer)) != -1) {
            sb.append(buffer, 0, readCount);
        }
        return sb.toString();
    }

}