package ch.renuo.hackzurich2016.models;

import java.util.List;
import java.util.UUID;

public interface Device {
    UUID getId();

    List<SystemAlarm> getSystemAlarms();
}
