#import "AppLovinPlugin.h"

@implementation AppLovinPlugin

- (void) pluginInitialize {
  [super pluginInitialize];
  [ALSdk initializeSdk];
  NSLog(@"AppLovin: SDK initialized");
}

- (void) trackEvent: (CDVInvokedUrlCommand*)command {
    ALEventService* eventService = [ALSdk shared].eventService;

    NSString* event = [command argumentAtIndex:0];
    NSArray* data = [command argumentAtIndex:1 withDefault:[NSNull null]];

    if( [event isEqualToString:@"USER_COMPLETED_LEVEL"] ) {
        [eventService trackEvent: kALEventTypeUserCompletedLevel
            parameters: @{
                kALEventParameterCompletedLevelKey : data[0]
            }
        ];
    } else if( [event isEqualToString:@"USER_COMPLETED_IN_APP_PURCHASE"] ) {
        [eventService trackEvent: kALEventTypeUserViewedProduct
            parameters: @{
                kALEventParameterRevenueAmountKey : data[0],
                kALEventParameterRevenueCurrencyKey : data[1],
                kALEventParameterProductIdentifierKey : data[2]
            }
        ];
    } else if( [event isEqualToString:@"USER_SPENT_VIRTUAL_CURRENCY"] ) {
        [eventService trackEvent: kALEventTypeUserCompletedInAppPurchase
            parameters: @{
                kALEventParameterVirtualCurrencyAmountKey : data[0],
                kALEventParameterVirtualCurrencyNameKey : data[1],
            }
        ];
    } else if( [event isEqualToString:@"USER_COMPLETED_TUTORIAL"] ) {
        [eventService trackEvent: kALEventTypeUserCompletedTutorial];
    } else if( [event isEqualToString:@"USER_VIEWED_PRODUCT"] ) {
        [eventService trackEvent: kALEventTypeUserViewedProduct
            parameters: @{
                kALEventParameterProductIdentifierKey : data[0]
            }
        ];
    } else if( [event isEqualToString:@"USER_LOGGED_IN"] ) {
        [eventService trackEvent: kALEventTypeUserLoggedIn];
    }
}

- (void) isReady:(CDVInvokedUrlCommand *)command {
    CDVPluginResult *pluginResult;
    NSString *callbackId = command.callbackId;

    BOOL isReady = [ALIncentivizedInterstitialAd isReadyForDisplay];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isReady];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void) loadVideoAd: (CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult;
    NSString *callbackId = command.callbackId;

    BOOL isReady = [ALIncentivizedInterstitialAd isReadyForDisplay];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isReady];

    [self.commandDelegate runInBackground:^{
      [self _loadVideoAd];
    }];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void) _loadVideoAd {
    [ALIncentivizedInterstitialAd shared].adDisplayDelegate = self;
    [ALIncentivizedInterstitialAd shared].adVideoPlaybackDelegate = self;
    [ALIncentivizedInterstitialAd preloadAndNotify: self];
}

- (void) showVideoAd: (CDVInvokedUrlCommand*)command {
    CDVPluginResult *pluginResult;
    NSString *callbackId = command.callbackId;

    BOOL isReady = [ALIncentivizedInterstitialAd isReadyForDisplay];
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:isReady];

    [self.commandDelegate runInBackground:^{
      [self _showVideoAd];
    }];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
}

- (void) _showVideoAd {
    [ALIncentivizedInterstitialAd showAndNotify: self];
}

#pragma mark - Ad Load Delegate

- (void)adService:(ALAdService *)adService didLoadAd:(ALAd *)ad
{
    NSLog(@"AppLovin: Rewarded video loaded");
    [self fireEvent:@"" event:@"applovin.load" withData:nil];
}

- (void)adService:(ALAdService *)adService didFailToLoadAdWithError:(int)code
{
    NSLog(@"AppLovin: Rewarded video failed to load with error code %d", code);
    [self fireEvent:@"" event:@"applovin.load_error" withData:nil];
}

#pragma mark - Ad Reward Delegate

- (void)rewardValidationRequestForAd:(ALAd *)ad didSucceedWithResponse:(NSDictionary *)response
{
    /* AppLovin servers validated the reward. Refresh user balance from your server.  We will also pass the number of coins
     awarded and the name of the currency.  However, ideally, you should verify this with your server before granting it. */

    // i.e. - "Coins", "Gold", whatever you set in the dashboard.
    NSString *currencyName = response[@"currency"];

    // For example, "5" or "5.00" if you've specified an amount in the UI.
    NSString *amountGivenString = response[@"amount"];
    NSNumber *amountGiven = @([amountGivenString floatValue]);

    NSLog(@"AppLovin: Rewarded %@ %@", amountGiven, currencyName);
    [self fireEvent:@"" event:@"applovin.reward" withData:nil];
}

