package ch.renuo.hackzurich2016.activities;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ui.AcquireEmailHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import ch.renuo.hackzurich2016.AccountUtils;
import ch.renuo.hackzurich2016.MainActivity;
import ch.renuo.hackzurich2016.Manifest;
import ch.renuo.hackzurich2016.R;
import ch.renuo.hackzurich2016.UI;
import ch.renuo.hackzurich2016.alarms.SystemAlarmService;
import ch.renuo.hackzurich2016.data.HouseholdDatabase;
import ch.renuo.hackzurich2016.data.HouseholdDatabaseImpl;
import ch.renuo.hackzurich2016.data.SuccessValueEventListener;
import ch.renuo.hackzurich2016.helpers.PrefsHelper;
import ch.renuo.hackzurich2016.models.Cluster;
import ch.renuo.hackzurich2016.models.ClusterAlarm;
import ch.renuo.hackzurich2016.models.ClusterAlarmImpl;
import ch.renuo.hackzurich2016.models.ClusterImpl;
import ch.renuo.hackzurich2016.models.Device;
import ch.renuo.hackzurich2016.models.DeviceImpl;
import ch.renuo.hackzurich2016.models.Household;
import ch.renuo.hackzurich2016.models.HouseholdImpl;
import ch.renuo.hackzurich2016.models.SystemAlarm;

public class HouseholdActivity extends AppCompatActivity {

    public static final int EDIT_ALARM_REQUEST = 1;

    private UUID deviceId;
    private String householdId;

    private Household household = new HouseholdImpl(UUID.randomUUID(), new ArrayList<Cluster>());

    private HouseholdDatabase hdb;

    private HouseholdActivity self = this;

