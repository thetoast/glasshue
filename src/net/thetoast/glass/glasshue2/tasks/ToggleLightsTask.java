package net.thetoast.glass.glasshue2.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class ToggleLightsTask extends AsyncTask <Void, Void, Boolean> {
	private static final String TAG = "ToggleLightsTask";
	
	private ResultListener listener;
	private String bridgeAddr;
	private String apiUser;
	private int id;
	private boolean isGroup;
	private boolean on;
	
	public ToggleLightsTask(String bridgeAddr, String apiUser, int id, boolean isGroup, boolean on, ResultListener listener) {
		this.listener = listener;
		this.bridgeAddr = bridgeAddr;
		this.apiUser = apiUser;
		this.id = id;
		this.isGroup = isGroup;
		this.on = on;
	}
	
	protected Boolean doInBackground(Void... voids) {
		Log.d(TAG, "Toggling lights");
		DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
		StringBuilder sb = new StringBuilder();
		sb.append("http://").append(bridgeAddr).append("/api/").append(apiUser)
		  .append(isGroup ? "/groups/" : "/lights/").append(id)
		  .append(isGroup ? "/action" : "/state");
		HttpPut put = new HttpPut(sb.toString());
		InputStream is = null;
		boolean result = false;
		
		try {
			JSONObject recData = new JSONObject();
			recData.put("on", on);
			put.setEntity(new StringEntity(recData.toString()));
			
			HttpResponse resp = httpClient.execute(put);
			HttpEntity entity = resp.getEntity();
			
			is = entity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			sb = new StringBuilder();
			String line = null;
			
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			
			JSONArray arr = new JSONArray(sb.toString());
			if (arr.length() > 0) {
				JSONObject json = arr.getJSONObject(0);
				if (json.has("success")) {
					result = true;
				} else {
					Log.e(TAG, "Unable to toggle lights: " + json.getJSONObject("error").getString("description"));
				}
			} else {
				Log.e(TAG, "No respose found");
			}
		} catch (ClientProtocolException cpe) {
			Log.e(TAG, "ClientProtocolException: " + cpe.toString());
		} catch (IOException ioe) {
			Log.e(TAG, "IOException: " + ioe.toString());
		} catch (JSONException jsone) {
			Log.e(TAG, "JSONException: " + jsone.toString());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException buried) { }
			}
		}
		
		return result;
	}
	
	protected void onPostExecute(Boolean success) {
		if (success) {
			listener.notifyResult(success);
		} else {
			Log.e(TAG, "Unable to toggle lights");
			listener.notifyError();
		}
	}
	
	public interface ResultListener {
		void notifyResult(Boolean success);
		void notifyError();
	}
}
