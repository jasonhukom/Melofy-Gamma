package com.melofy.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melofy.app.ui.theme.IndigoSecondary
import com.melofy.app.ui.theme.PurplePrimary
import com.melofy.app.ui.theme.SurfaceBorder
import com.melofy.app.ui.theme.TextMuted

sealed class BottomNavTab(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val isCenterSearch: Boolean = false
) {
    object Home : BottomNavTab("home", "Home", Icons.Filled.Home)
    object Trending : BottomNavTab("trending", "Trending", null)
    object Search : BottomNavTab("search", "Search", Icons.Filled.Search, isCenterSearch = true)
    object Download : BottomNavTab("download", "Downloads", Icons.Filled.Download)
    object Profile : BottomNavTab("profile", "Profile", Icons.Filled.Person)
}

@Composable
fun MelofyBottomBar(
    currentRoute: String,
    onTabSelected: (BottomNavTab) -> Unit,
    isUserLoggedIn: Boolean = false,
    userProfileUrl: String? = null
) {
    val tabs = listOf(
        BottomNavTab.Home,
        BottomNavTab.Trending,
        BottomNavTab.Search,
        BottomNavTab.Download,
        BottomNavTab.Profile
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF0E1017))
            .border(
                width = 1.dp,
                color = SurfaceBorder,
                shape = androidx.compose.ui.graphics.RectangleShape
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = currentRoute == tab.route

                if (tab.isCenterSearch) {
                    // Center Circular Search Button
                    Box(
                        modifier = Modifier
                            .offset(y = (-8).dp)
                            .size(54.dp)
                            .shadow(12.dp, CircleShape, spotColor = PurplePrimary)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(PurplePrimary, IndigoSecondary)
                                )
                            )
                            .border(2.dp, Color(0x40FFFFFF), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = Color.White)
                            ) {
                                onTabSelected(tab)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                } else {
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) PurplePrimary else TextMuted,
                        animationSpec = tween(durationMillis = 200), label = "tabColor"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTabSelected(tab)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (tab) {
                            BottomNavTab.Trending -> {
                                SixteenthNoteIcon(
                                    tint = contentColor,
                                    size = 22.dp
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = tab.icon ?: Icons.Filled.Home,
                                    contentDescription = tab.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Text(
                            text = tab.title,
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
