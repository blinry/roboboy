package cc.morr.roboboy;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ScrollView;

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
        filename = getDir("wiki", Context.MODE_WORLD_WRITEABLE).getPath()+"/"+pageName;

        editText = (EditText)findViewById(R.id.page_text);
        ScrollView scrollView = (ScrollView)findViewById(R.id.page_scroll_view);
        scrollView.setSmoothScrollingEnabled(true);

        try {
            editText.setText(new Scanner(new File(filename)).useDelimiter("\\Z").next());
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Could not read page", Toast.LENGTH_LONG).show();
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(pageName);
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
}
