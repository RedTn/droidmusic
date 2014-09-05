package com.ubc.drive;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.ubc.music.MusicFragment;
import com.ubc.music.R;
import com.ubc.social.ShareActivity;

public class GoogleDriveActivity extends Activity implements OnMenuItemClickListener{

	final static String PARENT_FOLDER_TITLE = "Social_Stereotype_Library";
	final static String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
	final static String PLAYLIST_KEY = "@com.social_stereotype";

	final static int REQUEST_ACCOUNT_PICKER = 1;
	final static int REQUEST_AUTHORIZATION = 2;
	final static int CAPTURE_IMAGE = 3;
	final static int GOOGLE_PLUS = 41;


	private static Uri fileUri;
	private static Drive service;
	private GoogleAccountCredential credential;
	private static final String ACTION_DRIVE_OPEN = "com.google.android.apps.drive.DRIVE_OPEN";
	private static final String contextGooglePlus = "Share on Google+";
	private static final String EXTRA_FILE_ID = "resourceId";
	private String mFileId;

	private int lastPlaylistPosition;
	private String lastPlaylistTitle;
	private View lastPlaylistView;

	private ListView DrivePlaylistView;
	private ArrayList<String> drivePlaylist_Titles;
	private ArrayList<DrivePlayList> drive_List_Of_Playlists;
	private String googleDownloadURL;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.google_drive_activity);
		
	}

	@Override
	public onResume(){
		super.onResume();
		
		final String action = getIntent().getAction();
		
		//If the intent was for google+ then get the playlist download URL from the link and download it
		if (action != null && action.equals("googlePlus")) {

			googleDownloadURL = getIntent().getExtras().getString("googleURL");
			initializeDrivePlayList(); 
			credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
			startActivityForResult(credential.newChooseAccountIntent(), GOOGLE_PLUS);			
		}
		//Otherwise restore the previous play list
		else{
			drive_List_Of_Playlists = MusicFragment.restoreList();
			if(drive_List_Of_Playlists != null){
				initializeDrivePlayList();
				drive_List_Of_Playlists = MusicFragment.restoreList();
				updateDrivePlayLists();
			}
		}
	}
	//Pull playlists from drive
	public void syncButton(View v){	
		initializeDrivePlayList(); 
		credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
		startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
	}


	//Save play lists into music fragment
	public void saveList(){
		MusicFragment.saveDriveList(drive_List_Of_Playlists);
	}
	
	
	//Save play list when current activity to switch away from
	@Override
	public void onPause(){
		if(drive_List_Of_Playlists != null)
			saveList();
		super.onPause();
	}

	//Create the list view for play list
	private void initializeDrivePlayList() {
		DrivePlaylistView = (ListView)this.findViewById(R.id.DrivePlaylistView);
		drivePlaylist_Titles = new ArrayList<String>();
		drive_List_Of_Playlists = new ArrayList<DrivePlayList>();

		DrivePlaylistView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,  drivePlaylist_Titles));		
		registerForContextMenu(DrivePlaylistView);

		DrivePlaylistView.setOnItemClickListener(new OnItemClickListener()
		{			

			@Override
			public void onItemClick(AdapterView<?> parentAdapter, View view, int position, long rowID) {
				playlistPickedSong(parentAdapter, view, position, rowID);
			}

		});

		DrivePlaylistView.setOnItemLongClickListener (new OnItemLongClickListener() {


			@Override
			public boolean onItemLongClick(AdapterView<?> parentAdapter, View view, int position, long rowID) {
				playlistPickedSong(parentAdapter, view, position, rowID);
				DrivePlaylistView.showContextMenu();
				return true;
			}
		});


	}

	//Store new play list titles into list view and update it
	private void updateDrivePlayLists(){
		for(DrivePlayList dpl: drive_List_Of_Playlists){
			drivePlaylist_Titles.add(dpl.getPlaylistTitle());
		}
		((BaseAdapter) DrivePlaylistView.getAdapter()).notifyDataSetChanged();
	}


	//Store the last clicked list view settings
	private void playlistPickedSong(AdapterView<?> parentAdapter, View view, int position, long rowID)
	{
		if(lastPlaylistView != null) 
			lastPlaylistView.setBackgroundColor(getResources().getColor(android.R.color.transparent));		    	

		lastPlaylistView = view;
		view.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
		lastPlaylistTitle = parentAdapter.getItemAtPosition(position).toString();
		lastPlaylistPosition = position;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add("Change to PlayList?").setOnMenuItemClickListener(this);
		menu.add("Share on Google Plus").setOnMenuItemClickListener(this);
		menu.add("Cancel");
	}

	@Override
	public boolean onMenuItemClick(MenuItem item){
		final String itemName = item.getTitle().toString();

		if(itemName.equals("Change to PlayList?"))
		{
			MusicFragment.swapPlayList(drive_List_Of_Playlists.get(lastPlaylistPosition).getPlaylist()
					, drive_List_Of_Playlists.get(lastPlaylistPosition).getPlaylistIndex());
		}
		else if(itemName.equals("Share on Google Plus")){
			Intent intent = new Intent(this, ShareActivity.class);
			intent.putExtra("url", drive_List_Of_Playlists.get(lastPlaylistPosition).getPlaylistURL());
			startActivity(intent);
		}

		return true;
	}

	//After sign in, either connect and download from google+ or pull all play lists from drive
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
				String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					service = getDriveService(credential);
					retrievePlayLists();
				}
			}
			break;
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				//saveFileToDrive();
			} else {
				//startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
			}
			break;
		case GOOGLE_PLUS:
			if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
				String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					service = getDriveService(credential);
					
					
					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							downloadFileFromDrive(googleDownloadURL, "Google+ Friend");
							Log.i("Drive", "Finished downloading");
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									updateDrivePlayLists();	

								}
							});
						}
					});t.start();				
				}
			}
			break;
	
		}
	}

	//Upload file to drive
	private void saveFileToDrive(final String parentId) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					java.io.File tempFile = java.io.File.createTempFile("supertime", "txt");

					String tempPath = tempFile.getAbsolutePath();

					BufferedWriter bw = new BufferedWriter(new FileWriter(tempPath));
					bw.write("sup");
					bw.close();

					FileContent mediaContent = new FileContent("text/plain", tempFile);
					FileInputStream fis = new FileInputStream(tempPath);

					// File's metadata.
					File body = new File();
					body.setTitle("Kapleck");
					body.setMimeType("text/plain");
					File file = service.files().insert(body, mediaContent).execute();
					if (file != null) {
						showToast("Playlist Uploaded: " + file.getTitle());
					}
				} catch (UserRecoverableAuthIOException e) {
					startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
				} catch (IOException e) {
					Log.i("Drive", "io excpetion");
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	private Drive getDriveService(GoogleAccountCredential credential) {
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
		.build();
	}

	public void showToast(final String toast) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
			}
		});
	}

	//Query Google drive for the files, store them, and show on play list view
	private void retrievePlayLists() {

		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {

				List<File> result = new ArrayList<File>();
				Files.List request = null;

				try {
					request = service.files().list();

					do {
						try {
							final String query = "mimeType = '" + FOLDER_MIME_TYPE + "' and " + "title = '" + PARENT_FOLDER_TITLE + "' and trashed=false";
							request.setQ(query);
							FileList files = request.execute();
							result.addAll(files.getItems());
							request.setPageToken(files.getNextPageToken());
						} catch (IOException e) {
							System.out.println("An error occurred: " + e);
							request.setPageToken(null);
						}
					} while (request.getPageToken() != null &&
							request.getPageToken().length() > 0);
					
					//Folder does not exist
					if(result.size() == 0){
						createParentFolder();
						showToast("App folder did not exist.\n Creating Social Stereotype Library Folder");
					}
					else{
						final String folderID = result.get(0).getId();
						do {
							final String query = "'" + folderID + "' in parents";
							result = new ArrayList<File>();
							request.setQ(query);
							FileList files = request.execute();
							result.addAll(files.getItems());

						} while (request.getPageToken() != null &&
								request.getPageToken().length() > 0);
						AddDriveToPlayList(result);
					}
				} catch (IOException e1) {
					e1.printStackTrace();
				}

			}
		});
		t.start();
	}



	//Create folder in drive folder
	private File createParentFolder() {

		File body = new File();
		body.setTitle(PARENT_FOLDER_TITLE);
		body.setMimeType(FOLDER_MIME_TYPE);

		try {
			File file = service.files().insert(body).execute();

			return file;
		} catch (IOException e) {
			Log.e("Drive", "An error occured: " + e);
			return null;
		}
	}


	//Ran in no main ui thread
	//Get content from drive from download URL
	private void downloadFileFromDrive(final String downloadUrl, final String title) {
		
		try{
			if (downloadUrl!= null && downloadUrl.length() > 0) {
				HttpResponse resp = service.getRequestFactory().buildGetRequest(new GenericUrl(downloadUrl)).execute();
				InputStream stream = resp.getContent();
				drive_List_Of_Playlists.add(new DrivePlayList(stream, downloadUrl, title));
			}
		} catch (UserRecoverableAuthIOException e) {
			startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//Download each play list and add to list view
	private void AddDriveToPlayList(final List<File> result){
		for(int ii = 0; ii < result.size(); ii++){
			String title = result.get(ii).getTitle();
			String downloadURL = result.get(ii).getDownloadUrl();
			downloadFileFromDrive(downloadURL, title);
		}

		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateDrivePlayLists();	

			}
		});
	}

}