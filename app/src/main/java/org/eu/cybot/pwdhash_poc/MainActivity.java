package org.eu.cybot.pwdhash_poc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private String url;
    private String pwd;
    private boolean fromShare;
    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final EditText editURL = findViewById(R.id.editURL);
        final EditText editPassword = findViewById(R.id.editPassword);
        final TextView textMode = findViewById(R.id.textMode);
        final FloatingActionButton fab = findViewById(R.id.fab);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (prefs.getBoolean("legacy", getString(R.string.pref_default_legacy).equalsIgnoreCase("true")))
            textMode.setText(getString(R.string.legacy_enabled));
        editPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    fab.requestFocus();
                    fab.callOnClick();
                    //Return false so done action gets performed, hiding the keyboard
                }
                return handled;
            }
        });

        fromShare = Intent.ACTION_SEND.equals(action) && "text/plain".equals(type);
        if (fromShare) {
            editURL.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
            editPassword.requestFocus();
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                boolean legacy = prefs.getBoolean("legacy", getString(R.string.pref_default_legacy).equalsIgnoreCase("true"));
                String salt = legacy ? null : prefs.getString("user_salt",  getString(R.string.pref_default_user_salt));
                String iterations = legacy ? null : prefs.getString("iterations", getString(R.string.pref_default_iterations));
                String uri = editURL.getText().toString();
                String domain = DomainExtractor.extractDomain(uri);
                String password = editPassword.getText().toString();

                View focus_view = MainActivity.this.getCurrentFocus();
                if (focus_view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null)
                        imm.hideSoftInputFromWindow(focus_view.getWindowToken(), 0);
                }

                Snackbar.make(view, "Calculating, please wait...", Snackbar.LENGTH_INDEFINITE).show();

                MainActivity.this.view = view;

                new HashPasswordTask(MainActivity.this).execute(password, domain, salt, iterations);
            }
        });

            }
        });
    }

    private static class HashPasswordTask extends AsyncTask<String, Void, String> {
        private final WeakReference<MainActivity> activityRef;
        HashPasswordTask(MainActivity context) {
            this.activityRef = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(String... strings) {
            if (strings.length != 4)
                return null;
            if (strings[2] == null)
                return HashedPassword.create(strings[0], strings[1]).toString();
            else
                return HashedPasswordPoC.create(strings[0], strings[1], strings[2], Integer.valueOf(strings[3])).toString();
        }

        @Override
        protected void onPostExecute(String hashed) {
            MainActivity activity = activityRef.get();
            if (activity == null || activity.isFinishing())
                return;

            ClipboardManager cb = (ClipboardManager) activity.getSystemService(CLIPBOARD_SERVICE);
            if (cb != null)
                cb.setPrimaryClip(ClipData.newPlainText("password", hashed));
            Snackbar.make(activity.view, "Password has been copied to clipboard", Snackbar.LENGTH_LONG).show();
            if (activity.fromShare)
                activity.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent modifySettings = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(modifySettings);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        url = ((EditText) findViewById(R.id.editURL)).getText().toString();
        pwd = ((EditText) findViewById(R.id.editPassword)).getText().toString();
    }

    @Override
    protected void onResume() {
        super.onResume();

        revealSwitch.setVisibility(View.INVISIBLE);
        revealText.setVisibility(View.INVISIBLE);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        final EditText editURL = findViewById(R.id.editURL);
        final EditText editPassword = findViewById(R.id.editPassword);
        final TextView textMode = findViewById(R.id.textMode);

        if (prefs.getBoolean("legacy", getString(R.string.pref_default_legacy).equalsIgnoreCase("true")))
            textMode.setText(getString(R.string.legacy_enabled));
        else
            textMode.setText("");

        if (pwd != null && !pwd.isEmpty())
            editPassword.setText(pwd);
        if (url != null && !url.isEmpty()) {
            editURL.setText(url);
            editPassword.requestFocus();
            editPassword.setSelection(editPassword.getText().length());
        }
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
    }
}
