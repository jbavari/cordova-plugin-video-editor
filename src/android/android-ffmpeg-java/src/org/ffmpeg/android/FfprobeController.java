package org.ffmpeg.android;


import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.ffmpeg.android.ShellUtils.ShellCallback;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

public class FfprobeController {

	
	private String mFfprobeBin;
	
	private final static String TAG = "FFPROBE";
	
	private String mCmdCat = "sh cat";
	
	public FfprobeController(Context context) throws FileNotFoundException, IOException {
		
		installBinaries(context, false);
	}
	
	public void installBinaries(Context context, boolean overwrite)
	{
		mFfprobeBin = installBinary(context, R.raw.ffprobe, "ffprobe", overwrite);
	}
	
	public String getBinaryPath ()
	{
		return mFfprobeBin;
	}
	
	private static String installBinary(Context ctx, int resId, String filename, boolean upgrade) {
		try {
			File f = new File(ctx.getDir("bin", 0), filename);
			if (f.exists()) {
				f.delete();
			}
			copyRawFile(ctx, resId, f, "0755");
			return f.getCanonicalPath();
		} catch (Exception e) {
			Log.e(TAG, "installBinary failed: " + e.getLocalizedMessage());
			return null;
		}
	}
	
	/**
	 * Copies a raw resource file, given its ID to the given location
	 * @param ctx context
	 * @param resid resource id
	 * @param file destination file
	 * @param mode file permissions (E.g.: "755")
	 * @throws IOException on error
	 * @throws InterruptedException when interrupted
	 */
	private static void copyRawFile(Context ctx, int resid, File file, String mode) throws IOException, InterruptedException
	{
		final String abspath = file.getAbsolutePath();
		// Write the iptables binary
		final FileOutputStream out = new FileOutputStream(file);
		final InputStream is = ctx.getResources().openRawResource(resid);
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
		// Change the permissions
		Runtime.getRuntime().exec("chmod "+mode+" "+abspath).waitFor();
	}

	
	
	public void execFFPROBE (List<String> cmd, ShellCallback sc, File fileExec) throws IOException, InterruptedException {
	
		enablePermissions();
		
		execProcess (cmd, sc, fileExec);
	}
	
	private void enablePermissions () throws IOException
	{
		Runtime.getRuntime().exec("chmod 700 " + mFfprobeBin);
    	
	}

	public void execFFPROBE (List<String> cmd, ShellCallback sc) throws IOException, InterruptedException {
		execFFPROBE(cmd, sc, new File(mFfprobeBin).getParentFile());
	}
	
	private int execProcess(List<String> cmds, ShellCallback sc, File fileExec) throws IOException, InterruptedException {		
        
		//ensure that the arguments are in the correct Locale format
		for (String cmd :cmds)
		{
			cmd = String.format(Locale.US, "%s", cmd);
		}
		
		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(fileExec);
		
		StringBuffer cmdlog = new StringBuffer();

		for (String cmd : cmds)
		{
			cmdlog.append(cmd);
			cmdlog.append(' ');
		}
		
		sc.shellOut(cmdlog.toString());
		
		//pb.redirectErrorStream(true);
		
		Process process = pb.start();    
    

		// any error message?
		StreamGobbler errorGobbler = new StreamGobbler(
				process.getErrorStream(), "ERROR", sc);

    	 // any output?
        StreamGobbler outputGobbler = new 
            StreamGobbler(process.getInputStream(), "OUTPUT", sc);

        errorGobbler.start();
        outputGobbler.start();

        int exitVal = process.waitFor();
        
        sc.processComplete(exitVal);
        
        return exitVal;
		
	}
	

	private int execProcess(String cmd, ShellCallback sc, File fileExec) throws IOException, InterruptedException {		
        
		//ensure that the argument is in the correct Locale format
		cmd = String.format(Locale.US, "%s", cmd);
		
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(fileExec);

	//	pb.redirectErrorStream(true);
    	Process process = pb.start();    
    	
    
    	  // any error message?
        StreamGobbler errorGobbler = new 
            StreamGobbler(process.getErrorStream(), "ERROR", sc);            
        
    	 // any output?
        StreamGobbler outputGobbler = new 
            StreamGobbler(process.getInputStream(), "OUTPUT", sc);
            
        // kick them off
        errorGobbler.start();
        outputGobbler.start();
     

        int exitVal = process.waitFor();
        
        sc.processComplete(exitVal);
        
        return exitVal;


		
	}
	
	private class StreamGobbler extends Thread
	{
	    InputStream is;
	    String type;
	    ShellCallback sc;
	    
	    StreamGobbler(InputStream is, String type, ShellCallback sc)
	    {
	        this.is = is;
	        this.type = type;
	        this.sc = sc;
	    }
	    
	    public void run()
	    {
	        try
	        {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
				StringBuilder result=new StringBuilder();
				while ((line = br.readLine()) != null)
					if (sc != null)
						result.append(line + System.lineSeparator());
				sc.shellOut(result.toString().trim());
	                
	            } catch (IOException ioe)
	              {
	             //   Log.e(TAG,"error reading shell slog",ioe);
	            	ioe.printStackTrace();
	              }
	    }
	}
}