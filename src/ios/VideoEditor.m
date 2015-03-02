//
//  VideoEditor.m
//
//  Created by Josh Bavari on 01-14-2014
//  Modified by Ross Martin on 01-29-2015
//

#import <Cordova/CDV.h>
#import "VideoEditor.h"

@interface VideoEditor ()

@end

@implementation VideoEditor

/*  transcodeVideo arguments:
 fileUri: video input url
 outputFileName: output file name
 quality: transcode quality
 outputFileType: output file type
 optimizeForNetworkUse: optimize for network use
 saveToLibrary: bool - save to photo album
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
    
    BOOL optimizeForNetworkUse = ([options objectForKey:@"optimizeForNetworkUse"]) ? [[options objectForKey:@"optimizeForNetworkUse"] intValue] : NO;
    
    float videoDuration = [[options objectForKey:@"duration"] floatValue];
    
    BOOL saveToPhotoAlbum = [options objectForKey:@"saveToLibrary"] ? [[options objectForKey:@"saveToLibrary"] boolValue] : YES;
    
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
    
    // remove file:// from the assetPath if it is there
    assetPath = [[assetPath stringByReplacingOccurrencesOfString:@"file://" withString:@""] mutableCopy];
    
    // check if the video can be saved to photo album before going further
    if (saveToPhotoAlbum && !UIVideoAtPathIsCompatibleWithSavedPhotosAlbum(assetPath))
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
        
        NSLog(@"videopath of your file: %@", videoPath);
        
        if (videoDuration)
        {
            int32_t preferredTimeScale = 600;
            CMTime startTime = CMTimeMakeWithSeconds(0, preferredTimeScale);
            CMTime stopTime = CMTimeMakeWithSeconds(videoDuration, preferredTimeScale);
            CMTimeRange exportTimeRange = CMTimeRangeFromTimeToTime(startTime, stopTime);
            exportSession.timeRange = exportTimeRange;
        }

        [exportSession exportAsynchronouslyWithCompletionHandler:^{
            switch ([exportSession status]) {
                case AVAssetExportSessionStatusCompleted:
                    if (saveToPhotoAlbum) {
                        UISaveVideoAtPathToSavedPhotosAlbum(videoPath, self, nil, nil);
                    }
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

- (void) createThumbnail:(CDVInvokedUrlCommand*)command
{
    NSDictionary* options = [command.arguments objectAtIndex:0];
    
    NSString* srcVideoPath = [options objectForKey:@"fileUri"];
    NSString* outputFileName = [options objectForKey:@"outputFileName"];
    
    NSString* outputFilePath = extractVideoThumbnail(srcVideoPath, outputFileName);
    
    if (outputFilePath != nil)
    {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:outputFilePath] callbackId:command.callbackId];
    }
    else
    {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:outputFilePath] callbackId:command.callbackId];
    }
}

NSString* extractVideoThumbnail(NSString *srcVideoPath, NSString *outputFileName)
{
    
    UIImage *thumbnail;
    NSURL *url;
    
    NSLog(@"srcVideoPath: %@", srcVideoPath);
    
    if ([srcVideoPath rangeOfString:@"://"].location == NSNotFound)
    {
        url = [NSURL URLWithString:[[@"file://localhost" stringByAppendingString:srcVideoPath] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    }
    else
    {
        url = [NSURL URLWithString:[srcVideoPath stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    }
    
    // http://stackoverflow.com/a/6432050
    MPMoviePlayerController *mp = [[MPMoviePlayerController alloc] initWithContentURL:url];
    mp.shouldAutoplay = NO;
    mp.initialPlaybackTime = 1;
    mp.currentPlaybackTime = 1;
    // get the thumbnail
    thumbnail = [mp thumbnailImageAtTime:1 timeOption:MPMovieTimeOptionNearestKeyFrame];
    [mp stop];
    
    NSString *outputFilePath = [documentsPathForFileName(outputFileName) stringByAppendingString:@".jpg"];
    
    NSLog(@"path to your video thumbnail: %@", outputFilePath);
    
    // write out the thumbnail; a return of nil will be a failure.
    if ([UIImageJPEGRepresentation (thumbnail, 1.0) writeToFile:outputFilePath atomically:YES])
    {
        return outputFilePath;
    }
    else
    {
        return nil;
    }
}

NSString *documentsPathForFileName(NSString *name)
{
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,NSUserDomainMask, YES);
    NSString *documentsPath = [paths objectAtIndex:0];
    
    return [documentsPath stringByAppendingPathComponent:name];
}

@end