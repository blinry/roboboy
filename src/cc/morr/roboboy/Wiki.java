package cc.morr.roboboy;

import java.io.IOException;
import java.io.File;
import java.util.Iterator;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.Session;

public class Wiki {
    private String directory;
    private String authorName;
    private String authorEmail;

    private Git git;
    private Repository repository;

    public Wiki(String directory, String authorName, String authorEmail) {
        this.directory = directory;
        setAuthor(authorName, authorEmail);

        try {
            File localRepoDir = new File(directory + "/.git");
            if (! localRepoDir.isDirectory()) {
                Git.init().setDirectory(new File(directory)).call();
            }

            repository = new FileRepository(localRepoDir);
            git = new Git(repository);

            if (repository.getRef("HEAD") == null) {
                git.commit().setMessage("Sync from RoboBoy").setAuthor(authorName, authorEmail).call();
            }

            if (repository.getRef("phone") == null) {
                git.checkout().setCreateBranch(true).setName("phone").call();
            } else {
                git.checkout().setName("phone").call();
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
            /*
               runOnUiThread(new Runnable() {
               public void run() {
               Toast.makeText(context, "Sync: Git error", Toast.LENGTH_LONG).show();
               }
               });
               */
        } catch (IOException e) {
            e.printStackTrace();
            /*
               runOnUiThread(new Runnable() {
               public void run() {
               Toast.makeText(context, "Sync: IO error", Toast.LENGTH_LONG).show();
               }
               });
               */
        }
    }

    public void setAuthor(String authorName, String authorEmail) {
        this.authorName = authorName;
        this.authorEmail = authorEmail;
    }

    public void setKeyLocation(final String keyPath) {
        SshSessionFactory.setInstance(new JschConfigSessionFactory() {
            public void configure(Host hc, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
                try {
                    getJSch(hc, FS.DETECTED).addIdentity(keyPath);
                } catch (Exception e) {
                    /*
                       runOnUiThread(new Runnable() {
                       public void run() {
                       Toast.makeText(context, "Could not find SSH key", Toast.LENGTH_LONG).show();
                       }
                       });
                       */
                }
            }
        });
    }

    public void setRemoteURL(String url) {
        StoredConfig config = repository.getConfig();
        config.setString("remote", "origin", "url", url);
        config.setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*");
        try {
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDirectory() {
        return directory;
    }

    public String sync(final boolean isNetworkAvailable) {
        String message = "";

        try {
            git.add().addFilepattern(".").call();
            int changedFiles = git.status().call().getChanged().size();

            if (changedFiles > 0) {
                git.commit().setMessage("Sync from RoboBoy").setAuthor(authorName, authorEmail).call();
                message += "Committed "+changedFiles+" file"+(changedFiles == 1 ? "" : "s")+". ";
            }

            boolean diverged = false;

            if (isNetworkAvailable) {
                git.fetch().call();
                ObjectId headBeforeMerge = repository.resolve("HEAD");
                MergeResult mergeResult = git.merge().setFastForward(MergeCommand.FastForwardMode.FF_ONLY).include(repository.getRef("origin/master")).call();

                if (!mergeResult.getMergeStatus().isSuccessful()) {
                    message += "Master diverged. Please merge on a proper computer. ";
                    diverged = true;
                } else {
                    Iterator<RevCommit> it = git.log().addRange(headBeforeMerge, mergeResult.getNewHead()).call().iterator();

                    int newCommits = 0;
                    while(it.hasNext()) {
                        newCommits++;
                        it.next();
                    }

                    if (newCommits > 0) {
                        message += "Fetched "+newCommits+" commit"+(newCommits == 1 ? "" : "s")+". ";
                    }
                }

                git.push().add("phone").call();
                if (message.equals("")) {
                    message += "Synced.";
                }
            } else {
                message += "No network.";
            }
        } catch (CheckoutConflictException e) {
            message += "Conflict.";
        } catch (GitAPIException e) {
            message += "Git error.";
        } catch (IOException e) {
            message += "IO error.";
        }

        return message.trim();
    }
}
