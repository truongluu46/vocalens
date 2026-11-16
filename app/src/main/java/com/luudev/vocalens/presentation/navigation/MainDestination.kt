package com.luudev.vocalens.presentation.navigation

import com.luudev.vocalens.presentation.components.BottomTab

enum class MainDestination(val tab: BottomTab) {
    Home(BottomTab.Home),
    Vocabulary(BottomTab.Vocabulary),
    Flashcards(BottomTab.Vocabulary),
    Camera(BottomTab.Camera),
    Practice(BottomTab.Practice),
    Profile(BottomTab.Profile);

    companion object {
        fun fromTab(tab: BottomTab): MainDestination = entries.first { it.tab == tab }
    }
}
