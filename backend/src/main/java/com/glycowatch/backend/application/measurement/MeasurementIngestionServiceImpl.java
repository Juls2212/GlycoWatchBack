package com.glycowatch.backend.application.measurement;

import com.glycowatch.backend.domain.device.DeviceEntity;
import com.glycowatch.backend.domain.device.UserDeviceLinkEntity;
import com.glycowatch.backend.domain.measurement.GlucoseMeasurementEntity;
import com.glycowatch.backend.infrastructure.persistence.repository.DeviceRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.GlucoseMeasurementRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.UserDeviceLinkRepository;
import com.glycowatch.backend.interfaces.dto.measurement.IngestMeasurementRequestDto;
import com.glycowatch.backend.interfaces.dto.measurement.IngestMeasurementResponseDto;
import com.glycowatch.backend.interfaces.exception.ApiException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeasurementIngestionServiceImpl implements MeasurementIngestionService {

    private static final String DEFAULT_UNIT = "mg/dL";
    private static final BigDecimal PHYSIOLOGICAL_MIN = new BigDecimal("20.00");
    private static final BigDecimal PHYSIOLOGICAL_MAX = new BigDecimal("600.00");

    private final DeviceRepository deviceRepository;
    private final UserDeviceLinkRepository userDeviceLinkRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public IngestMeasurementResponseDto ingestMeasurement(
            String deviceIdentifierHeader,
            String deviceKeyHeader,
            IngestMeasurementRequestDto request
    ) {
        String deviceIdentifier = validateHeader(deviceIdentifierHeader, "X-DEVICE-ID");
        String deviceKey = validateHeader(deviceKeyHeader, "X-DEVICE-KEY");

        DeviceEntity device = resolveDevice(deviceIdentifier);

        if (!passwordEncoder.matches(deviceKey, device.getAuthKeyHash())) {
            throw new ApiException("DEVICE_NOT_AUTHORIZED", "Device credentials are invalid.", HttpStatus.UNAUTHORIZED);
        }

        UserDeviceLinkEntity link = userDeviceLinkRepository.findTopByDeviceIdAndActiveTrueOrderByLinkedAtDesc(device.getId())
                .orElseThrow(() -> new ApiException("DEVICE_NOT_LINKED", "Device is not linked to an active user.", HttpStatus.FORBIDDEN));

        String normalizedUnit = normalizeUnit(request.unit());
        String deduplicationHash = calculateDeduplicationHash(
                device.getId(),
                request.glucoseValue(),
                normalizedUnit,
                request.measuredAt()
        );

        if (glucoseMeasurementRepository.existsByDeduplicationHash(deduplicationHash)) {
            throw new ApiException("DUPLICATE_MEASUREMENT", "Measurement was already ingested.", HttpStatus.CONFLICT);
        }

        ValidationResult validationResult = validateMeasurement(request.glucoseValue(), normalizedUnit);
        Instant now = Instant.now();

        GlucoseMeasurementEntity measurement = GlucoseMeasurementEntity.builder()
                .user(link.getUser())
                .device(device)
                .glucoseValue(request.glucoseValue())
                .unit(normalizedUnit)
                .measuredAt(request.measuredAt())
                .receivedAt(now)
                .isValid(validationResult.valid())
                .invalidReason(validationResult.invalidReason())
                .deduplicationHash(deduplicationHash)
                .createdAt(now)
                .createdBy(device.getUniqueIdentifier())
                .build();

        GlucoseMeasurementEntity saved = glucoseMeasurementRepository.save(measurement);

        device.setLastSeenAt(now);
        device.setUpdatedAt(now);
        device.setUpdatedBy(device.getUniqueIdentifier());
        deviceRepository.save(device);

        return new IngestMeasurementResponseDto(saved.getId(), Boolean.TRUE.equals(saved.getIsValid()), saved.getInvalidReason());
    }

    private String validateHeader(String value, String headerName) {
        if (value == null || value.isBlank()) {
            throw new ApiException("MISSING_DEVICE_HEADER", headerName + " header is required.", HttpStatus.BAD_REQUEST);
        }
        return value.trim();
    }

    private DeviceEntity resolveDevice(String deviceHeaderValue) {
        try {
            Long deviceId = Long.parseLong(deviceHeaderValue);
            return deviceRepository.findByIdAndActiveTrue(deviceId)
                    .orElseGet(() -> deviceRepository.findByUniqueIdentifierIgnoreCaseAndActiveTrue(deviceHeaderValue)
                            .orElseThrow(() -> new ApiException("DEVICE_NOT_AUTHORIZED", "Device credentials are invalid.", HttpStatus.UNAUTHORIZED)));
        } catch (NumberFormatException ignored) {
            // Header is not numeric id; continue with identifier lookup.
        }
        return deviceRepository.findByUniqueIdentifierIgnoreCaseAndActiveTrue(deviceHeaderValue)
                .orElseThrow(() -> new ApiException("DEVICE_NOT_AUTHORIZED", "Device credentials are invalid.", HttpStatus.UNAUTHORIZED));
    }

    private String normalizeUnit(String rawUnit) {
        String normalized = rawUnit == null ? "" : rawUnit.trim();
        if (normalized.equalsIgnoreCase(DEFAULT_UNIT)) {
            return DEFAULT_UNIT;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private ValidationResult validateMeasurement(BigDecimal glucoseValue, String unit) {
        if (!DEFAULT_UNIT.equals(unit)) {
            return new ValidationResult(false, "Unsupported unit. Only mg/dL is accepted.");
        }
        if (glucoseValue.compareTo(PHYSIOLOGICAL_MIN) < 0 || glucoseValue.compareTo(PHYSIOLOGICAL_MAX) > 0) {
            return new ValidationResult(false, "Value is outside physiological range.");
        }
        return new ValidationResult(true, null);
    }

    private String calculateDeduplicationHash(Long deviceId, BigDecimal glucoseValue, String unit, Instant measuredAt) {
        String payload = deviceId + "|" + glucoseValue.stripTrailingZeros().toPlainString() + "|" + unit + "|" + measuredAt.toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new ApiException("HASH_CALCULATION_FAILED", "Unable to calculate deduplication hash.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private record ValidationResult(boolean valid, String invalidReason) {
    }
}
