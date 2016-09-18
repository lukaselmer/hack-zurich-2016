package ch.renuo.hackzurich2016.serializers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ch.renuo.hackzurich2016.models.Cluster;
import ch.renuo.hackzurich2016.models.ClusterAlarm;
import ch.renuo.hackzurich2016.models.ClusterAlarmImpl;
import ch.renuo.hackzurich2016.models.ClusterImpl;
import ch.renuo.hackzurich2016.models.Device;
import ch.renuo.hackzurich2016.models.DeviceImpl;
import ch.renuo.hackzurich2016.models.Household;
import ch.renuo.hackzurich2016.models.HouseholdImpl;

public class HouseholdDeserializer {
    private final Map<String, Object> serialized;
    private List<Cluster> clusters;

    public HouseholdDeserializer(Object serialized) {
        this.serialized = (Map<String, Object>) serialized;
    }

    public Household deserialize() {
        deserializeClusters();
        deserializeDevices();
        deserializeClusterAlarms();

        return new HouseholdImpl(getUuid(this.serialized.get("id")), clusters);
    }

    private void deserializeClusters() {
        clusters = new ArrayList<>();

        Object serializedClusters = serialized.get("clusters");
        if (serializedClusters == null) return;

        for (Map<String, Object> serializedCluster : (List<Map<String, Object>>) serializedClusters)
            clusters.add(getCluster(serializedCluster));
    }

    private Cluster getCluster(Map<String, Object> serializedCluster) {
        UUID id = getUuid(serializedCluster.get("id"));
        String name = (String) serializedCluster.get("name");
        Cluster cluster = new ClusterImpl(id, name, new ArrayList<ClusterAlarm>(), new ArrayList<Device>());
        return cluster;
    }

    private void deserializeDevices() {
        List<Map<String, Object>> serializedDevices = (List<Map<String, Object>>) serialized.get("devices");
        if (serializedDevices == null) return;

        for (Map<String, Object> serializedDevice : serializedDevices)
            addDeviceToCluster(getDevice(serializedDevice), getUuid(serializedDevice.get("clusterId")));
    }

    private Device getDevice(Map<String, Object> serializedDevice) {
        UUID id = getUuid(serializedDevice.get("id"));
        String imageUrl = (String) serializedDevice.get("imageUrl");
        return new DeviceImpl(id, imageUrl);
    }

    private void addDeviceToCluster(Device device, UUID clusterId) {
        for (Cluster cluster : clusters)
            if (cluster.getId().equals(clusterId))
                cluster.getDevices().add(device);
    }


    private void deserializeClusterAlarms() {
        List<Map<String, Object>> serializedClusterAlarms = (List<Map<String, Object>>) serialized.get("clusterAlarms");
        if (serializedClusterAlarms == null) return;

        for (Map<String, Object> serializedClusterAlarm : serializedClusterAlarms)
            addClusterAlarmToCluster(getClusterAlarm(serializedClusterAlarm),
                    getUuid(serializedClusterAlarm.get("clusterId")));
    }

    private void addClusterAlarmToCluster(ClusterAlarm clusterAlarm, UUID clusterId) {
        for (Cluster cluster : clusters)
            if (cluster.getId().equals(clusterId))
                cluster.getClusterAlarms().add(clusterAlarm);
    }

    private ClusterAlarm getClusterAlarm(Map<String, Object> serializedClusterAlarm) {
        UUID id = getUuid(serializedClusterAlarm.get("id"));
        String time = (String) serializedClusterAlarm.get("time");
        boolean active = (boolean) serializedClusterAlarm.get("active");
        return new ClusterAlarmImpl(id, time, active);
    }

    private UUID getUuid(Object id) {
        return UUID.fromString((String) id);
    }
}
