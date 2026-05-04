package org.example.CSVHelper;

import org.example.Helper.PaymentService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CSVMeth
{
    public static List<CSVRow> parseCSV(InputStream is) throws IOException
    {
        List<CSVRow> csvRowList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is)))
        {
            String line;
            reader.readLine();

            while ((line = reader.readLine()) != null)
            {

                String[] parts = line.split(",");

                if (parts.length < 3) continue;

                csvRowList.add(new CSVRow(
                        parts[0],
                        parts[1],
                        parts[2]
                ));
            }
        }

        return (csvRowList);
    }

    public static void applyCsv(List<CSVRow> rows, PaymentService paymentService, int month) throws SQLException
    {

        int currentYear = LocalDate.now().getYear();

        for (CSVRow row : rows)
        {

            String name = row.getName().toLowerCase().trim();
            String firstName = row.getFirstName().toLowerCase().trim();

            Integer id = paymentService.findStudentIdByName(name, firstName);

            if (id == null) continue; // no match

            if (isPaid(row.getStatus()))
            {

                paymentService.updatePaymentDB(
                        id,
                        currentYear,
                        month,
                        LocalDate.now()
                );
            }
        }
    }

    private static boolean isPaid(String status)
    {
        if (status == null) return false;

        return switch (status.trim().toLowerCase())
        {
            case "paid", "payé", "paye", "yes", "oui" -> true;
            default -> false;
        };
    }
}
