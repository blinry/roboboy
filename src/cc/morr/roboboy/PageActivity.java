package cc.morr.roboboy;

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

        editText = (EditText)findViewById(R.id.page_text);

        String pageName = getIntent().getStringExtra(MainActivity.PAGE_NAME);
        editText.setText(pageName);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
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
