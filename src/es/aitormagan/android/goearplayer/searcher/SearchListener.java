package es.aitormagan.android.goearplayer.searcher;

import java.util.ArrayList;
import es.aitormagan.android.goearplayer.Song;

public interface SearchListener {
	
	public void searchStarted();
	public void searchCompleted(ArrayList<Song> songs);
	public void searchError(SearchError error);

}
