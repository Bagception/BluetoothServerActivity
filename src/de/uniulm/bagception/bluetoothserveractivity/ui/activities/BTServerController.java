package de.uniulm.bagception.bluetoothserveractivity.ui.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.philipphock.android.lib.broadcast.blutooth.BluetoothStateActor;
import de.philipphock.android.lib.broadcast.blutooth.BluetoothStateChangeReactor;
import de.philipphock.android.lib.logging.LOG;
import de.philipphock.android.lib.services.ServiceUtil;
import de.philipphock.android.lib.services.messenger.MessengerService;
import de.philipphock.android.lib.services.observation.ServiceObservationActor;
import de.philipphock.android.lib.services.observation.ServiceObservationReactor;
import de.uniulm.bagception.bluetoothserveractivity.R;
import de.uniulm.bagception.bluetoothservermessengercommunication.MessengerConstants;
import de.uniulm.bagception.broadcastconstants.BagceptionBroadcastContants;
import de.uniulm.bagception.services.ServiceNames;

public class BTServerController extends Activity implements
		ServiceObservationReactor, BluetoothStateChangeReactor {
	public final String TAG = getClass().getName();

	private ServiceObservationActor soActor;
	private BluetoothStateActor btStateActor;
	private boolean isConnectedWithService = false;
	private Messenger serviceMessenger; // send messages to the server

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_btserver_controller);
		soActor = new ServiceObservationActor(this,
				ServiceNames.BLUETOOTH_SERVER_SERVICE);
		btStateActor = new BluetoothStateActor(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		onServiceStopped(null);
		soActor.register(this);
		btStateActor.register(this);
		// onBluetoothEnabledChanged(BluetoothAdapter.getDefaultAdapter().isEnabled());
		ServiceUtil.requestStatusForServiceObservable(this,
				ServiceNames.BLUETOOTH_SERVER_SERVICE);
		btStateActor.refireBluetoothCallbacks();
		updateClientConnected(0);
		registerReceiver(serverStatusListener, new IntentFilter(
				BagceptionBroadcastContants.BROADCAST_CLIENTS_CONNECTION_UPDATE));
		sendUpdateServerInfoRequest();
	}

	private void updateClientConnected(int count) {
		TextView v = (TextView) findViewById(R.id.ssClcon);
		v.setText(""+count);
	}

	@Override
	protected void onPause() {
		super.onPause();
		soActor.unregister(this);
		btStateActor.unregister(this);
		unregisterReceiver(serverStatusListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.btserver_controller, menu);
		return true;
	}

	// UI Callbacks

	public void onSendBtn(View v) {
		EditText toSendTxt = (EditText) findViewById(R.id.toSendTxt);
		String txt = toSendTxt.getText().toString();
		Bundle b = new Bundle();
		b.putString("cmd", "msg");
		b.putString("payload", txt);
		Message m = Message.obtain(null,
				MessengerConstants.MESSAGE_BUNDLE_MESSAGE);
		m.setData(b);
		try {
			serviceMessenger.send(m);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	public void onStartStopServerClicket(View v) {
		if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			startActivityForResult(new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
			return;
		}
		startStopService();

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1 && resultCode == 1) {
			startStopService();
		}
	}

	@Override
	public void onServiceStarted(String serviceName) {
		TextView v = (TextView) findViewById(R.id.serverStatus);
		v.setTextColor(Color.GREEN);
		v.setText("online");

		Button startStopButton = (Button) findViewById(R.id.startStopBTServer);
		startStopButton.setText("stop server");

		bindService(new Intent(ServiceNames.BLUETOOTH_SERVER_SERVICE), sconn,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onServiceStopped(String serviceName) {

		TextView v = (TextView) findViewById(R.id.serverStatus);
		v.setTextColor(Color.RED);
		v.setText("offline");

		Button startStopButton = (Button) findViewById(R.id.startStopBTServer);
		startStopButton.setText("start server");
		doUnbindService();

	} 

	private void startStopService() {

		Intent i = new Intent(ServiceNames.BLUETOOTH_SERVER_SERVICE);
		ServiceUtil.logRunningServices(this, "SERVICES");
		if (ServiceUtil.isServiceRunning(this, ServiceNames.BLUETOOTH_SERVER_SERVICE)) {//TODO may not work
			doUnbindService();
			stopService(i);
		} else {
			startService(i);
		}

	}

	public void makeDiscoverable(View v) {
		Intent discoverableIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(
				BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
		startActivity(discoverableIntent);
	}

	@Override
	public void onBluetoothEnabledChanged(boolean isEnabled) {
		TextView bt = (TextView) findViewById(R.id.btStatus);
		bt.setText(isEnabled ? "enabled" : "disabled");
		bt.setTextColor(isEnabled ? Color.GREEN : Color.RED);

		if (!isEnabled) {
			Button startStopButton = (Button) findViewById(R.id.startStopBTServer);
			startStopButton.setText("enable bluetooth");
		}
	}

	// BT listener
	@Override
	public void onBluetoothTurningOn() {

	}

	@Override
	public void onBluetoothTurningOff() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBluetoothIsDiscoverable() {
		Button makeDiscoverableBtn = (Button) findViewById(R.id.discoverableBtn);
		makeDiscoverableBtn.setEnabled(false);
	}

	@Override
	public void onBluetoothIsConnectable() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBluetoothIsNotConnectableAndNotDiscoveralbe() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBluetoothIsNotDiscoveralbe() {

		Button makeDiscoverableBtn = (Button) findViewById(R.id.discoverableBtn);
		makeDiscoverableBtn.setEnabled(true);
	}

	private final BroadcastReceiver serverStatusListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("BT", "serverStatus recv");
			int cnt=intent.getIntExtra(BagceptionBroadcastContants.BROADCAST_CLIENTS_CONNECTION_UPDATE, 0);
			updateClientConnected(cnt);

		}
	};

	// IPC using messenger

	ServiceConnection sconn = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger = null;
			isConnectedWithService = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger = new Messenger(service);
			isConnectedWithService = true;

			// We want to monitor the service for as long as we are
			// connected to it.
			try {
				Message msg = Message.obtain(null,
						MessengerService.MSG_REGISTER_CLIENT);
				msg.replyTo = incomingMessenger;
				serviceMessenger.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	// ###### IPC ######\\
	private final Handler incomingHandler = new Handler(new Handler.Callback() {

		// Handles incoming messages
		@Override
		public boolean handleMessage(Message msg) {
			switch(msg.what){
			case MessengerConstants.MESSAGE_BUNDLE_MESSAGE:
				Log.d(TAG, "handle " + msg.getData().toString());
				for (String key:msg.getData().keySet()){
					LOG.out(key,msg.getData().get(key));
				}
				Toast.makeText(BTServerController.this, msg.getData().toString(),
						Toast.LENGTH_SHORT).show();

				break;
			}
				return false;
		}
	});

	// delivered to the server, handles incoming messages
	private final Messenger incomingMessenger = new Messenger(incomingHandler);

	public void doUnbindService() {
		if (isConnectedWithService) {
			// If we have received the service, and hence registered with
			// it, then now is the time to unregister.
			if (serviceMessenger != null) {
				try {
					Message msg = Message.obtain(null,
							MessengerService.MSG_UNREGISTER_CLIENT);
					msg.replyTo = incomingMessenger;
					serviceMessenger.send(msg);
				} catch (RemoteException e) {
					// There is nothing special we need to do if the service
					// has crashed.
				}
			}

			// Detach our existing connection.
			unbindService(sconn);
			isConnectedWithService = false;

		}
	}

	
	private void sendUpdateServerInfoRequest(){
		
		Intent br = new Intent();
		br.setAction(BagceptionBroadcastContants.BROADCAST_CLIENTS_CONNECTION_UPDATE_REQUEST);
		sendBroadcast(br);
	}
	
	// ###### /IPC ######\\

}
