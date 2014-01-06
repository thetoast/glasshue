package net.thetoast.glass.glasshue.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class TestConnectionTask extends AsyncTask <Void, Void, String> {
	private static final String TAG = "TestConnectionTask";
	
	private ResultListener listener;
	private String bridgeAddr;
	private String apiUser;
	
	public TestConnectionTask(String bridgeAddr, String apiUser, ResultListener listener) {
		this.listener = listener;
		this.bridgeAddr = bridgeAddr;
		this.apiUser = apiUser;
	}
	
	protected String doInBackground(Void... voids) {
		Log.d(TAG, "Testing connection");
		DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
		HttpGet get = new HttpGet("http://" + bridgeAddr + "/api/" + apiUser + "/config");
		InputStream is = null;
		String result = null;
		
		try {			
			HttpResponse resp = httpClient.execute(get);
			HttpEntity entity = resp.getEntity();
			
			is = entity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line = null;
			
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			
			JSONObject json = new JSONObject(sb.toString());				
			if (json.has("name")) {
				result = json.getString("name");
			} else {
				Log.e(TAG, "Unable to get bridge name: " + json.getJSONObject("error").getString("description"));
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
	
	protected void onPostExecute(String result) {
		if ((result != null) && !result.isEmpty()) {
			Log.d(TAG, "Bridge name is: " + result);
			listener.notifyResult(result);
		} else {
			Log.e(TAG, "Unable to get bridge name");
			listener.notifyError();
		}
	}
	
	public interface ResultListener {
		void notifyResult(String result);
		void notifyError();
	}
}
