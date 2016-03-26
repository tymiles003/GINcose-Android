package com.gintechsystems.gincose.messages;

import com.gintechsystems.gincose.CRC;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/26/16.
 */
public class SensorTxMessage extends TransmitterMessage {
    int opcode = 0x2e;
    byte[] crc = CRC.calculate(ByteBuffer.allocate(4).putInt(opcode).array());

    public SensorTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put((byte)opcode);
        data.put(crc);
        byteSequence = data.array();
    }
}
