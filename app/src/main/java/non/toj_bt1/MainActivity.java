package non.toj_bt1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TabHost;
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;

public class MainActivity extends Activity implements OnClickListener, OnItemSelectedListener, OnCheckedChangeListener, NumberPicker.OnScrollListener, View.OnTouchListener {
    private static final String TAG = "toj_bt1";

    TextView txtArduino;
    ToggleButton checkTBtn;
    Button btnOn,btnOff,btnR1,btnR2,btnR3,btnR4,btnCncl,readDots,applyDots,btnReset;
    NumberPicker NumberPicker1,NumberPicker2,NumberPicker3,NumberPicker4;
    NumberPicker NumberPicker5,NumberPicker6,NumberPicker7,NumberPicker8;
    TabHost tabHost;
    Spinner coolTime,delayTime;
    Handler h;
    Integer FirstLaunch = 2;  //костыль, чтоб при запуске приложения не отсылал coolTime,delayTime;
    float dwnx,upx;
    LinearLayout dtTxt8,dtTxt7,dtTxt6,dtTxt5,dtTxt4,dtTxt3,dtTxt2,dtTxt1;

    private static final int REQUEST_ENABLE_BT = 1;
    final int RECIEVE_MESSAGE = 1;        // Статус для Handler
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID сервиса
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-адрес Bluetooth модуля
    private static String address = "20:16:04:12:23:70";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // получаем локальный Bluetooth адаптер
        checkBTState();

        setContentView(R.layout.activity_main);

        tabHost = (TabHost) findViewById(android.R.id.tabhost);
        // инициализация
        tabHost.setup();
        TabHost.TabSpec tabSpec;

        // создаем вкладку и указываем тег
        tabSpec = tabHost.newTabSpec("tag1");
        // название вкладки
        tabSpec.setIndicator("Основное меню");
        // указываем id компонента из FrameLayout, он и станет содержимым
        tabSpec.setContent(R.id.tab1);
        // добавляем в корневой элемент
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("tag2");
        // указываем название и картинку
        // в нашем случае вместо картинки идет xml-файл,
        // который определяет картинку по состоянию вкладки
        tabSpec.setIndicator("Ручное управление");
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);

        //  вкладка будет выбрана по умолчанию
        tabHost.setCurrentTabByTag("tag2");
        // обработчик переключения вкладок
        /*tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                //Toast.makeText(getBaseContext(), "tabId = " + tabId, Toast.LENGTH_SHORT).show();
            }
        });*/
        tabHost.setOnTouchListener(this);

        //числовые
        MakeNP();
        //кнопки
        MakeButtons();
        //спинеры
        coolTime = (Spinner) findViewById(R.id.coolTime);
        coolTime.setEnabled(false);
        delayTime = (Spinner) findViewById(R.id.delayTime);
        delayTime.setEnabled(false);
        coolTime.setOnItemSelectedListener(this);
        delayTime.setOnItemSelectedListener(this);
        //тугл кнопки
        checkTBtn = (ToggleButton) findViewById(R.id.checkTBtn);
        checkTBtn.setEnabled(false);
        checkTBtn.setOnCheckedChangeListener(this);
        //текстовые
        txtArduino = (TextView) findViewById(R.id.txtArduino);      // для вывода текста, полученного от Arduino

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECIEVE_MESSAGE:                                                   // если приняли сообщение в Handler
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        sb.append(strIncom);                                                // формируем строку
                        int endOfLineIndex = sb.indexOf("\r\n");

                        if ( endOfLineIndex > 0 ) {                                            // если встречаем конец строки,
                            String sbprint = sb.substring(0, endOfLineIndex);               // то извлекаем строку
                            sb.delete(0, sb.length());                                      // и очищаем sb
                            txtArduino.setText(sbprint);             // обновляем TextView
                            Log.d(TAG, "...Arduino шлёт:" + sbprint);
                            FlashtempDots(sbprint); //считываем точки
                            FlashCheck(sbprint); // считываем проверку при включении
                            FlashCT(sbprint); // время проверки
                            FlashDT(sbprint); // время опроса
                            GetManual(sbprint); // какой мануал включился
                            GetTempState(sbprint); //показывает похождение точек через индикаторы
                        }
                        //Log.d(TAG, "...Строка:"+ sb.toString() +  "Байт:" + msg.arg1 + "...");
                        break;
                }
            }
        };

