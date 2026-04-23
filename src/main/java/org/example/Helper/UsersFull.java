package org.example.Helper;

/**
 * Is used to store the students info from the database
 * and is used by Grid component
 * */
public record UsersFull
        (
            String inscription_number,
            String name,
            String firstName,
            String level,
            String email,
            String parcours,
            int id
        )
{}
