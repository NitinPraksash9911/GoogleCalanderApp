package com.example.calanderapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.lifecycle.lifecycleScope
import com.example.calanderapp.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.Event.Reminders
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date

const val TAG = "nowLogin"

/***
 *
 * NOTE: 1. we cannot use one Tap because it is not in our use case  {@link OneTapSignIn Activity's Notes on the top of class}
 *       2. we also cannot use GIS because there is no functionality for access the scopes
 * 1. check internet before signin
 * 2. if user want to delete the calendar event them give optional message to send the attendees if attendees available
 * 3. Or check after deleting the calendar event email will send to user automatically or not by google
 * */

class MainActivity : AppCompatActivity() {

    lateinit var service: Calendar
    val httpTransport = AndroidHttp.newCompatibleTransport()
    val jsonFactory: JsonFactory = JacksonFactory.getDefaultInstance()

    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initGoogleSignInClient()


        binding.signInButton.setOnClickListener {

            val account = getUserAcount()

            if (account == null) {

                signIn()

            } else {

                binding.tv.text = account.displayName

                // Use the authenticated account to sign in to the Calendar service.
                val credential = GoogleAccountCredential.usingOAuth2(
                    this, setOf(CalendarScopes.CALENDAR)
                )
                credential.selectedAccount = account.account

                service = Calendar.Builder(
                    httpTransport,
                    jsonFactory,
                    credential
                ).setApplicationName("Calendar App test")
                    .build()

                getCaledarData(service)

            }

        }

        binding.signOut.setOnClickListener {

            mGoogleSignInClient.signOut()
                .addOnCompleteListener(this) {
                    binding.calendarTv.text = ""
                    binding.tv.text = " please login"
                }.addOnFailureListener {

                    binding.tv.text = "please try again ${it.localizedMessage}"

                }
        }

