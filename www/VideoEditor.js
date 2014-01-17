//
//  VideoEditor.js
//
//  Created by Josh Bavari on 01-14-2014
//

var cordova = require('cordova'),
    exec = require('cordova/exec');

var VideoEditorPlugin = function() {
        this.options = {};
};

VideoEditorPlugin.prototype = {
    transcodeVideo: function transcodeVideo( fileUri, outputFileUri, quality, outputFileType, optimizeForNetworkUse, success, error ) {
        cordova.exec( success, error, "VideoEditorPlugin", "transcodeVideo", 
        	[fileUri, outputFileUri, quality, outputFileType, optimizeForNetworkUse] );
    }
};

var VideoEditorPluginInstance = new VideoEditorPlugin();

// module.exports = VideoEditorInstance;


if (typeof exports !== 'undefined') {
  if (typeof module !== 'undefined' && module.exports) {
    exports = module.exports = VideoEditorInstance;
  }
}