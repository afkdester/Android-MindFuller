package org.rmit.mindfulapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

import static android.provider.Telephony.Mms.Part.TEXT;

public class MainActivity extends AppCompatActivity{
    private static final String TAG = "TEST TAG";
    ArrayList<Excercise> todoArray;
    ListView todoListView;
    private TextView totalTimer;
    private long totalTimeNum;
    private boolean firstTime = Boolean.parseBoolean(null);
    private long miliSecReturn;


    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(isFirstTime()){
            totalTimeNum = 0;
            miliSecReturn = 0;
            permissionValidator();
        }
        else{
            permissionValidator();
            totalTimeNum = retrieveTotalTime();
        }
        // CHECK AND ASK FOR PERMISSION

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        todoListView = findViewById(R.id.todoListView);
        totalTimer = findViewById(R.id.totalTimer);
        //Initiate Json Tool for file reading
        JsonTool jsonTool = new JsonTool();
        jsonTool.context = MainActivity.this;
        todoArray = jsonTool.readFile();
        totalTimer.setText("Total Time You Have Been MindFull:" + "\n" + getTime(totalTimeNum));


        // SET UP ARRAY WITH ITEM IN IT //
        if(todoArray.size() == 0){
            addExcercise("Mindfulness Breathing",15);
            addExcercise("BedTime Retrospection",15);
            final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Default Excercise Have Been added");
            alertDialog.setMessage("Excercises have been added for u");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OKAY", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.dismiss();
                    displayExcercise();
                }
            });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NAH", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    todoArray.clear();
                    alertDialog.dismiss();
                    displayExcercise();
                }
            });
            alertDialog.show();
        }
        repeat4AM();
        displayExcercise();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(MainActivity.this,"Pause",Toast.LENGTH_LONG).show();
        repeat4AM();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(MainActivity.this,"Resume",Toast.LENGTH_LONG).show();
        totalTimer.setText("Total Time You Have Been MindFull:" + "\n" + getTime(totalTimeNum));
        repeat4AM();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        JsonTool jsonTool = new JsonTool();
        jsonTool.arrayList = todoArray;
        ArrayList<JSONObject>savedArray = new ArrayList<>();
        if(permissionValidatorwithBoolean()){
            try {
                Writer output;
                File emptyFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"MindFuller.txt");
                emptyFile.delete();
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"MindFuller.txt");
                output = new BufferedWriter(new FileWriter(String.valueOf(file)));
                for (Excercise excerise:jsonTool.arrayList){
                    output.write(jsonTool.saveFile(excerise.id,excerise.sname,excerise.duration,excerise.status).toString() + "\n");
                    Toast.makeText(MainActivity.this,"Confirm",Toast.LENGTH_LONG).show();
                }
                output.close();
                Log.d(TAG, savedArray.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            finish();
        }
        saveTotalTime();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Toast.makeText(MainActivity.this,"Restart",Toast.LENGTH_LONG).show();
        repeat4AM();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Toast.makeText(MainActivity.this,"Start",Toast.LENGTH_LONG).show();
        repeat4AM();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode == RESULT_OK){
            if(requestCode == 1){
                Integer excerStatusID = (Integer) Objects.requireNonNull(data.getExtras()).get("excerIdReturn");
                Long getMilisec = (Long) Objects.requireNonNull(data.getExtras()).get("statusReturn");
                for (Excercise excercise:todoArray) {
                    if(excercise.getId() == excerStatusID){
                        excercise.setStatus("DONE");
                        totalTimeNum += getMilisec;
                        Log.d(TAG, "onActivityResult: " + totalTimeNum);
                        displayExcercise();
                    }
                }
            }else if(requestCode == 2){
                String returnName = (String) data.getExtras().get("returnName");
                Integer returnDuration = (Integer) Objects.requireNonNull(data.getExtras()).get("returnDuration");
                addExcercise(returnName,returnDuration);
                if(!permissionValidatorwithBoolean()){
                    Toast.makeText(this,"PLEASE APPROVE THE FOLLOWING",Toast.LENGTH_LONG);
                    permissionValidator();
                }
            }else if(requestCode == 3){
                Integer recievedEditID = (Integer) Objects.requireNonNull(data.getExtras()).get("returnID");
                for (Excercise excercise:todoArray){
                    if(excercise.id == recievedEditID){
                        excercise.setSname(data.getExtras().get("returnName").toString());
                        excercise.setDuration((Integer)data.getExtras().get("returnDuration"));
                    }
                }
                displayExcercise();
            }
        }else if(resultCode == RESULT_CANCELED){
            if(requestCode == 1){
                try {
                    Long getMilisec = (Long) Objects.requireNonNull(data.getExtras()).get("statusReturn");
                    miliSecReturn = getMilisec;
                    totalTimeNum += miliSecReturn;
                    Log.d(TAG, "onActivityResult: SUCCESS");
                }catch (Exception e){
                    Log.d(TAG, "onActivityResult: " + "failed");
                    miliSecReturn = 0;
                }

            }
        }
    }

    //EXTRA FUNCTIONS FOR THE APPLICATION
    public void timerView(View view, ArrayList<Excercise> arrayList, Integer position) {
        Intent intent = new Intent(this,TimerActivity.class);
        intent.putExtra("duration",arrayList.get(position).duration);
        intent.putExtra("excerID", arrayList.get(position).id);
        intent.putExtra("excerciseName", arrayList.get(position).sname);
        startActivityForResult(intent,1);
    }

    public void displayExcercise(){
        CustomListView customListView = new CustomListView(this, todoArray);
        todoListView.setAdapter(customListView);
        todoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                timerView(view,todoArray,position);
            }
        });
        todoListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                ShowUpMenuActivity showUpMenuActivity = new ShowUpMenuActivity(MainActivity.this,position);
                showUpMenuActivity.showPopup(view);
                return true;
            }
        });
    }

    public void buttonReader(View view){
        switch (view.getId()){
            case R.id.addExcercise:
                Intent intent = new Intent(this,AddEditActivity.class);
                intent.putExtra("requestType","add");
                startActivityForResult(intent,2);
        }
    }

    public void addExcercise(String name, int duration){
        Integer idIndicator = 0;
        for (Excercise excercise:todoArray){
            if(excercise.id > idIndicator){
                idIndicator = excercise.id;
            }
        }
        idIndicator += 1;
        Excercise excercise = new Excercise(idIndicator, name,duration,"NEW");
        todoArray.add(excercise);
        displayExcercise();
    }

    public void deleteExcercise(int id){
        todoArray.remove(id);
    }

    public void editExcercise(int id){
        Intent intent = new Intent(this,AddEditActivity.class);
        intent.putExtra("sentName",todoArray.get(id).sname);
        intent.putExtra("sentDuration",todoArray.get(id).duration);
        intent.putExtra("sentID",todoArray.get(id).id);
        intent.putExtra("requestType","edit");
        startActivityForResult(intent,3);
    }

    public String getTime(long inputTime){
        inputTime += miliSecReturn;
        totalTimeNum += miliSecReturn;
        Log.d(TAG, "getTimeInput: " + inputTime);
        int week = (int) (inputTime / (1000*60*60*24*7));
        int day = (int) (inputTime / (1000*60*60*24)) % 7;
        int hour = (int) (inputTime  / (1000*60*60)) % 24;
        int minute = (int) ((inputTime / (1000*60)) % 60);
        int second = (int) (inputTime / 1000 % 60);
        String timeFormat = String.format(Locale.getDefault()," %02d weeks: %02d days \n %02d hours: %02d minutes: %02d seconds",week,day,hour,minute,second);
        saveTotalTime();
        return timeFormat;
    }

    //Save TotalTimer Everytime
    public void saveTotalTime(){
        SharedPreferences sharedPreferences = getSharedPreferences("totalTime",Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TEXT,Long.toString(totalTimeNum));
        Log.d(TAG, "saveTotalTime: " + totalTimeNum);
        editor.apply();
    }

    //Retrieve the timeNum
    public long retrieveTotalTime(){
        SharedPreferences sharedPreferences = getSharedPreferences("totalTime",Context.MODE_PRIVATE);
        String totalNum = sharedPreferences.getString(TEXT,"");
        Log.d(TAG, "retrieveTotalTime: " + totalNum);
        return Long.parseLong(totalNum);
    }

    // VALIDATE APP'S FIRST LAUNCH
    private boolean isFirstTime(){
        if(firstTime == Boolean.parseBoolean(null)){
            SharedPreferences mPreferences = this.getSharedPreferences("first_time", Context.MODE_PRIVATE);
            firstTime = mPreferences.getBoolean("firstTime", true);
            if (firstTime) {
                SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean("firstTime", false);
                editor.apply();
            }
        }
        return firstTime;
    }

    // CLASS FOR CREATION OF POPUP MENU
    public class ShowUpMenuActivity implements PopupMenu.OnMenuItemClickListener {

        private Activity context;
        private Integer excerNum;

        ShowUpMenuActivity(Activity context, Integer excerNum) {
            this.context = context;
            this.excerNum = excerNum;
        }

        void showPopup(View v){
            PopupMenu popupMenu = new PopupMenu(context,v);
            popupMenu.setOnMenuItemClickListener(this);
            popupMenu.inflate(R.menu.popup_layout);
            popupMenu.show();
        }

        @Override public boolean onMenuItemClick(MenuItem item){
            switch (item.getItemId()){
                case R.id.delete:
                    deleteExcercise(excerNum);
                    Toast.makeText(context,"item has been deleted",Toast.LENGTH_LONG).show();
                    displayExcercise();
                    break;
                case R.id.edit:
                    editExcercise(excerNum);
                    break;
            }
            return false;
        }
    }

    //Todo: TRY TO WORK OUT THE 4AM TIMER
    public void checkDefaultExcercise(){
        if(todoArray.size() == 0){
            addExcercise("Mindfulness breathing",15);
            addExcercise("BedTime retrospection",15);
            Toast.makeText(this,"Default Excercise Have been added",Toast.LENGTH_SHORT).show();
        }else{
            for (Excercise excercise:todoArray){
                if(excercise.sname.matches("Mindfulness breathing") && excercise.status.matches("DONE") && excercise.sname.matches("BedTime retrospection") && excercise.status.matches("DONE")){
                    excercise.setStatus("NEW");
                }
            }
        }
    }

    public void repeat4AM(){
        Calendar calendar = Calendar.getInstance();

        //SET TIME TO REPEAT
        calendar.set(Calendar.HOUR_OF_DAY,4);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis())
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        Intent serviceIntent = new Intent(this, FuncRunner.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Objects.requireNonNull(am).set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    public class FuncRunner extends IntentService {
        private static final String TAG = "MyService";

        /* Services must have a no-arg constructor */
        public FuncRunner() {
            super(TAG);
        }
        @Override
        protected void onHandleIntent( @Nullable Intent intent) {
            checkDefaultExcercise();
            Toast.makeText(this,"Default Excercise Have been added",Toast.LENGTH_SHORT).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void permissionValidator(){
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG,"Permission is granted");
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean permissionValidatorwithBoolean(){
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}

