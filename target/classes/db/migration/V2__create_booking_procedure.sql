CREATE OR REPLACE PROCEDURE hold_booking_seats(
    IN p_show_id VARCHAR,
    IN p_booking_id UUID,
    IN p_seat_ids VARCHAR[]
)
LANGUAGE plpgsql
AS $$
DECLARE
affected_rows INTEGER;
BEGIN

UPDATE show_seat
SET status = 'HELD',
    booking_id = p_booking_id
WHERE show_id = p_show_id
  AND seat_id = ANY(p_seat_ids)
  AND status = 'AVAILABLE';

GET DIAGNOSTICS affected_rows = ROW_COUNT;

IF affected_rows <> CARDINALITY(p_seat_ids) THEN
        RAISE EXCEPTION 'One or more seats are unavailable';
END IF;

END;
$$;