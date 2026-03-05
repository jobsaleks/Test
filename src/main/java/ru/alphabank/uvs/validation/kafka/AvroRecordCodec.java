package ru.alphabank.uvs.validation.kafka;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroRecordCodec {

    public byte[] serialize(SpecificRecord record) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            new SpecificDatumWriter<SpecificRecord>(record.getSchema()).write(record, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to serialize avro record", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends SpecificRecord> T deserialize(byte[] payload, Schema schema, Class<T> clazz) {
        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payload, null);
            SpecificDatumReader<T> reader = new SpecificDatumReader<>(schema);
            return reader.read((T) clazz.getDeclaredConstructor().newInstance(), decoder);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to deserialize avro record", ex);
        }
    }

    public SpecificRecordBase deserialize(byte[] payload, Schema schema) {
        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(payload, null);
            SpecificDatumReader<SpecificRecordBase> reader = new SpecificDatumReader<>(schema);
            return reader.read(null, decoder);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to deserialize avro response", ex);
        }
    }
}
