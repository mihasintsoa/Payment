package org.example.CSVHelper;

import org.example.Checking.Checking;
import org.example.Helper.PaymentService;
import org.example.Helper.StudentsPayment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static void applyCsv(List<CSVRow> rows, PaymentService paymentService) throws SQLException
    {

        Map<String, StudentsPayment> index = new HashMap<>();

        for (StudentsPayment sp : Checking.studentsPaymentList)
        {
            String key = (sp.getName().toLowerCase().trim() + "_" + sp.getFirstName()).toLowerCase().trim();
            index.put(key, sp);
        }

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        for (CSVRow row : rows)
        {

            String key = row.getName().toLowerCase().trim() + "_" + row.getFirstName().toLowerCase().trim();
            System.out.println(key);

            StudentsPayment student = index.get(key);

            if (student == null) continue; // no match

            if (row.getStatus().trim().equals("paid"))
            {

                paymentService.updatePaymentDB(
                        student.getID(),
                        currentYear,
                        currentMonth,
                        LocalDate.now()
                );
            }
        }
    }
}
