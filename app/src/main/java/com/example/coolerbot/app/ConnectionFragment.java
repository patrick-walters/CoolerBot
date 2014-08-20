package com.example.coolerbot.app;

import android.app.Activity;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ConnectionFragment extends ListFragment implements WifiP2pManager.PeerListListener,
        WifiP2pManager.ConnectionInfoListener, View.OnClickListener{

    private static final String ARG_SECTION_NUMBER = "section_number";

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private ProgressDialog progressDialog;
    private WifiP2pDevice localDevice;

    private Context context;

    private RemoteBroadcastReceiver receiver;
    private boolean isWifiP2pEnabled;

    //Intent filter used to receive intents from the android OS for the WIFI connections
    private final IntentFilter intentFilter = new IntentFilter();

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;

    //Create new instance for fragment.
    public static ConnectionFragment newInstance(int sectionNumber) {
        ConnectionFragment fragment = new ConnectionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ConnectionFragment() {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.context = activity;
        this.setListAdapter(new WifiPeerListAdapter(getActivity(),
                R.layout.device_information, peers));

        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // Manager and channel for interacting with WIFI
        mManager = (WifiP2pManager) activity.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(activity, Looper.getMainLooper(), null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Inflate information layout
        View rootView = inflater.inflate(R.layout.connection_fragment, container, false);

        Button discoverDevices = (Button) rootView.findViewById(R.id.discoverDevices);
        discoverDevices.setOnClickListener(this);
        Button enableWifi = (Button) rootView.findViewById(R.id.enableWifi);
        enableWifi.setOnClickListener(this);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new RemoteBroadcastReceiver(mManager, mChannel, this);
        context.registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        context.unregisterReceiver(receiver);
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        WifiP2pDevice device = peers.get(position);
        connectDevice(device);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.discoverDevices:
                if (!isWifiP2pEnabled) {
                    Toast.makeText(context, "Enable WIFI P2P", Toast.LENGTH_SHORT).show();
                    break;
                }
                onInitiateDiscovery();
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(context, "Discovery Initiated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(context, "Discovery Failed : " + reasonCode, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.enableWifi:
                if (mManager != null && mChannel != null) {
                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    //WifiP2pDevice device1 = new WifiP2pDevice();
                    //device1.deviceAddress = "127.0.0.1";
                    //device1.status = WifiP2pDevice.AVAILABLE;
                    //device1.deviceName = "Fo So";
                    //WifiP2pDevice device2 = new WifiP2pDevice();
                    //device2.deviceAddress = "127.0.0.2";
                    //device2.status = WifiP2pDevice.AVAILABLE;
                    //device2.deviceName = "Fo So 2";
                    //peers.add(device1);
                    //peers.add(device2);
                    //((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
                }
                else {
                    Log.e("WIFI", "Channel or manager is null");
                }
                break;
        }
    }

    private class WifiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

        private List<WifiP2pDevice> devices;

        public WifiPeerListAdapter(Context context, int resourceId,
                                   List<WifiP2pDevice> devices) {
            super(context, resourceId, devices);
            this.devices = devices;
        }

        @Override
        public View getView(int position, View view, ViewGroup container) {
            if (view == null) {
                LayoutInflater layoutInflater = (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(R.layout.device_information, null);
            }

            WifiP2pDevice device = devices.get(position);

            if (device != null) {
                TextView name = (TextView) view.findViewById(R.id.device_name);
                name.setText(device.deviceName);
                TextView details = (TextView) view.findViewById(R.id.device_details);
                details.setText(getDeviceStatus(device.status));
            }
            return view;
        }
    }

    public void connectDevice(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "WIFI P2P",
                "Connecting to :" + device.deviceAddress, true, true );

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(context, "Connect failed. Retry.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // Connected
        Toast.makeText(context,"Group Owner IP: " + wifiP2pInfo.groupOwnerAddress.getHostAddress(),
                Toast.LENGTH_SHORT).show();

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        //if (info.groupFormed && info.isGroupOwner) {
        //    new FileServerAsyncTask(getActivity(), mContentView.findViewById(R.id.status_text))
        //            .execute();
        //} else if (info.groupFormed) {
        //    // The other device acts as the client. In this case, we enable the
        //    // get file button.
        //    mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
        //    ((TextView) mContentView.findViewById(R.id.status_text)).setText(getResources()
        //            .getString(R.string.client_text));
        //}
    }


    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        peers.clear();
        peers.addAll(peerList.getDeviceList());

        ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Log.d("Device", "No devices found");
        }
    }

    public void updateLocalDevice(WifiP2pDevice device) {
        localDevice = device;
    }

    public WifiP2pDevice getLocalDevice() {
        return localDevice;
    }

    private static String getDeviceStatus(int deviceStatus) {
        Log.d("Device", "Peer status:" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }

    public void onInitiateDiscovery() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = ProgressDialog.show(context, "WIFI P2P",
                "Discovering Peers...", true, true);
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    public static class ServerAsyncTask extends AsyncTask<Void,Void,Void> {

        public ServerAsyncTask() {}

        @Override
        protected Void doInBackground(Void... Void) {

            return null;
        }
    }
}
