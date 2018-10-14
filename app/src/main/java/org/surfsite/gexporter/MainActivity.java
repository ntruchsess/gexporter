package org.surfsite.gexporter;

import android.Manifest;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tracks.exporter.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final Logger Log = LoggerFactory.getLogger(MainActivity.class);

    @Nullable
    private WebServer server = null;
    private TextView mTextView;
    private Spinner mSpeedUnit;
    private EditText mSpeed;
    private CheckBox mForceSpeed;
    private CheckBox mUse3DDistance;
    private CheckBox mInjectCoursePoints;
    private CheckBox mUseWalkingGrade;
    private CheckBox mReducePoints;
    private EditText mMaxPoints;
    private CheckBox mConvertRte2Wpts;
    private CheckBox mSetProximity;
    private EditText mMinProximity;
    private EditText mMaxProximity;
    private EditText mProximityRatio;
    private Gpx2FitOptions mGpx2FitOptions = null;
    private Gpx.Options mGpxOptions = null;
    File mDirectory = null;
    private NumberFormat mNumberFormat = NumberFormat.getInstance(Locale.getDefault());
    ArrayList<Uri> mUris;
    String mType;
    ContentResolver mCR;

    private final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 300;
    private final static int MY_PERMISSIONS_REQUEST_INTERNET = 301;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCR = getContentResolver();

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mSpeedUnit = (Spinner) findViewById(R.id.SPspeedUnits);
        ArrayAdapter<CharSequence> mSpeedUnitAdapter = ArrayAdapter.createFromResource(this,
                R.array.speedunits, android.R.layout.simple_spinner_item);
        mSpeedUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpeedUnit.setAdapter(mSpeedUnitAdapter);
        mSpeedUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long selId) {
                if (mGpx2FitOptions != null)
                    mGpx2FitOptions.setSpeedUnit(pos);
                setSpeedText(pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                if (mGpx2FitOptions != null)
                    mGpx2FitOptions.setSpeedUnit(0);
            }
        });

        mSpeed = (EditText) findViewById(R.id.editSpeed);
        char separator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
        mSpeed.setKeyListener(DigitsKeyListener.getInstance("0123456789:" + separator));
        mSpeed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (getCurrentFocus() != mSpeed) {
                        return;
                }

                if (editable.length() > 0 && mGpx2FitOptions != null) {
                    double speed;
                    try {
                        String s = editable.toString();
                        String[] as = s.split(":");
                        Collections.reverse(Arrays.asList(as));
                        speed = mNumberFormat.parse(as[0]).doubleValue();
                        if (as.length > 1) {
                            speed = mNumberFormat.parse(as[1]).doubleValue() + speed/60.0;
                        }
                        if (as.length > 2) {
                            speed = mNumberFormat.parse(as[2]).doubleValue() * 60 + speed;
                        }
                    } catch (ParseException e) {
                        speed = .0;
                    }
                    if (speed > .0) {
                        switch (mGpx2FitOptions.getSpeedUnit()) {
                            case 0:
                                speed = 1000.0 / 60.0 / speed;
                                break;
                            case 1:
                                speed = speed * 1000.0 / 3600.0;
                                break;
                            case 2:
                                speed = 1609.344 / 60.0 / speed;
                                break;
                            case 3:
                                speed = speed * 1609.344 / 3600.0;
                        }
                    }
                    mGpx2FitOptions.setSpeed(speed);
                }
            }
        });

        mForceSpeed = (CheckBox) findViewById(R.id.CBforceSpeed);
        mForceSpeed.setOnClickListener(this);

        mUse3DDistance = (CheckBox) findViewById(R.id.CBuse3D);
        mUse3DDistance.setOnClickListener(this);

        mInjectCoursePoints = (CheckBox) findViewById(R.id.CBinject);
        mInjectCoursePoints.setOnClickListener(this);

        mUseWalkingGrade = (CheckBox) findViewById(R.id.CBuseWalkingGrade);
        mUseWalkingGrade.setOnClickListener(this);

        mReducePoints = (CheckBox) findViewById(R.id.CBreducePoints);
        mReducePoints.setOnClickListener(this);

        mMaxPoints = (EditText) findViewById(R.id.editPointNumber);
        mMaxPoints.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0 && mGpx2FitOptions != null && mReducePoints.isChecked())
                    mGpx2FitOptions.setMaxPoints(Integer.valueOf(editable.toString()));
            }
        });

        mTextView = (TextView) findViewById(R.id.textv);

        mConvertRte2Wpts = (CheckBox) findViewById(R.id.CBroute2WayPoints);
        mConvertRte2Wpts.setOnClickListener(this);

        mSetProximity = (CheckBox) findViewById(R.id.CBsetProximity);
        mSetProximity.setOnClickListener(this);

        mMinProximity = (EditText) findViewById(R.id.editMinProximity);
        mMinProximity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0 && mGpxOptions != null && mSetProximity.isChecked())
                    mGpxOptions.setMinProximity(Double.valueOf(editable.toString()));
            }
        });

        mMaxProximity = (EditText) findViewById(R.id.editMaxProximity);
        mMaxProximity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0 && mGpxOptions != null && mSetProximity.isChecked())
                    mGpxOptions.setMaxProximity(Double.valueOf(editable.toString()));
            }
        });

        mProximityRatio = (EditText) findViewById(R.id.editProximityRatio);
        mProximityRatio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0 && mGpxOptions != null && mSetProximity.isChecked())
                    mGpxOptions.setProximityRatio(Double.valueOf(editable.toString())/100);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            case R.id.help:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/gimportexportdevs/gexporter/wiki/Help"));
                startActivity(browserIntent);
                break;
        }
        return true;
    }

    private void setSpeedText(int pos) {
        double speed = mGpx2FitOptions.getSpeed();
        if (Double.isNaN(speed))
            speed = 10.0;

        String val = null;
        switch (pos) {
            case 0:
                speed = 1000.0 / speed / 60.0;
                if (speed < 60) {
                    val = String.format(Locale.getDefault(), "%d:%02d", (int) speed, ((int) (speed * 60.0 + 0.5) % 60));
                } else {
                    val = String.format(Locale.getDefault(), "%d:%02d:%02d",((int) speed) / 60,
                            ((int) speed % 60), ((int) (speed * 60.0 + 0.5) % 60));
                }
                break;
            case 1:
                speed = speed / 1000.0 * 3600.0;
                val = String.format(Locale.getDefault(), "%.2f", speed);
                break;
            case 2:
                speed = 1609.344 / speed / 60.0;
                if (speed < 60) {
                    val = String.format(Locale.getDefault(), "%d:%02d", (int) speed, ((int) (speed * 60.0 + 0.5) % 60));
                } else {
                    val = String.format(Locale.getDefault(), "%d:%02d:%02d",((int) speed) / 60,
                            ((int) speed % 60), ((int) (speed * 60.0 + 0.5) % 60));
                }
                break;
            case 3:
                speed = speed / 1609.344 * 3600.0;
                val = String.format(Locale.getDefault(), "%.2f", speed);
        }
        if (mSpeed != null && val != null)
            mSpeed.setText(val);
    }

    @Override
    public void onClick(View v) {
        if (mGpx2FitOptions != null) {
            switch (v.getId()) {
                case R.id.CBforceSpeed:
                    mGpx2FitOptions.setForceSpeed(mForceSpeed.isChecked());
                    break;
                case R.id.CBinject:
                    mGpx2FitOptions.setInjectCoursePoints(mInjectCoursePoints.isChecked());
                    break;
                case R.id.CBreducePoints:
                    if (mReducePoints.isChecked()) {
                        String t = mMaxPoints.getText().toString();
                        if (t.length() > 0)
                            mGpx2FitOptions.setMaxPoints(Integer.decode(t));
                    } else
                        mGpx2FitOptions.setMaxPoints(0);
                    break;
                case R.id.CBuse3D:
                    mGpx2FitOptions.setUse3dDistance(mUse3DDistance.isChecked());
                    break;
                case R.id.CBuseWalkingGrade:
                    mGpx2FitOptions.setWalkingGrade(mUseWalkingGrade.isChecked());
                    break;
            }
        }
        if (mGpxOptions != null) {
            switch (v.getId()) {
                case R.id.CBroute2WayPoints:
                    mGpxOptions.setTransformRte2Wpts(mConvertRte2Wpts.isChecked());
                    break;
                case R.id.CBsetProximity:
                    mGpxOptions.setSetProximity(mSetProximity.isChecked());
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @Nullable String permissions[], @Nullable int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults != null && grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    try {
                        serveFiles();
                    } catch (IOException e) {
                        Log.error("Serving files failed: {}", e);
                    }

                } else {
                    mTextView.setText(R.string.no_permission);
                }
            }
        }
    }

    void rmdir(File f) throws IOException {
        if (f.isDirectory()) {
            for (File c : f.listFiles())
                rmdir(c);
        }
        if (!f.delete())
            throw new FileNotFoundException("Failed to delete file: " + f);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (server != null) {
            server.stop();
            server = null;
            clearTempDir();
        }
    }

    public void save(Gpx2FitOptions options) {
        Application app = getApplication();
        SharedPreferences mPrefs=app.getSharedPreferences(app.getApplicationInfo().name, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed=mPrefs.edit();
        Gson gson = new Gson();
        ed.putString(options.getClass().getName(), gson.toJson(options));
        ed.apply();
    }

    public Gpx2FitOptions load() {
        Application app = getApplication();
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        JsonParser parser=new JsonParser();
        SharedPreferences mPrefs=app.getSharedPreferences(app.getApplicationInfo().name, Context.MODE_PRIVATE);
        String json = mPrefs.getString(Gpx2FitOptions.class.getName(), null);
        Gpx2FitOptions opts = null;
        if (json != null && json.length() > 0)
            opts = gson.fromJson(parser.parse(json).getAsJsonObject(), Gpx2FitOptions.class);
        if (opts != null)
            return opts;
        else
            return new Gpx2FitOptions();
    }

    public void saveGpxOptions(Gpx.Options options) {
        Application app = getApplication();
        SharedPreferences mPrefs=app.getSharedPreferences(app.getApplicationInfo().name, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed=mPrefs.edit();
        Gson gson = new Gson();
        ed.putString(options.getClass().getName(), gson.toJson(options));
        ed.apply();
     }

    public Gpx.Options loadGpxOptions() {
        Application app = getApplication();
        Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
        JsonParser parser=new JsonParser();
        SharedPreferences mPrefs=app.getSharedPreferences(app.getApplicationInfo().name, Context.MODE_PRIVATE);
        String json = mPrefs.getString(Gpx.Options.class.getName(), null);
        Gpx.Options opts = null;
        if (json != null && json.length() > 0)
            opts = gson.fromJson(parser.parse(json).getAsJsonObject(), Gpx.Options.class);
        if (opts != null)
            return opts;
        else
            return new Gpx.Options();
    }

    @Override
    public void onPause() {
        super.onPause();
        save(mGpx2FitOptions);
        saveGpxOptions(mGpxOptions);
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        mGpx2FitOptions = load();
        mGpxOptions = loadGpxOptions();

        setSpeedText(mGpx2FitOptions.getSpeedUnit());

        mForceSpeed.setChecked(mGpx2FitOptions.isForceSpeed());
        mUse3DDistance.setChecked(mGpx2FitOptions.isUse3dDistance());
        mInjectCoursePoints.setChecked(mGpx2FitOptions.isInjectCoursePoints());
        mUseWalkingGrade.setChecked(mGpx2FitOptions.isWalkingGrade());

        mReducePoints.setChecked(mGpx2FitOptions.getMaxPoints() > 0);
        int maxp = mGpx2FitOptions.getMaxPoints();
        if (maxp == 0)
            maxp = 1000;
        mMaxPoints.setText(String.format(Locale.getDefault(), "%d", maxp));

        mSpeedUnit.setSelection(mGpx2FitOptions.getSpeedUnit());

        mConvertRte2Wpts.setChecked(mGpxOptions.isTransformRte2Wpts());
        mSetProximity.setChecked(mGpxOptions.isSetProximity());

        int minProx = (int)mGpxOptions.getMinProximity();
        if (minProx == 0) {
            minProx = 20;
        }
        mMinProximity.setText(String.format(Locale.getDefault(),"%d",minProx));

        int maxProx = (int)mGpxOptions.getMaxProximity();
        if (maxProx == 0) {
            maxProx = 100;
        }
        mMaxProximity.setText(String.format(Locale.getDefault(),"%d",maxProx));

        int proxRatio = (int)(mGpxOptions.getProximityRatio()*100);
        if (proxRatio == 0) {
            proxRatio = 50;
        }
        mProximityRatio.setText(String.format(Locale.getDefault(),"%d",proxRatio));
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        Log.debug("{}", intent);
        mType = intent.getType();

        if (Intent.ACTION_SEND.equals(action)) {
            Log.debug("ACTION_SEND");
            Uri uri = intent.getData();
            String URL;
            if (uri == null)
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri == null) {
                uri = Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT));
            }

            if (uri == null) {
                Log.debug("{}", intent.toString());
            }

            if (uri != null) {
                Log.debug("URI {}: type {} scheme {}", uri, intent.getType(), intent.getScheme());
                mUris = new ArrayList<>();
                mUris.add(uri);
            }
        }
        if (Intent.ACTION_VIEW.equals(action)) {
            Log.debug("ACTION_VIEW");
            Uri uri = intent.getData();
            if (uri != null) {
                Log.debug("URI {}: type '{}'", uri, intent.getType());
                mUris = new ArrayList<>();
                mUris.add(uri);
            }
        }
        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            Log.debug("ACTION_SEND_MULTIPLE");

            mUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }

        if (mUris != null && server != null) {
            server.stop();
        }

        if (server == null) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                    ) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                try {
                    serveFiles();
                } catch (IOException e) {
                    Log.error("Serving files failed: {}", e);
                }
            }
        }
    }

    private void copyInputStreamToFile( InputStream in, File file ) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            len=in.read(buf);
            String name = file.getAbsolutePath();
            if (!(name.endsWith(".fit") || name.endsWith(".FIT") || name.endsWith(".gpx") || name.endsWith(".GPX"))) {
                String sig = new String(Arrays.copyOf(buf, 8), "UTF-8");
                if (sig.length() > 5 && (sig.startsWith("<?xml") || sig.endsWith("<?xml"))) {
                    //noinspection ResultOfMethodCallIgnored
                    file.renameTo(new File(name + ".gpx"));
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    file.renameTo(new File(name + ".fit"));
                }
            }
            while(len>0){
                out.write(buf,0,len);
                len=in.read(buf);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            Log.error("copyInputStreamToFile {}", e);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                Log.error("Closing InputStream {}", e);
            }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = mCR.query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    protected void serveFiles() throws IOException {
        String rootdir;

        if (mUris == null) {
            rootdir = Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/Download/";
        } else {
            clearTempDir();

            mDirectory = new File(getCacheDir(), Long.toString(System.nanoTime()));
            //noinspection ResultOfMethodCallIgnored
            mDirectory.mkdir();

            rootdir = mDirectory.getAbsolutePath();

            for (Uri uri : mUris) {
                if (!uri.getScheme().equals("content") && !uri.getScheme().equals("file")) {
                    Log.debug("Skip URI {} scheme {}", uri, uri.getScheme());
                    continue;
                }
                Log.debug("Open URI {} scheme {}", uri, uri.getScheme());
                try {
                    InputStream is = mCR.openInputStream(uri);
                    String name = getFileName(uri);
                    copyInputStreamToFile(is, new File(mDirectory, name));
                } catch (FileNotFoundException e) {
                    Log.error("Exception Open URI:", e);
                }
            }
        }

        try {
            server = new WebServer(new File(rootdir), getCacheDir(), 22222, mGpx2FitOptions, mGpxOptions);
            server.start();
            Log.info("Web server initialized.");
        } catch (IOException | NoSuchAlgorithmException e) {
            Log.error("The server could not start: {}", e);
            mTextView.setText(R.string.no_server);
        }

        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".fit") || name.endsWith(".FIT") || name.endsWith(".gpx") || name.endsWith(".GPX");
            }
        };

        String[] filelist = new File(rootdir).list(filenameFilter);

        if (filelist == null) {
            mTextView.setText(R.string.no_permission);
        } else {
            if (filelist.length == 0) {
                mTextView.setText(R.string.no_files_to_serve);
            } else {
                Arrays.sort(filelist);
                mTextView.setText(String.format(getResources().getString(R.string.serving_from), rootdir, TextUtils.join("\n", filelist)));
            }
        }
    }

    private void clearTempDir() {
        if (mDirectory != null) {
            try {
                rmdir(mDirectory);
            } catch (IOException e) {
                Log.error("Failed to delete {} {}", mDirectory.getAbsolutePath(), e);
            } finally {
                mDirectory = null;
            }
        }
    }
}
