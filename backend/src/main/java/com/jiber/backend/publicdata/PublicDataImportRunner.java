package com.jiber.backend.publicdata;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "jiber.public-data", name = "enabled", havingValue = "true")
public class PublicDataImportRunner implements ApplicationRunner {

    private final PublicDataImportProperties properties;
    private final PublicDataImportService importService;

    public PublicDataImportRunner(PublicDataImportProperties properties, PublicDataImportService importService) {
        this.properties = properties;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        importService.importRecentApartmentTransactions(
                new PublicDataImportCommand(properties.dryRun(), properties.limit())
        );
    }
}
