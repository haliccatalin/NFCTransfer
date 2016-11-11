package ro.halic.catalin.nfctransfer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends Activity implements NfcAdapter.OnNdefPushCompleteCallback, NfcAdapter.CreateNdefMessageCallback{

    //variables
    private Context mContext;
    private NfcAdapter mNfcAdapter;
    private ArrayList<String> messageToSendList = new ArrayList<>();
    private ArrayList<String> messageReceivedList = new ArrayList<>();

    private final String TAG_MESSAGE_TO_SEND_LIST = "messageToSendList", TAG_MESSAGE_RECEIVED_LIST = "messageReceivedList";

    //UI
    private TextView textView_messageToSend, textView_messageReceived;
    private EditText editText_messageInput;
    private Button button_addMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        init();
    }

    private void init() {

        //init context
        mContext = MainActivity.this;

        //Check if device support NFC
       checkSupportNfc();

        textView_messageToSend = (TextView) findViewById(R.id.textView_messageToSend);
        textView_messageReceived = (TextView) findViewById(R.id.textView_messageReceived);
        editText_messageInput = (EditText) findViewById(R.id.editText_addMessage);
        button_addMessage = (Button) findViewById(R.id.button_addMessage);
        button_addMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMessage();
            }
        });

        updateUI();

        if(getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            handleNfcIntent(getIntent());
        }
    }

    private void checkSupportNfc() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(mContext);

        if(mNfcAdapter != null) {

            //this will call for creating message
            mNfcAdapter.setNdefPushMessageCallback(this, this);

            //this will call when the message was sent
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        } else {
            Toast.makeText(mContext, "This device not support NFC!", Toast.LENGTH_SHORT).show();
        }
    }

    /**Update the textViews*/
    private void updateUI() {

        //For Send messages
        textView_messageToSend.setText("Message to send:\n");

        if(messageToSendList.size() > 0) {
            for(String text : messageToSendList) {
                textView_messageToSend.append(text);
                textView_messageToSend.append("\n");
            }
        }

        //For Received messages
        textView_messageReceived.setText("Message received:\n");

        if(messageReceivedList.size() > 0) {
            for(String text : messageReceivedList) {
                textView_messageReceived.append(text);
                textView_messageReceived.append("\n");
            }
        }
    }

    private void addMessage() {
        String message = editText_messageInput.getText().toString();

        editText_messageInput.setText("");

        messageToSendList.add(message);

        updateUI();
    }

    private void handleNfcIntent(Intent intent) {
        
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            Parcelable[] receivedArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            
            if(receivedArray != null) {
                
                //clear received message list
                messageReceivedList.clear();
                
                //get first element from list
                NdefMessage message = (NdefMessage) receivedArray[0];
                
                //get records from message
                NdefRecord[] attachedRecord = message.getRecords();
                
                for(NdefRecord record : attachedRecord) {
                    
                    //convert bytes in string
                    String rec = new String(record.getPayload());
                    
                    //ignore the package name
                    if(rec.equals(getPackageName())) {
                        continue;
                    }
                    
                    messageReceivedList.add(rec);
                }

                Toast.makeText(mContext, "Received " + messageReceivedList.size() + " messages", Toast.LENGTH_SHORT).show();
                
                //update the UI with new messages
                updateUI();
            } else {
                Toast.makeText(mContext, "Received Blank Parcelable", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //store the list
        outState.putStringArrayList(TAG_MESSAGE_TO_SEND_LIST, messageToSendList);
        outState.putStringArrayList(TAG_MESSAGE_RECEIVED_LIST, messageReceivedList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        //get list values
        messageToSendList = savedInstanceState.getStringArrayList(TAG_MESSAGE_TO_SEND_LIST);
        messageReceivedList = savedInstanceState.getStringArrayList(TAG_MESSAGE_RECEIVED_LIST);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        handleNfcIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleNfcIntent(intent);
    }

    public NdefRecord[] createRecords() {

        NdefRecord[] records = new NdefRecord[messageToSendList.size() + 1];

        for(int i = 0; i < messageToSendList.size(); i++) {

            byte[] payload = messageToSendList.get(i).getBytes(Charset.forName("UTF-8"));

            NdefRecord rec = new NdefRecord(
                    NdefRecord.TNF_WELL_KNOWN,  //type name format
                    NdefRecord.RTD_TEXT,        //description of out payload
                    new byte[0],                //the optional id for record
                    payload                     //payload for record
            );

            records[i] = rec;
        }

        records[messageToSendList.size()] = NdefRecord.createApplicationRecord(getPackageName());

        return records;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        if(messageToSendList.size() == 0) {
            return null;
        }

        NdefRecord[] recordsToAttach = createRecords();

        return new NdefMessage(recordsToAttach);
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        messageToSendList.clear();
    }
}
