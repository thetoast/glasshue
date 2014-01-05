package net.thetoast.glass.glasshue2;

import java.util.ArrayList;

import net.thetoast.glass.glasshue2.tasks.CreateUserTask;
import net.thetoast.glass.glasshue2.tasks.FlashLightsTask;
import net.thetoast.glass.glasshue2.tasks.GetBridgeTask;
import net.thetoast.glass.glasshue2.tasks.TestConnectionTask;
import net.thetoast.glass.glasshue2.tasks.ToggleLightsTask;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String PREFS_NAME = "GlassHuePrefs";

	private String bridgeAddr;
	private String apiUser;
	private String alias;
	private boolean connected;
	private boolean doConnect;
	private boolean hasCommand;
	private boolean instructionsError;
	private boolean quitAfterCommand;
	private SharedPreferences prefs;
	private int instructions;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// get voice command alias
        String className = getComponentName().getClassName();
        alias = className.substring(className.lastIndexOf(".") + 1);
        
		prefs = getSharedPreferences(PREFS_NAME, 0);
		bridgeAddr = prefs.getString("bridgeAddr", null);
		apiUser = prefs.getString("apiUser", null);
		
		setContentView(R.layout.activity_main);
		updateView();
		
		if ((bridgeAddr != null) && (apiUser != null)) {
			testConnection(alias.equals("test_connection") ? true : false);
		}
		
		if (alias.equals("flash") || alias.equals("on") || alias.equals("off")) {
			hasCommand = true;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
	        case R.id.connect:
	        	if (doConnect) {
	        		doConnect = false;
		        	instructions = R.string.instructions_connecting;
		        	beginConnecting();
	        	} else {
		        	instructions = R.string.instructions_connect;
		        	doConnect = true;
	        	}
	        	instructionsError = false;
	        	updateView();
	            return true;
	        case R.id.forget:
	        	bridgeAddr = null;
	        	apiUser = null;
				prefs.edit()
					.remove("bridgeAddr")
					.remove("apiUser")
					.commit();
				updateView();
	        	return true;
	        case R.id.test:
	        	testConnection(true);
	        	return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
          if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
              openOptionsMenu();
              return true;
          } else if (keyCode == KeyEvent.KEYCODE_BACK) {
        	  // RM -- TODO: figure out how to get the app
        	  // to quit with the "whoosh" sound without this.
        	  // after opening options menu, the app doesn't seem
        	  // to quit normally with a back swipe...
        	  finish();
          }
          return false;
    }
	
	private void runFlashLightCommand() {
		ArrayList<String> voiceResults = getIntent().getExtras()
		        .getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
		String speech = voiceResults.get(0);
		String[] args = speech.split(" ");
		int id=-1;
		
		if (args.length > 1) {
			if (args[1].equals("one")) {
				// Glass will transcribe the number 1 as "one"
				id = 1;
			} else if (args[1].equals("to") || args[1].equals("too")) {
				id = 2;
			} else {
				try {
					id = Integer.parseInt(args[1]);
				} catch (NumberFormatException nfr) {
					instructions = R.string.instructions_err_bad_id;
					instructionsError = true;
					updateView();
					return;
				}
			}
		}

		if (args[0].equals("light")) {
			if (id < 0) {
				instructions = R.string.instructions_err_not_enough_args;
				instructionsError = true;
			} else {
				quitAfterCommand = true;
				flashLights(id, false);
			}
		} else if (args[0].equals("group")) {
			if (id < 0) {
				instructions = R.string.instructions_err_not_enough_args;
				instructionsError = true;
			} else {
				quitAfterCommand = true;
				flashLights(id, true);
			}
		} else if (args[0].equals("all")) {
			quitAfterCommand = true;
			flashLights(0, true);
		} else {
			instructions = R.string.instructions_err_bad_flash_target;
			instructionsError = true;
		}
		
		updateView();
	}
	
	private void runToggleLightCommand(boolean on) {
		ArrayList<String> voiceResults = getIntent().getExtras()
		        .getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
		String speech = voiceResults.get(0);
		String[] args = speech.split(" ");
		int id=-1;
		
		if (args.length > 1) {
			if (args[1].equals("one")) {
				// Glass will transcribe the number 1 as "one"
				id = 1;
			} else if (args[1].equals("to") || args[1].equals("too")) {
				id = 2;
			} else {
				try {
					id = Integer.parseInt(args[1]);
				} catch (NumberFormatException nfr) {
					instructions = R.string.instructions_err_bad_id;
					instructionsError = true;
					updateView();
					return;
				}
			}
		}

		if (args[0].equals("light")) {
			if (id < 0) {
				instructions = R.string.instructions_err_not_enough_args;
				instructionsError = true;
			} else {
				quitAfterCommand = true;
				toggleLights(id, false, on);
			}
		} else if (args[0].equals("group")) {
			if (id < 0) {
				instructions = R.string.instructions_err_not_enough_args;
				instructionsError = true;
			} else {
				quitAfterCommand = true;
				toggleLights(id, true, on);
			}
		} else if (args[0].equals("all")) {
			quitAfterCommand = true;
			toggleLights(0, true, on);
		} else {
			instructions = R.string.instructions_err_bad_flash_target;
			instructionsError = true;
		}
		
		updateView();
	}
	
	private void beginConnecting() {
		new GetBridgeTask(new GetBridgeTask.ResultListener() {
			
			@Override
			public void notifyResult(String result) {
				bridgeAddr = result;
				prefs.edit().putString("bridgeAddr", bridgeAddr).commit();
				updateView();
				beginCreatingUser();
			}
			
			@Override
			public void notifyError() {
	        	instructions = R.string.instructions_err_no_bridge;
	        	instructionsError = true;
			}
		}).execute();
	}
	
	private void beginCreatingUser() {
		new CreateUserTask(bridgeAddr, new CreateUserTask.ResultListener() {
			
			@Override
			public void notifyResult(String result) {
				apiUser = result;
				prefs.edit().putString("apiUser", apiUser).commit();
				updateView();
				testConnection(true);
			}
			
			@Override
			public void notifyError() {
				instructions = R.string.instructions_err_create_user;
				instructionsError = true;
			}
		}).execute();
	}
	
	private void testConnection(final boolean flash) {
		new TestConnectionTask(bridgeAddr, apiUser, new TestConnectionTask.ResultListener() {
			
			@Override
			public void notifyResult(String result) {
				connected = true;
				instructions = 0;
				instructionsError = false;
				updateView();
				
				if (flash) {
					flashLights(0, true);
				} else if (hasCommand) {
					if (alias.equals("flash")) {
						runFlashLightCommand();
					} if (alias.equals("on") || alias.equals("off")) {
						runToggleLightCommand(alias.equals("on"));
					}
				}
			}
			
			@Override
			public void notifyError() {
				connected = false;
				updateView();				
			}
		}).execute();
	}
	
	private void flashLights(int id, boolean isGroup) {
		new FlashLightsTask(bridgeAddr, apiUser, id, isGroup, new FlashLightsTask.ResultListener() {
			
			@Override
			public void notifyResult(Boolean success) {
				if (quitAfterCommand) {
					finish();
				}
			}
			
			@Override
			public void notifyError() {
				instructions = R.string.instructions_err_command_flash;
				instructionsError = true;
			}
		}).execute();
	}
	
	private void toggleLights(int id, boolean isGroup, boolean on) {
		new ToggleLightsTask(bridgeAddr, apiUser, id, isGroup, on, new ToggleLightsTask.ResultListener() {
			
			@Override
			public void notifyResult(Boolean success) {
				if (quitAfterCommand) {
					finish();
				}
			}
			
			@Override
			public void notifyError() {
				instructions = R.string.instructions_err_command_flash;
				instructionsError = true;
			}
		}).execute();
	}

	private void updateView() {
		TextView bridgeText = (TextView)findViewById(R.id.bridgeAddr);
		TextView statusText = (TextView)findViewById(R.id.status);
		TextView instructionsText = (TextView)findViewById(R.id.instructions);
		
		// set bridge addr field
		if (bridgeAddr != null) {
			bridgeText.setTextColor(Color.WHITE);
			bridgeText.setText(bridgeAddr);
		} else {
			bridgeText.setTextColor(Color.RED);
			bridgeText.setText(R.string.no_bridge);
		}
		
		// set status field
		if (connected) {
			statusText.setText(R.string.status_connected);
			statusText.setTextColor(Color.GREEN);
		} else {
			statusText.setTextColor(Color.RED);
			if (apiUser != null) {
				statusText.setText(R.string.status_not_connected);
			} else if (bridgeAddr != null) {
				statusText.setText(R.string.status_not_registered);
			} else {
				statusText.setText(R.string.status_not_associated);
			}
		}
		
		// set instructions field
		if (instructions > 0) {
			instructionsText.setText(instructions);
			if (instructionsError) {
				instructionsText.setTextColor(Color.RED);
			} else {
				instructionsText.setTextColor(Color.WHITE);
			}
		} else {
			instructionsText.setText(null);
		}
	}
}
