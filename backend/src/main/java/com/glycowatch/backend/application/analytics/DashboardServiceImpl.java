package com.glycowatch.backend.application.analytics;

import com.glycowatch.backend.domain.measurement.GlucoseMeasurementEntity;
import com.glycowatch.backend.domain.user.UserEntity;
import com.glycowatch.backend.infrastructure.persistence.repository.AlertRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.GlucoseMeasurementRepository;
import com.glycowatch.backend.infrastructure.persistence.repository.UserRepository;
import com.glycowatch.backend.interfaces.dto.analytics.DashboardResponseDto;
import com.glycowatch.backend.interfaces.exception.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final GlucoseMeasurementRepository glucoseMeasurementRepository;
    private final AlertRepository alertRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboard(String authenticatedEmail) {
        UserEntity user = resolveActiveUser(authenticatedEmail);
        Long userId = user.getId();

        GlucoseMeasurementEntity latest = null;
        BigDecimal average = BigDecimal.ZERO;
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        long alertsCount = 0L;

        try {
            latest = glucoseMeasurementRepository
                    .findFirstByUserIdAndIsValidTrueOrderByMeasuredAtDesc(userId)
                    .orElse(null);
        } catch (RuntimeException ignored) {
            latest = null;
        }

        try {
            Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
            Object[] recentStats = glucoseMeasurementRepository.getRecentStats(userId, since);
            average = castToBigDecimalOrZero(recentStats, 0);
            min = castToBigDecimalOrZero(recentStats, 1);
            max = castToBigDecimalOrZero(recentStats, 2);
        } catch (RuntimeException ignored) {
            average = BigDecimal.ZERO;
            min = BigDecimal.ZERO;
            max = BigDecimal.ZERO;
        }

        try {
            alertsCount = alertRepository.countByUserId(userId);
        } catch (RuntimeException ignored) {
            alertsCount = 0L;
        }

        DashboardResponseDto.LatestMeasurementDto latestDto = latest == null
                ? null
                : new DashboardResponseDto.LatestMeasurementDto(
                        latest.getGlucoseValue(),
                        latest.getUnit(),
                        latest.getMeasuredAt()
                );

        return new DashboardResponseDto(latestDto, average, min, max, alertsCount);
    }

    private UserEntity resolveActiveUser(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(authenticatedEmail)
                .filter(UserEntity::getActive)
                .orElseThrow(() -> new ApiException("USER_NOT_ACTIVE", "Authenticated user is not active.", HttpStatus.UNAUTHORIZED));
    }

    private BigDecimal castToBigDecimalOrZero(Object[] stats, int index) {
        if (stats == null || stats.length <= index || stats[index] == null) {
            return BigDecimal.ZERO;
        }
        Object value = stats[index];
        if (value instanceof BigDecimal decimalValue) {
            return decimalValue;
        }
        if (value instanceof Number numberValue) {
            return BigDecimal.valueOf(numberValue.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (RuntimeException ignored) {
            return BigDecimal.ZERO;
        }
    }
}
