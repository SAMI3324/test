package com.maiia.pro.service;

import com.maiia.pro.entity.Appointment;
import com.maiia.pro.entity.Availability;
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

        Appointment createdAppointment = appointmentRepository.save(appointment);
        updateAvailabilities(appointment);
        return createdAppointment;
    }

    private void updateAvailabilities(Appointment appointment) {
        List<Availability> availabilities = availabilityRepository.findByPractitionerId(
                appointment.getPractitionerId()
        );

        for (Availability availability : availabilities) {
            boolean completelyInside = !availability.getStartDate().isBefore(appointment.getStartDate()) &&
                    !availability.getEndDate().isAfter(appointment.getEndDate());

            boolean overlapsStart = availability.getStartDate().isBefore(appointment.getEndDate()) &&
                    availability.getEndDate().isAfter(appointment.getStartDate());
            if (overlapsStart && !completelyInside) {
                if (availability.getEndDate().isAfter(appointment.getEndDate())) {
                    availability.setStartDate(appointment.getEndDate());
                    availabilityRepository.save(availability);
                }
                if (availability.getStartDate().isBefore(appointment.getStartDate())) {
                    availability.setEndDate(appointment.getStartDate());
                    availabilityRepository.save(availability);
                }
            } else if (completelyInside) {
                availabilityRepository.delete(availability);
            }
        }
    }

}
