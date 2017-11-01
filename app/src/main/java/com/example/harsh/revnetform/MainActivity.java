package com.example.harsh.revnetform;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.graphics.Color.RED;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private EditText firstName, lastName, phone, email, company;
    private TextView firstnameHelper, lastnameHelper, phoneHelper, emailHelper, companyHelper;
    private DevicePolicyManager mDpm;

    String FirstName, LastName, Phone, Email, Company;
    String text,phoneRegex = "^[+]?[0-9]{10,13}$";
    DatabaseHelper mDatabaseHelper;
    private boolean mIsKioskEnabled = false;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        button = (Button) findViewById(R.id.button);
        firstName = (EditText) findViewById(R.id.firstname);
        lastName = (EditText) findViewById(R.id.lastname);
        phone = (EditText) findViewById(R.id.phone);
        email = (EditText) findViewById(R.id.email);
        company = (EditText) findViewById(R.id.company);

        firstnameHelper = (TextView) findViewById(R.id.firstnameHelper);
        lastnameHelper = (TextView) findViewById(R.id.lastnameHelper);
        phoneHelper = (TextView) findViewById(R.id.phoneHelper);
        emailHelper = (TextView) findViewById(R.id.emailHelper);
        companyHelper = (TextView) findViewById(R.id.companyHelper);

        mDatabaseHelper = new DatabaseHelper(this);
