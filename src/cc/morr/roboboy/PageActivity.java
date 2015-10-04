package cc.morr.roboboy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Scanner;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

public class PageActivity extends Activity {
    EditText editText;
    String filename;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.page);

        String pageName = getIntent().getStringExtra(MainActivity.PAGE_NAME);
        if (pageName == null) {
            pageName = getIntent().getData().toString().substring(18);
        }
        filename = MainActivity.LOCAL_PATH+pageName;

        editText = (EditText)findViewById(R.id.page_text);
        ScrollView scrollView = (ScrollView)findViewById(R.id.page_scroll_view);

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
        Pattern pagesPattern = Pattern.compile(getPagesPattern(new File(MainActivity.LOCAL_PATH)));
        String wikiViewURL = "cc.morr.roboboy://";
        Linkify.addLinks(editText, pagesPattern, wikiViewURL);
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
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
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
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Could not save page", Toast.LENGTH_LONG).show();
        }
    }

    private static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } finally {
            stream.close();
        }
    }

    private String getPagesPattern(File f) {
        List<String> fileList = new ArrayList<String>();
        fileList.clear();

        if (f.isDirectory()) {
            File[] files = f.listFiles();
            fileList.clear();
            for (File file : files){
                if (! file.getName().equals(".git"))
                    fileList.add(file.getName());
            }
            java.util.Collections.sort(fileList, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    if (o1.length() > o2.length()) {
                        return -1;
                    } else if (o1.length() < o2.length()) {
                        return 1;
                    }
                    return o1.compareTo(o2);
                } 
            });
        }

        String pattern = "(";
        pattern = pattern.concat(join(fileList,"|")).concat(")");
        System.out.println(pattern);
        return pattern;
    }

    static private String join(List<String> list, String conjunction)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : list)
        {
            if (first)
                first = false;
            else
                sb.append(conjunction);
            sb.append(item);
        }
        return sb.toString();
    }
}
