package cc.morr.roboboy;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class WikiService extends IntentService {
    private Handler handler;
    private Context context;

    public WikiService() {
        super("WikiService");
        handler = new Handler();
        context = this;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        if (pref.getString("repository", "") == "") {
            handler.post(new DisplayToast("Please specify a remote URL in the Settings"));
            return;
        }

        Wiki wiki = new Wiki(MainActivity.LOCAL_PATH, pref.getString("user_name", ""), pref.getString("user_email", ""));
        wiki.setKeyLocation(pref.getString("key_location", ""));
        wiki.setRemoteURL(pref.getString("repository", ""));

        String message = wiki.sync(true);
        handler.post(new DisplayToast(message));
        Intent reloadIntent = new Intent();
        reloadIntent.setAction("cc.morr.roboboy.RELOAD");
        LocalBroadcastManager.getInstance(this).sendBroadcast(reloadIntent);
    }

    private class DisplayToast implements Runnable {
        String text;

        public DisplayToast(String text){
           this.text = text;
        }

        public void run(){
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }
}
