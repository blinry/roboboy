package cc.morr.roboboy;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;

import android.view.MenuItem;
import android.app.ActionBar;

import android.content.Intent;

public class PageActivity extends Activity {

    EditText editText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page);

        String pageName = getIntent().getStringExtra(MainActivity.PAGE_NAME);

        editText = (EditText)findViewById(R.id.page_text);

        try {
            editText.setText(new Scanner(new File(MainActivity.LOCAL_PATH+pageName)).useDelimiter("\\Z").next());
        } catch (FileNotFoundException e) {
            //TODO
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(pageName);
    }
@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
