/*
Copyright (C) 2016-2017, Silent Circle, LLC.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Any redistribution, use, or modification is done solely for personal
      benefit and not for any commercial purpose or for monetary gain
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name Silent Circle nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL SILENT CIRCLE, LLC BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.silentcircle.messaging.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.util.Pair;

import com.silentcircle.contacts.ContactsUtils;
import com.silentcircle.logs.Log;
import com.silentcircle.messaging.services.ZinaMessaging;
import com.silentcircle.messaging.util.Action;
import com.silentcircle.messaging.util.Extra;
import com.silentcircle.messaging.util.Notifications;

import java.util.concurrent.TimeUnit;

/**
 * Receiver for chat message updates.
 *
 * This receiver is intended to receive notifications about chat message updates and arrival
 * and show notification, when a new chat message arrives but application is in background.
 */
public class ChatNotification extends BroadcastReceiver implements ZinaMessaging.AxoMessagingStateCallback {

    private static final String TAG = ChatNotification.class.getSimpleName();

    public static final long MESSAGE_NOTIFICATION_DELAY = TimeUnit.SECONDS.toMillis(1);

    private Intent mLastNotificationIntent;
    private Context mContext;

    private static long sLastNotificationTime;
    private static Handler sHandler;
    private static Pair<Context, Intent> sPendingNotification;
    private static Runnable sHandleNotificationIntentRunnable = new Runnable() {
        @Override
        public void run() {
            if (sPendingNotification == null) {
                return;
            }
            Context context =  sPendingNotification.first;
            Intent intent = sPendingNotification.second;
            if (context != null && intent != null) {
                sendMessageNotification(context, intent);
            }
        }
    };

    /*
     * Receive update on chat message and act on it.
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        mLastNotificationIntent = null;
        mContext = null;

        ZinaMessaging axoMessaging = ZinaMessaging.getInstance();
        boolean axoRegistered = axoMessaging.isRegistered();
        if (!axoRegistered) {
            Log.d(TAG, "Axolotl not yet registered, wait for it before showing notification.");
            mLastNotificationIntent = intent;
            mContext = context;
            axoMessaging.addStateChangeListener(this);
        }
        else {
            handleNotificationIntent(context, intent);
        }
    }

    @Override
    public void axoRegistrationStateChange(boolean registered) {
        Log.d(TAG, "Axolotl state: " + registered + ", intent: " + mLastNotificationIntent);
        if (registered && mLastNotificationIntent != null && mContext != null) {
            handleNotificationIntent(mContext, mLastNotificationIntent);

            ZinaMessaging axoMessaging = ZinaMessaging.getInstance();
            axoMessaging.removeStateChangeListener(this);
            mLastNotificationIntent = null;
            mContext = null;
        }
    }

    public static void handleNotificationIntent(Context context, Intent intent) {
        switch (Action.from(intent)) {
            case RECEIVE_MESSAGE:
            case UPDATE_CONVERSATION:
                long now = System.currentTimeMillis();
                if (sLastNotificationTime == 0 || now - sLastNotificationTime >= MESSAGE_NOTIFICATION_DELAY) {
                    sLastNotificationTime = now;
                    sPendingNotification = null;
                    sendMessageNotification(context, intent);
                }
                else {
                    sPendingNotification = new Pair<>(context, intent);
                    if (sHandler == null) {
                        sHandler = new Handler();
                    }
                    sHandler.removeCallbacks(sHandleNotificationIntentRunnable);
                    sHandler.postDelayed(sHandleNotificationIntentRunnable, MESSAGE_NOTIFICATION_DELAY);
                }
                break;
            case DATA_RETENTION_EVENT:
                sendPolicyErrorNotification(context, intent);
                break;
            default:
                break;
        }
    }

    private static void sendMessageNotification(Context context, Intent intent) {
        String conversationPartnerId = Extra.PARTNER.from(intent);
        // CharSequence messageId = getReceivedMessageId(intent);

        // create intent used to launch conversation activity.
        Intent messagingIntent = ContactsUtils.getMessagingIntent(conversationPartnerId, context);

        /* Show notification.
         *
         * Leave message empty for now. It could be passed in intent from AxoMessaging but
         * that does not seem to be secure.
         */
        Notifications.sendMessageNotification(context, messagingIntent, conversationPartnerId);
    }

    private static void sendPolicyErrorNotification(Context context, Intent intent) {
        String conversationPartnerId = Extra.PARTNER.from(intent);
        String reason = Extra.REASON.from(intent);

        // create intent used to launch conversation activity.
        Intent messagingIntent = ContactsUtils.getMessagingIntent(conversationPartnerId, context);

        // Show notification.
        Notifications.sendPolicyNotification(context, messagingIntent, reason);
    }

    /*
    @Nullable
    private static CharSequence getReceivedMessageId(final @NonNull Intent intent) {
        CharSequence result = Extra.ID.getCharSequence(intent);
        if (TextUtils.isEmpty(result)) {
            CharSequence[] ids = Extra.IDS.getCharSequences(intent);
            if (ids != null && ids.length > 0) {
                result = ids[ids.length - 1];
            }
        }
        return result;
    }
     */
}
