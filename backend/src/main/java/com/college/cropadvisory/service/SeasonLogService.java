package com.college.cropadvisory.service;

import com.college.cropadvisory.dto.SeasonLogRequest;
import com.college.cropadvisory.exception.NotFoundException;
import com.college.cropadvisory.model.entity.Farm;
import com.college.cropadvisory.model.entity.SeasonLog;
import com.college.cropadvisory.model.entity.User;
import com.college.cropadvisory.repository.SeasonLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeasonLogService {

    private final SeasonLogRepository seasonLogRepository;
    private final FarmService farmService;

    public SeasonLogService(SeasonLogRepository seasonLogRepository, FarmService farmService) {
        this.seasonLogRepository = seasonLogRepository;
        this.farmService = farmService;
    }

    /** Records a season against a farm the caller has already been authorized for. */
    public SeasonLog logSeason(Farm farm, SeasonLogRequest request) {
        SeasonLog log = new SeasonLog();
        log.setFarm(farm);
        applyRequest(log, request);
        return seasonLogRepository.save(log);
    }

    public List<SeasonLog> getLogsByFarm(Farm farm) {
        return seasonLogRepository.findByFarmOrderBySowingDateDesc(farm);
    }

    /**
     * Loads a log for reading, authorizing the caller against the log's own farm. The path's
     * {@code farmId} must also match, so a caller cannot pair their own farm with a foreign log id.
     */
    public SeasonLog getLogForFarm(Long farmId, Long logId, User user) {
        Farm farm = farmService.getFarmReadableBy(farmId, user);
        return requireLogBelongsToFarm(getLog(logId), farm);
    }

    public SeasonLog updateLog(Long farmId, Long logId, User user, SeasonLogRequest request) {
        SeasonLog log = getOwnedLog(farmId, logId, user);
        applyRequest(log, request);
        return seasonLogRepository.save(log);
    }

    public void deleteLog(Long farmId, Long logId, User user) {
        seasonLogRepository.delete(getOwnedLog(farmId, logId, user));
    }

    public SeasonLog getLog(Long logId) {
        return seasonLogRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Season log not found"));
    }

    private void applyRequest(SeasonLog log, SeasonLogRequest request) {
        log.setCropPlanted(request.getCropPlanted());
        log.setSowingDate(request.getSowingDate());
        log.setOutcomeNotes(request.getOutcomeNotes());
    }

    /**
     * Loads a log and verifies the caller owns its farm. Ownership is delegated to
     * {@link FarmService#getFarmOwnedBy} so the rule stays single-sourced across modules.
     */
    private SeasonLog getOwnedLog(Long farmId, Long logId, User user) {
        Farm farm = farmService.getFarmOwnedBy(farmId, user);
        return requireLogBelongsToFarm(getLog(logId), farm);
    }

    /**
     * Rejects a log that belongs to a different farm than the one named in the request, so the
     * farm id from the URL can never be used to reach a log under another farm.
     */
    private SeasonLog requireLogBelongsToFarm(SeasonLog log, Farm farm) {
        if (!log.getFarm().getId().equals(farm.getId())) {
            throw new NotFoundException("Season log not found");
        }
        return log;
    }
}
