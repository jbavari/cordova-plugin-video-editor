//
//  VideoEditor.js
//
//  Created by Josh Bavari on 01-14-2014
//

var exec = require('cordova/exec');

function VideoEditorPlugin() {
}

VideoEditorPlugin.prototype.transcodeVideo = function(fileUri, outputFileUri, quality, outputFileType, optimizeForNetworkUse, success, error) {
    exec(success, error, 'VideoEditorPlugin', 'transcodeVideo', 
    	[fileUri, outputFileUri, quality, outputFileType, optimizeForNetworkUse]);
};

module.exports = new VideoEditorPlugin();