package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyLog
import com.example.data.recipes.BrainHealthyRecipe
import com.example.data.recipes.RecipeCatalog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DIET_PREFS = listOf("Balanced", "Ketogenic", "Modified Atkins", "Low-Glycemic")
private val MEAL_CATS = listOf("All", "Breakfast", "Lunch", "Dinner", "Snack")
private val ACTIVITY_CATS = listOf("Sleep", "Exercise", "Hydration", "Stress / Mood", "Screen Time")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyWellnessScreen(
    dailyLogs: List<DailyLog>,
    initialDietPreference: String,
    onSaveDailyLog: (DailyLog) -> Unit,
    onDeleteDailyLog: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("food") } // "food" or "activity"
    var selectedDiet by remember(initialDietPreference) {
        mutableStateOf(initialDietPreference.ifBlank { "Balanced" })
    }
    var mealFilter by remember { mutableStateOf("All") }
    var selectedRecipe by remember { mutableStateOf<BrainHealthyRecipe?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val suggestedRecipes = remember(selectedDiet, mealFilter) {
        RecipeCatalog.suggestedFor(selectedDiet, mealFilter)
    }
    val filteredLogs = remember(dailyLogs, selectedTab) {
        dailyLogs.filter { it.entryType == selectedTab }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedTab == "food",
                    onClick = { selectedTab = "food" },
                    label = { Text("Food & Recipes") },
                    leadingIcon = { Icon(Icons.Filled.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("daily_tab_food")
                )
                FilterChip(
                    selected = selectedTab == "activity",
                    onClick = { selectedTab = "activity" },
                    label = { Text("Activities") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("daily_tab_activity")
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("add_daily_log_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (selectedTab == "food") "Log Food" else "Log Activity")
                }
            }
        }

        if (selectedTab == "food") {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 840.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_recipe_banner),
                        contentDescription = "Brain-healthy nutrition banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(MaterialTheme.shapes.large),
                        contentScale = ContentScale.Crop
                    )
                    SectionHeader(
                        title = "Suggested Brain-Healthy Recipes",
                        subtitle = "Filtered by dietary plan ($selectedDiet)"
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DIET_PREFS.forEach { diet ->
                            FilterChip(
                                selected = selectedDiet == diet,
                                onClick = { selectedDiet = diet },
                                label = { Text(diet) }
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MEAL_CATS.forEach { cat ->
                            FilterChip(
                                selected = mealFilter == cat,
                                onClick = { mealFilter = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            }

            items(suggestedRecipes, key = { it.id }) { recipe ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("${recipe.mealCategory} · ${recipe.prepMinutes} min · ${recipe.caloriesSummary}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(recipe.benefitNote, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            recipe.dietTags.forEach { tag ->
                                AssistChip(onClick = {}, label = { Text(tag) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { selectedRecipe = recipe }) {
                                Text("View Recipe")
                            }
                            Button(onClick = {
                                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                                onSaveDailyLog(
                                    DailyLog(
                                        entryType = "food",
                                        title = recipe.title,
                                        category = recipe.mealCategory,
                                        mealTime = nowTime,
                                        caloriesOrAmount = recipe.caloriesSummary,
                                        dietTags = recipe.dietTags,
                                        notes = "Logged from AuraCare recipe suggestion",
                                        loggedAtMillis = System.currentTimeMillis()
                                    )
                                )
                            }) {
                                Text("Log as Meal")
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 840.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionHeader(
                        title = "Quick Activity Check-In",
                        subtitle = "Track sleep, hydration, exercise, and stress triggers"
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            onSaveDailyLog(
                                DailyLog(
                                    entryType = "activity",
                                    title = "Night Sleep (7.5 hrs)",
                                    category = "Sleep",
                                    durationMinutes = 450,
                                    caloriesOrAmount = "7.5 hours",
                                    loggedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }) { Text("+ 7.5h Sleep") }
                        OutlinedButton(onClick = {
                            onSaveDailyLog(
                                DailyLog(
                                    entryType = "activity",
                                    title = "Hydration (500 ml Water)",
                                    category = "Hydration",
                                    caloriesOrAmount = "500 ml",
                                    loggedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }) { Text("+ 500ml Water") }
                        OutlinedButton(onClick = {
                            onSaveDailyLog(
                                DailyLog(
                                    entryType = "activity",
                                    title = "Mindful Walk",
                                    category = "Exercise",
                                    durationMinutes = 30,
                                    loggedAtMillis = System.currentTimeMillis()
                                )
                            )
                        }) { Text("+ 30m Walk") }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.widthIn(max = 840.dp)) {
                SectionHeader(
                    title = if (selectedTab == "food") "Logged Food Intake (${filteredLogs.size})" else "Logged Activities (${filteredLogs.size})"
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Column(modifier = Modifier.widthIn(max = 840.dp)) {
                    EmptyStateCard(
                        icon = if (selectedTab == "food") Icons.Filled.Restaurant else Icons.AutoMirrored.Filled.DirectionsRun,
                        title = if (selectedTab == "food") "No meals logged yet" else "No activities logged yet",
                        description = if (selectedTab == "food") {
                            "Log meals manually or tap 'Log as Meal' on any brain-healthy recipe above."
                        } else {
                            "Track sleep, exercise, hydration, and stress to spot seizure patterns."
                        },
                        buttonLabel = if (selectedTab == "food") "Log Food Intake" else "Log Activity",
                        onButtonClick = { showAddDialog = true }
                    )
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = log.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = formatDateTime(log.loggedAtMillis),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(log.title, style = MaterialTheme.typography.titleMedium)
                            val detailLine = listOfNotNull(
                                log.caloriesOrAmount.takeIf { it.isNotBlank() },
                                if (log.durationMinutes > 0) "${log.durationMinutes} min" else null,
                                log.notes.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")
                            if (detailLine.isNotBlank()) {
                                Text(detailLine, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { onDeleteDailyLog(log.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (selectedRecipe != null) {
        val recipe = selectedRecipe!!
        AlertDialog(
            onDismissRequest = { selectedRecipe = null },
            title = { Text(recipe.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${recipe.mealCategory} · ${recipe.prepMinutes} min · ${recipe.caloriesSummary}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(recipe.benefitNote, style = MaterialTheme.typography.bodyMedium)
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                    recipe.ingredients.forEach { ing ->
                        Text("• $ing", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Preparation", style = MaterialTheme.typography.titleMedium)
                    recipe.steps.forEachIndexed { idx, step ->
                        Text("${idx + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    onSaveDailyLog(
                        DailyLog(
                            entryType = "food",
                            title = recipe.title,
                            category = recipe.mealCategory,
                            mealTime = nowTime,
                            caloriesOrAmount = recipe.caloriesSummary,
                            dietTags = recipe.dietTags,
                            notes = "Logged from AuraCare recipe suggestion",
                            loggedAtMillis = System.currentTimeMillis()
                        )
                    )
                    selectedRecipe = null
                }) {
                    Text("Log as Meal")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecipe = null }) { Text("Close") }
            }
        )
    }

    if (showAddDialog) {
        DailyLogEditorDialog(
            entryType = selectedTab,
            onDismiss = { showAddDialog = false },
            onSave = { log ->
                showAddDialog = false
                onSaveDailyLog(log)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyLogEditorDialog(
    entryType: String,
    onDismiss: () -> Unit,
    onSave: (DailyLog) -> Unit
) {
    val isFood = entryType == "food"
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(if (isFood) "Lunch" else "Sleep") }
    var amountOrCalories by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf(if (isFood) "0" else "30") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val cats = if (isFood) listOf("Breakfast", "Lunch", "Dinner", "Snack") else ACTIVITY_CATS

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isFood) "Log Food Intake" else "Log Daily Activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorMsg = null },
                    label = { Text(if (isFood) "Meal / Food *" else "Activity *") },
                    placeholder = { Text(if (isFood) "e.g., Avocado Egg Toast" else "e.g., 8 hrs uninterrupted sleep") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    cats.forEach { cat ->
                        FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                    }
                }
                OutlinedTextField(
                    value = amountOrCalories,
                    onValueChange = { amountOrCalories = it },
                    label = { Text(if (isFood) "Portion / Calories / Carbs" else "Amount / Intensity") },
                    placeholder = { Text(if (isFood) "e.g., 420 kcal · 5g net carbs" else "e.g., 8 hrs or 1.5L water") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isFood) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text("Dietary Tags", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Ketogenic", "Low-GI", "High-Omega3", "Hydrating").forEach { tag ->
                            FilterChip(
                                selected = tag in selectedTags,
                                onClick = { selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isBlank()) {
                    errorMsg = "Please enter a title."
                    return@Button
                }
                val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                onSave(
                    DailyLog(
                        entryType = entryType,
                        title = title.trim(),
                        category = category,
                        mealTime = if (isFood) nowTime else "",
                        durationMinutes = durationText.toIntOrNull() ?: 0,
                        caloriesOrAmount = amountOrCalories.trim(),
                        dietTags = selectedTags.toList(),
                        notes = notes.trim(),
                        loggedAtMillis = System.currentTimeMillis()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
