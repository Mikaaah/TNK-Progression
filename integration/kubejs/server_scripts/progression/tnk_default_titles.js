// Pack-wide title entitlements. Runs on both dedicated and integrated servers.
// No client packets, web requests, per-tick scans or player permissions are used.
(function () {
    const rosterPath = 'kubejs/config/tnk-title-defaults.json';
    const catalogPath = 'config/tnk-titles.json';
    const idPattern = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
    let catalogPrepared = false;

    function uuid(value) {
        const compact = String(value).toLowerCase().replace(/-/g, '');
        if (!/^[0-9a-f]{32}$/.test(compact)) throw new Error('Invalid player UUID: ' + value);
        return compact.slice(0, 8) + '-' + compact.slice(8, 12) + '-' + compact.slice(12, 16) + '-' + compact.slice(16, 20) + '-' + compact.slice(20);
    }

    function read(path) {
        const text = JsonIO.readString(path);
        if (!text) throw new Error('Missing file: ' + path);
        return JSON.parse(String(text));
    }

    function reloadCatalog(server) {
        // KubeJS 2101 runCommandSilent returns void. Use Brigadier's actual result
        // to detect validation failure and roll back a rejected catalog update.
        return server.getCommands().getDispatcher().execute('tnkprogress title reload',
            server.createCommandSourceStack().withSuppressedOutput()) === 1;
    }

    function prepare(server) {
        if (!Platform.isLoaded('tnkprogression')) return null;
        try {
            const roster = read(rosterPath);
            if (roster.schemaVersion !== 1 || !Array.isArray(roster.titleDefinitions) ||
                !roster.grants || Array.isArray(roster.grants) || typeof roster.grants !== 'object') {
                throw new Error('Invalid title roster format');
            }
            const catalog = read(catalogPath);
            if (!Array.isArray(catalog.titles)) throw new Error('Invalid existing title catalog');
            const known = {};
            catalog.titles.forEach(function (title) { known[title.id] = true; });
            let changed = false;
            roster.titleDefinitions.forEach(function (title) {
                if (!title || typeof title.id !== 'string' || !idPattern.test(title.id)) {
                    throw new Error('Invalid default title definition');
                }
                // Existing custom labels, requirements and catalogue options remain authoritative.
                if (!known[title.id]) {
                    catalog.titles.push(title);
                    known[title.id] = true;
                    changed = true;
                }
            });
            const grants = {};
            Object.keys(roster.grants).forEach(function (key) {
                const playerId = uuid(key);
                const titles = roster.grants[key];
                if (grants[playerId] || !Array.isArray(titles) || titles.length > 256) {
                    throw new Error('Invalid or duplicate UUID entry: ' + key);
                }
                titles.forEach(function (id) {
                    if (typeof id !== 'string' || id.length > 100 || !idPattern.test(id) || !known[id]) {
                        throw new Error('Unknown or invalid default title: ' + id);
                    }
                });
                grants[playerId] = titles.filter(function (id, index) { return titles.indexOf(id) === index; });
            });
            // Let the mod perform its full definition validation before awarding anything.
            const original = read(catalogPath);
            if (changed) JsonIO.write(catalogPath, catalog);
            if ((!catalogPrepared || changed) && !reloadCatalog(server)) {
                if (changed) {
                    JsonIO.write(catalogPath, original);
                    reloadCatalog(server);
                }
                throw new Error('Title catalog rejected; default grants skipped');
            }
            catalogPrepared = true;
            return grants;
        } catch (error) {
            console.error('[TNK Default Titles] ' + error);
            return null;
        }
    }

    function award(player, grants) {
        if (!grants) return;
        const username = String(player.username);
        // Minecraft profile names are command-safe; names never decide eligibility.
        if (!/^[A-Za-z0-9_]{1,16}$/.test(username)) return;
        const titles = grants[uuid(player.getUUID().toString())] || [];
        titles.forEach(function (id) {
            // 'earn' is idempotent, preserves selection/hiding, and respects per-world revocations.
            player.server.runCommandSilent('tnkprogress title earn ' + username + ' ' + id);
        });
    }

    ServerEvents.loaded(function (event) {
        catalogPrepared = false;
        const grants = prepare(event.server);
        event.server.getPlayerList().getPlayers().forEach(function (player) { award(player, grants); });
    });
    PlayerEvents.loggedIn(function (event) {
        award(event.player, prepare(event.player.server));
    });
})();
