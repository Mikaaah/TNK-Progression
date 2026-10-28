// TNK Progression 0.2.0 optional pack bridge. Uses the current TK3 chapter completion IDs.
// Server-authorized one-time engineering commissions; no output-stack or machine-tick XP.
// FTB Quests/KubeJS integration follows the pack's existing tk3_stages.js API.
(function () {
    const commissions = [
        {
                "quest": "B0A94968CD02DE8A",
                "objective": "tnk:chapter_1_complete"
        },
        {
                "quest": "60278B0905A64DC9",
                "objective": "tnk:chapter_2_complete"
        },
        {
                "quest": "B81077B20C8371D3",
                "objective": "tnk:chapter_3_complete"
        },
        {
                "quest": "1CD504854E287D44",
                "objective": "tnk:chapter_4_complete"
        },
        {
                "quest": "4B5E0820B553B7C9",
                "objective": "tnk:chapter_5_complete"
        },
        {
                "quest": "DCEB2AF9B398CAEB",
                "objective": "tnk:chapter_6_complete"
        },
        {
                "quest": "BCC05513E2AD3BA9",
                "objective": "tnk:chapter_7_complete"
        },
        {
                "quest": "91CC6BEFB3017B3F",
                "objective": "tnk:chapter_8_complete"
        },
        {
                "quest": "DD2408B49260ECA5",
                "objective": "tnk:chapter_9_complete"
        },
        {
                "quest": "502988B461C73519",
                "objective": "tnk:chapter_10_complete"
        }
];
    function sync(player) {
        const data = FTBQuests.getServerDataFromPlayer(player);
        commissions.forEach(entry => {
            const key = 'tnk_engineering_' + entry.quest;
            if (data.isCompleted(entry.quest) && !player.persistentData.getBoolean(key)) {
                player.server.runCommandSilent('tnkprogress engineering ' + player.username + ' ' + entry.objective);
                player.persistentData.putBoolean(key, true);
            }
        });
    }
    PlayerEvents.loggedIn(event => sync(event.player));
    commissions.forEach(entry => FTBQuestsEvents.completed(entry.quest, event => {
        event.onlineMembers.forEach(player => sync(player));
    }));
})();
