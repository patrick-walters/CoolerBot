package com.example.coolerbot.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;

public class RemoteBroadcastReceiver extends BroadcastReceiver{

    private WifiP2pManager mManager;
    private Channel mChannel;
    private ConnectionFragment mFragment;

    public RemoteBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                   ConnectionFragment fragment) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mFragment = fragment;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Determine if Wifi P2P mode is enabled or not, alert
            // the Activity.
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                mFragment.setIsWifiP2pEnabled(true);
            } else {
                mFragment.setIsWifiP2pEnabled(false);
            }
        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Request available peers from the WIFI P2P manager. This is an asynchronous call and
            // the calling activity is notified with a callback an PeerListListener.onPeersAvailable
            if (mManager != null) {
                mManager.requestPeers(mChannel, mFragment);
            }
            Log.d("WIFI","Peers changed");
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = intent .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                // We are connected with the other device, request connection
                // info to find group owner IP

               mManager.requestConnectionInfo(mChannel, mFragment);
            }
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            mFragment.updateLocalDevice((WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

        }
    }
}
