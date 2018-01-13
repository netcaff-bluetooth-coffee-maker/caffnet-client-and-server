package com.quew8.netcaff.lib.ble.util;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Quew8
 */

public class ServiceBuilder {
    private final UUID serviceId;
    private ArrayList<BluetoothGattCharacteristic> chars = new ArrayList<>();

    public ServiceBuilder(UUID serviceId) {
        this.serviceId = serviceId;
    }

    /*public ServiceBuilder addCharacteristic(UUID charId, int properties, int permissions) {
        chars.add(new BluetoothGattCharacteristic(charId, properties, permissions));
        return this;
    }*/

    public CharacteristicBuilder newCharacteristic(UUID uuid) {
        return new CharacteristicBuilder(uuid);
    }

    public BluetoothGattService build() {
        BluetoothGattService service = new BluetoothGattService(serviceId, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        for(BluetoothGattCharacteristic bleChar: chars) {
            service.addCharacteristic(bleChar);
        }
        return service;
    }

    public class CharacteristicBuilder {
        private final UUID charId;
        private final ArrayList<BluetoothGattDescriptor> descriptors = new ArrayList<>();
        private int properties = 0, permissions = 0;

        private CharacteristicBuilder(UUID charId) {
            this.charId = charId;
        }

        public CharacteristicBuilder withProperties(int properties) {
            this.properties = this.properties | properties;
            return this;
        }

        public CharacteristicBuilder withPermissions(int permissions) {
            this.permissions = this.permissions | permissions;
            return this;
        }

        public CharacteristicBuilder readable() {
            return withProperties(BluetoothGattCharacteristic.PROPERTY_READ)
                    .withPermissions(BluetoothGattCharacteristic.PERMISSION_READ);
        }

        public CharacteristicBuilder notifiable() {
            return withProperties(BluetoothGattCharacteristic.PROPERTY_NOTIFY);
        }

        public CharacteristicBuilder writeable() {
            return withProperties(BluetoothGattCharacteristic.PROPERTY_WRITE)
                    .withPermissions(BluetoothGattCharacteristic.PERMISSION_WRITE);
        }

        public CharacteristicBuilder withDescriptor(UUID descriptorUUID) {
            descriptors.add(new BluetoothGattDescriptor(
                    descriptorUUID,
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
            ));
            return this;
        }

        public ServiceBuilder add() {
            BluetoothGattCharacteristic bgc = new BluetoothGattCharacteristic(charId, properties, permissions);
            for(BluetoothGattDescriptor bgd: descriptors) {
                bgc.addDescriptor(bgd);
            }
            ServiceBuilder.this.chars.add(bgc);
            return ServiceBuilder.this;
        }
    }
}
