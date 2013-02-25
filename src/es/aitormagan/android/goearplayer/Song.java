package es.aitormagan.android.goearplayer;

public class Song {
	
	private final String id;
	private final String title;
	private final String artist;
	private String mp3path;
	
	public Song(String id, String title, String artist, String mp3path) {
		super();
		
		//Corregir artist and title
		artist = artist.replace("&atilde;", "�");
		artist = artist.replace("&etilde;", "�");
		artist = artist.replace("&itilde;", "�");
		artist = artist.replace("&otilde;", "�");
		artist = artist.replace("&utilde;", "�");
		artist = artist.replace("&ntilde;", "�");
		
		title = title.replace("&atilde;", "�");
		title = title.replace("&etilde;", "�");
		title = title.replace("&itilde;", "�");
		title = title.replace("&otilde;", "�");
		title = title.replace("&utilde;", "�");
		title = title.replace("&ntilde;", "�");

		this.id = id;
		this.title = title;
		this.artist = artist;
		this.mp3path = mp3path;
	}
	
	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}
	
	public String getArtist() {
		return artist;
	}

	public String getMp3path() {
		return mp3path;
	}

	public void setMp3path(String mp3path) {
		this.mp3path = mp3path;
	}
	
	public String getPathToDownload(){
		return this.artist + " - " + this.title + ".mp3";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Song){
			Song other = (Song) obj;
			return other.id.equals(this.id);
		}
		
		return false;
	}
	
	
}