        binding.insertEvent.setOnClickListener {

            insertEvent()
        }



    }

    private fun initGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(CalendarScopes.CALENDAR))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, 101)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101) {

            handleSignInResult(data!!)
        }

    }

    private fun getUserAcount(): GoogleSignInAccount? {

        return GoogleSignIn.getLastSignedInAccount(this)
    }

    private fun handleSignInResult(result: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                Log.d(TAG, "Signed in as " + googleAccount.email)

                binding.tv.text = googleAccount.displayName

                // Use the authenticated account to sign in to the Calendar service.
                val credential = GoogleAccountCredential.usingOAuth2(
                    this, setOf(CalendarScopes.CALENDAR)
                ).setBackOff(ExponentialBackOff())

                credential.selectedAccount = googleAccount.account

                service = Calendar.Builder(
                    httpTransport,
                    jsonFactory,
                    credential
                ).setApplicationName("Calendar App test")
                    .build()


                getCaledarData(service)

            }
            .addOnFailureListener { exception: Exception? ->
                Log.e(
                    TAG,
                    "Unable to sign in.",
                    exception
                )
                ("Unable to sign in: " + exception!!.localizedMessage).also { binding.tv.text = it }
            }
    }

    private fun getCaledarData(service: Calendar) {

        // this is only the way to interact with google api like drive, calender etc...
        lifecycleScope.launch(Dispatchers.IO) {

            // List the next 10 events from the primary calendar.
            val calenderData = StringBuilder()

            try {
                val now = DateTime(System.currentTimeMillis())
                val events = service.events().list("primary")
                    .setMaxResults(10)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute()

                val items = events.items


                if (items.isEmpty()) {
                    println("No upcoming events found.")
                    calenderData.append("No upcoming events found.")
                } else {
                    println("Upcoming events")
                    for (event in items) {
                        var start = event.start.dateTime
                        if (start == null) {
                            start = event.start.date
                        }

                        event.conferenceData

                        calenderData.append("${event.summary}, $start \n")

                        System.out.printf("%s (%s) \n (%s)", event.summary, start, event.htmlLink)
                    }
                }

            } catch (e: ApiException) {
                Toast.makeText(this@MainActivity, e.localizedMessage, Toast.LENGTH_LONG).show()
                calenderData.append(e.localizedMessage)
            }

            withContext(Dispatchers.Main) {
                binding.calendarTv.text = calenderData.toString()

            }

        }
    }

    private fun getCalendarDataWithPermission() {
        // Get currently signed in account (or null)
        val account = GoogleSignIn.getLastSignedInAccount(this)

        // Synchronously check for necessary permissions
        if (!GoogleSignIn.hasPermissions(account, Scope(CalendarScopes.CALENDAR))) {
            // Note: this launches a sign-in flow, however the code to detect
            // the result of the sign-in flow and retry the API call is not
            // shown here.
            GoogleSignIn.requestPermissions(
                this, 101,
                account, Scope(CalendarScopes.CALENDAR)
            )
            return
        }
//        val client: DriveResourceClient = Drive.getDriveResourceClient(this, account)
//        client.createContents()
//            .addOnCompleteListener(OnCompleteListener<Any?> {
//                // ...
//            })

    }

    private fun insertEvent() {

        var date = Date()
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val answer: String = formatter.format(date)

        Log.d("answer", answer)

        var event: Event = Event()
            .setSummary("Google I/O 2015")
            .setLocation("800 Howard St., San Francisco, CA 94103")
            .setDescription("A chance to hear more about Google's developer products.")

//        DATE_FORMAT_12 = yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
//        The output will be -: 2018-12-05T10:37:43.937Z

        val startDateTime = DateTime("2021-12-21T17:00:00-07:00")
        val start = EventDateTime()
            .setDateTime(startDateTime)
            .setTimeZone("America/Los_Angeles")
        event.setStart(start)

        val endDateTime = DateTime("2021-12-21T17:00:00-08:00")
        val end = EventDateTime()
            .setDateTime(endDateTime)
            .setTimeZone("America/Los_Angeles")
        event.setEnd(end)

        val recurrence = mutableListOf("RRULE:FREQ=DAILY;COUNT=2")
        event.setRecurrence(recurrence)

        val attendees = mutableListOf(
            EventAttendee().setEmail("lpage@example.com"),
            EventAttendee().setEmail("sbrin@example.com")
        )
        event.setAttendees(attendees)

        val reminderOverrides = mutableListOf(
            EventReminder().setMethod("email").setMinutes(24 * 60),
            EventReminder().setMethod("popup").setMinutes(10)
        )
        val reminders: Event.Reminders = Reminders()
            .setUseDefault(false)
            .setOverrides(reminderOverrides)
        event.setReminders(reminders)

        val calendarId = "primary"

        lifecycleScope.launch(Dispatchers.IO) {
            try {

                event = service.events().insert(calendarId, event).execute()
                System.out.printf("Event created: %s\n", event.getHtmlLink());

            } catch (e: ApiException) {

                System.out.printf("Event failed: %s\n", e.localizedMessage);

            }
        }

    }

    /**
     * this is the other way to get calendar service using Access token mostly used in beckend-side
     * */
    private fun otherWaytoGetCalendarServiceUisngAccessToken() {

        val jsonfile: String = applicationContext.assets.open("credentials.json").bufferedReader().use { it.readText() }

        val clientSecrets = GoogleClientSecrets.load(JacksonFactory(), StringReader(jsonfile))

//            if (this@MainActivity::tokenResponse.isInitialized.not()) {
//            tokenResponse = GoogleAuthorizationCodeTokenRequest(
//                httpTransport,
//                jsonFactory,
//                "https://www.googleapis.com/oauth2/v4/token",
//                clientSecrets.details.clientId,
//                clientSecrets.details.clientSecret,
//                account.serverAuthCode,
//                ""
//            ).execute()
//            }
//

//

//            An access token typically has an expiration date of 1 hour,
//            after which you will get an error if you try to use it.
//            GoogleCredential takes care of automatically "refreshing" the token, which simply means getting a new access token.
//            val mGoogleCredential = GoogleCredential().setAccessToken(account.idToken)

//            service = Calendar.Builder(httpTransport, jsonFactory, mGoogleCredential)
//                .setApplicationName("APP NAME")
//                .build()
    }

}