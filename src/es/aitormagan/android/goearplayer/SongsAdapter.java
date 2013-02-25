package es.aitormagan.android.goearplayer;

import java.util.ArrayList;

import es.aitormagan.android.goearplayer.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SongsAdapter extends BaseAdapter{
	
	//Atributos
	private ArrayList<Song> list;
	private Context mContext;
	private LayoutInflater inflator;

	/**
	 * Crear el adaptador de la lista
	 * @param context El contexto de la canción
	 * @param list La lista de canciones
	 */
	public SongsAdapter(Context context, ArrayList<Song> list){
		this.mContext=context;
		this.inflator = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.list = list;
	}

	public int getCount() {
		return list.size();
	}

	public Object getItem(int position) {
		return list.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		//Attributes
		View v = convertView;
		final MainListHolder mHolder;

		if (convertView == null){
			mHolder = new MainListHolder();
			v = inflator.inflate(R.layout.song, null);
			mHolder.title = (TextView) v.findViewById(R.id.song);
			mHolder.artist = (TextView) v.findViewById(R.id.artist);
			v.setTag(mHolder);
		} else {
			mHolder = (MainListHolder) v.getTag();
		}

		mHolder.title.setText(list.get(position).getTitle());
		mHolder.artist.setText(list.get(position).getArtist());

		return v;
	}
	
	private class MainListHolder {
		private TextView title;
		private TextView artist;
	}

}