//        Log.d("Tag","delete db:"+String.valueOf(this.deleteDatabase("Revnet")));

        ComponentName deviceAdmin = new ComponentName(this, AdminReceiver.class);
        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (!mDpm.isAdminActive(deviceAdmin)) {
            Toast.makeText(this, "Not admin", Toast.LENGTH_SHORT).show();
        }

        if (mDpm.isDeviceOwnerApp(getPackageName())) {
            mDpm.setLockTaskPackages(deviceAdmin, new String[]{getPackageName()});
        } else {
            Toast.makeText(this, "Not device owner", Toast.LENGTH_SHORT).show();
        }


        enableKioskMode(true);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firstnameHelper.setText("");
                lastnameHelper.setText("");
                emailHelper.setText("");
                phoneHelper.setText("");
                companyHelper.setText("");
                Log.d("Tag", String.valueOf(isExternalStorageWritable()));
                Log.d("Tag", String.valueOf(isStoragePermissionGranted()));
                FirstName = firstName.getText().toString();
                LastName = lastName.getText().toString();
                Phone = phone.getText().toString();
                Email = email.getText().toString();
                Company = company.getText().toString();

                if (FirstName .equals("")) {
                    Toast.makeText(getApplicationContext(), "Please fill mandatory fields", Toast.LENGTH_LONG).show();
                    firstnameHelper.setText("Please enter a value");
                    }
                    else if(Email.equals("")) {
                        Toast.makeText(getApplicationContext(), "Please fill mandatory fields", Toast.LENGTH_LONG).show();
                        emailHelper.setText("Please enter a value");
                    }
                        else if(Phone.equals("")) {
                            Toast.makeText(getApplicationContext(), "Please fill mandatory fields", Toast.LENGTH_LONG).show();
                            phoneHelper.setText("Please enter a value");
                        }
                            else if(Company.equals("")) {
                                Toast.makeText(getApplicationContext(), "Please fill mandatory fields", Toast.LENGTH_LONG).show();
                                companyHelper.setText("Please enter a value");
                            }
                else {
                    if (!isValidEmail(Email)) {
                        Toast.makeText(getApplicationContext(), "Invalid email", Toast.LENGTH_LONG).show();
                        emailHelper.setText("Invalid email");
                    } else {
                        if (!isValidMobile(Phone)||!isValidPhone(Phone)){
                            Toast.makeText(getApplicationContext(), "Invalid phone", Toast.LENGTH_LONG).show();
                            phoneHelper.setText("Invalid phone");
                        }
                        else {
                            //Everything looks good
                            AddData(FirstName, LastName, Email, Phone, Company);
                            text = "\n" + FirstName + "   \t" + LastName + "   \t" + Phone + "   \t" + Email + "   \t" + Company;
                            String[] textDemo = {FirstName, LastName, Email, Phone, Company};

                            generateNoteOnSD(getApplicationContext(), "data.txt", "\n" + Arrays.toString(textDemo));
                        }
                    }

                }
            }
        });
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }



    public void generateNoteOnSD(Context context, String sFileName, String sBody) {
        try {
//            File file = new File(context.getFilesDir(), sFileName);
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
                Log.d("Tag","made dir");
            }
            File gpxfile = new File(root, sFileName);
            FileWriter writer = new FileWriter(gpxfile,true);
            writer.append(sBody);
            writer.flush();
            writer.close();
            Log.d("Tag","write success");
        } catch (IOException e) {
            Log.d("Tag","error");
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
       //Do nothing
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showChangeLangDialog();
            return true;
        }
        if(keyCode == KeyEvent.KEYCODE_POWER){
            //Do nothing
        }
        return super.onKeyLongPress(keyCode, event);
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private boolean isValidMobile(String phone) {
        return android.util.Patterns.PHONE.matcher(phone).matches();
    }

    public boolean isValidPhone(String phone)
    {
        String expression = "^([0-9\\+]|\\(\\d{1,3}\\))[0-9\\-\\. ]{3,15}$";
        CharSequence inputString = phone;
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(inputString);
        if (matcher.matches())
        {
            return true;
        }
        else{
            return false;
        }
    }

    public void AddData(String firstname, String lastname, String emailid, String mobile, String Company) {
        boolean insertData = mDatabaseHelper.addData(firstname,lastname,emailid,mobile,Company);

        if (insertData) {
            toastMessage("Data Successfully Inserted!");
            firstName.setText("");
            lastName.setText("");
            email.setText("");
            phone.setText("");
            company.setText("");
            firstnameHelper.setText("");
            lastnameHelper.setText("");
            emailHelper.setText("");
            phoneHelper.setText("");
            companyHelper.setText("");
            //redirect to another activity

        } else {
            toastMessage("Something went wrong");
        }
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("Tag","Permission is granted");
                return true;
            } else {

                Log.d("Tag","Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.d("Tag","Permission is granted");
            return true;
        }
    }

    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.d("Tag","Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void enableKioskMode(boolean enabled) {
        try {
            if (enabled) {
                if (mDpm.isLockTaskPermitted(this.getPackageName())) {
                    startLockTask();
                    mIsKioskEnabled = true;
                    //Toast.makeText(this, "Kiosk mode enabled", Toast.LENGTH_SHORT).show();
                } else {

                }
            } else {
                stopLockTask();
                mIsKioskEnabled = false;
            }
        } catch (Exception e) {
            // TODO: Log and handle appropriately

        }
    }

    public void showChangeLangDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText edt = (EditText) dialogView.findViewById(R.id.edit1);
        dialogBuilder.setMessage("Please Enter Password");
        dialogBuilder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            public void onClick(DialogInterface dialog, int whichButton) {

                if (edt.getText().toString().equals("revnet**")) {
                    Log.d("Tag","unpinning now..");
                    stopLockTask();
                    //button.setVisibility(View.VISIBLE);
                }

                else if (edt.getText().toString().equals("showdatabase")) {
                    Intent intent = new Intent(MainActivity.this, ListDataActivity.class);
                    startActivity(intent);;
                }

                else if(edt.getText().toString().equals("exportdb")){
                    exportDB();
                }

            }
        });
        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void exportDB() {

//        File dbFile = getDatabasePath("Revnet.db");
        DatabaseHelper dbhelper = new DatabaseHelper(getApplicationContext());
        File exportDir = new File(Environment.getExternalStorageDirectory(), "CSV");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        File file = new File(exportDir, "CSVfile.csv");
        try {
            file.createNewFile();
            CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM Revnet", null);
            csvWrite.writeNext(curCSV.getColumnNames());
            while (curCSV.moveToNext()) {
                //Which column you want to exprort
                String arrStr[] = {curCSV.getString(0), curCSV.getString(1), curCSV.getString(2), curCSV.getString(3),
                        curCSV.getString(4), curCSV.getString(5)};
                csvWrite.writeNext(arrStr);
            }
            csvWrite.close();
            curCSV.close();
        } catch (Exception sqlEx) {
            Log.e("MainActivity", sqlEx.getMessage(), sqlEx);
            Log.d("Tag","error in csv");
        }
    }
}
