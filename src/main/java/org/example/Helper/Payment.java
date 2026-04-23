package org.example.Helper;

import java.time.LocalDate;

public record Payment
        (
                int student_id,
                int paid_month,
                int paid_year,
                LocalDate payment_date,
                String status
        )
{
}
