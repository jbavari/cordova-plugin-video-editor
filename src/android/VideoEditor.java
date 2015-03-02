package org.apache.cordova.videoeditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.netcompss.loader.LoadJNI;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

/**
 * VideoEditor plugin for Android
 * Created by Ross Martin 2-2-15
 */
public class VideoEditor extends CordovaPlugin {

    private static final String TAG = "VideoEditor";

    private CallbackContext callback;
    
    private static final int HighQuality = 0;
    private static final int MediumQuality = 1;
    private static final int LowQuality = 2;
    
    private static final int M4V = 0;
    private static final int MPEG4 = 1;
    private static final int M4A = 2;
    private static final int QUICK_TIME = 3;
    
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
        }
        return false;
    }

    private void transcodeVideo(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "transcodeVideo firing");
        /*  transcodeVideo arguments:
         fileUri: video input url
         outputFileName: output file name
         quality: transcode quality
         outputFileType: output file type
         optimizeForNetworkUse: optimize for network use
         saveToLibrary: bool - save to gallery
         */
        
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
        final int videoQuality = options.optInt("quality", HighQuality);
        final int outputType = options.optInt("outputFileType", MPEG4);
        
        Log.d(TAG, "videoSrcPath: " + videoSrcPath);
                        
        String outputExtension;
        final String outputResolution; // arbitrary value used for ffmpeg, tailor to your needs
        
        switch(outputType) {
            case QUICK_TIME:
                outputExtension = ".mov";
                break;
            case M4A:
                outputExtension = ".m4a";
                break;
            case M4V:
                outputExtension = ".m4v";
                break;
            case MPEG4:
            default:
                outputExtension = ".mp4";
                break;
        }
        
        switch(videoQuality) {
            case LowQuality:
                outputResolution = "320x320";
                break;
            case MediumQuality:
                outputResolution = "480x480";
                break;
            case HighQuality:
            default:
                outputResolution = "640x640"; 
                break;
        }
        
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
            mediaStorageDir = new File(appContext.getExternalCacheDir().getPath());
        }
        
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdir()) {
                callback.error("Can't access or make Movies directory");
                return;
            }
        }
        
        final String outputFilePath =  new File(
            mediaStorageDir.getPath(),
            "VID_" + outputFileName + outputExtension
        ).getAbsolutePath();
        
        Log.v(TAG, "outputFilePath: " + outputFilePath);
        
        final double videoDuration = options.optDouble("duration", 0);
       
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {             
                
                LoadJNI vk = new LoadJNI();
                 try {
                    String workFolder = appContext.getFilesDir().getAbsolutePath();
                                        
                    ArrayList<String> al = new ArrayList<String>();
                    al.add("ffmpeg");
                    al.add("-y"); // overwrite output files
                    al.add("-i"); // input file
                    al.add(videoSrcPath); 
                    al.add("-strict");
                    al.add("experimental");
                    al.add("-s"); // frame size (resolution)
                    al.add(outputResolution);
                    al.add("-r"); // fps, TODO: control fps based on quality plugin argument
                    al.add("24"); 
                    al.add("-vcodec");
                    al.add("libx264"); // mpeg4 works good too
                    al.add("-preset");
                    al.add("ultrafast"); // needed b/c libx264 doesn't utilize all CPU cores
                    al.add("-b");
                    al.add("2097152"); // TODO: allow tuning the video bitrate based on quality plugin argument
                    //al.add("-ab"); // can't find this in ffmpeg docs, not sure on this yet
                    //al.add("48000");
                    al.add("-ac"); // audio channels 
                    al.add("1");
                    al.add("-ar"); // sampling frequency
                    al.add("22050"); 
                    if (videoDuration != 0) {
                        //al.add("-ss"); // start position may be either in seconds or in hh:mm:ss[.xxx] form.
                        //al.add("0");
                        al.add("-t"); // duration may be a number in seconds, or in hh:mm:ss[.xxx] form.
                        al.add(Double.toString(videoDuration));
                    }
                    
                    al.add(outputFilePath); // output file at end of string
                    
                    String[] ffmpegCommand = al.toArray(new String[al.size()]);
                    
                    vk.run(ffmpegCommand, workFolder, appContext);
                    
                    Log.d(TAG, "ffmpeg4android finished");
                    
                    File outFile = new File(outputFilePath);
                    if (!outFile.exists()) {
                        Log.d(TAG, "outputFile doesn't exist!");
                        callback.error("an error ocurred during transcoding");
                        return;
                    }
                                        
                    // make the gallery display the new file if saving to library
                    if (saveToLibrary) {
                        // remove the original input file when saving to gallery
                        // comment out or remove the delete based on your needs
                        if (!inFile.delete()) {
                            Log.d(TAG, "unable to delete in file");
                        }
                        
                        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        scanIntent.setData(Uri.fromFile(inFile));
                        scanIntent.setData(Uri.fromFile(outFile));
                        appContext.sendBroadcast(scanIntent);
                    }
                    
                    callback.success(outputFilePath);
                } catch (Throwable e) {
                    Log.d(TAG, "vk run exception.", e);
                    callback.error(e.toString());
                }
            }
        });
    }
    
    @SuppressWarnings("unused")
    private void createThumbnail(JSONArray args) throws JSONException, IOException {
        Log.d(TAG, "createThumbnail firing");
        /*  createThumbnail arguments:
         fileUri: video input url
         outputFileName: output file name
         */
        
        JSONObject options = args.optJSONObject(0);
        Log.d(TAG, "options: " + options.toString());

        File inFile = this.resolveLocalFileSystemURI(options.getString("fileUri"));
        if (!inFile.exists()) {
            Log.d(TAG, "input file does not exist");
            callback.error("input video does not exist.");
            return;
        }
        String srcVideoPath = inFile.getAbsolutePath();
        String outputFileName = options.optString(
            "outputFileName", 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date())
        );
        
        Context appContext = cordova.getActivity().getApplicationContext();
        PackageManager pm = appContext.getPackageManager();
        
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(cordova.getActivity().getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            ai = null;
        }
        final String appName = (String) (ai != null ? pm.getApplicationLabel(ai) : "Unknown");
                
        File tempStoragePath = appContext.getExternalCacheDir();
        
        File outputFile =  new File(
            tempStoragePath.getPath(),
            "PIC_" + outputFileName + ".jpg"
        );
        
        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(srcVideoPath, MediaStore.Images.Thumbnails.MINI_KIND);
        
        FileOutputStream theOutputStream;
        try {
            if (!outputFile.exists()) {
                if (!outputFile.createNewFile()) {
                    callback.error("Could not save thumbnail.");
                }
            }
            if (outputFile.canWrite()) {
                theOutputStream = new FileOutputStream(outputFile);
                if (theOutputStream != null) {
                    thumbnail.compress(CompressFormat.JPEG, 75, theOutputStream);
                } else {
                    callback.error("Could not save thumbnail; target not writeable");
                }
            }
        } catch (IOException e) {
            callback.error(e.toString());
        }
                
        callback.success(outputFile.getAbsolutePath());   
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
            throw new FileNotFoundException();
        }
        if (!fp.canRead()) {
            throw new IOException();
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