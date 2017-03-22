package com.denesb.popularmovies.utilities;

import com.denesb.popularmovies.MovieListItem;
import com.denesb.popularmovies.ReviewListItem;
import com.denesb.popularmovies.VideoListItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.denesb.popularmovies.data.PopularMoviesPreferences.AUTHOR;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.CONTENT;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.KEY;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.ID;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.IMAGE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.OVERVIEW;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.RELEASE_DATE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.TITLE;
import static com.denesb.popularmovies.data.PopularMoviesPreferences.VOTE_AVERAGE;

public final class ParseJSONData {

    public static String RESULTS = "results";

    /**
     * Parses the given JSON string into List of Movies.
     * @param moviesString
     * @return movieList
     * @throws JSONException
     */
    public static List<MovieListItem> parseMoviesStringFromJSON(String moviesString)
            throws JSONException {
        JSONObject moviesJson = new JSONObject(moviesString);
        JSONArray results = null;
        if (moviesJson.has(RESULTS)) {
            results = moviesJson.getJSONArray(RESULTS);
        }
        if (null == results) return null;

        List<MovieListItem> movieList = new ArrayList<>(results.length());
        for (int i = 0; i < results.length(); i++) {
            JSONObject movie = results.getJSONObject(i);
            if (movie.has(TITLE) && movie.has(IMAGE) && movie.has(OVERVIEW) &&
                    movie.has(VOTE_AVERAGE) && movie.has(RELEASE_DATE)) {
                movieList.add(new MovieListItem(movie.getInt(ID),
                                                 movie.getString(TITLE),
                                                 movie.getString(IMAGE),
                                                 movie.getString(OVERVIEW),
                                                 movie.getString(VOTE_AVERAGE),
                                                 movie.getString(RELEASE_DATE)));
            }
        }
        return movieList;
    }

    /**
     * Parses the given JSON string into List of Videos.
     * @param videosString
     * @return videoList
     * @throws JSONException
     */
    public static List<VideoListItem> parseVideosFromJSON(String videosString)
            throws JSONException {
        JSONObject videosJson = new JSONObject(videosString);
        JSONArray results = null;
        if (videosJson.has(RESULTS)) {
            results = videosJson.getJSONArray(RESULTS);
        }
        if (null == results) return null;

        List<VideoListItem> videoList = new ArrayList<>(results.length());
        for (int i = 0; i < results.length(); i++) {
            JSONObject video = results.getJSONObject(i);
            if (video.has(KEY)) {
                videoList.add(new VideoListItem(video.getString(KEY)));
            }
        }
        return videoList;
    }

    /**
     * Parses the given JSON string into List of Reviews.
     * @param reviewsString
     * @return reviewList
     * @throws JSONException
     */
    public static List<ReviewListItem> parseReviewsFromJSON(String reviewsString)
            throws JSONException {
        JSONObject reviewsJson = new JSONObject(reviewsString);
        JSONArray results = null;
        if (reviewsJson.has(RESULTS)) {
            results = reviewsJson.getJSONArray(RESULTS);
        }
        if (null == results) return null;

        List<ReviewListItem> reviewList = new ArrayList<>(results.length());
        for (int i = 0; i < results.length(); i++) {
            JSONObject review = results.getJSONObject(i);
            if (review.has(AUTHOR) && review.has(CONTENT)) {
                reviewList.add(new ReviewListItem(
                        review.getString(AUTHOR),
                        review.getString(CONTENT)));
            }
        }
        return reviewList;
    }
}
