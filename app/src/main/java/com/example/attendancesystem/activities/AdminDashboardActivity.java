package com.example.attendancesystem.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.attendancesystem.models.Admin;
import com.example.attendancesystem.services.FirebaseManager;
import com.example.attendancesystem.utils.Utils;
import com.example.attendancesystem.R;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";

    // Views
    private TextView tvWelcome, tvTotalUsers, tvActiveTerminals, tvTodayAttendance;
    private CardView cardUserManagement, cardTerminalManagement, cardSystemReports, cardSystemConfig;

    // Firebase
    private FirebaseManager firebaseManager;
    private Admin currentAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.attendancesystem.R.layout.activity_admin_dashboard);

        // Configurer la toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Dashboard Administrateur");
        }

        // Initialiser Firebase
        firebaseManager = FirebaseManager.getInstance();

        // Initialiser les views
        initViews();

        // Charger les données utilisateur
        loadUserData();

        // Configurer les listeners
        setupListeners();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tv_welcome);
        tvTotalUsers = findViewById(R.id.tv_total_users);
        tvActiveTerminals = findViewById(R.id.tv_active_terminals);
        tvTodayAttendance = findViewById(R.id.tv_today_attendance);

        cardUserManagement = findViewById(R.id.card_user_management);
        cardTerminalManagement = findViewById(R.id.card_terminal_management);
        cardSystemReports = findViewById(R.id.card_system_reports);
        cardSystemConfig = findViewById(R.id.card_system_config);
    }

    private void loadUserData() {
        String userEmail = Utils.getSavedUserEmail(this);
        if (userEmail != null) {
            firebaseManager.getAdminByEmail(userEmail, new FirebaseManager.DataCallback<Admin>() {
                @Override
                public void onSuccess(Admin admin) {
                    currentAdmin = admin;
                    updateUI();
                    loadSystemStats();
                }

                @Override
                public void onFailure(String error) {
                    Utils.showToast(AdminDashboardActivity.this, "Erreur de chargement: " + error);
                }
            });
        }
    }

    private void updateUI() {
        if (currentAdmin != null) {
            String firstName = Utils.getFirstName(currentAdmin.getFullName());
            String welcomeText = "Bonjour, Admin " + firstName + " !";
            tvWelcome.setText(welcomeText);

            // Afficher les informations de l'administrateur
            String adminInfo = currentAdmin.getDepartment();
        }
    }

    private void loadSystemStats() {
        // TODO: Charger les vraies statistiques depuis Firebase
        // Pour l'instant, affichage de données de test
        tvTotalUsers.setText("245 utilisateurs");
        tvActiveTerminals.setText("1 terminal actif"); // Puisqu'on n'a qu'un seul terminal
        tvTodayAttendance.setText("1,847 pointages aujourd'hui");

        // Charger les vraies statistiques système
        loadRealSystemStatistics();
    }

    private void loadRealSystemStatistics() {
        if (currentAdmin != null) {
            // TODO: Implémenter le calcul des vraies statistiques système
            // Compter les utilisateurs dans toutes les collections
            // Compter les pointages du jour
            // Etc.
        }
    }

    private void setupListeners() {
        cardUserManagement.setOnClickListener(v -> {
            Utils.showToast(this, "Redirection vers la gestion des users...");
            Intent intent = new Intent(AdminDashboardActivity.this, AdminUserManagementActivity.class);
            startActivity(intent);
        });

        cardTerminalManagement.setOnClickListener(v -> {
            Utils.showToast(this, "Redirection vers la gestion des justifications...");
            Intent intent = new Intent(AdminDashboardActivity.this, AdminJustificationActivity.class);
            startActivity(intent);
        });

        cardSystemReports.setOnClickListener(v -> {
            // TODO: Créer l'activité de rapports système
            Utils.showToast(this, "Rapports système - Fonctionnalité à venir");
        });

        cardSystemConfig.setOnClickListener(v -> {
            // TODO: Créer l'activité de configuration système
            Utils.showToast(this, "Configuration système - Fonctionnalité à venir");
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            loadUserData();
            Utils.showToast(this, "Données actualisées");
            return true;
        } else if (id == R.id.action_logout) {
            logout();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void logout() {
        firebaseManager.signOut();
        Utils.clearUserData(this);

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualiser les données quand on revient sur l'activité
        if (currentAdmin != null) {
            loadSystemStats();
        }
    }
}