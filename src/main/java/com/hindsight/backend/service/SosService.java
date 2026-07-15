package com.hindsight.backend.service;

import com.hindsight.backend.model.CrimeLocation;
import com.hindsight.backend.model.Sos;
import com.hindsight.backend.repository.CrimeLocationRepository;
import com.hindsight.backend.repository.SosRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SosService {

    private final SosRepository sosRepository;
    private final CrimeLocationRepository crimeLocationRepository;

    public SosService(SosRepository sosRepository, CrimeLocationRepository crimeLocationRepository) {
        this.sosRepository = sosRepository;
        this.crimeLocationRepository = crimeLocationRepository;
    }

    public Sos saveSos(Sos sos) {
        Sos saved = sosRepository.save(sos);
        log.info("SOS saved to database: {}", saved);

        // Async log crime location
        logCrimeLocationAsync(sos.getLocation(), sos.getStationId(), "HIGH", "Emergency SOS triggered");
        return saved;
    }

    @Async
    public void logCrimeLocationAsync(Sos.Location location, String stationId, String seriousness, String description) {
        try {
            CrimeLocation crimeLocation = CrimeLocation.builder()
                    .location(location)
                    .stationId(stationId)
                    .seriousness(seriousness)
                    .description(description)
                    .build();

            crimeLocationRepository.save(crimeLocation);
            log.info("Crime location logged successfully for station: {}", stationId);
        } catch (Exception e) {
            log.error("Failed to log crime location", e);
        }
    }
}
