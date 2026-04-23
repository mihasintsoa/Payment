package org.example.Initialiser;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@WebListener
public class DataInitialiser implements ServletContextListener
{
    public static String url = "jdbc:h2:~/testdb";
    public static String user = "sa";
    public static String pwd = "";
    @Override
    public void contextInitialized(ServletContextEvent sc)
    {

        try
        {
            Connection con = DriverManager.getConnection(url, user, pwd);
            Statement statement = con.createStatement();

            statement.execute("""
                        create table if not exists users_full(
                            id int primary key auto_increment,
                            inscription_number VARCHAR(30) unique not null,
                            name VARCHAR(150),
                            firstname VARCHAR(200),
                            level VARCHAR(5),
                            email VARCHAR(60),
                            parcours VARCHAR(90)
                        );
                    
                    """);

            statement.execute("""
                        create table if not exists payment(
                            id int primary key auto_increment,
                            student_id int not null references users_full(id),
                            paid_month int not null,
                            paid_year int not null,
                            payment_date date,
                            status VARCHAR(15)
                        );
                    
                    """);

            InputStream is = getClass().getClassLoader().getResourceAsStream("users_full.csv");
            if (is == null)
                throw new FileNotFoundException("Could not find users_full.csv in resources folder");

            LoadCSV.loadFromUsersFull(is, con);

            is = getClass().getClassLoader().getResourceAsStream("payment.csv");
            if (is == null)
                throw new FileNotFoundException("Could not find users_full.csv in resources folder");

            LoadCSV.loadFromPayment(is, con);

            con.commit();
            con.close();

        }catch (SQLException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sc) {

    }
}
