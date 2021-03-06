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
package com.silentcircle.accounts;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.silentcircle.logs.Log;
import com.silentcircle.silentphone2.R;
import com.silentcircle.silentphone2.activities.DialogHelperActivity;
import com.silentcircle.silentphone2.activities.ProvisioningActivity;
import com.silentcircle.silentphone2.util.ConfigurationUtilities;
import com.silentcircle.silentphone2.util.PinnedCertificateHandling;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;

import javax.net.ssl.SSLContext;


public class AccountCorpEmailEntry1 extends Fragment {
    private static final String TAG = "AccountCorpEmailEntry1";
    private AuthenticatorActivity mParent;

    String mProvisioningDomainError;
    String mSccpsProductionBaseUrl;
    String mDevelopmentBaseUrl;

    public static AccountCorpEmailEntry1 newInstance(Bundle args) {
        AccountCorpEmailEntry1 f = new AccountCorpEmailEntry1();
        f.setArguments(args);
        return f;
    }

    public AccountCorpEmailEntry1() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProvisioningDomainError = getString(R.string.provisioning_domain_error);
        mSccpsProductionBaseUrl = getString(R.string.sccps_production_base_url);
        mDevelopmentBaseUrl = getString(R.string.sccps_development_base_url);
    }

    private class GetDomainInfo extends AsyncTask<String, Void, JSONObject>
    {
        private String username;
        private String dom;

        @Override
        protected JSONObject doInBackground(String... params) {
            if(params.length != 1) {
                return null;
            } else {
                try {
                    // Username contains @ guaranteed by original caller
                    username = params[0];
                    String[] bits = params[0].split("@");
                    if(bits.length >= 2) {
                        dom = bits[1];
                    } else {
                        dom = bits[0];
                    }
                    SSLContext context = PinnedCertificateHandling.getPinnedSslContext(ConfigurationUtilities.mNetworkConfiguration);
                    if (context == null) {
                        Log.e(TAG, "Cannot get a trusted/pinned SSL context; failing");
                        throw new AssertionError("Failed to get pinned SSL context");
                    }
                    String burl = ConfigurationUtilities.mUseDevelopConfiguration
                            ? mDevelopmentBaseUrl : mSccpsProductionBaseUrl;
                    return AccountCorpUtil.httpsGet(context, burl +
                            ConfigurationUtilities.getAuthBase(mParent.getBaseContext()) +
                            URLEncoder.encode(username, "UTF-8") + "/", 0);

                } catch (Exception e) {
                    Log.d(TAG, "Could not get authorization data: ", e);
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(JSONObject resp) {

            if(resp == null) {
                Log.e(TAG, "Failed to obtain oauth host; HTTP error");
            } else {
                if(!resp.has("auth_type")) {
                    Log.e(TAG, "Failed to obtain auth_type; no result");
                }

                try {
                    String authType = resp.getString("auth_type");
                    if (!authType.equals("ADFS") && !authType.equals("OIDC")) {
                        Log.e(TAG, "Failed to obtain oauth host; unknown auth type " + authType);
                    } else {
                        if (!resp.has("auth_uri") || !resp.has("redirect_uri")) {
                            String msg = "internal_error";
                            if (resp.has("error")) {
                                msg = resp.getString("error");
                            } else if (resp.has("msg")) {
                                msg = resp.getString("msg");
                            }
                            Log.e(TAG, "Failed to obtain oauth host; no URL; msg=" + msg);
                        } else {
                            mParent.accountCorpEmailEntry2(
                                    resp.getString("auth_uri"), authType, dom, username,
                                    resp.getString("redirect_uri"));
                            return;
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to obtain oauth host; JSONException", e);
                }

                // Failed; give the user another chance
                String errorMsg = String.format(mProvisioningDomainError, dom);
                DialogHelperActivity.showDialog(R.string.information_dialog, errorMsg, android.R.string.ok, -1);
                mParent.accountStep1();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View retView = inflater.inflate(R.layout.provisioning_corp_account_email_entry1, container, false);
        if(retView == null) {
            return null;
        }

        return retView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle savedArgs = getArguments();

        new GetDomainInfo().execute(savedArgs.getString(ProvisioningActivity.USERNAME));
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mParent = (AuthenticatorActivity) getActivity();
    }

    /*
     * Deprecated on API 23
     * Use onAttachToContext instead
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mParent = (AuthenticatorActivity) activity;
        }
    }

}
