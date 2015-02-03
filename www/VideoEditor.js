//
//  VideoEditor.js
//
//  Created by Josh Bavari on 01-14-2014
//  Modified by Ross Martin on 01-29-15
//

var exec = require('cordova/exec'),
	pluginName = 'VideoEditorPlugin';

function VideoEditor() {
}

VideoEditor.prototype.transcodeVideo = function(success, error, options) {
	// options = fileUri, outputFileName, quality, outputFileType, optimizeForNetworkUse
    exec(success, error, pluginName, 'transcodeVideo', [options]);
};

module.exports = new VideoEditor();