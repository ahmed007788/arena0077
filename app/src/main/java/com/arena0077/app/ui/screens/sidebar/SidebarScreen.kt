package com.arena0077.app.ui.screens.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arena0077.app.data.models.HistoryItem
import com.arena0077.app.ui.screens.chat.ChatViewModel
import com.arena0077.app.ui.theme.ArenaPurple
import com.arena0077.app.ui.theme.ArenaGreen

/**
 * SidebarScreen - mirrors arena.ai's left sidebar.
 *
 *   ┌────────────────────────┐
 *   │  + New Chat            │
 *   │  🏆 Leaderboard    ▼   │
 *   │  🔍 Search             │
 *   ├────────────────────────┤
 *   │  Recent conversations  │
 *   │  ┌──────────────────┐  │
 *   │  │ Chat title...    │  │
 *   │  └──────────────────┘  │
 *   │  ┌──────────────────┐  │
 *   │  │ Another chat...  │  │
 *   │  └──────────────────┘  │
 *   ├────────────────────────┤
 *   │  🤖 Get More Done...   │  ← agent banner
 *   ├────────────────────────┤
 *   │  A                ⚙    │  ← user + settings
 *   └────────────────────────┘
 */
@Composable
fun SidebarScreen(
    onClose: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsStateWithLifecycle()
    val currentUser by viewModel.authManager.session.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top nav
        SidebarNavItem(
            icon = Icons.Outlined.Add,
            label = "New Chat",
            onClick = {
                viewModel.newChat()
                onClose()
            }
        )

        SidebarNavItem(
            icon = Icons.Outlined.Leaderboard,
            label = "Leaderboard",
            onClick = onLeaderboardClick
        )

        SidebarNavItem(
            icon = Icons.Outlined.Search,
            label = "Search",
            onClick = { /* TODO */ }
        )

        Spacer(Modifier.height(8.dp))
        Divider()

        // Recent conversations
        Text(
            text = "Recent",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(history, key = { it.id }) { item ->
                HistoryRow(
                    item = item,
                    onClick = {
                        viewModel.openConversation(item.id)
                        onClose()
                    }
                )
            }
        }

        // Agent banner
        Surface(
            color = ArenaPurple.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ArenaPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Get More Done With Agents",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Try it now",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArenaPurple
                    )
                }
            }
        }

        Divider()

        // User row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSettingsClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ArenaGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (currentUser.value?.user?.email?.firstOrNull() ?: "A").toString().uppercase(),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = currentUser.value?.user?.email ?: "Not signed in",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Outlined.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { /* TODO: more options */ }, modifier = Modifier.size(20.dp)) {
            Icon(
                Icons.Outlined.MoreHoriz,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline)
    )
}

// Need Color import - now properly imported above

