//
//  VideoEditorConstants.js
//
//  Created by Josh Bavari on 01-14-2014
//

var VideoEditorOptions = {
	Quality: {
		HIGH_QUALITY: 0
		MEDIUM_QUALITY: 1,
		LOW_QUALITY: 2,
	},
	OptimizeForNetworkUse: {
		NO: 0,
		YES: 1
	},
	OutputFileType: {
		M4V: 0,
		MPEG4: 1,
		M4A: 2,
		QUICK_TIME: 3
	}
};

if (typeof exports !== 'undefined') {
  if (typeof module !== 'undefined' && module.exports) {
    exports = module.exports = VideoEditorOptions;
  }
}