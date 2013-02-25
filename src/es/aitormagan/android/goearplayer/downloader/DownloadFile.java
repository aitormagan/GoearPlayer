package es.aitormagan.android.goearplayer.downloader;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;

public class DownloadFile extends AsyncTask<String, Integer, String>{
	
	private DownloadCompleteListener downloadCompleteListener;
	
	public DownloadFile(DownloadCompleteListener downloadCompleteListener){
		this.downloadCompleteListener = downloadCompleteListener;
	}

	@Override
	protected String doInBackground(String... params) {
		int count;
		StringBuffer fileContent = new StringBuffer();
		
		try {
			URL url = new URL(params[0]);
			URLConnection conexion = url.openConnection();
			conexion.connect();
			// this will be useful so that you can show a tipical 0-100% progress bar
			int lenghtOfFile = conexion.getContentLength();

			// download the file
			InputStream input = new BufferedInputStream(url.openStream());
			//OutputStreamWriter output = new OutputStreamWriter(this.goearPlayerActivity.openFileOutput(params[1],0));
			byte data[] = new byte[1024];

			long total = 0;

			while ((count = input.read(data)) != -1) {
				total += count;
				// publishing the progress....
				publishProgress((int)(total*100/lenghtOfFile));
				String dataString = new String(data);
				//output.append(dataString, 0, count);
				fileContent.append(dataString);
			}

			//output.flush();
			//output.close();
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return fileContent.toString();
	}
	
	protected void onPostExecute(String result) {
		this.downloadCompleteListener.downloadComplete(result);
    }

}
