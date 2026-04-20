package com.myrtletrip.bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.myrtletrip.bootstrap.service.LegacyWorkbookImportService;

@Component
public class LegacyImportRunner implements CommandLineRunner {

    private final LegacyWorkbookImportService importService;

    public LegacyImportRunner(LegacyWorkbookImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Toggle this on only when you want to run the import.
        boolean enabled = false;

        if (!enabled) {
            return;
        }

        importService.importPlayersCoursesAndScores("C:\\Users\\mwiel\\OneDrive\\Documents\\Myrtle Beach\\2025\\Myrtle Beach 2025 Scoring.xlsm");
        System.out.println("Legacy workbook import complete.");
    }
}