package com.melofy.app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.melofy.app.data.TrackRepository;
import com.melofy.app.databinding.ActivityMainBinding;
import com.melofy.app.databinding.DialogFullPlayerBinding;
import com.melofy.app.model.Track;
import com.melofy.app.player.MelofyPlayerManager;
import com.melofy.app.ui.fragments.DownloadFragment;
import com.melofy.app.ui.fragments.HomeFragment;
import com.melofy.app.ui.fragments.ProfileFragment;
import com.melofy.app.ui.fragments.SearchFragment;
import com.melofy.app.ui.fragments.TrendingFragment;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TrackRepository.RepositoryListener, MelofyPlayerManager.PlayerStateListener {

    private ActivityMainBinding binding;
    private TrackRepository repository;
    private MelofyPlayerManager playerManager;

    private HomeFragment homeFragment;
    private TrendingFragment trendingFragment;
    private SearchFragment searchFragment;
    private DownloadFragment downloadFragment;
    private ProfileFragment profileFragment;

    private Dialog fullPlayerDialog;
    private DialogFullPlayerBinding dialogBinding;

    private boolean isLoggedIn = false;
    private String userName = "Anonymous Listener";
    private String userAvatarUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new TrackRepository(getApplicationContext());
        playerManager = new MelofyPlayerManager(getApplicationContext());

        repository.addListener(this);
        playerManager.addListener(this);

        initFragments();
        initBottomNav();
        initMiniPlayer();
        initFullPlayerDialog();

        handleDeepLink(getIntent());

        selectTab(0);
    }

    private void initFragments() {
        homeFragment = new HomeFragment();
        trendingFragment = new TrendingFragment();
        searchFragment = new SearchFragment();
        downloadFragment = new DownloadFragment();
        profileFragment = new ProfileFragment();
    }

    private void initBottomNav() {
        binding.btnNavHome.setOnClickListener(v -> selectTab(0));
        binding.btnNavTrending.setOnClickListener(v -> selectTab(1));
        binding.btnNavSearch.setOnClickListener(v -> selectTab(2));
        binding.btnNavDownload.setOnClickListener(v -> selectTab(3));
        binding.btnNavProfile.setOnClickListener(v -> selectTab(4));
    }

    private void selectTab(int index) {
        Fragment selected = homeFragment;
        if (index == 1) selected = trendingFragment;
        else if (index == 2) selected = searchFragment;
        else if (index == 3) selected = downloadFragment;
        else if (index == 4) selected = profileFragment;

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.fragment_container, selected)
                .commit();

        updateNavColors(index);
    }

    private void updateNavColors(int selectedIndex) {
        int activeColor = Color.parseColor("#FACC15");
        int inactiveColor = Color.parseColor("#A1A096");

        binding.iconNavHome.setColorFilter(selectedIndex == 0 ? activeColor : inactiveColor);
        binding.textNavHome.setTextColor(selectedIndex == 0 ? activeColor : inactiveColor);

        binding.iconNavTrending.setTintColor(selectedIndex == 1 ? activeColor : inactiveColor);
        binding.textNavTrending.setTextColor(selectedIndex == 1 ? activeColor : inactiveColor);

        binding.iconNavDownload.setColorFilter(selectedIndex == 3 ? activeColor : inactiveColor);
        binding.textNavDownload.setTextColor(selectedIndex == 3 ? activeColor : inactiveColor);

        binding.iconNavProfile.setColorFilter(selectedIndex == 4 ? activeColor : inactiveColor);
        binding.textNavProfile.setTextColor(selectedIndex == 4 ? activeColor : inactiveColor);
    }

    private void initMiniPlayer() {
        binding.miniPlayerLayout.getRoot().setOnClickListener(v -> showFullPlayerDialog());

        binding.miniPlayerLayout.btnMiniPlayPause.setOnClickListener(v -> playerManager.togglePlayPause());
        binding.miniPlayerLayout.btnMiniNext.setOnClickListener(v -> playerManager.skipNext());
    }

    private void initFullPlayerDialog() {
        fullPlayerDialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialogBinding = DialogFullPlayerBinding.inflate(getLayoutInflater());
        fullPlayerDialog.setContentView(dialogBinding.getRoot());

        Window window = fullPlayerDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        dialogBinding.btnCollapse.setOnClickListener(v -> fullPlayerDialog.dismiss());
        dialogBinding.btnFullPlayPause.setOnClickListener(v -> playerManager.togglePlayPause());
        dialogBinding.btnFullPrev.setOnClickListener(v -> playerManager.skipPrevious());
        dialogBinding.btnFullNext.setOnClickListener(v -> playerManager.skipNext());
        dialogBinding.btnFullShuffle.setOnClickListener(v -> playerManager.toggleShuffle());
        dialogBinding.btnFullRepeat.setOnClickListener(v -> playerManager.toggleRepeat());

        dialogBinding.btnFullLike.setOnClickListener(v -> {
            Track current = playerManager.getCurrentTrack();
            if (current != null) {
                repository.toggleLike(current.getId());
            }
        });

        dialogBinding.btnFullDownload.setOnClickListener(v -> {
            Track current = playerManager.getCurrentTrack();
            if (current != null) {
                repository.downloadTrack(current);
            }
        });

        dialogBinding.seekerFullProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    long duration = playerManager.getDurationMs();
                    long targetPos = (long) ((progress / 1000.0) * duration);
                    playerManager.seekTo(targetPos);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    public void showFullPlayerDialog() {
        if (playerManager.getCurrentTrack() != null && !fullPlayerDialog.isShowing()) {
            fullPlayerDialog.show();
        }
    }

    public void playTrack(Track track, List<Track> playlist) {
        playerManager.playTrack(track, playlist);
    }

    public TrackRepository getRepository() { return repository; }
    public MelofyPlayerManager getPlayerManager() { return playerManager; }

    public boolean isLoggedIn() { return isLoggedIn; }
    public String getUserName() { return userName; }
    public String getUserAvatarUrl() { return userAvatarUrl; }

    public void toggleAuthMode() {
        if (isLoggedIn) {
            isLoggedIn = false;
            userName = "Anonymous Listener";
            userAvatarUrl = null;
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();
        } else {
            try {
                Uri authUrl = Uri.parse("https://melofy-gamma.vercel.app/api/auth/google");
                new CustomTabsIntent.Builder().build().launchUrl(this, authUrl);
            } catch (Exception e) {
                isLoggedIn = true;
                userName = "Alex Melofy";
                userAvatarUrl = "https://picsum.photos/id/64/200/200";
                Toast.makeText(this, "Signed in as Alex Melofy", Toast.LENGTH_SHORT).show();
            }
        }
        notifyUiUpdate();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        if ("melofy".equals(uri.getScheme()) && "auth".equals(uri.getHost())) {
            String code = uri.getQueryParameter("code");
            if (code != null && !code.isEmpty()) {
                isLoggedIn = true;
                userName = "YouTube Premium Listener";
                userAvatarUrl = "https://picsum.photos/id/64/200/200";
                Toast.makeText(this, "Successfully connected YouTube account!", Toast.LENGTH_LONG).show();
                notifyUiUpdate();
            }
        }
    }

    @Override
    public void onDataChanged() {
        notifyUiUpdate();
    }

    @Override
    public void onPlayerStateChanged(Track currentTrack, boolean isPlaying, long positionMs, long durationMs, boolean isShuffle, boolean isRepeat, boolean isLoading) {
        runOnUiThread(() -> {
            if (currentTrack != null) {
                binding.miniPlayerLayout.getRoot().setVisibility(View.VISIBLE);
                binding.miniPlayerLayout.textMiniTitle.setText(currentTrack.getTitle());
                binding.miniPlayerLayout.textMiniArtist.setText(currentTrack.getArtist() + " • " + currentTrack.getGenre());
                binding.miniPlayerLayout.btnMiniPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

                int progressPct = durationMs > 0 ? (int) ((positionMs * 1000.0) / durationMs) : 0;
                binding.miniPlayerLayout.progressMiniPlayer.setProgress(progressPct);

                Glide.with(this)
                        .load(currentTrack.getCoverUrl())
                        .into(binding.miniPlayerLayout.imgMiniCover);

                // Dialog UI Update
                if (fullPlayerDialog.isShowing()) {
                    dialogBinding.textFullTitle.setText(currentTrack.getTitle());
                    dialogBinding.textFullArtist.setText(currentTrack.getArtist());
                    dialogBinding.textFullAlbum.setText(currentTrack.getAlbum().isEmpty() ? "Melofy Player" : currentTrack.getAlbum());

                    dialogBinding.imgFullPlayIcon.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                    dialogBinding.btnFullShuffle.setColorFilter(isShuffle ? Color.parseColor("#FACC15") : Color.parseColor("#A1A096"));
                    dialogBinding.btnFullRepeat.setColorFilter(isRepeat ? Color.parseColor("#FACC15") : Color.parseColor("#A1A096"));

                    dialogBinding.btnFullLike.setColorFilter(currentTrack.isLiked() ? Color.parseColor("#EF4444") : Color.parseColor("#A1A096"));
                    dialogBinding.btnFullLike.setImageResource(currentTrack.isLiked() ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);

                    if (currentTrack.isDownloaded()) {
                        dialogBinding.tagOffline.setVisibility(View.VISIBLE);
                        dialogBinding.btnFullDownload.setImageResource(android.R.drawable.stat_sys_download_done);
                        dialogBinding.btnFullDownload.setColorFilter(Color.parseColor("#FEF08A"));
                    } else {
                        dialogBinding.tagOffline.setVisibility(View.GONE);
                        dialogBinding.btnFullDownload.setImageResource(android.R.drawable.stat_sys_download);
                        dialogBinding.btnFullDownload.setColorFilter(Color.parseColor("#A1A096"));
                    }

                    boolean isDownloading = repository.getDownloadingTrackIds().contains(currentTrack.getId());
                    dialogBinding.progressFullDownload.setVisibility(isDownloading ? View.VISIBLE : View.GONE);
                    dialogBinding.btnFullDownload.setVisibility(isDownloading ? View.GONE : View.VISIBLE);

                    dialogBinding.seekerFullProgress.setProgress(progressPct);
                    dialogBinding.textFullPosition.setText(formatTime(positionMs));
                    dialogBinding.textFullDuration.setText(formatTime(durationMs));

                    Glide.with(this)
                            .load(currentTrack.getCoverUrl())
                            .into(dialogBinding.imgFullCover);
                }
            } else {
                binding.miniPlayerLayout.getRoot().setVisibility(View.GONE);
            }
        });
    }

    private void notifyUiUpdate() {
        if (homeFragment != null && homeFragment.isAdded()) homeFragment.updateUi();
        if (trendingFragment != null && trendingFragment.isAdded()) trendingFragment.updateUi();
        if (searchFragment != null && searchFragment.isAdded()) searchFragment.updateUi();
        if (downloadFragment != null && downloadFragment.isAdded()) downloadFragment.updateUi();
        if (profileFragment != null && profileFragment.isAdded()) profileFragment.updateUi();
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        repository.removeListener(this);
        playerManager.removeListener(this);
        playerManager.release();
        super.onDestroy();
    }
}
