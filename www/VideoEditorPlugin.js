//
//  VideoEditor.js
//
//  Created by Josh Bavari on 01-14-2014
//  Modified by Ross Martin on 01-29-15
//

var exec = require('cordova/exec');

function VideoEditorPlugin() {
}

VideoEditorPlugin.prototype.transcodeVideo = function(fileUri, fileName, quality, outputFileType, optimizeForNetworkUse, success, error) {
    exec(success, error, 'VideoEditorPlugin', 'transcodeVideo', 
    	[fileUri, fileName, quality, outputFileType, optimizeForNetworkUse]);
};

module.exports = new VideoEditorPlugin();