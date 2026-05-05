package com.nwsweather.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.nwsweather.data.repository.CitySuggestion

@Composable
fun LocationSearchCard(
    searchQuery: String,
    citySuggestions: List<CitySuggestion>,
    onCitySuggestionSelected: (CitySuggestion) -> Unit,
    saveLabel: String,
    isLoading: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onSaveLabelChanged: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSearchAddress: () -> Unit,
    onAlertClick: () -> Unit = {},
    hasAlerts: Boolean = false,
    cardColor: Color = Color.White.copy(alpha = 0.88f),
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    showHelp: Boolean = false,
    onDismissHelp: () -> Unit = {}
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        focusedLabelColor = textColor.copy(alpha = 0.8f),
        unfocusedLabelColor = textColor.copy(alpha = 0.6f),
        focusedBorderColor = textColor.copy(alpha = 0.5f),
        unfocusedBorderColor = textColor.copy(alpha = 0.3f)
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Find weather",
                style = MaterialTheme.typography.titleLarge,
                color = textColor
            )

            if (showHelp) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Pro-tip: You can enter city, state, or even a specific ZIP code to find your forecast.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        TextButton(
                            onClick = onDismissHelp,
                            modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.End)
                        ) {
                            Text("Got it", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Address") },
                singleLine = true,
                enabled = !isLoading,
                colors = textFieldColors
            )

            if (citySuggestions.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    )
                ) {
                    Column {
                        citySuggestions.forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCitySuggestionSelected(suggestion) }
                                    .padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = suggestion.fullDisplay,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = saveLabel,
                onValueChange = onSaveLabelChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Save as label (optional)") },
                singleLine = true,
                enabled = !isLoading,
                colors = textFieldColors
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUseCurrentLocation,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MyLocation,
                        contentDescription = "Use current location"
                    )
                    Text(
                        text = "  Use current location",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onSearchAddress,
                    enabled = !isLoading && searchQuery.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search address"
                    )
                    Text(
                        text = "  Search",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            if (hasAlerts) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlertClick() },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Hazardous weather conditions reported in your area",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
