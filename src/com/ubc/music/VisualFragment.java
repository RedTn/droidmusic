package com.ubc.music;

import com.ubc.music.R;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class VisualFragment extends Fragment implements OnClickListener {
	
	RelativeLayout visualFragment;
	ObjectAnimator cloud1Animation;
	ObjectAnimator cloud2Animation;
	ValueAnimator skyAnimation;
	AnimationDrawable sunAnimation;
	AnimationDrawable trollAnimation;
	
	long bpmT1;
	long bpmT2;
	long bpmTotalTime;
	Boolean bpmReset = false;
	int sampleNumber = 100;
	int bpmCounter = 0;
		
	private RS232 rs232;
	
	/**
	* Loads the layout and creates animation instances
	*/
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		visualFragment = (RelativeLayout) inflater.inflate(R.layout.fragment_visual, container, false);
		visualFragment.findViewById(R.id.button1).setOnClickListener(this);
		
		createSunAnimation();
		createSkyAnimation();
		createCloud1Animation();
		createCloud2Animation();
		createTrollAnimation();
		
		rs232 = ((MainActivity)getActivity()).getRS232();
		
		return visualFragment;
	}
	
	/**
	* Animation for the first cloud
	*/
	private void createCloud1Animation() {
		float start = -visualFragment.findViewById(R.id.cloud1).getPaddingLeft()*2;
		float end = 500;
		cloud1Animation = ObjectAnimator.ofFloat(visualFragment.findViewById(R.id.cloud1), "x", start, end);
		cloud1Animation.setDuration(30000);
		cloud1Animation.setRepeatCount(ValueAnimator.INFINITE);
		cloud1Animation.setRepeatMode(ValueAnimator.RESTART);
	}
	
	/**
	* Animation for the second cloud
	*/
	private void createCloud2Animation() {
		float start = -visualFragment.findViewById(R.id.cloud1).getPaddingLeft()*2;
		float end = 500;
		cloud2Animation = ObjectAnimator.ofFloat(visualFragment.findViewById(R.id.cloud2), "x", start, end);
		cloud2Animation.setDuration(20000);
		cloud2Animation.setRepeatCount(ValueAnimator.INFINITE);
		cloud2Animation.setRepeatMode(ValueAnimator.RESTART);
	}
	
	/**
	* Animatino for the sky 
	*/
	private void createSkyAnimation() {
		skyAnimation = ObjectAnimator.ofInt(visualFragment.findViewById(R.id.Visual_Fragment_Layout),
				"backgroundColor", Color.rgb(0x66, 0xcc, 0xff), Color.rgb(0x00, 0x66, 0x99));
		skyAnimation.setDuration(300);
		skyAnimation.setRepeatCount(0);
		skyAnimation.setRepeatMode(ValueAnimator.REVERSE);
		skyAnimation.setEvaluator(new ArgbEvaluator());
	}
	
	/**
	* Animation for the sun 
	*/
	private void createSunAnimation() {
		sunAnimation = (AnimationDrawable) visualFragment.findViewById(R.id.sun).getBackground();
		
	}

	/**
	* Animation for the troll
	*/
	private void createTrollAnimation() {
		trollAnimation = (AnimationDrawable) visualFragment.findViewById(R.id.troll).getBackground();
		
	}

	/**
	* Starts the animation for the sun
	*/
	public void hitSun() {
		sunAnimation.setVisible(false, true);
		sunAnimation.start();
	}

	/**
	* Starts the animation for the sky
	*/
	public void hitSky() {
		skyAnimation.start();
	}
	
	/**
	* Depending on the value given, animate different views
	*/
	public void visualizeAmplitude(String inputData) {
		
		if (inputData.equals("low"))
			hitSun();
		else if (inputData.equals("med"))
			hitSky();
		else if (inputData.equals("high")) 
			hitTroll();
		
	}
	
	/**
	* Stops all running animations
	*/
	public void stopAllAnimations() {
		cloud1Animation.end();
		cloud2Animation.end();
		trollAnimation.stop();
	}
	
	/**
	* Depending on the BPM, change the speed of the troll animation
	*/
	private void changeTrollAnimationSpeed(int period) {
		
		int frameCnt = trollAnimation.getNumberOfFrames();
		for (int i=0; i<frameCnt; i++) {
			Drawable obj = trollAnimation.getFrame(i);
			trollAnimation.scheduleDrawable(obj, trollAnimation, period);
		}
	}
	
	/**
	* Update the current value of BPM
	*/
	public void updateBPM(int number) {
		if (bpmReset == true) {
			sampleNumber = number;
			bpmCounter = 0;
			bpmT1 = System.currentTimeMillis();
		}		
		bpmReset = false;
		bpmCounter++;
		Log.i("BPM ", Integer.toString(bpmCounter));
		if (bpmCounter == sampleNumber) {
			bpmT2 = System.currentTimeMillis();
			bpmTotalTime = bpmT2 - bpmT1;
			int period = (int)(bpmTotalTime/bpmCounter);
			Log.i("BPM CAP", Integer.toString(period));
			changeTrollAnimationSpeed(period*2);
			bpmCounter = 0;
			bpmT1 = System.currentTimeMillis();
		}		
	}
	
	/**
	* Resets the BPM
	*/
	public void resetBPM() {
		bpmT1 = System.nanoTime();
		bpmT2 = 0;
		bpmReset = true;
	}
	
	/**
	* Animate the troll
	*/
	public void hitTroll() {
		visualFragment.findViewById(R.id.troll).setVisibility(ImageView.INVISIBLE);
		visualFragment.findViewById(R.id.troll2).setVisibility(ImageView.VISIBLE);
		
		new AsyncTask<Void, Void, Void>(){

			@Override
			protected Void doInBackground(Void... params) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				visualFragment.findViewById(R.id.troll2).setVisibility(ImageView.INVISIBLE);
				visualFragment.findViewById(R.id.troll).setVisibility(ImageView.VISIBLE);		
			}
			
		}.execute((Void)null);
	}
	
	@Override
	public void onClick(View view) {
		char bytes[] = {0x01,0x14};
		final String nextMsg = new String(bytes);
		rs232.addToSendBuffer(nextMsg);	
		if (trollAnimation.isRunning()) {
			cloud1Animation.end();
			cloud2Animation.end();
			trollAnimation.stop();
		}
		else {
			cloud1Animation.start();
			cloud2Animation.start();
			trollAnimation.start();
		}
	}


	
	
	
}
