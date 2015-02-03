FFmpeg4Android Demo.


* By default, it will run the UI edit box command, you can change the command in the edit box, 
  or replace the command in the code (search for commandStr).
  
* Its highly recommended to use the complexCommand (String Array), as it supports all types of commands, and filenames with spaces, ]]
  and special characters.
 
* After running the demo, the out.mp4 will be created in the /sdcard/videokit folder.
 
* Supported devices: this will work on ARMv7 and above devices (most devices today are ARMv7 or above).
  If you use an emulator, make sure you select devices that support ARMv7 or above (e.g Nexus 7).
 
* Note that the Simple Example does not have stop support, so in-case you will not let the operation end, it can interfere with the next
  Transcoding operation.
  