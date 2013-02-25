package es.aitormagan.android.goearplayer.searcher;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import es.aitormagan.android.goearplayer.Song;
import es.aitormagan.android.goearplayer.downloader.DownloadCompleteListener;
import es.aitormagan.android.goearplayer.downloader.DownloadFile;

public class Search implements DownloadCompleteListener{

	private String query;
	private SearchListener listener;
	private int currentPage = 0;
	private boolean end = false, searching = false;

	public Search(String query, SearchListener listener) {
		super();
		this.query = query;
		this.listener = listener;
	}

	public String getQuery() {
		return query;
	}

	public void nextPage() {
		
		if (!this.searching){
			if (!this.end) {
				//Obtener y formatear query
				final String queryFinal = "http://goear.com/apps/android/" +
						"search_songs_json.php?q=" + query.replace(' ', '+') + 
						"&p=" + this.currentPage;
	
				//Descarga empezada
				this.listener.searchStarted();
				this.searching = true;
	
				//Descargar archivo (ejecuta nuestro thread de descarga)
				DownloadFile downloader = new DownloadFile(this);
				downloader.execute(queryFinal);
			}
		}
	}

	public void downloadComplete(String result) {
				
		//Descarga finalizada
		this.searching = false;

		//Array para guardar las canciones
		ArrayList<Song> songs = new ArrayList<Song>();
		
		//Analizar JSON...
		try {
			JSONArray jArray = new JSONArray(result);

			for (int i = 0; i < jArray.length(); i++){
				String id = jArray.getJSONObject(i).getString("id");
				String title = jArray.getJSONObject(i).getString("title");
				String artist = jArray.getJSONObject(i).getString("artist");
				String mp3path = jArray.getJSONObject(i).getString("mp3path");

				//Añadir canciones a la lista
				Song song = new Song(id, title, artist,mp3path);
				songs.add(song);
			}

			//Indicar que ha cambiado el conjunto de elementos...
			this.listener.searchCompleted(songs);

			//Establecer final de búsqueda si el número de resultados no es 10
			if (jArray.length() < 10){
				this.end = true;
			}
		} catch (JSONException e) {
			if (result.startsWith("[0]")){
				this.end = true;

				//Mostrar un mensaje en función de la página de búsqueda en que estemos
				if (this.currentPage == 0) {
					this.listener.searchError(SearchError.NO_SONGS_FOUND);
				} else {
					this.listener.searchError(null);
				}
			}else{
				this.listener.searchError(SearchError.UNDEFINED_ERROR);
			}
		}
		
		this.currentPage++;
	}
}
