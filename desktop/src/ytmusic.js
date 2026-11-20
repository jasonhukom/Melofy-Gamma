/**
 * Keyless YouTube Music search client using YouTube Music's InnerTube WEB_REMIX API.
 * Mirrors backend/core/youtube.py so desktop search/random picks match the web app
 * without requiring Python or any YouTube API key.
 */

const INNERTUBE_SEARCH_URL = 'https://music.youtube.com/youtubei/v1/search?prettyPrint=false';
const DATA_API_SEARCH_URL = 'https://www.googleapis.com/youtube/v3/search';
const SONGS_PARAMS = 'EgWKAQIIAWoMEAMQBBAJEAoQBRAV';
const CACHE_TTL_MS = 600 * 1000;

const RANDOM_SEED_QUERIES = [
  'pop hits', 'hip hop', 'rock anthems', 'electronic dance',
  'chill lofi beats', 'r&b soul', 'indie music', 'k-pop',
  'jazz standards', 'classical piano', 'acoustic guitar', 'reggae',
  'latin music', 'afrobeats', 'synthwave', 'folk music', 'indonesian pop',
];

const cache = new Map();

function extractTextRuns(runs) {
  if (!Array.isArray(runs)) return '';
  return runs.map((r) => (r && typeof r.text === 'string' ? r.text : '')).join('').trim();
}

function isDurationOrCount(text) {
  if (!text) return true;
  const trimmed = text.trim();
  if (/^\d+:\d{2}(:\d{2})?$/.test(trimmed)) return true;
  if (/^\d+(\.\d+)?[KMB]?\s+(plays|views|listeners)/i.test(trimmed)) return true;
  return false;
}

function extractArtists(artistRuns) {
  if (!Array.isArray(artistRuns) || !artistRuns.length) return '';

  // Prefer runs explicitly tagged as MUSIC_PAGE_TYPE_ARTIST or channel browseIds.
  const tagged = artistRuns
    .filter((r) => {
      const browse = r?.navigationEndpoint?.browseEndpoint;
      const pageType = browse?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType;
      return pageType === 'MUSIC_PAGE_TYPE_ARTIST' || (browse?.browseId && browse.browseId.startsWith('UC'));
    })
    .map((r) => (r.text || '').trim())
    .filter(Boolean);

  if (tagged.length) return tagged.join(', ');

  // Fallback: collect runs before the first bullet/duration/album separator.
  const fallback = [];
  for (const r of artistRuns) {
    const text = (r && r.text ? r.text : '').trim();
    if (!text) continue;
    if (text === '•' || text === '·') {
      if (fallback.length) break;
      continue;
    }
    if (/^(Song|Video|Single|EP|Album)$/i.test(text) && !fallback.length) continue;
    if (isDurationOrCount(text)) break;
    if (text === '&' || text === ',' || text === 'feat.' || text === 'ft.') continue;
    fallback.push(text);
  }
  return fallback.join(', ') || extractTextRuns(artistRuns);
}

function parseInnerTubeSongs(data, limit) {
  const songs = [];
  const seen = new Set();

  const tabs = data?.contents?.tabbedSearchResultsRenderer?.tabs || [];
  for (const tab of tabs) {
    const sections = tab?.tabRenderer?.content?.sectionListRenderer?.contents || [];
    for (const section of sections) {
      const items = section?.musicShelfRenderer?.contents || [];
      for (const item of items) {
        const renderer = item?.musicResponsiveListItemRenderer;
        if (!renderer) continue;

        const videoId =
          renderer?.playlistItemData?.videoId ||
          renderer?.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer
            ?.playNavigationEndpoint?.watchEndpoint?.videoId;

        if (!videoId || seen.has(videoId)) continue;
        seen.add(videoId);

        const cols = renderer?.flexColumns || [];
        const title = extractTextRuns(
          cols[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
        );
        const artist = extractArtists(
          cols[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
        );
        const thumbs = renderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];
        const thumbnail = thumbs.length ? thumbs[thumbs.length - 1].url || '' : '';

        songs.push({
          id: videoId,
          title: title || 'Untitled',
          artist: artist || 'YouTube Music',
          thumbnail,
        });

        if (songs.length >= limit) return songs;
      }
    }
  }
  return songs;
}

async function searchKeyless(query, limit = 20) {
  const response = await fetch(INNERTUBE_SEARCH_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'User-Agent':
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
      Origin: 'https://music.youtube.com',
      Referer: 'https://music.youtube.com/',
      'Accept-Language': 'en-US,en;q=0.9',
    },
    body: JSON.stringify({
      context: {
        client: {
          clientName: 'WEB_REMIX',
          clientVersion: '1.20241118.01.00',
          hl: 'en',
          gl: 'US',
        },
      },
      query,
      params: SONGS_PARAMS,
    }),
    signal: AbortSignal.timeout(10000),
  });

  if (!response.ok) {
    throw new Error(`InnerTube search failed with HTTP ${response.status}`);
  }

  const json = await response.json();
  return parseInnerTubeSongs(json, limit);
}

async function searchDataApi(query, limit = 20) {
  const apiKey = process.env.YOUTUBE_API_KEY || '';
  if (!apiKey) {
    throw new Error('YOUTUBE_API_KEY not configured');
  }
  const url = new URL(DATA_API_SEARCH_URL);
  url.searchParams.set('part', 'snippet');
  url.searchParams.set('type', 'video');
  url.searchParams.set('maxResults', String(limit));
  url.searchParams.set('videoCategoryId', '10');
  url.searchParams.set('q', query);
  url.searchParams.set('key', apiKey);

  const response = await fetch(url.toString(), { signal: AbortSignal.timeout(8000) });
  if (!response.ok) {
    throw new Error(`YouTube Data API search failed with HTTP ${response.status}`);
  }
  const json = await response.json();
  return (json.items || [])
    .map((item) => {
      const id = item?.id?.videoId;
      const sn = item?.snippet || {};
      if (!id) return null;
      const t = sn.thumbnails || {};
      return {
        id,
        title: sn.title || '',
        artist: sn.channelTitle || '',
        thumbnail: (t.medium || t.default || {}).url || '',
      };
    })
    .filter(Boolean);
}

async function search(query, maxResults = 20) {
  const normalized = String(query || '').trim();
  if (!normalized) return [];

  const cacheKey = `${normalized.toLowerCase()}::${maxResults}`;
  const hit = cache.get(cacheKey);
  if (hit && Date.now() - hit.ts < CACHE_TTL_MS) {
    return hit.songs;
  }

  let songs;
  try {
    songs = await searchKeyless(normalized, maxResults);
    if (!songs.length && process.env.YOUTUBE_API_KEY) {
      songs = await searchDataApi(normalized, maxResults);
    }
  } catch (err) {
    if (process.env.YOUTUBE_API_KEY) {
      songs = await searchDataApi(normalized, maxResults);
    } else {
      throw err;
    }
  }

  if (songs.length) {
    cache.set(cacheKey, { ts: Date.now(), songs });
  }
  return songs;
}

async function randomPicks(maxResults = 20) {
  const seed = RANDOM_SEED_QUERIES[Math.floor(Math.random() * RANDOM_SEED_QUERIES.length)];
  const songs = await search(seed, maxResults);
  return { seed, results: songs };
}

module.exports = {
  RANDOM_SEED_QUERIES,
  parseInnerTubeSongs,
  search,
  randomPicks,
};
