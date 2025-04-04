package org.synyx.urlaubsverwaltung.application.specialleave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SpecialLeaveSettingsRepository extends JpaRepository<SpecialLeaveSettingsEntity, Long> {

    @Override
    List<SpecialLeaveSettingsEntity> findAll();

    @Override
    List<SpecialLeaveSettingsEntity> findAllById(Iterable<Long> ids);

    Optional<SpecialLeaveSettingsEntity> findAllByMessageKey(String messageKey);
}
