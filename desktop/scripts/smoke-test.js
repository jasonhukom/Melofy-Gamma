const assert = require('assert');
const { createAppServer } = require('../src/server');
const { search, randomPicks } = require('../src/ytmusic');

async function main() {
  console.log('[1/3] Testing keyless YouTube Music search...');
  const searchResults = await search('lofi hip hop radio', 10);
  assert(Array.isArray(searchResults) && searchResults.length > 0, 'Expected non-empty search results');
  assert(searchResults[0].id && searchResults[0].title, 'Expected valid song fields');
  console.log(`  OK: search returned ${searchResults.length} songs (first: "${searchResults[0].title}" by ${searchResults[0].artist} [${searchResults[0].id}])`);

  console.log('[2/3] Testing random picks...');
  const randomData = await randomPicks(10);
  assert(randomData.seed && Array.isArray(randomData.results) && randomData.results.length > 0, 'Expected random picks');
  console.log(`  OK: randomPicks(seed="${randomData.seed}") returned ${randomData.results.length} songs`);

  console.log('[3/3] Testing embedded HTTP server...');
  const srv = createAppServer();
  const baseUrl = await srv.start(0, '127.0.0.1');
  try {
    const htmlRes = await fetch(`${baseUrl}/`);
    assert.strictEqual(htmlRes.status, 200);
    const html = await htmlRes.text();
    assert(!html.includes('{%'), 'Django template tags should be resolved');
    assert(html.includes('/static/js/player.js'), 'Expected static asset script path');

    const apiSearchRes = await fetch(`${baseUrl}/api/search?q=daft+punk`);
    assert.strictEqual(apiSearchRes.status, 200);
    const apiSearchJson = await apiSearchRes.json();
    assert(apiSearchJson.results.length > 0, 'Expected /api/search results');

    const apiRandomRes = await fetch(`${baseUrl}/api/random`);
    assert.strictEqual(apiRandomRes.status, 200);
    const apiRandomJson = await apiRandomRes.json();
    assert(apiRandomJson.results.length > 0, 'Expected /api/random results');

    const cfgRes = await fetch(`${baseUrl}/api/config`);
    assert.strictEqual(cfgRes.status, 200);

    console.log(`  OK: HTTP server responded cleanly at ${baseUrl}`);
  } finally {
    await srv.stop();
  }
}

main().catch((err) => {
  console.error('SMOKE TEST FAILED:', err);
  process.exit(1);
});
