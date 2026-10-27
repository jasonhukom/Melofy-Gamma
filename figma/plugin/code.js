// Lyra Melofy — Native Figma Plugin Generator
// Creates all 6 Android Phone Screens (Home, Trending, Search, Downloads, Profile, Full Player)
// as editable Figma frames with Space Grotesk & Manrope typography and the Dark Yellow palette.

const COLORS = {
  bgVoid: { r: 10 / 255, g: 9 / 255, b: 5 / 255 },         // #0A0905
  navBg: { r: 14 / 255, g: 13 / 255, b: 8 / 255 },         // #0E0D08
  surfaceCard: { r: 22 / 255, g: 20 / 255, b: 14 / 255 },  // #16140E
  surfaceElevated: { r: 30 / 255, g: 27 / 255, b: 18 / 255 }, // #1E1B12
  heroBanner: { r: 28 / 255, g: 24 / 255, b: 10 / 255 },   // #1C180A
  border: { r: 42 / 255, g: 38 / 255, b: 26 / 255 },       // #2A261A
  yellow: { r: 250 / 255, g: 204 / 255, b: 21 / 255 },     // #FACC15
  cyan: { r: 254 / 255, g: 240 / 255, b: 138 / 255 },      // #FEF08A
  white: { r: 1, g: 1, b: 1 },                              // #FFFFFF
  muted: { r: 161 / 255, g: 160 / 255, b: 150 / 255 },     // #A1A096
  green: { r: 16 / 255, g: 185 / 255, b: 129 / 255 },      // #10B981
  red: { r: 239 / 255, g: 68 / 255, b: 68 / 255 }          // #EF4444
};

async function loadFonts() {
  const fonts = [
    { family: "Inter", style: "Regular" },
    { family: "Inter", style: "Medium" },
    { family: "Inter", style: "Bold" }
  ];
  for (const f of fonts) {
    await figma.loadFontAsync(f);
  }
}

function solid(color, opacity = 1) {
  return [{ type: "SOLID", color, opacity }];
}

function createRect(name, x, y, w, h, fill, radius = 0, stroke = null, strokeWeight = 1) {
  const r = figma.createRectangle();
  r.name = name;
  r.x = x;
  r.y = y;
  r.resize(w, h);
  r.fills = solid(fill);
  r.cornerRadius = radius;
  if (stroke) {
    r.strokes = solid(stroke);
    r.strokeWeight = strokeWeight;
  }
  return r;
}

function createText(text, x, y, size, style, color) {
  const t = figma.createText();
  t.fontName = { family: "Inter", style };
  t.characters = text;
  t.fontSize = size;
  t.fills = solid(color);
  t.x = x;
  t.y = y;
  return t;
}

function addFooterAndMiniPlayer(frame, activeIndex, showMiniPlayer = true) {
  if (showMiniPlayer) {
    const mini = figma.createFrame();
    mini.name = "Mini Player Bar";
    mini.x = 8;
    mini.y = 706;
    mini.resize(374, 62);
    mini.cornerRadius = 14;
    mini.fills = solid(COLORS.surfaceElevated);
    mini.strokes = solid(COLORS.border);
    mini.strokeWeight = 1;

    mini.appendChild(createRect("Mini Cover", 8, 8, 46, 46, COLORS.surfaceCard, 10, COLORS.yellow, 1));
    mini.appendChild(createText("Midnight Horizon", 66, 12, 14, "Bold", COLORS.white));
    mini.appendChild(createText("Lofi Sunset • Lo-Fi", 66, 32, 12, "Regular", COLORS.muted));
    mini.appendChild(createText("▶", 304, 18, 18, "Bold", COLORS.yellow));
    mini.appendChild(createText("⏭", 340, 18, 18, "Bold", COLORS.white));
    mini.appendChild(createRect("Progress Track", 0, 60, 374, 2, COLORS.border));
    mini.appendChild(createRect("Progress Fill", 0, 60, 160, 2, COLORS.yellow));
    frame.appendChild(mini);
  }

  const nav = figma.createFrame();
  nav.name = "Bottom Navigation Footer (5 Buttons)";
  nav.x = 0;
  nav.y = 772;
  nav.resize(390, 72);
  nav.fills = solid(COLORS.navBg);
  nav.strokes = solid(COLORS.border);
  nav.strokeWeight = 1;

  const tabs = ["Home", "Trending", "Search", "Downloads", "Profile"];
  const colW = 390 / 5;
  tabs.forEach((label, idx) => {
    const cx = idx * colW;
    if (idx === 2) {
      const circle = figma.createEllipse();
      circle.name = "Center Search Button (62dp)";
      circle.x = cx + (colW - 62) / 2;
      circle.y = 5;
      circle.resize(62, 62);
      circle.fills = solid(COLORS.yellow);
      circle.strokes = solid(activeIndex === 2 ? COLORS.white : COLORS.border);
      circle.strokeWeight = activeIndex === 2 ? 3 : 2;
      nav.appendChild(circle);
      nav.appendChild(createText("🔍", cx + 28, 24, 20, "Bold", COLORS.bgVoid));
    } else {
      const isSelected = activeIndex !== 2 && activeIndex === idx;
      const tint = isSelected ? COLORS.yellow : COLORS.white;
      nav.appendChild(createRect(`${label} Icon`, cx + 28, 14, 22, 22, tint, 6));
      nav.appendChild(createText(label, cx + 16, 42, 11, idx === 0 ? "Bold" : "Medium", tint));
    }
  });

  frame.appendChild(nav);
}

