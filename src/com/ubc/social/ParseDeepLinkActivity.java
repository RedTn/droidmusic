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

package com.ubc.social;

import com.google.android.gms.plus.PlusShare;
import com.ubc.drive.GoogleDriveActivity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Example of parsing a deep link from a Google+ post, and launching the corresponding activity.
 */
public class ParseDeepLinkActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String deepLinkId = PlusShare.getDeepLinkId(this.getIntent());
        Intent target = processDeepLinkId(deepLinkId);
        if (target != null) {
            startActivity(target);
        }

        finish();
    }

    /**
     * Get the intent for an activity corresponding to the deep link ID.
     *
     * @param deepLinkId The deep link ID to parse.
     * @return The intent corresponding to the deep link ID.
     */
    private Intent processDeepLinkId(String deepLinkId) {
		Intent intent = new Intent(this, GoogleDriveActivity.class);
		intent.putExtra("googleURL", deepLinkId);
		intent.setAction("googlePlus");
    	Log.i("Google", deepLinkId);


        return intent;
    }
}
