package com.mridang.hotspot;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Scanner;

import org.acra.ACRA;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.android.apps.dashclock.api.ExtensionData;

/*
 * This class is the main class that provides the widget
 */
public class HotspotWidget extends ImprovedExtension {

	/*
	 * (non-Javadoc)
	 * @see com.mridang.battery.ImprovedExtension#getIntents()
	 */
	@Override
	protected IntentFilter getIntents() {
		return new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED");
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.battery.ImprovedExtension#getTag()
	 */
	@Override
	protected String getTag() {
		return getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.battery.ImprovedExtension#getUris()
	 */
	@Override
	protected String[] getUris() {
		return null;
	}

	/* This is the instance of the thread that keeps track of connected clients */
	private Thread thrPeriodicTicker;

	/*
	 * @see
	 * com.google.android.apps.dashclock.api.DashClockExtension#onUpdateData
	 * (int)
	 */
	@Override
	protected void onUpdateData(int intReason) {

		Log.d(getTag(), "Fetching hotspot connectivity information");
		final ExtensionData edtInformation = new ExtensionData();
		setUpdateWhenScreenOn(false);

		try {

			Log.d(getTag(), "Checking if the hotspot is on");
			final WifiManager wifManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			Method isWifiApEnabled = wifManager.getClass().getDeclaredMethod("isWifiApEnabled");
			Method getWifiApConfiguration = wifManager.getClass().getDeclaredMethod("getWifiApConfiguration");
			final WifiConfiguration wifSettings = (WifiConfiguration) getWifiApConfiguration.invoke(wifManager);
			final ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");

			if ((Boolean) isWifiApEnabled.invoke(wifManager)) {

				Log.d(getTag(), "Checking if the periodic ticker is enabled");
				if (thrPeriodicTicker == null) {

					Log.d(getTag(), "Starting a new periodic ticker to check neighbours");
					thrPeriodicTicker = new Thread() {

						public void run() {

							for (;;) {

								try {

									Log.d(getTag(), "Counting connections through hotspot");
									Scanner scaAddresses = new Scanner(new File("/proc/net/arp"));

									Integer intConnections = 0;
									while (scaAddresses.hasNextLine()) {

										Log.v(getTag(), scaAddresses.nextLine());
										intConnections++;

									}
									intConnections = intConnections - 1;
									scaAddresses.close();
									Log.d(getTag(), intConnections + " or more devices connected");

									if (wifSettings.SSID == null) {
										edtInformation.expandedTitle(getString(R.string.ssid_missing));
									} else {
										edtInformation.expandedTitle(wifSettings.SSID);
									}
									
									edtInformation.expandedBody(getQuantityString(R.plurals.connection, intConnections, intConnections));
									edtInformation.clickIntent(new Intent().setComponent(comp));
									edtInformation.icon(R.drawable.ic_dashclock);
									edtInformation.status(intConnections.toString());
									edtInformation.visible(intConnections > 0 || getBoolean("always", true));

									doUpdate(edtInformation);

									Thread.sleep(30000);

								} catch (InterruptedException e) {
									Log.d(getTag(), "Stopping the periodic checker.");
									return;
								} catch (Exception e) {
									Log.e(getTag(), "Encountered an error", e);
									ACRA.getErrorReporter().handleSilentException(e);
								}

							}

						}

					};

					thrPeriodicTicker.start();

				}

			} else {
				Log.d(getTag(), "Hotspot is off");
				if (thrPeriodicTicker != null)
					thrPeriodicTicker.interrupt();
			}

		} catch (Exception e) {
			edtInformation.visible(false);
			Log.e(getTag(), "Encountered an error", e);
			ACRA.getErrorReporter().handleSilentException(e);
		}

		edtInformation.icon(R.drawable.ic_dashclock);
		doUpdate(edtInformation);

	}

	/*
	 * (non-Javadoc)
	 * @see com.mridang.alarmer.ImprovedExtension#onReceiveIntent(android.content.Context, android.content.Intent)
	 */
	@Override
	protected void onReceiveIntent(Context ctxContext, Intent ittIntent) {
		onUpdateData(UPDATE_REASON_MANUAL);
	}

}