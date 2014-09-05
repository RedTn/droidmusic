package com.ubc.music;

import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ubc.drive.DrivePlayList;
import com.ubc.drive.GoogleDriveActivity;
import com.ubc.music.R;
import com.ubc.social.ShareActivity;
public class MusicFragment extends Fragment implements  OnClickListener, OnMenuItemClickListener{

	/**
	 * The layout of the fragment
	 */
	private LinearLayout musicFragmentLayout;

	/**
	 * View for the playlist
	 */
	private static ListView playlistView;

	/**
	 * Actual playlist
	 */
	private static ArrayList<String>playlist;
	
	private static ArrayList<Integer>playlistIndex;
	private static ArrayList<DrivePlayList> drive_List_Of_Playlists;

	private ImageButton playButton;
	private ImageButton	pauseButton;
	private ImageButton stopButton;
	private ImageButton volumeDownButton;
	private ImageButton	volumeUpButton;
	private ImageButton shuffleButton;
	private ImageButton playlistButton;
	private ImageButton loopButton;
	private int loopButtonState;
	private SeekBar volumeBar;
	private SeekBar songSeekBar;
	private static final int LOOP_UNPRESSED = 0;
	private static final int LOOP_SONG_ONLY = 1;
	private static final int LOOP_PLAY_ALL = 2;
	private static final String contextGooglePlus = "Share on Google+";
	private static final String contextCancel = "Cancel";
	private static final String contextAddToPlaylist = "Add to a Playlist";
	private static final String contextRemoveSong = "Remove Song from Playlist";

	private String lastSongTitle;
	private View lastSelectedSongOnPlayList= null;
	private int lastSongPosition;

	private RS232 rs232;
	private MusicPlayer musicPlayer;
	
	//Total time for the current song. Used for seekbar
	private long totalTime;
	private long currentTime;
	private long pauseTime = 0;
	private long resumeTime = 0;

	private TimerTask seekBarTimerTask;
	private Timer seekBarTimer;
	
	private int loopCounter=0;
	
	/**
	* Creates MusicPlayer, SeekBarTimer, and Timer instances.
	*/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		rs232 = ((MainActivity)getActivity()).getRS232();
		musicPlayer = new MusicPlayer(rs232);
		seekBarTimerTask = new SeekBarTimer();
		seekBarTimer = new Timer();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		/**
		 * assigning the layout to the private instance variable
		 * so that it can be used later instead of getActivity()
		 */
		musicFragmentLayout = (LinearLayout) inflater.inflate(R.layout.fragment_music, container, false);
		initializePlaylistAndView();
		
