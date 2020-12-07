package org.apache.cordova.videoeditor;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.graphics.Bitmap;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import net.ypresto.androidtranscoder.MediaTranscoder;
import net.ypresto.androidtranscoder.utils.MediaExtractorUtils;

/**
 * VideoEditor plugin for Android
 * Created by Ross Martin 2-2-15
 */
public class VideoEditor extends CordovaPlugin {

    private static final String TAG = "VideoEditor";

    private CallbackContext callback;
    private CordovaResourceApi resourceApi;

    /**
     * Initialization
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.resourceApi = webView.getResourceApi();
    }

    /**
     * Executes the request to the plugin.
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute method starting");

        this.callback = callbackContext;

        if (action.equals("transcodeVideo")) {
            try {
                this.transcodeVideo(args);
            } catch (IOException e) {
                callback.error(e.toString());
            }
            return true;
        } else if (action.equals("createThumbnail")) {
            try {
                this.createThumbnail(args);
            } catch (IOException e) {
                callback.error(e.toString());
            }
            return true;
        } else if (action.equals("getVideoInfo")) {
            try {
                this.getVideoInfo(args);
            } catch (IOException e) {
                callback.error(e.toString());
            }
            return true;
        }

        return false;
    }

    /**
     * transcodeVideo
     *
     * Transcodes a video
     *
     * ARGUMENTS
     * =========
     *
     * fileUri                     - path to input video
     * outputFileName              - output file name
     * saveToLibrary               - save to gallery
     * width                       - width for the output video
     * height                      - height for the output video
     * fps                         - fps the video
     * videoBitrate                - video bitrate for the output video in bits
     * audioBitrate                - audio bitrate for the output video in bits
     * audioChannels               - number of audio channels
     * skipVideoTranscodingIfAVC   - skip any transcoding actions (conversion/resizing/etc..) if the input video is avc video
     *
     * RESPONSE
     * ========
     *
     * outputFilePath - path to output file
     *
     * @param args arguments
     */
    private void transcodeVideo(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "transcodeVideo firing");

        JSONObject options = args.optJSONObject(0);
        Log.d(TAG, "options: " + options.toString());

        final ReadDataResult readResult = this.readDataFrom(options.getString("fileUri"));
        if (readResult == null) {
            return;
        }

