//
//  VideoEditor.m
//
//  Created by Josh Bavari on 01-14-2014
//  Modified by Ross Martin on 01-29-2015
//

#import <Cordova/CDV.h>
#import "VideoEditorPlugin.h"


@interface VideoEditorPlugin ()

@end

@implementation VideoEditorPlugin

/*  transcodeVideo arguments:
 * INDEX   ARGUMENT
 *  0       video input url
 *  1       output file name
 *  2       quality
 *  3       output file type
 *  4       optimize for network use
 */
- (void) transcodeVideo:(CDVInvokedUrlCommand*)command
{
    NSArray* arguments = command.arguments;
    
    NSString *assetPath = [arguments objectAtIndex:0];
    NSString *videoFileName = [arguments objectAtIndex:1];
    
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
    NSString *outputExtension = Nil;
    
    switch (outputFileType) {
        case QUICK_TIME:
            stringOutputFileType = AVFileTypeQuickTimeMovie;
            outputExtension = @".mov";
            break;
        case M4A:
            stringOutputFileType = AVFileTypeAppleM4A;
            outputExtension = @".m4a";
            break;
        case M4V:
            stringOutputFileType = AVFileTypeAppleM4V;
            outputExtension = @".m4v";
            break;
        case MPEG4:
        default:
            stringOutputFileType = AVFileTypeMPEG4;
            outputExtension = @".mp4";
            break;
    }
    
    // check if the video can be saved to photo album before going further
    if (!UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(assetPath))
    {
        NSString *error = @"Video cannot be saved to photo album";
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error ] callbackId:command.callbackId];
        return;
    }
    
    NSString *docDir = [NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *tempVideoPath =[NSString stringWithFormat:@"%@/%@%@", docDir, videoFileName, @".mov"];
    NSData *videoData = [NSData dataWithContentsOfFile:assetPath];
    [videoData writeToFile:tempVideoPath atomically:NO];


    if (UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(assetPath))
    {
        AVURLAsset *avAsset = [AVURLAsset URLAssetWithURL:[NSURL fileURLWithPath:tempVideoPath] options:nil];
        NSArray *compatiblePresets = [AVAssetExportSession exportPresetsCompatibleWithAsset:avAsset];
        
        if ([compatiblePresets containsObject:AVAssetExportPresetLowQuality])
        {
            AVAssetExportSession *exportSession = [[AVAssetExportSession alloc]initWithAsset:avAsset presetName: presetName];
            NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
            NSString *videoPath = [NSString stringWithFormat:@"%@/%@%@", [paths objectAtIndex:0], videoFileName, outputExtension];
            
            exportSession.outputURL = [NSURL fileURLWithPath:videoPath];
            exportSession.outputFileType = stringOutputFileType;
            exportSession.shouldOptimizeForNetworkUse = optimizeForNetworkUse;
            
            NSLog(@"videopath of your file = %@", videoPath);
            
            //CMTime start = CMTimeMakeWithSeconds(1.0, 600);
            //CMTime duration = CMTimeMakeWithSeconds(3.0, 600);
            //CMTimeRange range = CMTimeRangeMake(start, duration);
            //exportSession.timeRange = range;

            [exportSession exportAsynchronouslyWithCompletionHandler:^{
                switch ([exportSession status]) {
                    case AVAssetExportSessionStatusCompleted:
                        UISaveVideoAtPathToSavedPhotosAlbum(videoPath, self, nil, nil);
                        NSLog(@"Export Complete %d %@", exportSession.status, exportSession.error);
                        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:videoPath] callbackId:command.callbackId];
                        break;
                    case AVAssetExportSessionStatusFailed:
                        NSLog(@"Export failed: %@", [[exportSession error] localizedDescription]);
                        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[[exportSession error] localizedDescription]] callbackId:command.callbackId];
                        break;
                    case AVAssetExportSessionStatusCancelled:
                        NSLog(@"Export canceled");
                        break;
                    default:
                        NSLog(@"Export default in switch");
                        break;
                }
            }];
        }
        
    }

}

@end