package cc.morr.roboboy;

import java.io.IOException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.OpenSshConfig.Host;

import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import com.jcraft.jsch.Session;

public class MainActivity extends ListActivity
{
    private String localPath, remotePath;
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
                    throw new RuntimeException();
                }
            }
        });

        localPath = "/mnt/sdcard/wiki";
        remotePath = "git@morr.cc:wiki";
        //deleteRecursive(new File(localPath));

        try {
            localRepo = new FileRepository(localPath + "/.git");
            String branch = localRepo.getBranch();
            System.out.println(branch);
            git = new Git(localRepo);

            git.fetch().call();
            System.out.println("supa");

        } catch (Exception e) {
            System.out.println("ex, cloning");
            try {
            Git.cloneRepository().setURI(remotePath).setDirectory(new File(localPath)).call();
            } catch (GitAPIException e2) {
                System.out.println("apiex");
                e2.printStackTrace(System.out);
            }
            try {
                String branch = localRepo.getBranch();
                System.out.println(branch);
            } catch (IOException e2) {
                System.out.println("oh");
            }
        }

        listDir(new File(localPath));
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    void listDir(File f){
        File[] files = f.listFiles();
        fileList.clear();
        for (File file : files){
            fileList.add(file.getName());
        }
        java.util.Collections.sort(fileList);

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity, menu);
        return true;
    }

    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        Uri uri = Uri.parse("file://"+localPath+"/"+fileList.get(position));
        intent.setDataAndType(uri, "text/plain");
        startActivity(intent);
    }
}
