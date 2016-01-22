
module.exports = {

    trim: function (win, fail, args) {
        //get args from cordova app
        var options = args[0];
        var comp, outboundFilePath;

        //look up the video file
        Windows.Storage.StorageFile.getFileFromApplicationUriAsync(new Windows.Foundation.Uri(options.fileUri)).then(function (file) {
            //create a clip from the video
            return Windows.Media.Editing.MediaClip.createFromFileAsync(file);
        }).then(function (clip) {
            //apply the trims, which are in milliseconds
            clip.trimTimeFromStart = options.trimStart * 1000;
            clip.trimTimeFromEnd = (options.trimEnd * 1000);

            //setup a comp
            comp = new Windows.Media.Editing.MediaComposition();
            comp.clips.push(clip);

            //create an outbound file location
            return Windows.Storage.ApplicationData.current.localFolder.createFileAsync(options.outputFileName, Windows.Storage.CreationCollisionOption.replaceExisting);
        }).then(function (outboundFile) {
            outboundFilePath = outboundFile.path;
            //render the trimmed video to file
            return comp.renderToFileAsync(outboundFile);
        }).done(function (file) {
            //return the path to the success method
            win(outboundFilePath);
        });
    },

    createThumbnail: function (win, fail, args) {
        //get args from cordova app
        var options = args[0];
        var comp, outputStream, writer, reader, thumbnailStream, outboundFilePath;

        //look up the video file
        Windows.Storage.StorageFile.getFileFromApplicationUriAsync(new Windows.Foundation.Uri(options.fileUri)).then(function (file) {
            //create clip from video
            return Windows.Media.Editing.MediaClip.createFromFileAsync(file);
        }).then(function (clip) {
            //setup a comp
            comp = new Windows.Media.Editing.MediaComposition();
            comp.clips.push(clip);

            //create an outbound file location
            return Windows.Storage.ApplicationData.current.localFolder.createFileAsync(options.outputFileName, Windows.Storage.CreationCollisionOption.replaceExisting);
        }).then(function (outboundFile) {
            //store outbound file location in a temp variable
            outboundFilePath = outboundFile.path;
            //open the file for writing
            return outboundFile.openAsync(Windows.Storage.FileAccessMode.readWrite);
        }).then(function (outboundStream) {
            //prepare the output stream
            outputStream = outboundStream.getOutputStreamAt(0);
            //take a snip from the video
            return comp.getThumbnailAsync(0, 1080, 920, Windows.Media.Editing.VideoFramePrecision.nearestFrame);
        }).then(function (thumbnail) {
            //keep reference so we can dispose later
            thumbnailStream = thumbnail;
            //keep reference so we can dispose later
            reader = new Windows.Storage.Streams.DataReader(thumbnailStream.getInputStreamAt(0));

            //load the thumbprint
            return reader.loadAsync(thumbnailStream.size);
        }).then(function () {
            //keep reference so we can dispose later
            writer = new Windows.Storage.Streams.DataWriter(outputStream);
            //writer to the buffer
            while (reader.unconsumedBufferLength > 0) {
                writer.writeBuffer(reader.readBuffer(((reader.unconsumedBufferLength > 64) ? 64 : reader.unconsumedBufferLength)));
            }
            //transfer the buffer and write
            return outputStream.writeAsync(writer.detachBuffer());
        }).then(function (bytesWritten) {
            console.log('Bytes written ' + bytesWritten);
            //clear the stream
            return outputStream.flushAsync();
        }).done(function (outboundFile) {
            //dispose all references
            writer.close();
            reader.close();
            thumbnailStream.close();
            //call win function for cordova
            win(outboundFilePath);
        });
    },

    transcodeVideo: function (win, fail, args) {
        var videoQualities = Windows.Media.MediaProperties.VideoEncodingQuality;

        //get args from cordova app
        var options = args[0];
        //easier way to access the quality
        var qualities = [videoQualities.hd1080p, videoQualities.hd720p, videoQualities.wvga];
        //chosen quality
        var quality = qualities[options.quality];
        var mediaProfile, sourceFile, destinationFile, duration = options.duration, optimize = options.optimizeForNetworkUse;

        switch (options.outputFileType) {
            //both m4v and mpeg4 transcode to mp4
            case 0: 
            case 1:{
                mediaProfile = Windows.Media.MediaProperties.MediaEncodingProfile.createMp4(quality);
                break;
            }
            //m4a transcoded with m4a
            case 2:{
                mediaProfile = Windows.Media.MediaProperties.MediaEncodingProfile.createM4a(quality);
                break;
            }
            //we don't support anything more
            default:{
                throw 'output file type not supported on windows with this format';
                break;
            }
        
        }

        //get the source file
        Windows.Storage.StorageFile.getFileFromApplicationUriAsync(new Windows.Foundation.Uri(options.fileUri)).then(function (source) {
            sourceFile = source;
            //create the destination file
            return Windows.Storage.ApplicationData.current.localFolder.createFileAsync(options.outputFileName, Windows.Storage.CreationCollisionOption.replaceExisting);
        }).then(function (destination) {
            destinationFile = destination;

            var transcoder = new Windows.Media.Transcoding.MediaTranscoder();
            //quality over speed and performance 
            if (!optimize) {
                transcoder.videoProcessingAlgorithm = Windows.Media.Transcoding.MediaVideoProcessingAlgorithm.mrfCrf444
            }
            //prepare transcoding
            return transcoder.prepareFileTranscodeAsync(sourceFile, destinationFile, mediaProfile);
        }).then(function (preparedTranscode) {
            //perform the transcode
            return preparedTranscode.transcodeAsync();
        }).done(function () {
            //return the destination file path
            win(destinationFile.path);
        }, function (details) {
            //failed
            fail(details);
        });
    }
}

require("cordova/exec/proxy").add("VideoEditor", module.exports);