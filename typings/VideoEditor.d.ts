/**
 * Enumerations for transcoding
 */
declare module VideoEditorOptions {
    //output quailty
    enum Quality {
        HIGH_QUALITY,
        MEDIUM_QUALITY,
        LOW_QUALITY
    }
    //speed over quailty, maybe should be a bool
    enum OptimizeForNetworkUse {
        NO,
        YES
    }
    //type of encoding to do
    enum OutputFileType {
        M4V,
        MPEG4,
        M4A,
        QUICK_TIME
    }
}

/**
 * Transcode options that are required to reencode or change the coding of the video.
 */
declare interface VideoEditorTranscodeProperties {
        /** A well-known location where the editable video lives. */
        fileUri: string,
        /** A string that indicates what type of field this is, home for example. */
        outputFileName: string,
        /** The quality of the result. */
        quality: VideoEditorOptions.Quality,
        /** Instructions on how to encode the video. */
        outputFileType: VideoEditorOptions.OutputFileType,
        /** Should the video be processed with quailty or speed in mind */
        optimizeForNetworkUse: VideoEditorOptions.OptimizeForNetworkUse,
        /** Not supported in windows, the duration in seconds from the start of the video*/
        duration?: number,
        /** Not supported in windows, save into the device library*/
        saveToLibrary?: boolean,
        /** Not supported in windows, delete the orginal video*/
        deleteInputFile?: boolean,
        /** iOS only. Defaults to true */
        maintainAspectRatio?: boolean,
        /** Width of the result */
        width?: number,
        /** Height of the result */
        height?: number,
        /** Bitrate in bits. Defaults to 1 megabit (1000000). */
        videoBitrate?: number,
        /** Frames per second of the result. Android only. Defaults to 24. */
        fps?: number,
        /** Number of audio channels. iOS only. Defaults to 2. */
        audioChannels?: number,
        /** Sample rate for the audio. iOS only. Defaults to 4410. */
        audioBitrate?: number,
        /** Not supported in windows, progress on the transcode*/
        progress?: (info: any) => void
}

/**
 * Trim options that are required to locate, reduce start/ end and save the video.
 */
declare interface VideoEditorTrimProperties {
        /** A well-known location where the editable video lives. */
        fileUri: string,
        /** A number of seconds to trim the front of the video. */
        trimStart: number,
        /** A number of seconds to trim the front of the video. */
        trimEnd: number,
        /** A string that indicates what type of field this is, home for example. */
        outputFileName: string,
        /** Progress on transcode. */
        progress?: (info: any) => void
}

/**
 * Trim options that are required to locate, reduce start/ end and save the video.
 */
declare interface VideoEditorThumbnailProperties {
        /** A well-known location where the editable video lives. */
        fileUri: string,
        /** A string that indicates what type of field this is, home for example. */
        outputFileName: string,
        /** Location in video to create the thumbnail (in seconds). */
        atTime?: number,
        /** Width of the thumbnail. */
        width?: number,
        /** Height of the thumbnail. */
        height?: number,
        /** Quality of the thumbnail (between 1 and 100). */
        quality?: number
}

declare interface VideoEditorVideoInfoOptions {
        /** Path to the video on the device. */
        fileUri: string
}

declare interface VideoEditorVideoInfoDetails {
        /** Width of the video. */
        width: number,
        /** Height of the video. */
        height: number,
        /** Orientation of the video. Will be either portrait or landscape. */
        orientation: 'portrait' | 'landscape',
        /** Duration of the video in seconds. */
        duration: number,
        /** Size of the video in bytes. */
        size: number,
        /** Bitrate of the video in bits per second. */
        bitrate: number
}

/**
 * The VideoEditor object represents a tool for editing videos. Videos can only be trimmed, so far.
 */
interface VideoEditor {
    /**
    * The VideoEditor.transcode method executes asynchronously, encoding a video at a location
    * and returning the full path. Options can be set to change how the video is encoded. The resulting string 
    * is passed to the onSuccess callback function specified by the onSuccess parameter.
    * @param onSuccess Success callback function invoked with the full path of the video returned from successly saving the video
    * @param onError Error callback function, invoked when an error occurs.
    * @param transcodeOptions Transcode options that are required to reencode or change the coding of the video.
    */
    transcodeVideo(onSuccess: (path: string) => void,
        onError: (error: any) => void,
        options: VideoEditorTranscodeProperties): void;

    /**
     * The VideoEditor.trim method executes asynchronously, taking a video location and trimming the beginning and end of the video
     * and returning the full path of the trimmed video. The resulting string is passed to the onSuccess
     * callback function specified by the onSuccess parameter.
     * @param onSuccess Success callback function invoked with the full path of the video returned from successly saving the video
     * @param onError Error callback function, invoked when an error occurs.
     * @param trimOptions Trim options that are required to locate, reduce start/end and save the video.
     */
    trim(onSuccess: (path: string) => void,
        onError: (error: any) => void,
        trimOptions: VideoEditorTrimProperties): void;

    /**
    * The VideoEditor.trim method executes asynchronously, taking a video location and trimming the beginning and end of the video
    * and returning the full path of the trimmed video. The resulting string is passed to the onSuccess
    * callback function specified by the onSuccess parameter.
    * @param onSuccess Success callback function invoked with the full path of the video returned from successly saving the video
    * @param onError Error callback function, invoked when an error occurs.
    * @param trimOptions Trim options that are required to locate, reduce start/end and save the video.
    */
    createThumbnail(onSuccess: (path: string) => void,
        onError: (error: any) => void,
        options: VideoEditorThumbnailProperties): void;

    /**
     * The VideoEditor.getVideoInfo method executes asynchronously, taking a video location and returning the details of the video.
     * The resulting info object is passed to the onSuccess callback function specified by the onSuccess parameter.
     * @param onSuccess Success callback function invoked with the details of the video.
     * @param onError Error callback function, invoked when an error occurs.
     * @param infoOptions Info options that are required to locate the video.
     */
    getVideoInfo(onSuccess: (info: VideoEditorVideoInfoDetails) => void,
        onError: (error: any) => void,
        options: VideoEditorVideoInfoOptions): void;
}

declare var VideoEditor: VideoEditor;