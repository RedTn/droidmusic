package com.ubc.music;

import java.io.IOException;
import java.net.Socket;

import com.ubc.music.R;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

public class ConnectFragment extends Fragment {

	final int INPUT_TIMER_INTERVAL = 500;
	final int TIMER_START_DELAY = 3000;

	final int IP1_INITIAL_VALUE = 192;
	final int IP2_INITIAL_VALUE = 168;
	final int IP3_INITIAL_VALUE = 0;
	final int IP4_INITIAL_VALUE = 114;

	/**
	 * The layout of the fragment.
	 * Use this to invoke findViewById().
	 */
	private LinearLayout connectFragmentLayout;

	private RS232 rs232;
	private int connectX = Gravity.CENTER;
	private int connectY;
	private int disconnectX = Gravity.CENTER;
	private int disconnectY;

	private Button disconnectButton;
	private Button connectButton;
	/**
	 * Sets StrictMode for better thread error messages
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.detectDiskReads().detectDiskWrites().detectNetwork()
		.penaltyLog().build());
		super.onCreate(savedInstanceState);		
		rs232 = ((MainActivity)getActivity()).getRS232();
	}

	/**
	 * Stores the layout of the fragment so that it can be
	 * used by other methods within the fragment.
	 * Note:calling getActivity() to get Views from the 
	 * fragment layout does not work.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		connectFragmentLayout = (LinearLayout)inflater.inflate(R.layout.fragment_connect, container, false);	
		//setButtons();
		return connectFragmentLayout;
	}

	/**
	 * Anything related to the parent Activity needs
	 * to be done here. Otherwise methods such as
	 * getActivity() may return null.
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setButtons();
		NumberPicker ip = (NumberPicker)connectFragmentLayout.findViewById(R.id.ip1);
		ip.setMaxValue(999);
		ip.setMinValue(0);
		ip.setValue(IP1_INITIAL_VALUE);

		ip = (NumberPicker)connectFragmentLayout.findViewById(R.id.ip2);
		ip.setMaxValue(999);
		ip.setMinValue(0);
		ip.setValue(IP2_INITIAL_VALUE);

		ip = (NumberPicker)connectFragmentLayout.findViewById(R.id.ip3);
		ip.setMaxValue(999);
		ip.setMinValue(0);
		ip.setValue(IP3_INITIAL_VALUE);

		ip = (NumberPicker)connectFragmentLayout.findViewById(R.id.ip4);
		ip.setMaxValue(999);
		ip.setMinValue(0);
		ip.setValue(IP4_INITIAL_VALUE);
	}

	private void setButtons()
	{
		connectButton = (Button)connectFragmentLayout.findViewById(R.id.connectButton);

		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int coordinates[] = new int[2];
				connectButton.getLocationOnScreen(coordinates);
				connectY = coordinates[1];
				connectX = Gravity.CENTER;

				openSocket(v);

			}
		});

		disconnectButton = (Button)connectFragmentLayout.findViewById(R.id.disconnectButton);
		disconnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				int coordinates[] = new int[2];
				disconnectButton.getLocationOnScreen(coordinates);
				disconnectY = coordinates[1];
				disconnectX = Gravity.CENTER;
				closeSocket(v);
			}
		});


	}



	/**
	 * Called when the user presses "connect"
	 * @param view View of the caller
	 * @throws InterruptedException 
	 */
	private void openSocket(View view){
		/**
		 *  SocketConnect is a AsyncTask in which
		 *  a socket is created in the background on a 
		 *  separate thread and assign it to the global 
		 *  socket variable on the main UI thread.
		 */
		if(rs232.isConnected())
		{
			rs232.popUpMsg("Already Connected!", connectX, connectY);
		}
		else
		{
			new SocketConnect().execute((Void) null);
			rs232.setHasAttemptedConncetion(true);

		}
	}


	/**
	 * Called when the user closes a socket
	 * @param view View of the caller
	 */
	public void closeSocket(View view) {
		MyApplication app = (MyApplication) getActivity().getApplication();
		Socket s = app.sock;

		if(!rs232.isConnected())
		{
			rs232.popUpMsg("Cannot disconnect. No connection was made", disconnectX, disconnectY);
		}
		else
		{
			try {
				s.getOutputStream().close();
				s.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			rs232.popUpMsg("Connection successfully disconnected", disconnectX, disconnectY);

		}
	}

	/**
	 * Construct a IP address from the four number pickers
	 * @return String representation of IP adress
	 */
	private String getConnectToIP() {
		StringBuilder ipAddress = new StringBuilder();
		NumberPicker ip;
		ip = (NumberPicker) connectFragmentLayout.findViewById(R.id.ip1);
		ipAddress.append(ip.getValue());
		ipAddress.append(".");
		ip = (NumberPicker) connectFragmentLayout.findViewById(R.id.ip2);
		ipAddress.append(ip.getValue());
		ipAddress.append(".");
		ip = (NumberPicker) connectFragmentLayout.findViewById(R.id.ip3);
		ipAddress.append(ip.getValue());
		ipAddress.append(".");
		ip = (NumberPicker) connectFragmentLayout.findViewById(R.id.ip4);
		ipAddress.append(ip.getValue());
		return ipAddress.toString();
	}

	/**
	 * Construct a port from the port TextEdit
	 * @return
	 */
	private Integer getConnectToPort() {
		EditText port = (EditText) connectFragmentLayout.findViewById(R.id.port);
		return Integer.parseInt(port.getText().toString());
	}

	/**
	 * This class creates a socket on a separate
	 * Asynchronous thread and assigns it to the
	 * global socket variable on the main UI thread.
	 */
	private class SocketConnect extends AsyncTask<Void, Void, Socket> {

		@Override
		protected Socket doInBackground(Void... voids) {

			Socket s = null;

			try {

				s = new Socket(getConnectToIP(), getConnectToPort());	

			} catch (Exception e) {
				e.printStackTrace();
			}	

			return s;
		}

		@Override
		protected void onPostExecute(Socket s) {
			MyApplication myApp = (MyApplication) getActivity().getApplication();
			myApp.sock = s;
		}
	}


}
