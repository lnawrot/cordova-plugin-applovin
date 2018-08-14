//Copyright (c) 2018 Łukasz Nawrot
//Email: lukasz@nawrot.me
//License: MIT (http://opensource.org/licenses/MIT)

package me.nawrot.cordova.plugin.applovin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.util.Log;

import android.util.Log;
import android.view.View;
import java.util.Map;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;

import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.sdk.AppLovinEventParameters;
import com.applovin.sdk.AppLovinEventService;
import com.applovin.sdk.AppLovinEventTypes;

public class AppLovinPlugin extends CordovaPlugin {
	private static final String LOG_TAG = "AppLovinPlugin";
	private AppLovinIncentivizedInterstitial myIncent = null;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);

		AppLovinSdk.initializeSdk(cordova.getActivity().getApplicationContext());
		Log.d(LOG_TAG, "SDK Initialized");
	}

	@Override
	public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
    PluginResult result = null;

		if (Actions.IS_READY.equals(action)) {
			result = isReady(callbackContext);

		} else if (Actions.LOAD_VIDEO.equals(action)) {
			result = loadVideoAd(callbackContext);

		} else if (Actions.SHOW_VIDEO.equals(action)) {
			result = showVideoAd(callbackContext);

		} else if (Actions.TRACK_EVENT.equals(action)) {
			String event = inputs.getString(0);
			JSONArray parameters = inputs.optJSONArray(1);
			result = trackEvent(event, parameters, callbackContext);

		} else {
			Log.d(LOG_TAG, String.format("Invalid action passed: %s", action));
			result = new PluginResult(Status.INVALID_ACTION);
		}

		if (result != null) {
			callbackContext.sendPluginResult(result);
		}

		return true;
	}

	private PluginResult isReady(final CallbackContext callbackContext) {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				boolean result = myIncent != null && myIncent.isAdReadyToDisplay();
				callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, result));
			}
		});

		return null;
	}

	private PluginResult loadVideoAd(final CallbackContext callbackContext) {
		Log.d(LOG_TAG, "loadVideoAd");
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_loadVideoAd();
			}
		});
		callbackContext.success();

		return null;
	}

	private void _loadVideoAd() {
		Log.d(LOG_TAG, "_loadVideoAd");
		myIncent = AppLovinIncentivizedInterstitial.create(cordova.getActivity().getApplicationContext());
		Log.d(LOG_TAG, "AppLovinIncentivizedInterstitial.create");
		myIncent.preload(new AppLovinAdLoadListener() {
				@Override
				public void adReceived(AppLovinAd ad) {
					Log.d(LOG_TAG, "adReceived");

					JSONObject data = new JSONObject();
					try {
						data.put("id", ad.getAdIdNumber());
					} catch (JSONException e) {
						e.printStackTrace();
					}
					fireEvent("applovin.load", data);
				}
				@Override
				public void failedToReceiveAd(int errorCode) {
					Log.d(LOG_TAG, String.format("failedToReceiveAd: %d", errorCode));

					JSONObject data = new JSONObject();
					try {
						data.put("error", errorCode);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					fireEvent("applovin.load_error", data);
				}
		});
	}

	private PluginResult showVideoAd(final CallbackContext callbackContext) {
		final CallbackContext delayCallback = callbackContext;
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				boolean result = _showVideoAd();
				if (result) {
					delayCallback.success();
				} else {
					delayCallback.error("Ad is not ready!");
				}
			}
		});

		return null;
	}

	private boolean _showVideoAd() {
		if (myIncent == null || !myIncent.isAdReadyToDisplay()) {
			return false;
		}

		myIncent.show(
			cordova.getActivity(),
			new AppLovinAdRewardListener() {
				@Override
				public void userDeclinedToViewAd(final AppLovinAd appLovinAd)
				{
						Log.d(LOG_TAG, "User declined to view rewarded video" );
						fireEvent("applovin.close", null);
				}

				@Override
				public void userRewardVerified(final AppLovinAd ad, final Map map)
				{
						final String currency = (String) map.get( "currency" );
						final String amountStr = (String) map.get( "amount" );
						final int amount = (int) Double.parseDouble( amountStr ); // AppLovin returns amount as double

						Log.d(LOG_TAG, "Verified " + amount + " " + currency );

						JSONObject data = new JSONObject();
						try {
							data.put("amount", amount);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						fireEvent("applovin.reward", data);
				}
				@Override
		    public void userOverQuota(final AppLovinAd appLovinAd, final Map map)
		    {
		        Log.d(LOG_TAG, "Rewarded video validation request for ad did exceed quota with response: " + map );
		    }

		    @Override
		    public void validationRequestFailed(final AppLovinAd appLovinAd, final int errorCode)
		    {
		        Log.d(LOG_TAG, "Rewarded video validation request for ad failed with error code: " + errorCode );
		    }

		    @Override
		    public void userRewardRejected(final AppLovinAd appLovinAd, final Map map)
		    {
		        Log.d(LOG_TAG, "Rewarded video validation request was rejected with response: " + map );
		    }
			},
			new AppLovinAdVideoPlaybackListener() {
				@Override
		    public void videoPlaybackBegan(AppLovinAd ad)
		    {
		        Log.d(LOG_TAG, "Rewarded video playback began" );
		    }

				@Override
				public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched)
				{
					if (fullyWatched) {
						fireEvent("applovin.completed", null);
					}
				}
			},
			new AppLovinAdDisplayListener() {
				@Override
				public void adDisplayed(AppLovinAd appLovinAd) {
					fireEvent("applovin.start", null);
				}
				@Override
				public void adHidden(AppLovinAd appLovinAd) {
					fireEvent("applovin.close", null);
				}
			}
		);

		return true;
	}

	private PluginResult trackEvent(final String event, final JSONArray data, CallbackContext callbackContext) {
		final AppLovinEventService eventService = AppLovinSdk.getInstance(cordova.getActivity()).getEventService();

		Map<String, String> parameters = new HashMap<String, String>();
		String eventName;
		if (event.equalsIgnoreCase("USER_COMPLETED_LEVEL")) {
			eventName = AppLovinEventTypes.USER_COMPLETED_LEVEL;
			parameters.put(AppLovinEventParameters.COMPLETED_LEVEL_IDENTIFIER, data.optString(0, "0"));

		} else if (event.equalsIgnoreCase("USER_COMPLETED_IN_APP_PURCHASE")) {
			eventName = AppLovinEventTypes.USER_COMPLETED_IN_APP_PURCHASE;
			parameters.put(AppLovinEventParameters.REVENUE_AMOUNT, data.optString(0, "0"));
			parameters.put(AppLovinEventParameters.REVENUE_CURRENCY, data.optString(1, "USD"));
			parameters.put(AppLovinEventParameters.IN_APP_PURCHASE_TRANSACTION_IDENTIFIER, data.optString(2, ""));

		} else if (event.equalsIgnoreCase("USER_SPENT_VIRTUAL_CURRENCY")) {
			eventName = AppLovinEventTypes.USER_SPENT_VIRTUAL_CURRENCY;
			parameters.put(AppLovinEventParameters.VIRTUAL_CURRENCY_AMOUNT, data.optString(0, "0"));
			parameters.put(AppLovinEventParameters.VIRTUAL_CURRENCY_NAME, data.optString(1, ""));

		} else if (event.equalsIgnoreCase("USER_COMPLETED_TUTORIAL")) {
			eventName = AppLovinEventTypes.USER_COMPLETED_TUTORIAL;

		} else if (event.equalsIgnoreCase("USER_VIEWED_PRODUCT")) {
			eventName = AppLovinEventTypes.USER_VIEWED_PRODUCT;
			parameters.put(AppLovinEventParameters.PRODUCT_IDENTIFIER, data.optString(0, ""));

		} else if (event.equalsIgnoreCase("USER_LOGGED_IN")) {
			eventName = AppLovinEventTypes.USER_LOGGED_IN;

		} else {
			return null;
		}

		eventService.trackEvent(eventName, parameters);

		return null;
	}

  private void fireEvent(String eventName, JSONObject jsonObj) {
    String data = "";
    if (jsonObj != null) {
      data = jsonObj.toString();
    }

    StringBuilder js = new StringBuilder();
    js.append("javascript:cordova.fireDocumentEvent('");
    js.append(eventName);
    js.append("'");
    if (data != null && !"".equals(data)) {
      js.append(",");
      js.append(data);
    }
    js.append(");");

    final String code = js.toString();

    cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
    		webView.loadUrl(code);
			}
		});
  }
}
