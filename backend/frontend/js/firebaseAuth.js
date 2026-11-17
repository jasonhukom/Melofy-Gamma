/* firebaseAuth.js - Google sign-in + "Connect your YouTube account".
   The Firebase web config is fetched from /api/config (set via environment
   variables on the server), so nothing is hardcoded here. If the server has
   not been configured yet, the buttons say so instead of silently doing
   nothing. Exposes window.FirebaseAuth for the non-module app code. */
const SDK = "https://www.gstatic.com/firebasejs/10.12.2/";
const IS_ANDROID = /MelofyAndroidApp/.test(navigator.userAgent);

let sdk = null, auth = null, provider = null;
let serverConfig = { firebase: null, youtubeLinking: false };
let currentUser = null;
const listeners = [];
function emit() { listeners.forEach(cb => { try { cb(currentUser); } catch (e) {} }); }
function toast(msg) { if (window.UI) window.UI.toast(msg); }
function asUser(d) { return { uid: d.uid, email: d.email, displayName: d.displayName, youtubeConnected: d.youtubeConnected }; }

async function loadFirebase() {
  try { serverConfig = await (await fetch("/api/config", { credentials: "same-origin" })).json(); } catch (e) {}
  if (!serverConfig.firebase) return false;
  try {
    const [appMod, authMod] = await Promise.all([import(SDK + "firebase-app.js"), import(SDK + "firebase-auth.js")]);
    sdk = authMod;
    auth = authMod.getAuth(appMod.initializeApp(serverConfig.firebase));
    provider = new authMod.GoogleAuthProvider();
    return true;
  } catch (e) { return false; }
}

async function refreshSessionFromServer() {
  const data = await (await fetch("/api/auth/me", { credentials: "same-origin" })).json();
  currentUser = data.signedIn ? asUser(data) : null;
  emit();
}

async function establishSession(user) {
  const idToken = await user.getIdToken();
  const res = await fetch("/api/auth/session", {
    method: "POST", headers: { "Content-Type": "application/json" },
    credentials: "same-origin", body: JSON.stringify({ idToken })
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || "Sign-in failed");
  currentUser = asUser(data);
  emit();
}

async function signIn() {
  if (!auth) { toast("Sign-in is not set up on this server yet"); return; }
  try {
    if (IS_ANDROID) { await sdk.signInWithRedirect(auth, provider); return; }
    const result = await sdk.signInWithPopup(auth, provider);
    await establishSession(result.user);
    toast("Signed in");
  } catch (e) {
    if (e && (e.code === "auth/popup-closed-by-user" || e.code === "auth/cancelled-popup-request")) return;
    if (e && e.code === "auth/unauthorized-domain") { toast("This domain is not authorized in Firebase yet"); return; }
    toast((e && e.message) || "Could not sign in. Please try again.");
  }
}

async function signOutEverywhere() {
  try { if (auth) await sdk.signOut(auth); } catch (e) {}
  await fetch("/api/auth/logout", { method: "POST", credentials: "same-origin" });
  currentUser = null;
  emit();
}

function connectYouTube() {
  if (!serverConfig.youtubeLinking) { toast("YouTube linking is not set up on this server yet"); return; }
  window.location.href = `/api/youtube/connect?platform=${IS_ANDROID ? "android" : "web"}`;
}

async function disconnectYouTube() {
  await fetch("/api/youtube/disconnect", { method: "POST", credentials: "same-origin" });
  if (currentUser) currentUser.youtubeConnected = false;
  emit();
}

window.completeAndroidHandoff = async function (code) {
  const res = await fetch(`/api/auth/exchange?code=${encodeURIComponent(code)}`, { credentials: "same-origin" });
  const data = await res.json();
  if (res.ok) { currentUser = asUser(data); emit(); toast("Signed in"); }
};

async function init() {
  const ready = await loadFirebase();
  if (ready) {
    try {
      const result = await sdk.getRedirectResult(auth);
      if (result && result.user) {
        if (IS_ANDROID) {
          const idToken = await result.user.getIdToken();
          const res = await fetch("/api/auth/mobile-handoff", {
            method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ idToken })
          });
          const data = await res.json();
          if (res.ok) window.location.href = `melofy://auth/complete?code=${encodeURIComponent(data.code)}`;
          return;
        }
        await establishSession(result.user);
        return;
      }
    } catch (e) { /* no pending redirect */ }
  }
  await refreshSessionFromServer();
}

window.FirebaseAuth = {
  init, signIn, signOutEverywhere, connectYouTube, disconnectYouTube,
  onChange: (cb) => listeners.push(cb),
  getUser: () => currentUser,
  isConfigured: () => !!serverConfig.firebase
};
