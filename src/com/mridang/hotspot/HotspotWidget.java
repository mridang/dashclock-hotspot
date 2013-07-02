package com.mridang.hotspot;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Scanner;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class HotspotWidget extends DashClockExtension {

	/* This is the instance of the receiver that deals with hotspot status */
    private SRChangeReceiver objHotspotReceiver;

    /*
     * This class is the receiver for getting hotspot toggle events
     */
    private class SRChangeReceiver extends BroadcastReceiver {
       
    	/*
    	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
    	 */
        @Override
        public void onReceive(Context context, Intent intent) {
            
        	if (intent.getIntExtra("wifi_state", 0) == 13) {
        		
        		Log.v("HotspotWidget", "Hotspot enabled");
        		onUpdateData(1);
        		return;

        	}

        	if (intent.getIntExtra("wifi_state", 0) == 11) {

        		Log.v("HotspotWidget", "Hotspot disabled");
        		onUpdateData(1);
        		return;

        	}

        }

    }

    /*
     * @see com.google.android.apps.dashclock.api.DashClockExtension#onInitialize(boolean)
     */
    @Override
    protected void onInitialize(boolean booReconnect) {

    	super.onInitialize(booReconnect);

    	if (objHotspotReceiver != null) {
            try {

            	Log.d("HotspotWidget", "Unregistered any existing status receivers");
            	unregisterReceiver(objHotspotReceiver);

            } catch (Exception e) {
                e.printStackTrace();
            }

    	}

    	IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
    	objHotspotReceiver = new SRChangeReceiver();
        registerReceiver(objHotspotReceiver, intentFilter);
        Log.d("HotspotWidget", "Registered the status receiver");

    }

    /*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("HotspotWidget", "Created");
		BugSenseHandler.initAndStartSession(this, "17259500");

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int arg0) {

		setUpdateWhenScreenOn(true);

		Log.d("HotspotWidget", "Fetching hotspot connectivity information");
		ExtensionData edtInformation = new ExtensionData();
		edtInformation.visible(false);

		WifiManager wifManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		try {

			Log.d("HotspotWidget", "Checking if the hotspot is on");
			Method isWifiApEnabled = wifManager.getClass().getDeclaredMethod("isWifiApEnabled");

			if ((Boolean) isWifiApEnabled.invoke(wifManager)) {

				Log.d("HotspotWidget", "Counting connections through hotspot");
				Scanner scaAddresses = new Scanner(new File("/proc/net/arp"));

				Integer intConnections = 0;
				while (scaAddresses.hasNextLine()) {
					Log.v("HotspotWidget", scaAddresses.nextLine());
					intConnections++;
				}
				intConnections = intConnections - 1;

				scaAddresses.close();
				Log.d("HotspotWidget", intConnections + " or more devices connected");

				ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
				edtInformation.clickIntent(new Intent().setComponent(comp));
				
				edtInformation.expandedBody(getResources().getQuantityString(R.plurals.connection, intConnections, intConnections));
				edtInformation.visible(true);
				
				Method getWifiApConfiguration = wifManager.getClass().getDeclaredMethod("getWifiApConfiguration");
				edtInformation.status(((WifiConfiguration) getWifiApConfiguration.invoke(wifManager)).SSID);				

			} else {
				Log.d("HotspotWidget", "Hotspot is off");
			}

		} catch (Exception e) {
			Log.e("HotspotWidget", "Encountered an error", e);
			BugSenseHandler.sendException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		publishUpdate(edtInformation);
		Log.d("HotspotWidget", "Done");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onDestroy()
	 */
	public void onDestroy() {

		super.onDestroy();

		if (objHotspotReceiver != null) {

			try {

				Log.d("HotspotWidget", "Unregistered the status receiver");
				unregisterReceiver(objHotspotReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}

		Log.d("HotspotWidget", "Destroyed");
		BugSenseHandler.closeSession(this);

	}

}