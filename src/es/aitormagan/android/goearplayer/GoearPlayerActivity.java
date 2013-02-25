package es.aitormagan.android.goearplayer;

import java.io.File;

import java.util.ArrayList;

import es.aitormagan.android.goearplayer.R;
import es.aitormagan.android.goearplayer.searcher.Search;
import es.aitormagan.android.goearplayer.searcher.SearchError;
import es.aitormagan.android.goearplayer.searcher.SearchListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class GoearPlayerActivity extends Activity implements SearchListener {

	//Attributes
	private ProgressDialog searchDialog;
	private ListView list;
	private EditText searchField;
	private Button searchButton;
	private SeekBar playerSeekBar;
	private TextView positionText;
	private TextView durationText;
	private TextView infoSongText;
	private ImageButton playPause;
	private MediaPlayer mediaPlayer;
	private ProgressBar loading;
	private ArrayList<Song> songs;
	private SongsAdapter songsAdapter;
	private Search currentSearch;
	private Handler mediaPlayerHandler = new Handler();
	private DownloadManagerWrapper songsDownloadManager;
	private Song songPlaying;
	private File songsFolderPath;
	

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//Crear ventana
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		//Inicializar la carpeta de descargas
		this.songsFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

		//Inicializar array donde estarán almacenadas las canciones
		this.songs = new ArrayList<Song>();
		
		//Inicializar el donwloadManager
		this.songsDownloadManager = new DownloadManagerWrapper(this, this.songsFolderPath);
		//this.downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

		//Inicializar los elementos visuales de la aplicación
		this.initializeUI();

		//Desactivar el botón de play/pause y la brra de desplazamiento
		this.playPause.setClickable(false);
		this.playerSeekBar.setEnabled(false);
		this.restartPlayerInfo();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.main);

		//Obtener la información de antes de que se produjese el cambio
		String lastSearch = this.searchField.getText().toString();
		String positionInfo = this.positionText.getText().toString();
		String durationInfo = this.durationText.getText().toString();
		String songInfo = this.infoSongText.getText().toString();
		int progressMain = this.playerSeekBar.getProgress();
		int progressSecondary = this.playerSeekBar.getSecondaryProgress();
		int loadingVisibility = this.loading.getVisibility();
		int playPauseVisibility = this.playPause.getVisibility();
		
		//Inicializar los elementos visuales de la aplicación
		this.initializeUI();

		//Establecer la información de antes de que se produjese el cambio
		//en los nuevos componentes
		this.searchField.setText(lastSearch);
		this.positionText.setText(positionInfo);
		this.durationText.setText(durationInfo);
		this.infoSongText.setText(songInfo);
		this.playerSeekBar.setProgress(progressMain);
		this.playerSeekBar.setSecondaryProgress(progressSecondary);
		this.loading.setVisibility(loadingVisibility);
		this.playPause.setVisibility(playPauseVisibility);
		
		//Configurar player
		if (this.mediaPlayer == null){
			this.playPause.setClickable(false);
			this.playerSeekBar.setEnabled(false);
		} else if (this.mediaPlayer.isPlaying()) {
			this.playPause.setImageResource(android.R.drawable.ic_media_pause);
		} else {
			this.playPause.setImageResource(android.R.drawable.ic_media_play);
		}
		
	}

	//@Override
	public void finish(){
		super.finish();

		//Quitar todos los mensajes a la espera para actualizar el player
		this.mediaPlayerHandler.removeCallbacksAndMessages(null);

		//Parar reproductor
		if (this.mediaPlayer != null)
			this.mediaPlayer.release();
		
		//Quitar broadcastreceiver
		songsDownloadManager.unregisterReceiver();
	}
	
	public void restartPlayerInfo(){
		//Espacios añadidos para que haya espacio con la barra de búsqueda
		this.positionText.setText("00:00 ");
		this.durationText.setText(" 00:00");
		this.infoSongText.setText("");

	}

	public void initializeUI(){
		this.list = (ListView) findViewById(R.id.listView);
		this.searchButton = (Button) findViewById(R.id.searchButton);
		this.searchField = (EditText) findViewById(R.id.searchField);
		this.playPause = (ImageButton) findViewById(R.id.playPause);
		this.playerSeekBar = (SeekBar) findViewById(R.id.playerSeekBar);
		this.positionText = (TextView) findViewById(R.id.positionText);
		this.durationText = (TextView) findViewById(R.id.durationText);
		this.infoSongText = (TextView) findViewById(R.id.infoSongText);
		this.loading = (ProgressBar) findViewById(R.id.loadingBar);

		//Listener al botón de parar y reproducir
		this.playPause.setOnClickListener(new OnClickListener() {

			public void onClick(View arg0) {
				if (mediaPlayer != null){
					if (mediaPlayer.isPlaying()){
						mediaPlayer.pause();
						playPause.setImageResource(android.R.drawable.ic_media_play);
					}else{
						mediaPlayer.start();
						playPause.setImageResource(android.R.drawable.ic_media_pause);

						//Iniciar actualizador de la barra de estado
						startPlayProgressUpdater();
					}
				}				
			}
		});

		//Añadir listener para la lista
		this.list.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> a, View v, int position, long id) { 
				final Song song = (Song) list.getItemAtPosition(position);
				//Toast.makeText(GoearPlayerActivity.this, "You have chosen: " + " " + o.getArtist(), Toast.LENGTH_LONG).show();

				final CharSequence[] items = {getString(R.string.play), 
						getString(R.string.download)};

				AlertDialog.Builder builder = new AlertDialog.Builder(GoearPlayerActivity.this);
				builder.setTitle(R.string.chooseAnOption);
				builder.setItems(items, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						if (item == 0){
							if (songPlaying == null || !songPlaying.equals(song)){
								playSong(song);
							} else {
								if (!mediaPlayer.isPlaying()){
									mediaPlayer.start();
									playPause.setImageResource(android.R.drawable.ic_media_pause);

									//Iniciar actualizador de la barra de estado
									startPlayProgressUpdater();								
								} else {
									Toast.makeText(GoearPlayerActivity.this, 
											R.string.alreadyPlaying, 
											Toast.LENGTH_LONG).show();
								} 
							}
						} else if (item == 1) {
							downloadSong(song);
						}
					}
				});

				AlertDialog alert = builder.create();
				alert.show();	
			}
		});

		//Añadir listener para cargar más elementos cuando se llegue al final
		this.list.setOnScrollListener(new OnScrollListener() {
			private int priorFirst = -1;

			public void onScroll(final AbsListView view, final int first, 
					final int visible, final int total) {
				// detect if last item is visible
				if (visible < total && (first + visible == total)) {
					// see if we have more results
					if (first != priorFirst) {
						priorFirst = first;

						//Nueva Búsqueda
						currentSearch.nextPage();
					}
				}
			}

			public void onScrollStateChanged(AbsListView view, 
					int scrollState) {
				

			}
		});

		//Asociar el adapter de la lista
		this.songsAdapter = new SongsAdapter(this, this.songs);
		this.list.setAdapter(songsAdapter);

		//Añadir listener para buscar cuando pulse "FIN"
		this.searchField.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
								
				if (keyCode == 66 && event.getAction() == KeyEvent.ACTION_UP){
					newSearch();
				}

				return false;
			}
		});

		//Asociar listener al botón de búsqueda
		this.searchButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				newSearch();
			}
		});

		//Asociar listener a la barra de búsqueda para cambiar de posición según el lugar
		this.playerSeekBar.setOnSeekBarChangeListener(
				new OnSeekBarChangeListener() {

			public void onStopTrackingTouch(SeekBar seekBar) {
				if (mediaPlayer != null){
					int duration = mediaPlayer.getDuration();
					double percent = (double) seekBar.getProgress() / (double) seekBar.getMax();
					int position = (int) (duration * percent);

					mediaPlayer.seekTo(position);

					//Actuaizar barra y posición
					updateBarAndText();
				}else
					//Progress a cero si el player no está inicializado
					playerSeekBar.setProgress(0);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {

			}
		});
	}

	public void newSearch(){
		//Cerrar teclado
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);

		//Crear nueva búsqueda
		String query = searchField.getText().toString();
		this.currentSearch = new Search(query, this);
		
		//Borrar búsqueda anterior
		this.songs.clear();
		this.songsAdapter.notifyDataSetChanged();

		//Buscar
		this.currentSearch.nextPage();
	}
	
	private void closeDialog() {
		//Cerrar diálogo
		if (this.searchDialog != null) {
			this.searchDialog.dismiss();
		}
		
		this.searchDialog = null;
	}
	
	public void searchStarted() {
		//Mensaje de espera
		this.searchDialog = ProgressDialog.show(this, "", 
				getString(R.string.loading), true);
	}
	
	public void searchCompleted(ArrayList<Song> songs) {
		
		this.closeDialog();
		
		//Mostrar lista de canciones
		this.songs.addAll(songs);
		this.songsAdapter.notifyDataSetChanged();
	}
	
	public void searchError(SearchError error) {
				
		this.closeDialog();
		
		//Mostrar error
		if (error == SearchError.UNDEFINED_ERROR) {
			Toast.makeText(this, R.string.undefinedError, 
					Toast.LENGTH_LONG).show();
		} else if (error == SearchError.NO_SONGS_FOUND) {
			Toast.makeText(this, R.string.noSongsFound, 
					Toast.LENGTH_LONG).show();
		}
	}
	
	private String getStringTime (double time){
		time = time / 1000;
		int timeMins = (int) time / 60;
		int timeSecs = (int) time % 60;
		
		String timeSecsString;
		String timeMinsString;

		if (timeSecs < 10)
			timeSecsString = "0" + timeSecs;
		else
			timeSecsString = Integer.valueOf(timeSecs).toString();

		if (timeMins < 10)
			timeMinsString = "0" + timeMins;
		else
			timeMinsString = Integer.valueOf(timeMins).toString(); 
		
		return timeMinsString + ":" + timeSecsString;
	}

	public void updateBarAndText(){
		if (this.mediaPlayer != null && this.mediaPlayer.getDuration() > 0){
			//Update bar
			double currentPosition = this.mediaPlayer.getCurrentPosition();
			double totalLength = this.mediaPlayer.getDuration();
			double position = currentPosition * this.playerSeekBar.getMax() / totalLength;
			int seekBarPosition = (int) position;
			this.playerSeekBar.setProgress(seekBarPosition);

			//Update position text
			String currentPositionString = getStringTime(currentPosition);
			String totalTimeString = getStringTime(totalLength);

			//Espacios añadidos para que haya espacios con la barra de búsqueda
			this.positionText.setText(currentPositionString + " ");
			this.durationText.setText(" " + totalTimeString);
			
			//Updae song/artist info
			int currentPosInt = (int) currentPosition / 1000;
			
			if (currentPosInt % 10 == 0 || currentPosInt == 0) {
				this.infoSongText.setText(this.songPlaying.getArtist());
			}else if (currentPosInt % 5 == 0) {
				this.infoSongText.setText(this.songPlaying.getTitle());
			}
		}
	}

	public void startPlayProgressUpdater() {
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			updateBarAndText();

			Runnable notification = new Runnable() {
				public void run() {
					startPlayProgressUpdater();
				}
			};
			this.mediaPlayerHandler.postDelayed(notification,1000);
		}
	} 

	public void playSong(Song song){
		//Almacenar la canción que se está reproduciendo
		this.songPlaying = song;
		
		//Borrar el contenido de la barra de información de la canción
		this.restartPlayerInfo();
		
		//Quitar el botón de play pause
		this.playPause.setClickable(false);
		this.playPause.setVisibility(View.INVISIBLE);

		//Mostrar el icono de carga
		this.loading.setVisibility(View.VISIBLE);

		//Hacer que el slider no este activo
		this.playerSeekBar.setEnabled(false);


		try {
			String url = song.getMp3path(); // your URL here

			//Borrar el reproductor anterior
			if (this.mediaPlayer != null){
				this.mediaPlayer.stop();
				this.mediaPlayer.release();
				this.mediaPlayer = null;
			}

			//Crear nuevo mediaplayer
			this.mediaPlayer = new MediaPlayer();

			//Asociar acciones cuando se haya preparado...
			this.mediaPlayer.setOnPreparedListener(new OnPreparedListener() {

				public void onPrepared(MediaPlayer mp) {
					mediaPlayer.start();

					//Mostrar información de la reproducción
					String message = String.format(getString(R.string.playing), 
							songPlaying.getTitle(), songPlaying.getArtist());
					Toast.makeText(GoearPlayerActivity.this, message, 
							Toast.LENGTH_LONG).show();
					
					//Mostrar en la barra
					infoSongText.setText(songPlaying.getArtist());

					//Ocultar el icono de carga
					loading.setVisibility(View.INVISIBLE);

					//Mostrar el botón de play/pause con el icono de pause
					playPause.setVisibility(View.VISIBLE);
					playPause.setImageResource(android.R.drawable.ic_media_pause);

					//Activar el botón de play/pause y el slider
					playPause.setClickable(true);
					playerSeekBar.setEnabled(true);

					//Iniciar thread para actualizar la barra de búsqueda
					startPlayProgressUpdater();
				}
			});

			this.mediaPlayer.setOnBufferingUpdateListener(
					new OnBufferingUpdateListener() {
				
				public void onBufferingUpdate(MediaPlayer mp, int percent) {
					//System.out.println("BUFFERED " + percent);
					playerSeekBar.setSecondaryProgress(percent);
				}
			});

			//Asociar un listener para cambiar el botón cuando la canción acabe
			this.mediaPlayer.setOnCompletionListener(new OnCompletionListener() {

				public void onCompletion(MediaPlayer mp) {
					//Cambiar el botón a reproducir
					playPause.setImageResource(android.R.drawable.ic_media_play);
				}
			});

			//this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			this.mediaPlayer.setDataSource(url);
			this.mediaPlayer.prepareAsync(); // might take long! (for buffering, etc)
		} catch (Exception e) {
			//Mostrar información del error
			String message = String.format(getString(R.string.errorLoadingSongToPlay), 
					song.getTitle(), song.getArtist());
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		}
	}
	
	public void downloadSong(Song song){
		//Obtener el nombre del fichero
		String fileName = song.getPathToDownload();
		
		//Descargar la canción
		String messageToast = String.format(this.getString(R.string.downloading), 
				song.getTitle(), song.getArtist());
		this.songsDownloadManager.downloadFile(Uri.parse(song.getMp3path()), 
				messageToast, song.getArtist() + " - " + song.getTitle(), 
				fileName);
	}
	
}