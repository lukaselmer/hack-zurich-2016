package ch.renuo.hackzurich2016.serializers;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ch.renuo.hackzurich2016.models.Cluster;
import ch.renuo.hackzurich2016.models.ClusterAlarm;
import ch.renuo.hackzurich2016.models.ClusterAlarmImpl;
import ch.renuo.hackzurich2016.models.ClusterImpl;
import ch.renuo.hackzurich2016.models.Device;
import ch.renuo.hackzurich2016.models.DeviceImpl;
import ch.renuo.hackzurich2016.models.Household;
import ch.renuo.hackzurich2016.models.HouseholdImpl;

public class HouseholdMock {
    @NonNull
    public static Household getHousehold() {
        ClusterAlarm clusterAlarm = new ClusterAlarmImpl(UUID.randomUUID(), "time", true);

        List<ClusterAlarm> clusterAlarms = new ArrayList<>();
        clusterAlarms.add(clusterAlarm);

        String imageUrl = "";
        Device device = new DeviceImpl(UUID.randomUUID(), imageUrl);

        List<Device> devices = new ArrayList<>();
        devices.add(device);

        Cluster cluster = new ClusterImpl(UUID.randomUUID(), "some", clusterAlarms, devices);

        List<Cluster> clusters = new ArrayList<>();
        clusters.add(cluster);

        return new HouseholdImpl(UUID.randomUUID(), clusters);
    }
}
