"""Music search for Lyra Melofy.

Primary source is ytmusicapi (the unofficial YouTube Music API that apps like
Metrolist use): it needs NO API key. If YOUTUBE_API_KEY is set, the official
YouTube Data API v3 is used as a fallback when the keyless call fails.
Results are cached briefly so repeat searches stay fast and cheap.
"""
import random
import time

import requests
from django.conf import settings

SEARCH_URL = "https://www.googleapis.com/youtube/v3/search"
CACHE_TTL = 600
_cache = {}
_ytm = None

RANDOM_SEED_QUERIES = [
    "pop hits", "hip hop", "rock anthems", "electronic dance",
    "chill lofi beats", "r&b soul", "indie music", "k-pop",
    "jazz standards", "classical piano", "acoustic guitar", "reggae",
    "latin music", "afrobeats", "synthwave", "folk music", "indonesian pop",
]


def _client():
    global _ytm
    if _ytm is None:
        from ytmusicapi import YTMusic
        _ytm = YTMusic()
    return _ytm


def _search_keyless(query, limit):
    rows = _client().search(query, filter="songs", limit=limit)
    songs = []
    for row in rows:
        vid = row.get("videoId")
        if not vid:
            continue
        thumbs = row.get("thumbnails") or []
        songs.append({
            "id": vid,
            "title": row.get("title", ""),
            "artist": ", ".join(a.get("name", "") for a in row.get("artists") or []),
            "thumbnail": thumbs[-1].get("url", "") if thumbs else "",
        })
    return songs


def _search_data_api(query, limit):
    params = {"part": "snippet", "type": "video", "maxResults": limit,
              "videoCategoryId": "10", "q": query, "key": settings.YOUTUBE_API_KEY}
    resp = requests.get(SEARCH_URL, params=params, timeout=8)
    resp.raise_for_status()
    songs = []
    for item in resp.json().get("items", []):
        vid = item.get("id", {}).get("videoId")
        sn = item.get("snippet", {})
        if not vid:
            continue
        t = sn.get("thumbnails", {})
        songs.append({
            "id": vid, "title": sn.get("title", ""), "artist": sn.get("channelTitle", ""),
            "thumbnail": (t.get("medium") or t.get("default") or {}).get("url", ""),
        })
    return songs


def search(query, max_results=20):
    key = (query.strip().lower(), max_results)
    hit = _cache.get(key)
    if hit and time.time() - hit[0] < CACHE_TTL:
        return hit[1]
    try:
        songs = _search_keyless(query, max_results)
    except Exception:
        if not settings.YOUTUBE_API_KEY:
            raise
        songs = _search_data_api(query, max_results)
    if songs:
        _cache[key] = (time.time(), songs)
    return songs


def random_picks(max_results=20):
    seed = random.choice(RANDOM_SEED_QUERIES)
    return seed, search(seed, max_results=max_results)
