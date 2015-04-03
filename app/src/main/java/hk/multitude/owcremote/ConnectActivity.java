package hk.multitude.owcremote;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;


public class ConnectActivity extends ActionBarActivity {
    public static final String EXTRA_DEVICE = "hk.multitude.owcremote.intent.extra.device";

    private ListView mDeviceList;
    private ProgressBar mProgressBar = null;

    private BluetoothAdapter mBt;
    private boolean mBtEnabled = false;
    private boolean mBtRequested = false;

    private static final int BT_ENABLE = 0;
    private DeviceListAdapter mDeviceListAdapter;
    private AdapterView.OnItemClickListener mDeviceListClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = (BluetoothDevice) mDeviceListAdapter.getItem(position);
            Intent i = new Intent(ConnectActivity.this, ControlActivity.class);
            i.putExtra(EXTRA_DEVICE, device);
            startActivity(i);
        }
    };
    private BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mDeviceListAdapter.add(device);
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                mBtEnabled = (state == BluetoothAdapter.STATE_ON);
                if (mBtEnabled) {
                    onBluetoothChanged();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        setTitle(R.string.select_device);

        mBt = BluetoothAdapter.getDefaultAdapter();
        if (mBt == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Bluetooth unavailable")
                    .setMessage("Remote control requires Bluetooth connection!")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    }).show();
            return;
        }

        mDeviceList = (ListView) findViewById(R.id.deviceList);
        mDeviceListAdapter = new DeviceListAdapter(this, mBt.getBondedDevices());
        mDeviceList.setAdapter(mDeviceListAdapter);
        mDeviceList.setOnItemClickListener(mDeviceListClick);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBtReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBtReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mBt != null) {
            mBtEnabled = mBt.isEnabled();
            if (!mBtEnabled && !mBtRequested) {
                requestBluetooth();
                mBtRequested = true; // Just ask once
            } else {
                onBluetoothChanged();
            }
        }
    }

    @Override
    protected void onStop() {
        if (mBt != null) {
            mBt.cancelDiscovery();
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_ENABLE:
                if (resultCode == RESULT_OK) {
                    onBluetoothChanged();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Bluetooth is disabled")
                            .setMessage("Remote control requires Bluetooth connection!")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mProgressBar = (ProgressBar) menu.findItem(R.id.ab_progress).getActionView()
                                        .findViewById(R.id.progress);
        mProgressBar.setVisibility((mBt != null && mBt.isDiscovering())?View.VISIBLE:View.GONE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_scan:
                if (!mBtEnabled)
                    requestBluetooth();
                onBluetoothChanged();
                break;
            case R.id.action_about:
                new AlertDialog.Builder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override public void onClick(DialogInterface dialog, int which) { }
                    })
                    .show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onBluetoothChanged() {
        if (mBtEnabled) {
            mDeviceListAdapter = new DeviceListAdapter(this, mBt.getBondedDevices());
            mDeviceList.setAdapter(mDeviceListAdapter);
            mBt.startDiscovery();
        } else {
            mDeviceList.setAdapter(null);
        }
    }

    private void requestBluetooth() {
        Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(i, BT_ENABLE);
    }

    public static class DeviceListAdapter extends BaseAdapter {

        private final Context mContext;
        private HashMap<String, BluetoothDevice> mMap = new HashMap<>();
        private ArrayList<String> mKeys = new ArrayList<>();

        public DeviceListAdapter(Context context, Set<BluetoothDevice> items) {
            mContext = context;
            if (items != null) {
                for (BluetoothDevice i : items) {
                    mMap.put(i.getAddress(), i);
                    mKeys.add(i.getAddress());
                }
            }
        }

        @Override
        public int getCount() {
            return mKeys.size();
        }

        @Override
        public Object getItem(int position) {
            return mMap.get(mKeys.get(position));
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater li = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = li.inflate(R.layout.device_item, parent, false);
            }
            BluetoothDevice item = (BluetoothDevice) getItem(position);
            ((TextView) convertView.findViewById(R.id.title)).setText(item.getName());
            ((TextView) convertView.findViewById(R.id.description)).setText(item.getAddress());
            return convertView;
        }

        public void add(BluetoothDevice device) {
            if (mMap.put(device.getAddress(), device) == null) {
                mKeys.add(device.getAddress());
            }
            notifyDataSetChanged();
        }
    }
}
