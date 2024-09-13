package com.maiia.pro.service;

import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
import com.maiia.pro.exception.AvailabilityConflictException;
import com.maiia.pro.repository.AppointmentRepository;
import com.maiia.pro.repository.AvailabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProAppointmentService {
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private AvailabilityRepository availabilityRepository;

    public Appointment find(String appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }

    public List<Appointment> findByPractitionerId(Integer practitionerId) {
        return appointmentRepository.findByPractitionerId(practitionerId);
    }

    @Transactional
    public Appointment createAppointment(Appointment appointment) {
        // Vérifier la disponibilité avant de créer un rendez-vous
        boolean isSlotAvailable = checkAvailability(appointment);
        if (!isSlotAvailable) {
            throw new AvailabilityConflictException("Le créneau n'est pas disponible pour ce rendez-vous.");
        }

        // Créer le rendez-vous
        Appointment createdAppointment = appointmentRepository.save(appointment);

        // Mettre à jour les disponibilités du médecin
        updateAvailabilities(appointment);

        return createdAppointment;
    }

    private boolean checkAvailability(Appointment appointment) {
        // Logique pour vérifier si le créneau est disponible
        List<Availability> conflictingAvailabilities = availabilityRepository.findByPractitionerId(
                appointment.getPractitionerId()
        );

        return !conflictingAvailabilities.isEmpty();
    }

    private void updateAvailabilities(Appointment appointment) {
        List<Availability> availabilities = availabilityRepository.findByPractitionerId(
                appointment.getPractitionerId()
        );

        for (Availability availability : availabilities) {
            boolean overlapsStart = availability.getStartDate().isBefore(appointment.getEndDate()) &&
                    availability.getEndDate().isAfter(appointment.getStartDate());
            boolean completelyInside = !availability.getStartDate().isBefore(appointment.getStartDate()) &&
                    !availability.getEndDate().isAfter(appointment.getEndDate());

            // Diviser ou ajuster la disponibilité si elle chevauche partiellement le rendez-vous
            if (overlapsStart && !completelyInside) {
                if (availability.getStartDate().isBefore(appointment.getStartDate())) {
                    // Ajuster la fin de la disponibilité existante
                    availability.setEndDate(appointment.getStartDate());
                    availabilityRepository.save(availability);
                }
                if (availability.getEndDate().isAfter(appointment.getEndDate())) {
                    // Ajuster le début de la disponibilité existante ou créer une nouvelle disponibilité après le rendez-vous
                    availability.setStartDate(appointment.getEndDate());
                    availabilityRepository.save(availability);
                }
            } else if (completelyInside) {
                // Supprimer la disponibilité car elle est complètement englobée par le rendez-vous
                availabilityRepository.delete(availability);
            }
        }
    }

}
