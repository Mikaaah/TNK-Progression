// Controlled KubeJS fixtures; these are not a substitute for an installed client test.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const script = fs.readFileSync(path.join(root, 'integration/kubejs/server_scripts/progression/tnk_default_titles.js'), 'utf8');
const template = JSON.parse(fs.readFileSync(path.join(root, 'integration/kubejs/config/tnk-title-defaults.json'), 'utf8'));
const baseCatalog = JSON.parse(fs.readFileSync(path.join(root, 'src/main/resources/data/tnkprogression/default_titles.json'), 'utf8'));
const tester = '657bf016-419a-43dd-9b37-e0f29f08009c';
const other = '765bf016-419a-43dd-9b37-e0f29f08009c';
const copy = value => JSON.parse(JSON.stringify(value));
function fixture(roster, options = {}) {
    const files = new Map([
        ['kubejs/config/tnk-title-defaults.json', JSON.stringify(roster)],
        ['config/tnk-titles.json', JSON.stringify(options.catalog || baseCatalog)]
    ]);
    const commands = [], errors = [], owned = new Set(), revoked = new Set();
    let selected = '', login, loaded;
    let reloads = 0;
    const server = { players: [], getPlayerList() { return { getPlayers: () => this.players }; },
        getCommands() { return { getDispatcher: () => ({ execute: command => this.executeCommand(command) }) }; },
        createCommandSourceStack() { return { withSuppressedOutput() { return this; } }; },
        runCommandSilent(command) { this.executeCommand(command); }, // KubeJS 2101 returns void.
        executeCommand(command) {
        commands.push(command);
        if (command === 'tnkprogress title reload') {
            reloads++;
            return options.rejectFirstReload && reloads === 1 ? 0 : 1;
        }
        const [, , mode, name, id] = command.split(' ');
        assert.equal(mode, 'earn');
        assert.match(name, /^[A-Za-z0-9_]{1,16}$/);
        if (revoked.has(id) || owned.has(id)) return 0;
        owned.add(id);
        if (!selected) selected = id;
        return 1;
    }};
    vm.runInNewContext(script, {
        Platform: { isLoaded: () => options.modPresent !== false },
        JsonIO: { readString: file => files.get(file), write: (file, data) => files.set(file, JSON.stringify(data)) },
        ServerEvents: { loaded: fn => { loaded = fn; } },
        PlayerEvents: { loggedIn: fn => { login = fn; } },
        console: { error: message => errors.push(message) }
    });
    const player = (id = tester, username = 'TitleFixture') => ({ username, server, getUUID: () => ({ toString: () => id }) });
    return { files, commands, errors, owned, revoked, player,
        login: p => login({ player: p || player() }),
        loaded: () => loaded({ server }), server,
        selected: value => { if (value !== undefined) selected = value; return selected; } };
}
let roster = copy(template);
let f = fixture(roster); f.loaded(); f.login(); assert.equal(f.owned.size, 0, 'empty roster awards nobody');
roster.grants[tester] = ['tnk:beta_tester'];
f = fixture(roster); f.login(f.player(other)); assert.equal(f.owned.size, 0, 'unlisted UUID excluded');
f.login(); assert(f.owned.has('tnk:beta_tester')); assert.equal(f.selected(), 'tnk:beta_tester');
f.selected('tnk:ars_mystic'); f.login(); assert.equal(f.selected(), 'tnk:ars_mystic', 'selection preserved');
f.revoked.add('tnk:beta_tester'); f.owned.delete('tnk:beta_tester'); f.login(); assert(!f.owned.has('tnk:beta_tester'), 'earn respects revocation');
for (let world = 0; world < 2; world++) { f = fixture(roster); f.login(); assert(f.owned.has('tnk:beta_tester'), 'new world receives account grant'); }
f = fixture(roster); f.login(f.player(tester, 'ChangedName')); assert(f.commands.some(c => c.includes('earn ChangedName ')), 'UUID survives name changes');
// A non-matching UUID must never gain eligibility by using the tester's old name.
f = fixture(roster); f.login(f.player(other, 'TitleFixture')); assert.equal(f.owned.size, 0);
let legacy = copy(baseCatalog); legacy.titles = legacy.titles.filter(t => t.id !== 'tnk:beta_tester'); legacy.autoEquipFirst = false;
f = fixture(roster, { catalog: legacy }); f.loaded(); f.loaded();
let migrated = JSON.parse(f.files.get('config/tnk-titles.json'));
assert.equal(migrated.titles.filter(t => t.id === 'tnk:beta_tester').length, 1);
assert.equal(migrated.autoEquipFirst, false);
assert.deepEqual(migrated.titles.filter(t => t.id !== 'tnk:beta_tester'), legacy.titles);
let bad = copy(roster); bad.grants[tester].push('tnk:unknown'); f = fixture(bad); f.login(); assert.equal(f.owned.size, 0); assert(f.errors.length);
bad = copy(roster); bad.grants['not-a-uuid'] = ['tnk:beta_tester']; f = fixture(bad); f.login(); assert.equal(f.commands.length, 0);
bad = copy(roster); bad.grants[tester.replaceAll('-', '').toUpperCase()] = ['tnk:beta_tester']; f = fixture(bad); f.login(); assert.equal(f.owned.size, 0); assert(f.errors.length);
bad = copy(roster); bad.grants[tester] = ['tnk:beta_tester\n op @a']; f = fixture(bad); f.login(); assert.equal(f.commands.length, 0);
f = fixture(roster, { modPresent: false }); f.login(); assert.equal(f.commands.length, 0);
f = fixture(roster, { catalog: legacy, rejectFirstReload: true }); f.login(); assert.equal(f.owned.size, 0); assert.deepEqual(JSON.parse(f.files.get('config/tnk-titles.json')), legacy);
f = fixture(roster); f.server.players.push(f.player()); f.loaded(); assert(f.owned.has('tnk:beta_tester'), 'loaded online players caught up');
console.log('DEFAULT_TITLES_OK: UUID targeting, empty/unlisted roster, name changes, two fresh worlds, selection, duplicate login, revocation, catalog migration, invalid inputs, rollback and absent mod.');