function addTrackRow(frame, y, rank, title, artistGenre, isPlaying, isOffline, isLiked) {
  const row = figma.createFrame();
  row.name = `Track Row — ${title}`;
  row.x = 16;
  row.y = y;
  row.resize(358, 64);
  row.cornerRadius = 12;
  row.fills = solid(COLORS.surfaceCard);
  row.strokes = solid(COLORS.border);
  row.strokeWeight = 1;

  let offsetX = 8;
  if (rank !== null) {
    row.appendChild(createText(`#${rank}`, 10, 22, 14, "Bold", COLORS.yellow));
    offsetX = 38;
  }

  row.appendChild(createRect("Cover Art", offsetX, 8, 48, 48, COLORS.surfaceElevated, 8));
  row.appendChild(createText(title, offsetX + 60, 12, 14, "Bold", isPlaying ? COLORS.yellow : COLORS.white));
  row.appendChild(createText(artistGenre, offsetX + 60, 34, 12, "Regular", COLORS.muted));
  if (isOffline) {
    row.appendChild(createText("• Offline", offsetX + 185, 34, 11, "Bold", COLORS.green));
  }
  row.appendChild(createText("★", 292, 20, 18, "Bold", isLiked ? COLORS.red : COLORS.muted));
  row.appendChild(createText(isOffline ? "✓" : "⬇", 326, 20, 18, "Bold", isOffline ? COLORS.cyan : COLORS.white));
  frame.appendChild(row);
}

