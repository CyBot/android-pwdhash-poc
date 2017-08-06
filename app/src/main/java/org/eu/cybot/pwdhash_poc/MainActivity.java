package org.eu.cybot.pwdhash_poc;

import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final EditText editURL = ((EditText) findViewById(R.id.editURL));
        final EditText editPassword = ((EditText) findViewById(R.id.editPassword));
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

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

        final boolean fromShare = Intent.ACTION_SEND.equals(action) && "text/plain".equals(type);
        if (fromShare) {
            editURL.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
            editPassword.requestFocus();
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String salt = prefs.getString("user_salt",  getString(R.string.pref_default_user_salt));
                String iterations = prefs.getString("iterations", getString(R.string.pref_default_iterations));
                String uri = editURL.getText().toString();
                String domain = DomainExtractor.extractDomain(uri);
                String password = editPassword.getText().toString();

                Snackbar.make(view, "Calculating, please wait...", Snackbar.LENGTH_INDEFINITE).show();

                new AsyncTask<String, Integer, String>() {
                    @Override
                    protected String doInBackground(String... strings) {
                        if (strings.length != 4)
                            return null;
                        return HashedPasswordPoC.create(strings[0], strings[1], strings[2], Integer.valueOf(strings[3])).toString();
                    }

                    @Override
                    protected void onPostExecute(String hashed) {
                        ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        cb.setPrimaryClip(ClipData.newPlainText("password", hashed));
                        Snackbar.make(view, "Password has been copied to clipboard", Snackbar.LENGTH_LONG).show();
                        if (fromShare)
                            MainActivity.this.finish();
                    }
                }.execute(password, domain, salt, iterations);

            }
        });
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent modifySettings = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(modifySettings);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
