package org.apache.cordova.videoeditor;

import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import net.ypresto.androidtranscoder.format.MediaFormatStrategy;
import net.ypresto.androidtranscoder.format.OutputFormatUnavailableException;

/**
 * Created by ehmm on 02.05.2016.
 *
 *
 */
public class CustomAndroidFormatStrategy implements MediaFormatStrategy {

    private static final String TAG = "CustomFormatStrategy";
    private static final int DEFAULT_BITRATE = 8000000;
    private static final int DEFAULT_FRAMERATE = 30;
    private static final int DEFAULT_WIDTH = 0;
    private static final int DEFAULT_HEIGHT = 0;
    private final int mBitRate;
    private final int mFrameRate;
    private final int width;
    private final int height;

    public CustomAndroidFormatStrategy() {
        this.mBitRate = DEFAULT_BITRATE;
        this.mFrameRate = DEFAULT_FRAMERATE;
        this.width = DEFAULT_WIDTH;
        this.height = DEFAULT_HEIGHT;
    }

    public CustomAndroidFormatStrategy(final int bitRate, final int frameRate, final int width, final int height) {
        this.mBitRate = bitRate;
        this.mFrameRate = frameRate;
        this.width = width;
        this.height = height;
    }

    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        int videoWidth = inputFormat.getInteger("width");
        int videoHeight = inputFormat.getInteger("height");
        int outWidth;
        int outHeight;

        if (this.width > 0 || this.height > 0) {
            double aspectRatio = (double) videoWidth / (double) videoHeight;
            outWidth = Double.valueOf(this.height * aspectRatio).intValue();
            outHeight = Double.valueOf(outWidth / aspectRatio).intValue();
          } else {
            outWidth = videoWidth;
            outHeight = videoHeight;
        }

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", outWidth, outHeight);
        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mFrameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 3);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

        return format;

    }

    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        return null;
    }

}
