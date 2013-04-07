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
import com.jcraft.jsch.Session;

public class MainActivity extends ListActivity
{
    private String localPath;
    private Repository localRepo;
    private Git git;

    private List<String> fileList = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

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
        //localPath = getDir("hoho", Context.MODE_WORLD_WRITEABLE).getPath();

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
                Toast.makeText(this, "synced", Toast.LENGTH_SHORT).show();
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

    private void sync() {
        try {
            localRepo = new FileRepository(localPath + "/.git");
            String branch = localRepo.getBranch();
            System.out.println(branch);
            git = new Git(localRepo);

            git.fetch().call();
            try {
                git.merge().setFastForward(MergeCommand.FastForwardMode.FF_ONLY).include(localRepo.getRef("origin/master")).call();
            } catch (Exception e) {
                Toast.makeText(this, "master diverged, plese merge on a proper computer.", Toast.LENGTH_SHORT).show();
            }
            System.out.println("supa");
        } catch (Exception e) {
            System.out.println("ex, cloning");
            try {
                deleteRecursive(new File(localPath));
                System.out.println("repo: "+PreferenceManager.getDefaultSharedPreferences(this).getString("repository", ""));
                Git.cloneRepository().setURI(PreferenceManager.getDefaultSharedPreferences(this).getString("repository", "")).setDirectory(new File(localPath)).call();
            } catch (GitAPIException e2) {
                System.out.println("apiex");
                e2.printStackTrace(System.out);
            }
            try {
                String branch = localRepo.getBranch();
                System.out.println(branch);
                git = new Git(localRepo);
            } catch (IOException e2) {
                System.out.println("oh");
            }
        }

        try {
            if (localRepo.getRef("phone") == null)
                git.checkout().setCreateBranch(true).setName("phone").call();
            else
                git.checkout().setName("phone").call();
        } catch (GitAPIException e) {
            throw new RuntimeException();
        } catch (IOException e) {
            throw new RuntimeException();
        }

        try {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Autocommit RoboBoy").call();
            git.push().add("phone").setForce(true).call();
            System.out.println("push ok");
        } catch (GitAPIException e) {
            throw new RuntimeException();
        }

        listDir(new File(localPath));
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
