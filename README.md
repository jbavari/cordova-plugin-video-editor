[![npm version](https://badge.fury.io/js/cordova-plugin-video-editor.svg)](https://badge.fury.io/js/cordova-plugin-video-editor)

This is a cordova plugin to assist in several video editing tasks such as:

* Transcoding
* Trimming
* Creating thumbnails from a video file (now at a specific time in the video)
* Getting info on a video - width, height, orientation, duration, size, & bitrate.

After looking at an article on [How Vine Satisfied Its Need for Speed](http://www.technologyreview.com/view/510511/how-vine-satisfies-its-need-for-speed/), it was clear Cordova/Phonegap needed a way to modify videos to be faster for app's that need that speed.

This plugin will address those concerns, hopefully.

## Installation
```
cordova plugin add cordova-plugin-video-editor
```
`VideoEditor` and `VideoEditorOptions` will be available in the window after deviceready.

## Usage

### Transcode a video

```javascript
// parameters passed to transcodeVideo
VideoEditor.transcodeVideo(
    success, // success cb
    error, // error cb
    {
        fileUri: 'file-uri-here', // the path to the video on the device
        outputFileName: 'output-name', // the file name for the transcoded video
        outputFileType: VideoEditorOptions.OutputFileType.MPEG4, // android is always mp4
        optimizeForNetworkUse: VideoEditorOptions.OptimizeForNetworkUse.YES, // ios only
        saveToLibrary: true, // optional, defaults to true
        deleteInputFile: false, // optional (android only), defaults to false
        maintainAspectRatio: true, // optional (ios only), defaults to true
        width: 640, // optional, see note below on width and height
        height: 640, // optional, see notes below on width and height
        videoBitrate: 1000000, // optional, bitrate in bits, defaults to 1 megabit (1000000)
        fps: 24, // optional (android only), defaults to 24
        audioChannels: 2, // optional (ios only), number of audio channels, defaults to 2
        audioSampleRate: 44100, // optional (ios only), sample rate for the audio, defaults to 44100
        audioBitrate: 128000, // optional (ios only), audio bitrate for the video in bits, defaults to 128 kilobits (128000)
        progress: function(info) {} // info will be a number from 0 to 100
    }
);
```
#### A note on width and height used by transcodeVideo
I recommend setting `maintainAspectRatio` to true.  When this option is true you can provide any width/height and the height provided will be used to calculate the new width for the output video.  If you set `maintainAspectRatio` false there is a good chance you'll end up with videos that are stretched and/or distorted.  Here is the simplified formula used on iOS when `maintainAspectRatio` is true -
```objective-c
aspectRatio = videoWidth / videoHeight;
outputWidth = height * aspectRatio;
outputHeight = outputWidth / aspectRatio;
```

Android will always use the aspect ratio of the input video to calculate the scaled output width and height.  Setting `maintainAspectRatio` on android will make not make a difference.

If you don't provide width and height to `transcodeVideo` the output video will have the same dimensions as the input video.

#### transcodeVideo example -
```javascript
// options used with transcodeVideo function
// VideoEditorOptions is global, no need to declare it
var VideoEditorOptions = {
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
    // Wrap this below in a ~100 ms timeout on Android if
    // you just recorded the video using the capture plugin.
    // For some reason it is not available immediately in the file system.

    var file = mediaFiles[0];
    var videoFileName = 'video-name-here'; // I suggest a uuid

    VideoEditor.transcodeVideo(
        videoTranscodeSuccess,
        videoTranscodeError,
        {
            fileUri: file.fullPath,
            outputFileName: videoFileName,
            outputFileType: VideoEditorOptions.OutputFileType.MPEG4,
            optimizeForNetworkUse: VideoEditorOptions.OptimizeForNetworkUse.YES,
            saveToLibrary: true,
            maintainAspectRatio: true,
            width: 640,
            height: 640,
            videoBitrate: 1000000, // 1 megabit
            audioChannels: 2,
            audioSampleRate: 44100,
            audioBitrate: 128000, // 128 kilobits
            progress: function(info) {
                console.log('transcodeVideo progress callback, info: ' + info);
            }
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

#### Windows Quirks
Windows does not support any of the optional parameters at this time. Specifying them will not cause an error but, there is no functionality behind them.

### Trim a Video (iOS only)
```javascript
VideoEditor.trim(
    trimSuccess,
    trimFail,
    {
        fileUri: 'file-uri-here', // path to input video
        trimStart: 5, // time to start trimming in seconds
        trimEnd: 15, // time to end trimming in seconds
        outputFileName: 'output-name', // output file name
        progress: function(info) {} // optional, see docs on progress
    }
);

function trimSuccess(result) {
    // result is the path to the trimmed video on the device
    console.log('trimSuccess, result: ' + result);
}

function trimFail(err) {
    console.log('trimFail, err: ' + err);
}
```

### Create a JPEG thumbnail from a video
```javascript
VideoEditor.createThumbnail(
    success, // success cb
    error, // error cb
    {
        fileUri: 'file-uri-here', // the path to the video on the device
        outputFileName: 'output-name', // the file name for the JPEG image
        atTime: 2, // optional, location in the video to create the thumbnail (in seconds)
        width: 320, // optional, width of the thumbnail
        height: 480, // optional, height of the thumbnail
        quality: 100 // optional, quality of the thumbnail (between 1 and 100)
    }
);
// atTime will default to 0 if not provided
// width and height will be the same as the video input if they are not provided
// quality will default to 100 if not provided
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
    // Wrap this below in a ~100 ms timeout on Android if
    // you just recorded the video using the capture plugin.
    // For some reason it is not available immediately in the file system.

    var file = mediaFiles[0];
    var videoFileName = 'video-name-here'; // I suggest a uuid

    VideoEditor.createThumbnail(
        createThumbnailSuccess,
        createThumbnailError,
        {
            fileUri: file.fullPath,
            outputFileName: videoFileName,
            atTime: 2,
            width: 320,
            height: 480,
            quality: 100
        }
    );
}

function createThumbnailSuccess(result) {
    // result is the path to the jpeg image on the device
    console.log('createThumbnailSuccess, result: ' + result);
}
```

#### A note on width and height used by createThumbnail
The aspect ratio of the thumbnail created will match that of the video input.  This means you may not get exactly the width and height dimensions you give to `createThumbnail` for the jpeg.  This for your convenience but let us know if it is a problem.  I am considering adding a `maintainAspectRatio` option to `createThumbnail` (and when this option is false you might have stretched, square thumbnails :laughing:).

### Get info on a video (width, height, orientation, duration, size, & bitrate)
```javascript
VideoEditor.getVideoInfo(
    success, // success cb
    error, // error cb
    {
        fileUri: 'file-uri-here', // the path to the video on the device
    }
);
```

```javascript
VideoEditor.getVideoInfo(
    getVideoInfoSuccess,
    getVideoInfoError,
    {
        fileUri: file.fullPath
    }
);

function getVideoInfoSuccess(info) {
    console.log('getVideoInfoSuccess, info: ' + JSON.stringify(info, null, 2));
    // info is a JSON object with the following properties -
    {
        width: 1920,
        height: 1080,
        orientation: 'landscape', // will be portrait or landscape
        duration: 3.541, // duration in seconds
        size: 6830126, // size of the video in bytes
        bitrate: 15429777 // bitrate of the video in bits per second
    }
}
```

## Android & FFmpeg
FFmpeg has been removed from android for several reasons but mainly for performance.  If you still need the old functionality that FFmpeg provided  [V1.09](https://github.com/jbavari/cordova-plugin-video-editor/tree/1.0.9) is the last version that will use it.

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


## On Windows


## License

Android: Apache 2.0

iOS: MIT

Windows: Apache 2.0
