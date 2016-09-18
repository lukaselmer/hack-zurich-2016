package ch.renuo.hackzurich2016.alarms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

import ch.renuo.hackzurich2016.data.HouseholdDatabase;
import ch.renuo.hackzurich2016.data.HouseholdDatabaseImpl;
import ch.renuo.hackzurich2016.data.HouseholdQuery;
import ch.renuo.hackzurich2016.data.SuccessValueEventListener;
import ch.renuo.hackzurich2016.utils.PrefUtils;
import ch.renuo.hackzurich2016.models.ClusterAlarm;
import ch.renuo.hackzurich2016.models.Household;

public class SystemAlarmService extends Service {
    public static final String TAG = "SystemAlarmService";
    public static final String STOP_ALARM_EVENT = "STOP_ALARM_EVENT";

    private PrefUtils preferences;
    private HouseholdDatabase _db;
    private AlarmScheduler _scheduler;

    @Override
    public void onCreate() {
        initializePrefs();
        initializeScheduler();
        initializeDatabase();
    }

    private void initializeScheduler() {
        this._scheduler = new AlarmScheduler(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying Service");
        super.onDestroy();
    }

    private void initializeDatabase() {
        if(preferences.getHouseholdId() == null) {
            stopSelf();
            return;
        }

        this._db = new HouseholdDatabaseImpl(UUID.fromString(preferences.getHouseholdId()));
        _db.listenForUpdates(new SuccessValueEventListener<Household>() {

            @Override
            protected void onChange(Household household) {
                if(household == null){
                    Log.d(TAG, "Household is null");
                    showToast("Stopping Background Service: No DB");
                    stopSelf();
                    return;
                }

                ClusterAlarm nextAlarm = getNextClusterAlarm(household);
                if(nextAlarm == null) {
                    cancelScheduledAlarm();
                } else {
                    scheduleAlarm(nextAlarm);
                }
            }

            private ClusterAlarm getNextClusterAlarm(Household household) {
                return new HouseholdQuery(household).getNextClusterAlarm(preferences.getDeviceId());
            }
        });
    }

    private void initializePrefs() {
        this.preferences = new PrefUtils(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting with startId: " + startId);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleAlarm(ClusterAlarm alarm) {
        showToast("Scheduling Alarm: " + alarm.getTime());
        _scheduler.scheduleStartAlarm(alarm.getTimeAsCalendar(), alarm.getId().toString());
    }

    private void cancelScheduledAlarm() {
        _scheduler.cancelStartAlarm(null);
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
