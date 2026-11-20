package letshangllc.speedometer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Foreground-only GPS/network speedometer. Requests fine/coarse location at runtime;
 * does not use background location.
 */
public class MainActivity extends AppCompatActivity implements LocationListener {

    private double multiplicationFactor = 2.236936;

    private TextView tvSpeed;
    private LocationManager locationManager;
    private boolean receivingUpdates;
    private AdsHelper adsHelper;
    private Timer adRefreshTimer;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.root_layout);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        tvSpeed = findViewById(R.id.tvSpeed);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Switch switch1 = findViewById(R.id.switch1);
        final TextView tvLabel = findViewById(R.id.tvSpeedLabel);

        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    tvLabel.setText(R.string.unit_kmh);
                    multiplicationFactor = 3.6;
                } else {
                    tvLabel.setText(R.string.unit_mph);
                    multiplicationFactor = 2.236936;
                }
            }
        });

        updateSpeedDisplay(null);
        ensureLocationPermissionAndStart();
        runAds();
    }

    private void ensureLocationPermissionAndStart() {
        if (hasAnyLocationPermission()) {
            startLocationUpdates();
            return;
        }

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void onLocationPermissionResult(@NonNull Map<String, Boolean> result) {
        boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
        boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
        if (fine || coarse) {
            startLocationUpdates();
        } else {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
            tvSpeed.setText(R.string.speed_placeholder);
        }
    }

    private void startLocationUpdates() {
        if (locationManager == null || receivingUpdates) {
            return;
        }
        if (!hasAnyLocationPermission()) {
            return;
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 0f, this);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 0f, this);
            }
            receivingUpdates = true;
        } catch (SecurityException e) {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_LONG).show();
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null && receivingUpdates) {
            locationManager.removeUpdates(this);
            receivingUpdates = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasAnyLocationPermission()) {
            startLocationUpdates();
        }
        if (adsHelper != null) {
            adsHelper.onResume();
        }
    }

    @Override
    protected void onPause() {
        // Foreground-only: stop updates when not visible (no background location).
        stopLocationUpdates();
        if (adsHelper != null) {
            adsHelper.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        if (adRefreshTimer != null) {
            adRefreshTimer.cancel();
            adRefreshTimer = null;
        }
        if (adsHelper != null) {
            adsHelper.onDestroy();
        }
        super.onDestroy();
    }

    private boolean hasAnyLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        updateSpeedDisplay(location);
    }

    private void updateSpeedDisplay(@Nullable Location location) {
        if (tvSpeed == null) {
            return;
        }
        if (location == null) {
            tvSpeed.setText(R.string.speed_placeholder);
            return;
        }
        double speed = location.getSpeed() * multiplicationFactor;
        if (speed > 100) {
            tvSpeed.setTextSize(72.0f);
        }
        tvSpeed.setText(String.format(Locale.getDefault(), "%.2f", speed));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Deprecated on API 29+; required for LocationListener on older devices.
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    private void runAds() {
        adsHelper = new AdsHelper(
                getWindow().getDecorView(),
                getString(R.string.admob_banner_unit_id),
                this
        );
        adsHelper.setUpAds();

        int delay = 1000;
        int period = getResources().getInteger(R.integer.ad_refresh_rate);
        adRefreshTimer = new Timer();
        adRefreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (adsHelper != null) {
                    adsHelper.refreshAd();
                }
            }
        }, delay, period);
    }
}
