//
//  FacebookConnectPlugin.h
//  GapFacebookConnect
//
//  Created by Jesse MacFadyen on 11-04-22.
//  Copyright 2011 Nitobi. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <AssetsLibrary/ALAssetsLibrary.h>

#import <Cordova/CDV.h>

@interface VideoEditorPlugin : CDVPlugin {
}

- (void)transcodeVideo:(CDVInvokedUrlCommand*)command;
- (void)writeVideoToPhotoLibrary:(NSURL *)nsurlToSave;
@end