package com.mridang.hotspot;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Scanner;

import org.acra.ACRA;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class HotspotWidget extends DashClockExtension {

	/*
	 * This class is the receiver for getting hotspot toggle events
	 */
	private class ToggleReceiver extends BroadcastReceiver {

		/*
		 * @see
		 * android.content.BroadcastReceiver#onReceive(android.content.Context,
		 * android.content.Intent)
		 */
		@Override
		public void onReceive(Context ctxContext, Intent ittIntent) {

			if (ittIntent.getIntExtra("wifi_state", 0) == 13) {

				Log.v("HotspotWidget", "Hotspot enabled");
				onUpdateData(1);
				return;

			}

			if (ittIntent.getIntExtra("wifi_state", 0) == 11) {

				Log.v("HotspotWidget", "Hotspot disabled");
				onUpdateData(1);
				return;

			}

		}

	}

	/* This is the instance of the receiver that deals with hotspot status */
	private ToggleReceiver objHotspotReceiver;
	/* This is the instance of the thread that keeps track of connected clients */
	private Thread thrPeriodicTicker;

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onInitialize
	 * (boolean)
	 */
	@Override
	protected void onInitialize(boolean booReconnect) {

		super.onInitialize(booReconnect);

		if (objHotspotReceiver != null) {

			try {

				Log.d("HotspotWidget", "Unregistering any existing status receivers");
				unregisterReceiver(objHotspotReceiver);

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		IntentFilter intentFilter = new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
		objHotspotReceiver = new ToggleReceiver();
		registerReceiver(objHotspotReceiver, intentFilter);
		Log.d("HotspotWidget", "Registered the status receiver");

	}

	/*
	 * @see com.google.android.apps.dashclock.api.DashClockExtension#onCreate()
	 */
	public void onCreate() {

		super.onCreate();
		Log.d("HotspotWidget", "Created");
		ACRA.init(new AcraApplication(getApplicationContext()));

	}

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		Log.d("HotspotWidget", "Fetching hotspot connectivity information");
		final ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(false);

		final WifiManager wifManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		try {

			Log.d("HotspotWidget", "Checking if the hotspot is on");
			Method isWifiApEnabled = wifManager.getClass().getDeclaredMethod("isWifiApEnabled");

			if ((Boolean) isWifiApEnabled.invoke(wifManager)) {

				Log.d("HotspotWidget", "Checking if the periodic ticker is enabled");
				if (thrPeriodicTicker == null) {

					Log.d("HotspotWidget", "Starting a new periodic ticker to check neighbours");
					thrPeriodicTicker = new Thread() {

						public void run() {

							for (;;) {

								try {

									ComponentName comp = new ComponentName("com.android.settings",
											"com.android.settings.TetherSettings");

									edtInformation.clickIntent(new Intent().setComponent(comp));
									edtInformation.icon(R.drawable.ic_dashclock);

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

									edtInformation.expandedBody(getResources().getQuantityString(R.plurals.connection,
											intConnections, intConnections));
									Method getWifiApConfiguration = wifManager.getClass().getDeclaredMethod(
											"getWifiApConfiguration");
									WifiConfiguration wifSettings = (WifiConfiguration) getWifiApConfiguration
											.invoke(wifManager);
									edtInformation
											.expandedTitle(wifSettings.SSID == null ? getString(R.string.ssid_missing)
													: wifSettings.SSID);
									edtInformation.status(intConnections.toString());
									edtInformation.visible(intConnections > 0 ? true : PreferenceManager
											.getDefaultSharedPreferences(getApplicationContext()).getBoolean("always",
													true));

									publishUpdate(edtInformation);

									Thread.sleep(30000);

								} catch (InterruptedException e) {
									Log.d("HotspotWidget", "Stopping the periodic checker.");
									return;
								} catch (Exception e) {
									Log.e("HotspotWidget", "Encountered an error", e);
									ACRA.getErrorReporter().handleSilentException(e);
								}

							}

						}

					};

					thrPeriodicTicker.start();

				}

			} else {
				Log.d("HotspotWidget", "Hotspot is off");
				if (thrPeriodicTicker != null)
					thrPeriodicTicker.interrupt();
			}

			if (new Random().nextInt(5) == 0 && !(0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))) {

				PackageManager mgrPackages = getApplicationContext().getPackageManager();

				try {

					mgrPackages.getPackageInfo("com.mridang.donate", PackageManager.GET_META_DATA);

				} catch (NameNotFoundException e) {

					Integer intExtensions = 0;
					Intent ittFilter = new Intent("com.google.android.apps.dashclock.Extension");
					String strPackage;

					for (ResolveInfo info : mgrPackages.queryIntentServices(ittFilter, 0)) {

						strPackage = info.serviceInfo.applicationInfo.packageName;
						intExtensions = intExtensions + (strPackage.startsWith("com.mridang.") ? 1 : 0);

					}

					if (intExtensions > 1) {

						edtInformation.visible(true);
						edtInformation.clickIntent(new Intent(Intent.ACTION_VIEW).setData(Uri
								.parse("market://details?id=com.mridang.donate")));
						edtInformation.expandedTitle("Please consider a one time purchase to unlock.");
						edtInformation
								.expandedBody("Thank you for using "
										+ intExtensions
										+ " extensions of mine. Click this to make a one-time purchase or use just one extension to make this disappear.");
						setUpdateWhenScreenOn(true);

					}

				}

			} else {
				setUpdateWhenScreenOn(false);
			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e("HotspotWidget", "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
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

	}

}