- (void)rewardValidationRequestForAd:(ALAd *)ad didFailWithError:(NSInteger)responseCode
{
    if (responseCode == kALErrorCodeIncentivizedUserClosedVideo)
    {
        // Your user exited the video prematurely. It's up to you if you'd still like to grant
        // a reward in this case. Most developers choose not to. Note that this case can occur
        // after a reward was initially granted (since reward validation happens as soon as a
        // video is launched).
    }
    else if (responseCode == kALErrorCodeIncentivizedValidationNetworkTimeout || responseCode == kALErrorCodeIncentivizedUnknownServerError)
    {
        // Some server issue happened here. Don't grant a reward. By default we'll show the user
        // a UIAlertView telling them to try again later, but you can change this in the
        // Manage Apps UI.
    }
    else if (responseCode == kALErrorCodeIncentiviziedAdNotPreloaded)
    {
        // Indicates that the developer called for a rewarded video before one was available.
    }

    [self fireEvent:@"" event:@"applovin.close" withData:nil];
}

- (void)rewardValidationRequestForAd:(ALAd *)ad didExceedQuotaWithResponse:(NSDictionary *)response
{
    // Your user has already earned the max amount you allowed for the day at this point, so
    // don't give them any more money. By default we'll show them a UIAlertView explaining this,
    // though you can change that from the Manage Apps UI.
    NSLog(@"AppLovin: Reward validation request for ad did exceed quota with response: %@", response);
    [self fireEvent:@"" event:@"applovin.error" withData:nil];
}

- (void)rewardValidationRequestForAd:(ALAd *)ad wasRejectedWithResponse:(NSDictionary *)response
{
    // Your user couldn't be granted a reward for this view. This could happen if you've blacklisted
    // them, for example. Don't grant them any currency. By default we'll show them a UIAlertView explaining this,
    // though you can change that from the Manage Apps UI.
    [self fireEvent:@"" event:@"applovin.error" withData:nil];
}

- (void)userDeclinedToViewAd:(ALAd *)ad
{
    NSLog(@"AppLovin: User declined to view ad");
    [self fireEvent:@"" event:@"applovin.close" withData:nil];
}

#pragma mark - Ad Display Delegate

- (void)ad:(ALAd *)ad wasDisplayedIn:(UIView *)view
{
    NSLog(@"AppLovin: Ad Displayed");
}

- (void)ad:(ALAd *)ad wasHiddenIn:(UIView *)view
{
    NSLog(@"AppLovin: Ad Dismissed");
    [self fireEvent:@"" event:@"applovin.close" withData:nil];
}

- (void)ad:(ALAd *)ad wasClickedIn:(UIView *)view
{
    NSLog(@"AppLovin: Ad Clicked");
}

#pragma mark - Ad Video Playback Delegate

- (void)videoPlaybackBeganInAd:(ALAd *)ad
{
    NSLog(@"AppLovin: Video Started");
    [self fireEvent:@"" event:@"applovin.start" withData:nil];
}

- (void)videoPlaybackEndedInAd:(ALAd *)ad atPlaybackPercent:(NSNumber *)percentPlayed fullyWatched:(BOOL)wasFullyWatched
{
    NSLog(@"AppLovin: Video Ended");
    [self fireEvent:@"" event:@"applovin.completed" withData:nil];
}



- (void) fireEvent:(NSString *)obj event:(NSString *)eventName withData:(NSString *)jsonStr {
  NSString* js;
  if(obj && [obj isEqualToString:@"window"]) {
    js = [NSString stringWithFormat:@"var evt=document.createEvent(\"UIEvents\");evt.initUIEvent(\"%@\",true,false,window,0);window.dispatchEvent(evt);", eventName];
  } else if(jsonStr && [jsonStr length]>0) {
    js = [NSString stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@',%@);", eventName, jsonStr];
  } else {
    js = [NSString stringWithFormat:@"javascript:cordova.fireDocumentEvent('%@');", eventName];
  }
  [self.commandDelegate evalJs:js];
}

@end

