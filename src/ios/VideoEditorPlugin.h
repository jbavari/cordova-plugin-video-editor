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

enum CDVQualityType {
	HighQuality = 0,
	MediumQuality = 1,
	LowQuality = 2,
};
typedef NSUInteger CDVQualityType;

//enum CDVOptimizeForNetworkUse {
//	No = 0,
//	Yes = 1
//};
//typedef NSUInteger CDVOptimizeForNetworkUse;

enum CDVOutputFileType {
	M4V = 0,
	MPEG4 = 1,
	M4A = 2,
	QUICK_TIME = 3
};
typedef NSUInteger CDVOutputFileType;

@interface VideoEditorPlugin : CDVPlugin {
}

- (void)transcodeVideo:(CDVInvokedUrlCommand*)command;
- (void)writeVideoToPhotoLibrary:(NSURL *)nsurlToSave;

@end