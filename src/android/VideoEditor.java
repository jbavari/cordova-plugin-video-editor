package org.apache.cordova.videoeditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
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
        final int outputType = options.getInt("outputFileType");
        
        Log.d(TAG, "videoSrcPath: " + videoSrcPath);
                        
        String outputExtension = null;
        
        // WTF Java won't allow a switch on a string unless JRE 1.7+
        if (outputType == QUICK_TIME) {
            outputExtension = ".mov";
        } else if (outputType == M4A) {
            outputExtension = ".m4a";
        } else if (outputType == M4V) {
            outputExtension = ".m4v";
        } else {
            outputExtension = ".mp4";
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
       
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {             
                
                LoadJNI vk = new LoadJNI();
                 try {
                    String workFolder = appContext.getFilesDir().getAbsolutePath();
                    
                    String[] complexCommand = {
                        "ffmpeg" ,
                        "-y", // overwrite output files
                        "-i", // input file
                        videoSrcPath, 
                        "-strict",
                        "experimental",
                        "-s", // frame size (resolution)
                        "720x480", // TODO: provide res choices from quality plugin argument
                        "-r", // fps, TODO: control fps based on quality plugin argument
                        "24", 
                        "-vcodec", 
                        //"mpeg4", // TODO: try libx264 with -preset ultrafast flag
                        "libx264",
                        "-preset",
                        "ultrafast",
                        "-b",
                        "2097152", // TODO: allow tuning the video bitrate based on quality plugin argument
                        //"-ab", // can't find this in ffmpeg docs, not sure on this yet
                        //"48000",
                        "-ac", // audio channels 
                        "2",
                        "-ar", // sampling frequency
                        "22050", 
                        //"ss", // start position
                        //"00:00:00",
                        //"-t", // end position, TODO: allow specifying duration & do same for iOS
                        //"00:00:01",
                        outputFilePath
                    };
                    
                    vk.run(complexCommand, workFolder, appContext);
                    
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