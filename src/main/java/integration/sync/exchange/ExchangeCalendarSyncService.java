package integration.sync.exchange;

import integration.gather.AbsenceResponse;

import integration.sync.CalendarSyncService;

import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.service.SendInvitationsMode;
import microsoft.exchange.webservices.data.core.service.folder.CalendarFolder;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.Appointment;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import microsoft.exchange.webservices.data.search.FindFoldersResults;
import microsoft.exchange.webservices.data.search.FolderView;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;


/**
 * Daniel Hammann - <hammann@synyx.de>.
 */
@Component
public class ExchangeCalendarSyncService implements CalendarSyncService {

    @Value("${ews.email}")
    private String EMAIL_ADDRESS;

    @Value("${ews.password}")
    private String PASSWORD;

    @Value("${ews.calendar-name}")
    private String CALENDAR_NAME;

    @Value("${ews.autodiscover-url}")
    private String EWS_URL;

    private ExchangeService exchangeService;

    @Override
    public void syncVacationCalendar(List<AbsenceResponse> absenceResponses) {

        try {
            if (exchangeService == null) {
                exchangeService = connectViaExchangeAutodiscover(EMAIL_ADDRESS, PASSWORD);
            }

            // TODO: Check if folder already exist
            CalendarFolder calendar = findCalendar(CALENDAR_NAME, exchangeService);

            if (calendar == null) {
                calendar = createCalendar(CALENDAR_NAME, exchangeService);
            }

            for (AbsenceResponse absenceResponse : absenceResponses) {
                addAppointment(absenceResponse, calendar, exchangeService);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private CalendarFolder findCalendar(String searchedCalendarName, ExchangeService service) throws Exception {

        FindFoldersResults findCalendarRootFolderResults = service.findFolders(WellKnownFolderName.Calendar,
                new FolderView(Integer.MAX_VALUE));

        Folder calendarRootFolder = findCalendarRootFolderResults.getFolders().get(0);
        FindFoldersResults calendars = calendarRootFolder.findFolders(new FolderView(Integer.MAX_VALUE));

        for (Folder calendar : calendars.getFolders()) {
            if (calendar.getDisplayName().equals(searchedCalendarName)) {
                return (CalendarFolder) calendar;
            }
        }

        // No sub calendar found using root calendar
        return (CalendarFolder) calendarRootFolder;
    }


    private CalendarFolder createCalendar(String calendarName, ExchangeService service) throws Exception {

        CalendarFolder folder = new CalendarFolder(service);
        folder.setDisplayName(calendarName);
        folder.save(WellKnownFolderName.Calendar);

        return CalendarFolder.bind(service, folder.getId());
    }


    private void addAppointment(AbsenceResponse absence, CalendarFolder calendar, ExchangeService exchangeService)
        throws Exception {

        Appointment appointment = new Appointment(exchangeService);
        appointment.setSubject(String.format("Urlaub %s %s", absence.getPerson().getFirstName(),
                absence.getPerson().getLastName()));

        SimpleDateFormat formatter = new SimpleDateFormat(AbsenceResponse.DATE_FORMAT);
        Date startDate = formatter.parse(absence.getFrom());
        Date endDate = formatter.parse(absence.getTo());

        appointment.setStart(startDate);
        appointment.setEnd(endDate);
        appointment.setIsAllDayEvent(true);
        appointment.getRequiredAttendees().add("hammann@synyx.de");

        appointment.save(calendar.getId(), SendInvitationsMode.SendToAllAndSaveCopy);
    }


    private void createDummyAppointment(ExchangeService exchangeService) throws Exception {

        Appointment appointment = new Appointment(exchangeService);
        appointment.setSubject("Test Appointment Urlaubsverwaltung");
        appointment.setBody(MessageBody.getMessageBodyFromText("Body Text"));

        Date startDate = new Date();
        Date endDate = new Date(startDate.getTime() + 3600000);

        appointment.setStart(startDate);
        appointment.setEnd(endDate);

        appointment.save();
    }


    private ExchangeService connectViaExchangeAutodiscover(String email, String password) throws Exception {

        ExchangeService service = new ExchangeService();
        service.setCredentials(new WebCredentials(email, password));
        service.autodiscoverUrl(email, new RedirectionUrlCallback());
        service.setTraceEnabled(true);

        return service;
    }

    private static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {

        @Override
        public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {

            return redirectionUrl.toLowerCase().startsWith("https://");
        }
    }
}
