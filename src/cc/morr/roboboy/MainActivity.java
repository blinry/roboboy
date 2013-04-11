package cc.morr.roboboy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.Session;

public class MainActivity extends ListActivity {
    private String localPath;
    private Repository localRepo;
    private Git git;
    private Context context;

    private List<String> fileList = new ArrayList<String>();

    public final static String PAGE_NAME = "cc.morr.roboboy.PAGE_NAME";
    public final static String LOCAL_PATH = "/sdcard/wiki/";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this;

        SshSessionFactory.setInstance(new JschConfigSessionFactory() {
            public void configure(Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
                try {
                    getJSch(hc, FS.DETECTED).addIdentity("/sdcard/.ssh/phone");
                } catch (Exception e) {
                    throw new RuntimeException("Could not find private key");
                }
            }
        });

        localPath = LOCAL_PATH;

        listDir(new File(localPath));

        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setTextFilterEnabled(true);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        SearchView searchView = (SearchView)(menu.findItem(R.id.menu_search)).getActionView();
        searchView.setIconifiedByDefault(false);
        searchView.setMaxWidth(400);
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
                sync();
                return true;
            case R.id.menu_delete:
                deleteRecursive(new File(localPath));
                Toast.makeText(this, "Deleted local repository", Toast.LENGTH_SHORT).show();
                listDir(new File(localPath));
                return true;
            case R.id.menu_preferences:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_new:
                File newFile = new File(localPath+Long.toString(System.currentTimeMillis()));
                try {
                    newFile.createNewFile();
                    listDir(new File(localPath));
                } catch(IOException e) {
                    Toast.makeText(this, "Could not create new page", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sync() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    File localRepoDir = new File(localPath + "/.git");
                    if (! localRepoDir.isDirectory()) {
                        Git.cloneRepository().setURI(PreferenceManager.getDefaultSharedPreferences(context).getString("repository", "")).setDirectory(new File(localPath)).call();
                    }

                    localRepo = new FileRepository(localRepoDir);
                    git = new Git(localRepo);

                    if (localRepo.getRef("phone") == null)
                        git.checkout().setCreateBranch(true).setName("phone").call();
                    else
                        git.checkout().setName("phone").call();

                    git.fetch().call();
                    git.merge().setFastForward(MergeCommand.FastForwardMode.FF_ONLY).include(localRepo.getRef("origin/master")).call();
                    git.add().addFilepattern(".").call();
                    if (! git.status().call().isClean()) {
                        git.commit().setMessage("Sync from RoboBoy").call();
                        git.push().add("phone").setForce(true).call();
                    }

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context, "\"synced\"", Toast.LENGTH_SHORT).show();
                            listDir(new File(localPath));
                        }
                    });
                } catch (CheckoutConflictException e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context, "Sync: Conflict error", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (GitAPIException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context, "Sync: Git error", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context, "Sync: IO error", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
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
}
