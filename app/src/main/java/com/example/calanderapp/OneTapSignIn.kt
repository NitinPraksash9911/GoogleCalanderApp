package com.example.calanderapp

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

/**
 * @Note: Google Identity Services(GIS) will eventually replace the existing Google Sign-In API.
 * For new apps we recommend using Google Identity Services instead of the Google Sign-In API for sign-in and sign-up,
 * unless you need authorization, Server-Side Access, or custom OAuth scopes.
 * These features are coming in future versions of Google Identity Services.
 *
 * @Note: You should use GIS API only when the user explicitly shows intent to sign in with Google.
 * For example, use this API when they click a "Sign in with Google" button in your app.
 *
 *@Note: You should not use this GIS API to prompt the user to sign-in on app launch or in response to another trigger such as adding an item to the shopping cart.
 * For these use cases, use One Tap sign-in and sign-up.
 * */

const val client_id = "67810104480-qk8qddnukjot90mkveivt2fmmcm3m6p8.apps.googleusercontent.com"

class OneTapSignIn : AppCompatActivity() {
    private var showOneTapUI: Boolean = false
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_tap_sign_in)

//        initNewSignGIS()
        initOneTapSignin()
    }

    //this is for new SignIn API
    private fun initNewSignGIS() {
        val request =
            GetSignInIntentRequest.builder()
                .setServerClientId(client_id)
                .build();



        Identity.getSignInClient(this)
            .getSignInIntent(request).addOnSuccessListener { result ->
                startIntentSenderForResult(
                    result.getIntentSender(),
                    102,
                    /* fillInIntent= */ null,
                    /* flagsMask= */ 0,
                    /* flagsValue= */ 0,
                    /* extraFlags= */ 0,
                    /* options= */ null
                );

            }.addOnFailureListener {

            }


    }

    //this is for oneTAP signin
    private fun initOneTapSignin() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(client_id)
                    // Show all accounts on the device.
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, 101,
                        null, 0, 0, 0, null
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No saved credentials found. Launch the One Tap sign-up flow, or
                // do nothing and continue presenting the signed-out UI.
                Log.d(TAG, e.localizedMessage)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            101 -> {//one tap sigin
                try {
                    val credential = oneTapClient.getSignInCredentialFromIntent(data)
                    val idToken = credential.googleIdToken
                    val username = credential.id
                    val password = credential.password
                    Toast.makeText(this, username, Toast.LENGTH_LONG).show()
                    when {
                        idToken != null -> {
                            // Got an ID token from Google. Use it to authenticate
                            // with your backend.
                            Log.d(TAG, "Got ID token.")
                        }
                        password != null -> {
                            // Got a saved username and password. Use them to authenticate
                            // with your backend.
                            Log.d(TAG, "Got password.")
                        }
                        else -> {
                            // Shouldn't happen.
                            Log.d(TAG, "No ID token or password!")
                        }
                    }
                } catch (e: ApiException) {
                    catchApiException(e)
                }
            }
            102 -> {//GIS sigin

                try {
                    val credential = Identity.getSignInClient(this).getSignInCredentialFromIntent(data);
                    // Signed in successfully - show authenticated UI

                    Toast.makeText(this, credential.displayName, Toast.LENGTH_LONG).show()
                } catch (e: ApiException) {
                    catchApiException(e)
                }
            }
        }
    }

    private fun catchApiException(e: ApiException) {

        when (e.statusCode) {
            CommonStatusCodes.CANCELED -> {
                Log.d(TAG, "One-tap dialog was closed.")
                // Don't re-prompt the user.
                showOneTapUI = false
            }
            CommonStatusCodes.NETWORK_ERROR -> {
                Log.d(TAG, "One-tap encountered a network error.")
                // Try again or just ignore.
            }
            else -> {
                Log.d(
                    TAG, "Couldn't get credential from result." +
                            " (${e.localizedMessage})"
                )
            }
        }
    }
}



