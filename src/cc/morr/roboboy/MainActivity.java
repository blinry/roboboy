package cc.morr.roboboy;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.preference.PreferenceManager;
import android.util.Log;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.api.errors.CheckoutConflictException;

import com.jcraft.jsch.Session;

public class MainActivity extends ListActivity
{
    private String localPath;
    private Repository localRepo;
    private Git git;
    private Context context;

    private List<String> fileList = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this;

        SshSessionFactory.setInstance(new JschConfigSessionFactory() {
            public void configure(Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
                try {
                    getJSch(hc, FS.DETECTED).addIdentity("/mnt/sdcard/.ssh/id_rsa");
                } catch (Exception e) {
                    throw new RuntimeException("Could not find private key");
                }
            }
        });

        localPath = "/mnt/sdcard/wiki";
        //localPath = getDir("kähä", Context.MODE_WORLD_WRITEABLE).getPath();

        System.out.println(localPath);

        listDir(new File(localPath));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
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
                Toast.makeText(this, "deleted", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.menu_preferences:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void cloneIt() {
        try {
            Git.cloneRepository().setURI(PreferenceManager.getDefaultSharedPreferences(this).getString("repository", "")).setDirectory(new File(localPath)).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        Log.d("RoboBoy", "clone done");
    }

    private void init() {
        File localRepoDir = new File(localPath + "/.git");
        if (! localRepoDir.isDirectory()) {
            Log.d("RoboBoy", "need to clone");
            cloneIt();
        }
        try {
            localRepo = new FileRepository(localRepoDir);
        } catch (IOException e) {
            Log.d("RoboBoy", "repo creation failed");
            e.printStackTrace();
        }
        git = new Git(localRepo);

        try {
            if (localRepo.getRef("phone") == null)
                git.checkout().setCreateBranch(true).setName("phone").call();
            else
                git.checkout().setName("phone").call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.d("RoboBoy", "init done");
    }

    private void fetch() {
        try {
            git.fetch().call();
        } catch (GitAPIException e) {
            Log.d("RoboBoy", "fetch failed");
            e.printStackTrace();
        }
        Log.d("RoboBoy", "fetch done");
    }

    private void merge() {
        try {
            git.merge().setFastForward(MergeCommand.FastForwardMode.FF_ONLY).include(localRepo.getRef("origin/master")).call();
        } catch (CheckoutConflictException e) {
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(context, "master on remote diverged, please merge on a real computer", Toast.LENGTH_LONG).show();
                }
            });
        } catch (GitAPIException e) {
            Log.d("RoboBoy", "merge failed: git");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("RoboBoy", "merge failed: io");
            e.printStackTrace();
        }
        Log.d("RoboBoy", "merge done");
    }

    private void commit() {
        try {
            git.add().addFilepattern(".").call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        try {
            git.commit().setMessage("Sync from RoboBoy").call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        Log.d("RoboBoy", "commit done");
    }

    private void push() {
        try {
            git.push().add("phone").setForce(true).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        Log.d("RoboBoy", "push done");
    }

    private void sync() {
        new Thread(new Runnable() {
            public void run() {
                init();
                fetch();
                merge();
                commit();
                push();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(context, "\"synced\"", Toast.LENGTH_SHORT).show();
                        listDir(new File(localPath));
                    }
                });
            }
        }).start();
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        Uri uri = Uri.parse("file://"+localPath+"/"+fileList.get(position));
        intent.setDataAndType(uri, "text/plain");
        startActivity(intent);
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        }
        fileOrDirectory.delete();
    }

    private void listDir(File f){
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
