var cordova = require('cordova'),
    exec = require('cordova/exec');

var VideoEditor = function() {
        this.options = {};
};

VideoEditor.prototype = {
    /*
        Add your plugin methods here
    */
    transcodeVideo: function transcodeVideo( fileUri, quality, success, error ) {
        cordova.exec( success, error, "VideoEditor", "transcodeVideo", [fileUri, quality] );
    }
};

var VideoEditorInstance = new VideoEditor();

module.exports = VideoEditorInstance;
