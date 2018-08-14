'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var exec = _interopDefault(require('cordova/exec'));

var PLUGIN = 'AppLovinPlugin';

function isReady(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'isReady', []);
}

function loadVideoAd(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'loadVideoAd', []);
}

function showVideoAd(successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'showVideoAd', []);
}

function trackEvent(event, params, successCallback, failureCallback) {
  exec(successCallback, failureCallback, PLUGIN, 'trackEvent', [event, params]);
}

exports.isReady = isReady;
exports.loadVideoAd = loadVideoAd;
exports.showVideoAd = showVideoAd;
exports.trackEvent = trackEvent;
//# sourceMappingURL=applovin.js.map
