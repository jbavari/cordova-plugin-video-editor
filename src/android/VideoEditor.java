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
import android.os.Environment;
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
                outputResolution = "144x192";
                break;
            case MediumQuality:
            case HighQuality:
            default:
                outputResolution = "360x480"; 
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
        
        File mediaStorageDir = new File(
            Environment.getExternalStorageDirectory() + "/Movies",
            appName
        );
        
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
                    
                    // remove the original input file
                    if (!inFile.delete()) {
                        Log.d(TAG, "unable to delete in file");
                    }
                    
                    // make the gallery display the new file and not the deleted one
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(Uri.fromFile(inFile));
                    scanIntent.setData(Uri.fromFile(outFile));
                    appContext.sendBroadcast(scanIntent);
                    
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
        
        File mediaStorageDir = new File(
            Environment.getExternalStorageDirectory() + "/Pictures",
            appName
        );
        
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdir()) {
                callback.error("Can't access or make Pictures directory");
                return;
            }
        }
        
        File outputFile =  new File(
            mediaStorageDir.getPath(),
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
        
        // make the gallery display the new file and not the deleted one
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(outputFile));
        appContext.sendBroadcast(scanIntent);
        
        callback.success(outputFile.getAbsolutePath());   
    }
    
    @SuppressWarnings("deprecation")
    private File resolveLocalFileSystemURI(String url) throws IOException, JSONException {
        String decoded = URLDecoder.decode(url, "UTF-8");

        File fp = null;

        // Handle the special case where you get an Android content:// uri.
        if (decoded.startsWith("content:")) {
            Cursor cursor = this.cordova.getActivity().managedQuery(Uri.parse(decoded), new String[] { MediaStore.Images.Media.DATA }, null, null, null);
            // Note: MediaStore.Images/Audio/Video.Media.DATA is always "_data"
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            fp = new File(cursor.getString(column_index));
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
}