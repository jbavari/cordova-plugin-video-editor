//
//  VideoEditor.h
//
//  Created by Josh Bavari on 01-14-2014
//  Modified by Ross Martin on 01-29-2015
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <AssetsLibrary/ALAssetsLibrary.h>
#import <MediaPlayer/MediaPlayer.h>

#import <Cordova/CDV.h>

enum CDVOutputFileType {
    M4V = 0,
    MPEG4 = 1,
    M4A = 2,
    QUICK_TIME = 3
};
typedef NSUInteger CDVOutputFileType;

@interface VideoEditor : CDVPlugin {
}

- (void)transcodeVideo:(CDVInvokedUrlCommand*)command;
- (void) createThumbnail:(CDVInvokedUrlCommand*)command;
- (void) getVideoInfo:(CDVInvokedUrlCommand*)command;
- (void) trim:(CDVInvokedUrlCommand*)command;

@end