        final String outputFileName = options.optString(
                "outputFileName",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date())
        );

        final int width = options.optInt("width", CustomAndroidFormatStrategy.DEFAULT_WIDTH);
        final int height = options.optInt("height", CustomAndroidFormatStrategy.DEFAULT_HEIGHT);
        final int fps = options.optInt("fps", CustomAndroidFormatStrategy.DEFAULT_FRAMERATE);
        final int videoBitrate = options.optInt("videoBitrate", CustomAndroidFormatStrategy.DEFAULT_VIDEO_BITRATE); // default to 9 megabit
        final int audioBitrate = options.optInt("audioBitrate", CustomAndroidFormatStrategy.AUDIO_BITRATE_AS_IS);
        final int audioChannels = options.optInt("audioChannels", CustomAndroidFormatStrategy.AUDIO_CHANNELS_AS_IS);
        final boolean skipVideoTranscodingIfAVC = options.optBoolean("skipVideoTranscodingIfAVC", CustomAndroidFormatStrategy.DEFAULT_SKIP_AVC_VIDEO_TRANSCODING);

        final String outputExtension = ".mp4";

        final Context appContext = cordova.getActivity().getApplicationContext();
        final PackageManager pm = appContext.getPackageManager();

        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(cordova.getActivity().getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            ai = null;
        }
        final String appName = (String) (ai != null ? pm.getApplicationLabel(ai) : "Unknown");

        final boolean saveToLibrary = options.optBoolean("saveToLibrary", true);
        File mediaStorageDir;

        if (saveToLibrary) {
            mediaStorageDir = new File(
                    Environment.getExternalStorageDirectory() + "/Movies",
                    appName
            );
        } else {
            mediaStorageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + cordova.getActivity().getPackageName() + "/files/files/videos");
        }

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                callback.error("Can't access or make Movies directory");
                readResult.close();
                return;
            }
        }

        final String outputFilePath = new File(
                mediaStorageDir.getPath(),
                outputFileName + outputExtension
        ).getAbsolutePath();

        Log.d(TAG, "outputFilePath: " + outputFilePath);

        cordova.getThreadPool().execute(() -> {

            try {
                MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
                    @Override
                    public void onTranscodeProgress(double progress) {
                        Log.d(TAG, "transcode running " + progress);

                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("progress", progress);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        PluginResult progressResult = new PluginResult(PluginResult.Status.OK, jsonObj);
                        progressResult.setKeepCallback(true);
                        callback.sendPluginResult(progressResult);
                    }

                    @Override
                    public void onTranscodeCompleted() {

                        File outFile = new File(outputFilePath);
                        if (!outFile.exists()) {
                            Log.d(TAG, "outputFile doesn't exist!");
                            readResult.close();
                            callback.error("an error ocurred during transcoding");
                            return;
                        }

                        // make the gallery display the new file if saving to library
                        if (saveToLibrary) {
                            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            scanIntent.setData(readResult.result.uri);
                            scanIntent.setData(Uri.fromFile(outFile));
                            appContext.sendBroadcast(scanIntent);
                        }

                        readResult.close();
                        callback.success(outputFilePath);
                    }

                    @Override
                    public void onTranscodeCanceled() {
                        readResult.close();
                        callback.error("transcode canceled");
                        Log.d(TAG, "transcode canceled");
                    }

                    @Override
                    public void onTranscodeFailed(Exception exception) {
                        readResult.close();
                        callback.error(exception.toString());
                        Log.d(TAG, "transcode exception", exception);
                    }
                };

                final FileDescriptor fileDescriptor = readResult.getFD();

                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(fileDescriptor);

                MediaTranscoder.getInstance().transcodeVideo(
                        fileDescriptor,
                        outputFilePath,
                        new CustomAndroidFormatStrategy(videoBitrate, fps, width, height, audioBitrate, audioChannels, skipVideoTranscodingIfAVC),
                        listener
                );

            } catch (Throwable e) {
                Log.d(TAG, "transcode exception ", e);
                readResult.close();
                callback.error(e.toString());
            }

        });
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
     * atTime         - location in the video to create the thumbnail (in seconds)
     * width          - width for the thumbnail (optional)
     * height         - height for the thumbnail (optional)
     * quality        - quality of the thumbnail (optional, between 1 and 100)
     *
     * RESPONSE
     * ========
     *
     * outputFilePath - path to output file
     *
     * @param args arguments
     */
    private void createThumbnail(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "createThumbnail firing");

        JSONObject options = args.optJSONObject(0);
        Log.d(TAG, "options: " + options.toString());

        final ReadDataResult readResult = this.readDataFrom(options.getString("fileUri"));
        if (readResult == null) {
            return;
        }

        String outputFileName = options.optString(
                "outputFileName",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date())
        );

        final int quality = options.optInt("quality", 100);
        final int width = options.optInt("width", 0);
        final int height = options.optInt("height", 0);
        long atTimeOpt = options.optLong("atTime", 0);
        final long atTime = (atTimeOpt == 0) ? 0 : atTimeOpt * 1000000;

        File externalFilesDir =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + cordova.getActivity().getPackageName() + "/files/files/videos");
        if (!externalFilesDir.exists()) {
            if (!externalFilesDir.mkdirs()) {
                callback.error("Can't access or make Movies directory");
                readResult.close();
                return;
            }
        }

        final File outputFile =  new File(
                externalFilesDir.getPath(),
                outputFileName + ".jpg"
        );
        final String outputFilePath = outputFile.getAbsolutePath();

        // start task
        cordova.getThreadPool().execute(() -> {

            OutputStream outStream = null;

            try {
                final FileDescriptor fileDescriptor = readResult.getFD();
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(fileDescriptor);

                Bitmap bitmap = mmr.getFrameAtTime(atTime);

                if (width > 0 || height > 0) {
                    int videoWidth = bitmap.getWidth();
                    int videoHeight = bitmap.getHeight();
                    double aspectRatio = (double) videoWidth / (double) videoHeight;

                    Log.d(TAG, "videoWidth: " + videoWidth);
                    Log.d(TAG, "videoHeight: " + videoHeight);

                    int scaleWidth = Double.valueOf(height * aspectRatio).intValue();
                    int scaleHeight = Double.valueOf(scaleWidth / aspectRatio).intValue();

                    Log.d(TAG, "scaleWidth: " + scaleWidth);
                    Log.d(TAG, "scaleHeight: " + scaleHeight);

                    final Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, false);
                    bitmap.recycle();
                    bitmap = resizedBitmap;
                }

                outStream = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outStream);

                callback.success(outputFilePath);

            } catch (Throwable e) {
                if (outStream != null) {
                    try {
                        outStream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

                Log.d(TAG, "exception on thumbnail creation", e);
                callback.error(e.toString());

            } finally {
                readResult.close();
            }

        });
    }

    /**
     * getVideoInfo
     *
     * Gets info on a video
     *
     * ARGUMENTS
     * =========
     *
     * fileUri:      - path to input video
     *
     * RESPONSE
     * ========
     *
     * width              - width of the video
     * height             - height of the video
     * orientation        - orientation of the video
     * duration           - duration of the video (in seconds)
     * size               - size of the video (in bytes)
     * bitrate            - bitrate of the video (in bits per second)
     * videoMediaType     - Media type of the video
     * audioMediaType     - Media type of the audio track in video
     *
     * @param args arguments
     */
    private void getVideoInfo(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "getVideoInfo firing");

        JSONObject options = args.optJSONObject(0);
        Log.d(TAG, "options: " + options.toString());

        final ReadDataResult readResult = this.readDataFrom(options.getString("fileUri"));
        if (readResult == null) {
            return;
        }

        final FileDescriptor fileDescriptor = readResult.getFD();
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(fileDescriptor);
        float videoWidth = Float.parseFloat(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        float videoHeight = Float.parseFloat(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

        String orientation;
        if (Build.VERSION.SDK_INT >= 17) {
            String mmrOrientation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            Log.d(TAG, "mmrOrientation: " + mmrOrientation); // 0, 90, 180, or 270

            if (videoWidth < videoHeight) {
                if (mmrOrientation.equals("0") || mmrOrientation.equals("180")) {
                    orientation = "portrait";
                } else {
                    orientation = "landscape";
                }
            } else {
                if (mmrOrientation.equals("0") || mmrOrientation.equals("180")) {
                    orientation = "landscape";
                } else {
                    orientation = "portrait";
                }
            }
        } else {
            orientation = (videoWidth < videoHeight) ? "portrait" : "landscape";
        }

        double duration = Double.parseDouble(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000.0;
        long bitrate = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));

        String videoMediaType;
        String audioMediaType;
        try {
            final MediaExtractor mExtractor = new MediaExtractor();
            mExtractor.setDataSource(fileDescriptor);
            MediaExtractorUtils.TrackResult trackResult = MediaExtractorUtils.getFirstVideoAndAudioTrack(mExtractor);

            // get types
            videoMediaType = trackResult.mVideoTrackMime;
            audioMediaType = trackResult.mAudioTrackMime;

            // release resources
            mExtractor.release();
            trackResult = null;
        } catch (Throwable e) {
            Log.e(TAG, e.toString());
            callback.error(e.toString());
            readResult.close();
            return;
        }

        JSONObject response = new JSONObject();
        response.put("width", videoWidth);
        response.put("height", videoHeight);
        response.put("orientation", orientation);
        response.put("duration", duration);
        response.put("size", readResult.result.length);
        response.put("bitrate", bitrate);
        response.put("videoMediaType", videoMediaType);
        response.put("audioMediaType", audioMediaType);

        // release resources
        readResult.close();

        callback.success(response);
    }

    /**
     * Reads the data by the given url
     * @param url the url to read the data
     * @return results of the reading
     */
    private ReadDataResult readDataFrom(String url) throws IOException {
        if (!FileUtils.isLocal(url)) {
            final String msg = "The provided url is null or not local: " + url;
            Log.d(TAG, msg);
            callback.error(msg);
            return null;
        }

        final Context context = this.cordova.getActivity().getApplicationContext();
        Uri uri = Uri.parse(url);
        if (uri.isRelative()) {
            uri = Uri.parse(FileUtils.getPath(context, uri));
        }

        CordovaResourceApi.OpenForReadResult readResult = resourceApi.openForRead(uri, true);
        return new ReadDataResult(readResult);
    }

    /**
     * Simple wrapper over the CordovaResourceApi.OpenForReadResult with some util methods
     */
    public static final class ReadDataResult {
        public final CordovaResourceApi.OpenForReadResult result;
        private FileDescriptor fileDescriptor;

        public ReadDataResult(CordovaResourceApi.OpenForReadResult result) {
            this.result = result;
        }

        /**
         * Returns file descriptor based on the OpenForReadResult
         * @return FileDescriptor for the given result
         */
        public FileDescriptor getFD() throws IOException {
            if (this.fileDescriptor != null) {
                return this.fileDescriptor;
            }

            if (this.result.inputStream != null &&
                    this.result.inputStream instanceof FileInputStream) {
                this.fileDescriptor = ((FileInputStream) this.result.inputStream).getFD();
                return this.fileDescriptor;
            }

            if (this.result.assetFd != null) {
                this.fileDescriptor = this.result.assetFd.getFileDescriptor();
                return this.fileDescriptor;
            }

            return null;
        }

        /**
         * Closes the stream and descriptor if them exists
         */
        public void close() {
            try {
                if (this.result.assetFd != null) {
                    this.result.assetFd.close();
                }
                if (this.result.inputStream != null) {
                    this.result.inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}