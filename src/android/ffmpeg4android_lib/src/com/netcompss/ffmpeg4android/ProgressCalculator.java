package com.netcompss.ffmpeg4android;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class ProgressCalculator {
	
	private int _durationOfCurrentWaitIndex = 0;
	private final int DURATION_OF_CURRENT_WAIT_INDEX_LIMIT = 12;
	private String _durationOfCurrent;
	private long _lastVklogSize = -1;
	private int _vkLogNoChangeCounter = 0;
	private SimpleDateFormat _simpleDateFormat;
	long _timeRef = -1;
	int  _prevProgress = 0;
	private String vkLogPath = null;
	
	public ProgressCalculator(String vkLogPathIn) {
		vkLogPath = vkLogPathIn;
		_simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SS");
		try {
			Date ref = _simpleDateFormat.parse("00:00:00.00");
			ref.setYear(112);
			_timeRef = ref.getTime();
		} catch (ParseException e) {
			Log.w(Prefs.TAG, "failed to set _timeRef");
		}
	}
	
	public void initCalcParamsForNextInter() {
		Log.i(Prefs.TAG, "initCalcParamsForNextInter");
		_lastVklogSize = -1;
		_vkLogNoChangeCounter = 0;
		_durationOfCurrent = null;

	}

			
	public int calcProgress() {
		//Log.i(Prefs.TAG, "========calc progress=======");
		int progress  = 0;
		if (_durationOfCurrent == null) {
			String dur = GeneralUtils.getDutationFromVCLogRandomAccess(vkLogPath);
			Log.d(Prefs.TAG, "dur: " + dur);
			if (dur == null || dur.equals("") || dur.equals("null") ) {
				Log.i(Prefs.TAG, "dur is not good, not setting ");
				if (_durationOfCurrentWaitIndex < DURATION_OF_CURRENT_WAIT_INDEX_LIMIT) {
					Log.i(Prefs.TAG, "waiting for real duration, going out of calcProgress with 0");
					_durationOfCurrentWaitIndex ++;
					return 0;
				}
				else {
					Log.i(Prefs.TAG, "_durationOfCurrentWaitIndex is equal to: " + DURATION_OF_CURRENT_WAIT_INDEX_LIMIT + " reseting.");
					_durationOfCurrentWaitIndex = 0;
					Log.i(Prefs.TAG, "setting fake Prefs.durationOfCurrent");

					_durationOfCurrent = "00:03:00.00";
					Log.w(Prefs.TAG, "setting fake Prefs.durationOfCurrent (Cant get from file): " + _durationOfCurrent);

				}
			}
			else {
				_durationOfCurrent = GeneralUtils.getDutationFromVCLogRandomAccess(vkLogPath);
				Log.i(Prefs.TAG, "duration: " + _durationOfCurrent + " \nTranscoding...");
			}
		}

		
		if (_durationOfCurrent != null) {
			
			long currentVkLogSize = -1;
			currentVkLogSize = GeneralUtils.getVKLogSizeRandomAccess(vkLogPath);
			//Log.d(Prefs.TAG, "currentVkLogSize: " + currentVkLogSize + " _lastVklogSize: " + _lastVklogSize);

			if (currentVkLogSize > _lastVklogSize) {
				_lastVklogSize = currentVkLogSize;
				_vkLogNoChangeCounter = 0;
			}
			else {
				//Log.w(Prefs.TAG, "Looks like Vk log is not increasing in size");
				_vkLogNoChangeCounter++;
			}

			
			String currentTimeStr = GeneralUtils.readLastTimeFromVKLogUsingRandomAccess(vkLogPath);
			//Log.d(Prefs.TAG, "currentTimeStr: " + currentTimeStr);
			if (currentTimeStr.equals("exit")) {
				Log.d(Prefs.TAG, "============Found one of the exit tokens in the log============");
				return 100;
			}
			else if (currentTimeStr.equals("error") && _prevProgress == 0) {
				Log.d(Prefs.TAG, "============Found error in the log============");
				return 100;
			}
			else if (_vkLogNoChangeCounter > 16) {
				Log.e(Prefs.TAG, "VK log is not changing in size, and no exit token found");
				return 100;
			}
			try {
				Date durationDate = _simpleDateFormat.parse(_durationOfCurrent);
				Date currentTimeDate = _simpleDateFormat.parse(currentTimeStr);
				currentTimeDate.setYear(112);
				durationDate.setYear(112);
				//Log.d(Prefs.TAG, " durationDate: " + durationDate + " currentTimeDate: " + currentTimeDate);
				
				long durationLong = durationDate.getTime() - _timeRef;
				long currentTimeLong = currentTimeDate.getTime() - _timeRef;
				//Log.d(Prefs.TAG, " durationLong: " + durationLong + " currentTimeLong: " + currentTimeLong + " diff: " + (durationLong - currentTimeLong));
				progress  = Math.round(((float)currentTimeLong / durationLong) * 100);
				if (progress >= 100) {
					Log.w(Prefs.TAG, "progress is 100, but can't find exit in the log, probably fake progress, still running...");
					progress = 99;
				}
				_prevProgress = progress;

				
			} catch (ParseException e) {
				Log.w(Prefs.TAG, e.getMessage());
			}
		}
		
		return progress;
	}


}
