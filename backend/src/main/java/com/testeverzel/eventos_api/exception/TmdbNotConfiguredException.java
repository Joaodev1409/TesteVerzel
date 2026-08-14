package com.testeverzel.eventos_api.exception;

public class TmdbNotConfiguredException extends RuntimeException {

    public TmdbNotConfiguredException() {
        super("TMDb integration is not configured (set TMDB_API_KEY)");
    }
}
