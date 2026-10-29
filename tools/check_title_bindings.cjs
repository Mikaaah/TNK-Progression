// Evaluate the shipped integration script with controlled KubeJS/FTB event fixtures.
// This verifies bridge logic and references, not the installed mod runtime.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const config = JSON.parse(fs.readFileSync(path.join(root, 'integration/kubejs/config/tnk-title-quests.json'), 'utf8'));
const catalog = JSON.parse(fs.readFileSync(path.join(root, 'src/main/resources/data/tnkprogression/default_titles.json'), 'utf8'));
const titles = new Set(catalog.titles.map(t => t.id));
assert.equal(titles.size, 55);
for (const binding of config.bindings) {
    assert(titles.has(binding.title));
    assert.equal(new Set(binding.quests).size, binding.quests.length);
}
const commands = [];
const complete = new Set();
const callbacks = new Map();
let login;
const player = { username: 'TitleFixture', server: { runCommandSilent: c => { commands.push(c); return 1; } } };
vm.runInNewContext(fs.readFileSync(path.join(root, 'integration/kubejs/server_scripts/progression/tnk_titles.js'), 'utf8'), {
    JsonIO: { readString: () => JSON.stringify(config) },
    FTBQuests: { getServerDataFromPlayer: () => ({ isCompleted: id => complete.has(id) }) },
    PlayerEvents: { loggedIn: callback => { login = callback; } },
    FTBQuestsEvents: { completed: (id, callback) => { assert(!callbacks.has(id)); callbacks.set(id, callback); } }
});
const awarded = id => commands.some(c => c === `tnkprogress title earn TitleFixture ${id}`);
login({ player }); assert.equal(commands.length, 0);
const board = config.bindings.find(b => b.title === 'tnk:board_taskmaster').quests;
complete.add(board[0]); login({ player }); assert(awarded('tnk:board_recruit')); assert(!awarded('tnk:board_citizen')); assert(!awarded('tnk:board_taskmaster'));
board.slice(1, 3).forEach(id => complete.add(id)); login({ player }); assert(awarded('tnk:board_citizen')); assert(!awarded('tnk:board_taskmaster'));
board.forEach(id => complete.add(id)); callbacks.get(board[6])({ onlineMembers: [player] }); assert(awarded('tnk:board_taskmaster'));
complete.clear(); commands.length = 0;
const ars = config.bindings.find(b => b.title === 'tnk:ars_mystic');
ars.quests.slice(0, -1).forEach(id => complete.add(id)); login({ player }); assert(!awarded('tnk:ars_mystic'));
complete.add(ars.quests.at(-1)); login({ player }); assert(awarded('tnk:ars_mystic')); assert(!awarded('tnk:dragon_slayer'));
assert(!commands.some(c => /letsdo_|wither_|naga_/.test(c)));
console.log('TITLE_BINDINGS_OK: empty state, 1/3/7 board thresholds, completion callbacks, chapter catch-up, no guide-to-boss cross-award, unbound titles stay locked.');
