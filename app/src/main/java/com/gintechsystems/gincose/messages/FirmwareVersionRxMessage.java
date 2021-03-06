package com.gintechsystems.gincose.messages;

import android.util.Log;

import com.gintechsystems.gincose.Extensions;
import com.gintechsystems.gincose.GINcoseWrapper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/30/16.
 */
public class FirmwareVersionRxMessage extends TransmitterMessage {
    byte opcode = 0x21;

    public FirmwareVersionRxMessage(byte[] packet) {
        if (packet.length >= 18) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

            }
        }
    }
}
