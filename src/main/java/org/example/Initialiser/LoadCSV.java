package org.example.Initialiser;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;

public class LoadCSV
{

    public static void loadFromUsersFull(InputStream csvPath, Connection con)
            throws SQLException, IOException
    {
        if (tableHasData(con, "users_full")) return ;

        String insertSQL = """
                INSERT INTO users_full (
                    inscription_number, name, firstname, level, email, parcours)
                     VALUES (?, ?, ?, ?, ?, ?);
                """;

        try(
                BufferedReader br = new BufferedReader(new InputStreamReader(csvPath));
                PreparedStatement ps = con.prepareStatement(insertSQL)
        )
        {
            con.setAutoCommit(false);
            br.readLine(); //remove the header

            String line;

            String insc_number ;
            String name ;
            String firstName ;
            String level ;
            String email ;
            String parcours ;
            int id;

            while ((line = br.readLine()) != null)
            {
                String[] v = line.split(";", -1);
                insc_number = v[0].trim();
                name = v[1].trim();
                firstName = v[2].trim();
                level = v[3].trim();
                email = v[4].trim();
                parcours = v[5].trim();
                id = Integer.parseInt(v[6].trim());


                ps.setString(1, insc_number);
                ps.setString(2, name);
                ps.setString(3, firstName);
                ps.setString(4, level);
                ps.setString(5, email);
                ps.setString(6, parcours);
                //ps.setInt(7, id);

                ps.addBatch();
            }

            ps.executeBatch(); //send all batch to the database in one go
            con.commit();
        }
    }

    public static void loadFromPayment(InputStream csvPath, Connection con)
            throws SQLException, IOException
    {
        if (tableHasData(con, "payment")) return;

        String insertSQL =
                """
                    INSERT INTO payment(
                        student_id,
                        paid_month,
                        paid_year,
                        payment_date,
                        status
                    )
                    VALUES(?, ?, ?, ?, ?);
                """;

        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(csvPath));
                PreparedStatement ps = con.prepareStatement(insertSQL)
        )
        {
            con.setAutoCommit(false);
            br.readLine();

            String line,
                    status;
            int id,
                    student_id,
                    paid_month,
                    paid_year;
            LocalDate payment_date;

            while ((line = br.readLine()) != null)
            {
                String[] v = line.split(",", -1);
                id = Integer.parseInt(v[0].trim());
                student_id = Integer.parseInt(v[1].trim());
                paid_month = Integer.parseInt(v[2].trim());
                paid_year = Integer.parseInt(v[3].trim());
                payment_date = LocalDate.parse(v[4].trim());
                status = v[5].trim();

                //ps.setInt(1, id);
                ps.setInt(1, student_id);
                ps.setInt(2, paid_month);
                ps.setInt(3, paid_year);
                ps.setDate(4,Date.valueOf(payment_date));
                ps.setString(5, status);

                ps.addBatch();
            }

            ps.executeBatch();
            con.commit();

        }
    }

    private static boolean tableHasData(Connection conn, String tableName) throws SQLException
    {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName))
        {
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
