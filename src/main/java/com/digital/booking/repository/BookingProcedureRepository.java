package com.digital.booking.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.util.UUID;

@Repository
public class BookingProcedureRepository {

    private final JdbcTemplate jdbcTemplate;

    public BookingProcedureRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void holdSeats(
            String showId,
            UUID bookingId,
            String[] seatIds) {

        CallableStatementCreator creator = connection -> {

            CallableStatement statement =
                    connection.prepareCall(
                            "{ call hold_booking_seats(?, ?, ?) }");

            statement.setString(1, showId);
            statement.setObject(2, bookingId);

            statement.setArray(
                    3,
                    connection.createArrayOf("varchar", seatIds)
            );

            return statement;
        };
        jdbcTemplate.execute(creator, CallableStatement::execute);
    }
}