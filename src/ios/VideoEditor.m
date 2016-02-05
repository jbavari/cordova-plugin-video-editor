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

/**
 * transcodeVideo
 *
 * Transcodes a video
 *
 * ARGUMENTS
 * =========
 *
 * fileUri:         - path to input video
 * outputFileName:  - output file name
 * quality:         - transcode quality
 * outputFileType:  - output file type
 * saveToLibrary:   - save to gallery
 * progress:        - optional callback function that receives progress info
 *
 * RESPONSE
 * ========
 *
 * outputFilePath - path to output file
 *
 * @param CDVInvokedUrlCommand command
 * @return void
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
            presetName = AVAssetExportPresetHighestQuality;
            break;
        case MediumQuality:
        default:
            presetName = AVAssetExportPresetMediumQuality;
            break;
        case LowQuality:
            presetName = AVAssetExportPresetLowQuality;
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

    NSString *cacheDir = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *tempVideoPath =[NSString stringWithFormat:@"%@/%@%@", cacheDir, videoFileName, @".mov"];
    NSData *videoData = [NSData dataWithContentsOfFile:assetPath];
    [videoData writeToFile:tempVideoPath atomically:NO];

    AVURLAsset *avAsset = [AVURLAsset URLAssetWithURL:[NSURL fileURLWithPath:tempVideoPath] options:nil];

    // run in background
    [self.commandDelegate runInBackground:^{
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

        //  Set up a semaphore for the completion handler and progress timer
        dispatch_semaphore_t sessionWaitSemaphore = dispatch_semaphore_create(0);

        void (^completionHandler)(void) = ^(void)
        {
            dispatch_semaphore_signal(sessionWaitSemaphore);
        };

        // do it
        [exportSession exportAsynchronouslyWithCompletionHandler:completionHandler];

        do {
            dispatch_time_t dispatchTime = DISPATCH_TIME_FOREVER;  // if we dont want progress, we will wait until it finishes.
            dispatchTime = getDispatchTimeFromSeconds((float)1.0);
            double progress = [exportSession progress] * 100;

            NSLog([NSString stringWithFormat:@"AVAssetExport running progress=%3.2f%%", progress]);

            NSMutableDictionary *dictionary = [[NSMutableDictionary alloc] init];
            [dictionary setValue: [NSNumber numberWithDouble: progress] forKey: @"progress"];

            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: dictionary];

            [result setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            dispatch_semaphore_wait(sessionWaitSemaphore, dispatchTime);
        } while( [exportSession status] < AVAssetExportSessionStatusCompleted );

        // this is kinda odd but must be done
        if ([exportSession status] == AVAssetExportSessionStatusCompleted) {
            NSMutableDictionary *dictionary = [[NSMutableDictionary alloc] init];
            // AVAssetExportSessionStatusCompleted will not always mean progress is 100 so hard code it below
            double progress = 100.00;
            [dictionary setValue: [NSNumber numberWithDouble: progress] forKey: @"progress"];

            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: dictionary];

            [result setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }

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

/**
 * createThumbnail
 *
 * Creates a thumbnail from the start of a video.
 *
 * ARGUMENTS
 * =========
 * fileUri        - input file path
 * outputFileName - output file name
 * atTime         - location in the video to create the thumbnail (in seconds),
 * width          - width of the thumbnail (optional)
 * height         - height of the thumbnail (optional)
 * quality        - quality of the thumbnail (between 1 and 100)
 *
 * RESPONSE
 * ========
 *
 * outputFilePath - path to output file
 *
 * @param CDVInvokedUrlCommand command
 * @return void
 */
- (void) createThumbnail:(CDVInvokedUrlCommand*)command
{
    NSLog(@"createThumbnail");
    NSDictionary* options = [command.arguments objectAtIndex:0];

    NSString* srcVideoPath = [options objectForKey:@"fileUri"];
    NSString* outputFileName = [options objectForKey:@"outputFileName"];
    float atTime = ([options objectForKey:@"atTime"]) ? [[options objectForKey:@"atTime"] floatValue] : 0;
    float width = [[options objectForKey:@"width"] floatValue];
    float height = [[options objectForKey:@"height"] floatValue];
    float quality = ([options objectForKey:@"quality"]) ? [[options objectForKey:@"quality"] floatValue] : 100;
    float thumbQuality = quality * 1.0 / 100;

    int32_t preferredTimeScale = 600;
    CMTime time = CMTimeMakeWithSeconds(atTime, preferredTimeScale);

    UIImage* thumbnail = [self generateThumbnailImage:srcVideoPath atTime:time];

    if (width && height) {
        NSLog(@"got width and height, resizing image");
        CGSize newSize = CGSizeMake(width, height);
        thumbnail = [self scaleImage:thumbnail toSize:newSize];
        NSLog(@"new size of thumbnail, width x height = %f x %f", thumbnail.size.width, thumbnail.size.height);
    }

    NSString *cacheDir = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];
    NSString *outputFilePath = [cacheDir stringByAppendingPathComponent:[NSString stringWithFormat:@"%@.%@", outputFileName, @"jpg"]];

    // write out the thumbnail
    if ([UIImageJPEGRepresentation(thumbnail, thumbQuality) writeToFile:outputFilePath atomically:YES])
    {
        NSLog(@"path to your video thumbnail: %@", outputFilePath);
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:outputFilePath] callbackId:command.callbackId];
    }
    else
    {
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"failed to create thumbnail file"] callbackId:command.callbackId];
    }
}

