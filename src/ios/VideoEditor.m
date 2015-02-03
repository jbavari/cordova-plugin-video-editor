//
//  VideoEditor.m
//
//  Created by Josh Bavari on 01-14-2014
//  Modified by Ross Martin on 01-29-2015
//

#import <Cordova/CDV.h>
#import "VideoEditor.h"


@interface VideoEditorPlugin ()

@end

@implementation VideoEditorPlugin

/*  transcodeVideo arguments:
 fileUri: video input url
 outputFileName: output file name
 quality: transcode quality
 outputFileType: output file type
 optimizeForNetworkUse: optimize for network use
 */
- (void) transcodeVideo:(CDVInvokedUrlCommand*)command
{
    NSDictionary* options = [command.arguments objectAtIndex:0];
    
    if ([options isKindOfClass:[NSNull class]]) {
        options = [NSDictionary dictionary];
    }
    
    NSString *assetPath = [options objectForKey:@"fileUri"];
    NSString *videoFileName = [options objectForKey:@"outputFileName"];
    
    CDVQualityType qualityType = ([options objectForKey:@"quality"]) ? [[options objectForKey:@"quality"] intValue] : LowQuality;
    
    NSString *presetName = Nil;
    
    switch(qualityType) {
        case HighQuality:
            presetName = AVAssetExportPresetHighestQuality; // 360x480
            break;
        case MediumQuality:
        default:
            presetName = AVAssetExportPresetMediumQuality; // 360x480
            break;
        case LowQuality:
            presetName = AVAssetExportPresetLowQuality; // 144x192
    }

    CDVOutputFileType outputFileType = ([options objectForKey:@"outputFileType"]) ? [[options objectForKey:@"outputFileType"] intValue] : MPEG4;
    
    BOOL optimizeForNetworkUse = ([options objectForKey:@"optimizeForNetworkUse"]) ? YES : NO;
    
    int videoDuration = [[options objectForKey:@"duration"] intValue];
    
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
        
        if (videoDuration)
        {
            CMTime startTime = CMTimeMake(0, 1);
            CMTime stopTime = CMTimeMake(videoDuration, 1);
            CMTimeRange exportTimeRange = CMTimeRangeFromTimeToTime(startTime, stopTime);
            exportSession.timeRange = exportTimeRange;
        }

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

@end