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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class GetBridgeTask extends AsyncTask <Void, Void, String> {
	private static final String TAG = "GetBridgeTask";
	
	private ResultListener listener;
	
	public GetBridgeTask(ResultListener listener) {
		this.listener = listener;
	}
	
	protected String doInBackground(Void... voids) {
		Log.d(TAG, "Beginning lookup");
		DefaultHttpClient httpClient = new DefaultHttpClient(new BasicHttpParams());
		HttpGet get = new HttpGet("https://www.meethue.com/api/nupnp");
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
			
			JSONArray arr = new JSONArray(sb.toString());
			if (arr.length() > 0) {
				JSONObject json = arr.getJSONObject(0);
				result = json.getString("internalipaddress");
			} else {
				Log.d(TAG, "No bridges found");
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
	
	protected void onPostExecute(String ip) {
		if ((ip != null) && !ip.isEmpty()) {
			Log.d(TAG, "Bridge IP is: " + ip);
			listener.notifyResult(ip);
		} else {
			Log.e(TAG, "Unable to get bridge IP");
			listener.notifyError();
		}
	}
	
	public interface ResultListener {
		void notifyResult(String result);
		void notifyError();
	}
}
