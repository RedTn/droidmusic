package com.ubc.social;
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.plus.GooglePlusUtil;
import com.google.android.gms.plus.PlusClient;
import com.google.android.gms.plus.PlusShare;
import com.google.android.gms.plus.model.people.Person;
import com.ubc.social.PlusClientFragment.OnSignedInListener;

/**
 * Example of sharing with Google+ through the ACTION_SEND intent.
 */
public class ShareActivity extends FragmentActivity implements 
        OnSignedInListener, PlusClient.OnPersonLoadedListener  {

    protected static final String TAG = ShareActivity.class.getSimpleName();

    private static final String STATE_SHARING = "resolving_error";

    private static final String TAG_ERROR_DIALOG_FRAGMENT = "errorDialog";

    private static final int REQUEST_CODE_PLUS_CLIENT_FRAGMENT = 1;
    private static final int REQUEST_CODE_RESOLVE_GOOGLE_PLUS_ERROR = 2;
    private static final int REQUEST_CODE_INTERACTIVE_POST = 3;

    private PlusClientFragment mPlusClientFragment;
    private boolean mSharing;
    private String userName;
    private String URLshare;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  
        mPlusClientFragment = PlusClientFragment.getPlusClientFragment(this, MomentUtil.VISIBLE_ACTIVITIES);
        mSharing = savedInstanceState != null && savedInstanceState.getBoolean(STATE_SHARING, false);
        
        //Expect to only call this activity when user wants share on google+
        Intent intent = getIntent();
        URLshare = intent.getExtras().getString("url");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_SHARING, mSharing);

    }

    //Whenever this activity is switched to immediately sign into google+
    public void onResume() {

    		super.onResume();
           // Set sharing so that the share is started in onSignedIn.
          mSharing = true;
          mPlusClientFragment.signIn(REQUEST_CODE_PLUS_CLIENT_FRAGMENT);
    }
    

    @Override
    public void onSignedIn(PlusClient plusClient) {

        Log.i("cTest","sign in");

     	plusClient.loadPerson(this, "me");
     	userName = plusClient.getCurrentPerson().getDisplayName(); 
        if (!mSharing) {
            // The share button hasn't been clicked yet.
            return;
        }
        // Reset sharing so future calls to onSignedIn don't start a share.
        mSharing = false;
        final int errorCode = GooglePlusUtil.checkGooglePlusApp(this);
        if (errorCode == GooglePlusUtil.SUCCESS) {
            startActivityForResult(getInteractivePostIntent(plusClient),
                    REQUEST_CODE_INTERACTIVE_POST);
        } else {
            // Prompt the user to install the Google+ app.
            GooglePlusErrorDialogFragment
                    .create(errorCode, REQUEST_CODE_RESOLVE_GOOGLE_PLUS_ERROR)
                    .show(getSupportFragmentManager(), TAG_ERROR_DIALOG_FRAGMENT);
        }
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (mPlusClientFragment.handleOnActivityResult(requestCode, resultCode, intent)) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_INTERACTIVE_POST:
                if (resultCode != RESULT_OK) {
                    Log.e(TAG, "Failed to create interactive post");
                }
                break;
            case REQUEST_CODE_RESOLVE_GOOGLE_PLUS_ERROR:
                if (resultCode != RESULT_OK) {
                    Log.e(TAG, "Unable to recover from missing Google+ app.");
                } else {
                    mPlusClientFragment.signIn(REQUEST_CODE_PLUS_CLIENT_FRAGMENT);
                }
                break;
        }
    }

    private Intent getInteractivePostIntent(PlusClient plusClient) {

    	//Post details
        String appImageURL = "http://i.imgur.com/fDpJ3pB.png?1";
        String appDescription = " shared a new Play List with you, check it out now!";
        String appTitle = "Social StereoType Android App";
        String dURL = "112757372714630658783/posts";
        String CALL_BUTTON_LABEL = "LISTEN";
        
        if(userName == null) userName = "Unknown";
        
        
        Uri callToActionUrl = Uri.parse(dURL);
        String callToActionDeepLinkId = URLshare;
        
        // Create an interactive post builder.
        PlusShare.Builder builder = new PlusShare.Builder(this, plusClient);

        // Set call-to-action metadata.
        builder.addCallToAction(CALL_BUTTON_LABEL, callToActionUrl, callToActionDeepLinkId);
        builder.setType("text/plain");
        

        // Set the target deep-link ID (for mobile use).
        builder.setContentDeepLinkId(URLshare, appTitle, userName + appDescription, Uri.parse(appImageURL));
        return builder.getIntent();
    }
    
    //Get user profile
    @Override
    public void onPersonLoaded(ConnectionResult status, Person person) {

        if (status.getErrorCode() == ConnectionResult.SUCCESS) {
            userName = person.getDisplayName();    
        	Log.i("cTest", userName);
        }

    }
    

}
