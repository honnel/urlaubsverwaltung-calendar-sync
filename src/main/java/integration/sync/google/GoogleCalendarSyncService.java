package integration.sync.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import integration.gather.AbsenceResponse;

import integration.sync.CalendarSyncService;

import org.apache.log4j.Logger;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;

import java.security.GeneralSecurityException;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


/**
 * Daniel Hammann - <hammann@synyx.de>.
 */
@Component
public class GoogleCalendarSyncService implements CalendarSyncService {

    private static final Logger LOG = Logger.getLogger(GoogleCalendarSyncService.class);

    @Value("${google.calendar-id}")
    private String CALENDAR_ID;

    /**
     * Be sure to specify the name of your application. If the application name is {@code null} or blank, the
     * application will log a warning. Suggested format is "MyCompany-ProductName/1.0".
     */
    @Value("${google.application-name}")
    private String APPLICATION_NAME;

    /**
     * Directory to store user credentials.
     */
    private final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"),
            ".store/synyx-urlaubsverwaltung_google-calendar-store");

    /**
     * Global instance of the {@link FileDataStoreFactory}. The best practice is to make it a single globally shared
     * instance across your application.
     */
    private FileDataStoreFactory dataStoreFactory;

    /**
     * Global instance of the HTTP transport.
     */
    private HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private com.google.api.services.calendar.Calendar client;

    /**
     * Authorizes the installed application to access user's protected data.
     */
    private Credential authorize() throws IOException {

        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(GoogleCalendarSyncService.class.getResourceAsStream("/client_secrets.json")));

        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println("Enter Client ID and Secret from https://code.google.com/apis/console/?api=calendar "
                + "into calendar-cmdline-sample/src/main/resources/client_secrets.json");
            System.exit(1);
        }

        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
                clientSecrets, Collections.singleton(CalendarScopes.CALENDAR)).setDataStoreFactory(dataStoreFactory)
            .build();

        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }


    private Calendar addCalendar() throws IOException {

        GoogleApiDebugView.header("Add Calendar");

        Calendar entry = new Calendar();
        entry.setSummary("Calendar for Urlaubsverwaltung Testing");

        Calendar result = client.calendars().insert(entry).execute();
        GoogleApiDebugView.display(result);

        return result;
    }


    private Calendar updateCalendar(Calendar calendar) throws IOException {

        GoogleApiDebugView.header("Update Calendar");

        Calendar entry = new Calendar();
        entry.setSummary("Updated Calendar for Testing");

        Calendar result = client.calendars().patch(calendar.getId(), entry).execute();
        GoogleApiDebugView.display(result);

        return result;
    }


    private void showCalendars() throws IOException {

        GoogleApiDebugView.header("Show Calendars");

        CalendarList feed = client.calendarList().list().execute();
        GoogleApiDebugView.display(feed);
    }


    private Event dummyEvent() {

        Event event = new Event();
        event.setSummary("New Event");

        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 3600000);
        DateTime start = new DateTime(startDate, TimeZone.getTimeZone("UTC"));
        event.setStart(new EventDateTime().setDateTime(start));

        DateTime end = new DateTime(endDate, TimeZone.getTimeZone("UTC"));
        event.setEnd(new EventDateTime().setDateTime(end));

        return event;
    }


    private void addEvent(Calendar calendar, AbsenceResponse absence) throws IOException, ParseException {

        Event event = new Event();
        event.setSummary(String.format("Urlaub %s %s", absence.getPerson().getFirstName(),
                absence.getPerson().getLastName()));

        SimpleDateFormat formatter = new SimpleDateFormat(AbsenceResponse.DATE_FORMAT);
        DateTime dateTimeStart = new DateTime(formatter.parse(absence.getFrom()));
        DateTime dateTimeEnd = new DateTime(formatter.parse(absence.getTo()));
        event.setStart(new EventDateTime().setDateTime(dateTimeStart));
        event.setEnd(new EventDateTime().setDateTime(dateTimeEnd));

        Event result = client.events().insert(calendar.getId(), event).execute();
        GoogleApiDebugView.display(result);
    }


    private void showEvents(Calendar calendar) throws IOException {

        GoogleApiDebugView.header("Show Events");

        Events feed = client.events().list(calendar.getId()).execute();
        GoogleApiDebugView.display(feed);
    }


    private Calendar getCalendar(String id) throws IOException {

        return client.calendars().get(id).execute();
    }


    @Override
    public void syncVacationCalendar(List<AbsenceResponse> absenceResponses) {

        try {
            // initialize the transport
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            // initialize the data store factory
            dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);

            // authorization
            Credential credential = authorize();

            // set up global Calendar instance
            client = new com.google.api.services.calendar.Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();

            Calendar calendar = getCalendar(CALENDAR_ID);

            for (AbsenceResponse absence : absenceResponses) {
                addEvent(calendar, absence);
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