		songSeekBar =  (SeekBar) musicFragmentLayout.findViewById(R.id.seekBar);
		songSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				//Do Nothing
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				//Do Nothing
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				char bytes[] = {0x01, 0x13};
				long progressTime = songSeekBar.getProgress();
				Integer progressTimeInSeconds = (int)(progressTime/1000)+1;
				Log.i("SEEK BAR", Long.toString(progressTimeInSeconds));
				String protocolStr = new String(bytes);			
				char timeByte[] = {(char) progressTimeInSeconds.byteValue()};
				String timeStr = new String(timeByte);
				final String seekMsg = new String(protocolStr+timeStr);
				Log.i("SeekMsg", seekMsg);
				rs232.addToSendBuffer(seekMsg);
				currentTime = System.currentTimeMillis() - progressTime;
			}
			
		});
		//Loads view instances
		pauseButton = (ImageButton) musicFragmentLayout.findViewById(R.id.pauseButton);
        playButton = (ImageButton) musicFragmentLayout.findViewById(R.id.playButton);
        stopButton = (ImageButton) musicFragmentLayout.findViewById(R.id.stopButton);
        volumeDownButton = (ImageButton) musicFragmentLayout.findViewById(R.id.volumeDownButton);
        volumeUpButton = (ImageButton) musicFragmentLayout.findViewById(R.id.volumeUpButton);
        playlistButton = (ImageButton) musicFragmentLayout.findViewById(R.id.playlistButton);
        shuffleButton = (ImageButton) musicFragmentLayout.findViewById(R.id.shuffleButton);
        loopButton = (ImageButton) musicFragmentLayout.findViewById(R.id.loopButton);

		// Attach listeners
        pauseButton.setOnClickListener(this);
        playButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        volumeDownButton.setOnClickListener(this);
        volumeUpButton.setOnClickListener(this);
        playlistButton.setOnClickListener(this);
        shuffleButton.setOnClickListener(this);
        loopButton.setOnClickListener(this);
        loopButtonState = LOOP_UNPRESSED;
		return musicFragmentLayout;
	}

	/**
	 * Attaches listeners or adaptors to buttons and views
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);

		/**
		 * Attach a listener to the option button
		 */
		ImageButton button= (ImageButton) musicFragmentLayout.findViewById(R.id.optionButton);
		button.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				openOption(v);				
			}
		});

		/**
		 * Attach ImageAdapter to the gallery..
		 * ImageAdapter contains all the images to be displayed.
		 */
		((Gallery) musicFragmentLayout.findViewById(R.id.musicPhotoGallery)).setAdapter(new ImageAdapter(getActivity()));
	}

	/**
	 * Intializes the playlist and its View
	 */
	private void initializePlaylistAndView() {
		createNewPlaylist();
		createNewPlaylistIndex();
		playlistView = (ListView)musicFragmentLayout.findViewById(R.id.playlistView);		
		playlistView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,  playlist));		
		registerForContextMenu(playlistView);
			
		playlistView.setOnItemClickListener(new OnItemClickListener()
		{			
	
		    @Override
		    public void onItemClick(AdapterView<?> parentAdapter, View view, int position, long rowID) {
		    	playlistPickedSong(parentAdapter, view, position, rowID, false);
		    }

		});
		
		playlistView.setOnItemLongClickListener (new OnItemLongClickListener() {
			
		
			@Override
			public boolean onItemLongClick(AdapterView<?> parentAdapter, View view, int position, long rowID) {
				playlistPickedSong(parentAdapter, view, position, rowID, true);
				playlistView.showContextMenu();
				return true;
			}
		});
		
	}
	
	//Save drive play list
	public static void saveDriveList(ArrayList<DrivePlayList> newList){
		drive_List_Of_Playlists = newList;
	}
	
	//Restore drive play list
	public static ArrayList<DrivePlayList> restoreList(){
		return drive_List_Of_Playlists;
	}
	
	//Switch play list from one from drive
	public static void swapPlayList(ArrayList<String> newSongs, ArrayList<Integer> newIndex){
		
		for(int ii = playlist.size() - 1; ii >= 0 ; ii--){
			playlist.remove(ii);
			playlistIndex.remove(ii);
		}
		
		for(int ii = 0; ii < newSongs.size(); ii++){
			playlist.add(newSongs.get(ii));
			playlistIndex.add(newIndex.get(ii));
		}
	}
	
	//Update play list whenever switch back to
	@Override
	public void onResume(){
		super.onResume();
		if(playlist != null){
			((BaseAdapter) playlistView.getAdapter()).notifyDataSetChanged();
		}
	}
	
	//Set last settings clicked from list view
	private void playlistPickedSong(AdapterView<?> parentAdapter, View view, int position, long rowID, boolean isLongClick)
	{
    	if(lastSelectedSongOnPlayList != null) 
    		lastSelectedSongOnPlayList.setBackgroundColor(getResources().getColor(android.R.color.transparent));		    	
    	
    	lastSelectedSongOnPlayList = view;
        view.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
        lastSongTitle = parentAdapter.getItemAtPosition(position).toString();
        lastSongPosition = position;
        if(isSongPlayable() && !isLongClick)
        		musicPlayer.selectSong(getPlaylistIndex().get(lastSongPosition));     	
	}	
	
 
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(contextGooglePlus).setOnMenuItemClickListener(this);
		menu.add(contextAddToPlaylist).setOnMenuItemClickListener(this);
		menu.add(contextRemoveSong).setOnMenuItemClickListener(this);
		menu.add(contextCancel).setOnMenuItemClickListener(this);
	}
	
	
	//Context menu listener
	@Override
	public boolean onMenuItemClick(MenuItem item){
		final String itemName = item.getTitle().toString();
		if(itemName.equals(contextGooglePlus))
		{
			Intent intent = new Intent(getActivity(), ShareActivity.class);
			startActivity(intent);
		}
		else if(itemName.equals(contextCancel))
		{
			Log.i("cTest", "cancel");
		}
		else if(itemName.equals(contextRemoveSong))
		{
			removeFromPlaylist(lastSongTitle);
		}
		return true;
	}

	/**
	 * opens a new activity - OptionActivity
	 * @param view
	 */
	private void openOption(View view) {
		Intent intent = new Intent(getActivity(), GoogleDriveActivity.class);
		startActivity(intent);
	}	

	public ArrayList<String> getPlaylist() {
		return playlist;
	}

	public View getPlayListView() {
		return playlistView;
	}
	
	public ArrayList<Integer> getPlaylistIndex(){
		return playlistIndex;
	}
	
	private void createNewPlaylist() {
		playlist = new ArrayList<String>();
	}
	
	private void createNewPlaylistIndex(){
		playlistIndex = new ArrayList<Integer>();
		lastSongPosition = -1;
	}
	
	public void addToPlaylistIndex(int index){
		getPlaylistIndex().add(index);
	}
	
	// Adds a title and its index to the play list
	public void addToPlaylist(String title, int index) {
		
		boolean songAlreadyExist = (getPlaylist().indexOf(title) != -1);
		//make sure no duplicates in current playlist
		if(!songAlreadyExist) 
		{
			getPlaylist().add(title);		
			getPlaylistIndex().add(index);
			((BaseAdapter) playlistView.getAdapter()).notifyDataSetChanged();
		}
	}
	
	// Removes a title from the play list
	public void removeFromPlaylist(String title)
	{
		int index  = getPlaylist().indexOf(title);
		if(index != -1)
		{
			playlist.remove(index);
			playlistIndex.remove(index);
			lastSongTitle = null;
			lastSelectedSongOnPlayList = null;
			((BaseAdapter) playlistView.getAdapter()).notifyDataSetChanged();
		}
	}

	/**
	 * ImageAdapter for the gallery
	 * @author Jae-Hwan Jung
	 *
	 */
	public class ImageAdapter extends BaseAdapter {

		private Context context;
		
		/** 
		 * All images to be displayed.
		 */
		private int[] galleryImageIds = {
				R.drawable.musiclogo,
				R.drawable.rockmusic,
				R.drawable.hiphopmusic,
				R.drawable.countrymusic
		};

		public ImageAdapter(Context c) { 
			context = c; 
		}

		/** 
		 * Returns the number of images stored  in the gallery.
		 */
		public int getCount() {
			return galleryImageIds.length; 
		}

		/**
		 * Use the array-Positions as unique IDs 
		 */
		public Object getItem(int position) { return galleryImageIds[position]; }
		public long getItemId(int position) { return galleryImageIds[position]; }

		/** 
		 * Returns a new ImageView to be displayed, 
		 * depending on the position passed. 
		 * Could also use an xml file and just inflate it.
		 */
		@SuppressWarnings("deprecation")
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView image = new ImageView(context);

			image.setImageResource(this.galleryImageIds[position]);
			image.setScaleType(ImageView.ScaleType.FIT_XY);
			image.setLayoutParams(new Gallery.LayoutParams(150, 150));
			return image;
		}
	}

	private boolean isSongPlayable(){
        return getPlaylistIndex().size() > 0 && lastSongPosition > -1;
	}
	
	//Sets what to do after each has finished, depending on the settings
	public void setPlayingStatus(int mode){
		
		if(getPlaylistIndex().size() == 0)
			return;
		
		if(shuffleButton.isSelected())
			mode = Constants.PLAY_SHUFFLE;
		
		int androidSongIndex = 0;
		TextView songPlaying = (TextView)getActivity().findViewById(R.id.playStatus);
		Log.i("loopButtonState", Integer.toString(loopButtonState));
		switch(mode){
			case(Constants.PLAY_SELECTED):				
				songPlaying.setText("PLAYING " + getPlaylist().get(lastSongPosition% (getPlaylistIndex().size())));
				loopCounter--;
				Log.i("LOOPCOUNTER-PLAY_SELECTED", Integer.toString(loopCounter));
				break;
			case(Constants.PLAY_NEXT):
				if (!(loopButtonState == LOOP_UNPRESSED && loopCounter<=1)) {
					if(loopButtonState != LOOP_SONG_ONLY)
						androidSongIndex = (++lastSongPosition) % (getPlaylistIndex().size());
					Log.i("PLAY_NEXT", Integer.toString(androidSongIndex));	
	    			musicPlayer.selectSongNoClick(getPlaylistIndex().get(androidSongIndex));
	    			playlistView.setSelection(androidSongIndex);
	    			songPlaying.setText("PLAYING " + getPlaylist().get(androidSongIndex));
	    			loopCounter--;
	    			Log.i("LOOPCOUNTER-PLAY_NEXT", Integer.toString(loopCounter));
				}
				else {					
					char bytesPlay[] = {0x03};
					String stopMSG = new String(bytesPlay);// + song;
					rs232.addToSendBuffer(stopMSG);
					songPlaying.setText("Stopped");
					lastSongPosition = playlistView.getSelectedItemPosition();
				}
				break;
			case(Constants.PLAY_SHUFFLE):
				if (!(loopButtonState == LOOP_UNPRESSED && loopCounter<=1)) {
					Random rand = new Random();
					androidSongIndex = (rand.nextInt(getPlaylistIndex().size()) % getPlaylistIndex().size());
	        		musicPlayer.selectSongNoClick(getPlaylistIndex().get(androidSongIndex));        		
	    			songPlaying.setText("PLAYING " + getPlaylist().get(androidSongIndex));
				} else {					
					char bytesPlay[] = {0x03};
					String stopMSG = new String(bytesPlay);// + song;
					rs232.addToSendBuffer(stopMSG);
					songPlaying.setText("Stopped");
					lastSongPosition = playlistView.getSelectedItemPosition();
				}
				break;			
		}
	}
	
	//Sets the seekbar to the given value
	public void setSeekBar(int inputData) {
		songSeekBar.setProgress(inputData);
	}
	
	//resets the seekbar to the given maximum value
	public void resetSeekBar(char inputData) {
		totalTime = (long)inputData;
		totalTime = totalTime * 1000;
		pauseTime = 0;
		resumeTime = 0;
		currentTime = System.currentTimeMillis();
		songSeekBar.setMax((int) totalTime);
		setSeekBar(0);
	}
	
	//Listener for buttons
	@Override
	public void onClick(View view) {
		boolean result = false;
		int coordinates[] = new int[2];
		switch (view.getId()) {
	        case R.id.playButton:
		         playButton.setSelected(true);
		         playButton.getLocationOnScreen(coordinates);  		         
		         if(isSongPlayable()){
		        	 //error message
	        		 if(!musicPlayer.play()){
	    			         rs232.popUpMsg("Not connected!", coordinates[0], coordinates[1]);
	        		 }
	        		 else{
	        			 //success display song title
	        			 setPlayingStatus(Constants.PLAY_SELECTED);
	        		 }
		         }
		         else{
			         rs232.popUpMsg("Must select a song!", coordinates[0], coordinates[1]);

		         }		         
		         try {
		        	 seekBarTimer.cancel();
		        	 seekBarTimer.purge();
		         } catch (Exception e) {
		        	 //Do nothing
		         }
		         if (pauseTime!=0)
		        	 resumeTime = System.currentTimeMillis();
		         seekBarTimer = new Timer();
		         seekBarTimerTask = new SeekBarTimer();
		         seekBarTimer.schedule(seekBarTimerTask, 0, 500);
		         result = true;
		         if (loopButtonState == LOOP_UNPRESSED) {
		        	 loopCounter = getPlaylistIndex().size();
		        	 Log.i("LOOPCOUNTER-ADD", Integer.toString(loopCounter));
		         }
	        	 break;	  
	        case R.id.pauseButton:
	        	 playButton.setSelected(false);
		         result = musicPlayer.playPause();
		         pauseButton.getLocationOnScreen(coordinates);
		         pauseTime = System.currentTimeMillis();
		         seekBarTimer.cancel();
		         seekBarTimer.purge();
	        	 break;
	        case R.id.stopButton:
	        	 playButton.setSelected(false);
		         result = musicPlayer.playStop();
		         stopButton.getLocationOnScreen(coordinates);
		         pauseTime = System.currentTimeMillis();
		         seekBarTimer.cancel();
		         seekBarTimer.purge();
				 break;
	        case R.id.volumeDownButton:
		         result = musicPlayer.volumeDown();
		         volumeDownButton.getLocationOnScreen(coordinates);  
	        	 break;
	        	 
	        case R.id.volumeUpButton:
		         result = musicPlayer.volumeUp();
		         volumeUpButton.getLocationOnScreen(coordinates);  
				 break;
				 
	        case R.id.playlistButton:
		         result = musicPlayer.refreshPlaylist();
		         playlistButton.getLocationOnScreen(coordinates);  
				 break;
				 
	        case R.id.shuffleButton:
	        	
	        	 if(shuffleButton.isSelected())
	        	 {
	        		 shuffleButton.setSelected(false);
	        	 }
	        	 else
	        	 {
	        		 shuffleButton.setSelected(true);
	        	 }
	        	 result = true;
	        	 break;
	        	
	        case R.id.loopButton:
	        	
	    		 char bytes[] = {0x01, 0x00};
	        	 switch(loopButtonState)
	        	 {
	        	 case(LOOP_UNPRESSED ):
	        		// bytes[1] = 0x0B;
	        	     loopButton.setImageResource(R.drawable.loopbutton_pressed);
	        		 loopButton.setSelected(true);
	        		 loopButtonState = LOOP_SONG_ONLY;	        		 
	        		 break;
	        	 case(LOOP_SONG_ONLY):
	        		// bytes[1] = 0x09;
	        	 	 loopButton.setImageResource(R.drawable.loopbutton_focused);
	        		 loopButton.setSelected(true);
	        		 loopButtonState = LOOP_PLAY_ALL;
	        		 break;
	        	 case(LOOP_PLAY_ALL ):
	        		// bytes[1] = 0x0A;
	        	 	 loopButton.setImageResource(R.drawable.loopbutton_normal);
	        		 loopButton.setSelected(true);
	        		 loopButtonState = LOOP_UNPRESSED ;
	        		 loopCounter = getPlaylistIndex().size();
	        		 break;
	        	 }
				 break;
		}	
		
		if(!result)
		{
			rs232.popUpMsg("Not connected", coordinates[0], coordinates[1]);
		}
	}
	
	
	public MusicPlayer getMusicPlayer()
	{
		return musicPlayer;
	}

	// Custom timertask for the seekbar updates
	class SeekBarTimer extends TimerTask {

		@Override
		public void run() {
			long newTime = System.currentTimeMillis();
			long progressTime = newTime - currentTime - (resumeTime - pauseTime);
			setSeekBar((int)progressTime);
		}
		
	}

}

