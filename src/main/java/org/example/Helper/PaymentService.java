package org.example.Helper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

import java.time.LocalDate;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PaymentService
{
    private Connection con;

    public PaymentService(Connection con)
    {
        this.con = con;
    }

    public void updatePaymentDB(int studentId, int year, int month, LocalDate date) throws SQLException
    {
        try (PreparedStatement ps = con.prepareStatement(
        """
                    INSERT INTO payment (
                        student_id, paid_month, paid_year, payment_date, status)
                    VALUES (?, ?, ?, ?, ?)
            """))
        {
            ps.setInt(1, studentId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            ps.setDate(4, Date.valueOf(date));
            ps.setString(5, "PAID");
            ps.executeUpdate();
        }
    }


    public Map<Integer, Set<Integer>> getAllPaymentsForYear(int year)
    {
        Map<Integer, Set<Integer>> payments = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT student_id, paid_month FROM payment WHERE paid_year = ? AND status = 'PAID'"))
        {
            ps.setInt(1, year);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int studentId = rs.getInt("student_id");
                int month = rs.getInt("paid_month");
                payments.computeIfAbsent(studentId, k -> new HashSet<>()).add(month);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return (payments);
    }

    public void deleteDB(int student_id, Set<Integer> paid_monthToDelete)
    {
        String sql = "DELETE FROM payment WHERE student_id = ? AND paid_month = ?";

        try (PreparedStatement ps = con.prepareStatement(sql))
        {
            for (Integer month : paid_monthToDelete)
            {
                ps.setInt(1, student_id);
                ps.setInt(2, month);
                ps.executeUpdate();
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }



}
