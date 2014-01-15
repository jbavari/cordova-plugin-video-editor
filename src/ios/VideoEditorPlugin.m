//
//  VideoEditorPlugin.m
//
//  Created by Josh Bavari on 01-14-2014
//

#import "VideoEditor.h"

@interface VideoEditorPlugin ()

// @property (strong, nonatomic) NSString *userid;
// @property (strong, nonatomic) NSString* loginCallbackId;
// @property (strong, nonatomic) NSString* dialogCallbackId;

@end

@implementation VideoEditorPlugin

/*  transcodeVideo arguments:
 * INDEX   ARGUMENT
 *  0       video input url
 *  1       video output url
 */
- (void) transcodeVideo:(CDVInvokedUrlCommand*)command
{
	NSString* callbackId = command.callbackId;
    NSArray* arguments = command.arguments;

    //"file:///private/var/mobile/Applications/8AF200CC-F3ED-439C-ACF7-FB6B3C012019/tmp/trim.BCCB55E5-8488-41B6-9867-3C50670818BB.MOV"


	NSURL *assetURL = [arguments objectAtIndex:0];
	NSURL *assetOutputURL = [arguments objectAtIndex:1];

	AVAsset *videoAsset = [AVURLAsset assetWithURL:assetURL];

	NSArray *compatiblePresets = [AVAssetExportSession exportPresetsCompatibleWithAsset:videoAsset];
	if ([compatiblePresets containsObject:AVAssetExportPresetLowQuality]) {
		AVAssetExportSession *exportSession = [[AVAssetExportSession alloc]
			initWithAsset:anAsset presetName:AVAssetExportPresetLowQuality];
		exportSession.outputURL = assetOutputURL;
		exportSession.outputFileType = AVFileTypeQuickTimeMovie;
	 
		CMTime start = CMTimeMakeWithSeconds(1.0, 600);
		CMTime duration = CMTimeMakeWithSeconds(3.0, 600);
		CMTimeRange range = CMTimeRangeMake(start, duration);
		exportSession.timeRange = range;

		[exportSession exportAsynchronouslyWithCompletionHandler:^{
			switch ([exportSession status]) {
				case AVAssetExportSessionStatusFailed:
					NSLog(@"Export failed: %@", [[exportSession error] localizedDescription]);
					break;
				case AVAssetExportSessionStatusCancelled:
					NSLog(@"Export canceled");
					break;
				default:
					CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
				    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
					break;
			}
		}];
	}
}

@end