#import "NotificationChecker.h"

@implementation NotificationChecker

NSDictionary *launchOptions;

+ (NSDictionary *) launchOptions
{ @synchronized(self) { return launchOptions; } }

+ (void) setLaunchOptions:(NSDictionary *)val
{ @synchronized(self) { launchOptions = val; } }

+ (void)load
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(createNotificationChecker:)
                                                 name:@"UIApplicationDidFinishLaunchingNotification" object:nil];
}

+ (void)createNotificationChecker:(NSNotification *)notification
{
    NotificationChecker.launchOptions = [notification userInfo];
}

@end