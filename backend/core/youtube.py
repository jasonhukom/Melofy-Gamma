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
INNERTUBE_SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"
SONGS_PARAMS = "EgWKAQIIAWoMEAMQBBAJEAoQBRAV"
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


def _search_innertube_direct(query, limit):
    payload = {
        "context": {
            "client": {
                "clientName": "WEB_REMIX",
                "clientVersion": "1.20241118.01.00",
                "hl": "en",
                "gl": "US",
            }
        },
        "query": query,
        "params": SONGS_PARAMS,
    }
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        "Origin": "https://music.youtube.com",
        "Referer": "https://music.youtube.com/",
    }
    resp = requests.post(INNERTUBE_SEARCH_URL, json=payload, headers=headers, timeout=10)
    resp.raise_for_status()
    data = resp.json()

    songs = []
    seen = set()
    tabs = data.get("contents", {}).get("tabbedSearchResultsRenderer", {}).get("tabs", [])
    for tab in tabs:
        sections = tab.get("tabRenderer", {}).get("content", {}).get("sectionListRenderer", {}).get("contents", [])
        for section in sections:
            for item in section.get("musicShelfRenderer", {}).get("contents", []):
                renderer = item.get("musicResponsiveListItemRenderer")
                if not renderer:
                    continue
                vid = (
                    renderer.get("playlistItemData", {}).get("videoId")
                    or renderer.get("overlay", {})
                    .get("musicItemThumbnailOverlayRenderer", {})
                    .get("content", {})
                    .get("musicPlayButtonRenderer", {})
                    .get("playNavigationEndpoint", {})
                    .get("watchEndpoint", {})
                    .get("videoId")
                )
                if not vid or vid in seen:
                    continue
                seen.add(vid)
                cols = renderer.get("flexColumns", [])
                title_runs = cols[0].get("musicResponsiveListItemFlexColumnRenderer", {}).get("text", {}).get("runs", []) if len(cols) > 0 else []
                artist_runs = cols[1].get("musicResponsiveListItemFlexColumnRenderer", {}).get("text", {}).get("runs", []) if len(cols) > 1 else []
                title = "".join(r.get("text", "") for r in title_runs).strip()
                artist_parts = [
                    r.get("text", "").strip()
                    for r in artist_runs
                    if r.get("navigationEndpoint", {}).get("browseEndpoint", {}).get("browseId", "").startswith("UC")
                    or r.get("navigationEndpoint", {}).get("browseEndpoint", {}).get("browseEndpointContextSupportedConfigs", {}).get("browseEndpointContextMusicConfig", {}).get("pageType") == "MUSIC_PAGE_TYPE_ARTIST"
                ]
                artist = ", ".join(p for p in artist_parts if p) or (artist_runs[0].get("text", "").strip() if artist_runs else "")
                thumbs = renderer.get("thumbnail", {}).get("musicThumbnailRenderer", {}).get("thumbnail", {}).get("thumbnails", [])
                songs.append({
                    "id": vid,
                    "title": title or "Untitled",
                    "artist": artist or "YouTube Music",
                    "thumbnail": thumbs[-1].get("url", "") if thumbs else "",
                })
                if len(songs) >= limit:
                    return songs
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
    songs = []
    try:
        songs = _search_keyless(query, max_results)
    except Exception:
        pass
    if not songs:
        try:
            songs = _search_innertube_direct(query, max_results)
        except Exception:
            if not settings.YOUTUBE_API_KEY:
                raise
    if not songs and settings.YOUTUBE_API_KEY:
        songs = _search_data_api(query, max_results)
    if songs:
        _cache[key] = (time.time(), songs)
    return songs


def random_picks(max_results=20):
    seed = random.choice(RANDOM_SEED_QUERIES)
    return seed, search(seed, max_results=max_results)
