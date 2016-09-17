package ch.renuo.hackzurich2016.data;

import android.util.Pair;

import java.util.List;

import ch.renuo.hackzurich2016.helpers.AlarmTimeHelper;
import ch.renuo.hackzurich2016.models.Cluster;
import ch.renuo.hackzurich2016.models.ClusterAlarm;
import ch.renuo.hackzurich2016.models.Device;
import ch.renuo.hackzurich2016.models.Household;

public class HouseholdQuery {

    private Household _household;

    public HouseholdQuery(Household household) {
        this._household = household;

    }

    public Cluster findClusterById(String id) {
        for (Cluster cluster : this._household.getClusters()) {
            if (cluster.getId().toString().equals(id)) {
                return cluster;
            }
        }
        return null;
    }

    public Pair<Cluster, ClusterAlarm> findAlarmById(String id) {
        for (Cluster cluster : this._household.getClusters()) {
            for (ClusterAlarm alarm : cluster.getClusterAlarms()) {
                if (alarm.getId().toString().equals(id)) {
                    return Pair.create(cluster, alarm);
                }
            }
        }
        return null;
    }

    public Device findDeviceById(String deviceId) {
        for (Cluster cluster : _household.getClusters()) {
            for (Device device : cluster.getDevices()) {
                if(device.getId().equals(deviceId)){
                    return device;
                }
            }
        }

        return null;
    }

    public List<ClusterAlarm> getClusterAlarmsForDevice(String deviceId) {
        for (Cluster cluster : _household.getClusters()) {
            for (Device device : cluster.getDevices()) {
                if(device.getId().equals(deviceId)){
                    return cluster.getClusterAlarms();
                }
            }
        }

        return null;
    }

    public ClusterAlarm getNextClusterAlarm(String deviceID) {
        ClusterAlarm nextAlarm = null;

        for(ClusterAlarm clusterAlarm : getClusterAlarmsForDevice(deviceID)) {

            if (nextAlarm == null) {
                nextAlarm = clusterAlarm;
            }

            if (clusterAlarm.getTimeAsCalendar().compareTo(nextAlarm.getTimeAsCalendar()) > 0) {
                nextAlarm = clusterAlarm;
            }
        }

        return nextAlarm;
    }
}
