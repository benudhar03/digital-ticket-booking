CREATE OR REPLACE PROCEDURE add_movie(
    IN p_movie_id UUID,
    IN p_title VARCHAR,
    IN p_genre VARCHAR,
    IN p_language VARCHAR,
    IN p_duration_minutes INTEGER,
    IN p_release_date DATE
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Validate movie ID
    IF p_movie_id IS NULL THEN
        RAISE EXCEPTION 'Movie id cannot be null'
            USING ERRCODE = '22004';
END IF;

    -- Validate title
    IF p_title IS NULL OR TRIM(p_title) = '' THEN
        RAISE EXCEPTION 'Movie title cannot be empty'
            USING ERRCODE = '22023';
END IF;

    -- Validate genre
    IF p_genre IS NULL OR TRIM(p_genre) = '' THEN
        RAISE EXCEPTION 'Movie genre cannot be empty'
            USING ERRCODE = '22023';
END IF;

    -- Validate language
    IF p_language IS NULL OR TRIM(p_language) = '' THEN
        RAISE EXCEPTION 'Movie language cannot be empty'
            USING ERRCODE = '22023';
END IF;

    -- Validate duration
    IF p_duration_minutes IS NULL OR p_duration_minutes <= 0 THEN
        RAISE EXCEPTION 'Movie duration must be greater than zero'
            USING ERRCODE = '22023';
END IF;

    -- Insert movie
INSERT INTO movie (
    movie_id,
    title,
    genre,
    language,
    duration_minutes,
    release_date,
    created_at
)
VALUES (
           p_movie_id,
           TRIM(p_title),
           TRIM(p_genre),
           TRIM(p_language),
           p_duration_minutes,
           p_release_date,
           CURRENT_TIMESTAMP
       );
END;
$$;