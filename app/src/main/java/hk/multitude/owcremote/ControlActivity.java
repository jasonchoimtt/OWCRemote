package hk.multitude.owcremote;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import hk.multitude.owcremote.widgets.ButtonPad;
import hk.multitude.owcremote.widgets.JoystickPad;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jason on 12/3/15.
 */
public class ControlActivity extends ActionBarActivity implements Handler.Callback,
        JoystickPad.JoystickListener, ButtonPad.ButtonStateListener {
    private Handler mHandler;
    private BTConnectionThread mBtThread;
    private ControllerThread mController;
    private BluetoothAdapter mBt;
    private BluetoothDevice mDevice;

    private View mStatusCircle;
    private TransitionDrawable mStatusDrawable;
    private ProgressBar mStatusProgress;
    private Button mStatusText;

    private ListView mLogView;
    private LogAdapter mLogAdapter;

    private ListView mVarView;
    private VarAdapter mVarAdapter;
    private static final Pattern mVarPattern = Pattern.compile("^([^:;\r\n]+)(:|=)([^:;\r\n]+);?$");

    private boolean mRecording;
    private StringWriter mRecordOut;
    private String[] mRecordHeader;
    private long mRecordLast;
    private int mRecordPeriod;

    private DeviceConnection mConn = new DeviceConnection() {
        @Override
        public void write(String message) {
            mBtThread.write(message);
            String[] ms = message.split(";");
            for (String i : ms){
                final Matcher m = mVarPattern.matcher(i);
                if (m.find()) {
                    if (m.group(1).equals("#record")) {
                        if (!mRecording) {
                            mRecording = true;
                            mRecordOut = new StringWriter();
                            mRecordHeader = mVarAdapter.keys();
                            mRecordOut.write("time,");
                            for (int j = 0; j < mRecordHeader.length-1; j++) {
                                mRecordOut.write(mRecordHeader[j]+",");
                            }
                            mRecordOut.write(mRecordHeader[mRecordHeader.length-1]+"\n");
                            mRecordLast = new Date().getTime();
                            try {
                                mRecordPeriod = Integer.parseInt(m.group(3));
                            } catch (NumberFormatException e) {
                                mRecordPeriod = 500;
                            }
                            Toast.makeText(ControlActivity.this, "Recording vars every "+mRecordPeriod+"ms", Toast.LENGTH_SHORT).show();
                        } else {
                            mRecording = false;
                            String out = mRecordOut.toString();
                            ClipboardManager cm = (ClipboardManager) ControlActivity.this.getSystemService(CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText("record", out));
                            Toast.makeText(ControlActivity.this, R.string.copied, Toast.LENGTH_SHORT).show();
                            mRecordOut = null;
                            Intent it = new Intent(Intent.ACTION_SEND);
                            it.setType("text/csv");
                            it.putExtra(Intent.EXTRA_TEXT, out);
                            startActivity(Intent.createChooser(it, "Share CSV to"));
                        }
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mVarAdapter.add(m.group(1), m.group(3));
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void flush() {
            mBtThread.flush();
        }

        @Override
        public boolean isConnected() {
            return mBtThread.isConnected();
        }
    };

    private ControlState mState = new ControlState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        mHandler = new Handler(this);
        mBt = BluetoothAdapter.getDefaultAdapter();
        mDevice = getIntent().getParcelableExtra(ConnectActivity.EXTRA_DEVICE);
        mBtThread = null;

        ((JoystickPad) findViewById(R.id.joystick)).setJoystickListener(this);
        ((ButtonPad) findViewById(R.id.buttonPad)).setButtonStateListener(this);

        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName());

        mStatusCircle = findViewById(R.id.statusCircle);
        mStatusDrawable = (TransitionDrawable) findViewById(R.id.statusCircle).getBackground();
        mStatusProgress = (ProgressBar) findViewById(R.id.statusProgress);
        mStatusText = (Button) findViewById(R.id.statusText);

        mStatusText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBtThread.isAlive()) {
                    if (mBtThread.isConnected()) {
                        disconnect();
                    }
                } else {
                    connect();
                }
            }
        });

        mLogView = (ListView) findViewById(R.id.logView);
        mLogAdapter = new LogAdapter(this, new String[1000]);
        mLogView.setAdapter(mLogAdapter);
        mLogView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ClipboardManager cm = (ClipboardManager) ControlActivity.this.getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("log", (String) mLogAdapter.getItem(position)));
                Toast.makeText(ControlActivity.this, R.string.copied, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mVarView = (ListView) findViewById(R.id.varView);
        mVarAdapter = new VarAdapter(this);
        mVarView.setAdapter(mVarAdapter);

        ((EditText) findViewById(R.id.monitorInput)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND && mConn.isConnected()) {
                    mConn.write(v.getText().toString());
                    v.setText("");
                    return true;
                }
                return false;
            }
        });

        mRecording = false;

        // Configure orientation
        onConfigurationChanged(getResources().getConfiguration());

        Log.d("ControlActivity", "initialising...");
    }

    @Override
    protected void onStart() {
        super.onStart();
        connect();
    }

    @Override
    protected void onStop() {
        disconnect();
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        View statusLine = findViewById(R.id.statusLine);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) statusLine.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            findViewById(R.id.monitor).setVisibility(View.GONE);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        } else {
            findViewById(R.id.monitor).setVisibility(View.VISIBLE);
            lp.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
        }
        statusLine.setLayoutParams(lp);
    }

    private void connect() {
        mBtThread = new BTConnectionThread(mBt, mDevice, mHandler);
        mBtThread.start();
        mController = new ControllerThread(mConn);
        mController.start();

        mStatusDrawable.resetTransition();
        mStatusCircle.setVisibility(View.GONE);
        mStatusProgress.setVisibility(View.VISIBLE);
        mStatusText.setText(R.string.connecting);
        mLogAdapter.add("# Connecting");
    }

    private void disconnect() {
        mController.interrupt();
        mBtThread.close();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case BTConnectionThread.CONNECTED:
                mStatusDrawable.startTransition(200);
                mStatusCircle.setVisibility(View.VISIBLE);
                mStatusProgress.setVisibility(View.GONE);
                mStatusText.setText(R.string.connected);
                mLogAdapter.add("# Connected to device");
                return true;
            case BTConnectionThread.CONNECTION_FAILED:
                Toast.makeText(this, R.string.connection_failed, Toast.LENGTH_SHORT).show();
                mLogAdapter.add("# Connection failed");
                mStatusDrawable.resetTransition();
                mStatusCircle.setVisibility(View.VISIBLE);
                mStatusProgress.setVisibility(View.GONE);
                mStatusText.setText(R.string.disconnected);
                return true;
            case BTConnectionThread.DISCONNECTED:
                mLogAdapter.add("# Disconnected");
                mStatusDrawable.resetTransition();
                mStatusCircle.setVisibility(View.VISIBLE);
                mStatusProgress.setVisibility(View.GONE);
                mStatusText.setText(R.string.disconnected);
                return true;
            case BTConnectionThread.BT_MESSAGE:
                String message = (String) msg.obj;
                if (mConn.getOnReadListener() != null) {
                    mConn.getOnReadListener().onRead(message);
                }
                Matcher m = mVarPattern.matcher(message);
                if (m.find()) {
                    mVarAdapter.add(m.group(1), m.group(3));
                } else {
                    mLogAdapter.add("> " + message);
                }
                if (mRecording) {
                    long now = new Date().getTime();
                    if (now >= mRecordLast+mRecordPeriod) {
                        mRecordLast = mRecordLast+mRecordPeriod;
                        mRecordOut.write(now+",");
                        for (int j = 0; j < mRecordHeader.length - 1; j++) {
                            mRecordOut.write(mVarAdapter.get(mRecordHeader[j])+",");
                        }
                        mRecordOut.write(mVarAdapter.get(mRecordHeader[mRecordHeader.length - 1])+"\n");
                    }
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onButtonStateChanged(int x, int y, int state) {
        mState.buttons[y*3+x] = state;
        mController.pushState(mState);
    }

    @Override
    public void onJoystickMoved(int x, int y) {
        mState.joystickX = x;
        mState.joystickY = y;
        mController.pushState(mState);
    }

    public static class LogAdapter extends BaseAdapter {
        private static final SimpleDateFormat format = new SimpleDateFormat("mm:ss.SSS");

        private final Context mContext;
        private String[] mLog;
        private int mCount = 0;
        private int mHead = 0;

        public LogAdapter(Context context, String[] storage) {
            mContext = context;
            mLog = storage;
        }
        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public Object getItem(int position) {
            return mLog[(mHead+position)%mLog.length];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = ((LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE))
                                    .inflate(R.layout.log_item, parent, false);
            }
            ((TextView) convertView).setText((String) getItem(position));
            return convertView;
        }


        public void add(String item) {
            item = format.format(new Date())+" "+item;
            mLog[(mHead+mCount)%mLog.length] = item;
            if (mCount < mLog.length) mCount++;
            notifyDataSetChanged();
        }

        public void clear() {
            mHead = mCount = 0;
            notifyDataSetChanged();
        }
    }

    public static class VarAdapter extends BaseAdapter {
        private final Context mContext;
        private HashMap<String, String> mMap = new HashMap<>();
        private ArrayList<String> mKeys = new ArrayList<>();

        public VarAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return mKeys.size();
        }

        @Override
        public Object getItem(int position) {
            return new Pair<>(mKeys.get(position), mMap.get(mKeys.get(position)));
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = ((LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.log_item, parent, false);
            }
            @SuppressWarnings("unchecked")
            Pair<String, String> item = (Pair<String, String>) getItem(position);
            ((TextView) convertView).setText(item.first + ":" + item.second);
            return convertView;
        }

        public void add(String key, String value) {
            if (mMap.put(key, value) == null) {
                mKeys.add(key);
            }
            notifyDataSetChanged();
        }

        public String[] keys() {
            return mKeys.toArray(new String[mKeys.size()]);
        }

        public String get(String key) {
            return mMap.get(key);
        }
    }
}
