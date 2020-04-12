package com.teamlilhax.smartparkingsystem;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Login extends Activity {

    private final OkHttpClient client = new OkHttpClient();
    TextView msgTxtView;
    EditText editTextEmail;
    EditText editTextPassword;

    SharedPreferences sh;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        sh = getSharedPreferences("MySharedPref", MODE_PRIVATE);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int)(width*.85),(int)(height*.55));

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        Button exitBtn = findViewById(R.id.exitBtn);

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button loginBtn = findViewById(R.id.loginBtn);

        msgTxtView = findViewById(R.id.msgTxtView);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editTextEmail.onEditorAction((EditorInfo.IME_ACTION_DONE));
                editTextPassword.onEditorAction((EditorInfo.IME_ACTION_DONE));
                String email = editTextEmail.getText().toString();
                String password = editTextPassword.getText().toString();

                if(email.equals("") || password.equals("")){
                    msgTxtView.setText("You cannot leave the parameters blank !");
                }else{
                    POSTRequest(email,password);
                }
            }
        });
    }


    public void POSTRequest(String username, String password){
        String url = "http://192.168.31.26:8080/account/auth";

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("username", username)
                .addFormDataPart("password", password)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        try {
            Response response = client.newCall(request).execute();
            String json_object = response.body().string();
            JSONObject mainObject = new JSONObject(json_object);
            System.out.println(mainObject.getString("status"));

            if(mainObject.getBoolean("status")){
                System.out.println(mainObject.getString("email"));
                saveTopic(mainObject.getString("email"));
            }else{
                msgTxtView.setText("Invalid Credentials");
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public void saveTopic(String username){
        SharedPreferences sharedPreferences
                = getSharedPreferences("MySharedPref",
                MODE_PRIVATE);
        SharedPreferences.Editor myEdit
                = sharedPreferences.edit();
        myEdit.putString("email", username);
        myEdit.putBoolean("email_status",true);
        myEdit.commit();

        String[] topic_arr = sh.getString("email","null").split("@");
        String topic = topic_arr[0]+"%"+topic_arr[1];

        addEmailToSharedPref(topic);
        unsubscribeAll();
        subscribe(topic);
    }

    private void unsubscribeAll() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        FirebaseMessaging messaging= FirebaseMessaging.getInstance();
        int index = sh.getInt("index",0);

        for(int i=index;i>=0;i--){
            String topic = sh.getString(Integer.toString(i),"null");
            messaging.unsubscribeFromTopic(topic);
        }
    }

    public void subscribe(final String topic){
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        final SharedPreferences.Editor myEdit = sharedPreferences.edit();

        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg ="Successfull";
                        if (!task.isSuccessful()) {
                            msg = "Failed";
                        }
                        myEdit.putBoolean("email_status",true);
                        myEdit.commit();
                        Toast.makeText(Login.this, msg+" subscribed to "+topic, Toast.LENGTH_SHORT).show();
                    }
                });
        Log.d("FCMToken", "token "+ FirebaseInstanceId.getInstance().getToken());
    }
    public void addEmailToSharedPref(String email){

        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();

        int index = sh.getInt("index",0);
        myEdit.putString(Integer.toString(index),email);
        myEdit.putInt("index",index+1);

        myEdit.commit();
    }
}