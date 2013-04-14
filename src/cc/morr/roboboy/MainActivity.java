package cc.morr.roboboy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

public class MainActivity extends ListActivity {
    private Wiki wiki;

    private String localPath;
    private Context context;

    private List<String> fileList = new ArrayList<String>();

    public final static String PAGE_NAME = "cc.morr.roboboy.PAGE_NAME";
    public final static String LOCAL_PATH = "/sdcard/wiki/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this;

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        localPath = LOCAL_PATH;

        wiki = new Wiki(localPath, pref.getString("user_name", ""), pref.getString("user_email", ""));
        wiki.setKeyLocation(pref.getString("key_location", ""));
        wiki.setRemoteURL(pref.getString("repository", ""));

        listDir(new File(localPath));

        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setTextFilterEnabled(true);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        if (PreferenceManager.getDefaultSharedPreferences(context).getString("repository", "") == "") {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        SearchView searchView = (SearchView)(menu.findItem(R.id.menu_search)).getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.setMaxWidth(500);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextChange(String newText) {
                ((ArrayAdapter)MainActivity.this.getListAdapter()).getFilter().filter(newText);
                return false;
            }
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync:
                new WikiSyncTask(this).execute(wiki);
                return true;
            case R.id.menu_delete:
                deleteRecursive(new File(localPath));
                Toast.makeText(this, "Deleted local repository", Toast.LENGTH_SHORT).show();
                listDir(new File(localPath));
                return true;
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
                /*
            case R.id.menu_new:
                File newFile = new File(localPath+Long.toString(System.currentTimeMillis()));
                try {
                    newFile.createNewFile();
                    listDir(new File(localPath));
                } catch(IOException e) {
                    Toast.makeText(this, "Could not create new page", Toast.LENGTH_SHORT).show();
                }
                return true;
                */
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, PageActivity.class);
        intent.putExtra(PAGE_NAME, (String)getListAdapter().getItem(position));
        startActivity(intent);
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        }
        fileOrDirectory.delete();
    }

    private void listDir(File f) {
        fileList.clear();

        if (f.isDirectory()) {
            File[] files = f.listFiles();
            fileList.clear();
            for (File file : files){
                if (! file.getName().equals(".git"))
                    fileList.add(file.getName());
            }
            java.util.Collections.sort(fileList);
        }

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager 
            = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private class WikiSyncTask extends AsyncTask<Wiki, Integer, String> {
        Context context;

        public WikiSyncTask(Context context) {
            this.context = context;
        }

        protected String doInBackground(Wiki... wikis) {
            String message = "";
            Wiki wiki = wikis[0];

            return wiki.sync(true);
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(String result) {
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            listDir(new File(localPath));
        }
    }
}
