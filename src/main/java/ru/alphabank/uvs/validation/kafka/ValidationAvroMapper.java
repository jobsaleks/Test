package ru.alphabank.uvs.validation.kafka;

import org.apache.avro.specific.SpecificRecord;
import ru.alphabank.uvs.validation.dto.ValidationRequestDto;
import ru.alphabank.uvs.validation.dto.ValidationResponseDto;

public interface ValidationAvroMapper {
    SpecificRecord toRequestRecord(ValidationRequestDto requestDto);

    ValidationResponseDto fromResponseRecord(SpecificRecord specificRecord);
}
