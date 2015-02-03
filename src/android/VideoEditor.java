package org.apache.cordova.videoeditor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

import com.netcompss.loader.LoadJNI;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Environment;
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
            this.transcodeVideo(args);
            return true;
        }
        return false;
    }

    private void transcodeVideo(JSONArray args) throws JSONException {
        Log.d(TAG, "transcodeVideo firing");
        /*  transcodeVideo arguments:
         * INDEX   ARGUMENT
         *  0       video input url
         *  1       output file name
         *  2       quality
         *  3       output file type
         *  4       optimize for network use
         */
        
        final File inFile = new File(args.getString(0).replace("file:", ""));
        final Context appContext = cordova.getActivity().getApplicationContext();
        final PackageManager pm = appContext.getPackageManager();
        
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(cordova.getActivity().getPackageName(), 0);
        } catch (final NameNotFoundException e) {
            ai = null;
        }
        final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "Unknown");
        
        if (!inFile.exists()) {
            Log.d(TAG, "input file does not exist");
            callback.error("input video does not exist.");
            return;
        }
        
        final String videoSrcPath = inFile.getAbsolutePath();
        final String outputFileName = args.getString(1);
        final int outputType = args.getInt(2);
        
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
        
        File mediaStorageDir = new File(
            Environment.getExternalStorageDirectory() + "/Movies",
            applicationName
        );
        
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdir()) {
                callback.error("Can't access or make Movies directory");
                return;
            }
        }
        
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        
        final String outputFilePath =  new File(
            mediaStorageDir.getPath(),
            "VID_" + timeStamp + outputExtension
        ).getAbsolutePath();
        
        Log.v(TAG, "outputFilePath: " + outputFilePath);
       
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {             
                
                LoadJNI vk = new LoadJNI();
                 try {
                    String workFolder = appContext.getFilesDir().getAbsolutePath();
                    //String[] complexCommand = {"ffmpeg","-i", videoSrcPath};
                    
                    // ffmpeg -y -i /sdcard/in.mp4 -strict experimental -s 160x120 -r 25 -vcodec mpeg4 -b 2097152 -ab 48000 -ac 2 -ar 22050 /sdcard/out.mp4﻿
                    
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
                    
                    // remove the input file from the gallery
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    scanIntent.setData(Uri.fromFile(inFile));
                    appContext.sendBroadcast(scanIntent);
                    
                    callback.success(outputFilePath);
                } catch (Throwable e) {
                    Log.d(TAG, "vk run exception.", e);
                    callback.error(e.toString());
                }
            }
        });
    }
}