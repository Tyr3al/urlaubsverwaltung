package org.synyx.urlaubsverwaltung.calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link AbsenceTimeConfiguration}.
 */
class AbsenceTimeConfigurationTest {

    private TimeSettings timeSettings;

    @BeforeEach
    void setUp() {
        timeSettings = new TimeSettings();
        timeSettings.setWorkDayBeginHour(8);
        timeSettings.setWorkDayBeginMinute(15);
        timeSettings.setWorkDayEndHour(16);
        timeSettings.setWorkDayEndMinute(30);
    }

    @Test
    void ensureCorrectMorningStartTime() {
        final AbsenceTimeConfiguration timeConfiguration = new AbsenceTimeConfiguration(timeSettings);
        assertThat(timeConfiguration.getMorningStartTime()).isEqualTo(LocalTime.of(8,15));
    }

    @Test
    void ensureCorrectMorningEndTime() {
        final AbsenceTimeConfiguration timeConfiguration = new AbsenceTimeConfiguration(timeSettings);
        assertThat(timeConfiguration.getMorningEndTime()).isEqualTo(LocalTime.of(12,22, 30));
    }

    @Test
    void ensureCorrectNoonStartTime() {
        final AbsenceTimeConfiguration timeConfiguration = new AbsenceTimeConfiguration(timeSettings);
        assertThat(timeConfiguration.getNoonStartTime()).isEqualTo(LocalTime.of(12,22, 30));
    }

    @Test
    void ensureCorrectNoonEndTime() {
        final AbsenceTimeConfiguration timeConfiguration = new AbsenceTimeConfiguration(timeSettings);
        assertThat(timeConfiguration.getNoonEndTime()).isEqualTo(LocalTime.of(16,30));
    }
}
