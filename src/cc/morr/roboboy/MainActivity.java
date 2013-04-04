package cc.morr.roboboy;

import java.io.IOException;
import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

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

public class MainActivity extends Activity
{
    private String localPath, remotePath;
    private Repository localRepo;
    private Git git;

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

        localPath = getDir("reponew", MODE_PRIVATE).getPath();
        remotePath = "git@morr.cc:wiki2";
        deleteRecursive(getDir("reponew", MODE_PRIVATE));

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

    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }
}
