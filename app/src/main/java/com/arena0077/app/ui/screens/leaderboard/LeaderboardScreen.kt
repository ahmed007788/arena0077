package com.arena0077.app.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arena0077.app.data.models.LeaderboardCategory
import com.arena0077.app.ui.theme.ArenaGreen
import com.arena0077.app.ui.theme.ArenaPurple

/**
 * LeaderboardScreen - mirrors arena.ai's leaderboard page.
 *
 * Shows top AI models ranked by arena score across multiple categories:
 * Overall / Text / Vision / Coding / Hard / Agent.
 *
 * Data is loaded from arena.ai's leaderboard endpoint (extracted but
 * not yet wired here - uses placeholder until WebView capture is added).
 */
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit
) {
    var category by remember { mutableStateOf(LeaderboardCategory.OVERALL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Category tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LeaderboardCategory.values().forEach { cat ->
                val isSelected = cat == category
                Surface(
                    color = if (isSelected) ArenaPurple.copy(alpha = 0.15f) else Color.Transparent,
                    contentColor = if (isSelected) ArenaPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { category = cat }
                ) {
                    Text(
                        text = cat.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Rank", modifier = Modifier.width(50.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Model", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Score", modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Votes", modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // List (placeholder data - real data will come from WebView)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(getPlaceholderLeaderboard(category)) { entry ->
                LeaderboardRow(entry)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntryDisplay) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (entry.rank <= 3) ArenaPurple.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.rank.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (entry.rank <= 3) ArenaPurple else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.width(12.dp))

        // Model info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.modelName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = entry.organization,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Score
        Text(
            text = entry.score.toString(),
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = ArenaGreen
        )

        // Votes
        Text(
            text = entry.votes,
            modifier = Modifier.width(70.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class LeaderboardEntryDisplay(
    val rank: Int,
    val modelName: String,
    val organization: String,
    val score: Int,
    val votes: String
)

// Placeholder data - real data will be loaded from arena.ai's leaderboard API
private fun getPlaceholderLeaderboard(category: LeaderboardCategory): List<LeaderboardEntryDisplay> {
    return listOf(
        LeaderboardEntryDisplay(1, "GPT-5", "OpenAI", 1428, "12.4k"),
        LeaderboardEntryDisplay(2, "Claude 4.5 Sonnet", "Anthropic", 1415, "11.8k"),
        LeaderboardEntryDisplay(3, "Gemini 2.5 Pro", "Google", 1407, "10.9k"),
        LeaderboardEntryDisplay(4, "Llama 4 70B", "Meta", 1389, "9.8k"),
        LeaderboardEntryDisplay(5, "Mistral Large 3", "Mistral AI", 1372, "8.7k"),
        LeaderboardEntryDisplay(6, "Qwen 3 Max", "Alibaba", 1365, "7.9k"),
        LeaderboardEntryDisplay(7, "DeepSeek V3.5", "DeepSeek", 1358, "7.1k"),
        LeaderboardEntryDisplay(8, "Grok 4", "xAI", 1349, "6.5k"),
        LeaderboardEntryDisplay(9, "Command R+ 2", "Cohere", 1334, "5.8k"),
        LeaderboardEntryDisplay(10, "Yi-Lightning", "01.AI", 1322, "4.9k")
    )
}
