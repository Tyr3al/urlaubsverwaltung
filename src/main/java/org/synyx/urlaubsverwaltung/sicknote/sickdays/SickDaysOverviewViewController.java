package org.synyx.urlaubsverwaltung.sicknote.sickdays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedata;
import org.synyx.urlaubsverwaltung.person.basedata.PersonBasedataService;
import org.synyx.urlaubsverwaltung.web.DateFormatAware;
import org.synyx.urlaubsverwaltung.web.FilterPeriod;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.StringUtils.hasText;
import static org.synyx.urlaubsverwaltung.person.Role.BOSS;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;
import static org.synyx.urlaubsverwaltung.person.Role.INACTIVE;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.SECOND_STAGE_AUTHORITY;
import static org.synyx.urlaubsverwaltung.sicknote.sickdays.SickDays.SickDayType.TOTAL;
import static org.synyx.urlaubsverwaltung.sicknote.sickdays.SickDays.SickDayType.WITH_AUB;

/**
 * Controller for overview about the sick days of all users.
 */
@Controller
@RequestMapping("/web")
public class SickDaysOverviewViewController {

    private final SickDaysStatisticsService sickDaysStatisticsService;
    private final PersonBasedataService personBasedataService;
    private final DepartmentService departmentService;
    private final PersonService personService;
    private final DateFormatAware dateFormatAware;
    private final Clock clock;

    @Autowired
    public SickDaysOverviewViewController(SickDaysStatisticsService sickDaysStatisticsService, PersonBasedataService personBasedataService,
                                          DepartmentService departmentService, PersonService personService, DateFormatAware dateFormatAware, Clock clock) {

        this.sickDaysStatisticsService = sickDaysStatisticsService;
        this.personBasedataService = personBasedataService;
        this.departmentService = departmentService;
        this.personService = personService;
        this.dateFormatAware = dateFormatAware;
        this.clock = clock;
    }

    @PreAuthorize("hasAnyAuthority('OFFICE', 'SICK_NOTE_VIEW')")
    @PostMapping("/sickdays/filter")
    public String filterSickNotes(@ModelAttribute("period") FilterPeriod period, Errors errors, RedirectAttributes redirectAttributes) {

        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute("filterPeriodIncorrect", true);
        }

        final String startDateIsoString = dateFormatAware.formatISO(period.getStartDate());
        final String endDateISoString = dateFormatAware.formatISO(period.getEndDate());

        return "redirect:/web/sickdays?from=" + startDateIsoString + "&to=" + endDateISoString;
    }

    @PreAuthorize("hasAnyAuthority('OFFICE', 'SICK_NOTE_VIEW')")
    @GetMapping("/sickdays")
    public String periodsSickNotes(@RequestParam(value = "from", defaultValue = "") String from,
                                   @RequestParam(value = "to", defaultValue = "") String to,
                                   Model model) {

        final LocalDate startDate = dateFormatAware.parse(from).orElse(null);
        final LocalDate endDate = dateFormatAware.parse(to).orElse(null);
        final FilterPeriod period = new FilterPeriod(startDate, endDate);

        final Person signedInUser = personService.getSignedInUser();

        final List<Person> persons = getMembersOfPersons(signedInUser);
        final List<SickDaysDetailedStatistics> sickDaysStatistics = sickDaysStatisticsService.getAll(signedInUser, period.getStartDate(), period.getEndDate());

        final Map<Person, SickDays> sickDaysByPerson = new HashMap<>();
        final Map<Person, SickDays> childSickDaysByPerson = new HashMap<>();
        sickDaysStatistics.forEach(statistic -> {
            final Person person = statistic.getPerson();
            sickDaysByPerson.put(person, statistic.getSickDays(startDate, endDate));
            childSickDaysByPerson.put(person, statistic.getChildSickDays(startDate, endDate));
        });
        persons.forEach(person -> {
            sickDaysByPerson.putIfAbsent(person, new SickDays());
            childSickDaysByPerson.putIfAbsent(person, new SickDays());
        });

        final Map<Integer, String> personnelNumberOfPersons = getPersonnelNumbersOfPersons(persons);
        final List<SickDaysOverviewDto> sickDaysOverviewDtos = persons.stream()
                .map(person -> toSickDaysOverviewDto(person, sickDaysByPerson::get, childSickDaysByPerson::get, personnelNumberOfPersons::get))
                .collect(toList());

        model.addAttribute("sickDaysStatistics", sickDaysOverviewDtos);
        model.addAttribute("showPersonnelNumberColumn", !personnelNumberOfPersons.isEmpty());

        model.addAttribute("today", LocalDate.now(clock));
        model.addAttribute("from", period.getStartDate());
        model.addAttribute("to", period.getEndDate());
        model.addAttribute("period", period);

        return "thymeleaf/sicknote/sick_days";
    }

    private static SickDaysOverviewDto toSickDaysOverviewDto(Person person,
                                                             Function<Person, SickDays> sickDaysSupplier,
                                                             Function<Person, SickDays> childSickDaysSupplier,
                                                             Function<Integer, String> personnelNumberSupplier) {

        final SickDays sickDays = sickDaysSupplier.apply(person);
        final SickDays childSickDays = childSickDaysSupplier.apply(person);

        return SickDaysOverviewDto.builder()
            .personId(person.getId())
            .personnelNumber(personnelNumberSupplier.apply(person.getId()))
            .personFirstName(person.getFirstName())
            .personLastName(person.getLastName())
            .personNiceName(person.getNiceName())
            .personAvatarUrl(person.getGravatarURL())
            .amountSickDays(sickDays.getDays().get(TOTAL.name()))
            .amountSickDaysWithAUB(sickDays.getDays().get(WITH_AUB.name()))
            .amountChildSickDays(childSickDays.getDays().get(TOTAL.name()))
            .amountChildSickNoteDaysWithAUB(childSickDays.getDays().get(WITH_AUB.name()))
            .build();
    }

    private Map<Integer, String> getPersonnelNumbersOfPersons(List<Person> persons) {
        return persons.stream()
            .map(person -> personBasedataService.getBasedataByPersonId(person.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(personBasedata -> hasText(personBasedata.getPersonnelNumber()))
            .collect(toMap(basedata -> basedata.getPersonId().getValue(), PersonBasedata::getPersonnelNumber));
    }

    private List<Person> getMembersOfPersons(Person signedInUser) {

        if (signedInUser.hasRole(BOSS) || signedInUser.hasRole(OFFICE)) {
            return personService.getActivePersons();
        }

        final List<Person> membersForDepartmentHead = signedInUser.hasRole(DEPARTMENT_HEAD)
            ? departmentService.getMembersForDepartmentHead(signedInUser)
            : List.of();

        final List<Person> memberForSecondStageAuthority = signedInUser.hasRole(SECOND_STAGE_AUTHORITY)
            ? departmentService.getMembersForSecondStageAuthority(signedInUser)
            : List.of();

        return Stream.concat(memberForSecondStageAuthority.stream(), membersForDepartmentHead.stream())
            .filter(person -> !person.hasRole(INACTIVE))
            .distinct()
            .sorted(comparing(Person::getFirstName).thenComparing(Person::getLastName))
            .collect(toList());
    }
}
