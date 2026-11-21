CREATE TABLE movie (
                       movie_id UUID PRIMARY KEY,
                       title VARCHAR(200) NOT NULL,
                       genre VARCHAR(50) NOT NULL,
                       language VARCHAR(50) NOT NULL,
                       duration_minutes INTEGER NOT NULL,
                       release_date DATE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT chk_movie_duration
                           CHECK (duration_minutes > 0)
);

CREATE INDEX idx_movie_title ON movie (LOWER(title));
CREATE INDEX idx_movie_genre ON movie (genre);