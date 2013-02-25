package es.aitormagan.android.goearplayer;

import java.io.File;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class SingleMediaScanner implements MediaScannerConnectionClient {

	private MediaScannerConnection mediaScanner;
	private File fileToScan;

	public SingleMediaScanner(Context context, File fileToScan) {
		this.fileToScan = fileToScan;
		this.mediaScanner = new MediaScannerConnection(context, this);
		this.mediaScanner.connect();
	}

	public void onMediaScannerConnected() {
		this.mediaScanner.scanFile(this.fileToScan.getAbsolutePath(), null);		
	}

	public void onScanCompleted(String path, Uri uri) {
		this.mediaScanner.disconnect();		
	}

}
