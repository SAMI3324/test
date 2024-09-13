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
import java.util.ArrayList;
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
    private static final Duration SLOT_DURATION = Duration.ofMinutes(15); // Durée des sous-créneaux

    public List<Availability> findByPractitionerId(Integer practitionerId) {
        return availabilityRepository.findByPractitionerId(practitionerId);
    }

    public List<Availability> generateAvailabilities(Integer practitionerId) {
        // Récupérer les créneaux horaires (TimeSlots) du praticien
        List<TimeSlot> timeSlots = timeSlotRepository.findByPractitionerId(practitionerId);
        // Récupérer les disponibilités existantes pour éviter les doublons
        List<Availability> availabilities = proAvailabilityService.findByPractitionerId(practitionerId);
        // Récupérer les rendez-vous du praticien
        List<Appointment> appointments = proAppointmentService.findByPractitionerId(practitionerId);

        // Convertir chaque TimeSlot en Availability
        for (TimeSlot timeSlot : timeSlots) {
            LocalDateTime start = timeSlot.getStartDate();
            LocalDateTime end = timeSlot.getEndDate();

            // Diviser le créneau en sous-créneaux de 15 minutes
            while (start.isBefore(end)) {
                LocalDateTime slotEnd = start.plus(SLOT_DURATION);
                if (slotEnd.isAfter(end)) {
                    slotEnd = end;
                }

                // Finaliser les variables pour les utiliser dans la lambda
                final LocalDateTime finalStart = start;
                final LocalDateTime finalSlotEnd = slotEnd;

                // Vérifier si ce créneau est occupé par un rendez-vous
                boolean conflictsWithAppointments = appointments.stream().anyMatch(appointment ->
                        (appointment.getStartDate().isBefore(finalSlotEnd) && appointment.getEndDate().isAfter(finalStart))
                );

                // Ajouter la disponibilité seulement si elle ne chevauche pas les rendez-vous existants
                if (!conflictsWithAppointments) {
                    // Vérification : s'assurer que le créneau ne chevauche pas d'autres créneaux existants
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

                // Passer au créneau suivant
                start = slotEnd;
            }
        }

        // Sauvegarder toutes les disponibilités générées dans le repository
        availabilityRepository.saveAll(availabilities);

        return availabilities;
    }

}
