package com.example.capstone.ui.chat.bluetooth;

import java.util.UUID;

/**
 * Constants for use in the Bluetooth LE Chat sample.
 */
public class Constants {
    /**
     * UUID identified with this app - set as Service UUID for BLE Chat.
     *
     * Bluetooth requires a certain format for UUIDs associated with Services.
     * The official specification can be found here:
     * [://www.bluetooth.org/en-us/specification/assigned-numbers/service-discovery][https]
     */
    public static final UUID SERVICE_UUID = UUID.fromString("0000b81d-0000-1000-8000-00805f9b34fb");

    /**
     * UUID for the message.
     */
    public static final UUID MESSAGE_UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b");

    /**
     * UUID to confirm device connection.
     */
    public static final UUID CONFIRM_UUID = UUID.fromString("36d4dc5c-814b-4097-a5a6-b93b39085928");

    public static final int REQUEST_ENABLE_BT = 1;
}