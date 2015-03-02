This is a cordova plugin to assist in several video editing tasks such as:

* Transcoding
* Trimming
* Taking still images from time moments (currently the start of a video)

After looking at an article on [How Vine Satisfied Its Need for Speed](http://www.technologyreview.com/view/510511/how-vine-satisfies-its-need-for-speed/), it was clear Cordova/Phonegap needed a way to modify videos to be faster for app's that need that speed.

This plugin will address those concerns, hopefully.

## Usage

###Transcode###
```javascript
// parameters passed to transcodeVideo
VideoEditor.transcodeVideo(
    success, // success cb
    error, // error cb
    {
        fileUri: 'file-uri-here', // the path to the video on the device
        outputFileName: 'output-name', // the file name for the transcoded video
        quality: VideoEditorOptions.Quality.MEDIUM_QUALITY,
        outputFileType: VideoEditorOptions.OutputFileType.MPEG4,
        optimizeForNetworkUse: VideoEditorOptions.OptimizeForNetworkUse.YES,
        duration: 20, // optional, specify duration in seconds from start of video
        saveToLibrary: true // optional, defaults to true
    }
)
```
```javascript
// options used with transcodeVideo function
var VideoEditorOptions = {
    Quality: {
        HIGH_QUALITY: 0,
        MEDIUM_QUALITY: 1,
        LOW_QUALITY: 2
    },
    OptimizeForNetworkUse: {
        NO: 0,
        YES: 1
    },
    OutputFileType: {
        M4V: 0,
        MPEG4: 1,
        M4A: 2,
        QUICK_TIME: 3
    }
};
```
```javascript
// this example uses the cordova media capture plugin
navigator.device.capture.captureVideo(
    videoCaptureSuccess, 
    videoCaptureError, 
    { 
        limit: 1, 
        duration: 20 
    }
);

function videoCaptureSuccess(mediaFiles) {
    var file = mediaFiles[0];
    var videoFileName = 'video-name-here'; // I suggest a uuid

    // Wrap this call in a ~100 ms timeout on Android if
    // you just recorded the video using the capture plugin.
    // For some reason it is not available immediately in the file system.
    VideoEditor.transcodeVideo(
        videoTranscodeSuccess,
        videoTranscodeError,
        {
            fileUri: file.fullPath, 
            outputFileName: videoFileName, 
            quality: VideoEditorOptions.Quality.MEDIUM_QUALITY,
            outputFileType: VideoEditorOptions.OutputFileType.MPEG4,
            optimizeForNetworkUse: VideoEditorOptions.OptimizeForNetworkUse.YES,
            duration: 20
        }
    );
}

function videoTranscodeSuccess(result) {
	// result is the path to the transcoded video on the device
    console.log('videoTranscodeSuccess, result: ' + result);
}

function videoTranscodeError(err) {
	console.log('videoTranscodeError, err: ' + err);
}
```

###Create JPEG Image From Video###
```javascript
VideoEditor.createThumbnail(
    success, // success cb
    error, // error cb
    {
        fileUri: 'file-uri-here', // the path to the video on the device
        outputFileName: 'output-name' // the file name for the JPEG image
    }
)
```

```javascript
// this example uses the cordova media capture plugin
navigator.device.capture.captureVideo(
    videoCaptureSuccess, 
    videoCaptureError, 
    { 
        limit: 1, 
        duration: 20 
    }
);

function videoCaptureSuccess(mediaFiles) {
    var file = mediaFiles[0];
    var videoFileName = 'video-name-here'; // I suggest a uuid

    // Wrap this call in a ~100 ms timeout on Android if
    // you just recorded the video using the capture plugin.
    // For some reason it is not available immediately in the file system.
    VideoEditor.createThumbnail(
        createThumbnailSuccess,
        createThumbnailError,
        {
            fileUri: mediaFile.fullPath,
            outputFileName: videoFileName
        }
    )
}

function createThumbnailSuccess(result) {
    // result is the path to the jpeg image on the device
    console.log('createThumbnailSuccess, result: ' + result);
}
```

## On iOS

[iOS Developer AVFoundation Documentation](https://developer.apple.com/library/ios/documentation/AudioVideo/Conceptual/AVFoundationPG/Articles/01_UsingAssets.html#//apple_ref/doc/uid/TP40010188-CH7-SW8)

[Video compression in AVFoundation](http://www.iphonedevsdk.com/forum/iphone-sdk-development/110246-video-compression-avassetwriter-in-avfoundation.html)

[AVFoundation slides - tips/tricks](https://speakerdeck.com/bobmccune/composing-and-editing-media-with-av-foundation)

[AVFoundation slides #2](http://www.slideshare.net/bobmccune/learning-avfoundation)

[Bob McCune's AVFoundation Editor - ios app example](https://github.com/tapharmonic/AVFoundationEditor)

[Saving videos after recording videos](http://stackoverflow.com/questions/20902234/save-video-to-library-after-capturing-video-using-phonegap-capturevideo)



## On Android

[Android Documentation](http://developer.android.com/guide/appendix/media-formats.html#recommendations)

[Android Media Stores](http://developer.android.com/reference/android/provider/MediaStore.html#EXTRA_VIDEO_QUALITY)

[How to Port ffmpeg (the Program) to Android–Ideas and Thoughts](http://www.roman10.net/how-to-port-ffmpeg-the-program-to-androidideas-and-thoughts/)

[How to Build Android Applications Based on FFmpeg by An Example](http://www.roman10.net/how-to-build-android-applications-based-on-ffmpeg-by-an-example/)