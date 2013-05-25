/*
 * CloudSet - Android devices settings synchronization
 * Copyright (C) 2013 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.cloudset.android;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClient;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * Common utilities for working with Cloud Endpoints.
 * 
 * If you'd like to test using a locally-running version of your App Engine
 * backend (i.e. running on the Development App Server), you need to set
 * LOCAL_ANDROID_RUN to 'true'.
 * 
 * See the documentation at
 * http://developers.google.com/eclipse/docs/cloud_endpoints for more
 * information.
 */
public class CloudEndpointUtils {

  /*
   * TODO: Need to change this to 'true' if you're running your backend locally using
   * the DevAppServer. See
   * http://developers.google.com/eclipse/docs/cloud_endpoints for more
   * information.
   */
  protected static final boolean LOCAL_ANDROID_RUN = false;

  /*
   * The root URL of where your DevAppServer is running (if you're running the
   * DevAppServer locally).
   */
  protected static final String LOCAL_APP_ENGINE_SERVER_URL = "http://localhost:8888/";

  /*
   * The root URL of where your DevAppServer is running when it's being
   * accessed via the Android emulator (if you're running the DevAppServer
   * locally). In this case, you're running behind Android's virtual router.
   * See
   * http://developer.android.com/tools/devices/emulator.html#networkaddresses
   * for more information.
   */
  protected static final String LOCAL_APP_ENGINE_SERVER_URL_FOR_ANDROID = "http://10.0.2.2:8888";

  /**
   * Updates the Google client builder to connect the appropriate server based
   * on whether LOCAL_ANDROID_RUN is true or false.
   * 
   * @param builder
   *            Google client builder
   * @return same Google client builder
   */
  public static <B extends AbstractGoogleClient.Builder> B updateBuilder(
      B builder) {
    if (LOCAL_ANDROID_RUN) {
      builder.setRootUrl(LOCAL_APP_ENGINE_SERVER_URL_FOR_ANDROID
          + "/_ah/api/");
    }

    // only enable GZip when connecting to remote server
    final boolean enableGZip = builder.getRootUrl().startsWith("https:");

    builder.setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
      public void initialize(AbstractGoogleClientRequest<?> request)
          throws IOException {
        if (!enableGZip) {
          request.setDisableGZipContent(true);
        }
      }
    });

    return builder;
  }

  /**
   * Logs the given message and shows an error alert dialog with it.
   * 
   * @param activity
   *            activity
   * @param tag
   *            log tag to use
   * @param message
   *            message to log and show or {@code null} for none
   */
  public static void logAndShow(Activity activity, String tag, String message) {
    Log.e(tag, message);
    showError(activity, message);
  }

  /**
   * Logs the given throwable and shows an error alert dialog with its
   * message.
   * 
   * @param activity
   *            activity
   * @param tag
   *            log tag to use
   * @param t
   *            throwable to log and show
   */
  public static void logAndShow(Activity activity, String tag, Throwable t) {
    Log.e(tag, "Error", t);
    String message = t.getMessage();
    // Exceptions that occur in your Cloud Endpoint implementation classes
    // are wrapped as GoogleJsonResponseExceptions
    if (t instanceof GoogleJsonResponseException) {
      GoogleJsonError details = ((GoogleJsonResponseException) t)
          .getDetails();
      if (details != null) {
        message = details.getMessage();
      }
    }
    showError(activity, message);
  }

  /**
   * Shows an error alert dialog with the given message.
   * 
   * @param activity
   *            activity
   * @param message
   *            message to show or {@code null} for none
   */
  public static void showError(final Activity activity, String message) {
    final String errorMessage = message == null ? "Error" : "[Error ] "
        + message;
    activity.runOnUiThread(new Runnable() {
      public void run() {
        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG)
            .show();
      }
    });
  }
}
