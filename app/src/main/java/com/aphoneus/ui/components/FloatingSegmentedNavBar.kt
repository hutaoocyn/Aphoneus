package com.aphoneus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aphoneus.ui.theme.NavPillAccent
import com.aphoneus.ui.theme.NavPillBackground
import com.aphoneus.ui.theme.SurfaceBorder
import com.aphoneus.ui.theme.SurfaceContainerElevated
import com.aphoneus.ui.theme.TextPrimary
import com.aphoneus.ui.theme.TextSecondary
import com.aphoneus.ui.theme.TextTertiary

enum class NavDestination(val route: String, val title: String, val icon: ImageVector) {
    DASHBOARD("dashboard", "Home", Icons.Default.Home),
    MODES("modes", "Modes", Icons.Default.Explore),
    QUICK_CYCLE("quick_cycle", "Cycle", Icons.Default.SyncAlt),
    CUSTOM("custom", "Tuning", Icons.Default.Tune),
    DIAGNOSTICS("diagnostics", "Tools", Icons.Default.Build)
}

/**
 * Floating Segmented Capsule Navbar derived from reference design (2986.jpg).
 * Features:
 * - Rounded floating pill container elevated above screen bottom
 * - Active-state clarity with illuminated pill segment
 * - Center highlighted action pill for instantaneous mode cycling
 * - Strictly clears Android Gesture Navigation Bar insets
 * - All interactive touch targets >= 48x48dp
 */
@Composable
fun FloatingSegmentedNavBar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    onQuickCycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color(0x33000000))
                .border(width = 1.dp, color = SurfaceBorder, shape = RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            color = SurfaceContainerElevated
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item 0: Home / Dashboard
                NavBarItem(
                    destination = NavDestination.DASHBOARD,
                    isSelected = currentDestination == NavDestination.DASHBOARD,
                    onClick = { onNavigate(NavDestination.DASHBOARD) }
                )

                // Item 1: Explore / Modes
                NavBarItem(
                    destination = NavDestination.MODES,
                    isSelected = currentDestination == NavDestination.MODES,
                    onClick = { onNavigate(NavDestination.MODES) }
                )

                // Center Action: Highlighted Cycle Capsule (Matching reference image center pill)
                CenterActionPill(
                    onClick = onQuickCycle
                )

                // Item 3: Analyze / Custom Tuning
                NavBarItem(
                    destination = NavDestination.CUSTOM,
                    isSelected = currentDestination == NavDestination.CUSTOM,
                    onClick = { onNavigate(NavDestination.CUSTOM) }
                )

                // Item 4: Tools / Diagnostics
                NavBarItem(
                    destination = NavDestination.DIAGNOSTICS,
                    isSelected = currentDestination == NavDestination.DIAGNOSTICS,
                    onClick = { onNavigate(NavDestination.DIAGNOSTICS) }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    destination: NavDestination,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animatedBgColor by animateColorAsState(
        targetValue = if (isSelected) NavPillBackground else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_item_bg"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isSelected) NavPillAccent else TextSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "nav_item_content"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Tab
                contentDescription = "${destination.title} tab, ${if (isSelected) "selected" else "not selected"}"
            }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = animatedContentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = destination.title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = animatedContentColor
        )
    }
}

@Composable
private fun CenterActionPill(
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(58.dp)
            .height(44.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(22.dp), spotColor = NavPillAccent)
            .clip(RoundedCornerShape(22.dp))
            .background(NavPillAccent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                contentDescription = "Quick Cycle: Switch to next power profile"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.SyncAlt,
            contentDescription = null,
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}
