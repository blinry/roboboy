package cc.morr.roboboy;

import java.util.Scanner;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ScrollView;

import android.text.util.Linkify;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.app.ActionBar;

import android.content.Context;

import android.content.Intent;

public class PageActivity extends Activity {

    EditText editText;
    String filename;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page);

        String pageName = getIntent().getStringExtra(MainActivity.PAGE_NAME);
        filename = MainActivity.LOCAL_PATH+pageName;

        editText = (EditText)findViewById(R.id.page_text);
        ScrollView scrollView = (ScrollView)findViewById(R.id.page_scroll_view);
        scrollView.setSmoothScrollingEnabled(true);

        try {
            String text = readFile(filename);
            editText.setText(text);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Could not read page", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Could not read page", Toast.LENGTH_LONG).show();
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(pageName);

        Linkify.addLinks(editText, Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        save();
    }

    public void save(MenuItem menuItem) {
        save();
    }

    public void save() {
        PrintWriter out;
        try {
            out = new PrintWriter(filename);
            out.print(editText.getText());
            out.close();
            Toast.makeText(this, "Saved page", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Could not save page", Toast.LENGTH_LONG).show();
        }
    }

    private static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            /* Instead of using default, pass in a decoder. */
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }
}
