CREATE OR REPLACE PROCEDURE add_movie(
    IN p_movie_id         UUID,
    IN p_title            VARCHAR,
    IN p_genre            VARCHAR,
    IN p_language         VARCHAR,
    IN p_duration_minutes INTEGER,
    IN p_release_date     DATE
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_movie_id IS NULL THEN
        RAISE EXCEPTION 'Movie id cannot be null'
            USING ERRCODE = '22004';
END IF;

    IF p_title IS NULL OR TRIM(p_title) = '' THEN
        RAISE EXCEPTION 'Movie title cannot be empty'
            USING ERRCODE = '22023';
END IF;

    IF p_duration_minutes IS NULL OR p_duration_minutes <= 0 THEN
        RAISE EXCEPTION 'Movie duration must be greater than zero'
            USING ERRCODE = '22023';
END IF;

INSERT INTO movie (
    movie_id,
    title,
    genre,
    language,
    duration_minutes,
    release_date
)
VALUES (
           p_movie_id,
           p_title,
           p_genre,
           p_language,
           p_duration_minutes,
           p_release_date
       );
END;
$$;