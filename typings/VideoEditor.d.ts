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
        outputFileName: string
}

/**
 * Trim options that are required to locate, reduce start/ end and save the video.
 */
declare interface VideoEditorThumbnailProperties {
        /** A well-known location where the editable video lives. */
        fileUri: string,
        /** A string that indicates what type of field this is, home for example. */
        outputFileName: string
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
}

declare var VideoEditor: VideoEditor;