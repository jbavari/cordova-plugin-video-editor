//
//  VideoEditorPlugin.m
//
//  Created by Josh Bavari on 01-14-2014
//

#import <Cordova/CDV.h>
#import "VideoEditorPlugin.h"


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
 *  2       quality
 *  3       output file type
 *  4       optimize for network use
 */
- (void) transcodeVideo:(CDVInvokedUrlCommand*)command
{
	NSString* callbackId = command.callbackId;
    NSArray* arguments = command.arguments;

    //"file:///private/var/mobile/Applications/8AF200CC-F3ED-439C-ACF7-FB6B3C012019/tmp/trim.BCCB55E5-8488-41B6-9867-3C50670818BB.MOV"

    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *outputURL = paths[0];
    
    NSURL *assetURL = [NSURL URLWithString:[arguments objectAtIndex:0]];
	NSURL *assetOutputURL = [NSURL URLWithString:[arguments objectAtIndex:1]];
    
    CDVQualityType qualityType = ([arguments objectAtIndex:2]) ? [[arguments objectAtIndex:2] intValue] : LowQuality;
    
    NSString *presetName = Nil;
    
    switch(qualityType) {
        case HighQuality:
            presetName = AVAssetExportPresetHighestQuality;
            break;
        case MediumQuality:
        default:
            presetName = AVAssetExportPresetMediumQuality;
            break;
        case LowQuality:
            presetName = AVAssetExportPresetLowQuality;
    }

    
    CDVOutputFileType outputFileType = ([arguments objectAtIndex:3]) ? [[arguments objectAtIndex:3] intValue] : MPEG4;
    
    BOOL optimizeForNetworkUse = ([arguments objectAtIndex:4]) ? YES : NO;
    
    NSString *stringOutputFileType = Nil;
    
    switch (outputFileType) {
        case QUICK_TIME:
            stringOutputFileType = AVFileTypeQuickTimeMovie;
            break;
        case M4A:
            stringOutputFileType = AVFileTypeAppleM4A;
            break;
        case M4V:
            stringOutputFileType = AVFileTypeAppleM4V;
            break;
        case MPEG4:
        default:
            stringOutputFileType = AVFileTypeMPEG4;
            break;
    }
    
    NSFileManager *manager = [NSFileManager defaultManager];
    [manager createDirectoryAtPath:outputURL withIntermediateDirectories:YES attributes:nil error:nil];
    outputURL = [outputURL stringByAppendingPathComponent:@"output.mp4"];
    
    [manager removeItemAtPath:outputURL error:nil];
    
    assetOutputURL = [NSURL fileURLWithPath:outputURL];

	AVAsset *videoAsset = [AVURLAsset assetWithURL:assetURL];

	NSArray *compatiblePresets = [AVAssetExportSession exportPresetsCompatibleWithAsset:videoAsset];
	if ([compatiblePresets containsObject:AVAssetExportPresetLowQuality]) {
		AVAssetExportSession *exportSession = [[AVAssetExportSession alloc]
			initWithAsset:videoAsset presetName:presetName];
		exportSession.outputURL = assetOutputURL;
//		exportSession.outputFileType = AVFileTypeQuickTimeMovie;
        exportSession.outputFileType = stringOutputFileType;
        exportSession.shouldOptimizeForNetworkUse = optimizeForNetworkUse;
        
		CMTime start = CMTimeMakeWithSeconds(1.0, 600);
		CMTime duration = CMTimeMakeWithSeconds(3.0, 600);
		CMTimeRange range = CMTimeRangeMake(start, duration);
//		exportSession.timeRange = range;

		[exportSession exportAsynchronouslyWithCompletionHandler:^{
			switch ([exportSession status]) {
                case AVAssetExportSessionStatusCompleted:
                    [self writeVideoToPhotoLibrary:assetOutputURL];
                    NSLog(@"Export Complete %d %@", exportSession.status, exportSession.error);
                    break;
				case AVAssetExportSessionStatusFailed:
					NSLog(@"Export failed: %@", [[exportSession error] localizedDescription]);
					break;
				case AVAssetExportSessionStatusCancelled:
					NSLog(@"Export canceled");
					break;
				default:
					break;
			}
		}];
	}
    
    //OTHER CODE
    //[[NSFileManager defaultManager] removeItemAtURL:outputURL error:nil];
//    AVURLAsset *asset = [AVURLAsset URLAssetWithURL:videoToTrimURL options:nil];
//    AVAssetExportSession *exportSession = [[AVAssetExportSession alloc] initWithAsset:asset presetName:AVAssetExportPresetHighestQuality];
//    
//    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
//    NSString *outputURL = paths[0];
//    NSFileManager *manager = [NSFileManager defaultManager];
//    [manager createDirectoryAtPath:outputURL withIntermediateDirectories:YES attributes:nil error:nil];
//    outputURL = [outputURL stringByAppendingPathComponent:@"output.mp4"];
//    // Remove Existing File
//    [manager removeItemAtPath:outputURL error:nil];
//    
//    
//    exportSession.outputURL = [NSURL fileURLWithPath:outputURL];
//    exportSession.shouldOptimizeForNetworkUse = YES;
//    exportSession.outputFileType = AVFileTypeQuickTimeMovie;
//    CMTime start = CMTimeMakeWithSeconds(1.0, 600);
//    CMTime duration = CMTimeMakeWithSeconds(3.0, 600);
//    CMTimeRange range = CMTimeRangeMake(start, duration);
//    exportSession.timeRange = range;
//    [exportSession exportAsynchronouslyWithCompletionHandler:^(void)
//     {
//         switch (exportSession.status) {
//             case AVAssetExportSessionStatusCompleted:
//                 [self writeVideoToPhotoLibrary:[NSURL fileURLWithPath:outputURL]];
//                 NSLog(@"Export Complete %d %@", exportSession.status, exportSession.error);
//                 break;
//             case AVAssetExportSessionStatusFailed:
//                 NSLog(@"Failed:%@",exportSession.error);
//                 break;
//             case AVAssetExportSessionStatusCancelled:
//                 NSLog(@"Canceled:%@",exportSession.error);
//                 break;
//             default:
//                 break;
//         }
//         
//         //[exportSession release];
    
    //AFTER OTHER CODE

	CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

-(void)writeVideoToPhotoLibrary:(NSURL *)nsurlToSave{
    ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
    NSURL *recordedVideoURL= nsurlToSave;
    if ([library videoAtPathIsCompatibleWithSavedPhotosAlbum:recordedVideoURL]) {
        [library writeVideoAtPathToSavedPhotosAlbum:recordedVideoURL completionBlock:^(NSURL *assetURL, NSError *error){} ];
    }
    [library release];
}

@end