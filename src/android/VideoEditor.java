package org.apache.cordova.videoeditor;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.graphics.Bitmap;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import net.ypresto.androidtranscoder.MediaTranscoder;

/**
 * VideoEditor plugin for Android
 * Created by Ross Martin 2-2-15
 */
public class VideoEditor extends CordovaPlugin {

    private static final String TAG = "VideoEditor";

    private CallbackContext callback;

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
     * fileUri              - path to input video
     * outputFileName       - output file name
     * saveToLibrary        - save to gallery
     * deleteInputFile      - optionally remove input file
     * width                - width for the output video
     * height               - height for the output video
     * fps                  - fps the video
     * videoBitrate         - video bitrate for the output video in bits
     * duration             - max video duration (in seconds?)
     *
     * RESPONSE
     * ========
     *
     * outputFilePath - path to output file
     *
     * @param JSONArray args
     * @return void
     */
    private void transcodeVideo(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "transcodeVideo firing");

        JSONObject options = args.optJSONObject(0);
        Log.d(TAG, "options: " + options.toString());

        final File inFile = this.resolveLocalFileSystemURI(options.getString("fileUri"));
        if (!inFile.exists()) {
            Log.d(TAG, "input file does not exist");
            callback.error("input video does not exist.");
            return;
        }

        final String videoSrcPath = inFile.getAbsolutePath();
        final String outputFileName = options.optString(
                "outputFileName",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date())
        );

        final boolean deleteInputFile = options.optBoolean("deleteInputFile", false);
        final int width = options.optInt("width", 0);
        final int height = options.optInt("height", 0);
        final int fps = options.optInt("fps", 24);
        final int videoBitrate = options.optInt("videoBitrate", 1000000); // default to 1 megabit
        final long videoDuration = options.optLong("duration", 0) * 1000 * 1000;

        Log.d(TAG, "videoSrcPath: " + videoSrcPath);

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
                return;
            }
        }

        final String outputFilePath = new File(
                mediaStorageDir.getPath(),
                outputFileName + outputExtension
        ).getAbsolutePath();

        Log.d(TAG, "outputFilePath: " + outputFilePath);

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                try {

                    FileInputStream fin = new FileInputStream(inFile);

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
                                callback.error("an error ocurred during transcoding");
                                return;
                            }

                            // make the gallery display the new file if saving to library
                            if (saveToLibrary) {
                                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                scanIntent.setData(Uri.fromFile(inFile));
                                scanIntent.setData(Uri.fromFile(outFile));
                                appContext.sendBroadcast(scanIntent);
                            }

                            if (deleteInputFile) {
                                inFile.delete();
                            }

                            callback.success(outputFilePath);
                        }

                        @Override
                        public void onTranscodeCanceled() {
                            callback.error("transcode canceled");
                            Log.d(TAG, "transcode canceled");
                        }

                        @Override
                        public void onTranscodeFailed(Exception exception) {
                            callback.error(exception.toString());
                            Log.d(TAG, "transcode exception", exception);
                        }
                    };

                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(videoSrcPath);

                    String orientation;
                    String mmrOrientation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
                    Log.d(TAG, "mmrOrientation: " + mmrOrientation); // 0, 90, 180, or 270

                    float videoWidth = Float.parseFloat(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    float videoHeight = Float.parseFloat(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

                    MediaTranscoder.getInstance().transcodeVideo(fin.getFD(), outputFilePath,
                            new CustomAndroidFormatStrategy(videoBitrate, fps, width, height), listener, videoDuration);

                } catch (Throwable e) {
                    Log.d(TAG, "transcode exception ", e);
                    callback.error(e.toString());
                }

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
     * @param JSONArray args
     * @return void
     */
    private void createThumbnail(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "createThumbnail firing");


        JSONObject options = args.optJSONObject(0);
        Log.d(TAG, "options: " + options.toString());

        String fileUri = options.getString("fileUri");
        if (!fileUri.startsWith("file:/")) {
            fileUri = "file:/" + fileUri;
        }

        File inFile = this.resolveLocalFileSystemURI(fileUri);
        if (!inFile.exists()) {
            Log.d(TAG, "input file does not exist");
            callback.error("input video does not exist.");
            return;
        }
        final String srcVideoPath = inFile.getAbsolutePath();
        String outputFileName = options.optString(
                "outputFileName",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date())
        );

        final int quality = options.optInt("quality", 100);
        final int width = options.optInt("width", 0);
        final int height = options.optInt("height", 0);
        long atTimeOpt = options.optLong("atTime", 0);
        final long atTime = (atTimeOpt == 0) ? 0 : atTimeOpt * 1000000;

        final Context appContext = cordova.getActivity().getApplicationContext();
        PackageManager pm = appContext.getPackageManager();

        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(cordova.getActivity().getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            ai = null;
        }
        final String appName = (String) (ai != null ? pm.getApplicationLabel(ai) : "Unknown");

        File externalFilesDir =  new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + cordova.getActivity().getPackageName() + "/files/files/videos");

        if (!externalFilesDir.exists()) {
            if (!externalFilesDir.mkdirs()) {
                callback.error("Can't access or make Movies directory");
                return;
            }
        }

        final File outputFile =  new File(
                externalFilesDir.getPath(),
                outputFileName + ".jpg"
        );
        final String outputFilePath = outputFile.getAbsolutePath();

        // start task
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                OutputStream outStream = null;

                try {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(srcVideoPath);

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

                }

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
     * width         - width of the video
     * height        - height of the video
     * orientation   - orientation of the video
     * duration      - duration of the video (in seconds)
     * size          - size of the video (in bytes)
     * bitrate       - bitrate of the video (in bits per second)
     *
     * @param JSONArray args
     * @return void
     */
    private void getVideoInfo(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "getVideoInfo firing");

        JSONObject options = args.optJSONObject(0);
        Log.d(TAG, "options: " + options.toString());

        File inFile = this.resolveLocalFileSystemURI(options.getString("fileUri"));
        if (!inFile.exists()) {
            Log.d(TAG, "input file does not exist");
            callback.error("input video does not exist.");
            return;
        }

        String videoSrcPath = inFile.getAbsolutePath();
        Log.d(TAG, "videoSrcPath: " + videoSrcPath);

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(videoSrcPath);
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

        JSONObject response = new JSONObject();
        response.put("width", videoWidth);
        response.put("height", videoHeight);
        response.put("orientation", orientation);
        response.put("duration", duration);
        response.put("size", inFile.length());
        response.put("bitrate", bitrate);

        callback.success(response);
    }


    @SuppressWarnings("deprecation")
    private File resolveLocalFileSystemURI(String url) throws IOException, JSONException {
        String decoded = URLDecoder.decode(url, "UTF-8");

        File fp = null;

        // Handle the special case where you get an Android content:// uri.
        if (decoded.startsWith("content:")) {
            fp = new File(getPath(this.cordova.getActivity().getApplicationContext(), Uri.parse(decoded)));
        } else {
            // Test to see if this is a valid URL first
            @SuppressWarnings("unused")
            URL testUrl = new URL(decoded);

            if (decoded.startsWith("file://")) {
                int questionMark = decoded.indexOf("?");
                if (questionMark < 0) {
                    fp = new File(decoded.substring(7, decoded.length()));
                } else {
                    fp = new File(decoded.substring(7, questionMark));
                }
            } else if (decoded.startsWith("file:/")) {
                fp = new File(decoded.substring(6, decoded.length()));
            } else {
                fp = new File(decoded);
            }
        }

        if (!fp.exists()) {
            throw new FileNotFoundException( "" + url + " -> " + fp.getCanonicalPath());
        }
        if (!fp.canRead()) {
            throw new IOException("can't read file: " + url + " -> " + fp.getCanonicalPath());
        }
        return fp;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}