async function run() {
  await loadFonts();

  const screens = [
    { name: "01 — Home", activeTab: 0 },
    { name: "02 — Trending", activeTab: 1 },
    { name: "03 — Search & Explore", activeTab: 2 },
    { name: "04 — Downloads (Offline Library)", activeTab: 3 },
    { name: "05 — Profile & Settings", activeTab: 4 },
    { name: "06 — Full Player Dialog", activeTab: 0 }
  ];

  const created = [];

  screens.forEach((scr, i) => {
    const frame = figma.createFrame();
    frame.name = scr.name;
    frame.x = i * 440;
    frame.y = 0;
    frame.resize(390, 844);
    frame.fills = solid(COLORS.bgVoid);

    if (i === 0) {
      // Home
      frame.appendChild(createText("Home", 54, 22, 22, "Bold", COLORS.white));
      frame.appendChild(createRect("Offline Ready Pill", 268, 20, 102, 28, COLORS.surfaceElevated, 14, COLORS.border, 1));
      frame.appendChild(createText("• Offline Ready", 280, 27, 11, "Bold", COLORS.green));

      // Hero Banner
      frame.appendChild(createRect("Hero Banner", 20, 68, 350, 92, COLORS.heroBanner, 16, COLORS.yellow, 1.5));
      frame.appendChild(createText("LYRA MELOFY", 38, 84, 11, "Bold", COLORS.yellow));
      frame.appendChild(createText("Your Home Music Hub", 38, 102, 18, "Bold", COLORS.white));
      frame.appendChild(createText("Listen online or offline to your saved songs.", 38, 128, 12, "Regular", COLORS.muted));

      // Quick Picks
      frame.appendChild(createText("Quick Picks", 20, 182, 16, "Bold", COLORS.white));
      const picks = [
        ["Midnight Horizon", "Lofi Sunset"],
        ["Neon Synthwave", "Aetherium"],
        ["Acoustic Coffee", "Luna & Pines"]
      ];
      picks.forEach((p, idx) => {
        const cx = 20 + idx * 152;
        frame.appendChild(createRect(`Card ${p[0]}`, cx, 212, 140, 182, COLORS.surfaceCard, 12, COLORS.border, 1));
        frame.appendChild(createRect("Cover", cx + 8, 220, 124, 124, COLORS.surfaceElevated, 10));
        frame.appendChild(createText(p[0], cx + 8, 352, 13, "Bold", COLORS.white));
        frame.appendChild(createText(p[1], cx + 8, 370, 11, "Regular", COLORS.muted));
      });

      frame.appendChild(createText("Recommended For You", 20, 416, 16, "Bold", COLORS.white));
      addTrackRow(frame, 446, null, "Midnight Horizon", "Lofi Sunset • Lo-Fi", true, true, true);
      addTrackRow(frame, 518, null, "Neon Synthwave Drift", "Aetherium • Electronic", false, false, false);
      addTrackRow(frame, 590, null, "Acoustic Morning Coffee", "Luna & The Pines • Pop", false, true, false);
      addFooterAndMiniPlayer(frame, 0, true);
    } else if (i === 1) {
      // Trending
      frame.appendChild(createText("Trending Charts", 56, 20, 22, "Bold", COLORS.white));
      frame.appendChild(createText("Top played tracks this week", 56, 48, 12, "Regular", COLORS.muted));
      addTrackRow(frame, 84, 1, "Midnight Horizon", "Lofi Sunset • Lo-Fi", true, true, true);
      addTrackRow(frame, 156, 2, "Neon Synthwave Drift", "Aetherium • Electronic", false, false, false);
      addTrackRow(frame, 228, 3, "Acoustic Morning Coffee", "Luna & The Pines • Pop", false, true, false);
      addTrackRow(frame, 300, 4, "Ethereal Echoes", "Solaris Wave • Ambient", false, false, false);
      addTrackRow(frame, 372, 5, "Urban Rhythm Groove", "Metro Beatmakers • Hip-Hop", false, false, true);
      addTrackRow(frame, 444, 6, "Velvet Afterdark", "Kira Sound • R&B", false, false, false);
      addFooterAndMiniPlayer(frame, 1, true);
    } else if (i === 2) {
      // Search
      frame.appendChild(createText("Search & Explore", 56, 22, 22, "Bold", COLORS.white));
      frame.appendChild(createRect("Search Input Box", 20, 68, 350, 52, COLORS.surfaceCard, 12, COLORS.border, 1));
      frame.appendChild(createText("Search songs, artists, genres...", 56, 86, 14, "Regular", COLORS.muted));
      frame.appendChild(createText("Browse Categories", 20, 140, 16, "Bold", COLORS.white));

      const cats = [
        ["Chill Lo-Fi", COLORS.surfaceElevated, COLORS.border],
        ["Synth Wave", COLORS.heroBanner, COLORS.yellow],
        ["Acoustic Vibes", COLORS.surfaceCard, COLORS.border],
        ["Offline Tracks", COLORS.surfaceElevated, COLORS.border],
        ["Hip-Hop Beats", COLORS.heroBanner, COLORS.yellow],
        ["Ambient & Zen", COLORS.surfaceCard, COLORS.border]
      ];
      cats.forEach((c, idx) => {
        const col = idx % 2;
        const row = Math.floor(idx / 2);
        const x = 20 + col * 181;
        const y = 172 + row * 92;
        frame.appendChild(createRect(c[0], x, y, 169, 80, c[1], 14, c[2], 1.5));
        frame.appendChild(createText(c[0], x + 14, y + 48, 14, "Bold", COLORS.white));
      });
      addFooterAndMiniPlayer(frame, 2, true);
    } else if (i === 3) {
      // Downloads
      frame.appendChild(createText("Offline Music Library", 56, 20, 22, "Bold", COLORS.white));
      frame.appendChild(createText("Playable anytime without internet connection", 56, 48, 12, "Regular", COLORS.muted));
      frame.appendChild(createRect("Storage Card", 20, 84, 350, 110, COLORS.heroBanner, 16, COLORS.yellow, 1.5));
      frame.appendChild(createText("• OFFLINE STORAGE ACTIVE", 40, 104, 10, "Bold", COLORS.green));
      frame.appendChild(createText("2 Tracks Saved", 268, 102, 12, "Regular", COLORS.muted));
      frame.appendChild(createRect("Btn Play All", 40, 134, 150, 42, COLORS.yellow, 12));
      frame.appendChild(createText("Play All", 90, 147, 14, "Bold", COLORS.bgVoid));
      frame.appendChild(createRect("Btn Shuffle", 200, 134, 150, 42, COLORS.heroBanner, 12, COLORS.yellow, 1.5));
      frame.appendChild(createText("Shuffle", 252, 147, 14, "Bold", COLORS.white));

      frame.appendChild(createText("Downloaded Tracks", 20, 214, 18, "Bold", COLORS.white));
      addTrackRow(frame, 248, null, "Midnight Horizon", "Lofi Sunset • Lo-Fi", true, true, true);
      addTrackRow(frame, 320, null, "Acoustic Morning Coffee", "Luna & The Pines • Pop", false, true, false);
      addFooterAndMiniPlayer(frame, 3, true);
    } else if (i === 4) {
      // Profile
      frame.appendChild(createRect("User Profile Card", 20, 20, 350, 310, COLORS.surfaceElevated, 14, COLORS.border, 1));
      const av = figma.createEllipse();
      av.x = 149; av.y = 40; av.resize(92, 92);
      av.fills = solid(COLORS.surfaceElevated);
      av.strokes = solid(COLORS.yellow);
      av.strokeWeight = 2;
      frame.appendChild(av);
      frame.appendChild(createText("Anonymous Listener", 104, 146, 20, "Bold", COLORS.white));
      frame.appendChild(createText("Guest Mode • Offline Listening Available", 84, 172, 12, "Regular", COLORS.muted));

      frame.appendChild(createRect("Stats Row", 40, 200, 310, 56, COLORS.bgVoid, 10));
      frame.appendChild(createText("2", 86, 210, 18, "Bold", COLORS.red));
      frame.appendChild(createText("Liked Songs", 60, 232, 11, "Regular", COLORS.muted));
      frame.appendChild(createText("2", 190, 210, 18, "Bold", COLORS.cyan));
      frame.appendChild(createText("Offline Tracks", 160, 232, 11, "Regular", COLORS.muted));
      frame.appendChild(createText("320k", 276, 210, 18, "Bold", COLORS.yellow));
      frame.appendChild(createText("Audio Quality", 262, 232, 11, "Regular", COLORS.muted));

      frame.appendChild(createRect("Btn Connect Account", 40, 270, 310, 44, COLORS.yellow, 14));
      frame.appendChild(createText("Connect YouTube / Google Account", 76, 284, 14, "Bold", COLORS.bgVoid));

      frame.appendChild(createText("App Settings", 20, 350, 16, "Bold", COLORS.white));
      frame.appendChild(createRect("Settings Box", 20, 382, 350, 108, COLORS.surfaceCard, 12, COLORS.border, 1));
      frame.appendChild(createText("High Quality Audio (320 kbps)", 36, 402, 14, "Regular", COLORS.white));
      frame.appendChild(createRect("Switch 1", 310, 398, 44, 24, COLORS.yellow, 12));
      frame.appendChild(createRect("Divider", 36, 436, 318, 1, COLORS.border));
      frame.appendChild(createText("Download Over Wi-Fi Only", 36, 454, 14, "Regular", COLORS.white));
      frame.appendChild(createRect("Switch 2", 310, 450, 44, 24, COLORS.yellow, 12));
      addFooterAndMiniPlayer(frame, 4, true);
    } else if (i === 5) {
      // Full Player Dialog
      frame.appendChild(createText("⌄", 24, 32, 24, "Bold", COLORS.white));
      frame.appendChild(createText("PLAYING FROM LIBRARY", 132, 32, 10, "Bold", COLORS.muted));
      frame.appendChild(createText("Chill Vibes Vol. 1", 142, 48, 13, "Bold", COLORS.white));

      frame.appendChild(createRect("Full Album Cover", 55, 110, 280, 280, COLORS.surfaceCard, 20, COLORS.yellow, 1.5));

      frame.appendChild(createText("Midnight Horizon", 24, 440, 22, "Bold", COLORS.white));
      frame.appendChild(createText("Lofi Sunset", 24, 470, 15, "Regular", COLORS.muted));
      frame.appendChild(createText("OFFLINE", 112, 473, 9, "Bold", COLORS.green));
      frame.appendChild(createText("★", 308, 450, 22, "Bold", COLORS.red));
      frame.appendChild(createText("✓", 344, 450, 22, "Bold", COLORS.cyan));

      frame.appendChild(createRect("Seekbar Track", 24, 524, 342, 4, COLORS.border, 2));
      frame.appendChild(createRect("Seekbar Progress", 24, 524, 148, 4, COLORS.yellow, 2));
      frame.appendChild(createText("01:42", 24, 538, 12, "Regular", COLORS.muted));
      frame.appendChild(createText("06:12", 334, 538, 12, "Regular", COLORS.muted));

      frame.appendChild(createText("🔀", 42, 608, 20, "Bold", COLORS.yellow));
      frame.appendChild(createText("⏮", 106, 604, 26, "Bold", COLORS.white));
      const playCircle = figma.createEllipse();
      playCircle.x = 163; playCircle.y = 584; playCircle.resize(64, 64);
      playCircle.fills = solid(COLORS.yellow);
      frame.appendChild(playCircle);
      frame.appendChild(createText("⏸", 185, 602, 24, "Bold", COLORS.bgVoid));
      frame.appendChild(createText("⏭", 262, 604, 26, "Bold", COLORS.white));
      frame.appendChild(createText("🔁", 328, 608, 20, "Bold", COLORS.muted));
    }

    figma.currentPage.appendChild(frame);
    created.push(frame);
  });

  figma.viewport.scrollAndZoomIntoView(created);
  figma.closePlugin("Created all 6 Lyra Melofy phone screens on the Figma canvas!");
}

run();
