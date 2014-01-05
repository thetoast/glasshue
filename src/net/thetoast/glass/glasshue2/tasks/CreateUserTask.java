package net.thetoast.glass.glasshue2.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class CreateUserTask extends AsyncTask <Void, Void, String> {
	private static final String TAG = "CreateUserTask";
	
	private ResultListener listener;
	private String bridgeAddr;
	
	public CreateUserTask(String bridgeAddr, ResultListener listener) {
		this.listener = listener;
		this.bridgeAddr = bridgeAddr;
	}
	
	protected String doInBackground(Void... voids) {
		Log.d(TAG, "Creating user");
		DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
		HttpPost post = new HttpPost("http://" + bridgeAddr + "/api");
		InputStream is = null;
		String result = null;
		
		try {
			JSONObject recData = new JSONObject();
			recData.put("devicetype", "GlassHue");
			post.setEntity(new StringEntity(recData.toString()));
			
			HttpResponse resp = httpClient.execute(post);
			HttpEntity entity = resp.getEntity();
			
			is = entity.getContent();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String line = null;
			
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			
			JSONArray arr = new JSONArray(sb.toString());
			if (arr.length() > 0) {
				JSONObject json = arr.getJSONObject(0);
				if (json.has("success")) {
					result = json.getJSONObject("success").getString("username");
				} else {
					Log.e(TAG, "Unable to create user: " + json.getJSONObject("error").getString("description"));
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
	
	protected void onPostExecute(String apiUser) {
		if ((apiUser != null) && !apiUser.isEmpty()) {
			Log.d(TAG, "API User is: " + apiUser);
			listener.notifyResult(apiUser);
		} else {
			Log.e(TAG, "Unable to create API User");
			listener.notifyError();
		}
	}
	
	public interface ResultListener {
		void notifyResult(String result);
		void notifyError();
	}
}
