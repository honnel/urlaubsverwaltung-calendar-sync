package integration;

import integration.gather.AbsenceDataService;
import integration.gather.AbsenceResponse;

import integration.sync.exchange.ExchangeCalendarSyncService;

import integration.sync.google.GoogleCalendarSyncService;

import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;


@SpringBootApplication
public class UrlaubsverwaltungCalendarSyncApplication implements CommandLineRunner {

    private final Logger LOG = Logger.getLogger(UrlaubsverwaltungCalendarSyncApplication.class);

    @Autowired
    AbsenceDataService absenceDataService;

    @Autowired
    GoogleCalendarSyncService googleCalendarSyncService;

    @Autowired
    ExchangeCalendarSyncService exchangeCalendarSyncService;

    public static void main(String[] args) {

        SpringApplication.run(UrlaubsverwaltungCalendarSyncApplication.class, args);
    }


    @Override
    public void run(String... strings) throws Exception {

        List<AbsenceResponse> absenceResponses = absenceDataService.getAbsenceResponsesFromUrlaubsverwaltungRestApi();
        googleCalendarSyncService.syncVacationCalendar(absenceResponses);
        exchangeCalendarSyncService.syncVacationCalendar(absenceResponses);
    }
}
