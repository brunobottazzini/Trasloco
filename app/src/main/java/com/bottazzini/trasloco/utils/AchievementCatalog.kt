package com.bottazzini.trasloco.utils

import com.bottazzini.trasloco.R

object AchievementCatalog {
    val all: List<AchievementDef> = listOf(
        // --- Vittorie totali (6) ---
        AchievementDef("first_win",   "🎉", R.string.achievement_first_win_name,   R.string.achievement_first_win_desc),
        AchievementDef("wins_10",     "🃏", R.string.achievement_wins_10_name,     R.string.achievement_wins_10_desc),
        AchievementDef("wins_50",     "🎰", R.string.achievement_wins_50_name,     R.string.achievement_wins_50_desc),
        AchievementDef("wins_100",    "🏆", R.string.achievement_wins_100_name,    R.string.achievement_wins_100_desc),
        AchievementDef("wins_500",    "👑", R.string.achievement_wins_500_name,    R.string.achievement_wins_500_desc),
        AchievementDef("wins_1000",   "♾️", R.string.achievement_wins_1000_name,   R.string.achievement_wins_1000_desc),
        // --- Streak (12) ---
        AchievementDef("streak_3",   "🔥", R.string.achievement_streak_3_name,   R.string.achievement_streak_3_desc),
        AchievementDef("streak_6",   "🔥", R.string.achievement_streak_6_name,   R.string.achievement_streak_6_desc),
        AchievementDef("streak_9",   "🔥", R.string.achievement_streak_9_name,   R.string.achievement_streak_9_desc),
        AchievementDef("streak_12",  "💥", R.string.achievement_streak_12_name,  R.string.achievement_streak_12_desc),
        AchievementDef("streak_15",  "💥", R.string.achievement_streak_15_name,  R.string.achievement_streak_15_desc),
        AchievementDef("streak_18",  "💥", R.string.achievement_streak_18_name,  R.string.achievement_streak_18_desc),
        AchievementDef("streak_21",  "⚡️", R.string.achievement_streak_21_name,  R.string.achievement_streak_21_desc),
        AchievementDef("streak_24",  "⚡️", R.string.achievement_streak_24_name,  R.string.achievement_streak_24_desc),
        AchievementDef("streak_27",  "⚡️", R.string.achievement_streak_27_name,  R.string.achievement_streak_27_desc),
        AchievementDef("streak_30",  "🌟", R.string.achievement_streak_30_name,  R.string.achievement_streak_30_desc),
        AchievementDef("streak_50",  "🌟", R.string.achievement_streak_50_name,  R.string.achievement_streak_50_desc),
        AchievementDef("streak_100", "💎", R.string.achievement_streak_100_name, R.string.achievement_streak_100_desc),
        // --- Velocità (4) ---
        AchievementDef("speed_3min", "⏱️",  R.string.achievement_speed_3min_name, R.string.achievement_speed_3min_desc),
        AchievementDef("speed_2min", "🚀",  R.string.achievement_speed_2min_name, R.string.achievement_speed_2min_desc),
        AchievementDef("speed_1min", "✈️",  R.string.achievement_speed_1min_name, R.string.achievement_speed_1min_desc),
        AchievementDef("speed_45s",  "🌪️", R.string.achievement_speed_45s_name,  R.string.achievement_speed_45s_desc),
        // --- Stile (8) ---
        AchievementDef("tutorial_done", "📚", R.string.achievement_tutorial_done_name, R.string.achievement_tutorial_done_desc),
        AchievementDef("first_loss",    "😅", R.string.achievement_first_loss_name,    R.string.achievement_first_loss_desc),
        AchievementDef("games_50",      "🏋️", R.string.achievement_games_50_name,      R.string.achievement_games_50_desc),
        AchievementDef("games_200",     "🎪", R.string.achievement_games_200_name,     R.string.achievement_games_200_desc),
        AchievementDef("games_500",     "🌍", R.string.achievement_games_500_name,     R.string.achievement_games_500_desc),
        AchievementDef("hint_free",     "🎯", R.string.achievement_hint_free_name,     R.string.achievement_hint_free_desc),
        AchievementDef("no_assist",     "🧘", R.string.achievement_no_assist_name,     R.string.achievement_no_assist_desc),
        AchievementDef("resilient",     "💪", R.string.achievement_resilient_name,     R.string.achievement_resilient_desc),
        // --- Speciali (4) ---
        AchievementDef("morning",     "🌅", R.string.achievement_morning_name,     R.string.achievement_morning_desc),
        AchievementDef("midnight",    "🌙", R.string.achievement_midnight_name,    R.string.achievement_midnight_desc),
        AchievementDef("christmas",   "🎄", R.string.achievement_christmas_name,   R.string.achievement_christmas_desc),
        AchievementDef("new_record",  "⭐️", R.string.achievement_new_record_name,  R.string.achievement_new_record_desc),
        // --- Sconfitte consecutive (6) ---
        AchievementDef("loss_2",        "😬", R.string.achievement_loss_2_name,        R.string.achievement_loss_2_desc),
        AchievementDef("loss_3",        "🤦", R.string.achievement_loss_3_name,        R.string.achievement_loss_3_desc),
        AchievementDef("loss_5",        "💀", R.string.achievement_loss_5_name,        R.string.achievement_loss_5_desc),
        AchievementDef("loss_7",        "🫣", R.string.achievement_loss_7_name,        R.string.achievement_loss_7_desc),
        AchievementDef("loss_10",       "🃏", R.string.achievement_loss_10_name,       R.string.achievement_loss_10_desc),
        AchievementDef("big_loser",     "🏳️", R.string.achievement_big_loser_name,     R.string.achievement_big_loser_desc),
        // --- Rimonte e stile (8) ---
        AchievementDef("comeback_2",    "😤", R.string.achievement_comeback_2_name,    R.string.achievement_comeback_2_desc),
        AchievementDef("slow_win",      "🐢", R.string.achievement_slow_win_name,      R.string.achievement_slow_win_desc),
        AchievementDef("hint_hero",     "💡", R.string.achievement_hint_hero_name,     R.string.achievement_hint_hero_desc),
        AchievementDef("hint_addict",   "🧪", R.string.achievement_hint_addict_name,   R.string.achievement_hint_addict_desc),
        AchievementDef("perfectionist", "🎭", R.string.achievement_perfectionist_name, R.string.achievement_perfectionist_desc),
        AchievementDef("speed_freak",   "⚡", R.string.achievement_speed_freak_name,   R.string.achievement_speed_freak_desc),
        AchievementDef("lunch_win",     "🍝", R.string.achievement_lunch_win_name,     R.string.achievement_lunch_win_desc),
        AchievementDef("sunday_player", "☕", R.string.achievement_sunday_player_name, R.string.achievement_sunday_player_desc),
        // --- Sessioni (3) ---
        AchievementDef("night_owl_3",   "🦉", R.string.achievement_night_owl_3_name,   R.string.achievement_night_owl_3_desc),
        AchievementDef("same_day_3",    "📅", R.string.achievement_same_day_3_name,    R.string.achievement_same_day_3_desc),
        AchievementDef("same_day_5",    "🔁", R.string.achievement_same_day_5_name,    R.string.achievement_same_day_5_desc),
        // --- Nuove festività (4) ---
        AchievementDef("new_year_eve",  "🥂", R.string.achievement_new_year_eve_name,  R.string.achievement_new_year_eve_desc),
        AchievementDef("new_year",      "🎆", R.string.achievement_new_year_name,      R.string.achievement_new_year_desc),
        AchievementDef("halloween",     "🎃", R.string.achievement_halloween_name,     R.string.achievement_halloween_desc),
        AchievementDef("ferragosto",    "☀️", R.string.achievement_ferragosto_name,    R.string.achievement_ferragosto_desc)
    )

    fun findById(id: String): AchievementDef? = all.find { it.id == id }
}
