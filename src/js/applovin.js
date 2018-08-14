import exec from 'cordova/exec'

const PLUGIN = 'AppLovinPlugin';

export function isReady(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'isReady', []);
}

export function loadVideoAd(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'loadVideoAd', []);
}

export function showVideoAd(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'showVideoAd', []);
}

export function trackEvent(event, params, successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'trackEvent', [event, params]);
}

