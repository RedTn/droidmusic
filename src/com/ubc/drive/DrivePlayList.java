package com.ubc.drive;

/*Stores the song meta data and content in here
 * 
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import android.util.Log;

public class DrivePlayList{
	
	private ArrayList<String> playlist;
	private ArrayList<Integer> playlistIndex;
	private String playlistTitle;
	private String downloadURL;
	
	public DrivePlayList(InputStream stream, String downloadURL, String title){
		playlist = new ArrayList<String>();
		playlistIndex = new ArrayList<Integer>();
		this.playlistTitle = title;
		readFromFile(stream);
		this.downloadURL = downloadURL;
	}

	/*Parse play list file from drive
	 * 
	 */
	private void readFromFile(InputStream stream) {
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		String line;
		try {
			line = br.readLine(); //skip first line which is used as a key
			while ((line = br.readLine()) != null) {
				
				//Title and index is split by a "&"
				String split[] = line.split("&");
				playlist.add(split[0]);
				playlistIndex.add(Integer.valueOf(split[1]));
			}

			br.close();
		} catch (IOException e) {
			Log.i("Drive", "IoHappened");
			e.printStackTrace();
		}
	}
	
	public ArrayList<String> getPlaylist(){
		return playlist;
	}
	
	public ArrayList<Integer> getPlaylistIndex(){
		return playlistIndex;
	}
	
	public String getPlaylistTitle(){
		return playlistTitle;
	}
	
	public String getPlaylistURL(){
		return downloadURL;
	}
}
	
	
	
