// Personal, permanent titles earned through the team's authored FTB Quests progress.
// Copy with kubejs/config/tnk-title-quests.json; reload scripts/server after changing bindings.
(function () {
    const raw = JsonIO.readString('kubejs/config/tnk-title-quests.json');
    const config = raw ? JSON.parse(String(raw)) : null;
    if (!config || !config.bindings) return;
    const bindings = config.bindings.filter(entry =>
        /^[a-z0-9_.-]+:[a-z0-9_/.\-]+$/.test(String(entry.title)) && entry.quests && entry.quests.length > 0 &&
        entry.required >= 1 && entry.required <= entry.quests.length &&
        entry.quests.every(id => /^[0-9A-Fa-f]{16}$/.test(String(id))));
    function sync(player) {
        const data = FTBQuests.getServerDataFromPlayer(player);
        bindings.forEach(entry => {
            const complete = entry.quests.filter(id => data.isCompleted(String(id))).length;
            if (complete >= entry.required) {
                // 'earn' is idempotent and respects administrator revocation; it never changes an existing selection.
                player.server.runCommandSilent('tnkprogress title earn ' + player.username + ' ' + entry.title);
            }
        });
    }
    const questIds = {};
    bindings.forEach(entry => entry.quests.forEach(id => { questIds[String(id)] = true; }));
    PlayerEvents.loggedIn(event => sync(event.player));
    Object.keys(questIds).forEach(id => FTBQuestsEvents.completed(id, event => {
        event.onlineMembers.forEach(player => sync(player));
    }));
})();