    private void redraw(){
        final Cluster myCluster = findMyCluster(this.household);
        List<Cluster> clusters = this.household.getClusters();
        Collections.sort(clusters, new Comparator<Cluster>() {
            @Override
            public int compare(Cluster o1, Cluster o2) {
                if(o1 == myCluster)
                    return -1;
                if(o2 == myCluster)
                    return 1;
                return 0;
            }
        });
        final ClusterListAdapter adapter = new ClusterListAdapter(this, clusters);
        ((ListView)findViewById(R.id.clusterList)).setAdapter(adapter);
        setUserFromFirebase(FirebaseAuth.getInstance());
        Log.e("r", "redraw");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(new Intent(this, SystemAlarmService.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_household);

        final Intent intent = getIntent();
        this.deviceId = UUID.fromString(intent.getStringExtra(getString(R.string.device_id)));
        this.householdId = intent.getStringExtra(getString(R.string.household_id));
        boolean create = intent.getBooleanExtra(getString(R.string.create_household), false);


        UI.ui().registerRefreshCallback(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        getListView().invalidateViews();
                        ClusterListAdapter adapter = ((ClusterListAdapter)((ListView)self.findViewById(R.id.clusterList)).getAdapter());
                        if(adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                        redraw();
                    }
                });
            }
        });

        this.hdb = this.initializeDatabase(this.householdId, create);

        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.e("a", "auth state changed");
                self.invalidateOptionsMenu();
                setUserFromFirebase(firebaseAuth);
            }
        });

    }

    private void setUser(){
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            Log.e("p", "permission granted auto");
            setUserFromLocal();
        }else {
            Log.e("p", "asking for permission");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS}, 45);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 45){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e("p", "permission granted");
                setUserFromLocal();
            }
        }
    }

    private void setUserFromLocal() {
        Uri photoUrl = AccountUtils.getUserProfile(this).possiblePhoto();
        List<String> names = AccountUtils.getUserProfile(this).possibleNames();
        if(self.household != null) {
            Device device = findMyDevice(self.household);
            Cluster cluster = findMyCluster(self.household);
            if (cluster != null) {
                if(names != null && names.size() > 0) {
                    String name = names.get(0);
                    if (cluster.getName() == null || cluster.getName().equals("You") || cluster.getName().length() == 0) {
                        cluster.setName(name);
                        hdb.updateHousehold(self.household);
                    }
                }
                if (photoUrl != null) {
                    String imageUrl = photoUrl.toString();
                    if (device.getImageUrl() == null) {
                        device.setImageUrl(imageUrl);
                        hdb.updateHousehold(self.household);
                    }
                }
            }
        }else{
            Log.e("h", "household null");
        }
    }

    private void setUserFromFirebase(@NonNull FirebaseAuth firebaseAuth) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if(user != null && self.household != null) {
            Device device = findMyDevice(self.household);
            Cluster cluster = findMyCluster(self.household);
            if (cluster != null) {
                String name = user.getDisplayName();
                if (cluster.getName() == null || cluster.getName().equals("You") || cluster.getName().length() == 0) {
                    cluster.setName(name);
                    hdb.updateHousehold(self.household);
                }
                Uri photoUrl = user.getPhotoUrl();
                if (photoUrl != null) {
                    String imageUrl = photoUrl.toString();
                    if (device.getImageUrl() == null || !device.getImageUrl().equals(imageUrl)) {
                        device.setImageUrl(imageUrl);
                        hdb.updateHousehold(self.household);
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

//    private HouseholdDatabaseMock initializeDatabase(String householdId){
//        if(HouseholdDatabaseMock.db == null){
//            HouseholdDatabaseMock.db = new HouseholdDatabaseMock(householdId);
//        }
//        Log.e("e", "initializing db");
//        return HouseholdDatabaseMock.db;
//    }

    private Device findMyDevice(Household household){
        for (Cluster cluster : household.getClusters()) {
            for (Device device : cluster.getDevices()) {
                if(device.getId().equals(self.deviceId)){
                    return device;
                }
            }
        }

        return null;
    }

    private Cluster findMyCluster(Household household){
        for (Cluster cluster : household.getClusters()) {
            for (Device device : cluster.getDevices()) {
                if(device.getId().equals(self.deviceId)){
                    return cluster;
                }
            }
        }

        return null;
    }

    private HouseholdDatabase initializeDatabase(String householdId, boolean create){
        HouseholdDatabase db = new HouseholdDatabaseImpl(UUID.fromString(householdId));
        SuccessValueEventListener<Household> listener = new SuccessValueEventListener<Household>() {
            @Override
            protected void onChange(Household household) {
                if(household == null){
                    getApplicationContext().getSharedPreferences(PrefsHelper.PREFKEY, Context.MODE_PRIVATE).edit().clear().apply();
                    self.finish();
                    startActivity(new Intent(self, MainActivity.class));
                    return;
                }
                self.household = household;

                Log.e("e","onchange");
                UI.ui().refreshUI();

                //ensure that we ourselves are in the list
                Cluster myCluster = findMyCluster(household);
                if(myCluster == null){
                    myCluster = new ClusterImpl(UUID.randomUUID(), "You", new ArrayList<ClusterAlarm>(), new ArrayList<Device>());
                    // TODO: add image url from file
                    String imageUrl = null;
                    myCluster.getDevices().add(new DeviceImpl(self.deviceId, imageUrl));
                    household.getClusters().add(myCluster);
                    hdb.updateHousehold(household);
                    setUser();
                }
            }
        };
        if(create){
            db.createHousehold(listener);
        }else{
            db.listenForUpdates(listener);
        }
        return db;
    }


    private class ClusterListAdapter extends ArrayAdapter<Cluster>{
        private List<Cluster> clusters;
        private Context context;

        public ClusterListAdapter(Context context, List<Cluster> clusters) {
            super(context, -1, clusters);
            this.clusters = clusters;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final Cluster cluster = clusters.get(position);
            View rowView = inflater.inflate(R.layout.list_row, parent, false);
            ImageView imageView = (ImageView)rowView.findViewById(R.id.clusterImage);
            setClusterImage(cluster, imageView);
            ((TextView)rowView.findViewById(R.id.clusterName)).setText(cluster.getName());
            ((TextView)rowView.findViewById(R.id.clusterName)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText input = new EditText(self);
                    new AlertDialog.Builder(self).setTitle("Change Name").setView(input).setPositiveButton(
                            "OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String result = input.getText().toString();
                                    if(result != null && result.length() > 0) {
                                        cluster.setName(result);
                                        hdb.updateHousehold(self.household);
                                    }
                                }
                            }
                    ).setNegativeButton(
                            "Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            }
                    ).show();
                }
            });
            ((ImageButton)rowView.findViewById(R.id.addAlarmButton)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(self, EditAlarmActivity.class);
                    intent.putExtra(getString(R.string.cluster_id), cluster.getId().toString());
                    intent.putExtra(getString(R.string.alarm_id), UUID.randomUUID().toString());
                    intent.putExtra(getString(R.string.alarm_time), "12:00");
                    intent.putExtra(getString(R.string.alarm_active), true);
                    intent.putExtra(getString(R.string.alarm_new), true);
                    startActivityForResult(intent, EDIT_ALARM_REQUEST);
                }
            });

            RecyclerView tlv = (RecyclerView)rowView.findViewById(R.id.timerListView);
            tlv.setLayoutManager(new LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false));
            TimerListAdapter adapter = new TimerListAdapter(cluster, cluster.getClusterAlarms());
            tlv.setAdapter(adapter);
            return rowView;
        }

        private class TimerListAdapter extends RecyclerView.Adapter<ViewHolder>{

            private Cluster cluster;
            private List<ClusterAlarm> alarms;

            public TimerListAdapter(Cluster cluster, List<ClusterAlarm> alarms){
                this.cluster = cluster;
                this.alarms = alarms;
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.timer_item, parent, false);
                return new ViewHolder(v){};
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                final TextView tc = (TextView)holder.itemView.findViewById(R.id.textClock);
                final ClusterAlarm alarm = this.alarms.get(position);
                tc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alarm.setActive(!alarm.getActive());
                        hdb.updateHousehold(self.household);
                    }
                });
                tc.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Intent intent = new Intent(self, EditAlarmActivity.class);
                        intent.putExtra(getString(R.string.cluster_id), cluster.getId().toString());
                        intent.putExtra(getString(R.string.alarm_id), alarm.getId().toString());
                        intent.putExtra(getString(R.string.alarm_time), alarm.getTime());
                        intent.putExtra(getString(R.string.alarm_active), alarm.getActive());
                        intent.putExtra(getString(R.string.alarm_new), false);
                        startActivityForResult(intent, EDIT_ALARM_REQUEST);
                        return true;
                    }
                });
                tc.setText(alarm.getTime());
                if (alarm.getActive()) {
                    tc.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
                } else {
                    tc.setBackgroundColor(ContextCompat.getColor(context, R.color.colorSecondary));
                }
            }

            @Override
            public int getItemCount() {
                return this.alarms.size();
            }

        }
    }

    private void setClusterImage(Cluster cluster, final ImageView imageView) {
        if(cluster == null)
            return;
        String imageUrl = null;
        for (Device device : cluster.getDevices()) {
            if(device.getImageUrl() != null && device.getImageUrl().length() > 0){
                imageUrl = device.getImageUrl();
                break;
            }
        }
        if(imageUrl == null) {
            Log.e("i", "no image");
            return;
        }
        final String finalImageUrl = imageUrl;
        Log.e("i", finalImageUrl);
        if(imageUrl.startsWith("content")){
            imageView.setImageResource(0);
            imageView.setImageURI(Uri.parse(imageUrl));
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(finalImageUrl);
                        final Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bmp);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private Cluster findClusterById(String id){
        for (Cluster cluster : this.household.getClusters()) {
            if(cluster.getId().toString().equals(id)){
                return cluster;
            }
        }
        return null;
    }

    private Pair<Cluster, ClusterAlarm> findAlarmById(String id){
        for (Cluster cluster : this.household.getClusters()) {
            for (ClusterAlarm alarm : cluster.getClusterAlarms()) {
                if(alarm.getId().toString().equals(id)){
                    return Pair.create(cluster, alarm);
                }
            }
        }
        return null;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == EDIT_ALARM_REQUEST){
            String clusterId = data.getStringExtra(getString(R.string.cluster_id));
            String alarmId = data.getStringExtra(getString(R.string.alarm_id));
            String alarmTime = data.getStringExtra(getString(R.string.alarm_time));
            boolean alarmActive = data.getBooleanExtra(getString(R.string.alarm_active), false);
            boolean alarmNew = data.getBooleanExtra(getString(R.string.alarm_new), false);

            Pair<Cluster, ClusterAlarm> pp = findAlarmById(alarmId);
            if(resultCode > 0 && pp != null){
                Cluster cluster = pp.first;
                ClusterAlarm alarm = pp.second;
                cluster.getClusterAlarms().remove(alarm);
                hdb.updateHousehold(self.household);
            }

            else if(resultCode == 0){

                if(alarmNew) {
                    Cluster cluster = findClusterById(clusterId);
                    ClusterAlarm alarm = new ClusterAlarmImpl(UUID.randomUUID(), alarmTime, alarmActive);
                    cluster.getClusterAlarms().add(alarm);
                    hdb.updateHousehold(self.household);
                    Log.e("n", "newalarm");
                }

                else if(pp != null){
                    Cluster cluster = pp.first;
                    ClusterAlarm alarm = pp.second;
                    alarm.setTime(alarmTime);
                    alarm.setActive(alarmActive);
                    this.hdb.updateHousehold(self.household);
                }
            }

        }

        else if (requestCode== AcquireEmailHelper.RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Log.e("a", "auth result is not ok");
                // user is signed in!
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Log.e("a", "auth result is not ok");
                // user is not signed in. Maybe just wait for the user to press
                // "sign in" again, or show a message
            }
        }
    }

    public void addMemberButtonClicked(View view){
        Intent intent = new Intent(this, BarcodeActivity.class);
        intent.putExtra(getString(R.string.household_id), this.householdId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        if(menu.size() > 0) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            menu.getItem(1).setVisible(user == null);
            menu.getItem(2).setVisible(user != null);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_leave_household) {
            Cluster myCluster = findMyCluster(self.household);
            self.household.getClusters().remove(myCluster);
            getApplicationContext().getSharedPreferences(PrefsHelper.PREFKEY, Context.MODE_PRIVATE).edit().clear().apply();
            finish();
            startActivity(new Intent(self, MainActivity.class));
            return true;
        }

        else if (id == R.id.action_login){
            Log.e("a", "login");
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setProviders(
                                    AuthUI.EMAIL_PROVIDER,
                                    AuthUI.GOOGLE_PROVIDER)
                            .build(),
                    AcquireEmailHelper.RC_SIGN_IN);
            return true;
        }

        else if (id == R.id.action_logout){
            Log.e("a", "logout");
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signOut();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



}
