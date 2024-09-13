package com.maiia.pro.service;

import com.maiia.pro.entity.Availability;
import com.maiia.pro.entity.TimeSlot;
import com.maiia.pro.entity.Appointment;
import com.maiia.pro.repository.AvailabilityRepository;
import com.maiia.pro.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProAvailabilityService {

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private ProAvailabilityService proAvailabilityService;
    @Autowired
    private ProAppointmentService proAppointmentService;
    private static final Duration Crenau = Duration.ofMinutes(15);

    public List<Availability> findByPractitionerId(Integer practitionerId) {
        return availabilityRepository.findByPractitionerId(practitionerId);
    }

    public List<Availability> generateAvailabilities(Integer practitionerId) {

        List<TimeSlot> timeSlots = timeSlotRepository.findByPractitionerId(practitionerId);
        List<Availability> availabilities = proAvailabilityService.findByPractitionerId(practitionerId);
        List<Appointment> appointments = proAppointmentService.findByPractitionerId(practitionerId);

        for (TimeSlot timeSlot : timeSlots) {
            LocalDateTime start = timeSlot.getStartDate();
            LocalDateTime end = timeSlot.getEndDate();

            while (start.isBefore(end)) {
                LocalDateTime finCrenau = start.plus(Crenau);
                if (finCrenau.isAfter(end)) {
                    finCrenau = end;
                }
                final LocalDateTime finalStart = start;
                final LocalDateTime finalSlotEnd = finCrenau;

                boolean conflictsWithAppointments = appointments.stream().anyMatch(appointment ->
                        (appointment.getStartDate().isBefore(finalSlotEnd) && appointment.getEndDate().isAfter(finalStart))
                );

                if (!conflictsWithAppointments) {
                    boolean overlaps = availabilities.stream().anyMatch(existing ->
                            (existing.getStartDate().isBefore(finalSlotEnd) && existing.getEndDate().isAfter(finalStart))
                    );

                    if (!overlaps) {
                        Availability availability = new Availability();
                        availability.setPractitionerId(practitionerId);
                        availability.setStartDate(finalStart);
                        availability.setEndDate(finalSlotEnd);
                        availabilities.add(availability);
                    }
                }

                start = finCrenau;
            }
        }
        availabilityRepository.saveAll(availabilities);
        return availabilities;
    }

}