/**
 * trim
 *
 * Performs a trim operation on a clip, while encoding it.
 *
 * ARGUMENTS
 * =========
 * fileUri        - input file path
 * trimStart      - time to start trimming
 * trimEnd        - time to end trimming
 * outputFileName - output file name
 * progress:      - optional callback function that receives progress info
 *
 * RESPONSE
 * ========
 *
 * outputFilePath - path to output file
 *
 * @param CDVInvokedUrlCommand command
 * @return void
 */
- (void) trim:(CDVInvokedUrlCommand*)command {
    NSLog(@"[Trim]: trim called");

    // extract arguments
    NSDictionary* options = [command.arguments objectAtIndex:0];
    if ([options isKindOfClass:[NSNull class]]) {
        options = [NSDictionary dictionary];
    }
    NSString *inputFile = [options objectForKey:@"fileUri"];
    float trimStart = [[options objectForKey:@"trimStart"] floatValue];
    float trimEnd = [[options objectForKey:@"trimEnd"] floatValue];
    NSString *outputName = [options objectForKey:@"outputFileName"];

    // remove file:// from the inputFile path if it is there
    inputFile = [[inputFile stringByReplacingOccurrencesOfString:@"file://" withString:@""] mutableCopy];

    NSFileManager *fileMgr = [NSFileManager defaultManager];
    NSString *cacheDir = [NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0];

    // videoDir
    NSString *videoDir = [cacheDir stringByAppendingPathComponent:@"mp4"];
    if ([fileMgr createDirectoryAtPath:videoDir withIntermediateDirectories:YES attributes:nil error: NULL] == NO){
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"failed to create video dir"] callbackId:command.callbackId];
        return;
    }
    NSString *videoOutput = [videoDir stringByAppendingPathComponent:[NSString stringWithFormat:@"%@.%@", outputName, @"mp4"]];

    NSLog(@"[Trim]: inputFile path: %@", inputFile);
    NSLog(@"[Trim]: outputPath: %@", videoOutput);

    // run in background
    [self.commandDelegate runInBackground:^{

        AVURLAsset *avAsset = [AVURLAsset URLAssetWithURL:[NSURL fileURLWithPath:inputFile] options:nil];

        AVAssetExportSession *exportSession = [[AVAssetExportSession alloc]initWithAsset:avAsset presetName: AVAssetExportPresetHighestQuality];
        exportSession.outputURL = [NSURL fileURLWithPath:videoOutput];
        exportSession.outputFileType = AVFileTypeQuickTimeMovie;
        exportSession.shouldOptimizeForNetworkUse = NO;

        int32_t preferredTimeScale = 600;
        CMTime startTime = CMTimeMakeWithSeconds(trimStart, preferredTimeScale);
        CMTime stopTime = CMTimeMakeWithSeconds(trimEnd, preferredTimeScale);
        CMTimeRange exportTimeRange = CMTimeRangeFromTimeToTime(startTime, stopTime);
        exportSession.timeRange = exportTimeRange;

        // debug timings
        NSString *trimStart = (NSString *) CFBridgingRelease(CMTimeCopyDescription(NULL, startTime));
        NSString *trimEnd = (NSString *) CFBridgingRelease(CMTimeCopyDescription(NULL, stopTime));
        NSLog(@"[Trim]: duration: %lld, trimStart: %@, trimEnd: %@", avAsset.duration.value, trimStart, trimEnd);

        //  Set up a semaphore for the completion handler and progress timer
        dispatch_semaphore_t sessionWaitSemaphore = dispatch_semaphore_create(0);

        void (^completionHandler)(void) = ^(void)
        {
            dispatch_semaphore_signal(sessionWaitSemaphore);
        };

        // do it
        [exportSession exportAsynchronouslyWithCompletionHandler:completionHandler];

        do {
            dispatch_time_t dispatchTime = DISPATCH_TIME_FOREVER;  // if we dont want progress, we will wait until it finishes.
            dispatchTime = getDispatchTimeFromSeconds((float)1.0);
            double progress = [exportSession progress] * 100;

            NSLog([NSString stringWithFormat:@"AVAssetExport running progress=%3.2f%%", progress]);

            NSMutableDictionary *dictionary = [[NSMutableDictionary alloc] init];
            [dictionary setValue: [NSNumber numberWithDouble: progress] forKey: @"progress"];

            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: dictionary];

            [result setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            dispatch_semaphore_wait(sessionWaitSemaphore, dispatchTime);
        } while( [exportSession status] < AVAssetExportSessionStatusCompleted );

        // this is kinda odd but must be done
        if ([exportSession status] == AVAssetExportSessionStatusCompleted) {
            NSMutableDictionary *dictionary = [[NSMutableDictionary alloc] init];
            // AVAssetExportSessionStatusCompleted will not always mean progress is 100 so hard code it below
            double progress = 100.00;
            [dictionary setValue: [NSNumber numberWithDouble: progress] forKey: @"progress"];

            CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: dictionary];

            [result setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }

        switch ([exportSession status]) {
            case AVAssetExportSessionStatusCompleted:
                NSLog(@"[Trim]: Export Complete %d %@", exportSession.status, exportSession.error);
                [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:videoOutput] callbackId:command.callbackId];
                break;
            case AVAssetExportSessionStatusFailed:
                NSLog(@"[Trim]: Export failed: %@", [[exportSession error] localizedDescription]);
                [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[[exportSession error] localizedDescription]] callbackId:command.callbackId];
                break;
            case AVAssetExportSessionStatusCancelled:
                NSLog(@"[Trim]: Export canceled");
                break;
            default:
                NSLog(@"[Trim]: Export default in switch");
                break;
        }

    }];
}