//    mConnectedThread.write("flash");
    }

    private void GetTempState(String sbprint) {
        //toj_temp_377
        //123456789012
        if ((sbprint.length() == 12) && sbprint.substring(0,9).equals("toj_temp_")) {
            int tempVal = Integer.valueOf(sbprint.substring(9,12));
            setStatedtTxt(tempVal, 0, NumberPicker1.getValue(), dtTxt1, null);
            setStatedtTxt(tempVal, NumberPicker2.getValue(), NumberPicker3.getValue(), dtTxt2, dtTxt3);
            setStatedtTxt(tempVal, NumberPicker4.getValue(), NumberPicker5.getValue(), dtTxt4, dtTxt5);
            setStatedtTxt(tempVal, NumberPicker6.getValue(), NumberPicker7.getValue(), dtTxt6, dtTxt7);
            setStatedtTxt(tempVal, NumberPicker8.getValue(), 0 , dtTxt8, null);
        }
    }

    private void setStatedtTxt(int val, int numPick1, int numPick2, LinearLayout dtTxt01 , LinearLayout dtTxt02) {
        //toj_temp_377
        //123456789012
        if (numPick1 > val && val > numPick2) {
            dtTxt01.setBackgroundColor(getColor(android.R.color.holo_green_light));
            try {
              dtTxt02.setBackgroundColor(getColor(android.R.color.holo_green_light));
            }
            catch(Exception ex) {}
          }
        else {
            dtTxt01.setBackgroundColor(getColor(android.R.color.holo_red_light));
            dtTxt02.setBackgroundColor(getColor(android.R.color.holo_red_light));
          }
    }

    public void GetManual(String sbprint) {
        //I get->manualx
        //I get->off
        //I get->on
        //I get->no
        //12345678901234
        if ((sbprint.length() == 14 ) && sbprint.substring(0,13).equals("I get->manual"))
            switch (Integer.valueOf(sbprint.substring(13,14))) {
                case 1:
                    setBtnsNoGreen(btnR1);
                    break;
                case 2:
                    setBtnsNoGreen(btnR2);
                    break;
                case 3:
                    setBtnsNoGreen(btnR3);
                    break;
                case 4:
                    setBtnsNoGreen(btnR4);
                    break;
                default:
                    break;
            }
        if ((sbprint.length() >= 9 ) && sbprint.substring(0,7).equals("I get->"))
            switch (sbprint.substring(7,9)) {
                case "on":
                    setBtnsNoGreen(btnOn);
                    break;
                case "of":
                    setBtnsNoGreen(btnOff);
                    break;
                case "no":
                    setBtnsNoGreen(null); //при нажати отмена
                    break;
                case "re":
                    setBtnsNoGreen(null); // при нажати ресет
                    break;
                default:
                    break;
            }
    }

    private void MakeButtons() {
        btnOn = (Button) findViewById(R.id.btnOn);
        btnOff = (Button) findViewById(R.id.btnOff);
        btnR1 = (Button) findViewById(R.id.btnR1);
        btnR2 = (Button) findViewById(R.id.btnR2);
        btnR3 = (Button) findViewById(R.id.btnR3);
        btnR4 = (Button) findViewById(R.id.btnR4);
        btnCncl = (Button) findViewById(R.id.btnCncl);
        readDots = (Button) findViewById(R.id.readDots);
        applyDots = (Button) findViewById(R.id.applyDots);
        applyDots.setEnabled(false);
        btnReset = (Button) findViewById(R.id.btnReset);

        btnOn.setOnClickListener(this);
        btnOff.setOnClickListener(this);
        btnR1.setOnClickListener(this);
        btnR2.setOnClickListener(this);
        btnR3.setOnClickListener(this);
        btnR4.setOnClickListener(this);
        btnCncl.setOnClickListener(this);
        readDots.setOnClickListener(this);
        applyDots.setOnClickListener(this);
        btnReset.setOnClickListener(this);
    }

    private void MakeNP() {
        NumberPicker1 = (NumberPicker) findViewById(R.id.numberPicker1);
        NumberPicker1 = setNumP(NumberPicker1);
        NumberPicker2 = (NumberPicker) findViewById(R.id.numberPicker2);
        NumberPicker2 = setNumP(NumberPicker2);
        NumberPicker3 = (NumberPicker) findViewById(R.id.numberPicker3);
        NumberPicker3 = setNumP(NumberPicker3);
        NumberPicker4 = (NumberPicker) findViewById(R.id.numberPicker4);
        NumberPicker4 = setNumP(NumberPicker4);
        NumberPicker5 = (NumberPicker) findViewById(R.id.numberPicker5);
        NumberPicker5 = setNumP(NumberPicker5);
        NumberPicker6 = (NumberPicker) findViewById(R.id.numberPicker6);
        NumberPicker6 = setNumP(NumberPicker6);
        NumberPicker7 = (NumberPicker) findViewById(R.id.numberPicker7);
        NumberPicker7 = setNumP(NumberPicker7);
        NumberPicker8 = (NumberPicker) findViewById(R.id.numberPicker8);
        NumberPicker8 = setNumP(NumberPicker8);

        NumberPicker2.setOnScrollListener(this);
        NumberPicker3.setOnScrollListener(this);
        NumberPicker4.setOnScrollListener(this);
        NumberPicker5.setOnScrollListener(this);
        NumberPicker6.setOnScrollListener(this);
        NumberPicker7.setOnScrollListener(this);
        NumberPicker8.setOnScrollListener(this);

        dtTxt1 = (LinearLayout) findViewById(R.id.dtTxt1);
        dtTxt2 = (LinearLayout) findViewById(R.id.dtTxt2);
        dtTxt3 = (LinearLayout) findViewById(R.id.dtTxt3);
        dtTxt4 = (LinearLayout) findViewById(R.id.dtTxt4);
        dtTxt5 = (LinearLayout) findViewById(R.id.dtTxt5);
        dtTxt6 = (LinearLayout) findViewById(R.id.dtTxt6);
        dtTxt7 = (LinearLayout) findViewById(R.id.dtTxt7);
        dtTxt8 = (LinearLayout) findViewById(R.id.dtTxt8);
    }

    private void FlashDT(String message) {
        //String tmpstr;
        //delaytime -> x
        //123456789012345678
        if (message.length() == 14 && message.substring(0,13).equals("delaytime -> ")) {
            try {
                /*int dot = Integer.valueOf(message.substring(13, 14));
                dot -= 1;*/
                delayTime.setSelection(Integer.valueOf(message.substring(13, 14)));
                delayTime.setEnabled(true);
            } catch (NumberFormatException ex) {
                System.err.println("Неверный формат строки!");
                }
        }
    }

    public void FlashCT(String message) {
        //надо исправить
        //cooltime -> x
        //12345678901234567
        if (message.length() == 13 && message.substring(0, 12).equals("cooltime -> ")) {
            try {
                /*int dot = Integer.valueOf(message.substring(13, 14));
                dot -= 1;*/
                coolTime.setSelection(Integer.valueOf(message.substring(12, 13)) - 1);
                coolTime.setEnabled(true);
            } catch (NumberFormatException ex) {
                System.err.println("Неверный формат строки!");
            }
        }
    }

    public void FlashCheck(String message) {
        //Check in start -> x
        //1234567890123456789
        if (message.length() == 19 && message.substring(0,18).equals("Check in start -> ")) {
            checkTBtn.setEnabled(true);
            switch (message.substring(18,19)) {
                case "1":
                    checkTBtn.setChecked(true);
                    break;
                case "0":
                    checkTBtn.setChecked(false);
                    break;
                default:
                    break;
            }
        }
    }

    public void FlashtempDots(String message) {
        String tmpstr;
        if (message.length() >= 9 && message.substring(0,5).equals("temp_")) {
            //temp_i_xxx
            //1234567890
            //tmpstr = message.substring(0, 5);
            applyDots.setEnabled(true);
            try {
                tmpstr = message.substring(7, 9);
                if (message.length() == 10)
                    tmpstr = tmpstr + message.substring(9, 10);
                Integer tmpdot = Integer.valueOf(tmpstr);
                switch (Integer.valueOf(message.substring(5,6))) {
                    case 1:
                        NumberPicker1.setValue(tmpdot);
                        NumberPicker1.setEnabled(true);
                        break;
                    case 2:
                        NumberPicker2.setValue(tmpdot);
                        NumberPicker2.setEnabled(true);
                        break;
                    case 3:
                        NumberPicker3.setValue(tmpdot);
                        NumberPicker3.setEnabled(true);
                        break;
                    case 4:
                        NumberPicker4.setValue(tmpdot);
                        NumberPicker4.setEnabled(true);
                        break;
                    case 5:
                        NumberPicker5.setValue(tmpdot);
                        NumberPicker5.setEnabled(true);
                        break;
                    case 6:
                        NumberPicker6.setValue(tmpdot);
                        NumberPicker6.setEnabled(true);
                        break;
                    case 7:
                        NumberPicker7.setValue(tmpdot);
                        NumberPicker7.setEnabled(true);
                        break;
                    case 8:
                        NumberPicker8.setValue(tmpdot);
                        NumberPicker8.setEnabled(true);
                        break;
                    default:
                        break;
                }
            } catch (NumberFormatException ex) {
                System.err.println("Неверный формат строки!");
                }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnOn:
                mConnectedThread.write("on\n");
                break;
            case R.id.btnOff:
                mConnectedThread.write("off\n");
                break;
            case R.id.btnR1:
                mConnectedThread.write("manual1\n");
                break;
            case R.id.btnR2:
                mConnectedThread.write("manual2\n");
                break;
            case R.id.btnR3:
                mConnectedThread.write("manual3\n");
                break;
            case R.id.btnR4:
                mConnectedThread.write("manual4\n");
                break;
            //применить точки
            case R.id.applyDots:
                if (NumberPicker1.isEnabled())
                    mConnectedThread.write("tmp" + 1 + NumberPicker1.getValue() + "\n");
                if (NumberPicker2.isEnabled())
                    mConnectedThread.write("tmp" + 2 + calcNPvalue(NumberPicker2.getValue()) + "\n");
                if (NumberPicker3.isEnabled())
                    mConnectedThread.write("tmp" + 3 + calcNPvalue(NumberPicker3.getValue()) + "\n");
                if (NumberPicker4.isEnabled())
                    mConnectedThread.write("tmp" + 4 + calcNPvalue(NumberPicker4.getValue()) + "\n");
                if (NumberPicker5.isEnabled())
                    mConnectedThread.write("tmp" + 5 + calcNPvalue(NumberPicker5.getValue()) + "\n");
                if (NumberPicker6.isEnabled())
                    mConnectedThread.write("tmp" + 6 + calcNPvalue(NumberPicker6.getValue()) + "\n");
                if (NumberPicker7.isEnabled())
                    mConnectedThread.write("tmp" + 7 + calcNPvalue(NumberPicker7.getValue()) + "\n");
                if (NumberPicker8.isEnabled())
                    mConnectedThread.write("tmp" + 8 + calcNPvalue(NumberPicker8.getValue()) + "\n");
                break;
            //считать точки
            case R.id.readDots:
                mConnectedThread.write("flash\n");
                break;
            //отменить ручное управление
            case R.id.btnCncl:
                mConnectedThread.write("no\n");
                break;
            case R.id.btnReset:
                mConnectedThread.write("reset\n");
                break;
            default:
                break;
        }
    }

    public void setBtnsNoGreen(Button b) {
        // 17170443 - white
        // 17170452 - holo_green_light
        btnOn.setTextColor(getColor(android.R.color.white));
        btnOff.setTextColor(getColor(android.R.color.white));
        btnR1.setTextColor(getColor(android.R.color.white));
        btnR2.setTextColor(getColor(android.R.color.white));
        btnR3.setTextColor(getColor(android.R.color.white));
        btnR4.setTextColor(getColor(android.R.color.white));
        if (b != null)
            b.setTextColor(getColor(android.R.color.holo_green_light));
            //setTextColor(17170452);
    }

    public String calcNPvalue(int value) {
        String tmp = "";
        try {
            int xxx = value / 100;
            int xx = value / 10 - xxx * 10;
            int x = value % 10;
            tmp += Integer.toString(xxx);
            tmp += Integer.toString(xx);
            tmp += Integer.toString(x);
        } catch (NumberFormatException e) {
            System.err.println("calcNPvalue Неверный формат строки!");
        }
        return tmp;
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - попытка соединения...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Соединяемся...");
        Toast.makeText(getApplicationContext(),"Соединяемся", Toast.LENGTH_SHORT).show();

        try {
            btSocket.connect();
                Log.d(TAG, "...Соединение установлено и готово к передачи данных...");
                Toast.makeText(getApplicationContext(), "Соединение установлено", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Создание Socket...");

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
        Log.d(TAG, "...Socket Создан...");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "...In onPause()...");
        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth не поддерживается");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth включен...");
                Toast.makeText(getApplicationContext(),"Bluetooth включен", Toast.LENGTH_SHORT).show();
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        //position ++;
        if (FirstLaunch == 0) {
            switch (parent.getId()) {
                case R.id.coolTime:
                    mConnectedThread.write("cooltime" + position + "\n");
                    break;
                case R.id.delayTime:
                    mConnectedThread.write("delaytime" + position + "\n");
                    break;
                default:
                    break;
            }
        }
        else FirstLaunch -= 1;
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override
    public void onCheckedChanged(CompoundButton tglBtn, boolean Checked) {
        switch (tglBtn.getId()) {
            case R.id.checkTBtn:
                mConnectedThread.write("Check\n");
                break;
            default:
                break;
        }
    }

    @Override
    public void onScrollStateChange(NumberPicker numberPicker, int newV) {
    }

    //настройка характеристик
    public NumberPicker setNumP(NumberPicker Num) {
        Num.setMaxValue(115);
        Num.setMinValue(25);
        Num.setWrapSelectorWheel(false);
        Num.setEnabled(false);
        return Num;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: // нажатие
                dwnx = event.getX();
                break;
            /*case MotionEvent.ACTION_MOVE: // движение
                break;*/
            case MotionEvent.ACTION_UP: // отпускание
                upx = event.getX();
                break;
            /*case MotionEvent.ACTION_CANCEL:
                break;*/
        }
        if ( (dwnx - upx) >= 200 )
            tabHost.setCurrentTabByTag("tag2");
        if ( (dwnx - upx) <= -200 )
            tabHost.setCurrentTabByTag("tag1");
        return true;
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {}

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Получаем кол-во байт и само собщение в байтовый массив "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();     // Отправляем в очередь сообщений Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        private void write(String message) {
            Log.d(TAG, "...Данные для отправки: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Ошибка отправки данных: " + e.getMessage() + "...");
                Toast.makeText(getApplicationContext(),"Не удалось отправить сообщение", Toast.LENGTH_SHORT).show();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
