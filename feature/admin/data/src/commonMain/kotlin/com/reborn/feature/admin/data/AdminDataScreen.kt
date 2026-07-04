package com.reborn.feature.admin.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reborn.core.designsystem.component.RebornTopAppBar
import com.reborn.core.ui.RebornLoadingScreen
import com.reborn.core.ui.component.SelectPickerSection
import com.reborn.core.ui.component.TabBar
import com.reborn.core.ui.ext.rebornDefault
import com.reborn.feature.admin.data.component.section.DataLineChartSection
import com.reborn.feature.admin.data.model.AdminDataIntent
import com.reborn.feature.admin.data.model.AdminDataUiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminDataRoute(
    viewModel: AdminDataViewModel = koinViewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(AdminDataIntent.LoadInitial)

        viewModel.event.collect { event ->
            when (event) {
                is AdminDataEvent.ShowErrorSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.throwable.message ?: "에러가 발생했습니다."
                    )
                }
                is AdminDataEvent.Exit -> onBackClick()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { _ ->
        when (val state = uiState) {
            is AdminDataUiState.Loading -> RebornLoadingScreen()
            is AdminDataUiState.Data -> AdminDataScreen(
                state = state,
                onCategoryClick = { category -> viewModel.onIntent(AdminDataIntent.ClickCategoryTab(category)) },
                onPeriodClick = { period -> viewModel.onIntent(AdminDataIntent.ClickPeriod(period)) }
            )
        }
    }
}

@Composable
fun AdminDataScreen(
    state: AdminDataUiState.Data,
    onCategoryClick: (AdminDataUiState.Category) -> Unit,
    onPeriodClick: (AdminDataUiState.Period) -> Unit
) {
    Column(
        modifier = Modifier
            .rebornDefault(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        RebornTopAppBar(title = "${state.place} 보고서")
        TabBar(
            tabItems = AdminDataUiState.Category.entries,
            selectedTab = state.selectedCategory,
            onTabSelected = onCategoryClick,
            getDisplayName = { it.label }
        )
        Column(
            modifier = Modifier.padding(16.dp, 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectPickerSection(
                title = "기간",
                options = AdminDataUiState.Period.entries,
                selectedOption = state.selectedPeriod,
                onOptionSelected = onPeriodClick,
                optionToString = { it.label }
            )
            DataLineChartSection(
                labels = state.chartLabels,
                values = state.chartValues
            )
        }
    }
}
