package com.example.convomail;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SearchTerm;

public class EmailDetailView extends AppCompatActivity {
    TextView fromAddress;
    TextView subject;
    TextView content;
    TextView date;
    SharedPreferences SharedPreferences;
    private String[] month = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static int msgno;
    public static final String PREFS_NAME = "myPrefsFile";
    DrawerLayout drawer;
    private ProgressBar spinner;
    Folder emailFolder;
    Store store;
    Message m;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_detail_view);
        try{
            String fileName = getIntent().getStringExtra("file");
            int pos = getIntent().getIntExtra("position", 0);
            spinner = findViewById(R.id.progressBar2);

            FileInputStream fis = getApplicationContext().openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);
            User user = (User) is.readObject();
            is.close();
            fis.close();
            Log.d("User", user.getUserID());
            Toolbar toolbar = findViewById(R.id.tool_bar1);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);


            if (fileName.contains("Primary")) {
                m = user.inbox.getPrimary().getMessages().get(pos);
            } else if (fileName.contains("Draft")) {
                m = user.inbox.getDraft().getMessages().get(pos);
            } else if (fileName.contains("Spam")) {
                m = user.inbox.getSpam().getMessages().get(pos);
            } else if (fileName.contains("Trash")) {
                m = user.inbox.getTrash().getMessages().get(pos);
            } else if (fileName.contains("SentMail")) {
                m = user.inbox.getSentMail().getMessages().get(pos);
            }
            msgno = m.getMsgno();
            InternetAddress person = (InternetAddress)m.getFromAddress()[0];
            fromAddress = findViewById(R.id.fromAddr);
            subject = findViewById(R.id.subject);

            date = findViewById(R.id.date1);
            date.setText(month[m.getDate().getMonth()]+ " " +  m.getDate().getDate() +" "+ (m.getDate().getYear()+1900));
            fromAddress.setText(person.getAddress());

            subject.setText(m.getSubject());
            new RetrieveContent().execute(user.getUserID(), user.getPassword());
        }catch (Exception e){
            Log.d("Error", e.toString());
        }
    }

    void setMail(String content1) {
        content = findViewById(R.id.content);
        content.setMovementMethod(new ScrollingMovementMethod());
        content.setVisibility(View.VISIBLE);
        content.setText(content1);



    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem menuItem = menu.findItem(R.id.sync1);
        menuItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")

    class RetrieveContent extends AsyncTask<String, Void, String>{
        private Context context;
        private ProgressDialog progressDialog;
        private String getHost(String user){
            String[]s = user.split("@");
            Properties properties = new Properties();
            if(s[1].equals("gmail.com")){
                return "smtp.gmail.com";
            }
            else if(s[1].equals("outlook.com")){
                return "smtp.office365.com";
            }
            return "";
        }
        private String getTextFromMessage(javax.mail.Message message) throws Exception {
            if (message.isMimeType("text/plain")){
                return message.getContent().toString();
            }else if (message.isMimeType("multipart/*")) {
                String result = "";
                MimeMultipart mimeMultipart = (MimeMultipart)message.getContent();
                int count = mimeMultipart.getCount();
                for (int i = 0; i < count; i ++){
                    BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                    if (bodyPart.isMimeType("text/plain")){
                        result = result + "\n" + bodyPart.getContent();
                        break;  //without break same text appears twice in my tests
                    } else if (bodyPart.isMimeType("text/html")){
                        String html = (String) bodyPart.getContent();
                        result = result + "\n" + Jsoup.parse(html).text();

                    }
                }
                return result;
            }
            return "";
        }
        private Properties getProp(String user){
            String[] s = user.split("@");
            Properties properties = new Properties();
            if(s[1].equals("gmail.com")){
                properties.put("mail.smtp.host", this.getHost(user));
                properties.put("mail.smtp.socketFactory.port", "587");
                properties.put("mail.smtp.port", "587");
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
            }
            else if(s[1].equals("outlook.com")){
                properties.put("mail.smtp.host", this.getHost(user));
                properties.put("mail.smtp.socketFactory.port", "465");
                properties.put("mail.smtp.port", "465");
                properties.put("mail.smtp.auth", "true");
            }
            return properties;
        }
        private String getFold(String user){
            String[] s = user.split("@");

            if(s[1].equals("gmail.com")){
                return "INBOX";
            }
            else if(s[1].equals("outlook.com")){
                return "INBOX" ;
            }
            return "";
        }

        @Override
        protected String  doInBackground(String... strings) {
            Inbox inbox = new Inbox(new Mail(new ArrayList<Message>()), new Mail(new ArrayList<com.example.convomail.Message>()), new Mail(new ArrayList<com.example.convomail.Message>()), new Mail(new ArrayList<com.example.convomail.Message>()));
            javax.mail.Message messages=null;
            Object c=null;
            String s=null;
            try{

                SearchTerm term = new SearchTerm() {
                    public boolean match(javax.mail.Message message) {
                        try {
                            if (message.getMessageNumber() == msgno) {
                                return true;
                            }
                        } catch (Exception ex) {
                            Log.d("Search Term exception", ex.toString());
                        }
                        return false;
                    }
                };
                // create properties field
                String host = this.getHost(strings[0]);
                Properties properties = this.getProp(strings[0]);

                Session emailSession = Session.getDefaultInstance(properties);
                // emailSession.setDebug(true);
                Log.d("nnn", "ss");

                // create the POP3 store object and connect with the pop server
                store = emailSession.getStore("imaps");
                store.connect(host, strings[0], strings[1]);

                // create the folder object and open it
                Folder[] f = store.getDefaultFolder().list();
                for(int i=0;i<f.length;i++){
                    Log.d("Folder", f[i].toString());
                }

                emailFolder = store.getFolder(this.getFold(strings[0]));
                emailFolder.open(Folder.READ_WRITE);

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        System.in));

                // retrieve the messages from the folder in an array and print it

                messages = emailFolder.getMessage(msgno);
                s = getTextFromMessage(messages);
                if(emailFolder!=null){
                    emailFolder.close(false);
                }
                if(store!=null){
                    store.close();
                }

            }
            catch (AuthenticationFailedException e){
                Intent i = new Intent(context, MainActivity.class);
                i.putExtra("Auth", "autherror");
                startActivity(i);
            }
            catch (Exception e){
                Log.d("err1", e.toString()) ;

            }
            return s;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

                spinner.setVisibility(View.VISIBLE);


        }

        protected  void onPostExecute(String c) {


            spinner.setVisibility(View.GONE);

            setMail(c);
        }
    }
}
