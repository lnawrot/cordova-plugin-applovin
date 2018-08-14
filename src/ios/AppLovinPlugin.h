#import <Cordova/CDV.h>
#import <AppLovinSDK/AppLovinSDK.h>

@interface AppLovinPlugin : CDVPlugin

- (void) isReady:(CDVInvokedUrlCommand*)command;
- (void) loadVideoAd:(CDVInvokedUrlCommand*)command;
- (void) showVideoAd:(CDVInvokedUrlCommand*)command;
- (void) trackEvent:(CDVInvokedUrlCommand*)command;

@end
