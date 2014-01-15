var cordova = require('cordova'),
    exec = require('cordova/exec');

var VideoEditorPlugin = function() {
        this.options = {};
};

VideoEditorPlugin.prototype = {
    /*
        Add your plugin methods here
    */
    transcodeVideo: function transcodeVideo( fileUri, outputFileUri, quality, success, error ) {
        cordova.exec( success, error, "VideoEditorPlugin", "transcodeVideo", [fileUri, outputFileUri, quality] );
    }
};

var VideoEditorPluginInstance = new VideoEditorPlugin();

module.exports = VideoEditorInstance;
