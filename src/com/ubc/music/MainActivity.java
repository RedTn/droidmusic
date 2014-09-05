package com.ubc.music;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import com.ubc.music.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends FragmentActivity {

	/**
	 * The number of pages to be shown in the swiping/sliding mechanism.
	 * I think it's called 'Fling'
	 */
	private static final int NUM_PAGES = 4;

	/**
	 * The pager widget, which handles animation and allows swiping 
	 * horizontally to access previous and next pages.
	 */
	private ViewPager viewPager;

	/**
	 * The pager adapter, which provides the pages to the View Pager.
	 */
	private PagerAdapter viewPagerAdapter;
	
	private RS232 rs232;
	
	private Timer tcp_timer;
	
	private MusicFragment musicFragment;
	private ConnectFragment connectFragment;
	private VisualFragment visualFragment;
	private JockeyFragment jockeyFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		rs232 = new RS232((MyApplication)getApplication(), this);
		tcp_timer = new Timer();
		tcp_timer.schedule( rs232, 2000, 200);
		
		// Instantiate a ViewPager and a PagerAdapter.
		viewPager = (ViewPager) findViewById(R.id.fragment_container);
		viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
		viewPager.setAdapter(viewPagerAdapter);

		/**
		 * Sets the number of offscreen pages
		 * Anything over this number will be destroyed and
		 * if the destroyed page had some thread/time going on
		 * error may be caused
		 */
		viewPager.setOffscreenPageLimit(NUM_PAGES);

		/**
		 * Gets rid of the icon and the name of the app on the top left corner
		 */
		getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().setDisplayShowTitleEnabled(false);
	}

	/**
	 * This the menu at the top of the app
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		rs232.setMenu(menu);
		return true;
	}

	//When main activity is activated check if a special action was given
	@Override
	protected void onResume(){
		super.onResume();
		
		final String action = getIntent().getAction();
	    if (getString(R.string.id_open_app_from_gplus).equals(action)) {
	    	goToMusicFragment();
	      }
	}
	
	/**
	 * Called when the menu at the top of the app gets selected
	 * The ids can be found in main.xml which is the layout for menue
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.action_Connect)
			goToConnectFragment();
		else if (item.getItemId() == R.id.action_Music)
			goToMusicFragment();
		else if (item.getItemId() == R.id.action_Visual)
			goToVisualFragment();
		else if (item.getItemId() == R.id.action_Jockey)
			goToJockeyFragment();
		return true;
	}

	/**
	 * Customize the back function that it slides
	 * the pages instead of activities (if there are
	 * pages to be slided. If no pages are left, then
	 * activities are slided).
	 */
	@Override
	public void onBackPressed() {
		if (viewPager.getCurrentItem() == 0) {
			super.onBackPressed();
		} else {
			viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
		}
	}

	/**
	 * Set the view pager to VisualFragment
	 */
	private void goToJockeyFragment() {
		viewPager.setCurrentItem(3, true);
	}
	
	/**
	 * Set the view pager to VisualFragment
	 */
	private void goToVisualFragment() {
		viewPager.setCurrentItem(2, true);
	}

	/**
	 * Set the view pager to MusicFragment
	 */
	private void goToMusicFragment() {
		viewPager.setCurrentItem(1, true);
	}

	/**
	 * Set the view pager to ConnectFragment
	 */
	private void goToConnectFragment() {
		viewPager.setCurrentItem(0, true);
	}
	
	/**
	 * adapter to hold pages for swapping/sliding feature
	 */
	private class ViewPagerAdapter extends FragmentStatePagerAdapter {

		/**
		 * Holds all the fragments to be displayed in a Fling
		 */
		private List<Fragment> fragments;

		public ViewPagerAdapter(FragmentManager fm) {
			super(fm);
			fragments = new ArrayList<Fragment>();
			
			Bundle args = new Bundle();
			connectFragment = new ConnectFragment();
			connectFragment.setArguments(args);
			fragments.add(connectFragment);

			musicFragment = new MusicFragment();
			musicFragment.setArguments(args);
			fragments.add(musicFragment);

			visualFragment = new VisualFragment();
			visualFragment.setArguments(args);
			fragments.add(visualFragment);
			
			jockeyFragment = new JockeyFragment();
			jockeyFragment.setArguments(args);
			fragments.add(jockeyFragment);
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);

		}

		@Override
		public int getCount() {
			return NUM_PAGES;
		}
	}
	
	public MusicFragment getMusicFragment()
	{
		return musicFragment;
	}
	
	public VisualFragment getVisualFragment()
	{
		return visualFragment;
	}
	
	public RS232 getRS232()
	{
		return this.rs232;
	}
	

}
