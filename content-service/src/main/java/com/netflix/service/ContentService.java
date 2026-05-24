package com.netflix.service;

import com.netflix.dto.MovieRequest;
import com.netflix.dto.MovieResponse;
import com.netflix.mapper.MovieMapper;
import com.netflix.model.Genre;
import com.netflix.model.Movie;
import com.netflix.model.VideoStatus;
import com.netflix.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {

    private final MovieRepository movieRepository;

    /**
     * Add a new Movie to the catalog
     * Video is not uploaded yet at this stage
     */
    public MovieResponse addMovie(MovieRequest request) {

        log.info("Adding new movie: {}", request.getTitle());

        Movie movie = new Movie();
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDirector(request.getDirector());
        movie.setCast(request.getCast());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setRating(request.getRating());
        movie.setThumbnailUrl(request.getThumbnailUrl());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setVideoStatus(VideoStatus.PENDING);

        Movie saveMovie = movieRepository.save(movie);
        log.info("Saved movie Id: {}", saveMovie.getId());

        return MovieMapper.toDTO(saveMovie);
    }

    /**
     * Get all movies in the catalog
     */
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(MovieMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get movies by genre
     */
    public List<MovieResponse> getMoviesByGenre(Genre genre) {
        return movieRepository.findByGenre(genre)
                .stream()
                .map(MovieMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get movie by id
     */
    public MovieResponse getMovieById(String movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));
        return MovieMapper.toDTO(movie);
    }

    /**
     * Get movie by title
     */
    public List<MovieResponse> searchMovies(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(MovieMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Update movie video details after upload.
     * Called by Video Service after successful S3 upload
     */
    public void updateVideoKey(String movieId, String videoKey) {
        log.info("Updating video key for movie id: {}", movieId);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));

        movie.setVideoKey(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        movieRepository.save(movie);
    }

    /**
     * Update movie HLS URL after encoding is complete.
     * Called by encoding service after FFmpeg processing.
     */
    public void updateHlsUrl(String movieId, String hlsUrl) {
        log.info("Updating HLS url for movie id: {}", movieId);

        Movie movie =  movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));

        movie.setHlsUrl(hlsUrl);
        movie.setVideoStatus(VideoStatus.READY);

        movieRepository.save(movie);
        log.info("Movie {} is now ready for streaming", movieId);
    }

    public void updateVideoStatus(String movieId, VideoStatus videoStatus) {
        Movie movie =  movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));
        movie.setVideoStatus(videoStatus);
        movieRepository.save(movie);
    }
}
