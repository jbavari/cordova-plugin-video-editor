This is a cordova plugin to assist in several video editing tasks such as:

* Transcoding
* Trimming (TODO)
* Taking still images from time moments (TODO)

After looking at an article on [How Vine Satisfied Its Need for Speed](http://www.technologyreview.com/view/510511/how-vine-satisfies-its-need-for-speed/), it was clear Cordova/Phonegap needed a way to modify videos to be faster for app's that need that speed.

This plugin will address those concerns, hopefully.

## Usage

###Transcode###
```javascript
// parameters passed to transcodeVideo
VideoEditorPlugin.transcodeVideo(
    fileUri, // the path to the video on the device
    fileName, // the file name for the transcoded video
    quality, // VideoEditorConstant
    outputFileType, // VideoEditorConstant
    optimizeForNetworkUse, // VideoEditorConstant
    success, // success cb
    error // error cb
)
```
```javascript
// constants used with transcodeVideo function
var VideoEditorConstants = {
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
    var videoFileName = 'video-name-here';

    VideoEditorPlugin.transcodeVideo(
        file.fullPath, 
        videoFileName, // I suggest generating a uuid for file name
        VideoEditorConstants.Quality.MEDIUM_QUALITY,
        VideoEditorConstants.OutputFileType.MPEG4,
        VideoEditorConstants.OptimizeForNetworkUse.YES,
        videoTranscodeSuccess,
        videoTranscodeError
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

### Other helpful tidbits:

