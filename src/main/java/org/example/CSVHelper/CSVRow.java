package org.example.CSVHelper;

public class CSVRow
{
    private String name,
                   firstName,
                   status;

    public CSVRow(String n , String f, String stat)
    {
        name = n;
        firstName = f;
        status = stat;
    }

    public String getFirstName()
    {
        return firstName;
    }

    public String getName()
    {
        return name;
    }

    public String getStatus()
    {
        return status;
    }
}
