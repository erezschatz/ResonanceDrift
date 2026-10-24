package net.erezschatz.resonancedrift.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import net.erezschatz.resonancedrift.ui.nav.Tab

/** The five tabs. Tapping the current tab returns it to its root (Navigator.selectTab). */
@Composable
fun BottomNav(currentTab: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar {
        for (tab in Tab.entries) {
            NavigationBarItem(
                selected = tab == currentTab,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
            )
        }
    }
}
