package com.ubc.music;

import com.ubc.music.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class JockeyFragment extends Fragment implements OnClickListener {

	private LinearLayout jockeyFragmentLayout;

	private ImageButton clapButton;
	private ImageButton bassButton;

	private RS232 rs232;
	private MusicPlayer musicPlayer;
	
	/**
	* Loads RS232 and MusicPlayer instances.
	*/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		rs232 = ((MainActivity)getActivity()).getRS232();
		musicPlayer = new MusicPlayer(rs232);
	}
	
	/**
	* Loads the buttons and attaches listeners
	*/
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		jockeyFragmentLayout = (LinearLayout) inflater.inflate(R.layout.fragment_jockey, container, false);
		clapButton = (ImageButton) jockeyFragmentLayout.findViewById(R.id.clapButton);
		bassButton = (ImageButton) jockeyFragmentLayout.findViewById(R.id.bassButton);
		clapButton.setOnClickListener(this);
		bassButton.setOnClickListener(this);
		return jockeyFragmentLayout;
	}

	/**
	* Listener for buttons
	*/
	@Override
	public void onClick(View view) {
		boolean result = false;
		int coordinates[] = new int[2];
		
		switch (view.getId()) {
		case R.id.clapButton:
			result = musicPlayer.playClap();
			clapButton.getLocationOnScreen(coordinates);  
			break;
		case R.id.bassButton:
			result = musicPlayer.playBass();
			bassButton.getLocationOnScreen(coordinates);  
			break;
		}	

		if(!result)
		{
			rs232.popUpMsg("Not connected", coordinates[0], coordinates[1]);
		}
	}


}
