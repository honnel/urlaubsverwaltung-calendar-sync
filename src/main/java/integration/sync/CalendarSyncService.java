package integration.sync;

import integration.gather.AbsenceResponse;

import java.util.List;


/**
 * Daniel Hammann - <hammann@synyx.de>.
 */
public interface CalendarSyncService {

    void syncVacationCalendar(List<AbsenceResponse> absenceResponses);
}