// modified version of http://stackoverflow.com/a/21230645/1673842
- (UIImage *)generateThumbnailImage: (NSString *)srcVideoPath atTime:(CMTime)time
{
    NSURL *url = [NSURL fileURLWithPath:srcVideoPath];

    if ([srcVideoPath rangeOfString:@"://"].location == NSNotFound)
    {
        url = [NSURL URLWithString:[[@"file://localhost" stringByAppendingString:srcVideoPath] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    }
    else
    {
        url = [NSURL URLWithString:[srcVideoPath stringByAddingPercentEscapesUsingEncoding: NSUTF8StringEncoding]];
    }

    AVAsset *asset = [AVAsset assetWithURL:url];
    AVAssetImageGenerator *imageGenerator = [[AVAssetImageGenerator alloc] initWithAsset:asset];
    imageGenerator.requestedTimeToleranceAfter = kCMTimeZero; // needed to get a precise time (http://stackoverflow.com/questions/5825990/i-cannot-get-a-precise-cmtime-for-generating-still-image-from-1-8-second-video)
    imageGenerator.requestedTimeToleranceBefore = kCMTimeZero; // ^^
    imageGenerator.appliesPreferredTrackTransform = YES; // crucial to have the right orientation for the image (http://stackoverflow.com/questions/9145968/getting-video-snapshot-for-thumbnail)
    CGImageRef imageRef = [imageGenerator copyCGImageAtTime:time actualTime:NULL error:NULL];
    UIImage *thumbnail = [UIImage imageWithCGImage:imageRef];
    CGImageRelease(imageRef);  // CGImageRef won't be released by ARC

    return thumbnail;
}

// to scale images without changing aspect ratio (http://stackoverflow.com/a/8224161/1673842)
- (UIImage*)scaleImage:(UIImage*)image
              toSize:(CGSize)newSize;
{
    float oldWidth = image.size.width;
    float scaleFactor = newSize.width / oldWidth;

    float newHeight = image.size.height * scaleFactor;
    float newWidth = oldWidth * scaleFactor;

    UIGraphicsBeginImageContext(CGSizeMake(newWidth, newHeight));
    [image drawInRect:CGRectMake(0, 0, newWidth, newHeight)];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return newImage;
}

static dispatch_time_t getDispatchTimeFromSeconds(float seconds) {
    long long milliseconds = seconds * 1000.0;
    dispatch_time_t waitTime = dispatch_time( DISPATCH_TIME_NOW, 1000000LL * milliseconds );
    return waitTime;
}

@end
