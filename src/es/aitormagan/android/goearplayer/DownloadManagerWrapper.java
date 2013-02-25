package es.aitormagan.android.goearplayer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.widget.EditText;
import android.widget.Toast;

public class DownloadManagerWrapper {
	
	private Context context;
	private DownloadManager downloadManager;
	private BroadcastReceiver broadcastReceiver;
	private File folderPath;
	
	public DownloadManagerWrapper(Context context, File folderPath) {
		
		this.context = context;
		this.folderPath = folderPath;
		this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
		
		this.broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                	//Obtener ID de la descarga completada
                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    
                    //Consultar información para dicha descarga
                    Query query = new Query();
                    query.setFilterById(downloadId);
                    Cursor c = downloadManager.query(query);
                    
                    //Acciones con la descarga completada
                    if (c.moveToFirst()) {
                        
                        //Obtener el estado de la descarga
                        int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int status = c.getShort(columnIndex);
                        
                    	//Obtener el titulo de la descarga
                        columnIndex = c.getColumnIndex(DownloadManager.COLUMN_TITLE);
                        String title = c.getString(columnIndex);
                        	                        
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
	                        	                        
		                    //Obtener el path de la descarga 
	                        columnIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
	                        String path = c.getString(columnIndex).replace("file://", "");
	                        	                        
	                        //Scannear el archivo
	                        scanDownload(path);
	                        
	                        //Indicar mediante un Toast que se ha completado
	                        String message = String.format(context.getString(R.string.downloadComplete), title);
							Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	                        //notificationStatusBar(message,true);
                        } else {
                        	String message = String.format(context.getString(R.string.downloadError), title);
                        	Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        };
        
        context.registerReceiver(this.broadcastReceiver, new IntentFilter(
                DownloadManager.ACTION_DOWNLOAD_COMPLETE));

	}
	
	private static boolean isSDCardMounted(){
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}
	
	public void unregisterReceiver() {
		this.context.unregisterReceiver(this.broadcastReceiver);
	}
	
	public void downloadFile(Uri uri, String messageToast, String messageNC, 
			String filePath){
		
		File fileToDownload = new File(this.folderPath, filePath);
		this.downloadFile(uri, messageToast, messageNC, fileToDownload);

	}
	
	private void notificationStatusBar(String message){
        //Obtener el manager de notificaciones
        NotificationManager mNotificationManager = (NotificationManager) 
        		this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        //Construir notificación
        int icon = android.R.drawable.stat_sys_download;
        
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, message, when);
        
        //Información Adicional para la notificación
        Context contextApp = this.context.getApplicationContext();
        CharSequence contentTitle = "Downloading: " + message;
        CharSequence contentText = message;
        Intent notificationIntent = new Intent(this.context, this.context.getClass());
        PendingIntent contentIntent = PendingIntent.getActivity(this.context, 0, notificationIntent, 0);

        notification.setLatestEventInfo(contextApp, contentTitle, contentText, contentIntent);
        
        //Notificar
        mNotificationManager.notify(1, notification);
        
        //Borrar la notificación al instante
        mNotificationManager.cancel(1);
	}
	
	private void scanDownload(String path){
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			File f = new File(decodedPath);
						
			if (f.exists())
				new SingleMediaScanner(this.context, f);
		} catch (UnsupportedEncodingException e) {
			// It is not possible...
		}

	}
	
	private void askFilePathToDownload(final Uri uri, final String messageToast, 
			final String messageNC, File path){
		AlertDialog.Builder alertInput = new AlertDialog.Builder(this.context);

		alertInput.setTitle(R.string.downloadExistsTitle);
		alertInput.setMessage(R.string.downloadExistsInfo);

		// Set an EditText view to get user input 
		final EditText input = new EditText(this.context);
		input.setText(path.getName());
		alertInput.setView(input);

		//Establecer las acciones a hacer cuando se pulsa el botón de comprobación
		alertInput.setPositiveButton(R.string.download, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				File filePath = new File(folderPath, input.getText().toString());
				downloadFile(uri, messageToast, messageNC, filePath);
			}
		});

		alertInput.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
				dialog.dismiss();
			}
		});

		alertInput.show();
	}
	
	private void downloadFile(Uri uri, String messageToast, String messageNC, 
			File fileToDownload){
		
		if (isSDCardMounted()){
			
			//Asegurarse de que la carpeta de descargas existe
			this.folderPath.mkdirs();

			if (fileToDownload.exists()){
				//Mostar información de que la descarga existe
				askFilePathToDownload(uri, messageToast, messageNC, fileToDownload);
			}else{
				//Descargar canción
				Toast.makeText(this.context, messageToast, Toast.LENGTH_LONG).show();
				this.notificationStatusBar(messageNC);
			
				//Construir request
				//Request request = new Request(Uri.parse(song.getMp3path()));
				Request request = new Request(uri);
				request.setTitle(messageNC);
				//request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_MUSIC, fileName);
				request.setDestinationUri(Uri.fromFile(fileToDownload));
				//request.allowScanningByMediaScanner();
				downloadManager.enqueue(request);
			}
		} else {
			Toast.makeText(this.context, R.string.unavailableSDCard, Toast.LENGTH_LONG).show();
		}
	}

}
