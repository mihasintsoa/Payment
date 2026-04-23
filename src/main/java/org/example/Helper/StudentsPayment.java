package org.example.Helper;

import org.example.Helper.Payment;
import org.example.Helper.Students;

import java.util.HashSet;
import java.util.Set;

public class StudentsPayment
{
    private Students student;
    private Set<Payment> payments = new HashSet<>();

    public StudentsPayment(Students student) {
        this.student = student;
    }

    public void addPayment(Payment payment) {
        if (payment != null) {
            payments.add(payment);
        }
    }

    public Students getStudent() {
        return student;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public int getID() { return student.id(); }
    public String getName() { return student.name(); }
    public String getFirstName() { return student.firstName(); }
}
