package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CompanyConfigRepository
import com.example.presentation.viewmodel.CatalogFormViewModel
import com.example.presentation.viewmodel.CatalogFormViewModelFactory
import com.example.ui.screens.MainCatalogScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and persistent repositories
        val appDatabase = AppDatabase.getDatabase(this)
        val catalogRepository = CatalogRepository(appDatabase.catalogDao())
        val companyConfigRepository = CompanyConfigRepository(this)

        // 2. Build form factory viewmodel
        val factory = CatalogFormViewModelFactory(
            application = application,
            catalogRepository = catalogRepository,
            configRepository = companyConfigRepository
        )
        val viewModel = ViewModelProvider(this, factory)[CatalogFormViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainCatalogScreen(viewModel = viewModel)
            }
        }
    }
}
